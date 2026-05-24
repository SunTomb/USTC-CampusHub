package com.campushub.beta;

import com.campushub.user.User;
import com.campushub.user.UserRepository;
import com.campushub.wallet.WalletAccount;
import com.campushub.wallet.WalletAccountRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Component
public class BetaDemoAccountInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(BetaDemoAccountInitializer.class);

    private final BetaDemoProperties properties;
    private final UserRepository userRepository;
    private final WalletAccountRepository walletAccountRepository;
    private final PasswordEncoder passwordEncoder;
    private final JdbcTemplate jdbcTemplate;

    public BetaDemoAccountInitializer(
            BetaDemoProperties properties,
            UserRepository userRepository,
            WalletAccountRepository walletAccountRepository,
            PasswordEncoder passwordEncoder,
            JdbcTemplate jdbcTemplate) {
        this.properties = properties;
        this.userRepository = userRepository;
        this.walletAccountRepository = walletAccountRepository;
        this.passwordEncoder = passwordEncoder;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (!properties.isDemoResetEnabled()) {
            return;
        }
        upsertDemoAccount(properties.getStudent(), false);
        upsertDemoAccount(properties.getAdmin(), true);
        log.info("Beta demo accounts are ready; disable CAMPUSHUB_BETA_DEMO_RESET_ENABLED after verification");
    }

    private void upsertDemoAccount(BetaDemoProperties.DemoAccount account, boolean admin) {
        validateAccount(account, admin ? "admin" : "student");
        User user = userRepository.findByEmail(account.getEmail())
                .orElseGet(() -> new User(
                        account.getStudentNo(),
                        account.getUsername(),
                        passwordEncoder.encode(account.getPassword()),
                        account.getRealName(),
                        account.getNickname(),
                        account.getPhone(),
                        account.getEmail(),
                        "ACTIVE"));
        user.resetPasswordHash(passwordEncoder.encode(account.getPassword()));
        user.updateBetaProfile(
                account.getRealName(),
                account.getNickname(),
                account.getPhone(),
                account.getWechatContact(),
                account.getQqContact());
        User saved = userRepository.save(user);
        ensureRole(saved.getId(), "ROLE_STUDENT");
        if (admin) {
            ensureRole(saved.getId(), "ROLE_ADMIN");
        }
        ensureWallet(saved);
    }

    private void validateAccount(BetaDemoProperties.DemoAccount account, String label) {
        requireText(account.getEmail(), label + " email");
        requireText(account.getUsername(), label + " username");
        requireText(account.getPassword(), label + " password");
        requireText(account.getStudentNo(), label + " student number");
        requireText(account.getPhone(), label + " phone");
        if (!StringUtils.hasText(account.getWechatContact()) && !StringUtils.hasText(account.getQqContact())) {
            throw new IllegalStateException("Beta demo " + label + " must provide WeChat or QQ contact");
        }
        if (account.getPassword().length() < 12 || account.getPassword().toLowerCase().contains("change-me")) {
            throw new IllegalStateException("Beta demo " + label + " password must be non-placeholder and at least 12 characters");
        }
    }

    private void requireText(String value, String field) {
        if (!StringUtils.hasText(value)) {
            throw new IllegalStateException("Missing beta demo " + field);
        }
    }

    private void ensureRole(Long userId, String roleCode) {
        Long roleId = jdbcTemplate.query(
                "SELECT id FROM roles WHERE code = ?",
                resultSet -> resultSet.next() ? resultSet.getLong("id") : null,
                roleCode);
        if (roleId == null) {
            return;
        }
        Integer existing = jdbcTemplate.query(
                "SELECT COUNT(*) FROM user_roles WHERE user_id = ? AND role_id = ?",
                resultSet -> resultSet.next() ? resultSet.getInt(1) : 0,
                userId,
                roleId);
        if (existing == null || existing == 0) {
            jdbcTemplate.update("INSERT INTO user_roles (user_id, role_id) VALUES (?, ?)", userId, roleId);
        }
    }

    private void ensureWallet(User user) {
        walletAccountRepository.findByUserId(user.getId())
                .orElseGet(() -> walletAccountRepository.save(new WalletAccount(user)));
    }
}
