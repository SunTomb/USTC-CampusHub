package com.campushub.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class JwtTokenServiceTest {

    @Test
    void issuesAndParsesUserToken() {
        JwtTokenService service = new JwtTokenService(
                "campushub-test",
                "01234567890123456789012345678901",
                60);

        String token = service.issueToken(1L, "alice");

        JwtTokenClaims claims = service.parse(token);
        assertThat(claims.userId()).isEqualTo(1L);
        assertThat(claims.username()).isEqualTo("alice");
    }

    @Test
    void rejectsTokenSignedWithDifferentSecret() {
        JwtTokenService issuer = new JwtTokenService(
                "campushub-test",
                "01234567890123456789012345678901",
                60);
        JwtTokenService verifier = new JwtTokenService(
                "campushub-test",
                "abcdefghijklmnopqrstuvwxyz123456",
                60);

        String token = issuer.issueToken(1L, "alice");

        assertThatThrownBy(() -> verifier.parse(token)).isInstanceOf(RuntimeException.class);
    }
}
