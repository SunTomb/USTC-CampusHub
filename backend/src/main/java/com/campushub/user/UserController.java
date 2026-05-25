package com.campushub.user;

import com.campushub.auth.CurrentUserService;
import com.campushub.common.ApiResponse;
import com.campushub.common.BusinessException;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserRepository userRepository;
    private final CurrentUserService currentUserService;
    private final PasswordEncoder passwordEncoder;

    public UserController(UserRepository userRepository, CurrentUserService currentUserService, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.currentUserService = currentUserService;
        this.passwordEncoder = passwordEncoder;
    }

    @PutMapping("/me/profile")
    public ApiResponse<UserSummary> updateProfile(@Valid @RequestBody UpdateProfileRequest request) {
        User user = currentUserService.requireUser();
        String username = normalizeUsername(request.username());
        String nickname = request.nickname().trim();
        if (!user.getUsername().equals(username) && userRepository.existsByUsername(username)) {
            throw new BusinessException("用户名已被使用");
        }
        user.updateProfile(username, nickname);
        return ApiResponse.ok(UserSummary.from(userRepository.save(user)));
    }

    @PutMapping("/me/password")
    public ApiResponse<Void> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        User user = currentUserService.requireUser();
        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw new BusinessException("当前密码不正确");
        }
        user.resetPasswordHash(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);
        return ApiResponse.ok(null);
    }

    private String normalizeUsername(String rawUsername) {
        String username = rawUsername.trim();
        if (!username.matches("[a-zA-Z0-9_]{3,64}")) {
            throw new BusinessException("用户名只能包含 3-64 位字母、数字或下划线");
        }
        return username;
    }

    @GetMapping
    public ApiResponse<List<UserSummary>> listUsers() {
        List<UserSummary> users = userRepository.findAll().stream()
                .map(UserSummary::from)
                .toList();
        return ApiResponse.ok(users);
    }

    @GetMapping("/{id}")
    public ApiResponse<UserSummary> getUser(@PathVariable Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new BusinessException("user not found"));
        return ApiResponse.ok(UserSummary.from(user));
    }
}
