package com.campushub.user;

import com.campushub.common.ApiResponse;
import com.campushub.common.BusinessException;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserRepository userRepository;

    public UserController(UserRepository userRepository) {
        this.userRepository = userRepository;
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
