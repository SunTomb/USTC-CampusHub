package com.campushub.auth;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class JwtTokenService {

    private final String issuer;
    private final SecretKey jwtKey;
    private final long expirationMinutes;

    public JwtTokenService(
            @Value("${campushub.jwt.issuer}") String issuer,
            @Value("${campushub.jwt.secret}") String jwtSecret,
            @Value("${campushub.jwt.expiration-minutes}") long expirationMinutes) {
        this.issuer = issuer;
        this.jwtKey = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
        this.expirationMinutes = expirationMinutes;
    }

    public String issueToken(Long userId, String username) {
        Instant now = Instant.now();
        Instant expiresAt = now.plus(expirationMinutes, ChronoUnit.MINUTES);
        return Jwts.builder()
                .issuer(issuer)
                .subject(username)
                .claim("userId", userId)
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiresAt))
                .signWith(jwtKey)
                .compact();
    }

    public long expirationMinutes() {
        return expirationMinutes;
    }

    public JwtTokenClaims parse(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(jwtKey)
                .requireIssuer(issuer)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        Long userId = claims.get("userId", Long.class);
        return new JwtTokenClaims(userId, claims.getSubject());
    }
}
