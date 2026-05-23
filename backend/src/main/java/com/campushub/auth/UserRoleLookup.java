package com.campushub.auth;

import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class UserRoleLookup {

    private final JdbcTemplate jdbcTemplate;

    public UserRoleLookup(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<String> findRoleCodes(Long userId) {
        return jdbcTemplate.query(
                "SELECT r.code FROM user_roles ur JOIN roles r ON ur.role_id = r.id WHERE ur.user_id = ?",
                (resultSet, rowNumber) -> resultSet.getString("code"),
                userId);
    }
}
