package com.campushub.auth;

import com.campushub.common.BusinessException;
import com.campushub.user.User;
import com.campushub.user.UserRepository;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class CurrentUserService {

    private final UserRepository userRepository;

    public CurrentUserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public Long requireUserId() {
        return currentPrincipal()
                .map(CurrentUserPrincipal::userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "请先登录"));
    }

    public User requireUser() {
        Long userId = requireUserId();
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "请先登录"));
    }

    public Long requireAdminId() {
        CurrentUserPrincipal principal = currentPrincipal()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "请先登录"));
        if (!hasAuthority(principal, "ROLE_ADMIN")) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "当前账号无权限执行此操作");
        }
        return principal.userId();
    }

    public Optional<Long> optionalUserId() {
        return currentPrincipal().map(CurrentUserPrincipal::userId);
    }

    public Long requireSameUser(Long requestedUserId) {
        Long currentUserId = requireUserId();
        if (currentUserId.equals(requestedUserId) || isAdmin()) {
            return requestedUserId;
        }
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "当前账号无权限执行此操作");
    }

    public boolean isAdmin() {
        return currentPrincipal()
                .map(principal -> hasAuthority(principal, "ROLE_ADMIN"))
                .orElse(false);
    }

    private Optional<CurrentUserPrincipal> currentPrincipal() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof CurrentUserPrincipal principal)) {
            return Optional.empty();
        }
        return Optional.of(principal);
    }

    private boolean hasAuthority(CurrentUserPrincipal principal, String authority) {
        return principal.authorities().stream().anyMatch(item -> authority.equals(item.getAuthority()));
    }
}
