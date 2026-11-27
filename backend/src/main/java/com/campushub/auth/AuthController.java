package com.campushub.auth;

import com.campushub.common.ApiResponse;
import com.campushub.common.BusinessException;
import com.campushub.user.User;
import com.campushub.user.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.Locale;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final RegistrationService registrationService;
    private final JwtTokenService jwtTokenService;
    private final CurrentUserService currentUserService;
    private final UserRoleLookup userRoleLookup;
    private final AuthRateLimitService authRateLimitService;

    public AuthController(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            RegistrationService registrationService,
            JwtTokenService jwtTokenService,
            CurrentUserService currentUserService,
            UserRoleLookup userRoleLookup,
            AuthRateLimitService authRateLimitService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.registrationService = registrationService;
        this.jwtTokenService = jwtTokenService;
        this.currentUserService = currentUserService;
        this.userRoleLookup = userRoleLookup;
        this.authRateLimitService = authRateLimitService;
    }

    @PostMapping("/register/send-code")
    public ApiResponse<RegisterCodeResponse> sendRegisterCode(@Valid @RequestBody SendRegisterCodeRequest request, HttpServletRequest httpRequest) {
        authRateLimitService.checkSendCodeAllowed(httpRequest);
        return ApiResponse.ok(registrationService.sendRegisterCode(request));
    }

    @PostMapping("/register")
    public ApiResponse<RegisterResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ApiResponse.ok(registrationService.register(request));
    }

    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        String account = request.username();
        try (var attempt = authRateLimitService.checkLoginAllowed(httpRequest, account)) {
            User user = userRepository.findByUsername(account)
                    .or(() -> userRepository.findByEmail(account.trim().toLowerCase(Locale.ROOT)))
                    .orElse(null);
            if (user == null || !passwordEncoder.matches(request.password(), user.getPasswordHash())) {
                authRateLimitService.recordLoginFailure(httpRequest, account);
                throw new BusinessException("用户名或密码错误");
            }
            if (!"ACTIVE".equals(user.getStatus())) {
                throw new BusinessException("账号状态不可用");
            }
            String token = jwtTokenService.issueToken(user.getId(), user.getUsername());
            LoginResponse response = new LoginResponse("Bearer", token, jwtTokenService.expirationMinutes(),
                    CurrentUserSummary.from(user, userRoleLookup.findRoleCodes(user.getId())));
            authRateLimitService.recordLoginSuccess(account);
            return ApiResponse.ok(response);
        }
    }

    @GetMapping("/me")
    public ApiResponse<CurrentUserSummary> me() {
        User user = currentUserService.requireUser();
        return ApiResponse.ok(CurrentUserSummary.from(user, userRoleLookup.findRoleCodes(user.getId())));
    }
}
