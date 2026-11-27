package com.campushub.auth;

import java.time.Clock;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AuthRateLimitServiceTest {
    private final Clock clock = mock(Clock.class);
    private final AuthRateLimitService limits = new AuthRateLimitService(true, clock);

    @Test
    void sendWindowsExpireAtTheirExactBoundaries() {
        var request = request("192.0.2.1");
        for (int batch = 0; batch < 3; batch++) {
            atMinute(batch * 10);
            for (int i = 0; i < 5; i++) limits.checkSendCodeAllowed(request);
            blocked(() -> limits.checkSendCodeAllowed(request));
        }
        atMinute(30);
        blocked(() -> limits.checkSendCodeAllowed(request));
        atMinute(60);
        limits.checkSendCodeAllowed(request);
    }

    @Test
    void simultaneousCredentialChecksReserveSlotsAndReleaseOnClose() {
        atMinute(0);
        var request = request("192.0.2.1");
        var attempts = new AuthRateLimitService.LoginAttempt[5];
        for (int i = 0; i < attempts.length; i++) attempts[i] = limits.checkLoginAllowed(request, "fixture");
        blocked(() -> limits.checkLoginAllowed(request, "fixture"));
        for (var attempt : attempts) {
            limits.recordLoginFailure(request, "fixture");
            attempt.close();
        }
        atMinute(14);
        blocked(() -> limits.checkLoginAllowed(request, "fixture"));
        atMinute(15);
        limits.checkLoginAllowed(request, "fixture").close();
    }

    @Test
    void fullMapPreservesActiveLimitsAndReclaimsExpiredKeys() {
        atMinute(0);
        var request = request("192.0.2.1");
        for (int i = 0; i < 9_999; i++) limits.recordLoginFailure(request, "fixture" + i);
        blocked(() -> limits.checkSendCodeAllowed(request));
        atMinute(16);
        limits.checkSendCodeAllowed(request);
    }

    @ParameterizedTest
    @ValueSource(strings = {"127.0.0.1", "::1", "0:0:0:0:0:0:0:1", "10.0.0.1", "172.16.0.1", "172.31.0.1", "192.168.1.2"})
    void trustedPeersUseTheFirstAddressWithRealIpTakingPrecedence(String peer) {
        var request = request(peer);
        request.addHeader("X-Forwarded-For", " 192.0.2.1 , 192.0.2.2");
        assertThat(limits.resolveClientIp(request)).isEqualTo("192.0.2.1");
        request.addHeader("X-Real-IP", " , 192.0.2.3 , 192.0.2.4");
        assertThat(limits.resolveClientIp(request)).isEqualTo("192.0.2.3");
    }

    @ParameterizedTest
    @ValueSource(strings = {"172.15.0.1", "172.32.0.1", "127.0.0.2", "192.0.2.25", "2001:db8::1"})
    void otherPeersIgnoreBothHeaders(String peer) {
        var request = request(peer);
        request.addHeader("X-Forwarded-For", "192.0.2.1");
        request.addHeader("X-Real-IP", "192.0.2.2");
        assertThat(limits.resolveClientIp(request)).isEqualTo(peer);
    }

    private static MockHttpServletRequest request(String peer) {
        var request = new MockHttpServletRequest();
        request.setRemoteAddr(peer);
        return request;
    }

    private void atMinute(int n) {
        when(clock.instant()).thenReturn(Instant.EPOCH.plusSeconds(60L * n));
    }

    private static void blocked(Runnable action) {
        var denied = assertThrows(ResponseStatusException.class, action::run);
        assertThat(denied.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
    }
}
