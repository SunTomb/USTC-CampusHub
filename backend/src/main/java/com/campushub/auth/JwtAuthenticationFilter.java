package com.campushub.auth;

import com.campushub.common.ApiResponse;
import com.campushub.user.User;
import com.campushub.user.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenService jwtTokenService;
    private final UserRepository userRepository;
    private final UserRoleLookup userRoleLookup;
    private final ObjectMapper objectMapper;

    public JwtAuthenticationFilter(
            JwtTokenService jwtTokenService,
            UserRepository userRepository,
            UserRoleLookup userRoleLookup,
            ObjectMapper objectMapper) {
        this.jwtTokenService = jwtTokenService;
        this.userRepository = userRepository;
        this.userRoleLookup = userRoleLookup;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String authorization = request.getHeader("Authorization");
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            JwtTokenClaims claims = jwtTokenService.parse(authorization.substring(7));
            User user = userRepository.findById(claims.userId()).orElseThrow();
            if (!"ACTIVE".equals(user.getStatus())) {
                throw new IllegalStateException("inactive user");
            }
            List<SimpleGrantedAuthority> authorities = userRoleLookup.findRoleCodes(user.getId()).stream()
                    .map(SimpleGrantedAuthority::new)
                    .toList();
            CurrentUserPrincipal principal = new CurrentUserPrincipal(
                    user.getId(), user.getUsername(), user.getNickname(), user.getStatus(), authorities);
            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                    principal, null, authorities);
            SecurityContextHolder.getContext().setAuthentication(authentication);
        } catch (Exception exception) {
            SecurityContextHolder.clearContext();
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding("UTF-8");
            objectMapper.writeValue(response.getWriter(), ApiResponse.fail("请先登录"));
            return;
        }
        filterChain.doFilter(request, response);
    }
}
