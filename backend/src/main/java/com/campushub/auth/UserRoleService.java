package com.campushub.auth;

import com.campushub.common.BusinessException;
import java.util.List;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class UserRoleService {

    private final JdbcTemplate jdbcTemplate;

    public UserRoleService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<String> findRoleCodes(Long userId) {
        return jdbcTemplate.query(
                "SELECT r.code FROM user_roles ur JOIN roles r ON ur.role_id = r.id WHERE ur.user_id = ?",
                (resultSet, rowNumber) -> resultSet.getString("code"),
                userId);
    }

    public void assignRole(Long userId, String roleCode) {
        Long roleId = jdbcTemplate.query(
                "SELECT id FROM roles WHERE code = ?",
                resultSet -> resultSet.next() ? resultSet.getLong("id") : null,
                roleCode);
        if (roleId == null) {
            throw new BusinessException("角色不存在");
        }
        try {
            jdbcTemplate.update(
                    "INSERT INTO user_roles (user_id, role_id) VALUES (?, ?)",
                    userId,
                    roleId);
        } catch (DuplicateKeyException ignored) {
            // Role assignment is intentionally idempotent.
        }
    }
}
