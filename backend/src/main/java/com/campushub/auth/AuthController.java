package com.campushub.auth;

import com.campushub.common.ApiResponse;
import com.campushub.common.BusinessException;
import com.campushub.user.User;
import com.campushub.user.UserRepository;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.validation.Valid;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserRepository userRepository;
    private final RegistrationService registrationService;
    private final SecretKey jwtKey;
    private final String issuer;
    private final long expirationMinutes;

    public AuthController(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            RegistrationService registrationService,
            @Value("${campushub.jwt.secret}") String jwtSecret,
            @Value("${campushub.jwt.issuer}") String issuer,
            @Value("${campushub.jwt.expiration-minutes}") long expirationMinutes) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.registrationService = registrationService;
        this.jwtKey = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
        this.issuer = issuer;
        this.expirationMinutes = expirationMinutes;
    }

    @PostMapping("/register/send-code")
    public ApiResponse<RegisterCodeResponse> sendRegisterCode(@Valid @RequestBody SendRegisterCodeRequest request) {
        return ApiResponse.ok(registrationService.sendRegisterCode(request));
    }

    @PostMapping("/register")
    public ApiResponse<RegisterResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ApiResponse.ok(registrationService.register(request));
    }

    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        User user = userRepository.findByUsername(request.username())
                .orElseThrow(() -> new BusinessException("用户名或密码错误"));
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new BusinessException("用户名或密码错误");
        }
        if (!"ACTIVE".equals(user.getStatus())) {
            throw new BusinessException("账号状态不可用");
        }
        Instant now = Instant.now();
        Instant expiresAt = now.plus(expirationMinutes, ChronoUnit.MINUTES);
        String token = Jwts.builder()
                .issuer(issuer)
                .subject(user.getUsername())
                .claim("userId", user.getId())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiresAt))
                .signWith(jwtKey)
                .compact();
        return ApiResponse.ok(new LoginResponse(
                "Bearer",
                token,
                expirationMinutes,
                CurrentUserSummary.from(user)));
    }
}
