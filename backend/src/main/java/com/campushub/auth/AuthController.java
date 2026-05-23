package com.campushub.auth;

import com.campushub.common.ApiResponse;
import com.campushub.common.BusinessException;
import com.campushub.user.User;
import com.campushub.user.UserRepository;
import jakarta.validation.Valid;
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

    public AuthController(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            RegistrationService registrationService,
            JwtTokenService jwtTokenService,
            CurrentUserService currentUserService,
            UserRoleLookup userRoleLookup) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.registrationService = registrationService;
        this.jwtTokenService = jwtTokenService;
        this.currentUserService = currentUserService;
        this.userRoleLookup = userRoleLookup;
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
        String token = jwtTokenService.issueToken(user.getId(), user.getUsername());
        return ApiResponse.ok(new LoginResponse(
                "Bearer",
                token,
                jwtTokenService.expirationMinutes(),
                CurrentUserSummary.from(user, userRoleLookup.findRoleCodes(user.getId()))));
    }

    @GetMapping("/me")
    public ApiResponse<CurrentUserSummary> me() {
        User user = currentUserService.requireUser();
        return ApiResponse.ok(CurrentUserSummary.from(user, userRoleLookup.findRoleCodes(user.getId())));
    }
}
