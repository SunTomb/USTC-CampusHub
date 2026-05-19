package com.campushub.auth;

import com.campushub.common.BusinessException;
import com.campushub.user.User;
import com.campushub.user.UserRepository;
import com.campushub.wallet.WalletAccount;
import com.campushub.wallet.WalletAccountRepository;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Locale;
import java.util.regex.Pattern;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RegistrationService {

    private static final String REGISTER_PURPOSE = "REGISTER";
    private static final int MAX_ATTEMPTS = 5;
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");
    private static final SecureRandom RANDOM = new SecureRandom();

    private final EmailVerificationCodeRepository codeRepository;
    private final RegisterMailService mailService;
    private final MailProperties mailProperties;
    private final UserRepository userRepository;
    private final WalletAccountRepository walletAccountRepository;
    private final PasswordEncoder passwordEncoder;
    private final JdbcTemplate jdbcTemplate;

    public RegistrationService(
            EmailVerificationCodeRepository codeRepository,
            RegisterMailService mailService,
            MailProperties mailProperties,
            UserRepository userRepository,
            WalletAccountRepository walletAccountRepository,
            PasswordEncoder passwordEncoder,
            JdbcTemplate jdbcTemplate) {
        this.codeRepository = codeRepository;
        this.mailService = mailService;
        this.mailProperties = mailProperties;
        this.userRepository = userRepository;
        this.walletAccountRepository = walletAccountRepository;
        this.passwordEncoder = passwordEncoder;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional
    public RegisterCodeResponse sendRegisterCode(SendRegisterCodeRequest request) {
        String email = normalizeAndValidateEmail(request.email());
        if (userRepository.existsByEmail(email)) {
            throw new BusinessException("该邮箱已注册");
        }

        Instant now = Instant.now();
        codeRepository.findFirstByEmailAndPurposeOrderByIdDesc(email, REGISTER_PURPOSE)
                .filter(code -> code.getLastSentAt().plus(mailProperties.code().resendSeconds(), ChronoUnit.SECONDS).isAfter(now))
                .ifPresent(code -> {
                    throw new BusinessException("验证码发送过于频繁，请稍后再试");
                });

        String rawCode = generateCode();
        EmailVerificationCode verificationCode = new EmailVerificationCode(
                email,
                passwordEncoder.encode(rawCode),
                REGISTER_PURPOSE,
                now.plus(mailProperties.code().ttlMinutes(), ChronoUnit.MINUTES),
                now);
        codeRepository.save(verificationCode);
        mailService.sendRegisterCode(email, rawCode);
        return new RegisterCodeResponse(email, mailProperties.code().ttlMinutes(), mailProperties.code().resendSeconds());
    }

    @Transactional
    public RegisterResponse register(RegisterRequest request) {
        String email = normalizeAndValidateEmail(request.email());
        if (userRepository.existsByEmail(email)) {
            throw new BusinessException("该邮箱已注册");
        }

        EmailVerificationCode verificationCode = codeRepository
                .findFirstByEmailAndPurposeOrderByIdDesc(email, REGISTER_PURPOSE)
                .orElseThrow(() -> new BusinessException("请先获取邮箱验证码"));
        Instant now = Instant.now();
        if (verificationCode.isUsed()) {
            throw new BusinessException("验证码已使用，请重新获取");
        }
        if (verificationCode.getExpiresAt().isBefore(now)) {
            throw new BusinessException("验证码已过期，请重新获取");
        }
        if (verificationCode.getAttemptCount() >= MAX_ATTEMPTS) {
            throw new BusinessException("验证码错误次数过多，请重新获取");
        }
        verificationCode.increaseAttemptCount();
        if (!passwordEncoder.matches(request.emailCode(), verificationCode.getCodeHash())) {
            throw new BusinessException("验证码错误");
        }
        verificationCode.markUsed(now);

        String baseName = email.substring(0, email.indexOf('@')).replaceAll("[^a-zA-Z0-9_]", "_");
        String username = uniqueUsername(baseName);
        User user = userRepository.save(new User(
                uniqueStudentNo(),
                username,
                passwordEncoder.encode(request.password()),
                "未认证学生",
                username,
                uniquePhonePlaceholder(),
                email,
                "ACTIVE"));
        assignStudentRoleIfPresent(user.getId());
        walletAccountRepository.save(new WalletAccount(user));
        return RegisterResponse.from(user);
    }

    private String normalizeAndValidateEmail(String rawEmail) {
        String email = rawEmail == null ? "" : rawEmail.trim().toLowerCase(Locale.ROOT);
        if (!EMAIL_PATTERN.matcher(email).matches()) {
            throw new BusinessException("邮箱格式不正确");
        }
        String domain = email.substring(email.indexOf('@') + 1);
        if (!(domain.equals("edu.cn") || domain.endsWith(".edu.cn"))) {
            throw new BusinessException("注册邮箱必须为 edu.cn 校园邮箱");
        }
        return email;
    }

    private String generateCode() {
        return String.format("%06d", RANDOM.nextInt(1_000_000));
    }

    private String uniqueUsername(String baseName) {
        String normalized = baseName == null || baseName.isBlank() ? "student" : baseName;
        String candidate = normalized.length() > 48 ? normalized.substring(0, 48) : normalized;
        int suffix = 0;
        while (userRepository.existsByUsername(candidate)) {
            suffix++;
            String tail = "_" + suffix;
            String prefix = normalized.length() + tail.length() > 64
                    ? normalized.substring(0, 64 - tail.length())
                    : normalized;
            candidate = prefix + tail;
        }
        return candidate;
    }

    private String uniqueStudentNo() {
        String candidate;
        do {
            candidate = "REG" + System.currentTimeMillis() + RANDOM.nextInt(1000);
        } while (userRepository.existsByStudentNo(candidate));
        return candidate;
    }

    private String uniquePhonePlaceholder() {
        String candidate;
        do {
            candidate = "EMAIL" + System.currentTimeMillis() + RANDOM.nextInt(1000);
        } while (userRepository.existsByPhone(candidate));
        return candidate;
    }

    private void assignStudentRoleIfPresent(Long userId) {
        Long roleId = jdbcTemplate.query(
                "SELECT id FROM roles WHERE code = ?",
                resultSet -> resultSet.next() ? resultSet.getLong("id") : null,
                "ROLE_STUDENT");
        if (roleId != null) {
            jdbcTemplate.update(
                    "INSERT INTO user_roles (user_id, role_id) VALUES (?, ?)",
                    userId,
                    roleId);
        }
    }
}
