package com.campushub.auth;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "campushub.mail")
public record MailProperties(
        boolean enabled,
        String provider,
        Smtp smtp,
        Code code) {

    public record Smtp(
            String host,
            int port,
            String username,
            String password,
            String from,
            String fromName) {
    }

    public record Code(
            long ttlMinutes,
            long resendSeconds) {
    }
}
