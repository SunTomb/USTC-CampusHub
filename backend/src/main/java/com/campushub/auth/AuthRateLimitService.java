package com.campushub.auth;

import jakarta.servlet.http.HttpServletRequest;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AuthRateLimitService {
    private static final int MAX_BUCKETS = 10_000;
    private static final String SEND_ERROR = "验证码请求过于频繁，请稍后再试";
    private static final String LOGIN_ERROR = "登录尝试过于频繁，请 15 分钟后再试";
    private static final Duration HOUR = Duration.ofHours(1), TEN_MIN = Duration.ofMinutes(10),
            IP_WINDOW = Duration.ofMinutes(5), ACCOUNT_WINDOW = Duration.ofMinutes(15);
    private final boolean enabled;
    private final Clock clock;
    // compute operations serialize each individual bucket. Only new keys share the capacity lock.
    private final ConcurrentHashMap<String, Bucket> buckets = new ConcurrentHashMap<>();
    private final Object capacityLock = new Object();
    private Instant nextSweep = Instant.MIN;

    private static final class Bucket {
        final Duration window;
        final Deque<Instant> events = new ArrayDeque<>();
        int pending;

        Bucket(Duration window) { this.window = window; }

        void prune(Instant now) {
            Instant cutoff = now.minus(window);
            while (!events.isEmpty() && !events.getFirst().isAfter(cutoff)) events.removeFirst();
        }

        boolean empty() { return pending == 0 && events.isEmpty(); }
    }

    @Autowired
    public AuthRateLimitService(@Value("${campushub.auth.rate-limit.enabled:true}") boolean enabled) {
        this(enabled, Clock.systemUTC());
    }

    AuthRateLimitService(boolean enabled, Clock clock) {
        this.enabled = enabled;
        this.clock = clock;
    }

    public String resolveClientIp(HttpServletRequest request) {
        String peer = request.getRemoteAddr();
        if (peer == null) return "unknown";
        if (!trustedPeer(peer)) return peer;
        String real = request.getHeader("X-Real-IP");
        if (real != null) {
            for (String token : real.split(",")) {
                if (token.isBlank()) continue;
                String ip = literalIp(token.trim());
                if (ip != null) return ip;
                break;
            }
        }
        String forwarded = request.getHeader("X-Forwarded-For");
        String ip = forwarded == null ? null : literalIp(forwarded.split(",", 2)[0].trim());
        return ip == null ? peer : ip;
    }

    private static boolean trustedPeer(String peer) {
        if ("127.0.0.1".equals(peer) || "::1".equals(peer) || "0:0:0:0:0:0:0:1".equals(peer)) return true;
        if (peer.indexOf(':') >= 0 || literalIp(peer) == null) return false;
        String[] parts = peer.split("\\.");
        int a = Integer.parseInt(parts[0]), b = Integer.parseInt(parts[1]);
        return a == 10 || (a == 172 && b >= 16 && b <= 31) || (a == 192 && b == 168);
    }

    private static String literalIp(String token) {
        if (token.isEmpty() || token.length() > 45) return null;
        if (token.indexOf(':') < 0) {
            String[] parts = token.split("\\.", -1);
            if (parts.length != 4) return null;
            for (String part : parts) {
                if (part.isEmpty() || part.length() > 3 || (part.length() > 1 && part.charAt(0) == '0')
                        || !part.chars().allMatch(c -> c >= '0' && c <= '9') || Integer.parseInt(part) > 255) return null;
            }
            return token;
        }
        // A colon and only literal-address characters: never resolve a supplied hostname.
        if (!token.matches("[0-9a-fA-F:.]+")) return null;
        try {
            return InetAddress.getByName(token).getHostAddress();
        } catch (UnknownHostException ignored) {
            return null;
        }
    }

    public void checkSendCodeAllowed(HttpServletRequest request) {
        if (!enabled) return;
        update("send:" + resolveClientIp(request), HOUR, SEND_ERROR, (bucket, now) -> {
            Instant cutoff = now.minus(TEN_MIN);
            if (bucket.events.size() >= 15 || bucket.events.stream().filter(t -> t.isAfter(cutoff)).count() >= 5) {
                throw limited(SEND_ERROR);
            }
            bucket.events.addLast(now);
        });
    }

    // Reserve slots until credentials have been checked, including when several checks overlap.
    public LoginAttempt checkLoginAllowed(HttpServletRequest request, String accountIdentifier) {
        if (!enabled) return () -> {};
        String ip = "ip:" + resolveClientIp(request), account = accountKey(accountIdentifier);
        reserve(ip, IP_WINDOW, 20);
        try {
            reserve(account, ACCOUNT_WINDOW, 5);
        } catch (RuntimeException denied) {
            release(ip);
            throw denied;
        }
        return () -> { release(account); release(ip); };
    }

    private void reserve(String key, Duration window, int limit) {
        update(key, window, LOGIN_ERROR, (bucket, now) -> {
            if (bucket.pending + bucket.events.size() >= limit) throw limited(LOGIN_ERROR);
            bucket.pending++;
        });
    }

    private void release(String key) {
        buckets.computeIfPresent(key, (k, bucket) -> {
            bucket.pending--;
            return bucket.empty() ? null : bucket;
        });
    }

    public void recordLoginFailure(HttpServletRequest request, String accountIdentifier) {
        if (!enabled) return;
        record("ip:" + resolveClientIp(request), IP_WINDOW, 20);
        record(accountKey(accountIdentifier), ACCOUNT_WINDOW, 5);
    }

    public void recordLoginSuccess(String accountIdentifier) {
        if (!enabled) return;
        buckets.computeIfPresent(accountKey(accountIdentifier), (k, bucket) -> {
            bucket.events.clear();
            return bucket.empty() ? null : bucket;
        });
    }

    private static String accountKey(String id) {
        String normalized = id.trim().toLowerCase(Locale.ROOT);
        // No stored identifier exceeds this bound; do not retain arbitrary request bodies as keys.
        return normalized.length() <= 320 ? "account:" + normalized : "overlong-account";
    }

    private void record(String key, Duration window, int limit) {
        update(key, window, LOGIN_ERROR, (bucket, now) -> {
            if (bucket.events.size() >= limit) bucket.events.removeFirst();
            bucket.events.addLast(now);
        });
    }

    private void update(String key, Duration window, String error, BiConsumer<Bucket, Instant> action) {
        BiFunction<String, Bucket, Bucket> apply = (k, bucket) -> {
            if (bucket == null) bucket = new Bucket(window);
            Instant now = clock.instant();
            bucket.prune(now);
            action.accept(bucket, now);
            return bucket;
        };
        if (buckets.computeIfPresent(key, apply) != null) return;
        synchronized (capacityLock) {
            if (buckets.size() >= MAX_BUCKETS) {
                Instant now = clock.instant();
                if (!now.isBefore(nextSweep)) {
                    for (String candidate : buckets.keySet()) {
                        buckets.computeIfPresent(candidate, (k, bucket) -> {
                            bucket.prune(now);
                            return bucket.empty() ? null : bucket;
                        });
                    }
                    nextSweep = now.plusSeconds(10);
                }
                if (buckets.size() >= MAX_BUCKETS && !buckets.containsKey(key)) throw limited(error);
            }
            buckets.compute(key, apply);
        }
    }

    private static ResponseStatusException limited(String reason) {
        return new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, reason);
    }

    public interface LoginAttempt extends AutoCloseable {
        @Override void close();
    }
}
