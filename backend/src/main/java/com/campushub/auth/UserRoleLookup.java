package com.campushub.auth;

import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class UserRoleLookup {

    private final UserRoleService userRoleService;

    public UserRoleLookup(UserRoleService userRoleService) {
        this.userRoleService = userRoleService;
    }

    public List<String> findRoleCodes(Long userId) {
        return userRoleService.findRoleCodes(userId);
    }
}
