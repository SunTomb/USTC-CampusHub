package com.campushub.auth;

import java.util.Collection;
import org.springframework.security.core.GrantedAuthority;

public record CurrentUserPrincipal(
        Long userId,
        String username,
        String nickname,
        String status,
        Collection<? extends GrantedAuthority> authorities) {
}
