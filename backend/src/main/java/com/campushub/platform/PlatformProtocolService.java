package com.campushub.platform;

import com.campushub.common.BusinessException;
import com.campushub.user.User;
import com.campushub.user.UserRepository;
import java.util.Locale;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PlatformProtocolService {

    private final PlatformProtocolCatalog catalog;
    private final PlatformProtocolVersionRepository versionRepository;
    private final PlatformProtocolConsentRepository consentRepository;
    private final UserRepository userRepository;

    public PlatformProtocolService(
            PlatformProtocolCatalog catalog,
            PlatformProtocolVersionRepository versionRepository,
            PlatformProtocolConsentRepository consentRepository,
            UserRepository userRepository) {
        this.catalog = catalog;
        this.versionRepository = versionRepository;
        this.consentRepository = consentRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public PlatformProtocolSummary current(String roleType, Long userId) {
        PlatformProtocolVersion version = currentVersion(roleType);
        var acceptedAt = userId == null
                ? null
                : consentRepository.findByUserIdAndRoleTypeAndAgreementVersionId(userId, normalizedRole(roleType), version.getId())
                        .map(PlatformProtocolConsent::getAcceptedAt)
                        .orElse(null);
        return summary(version, acceptedAt);
    }

    @Transactional
    public PlatformProtocolConsent acceptConsent(Long userId, String roleType, String requestedVersion) {
        PlatformProtocolVersion version = currentVersion(roleType);
        if (!version.getVersion().equals(requestedVersion)) {
            throw new BusinessException("协议版本已更新，请重新阅读最新协议");
        }
        User user = userRepository.findByIdForUpdate(userId)
                .orElseThrow(() -> new BusinessException("用户不存在"));
        String normalizedRole = normalizedRole(roleType);
        return consentRepository
                .findByUserIdAndRoleTypeAndAgreementVersionId(userId, normalizedRole, version.getId())
                .orElseGet(() -> consentRepository.save(new PlatformProtocolConsent(user, normalizedRole, version, "ROLE_APPLICATION")));
    }

    public PlatformProtocolVersion currentVersion(String roleType) {
        String code;
        try {
            code = catalog.currentForRole(roleType).code();
        } catch (IllegalArgumentException exception) {
            throw new BusinessException(exception.getMessage());
        }
        return versionRepository.findFirstByAgreementCodeAndStatusAndEffectiveAtLessThanEqualOrderByEffectiveAtDesc(code, "ACTIVE", java.time.LocalDateTime.now())
                .orElseThrow(() -> new BusinessException("当前协议暂不可用"));
    }

    public PlatformProtocolConsent findConsent(Long userId, String roleType, Long versionId) {
        return consentRepository.findByUserIdAndRoleTypeAndAgreementVersionId(userId, normalizedRole(roleType), versionId)
                .orElseThrow(() -> new BusinessException("请先阅读并同意当前协议"));
    }

    private PlatformProtocolSummary summary(PlatformProtocolVersion version, java.time.LocalDateTime acceptedAt) {
        return new PlatformProtocolSummary(
                version.getAgreementCode(),
                version.getVersion(),
                version.getTitle(),
                version.getContent(),
                version.getEffectiveAt(),
                acceptedAt);
    }

    private String normalizedRole(String roleType) {
        return roleType == null ? "" : roleType.trim().toUpperCase(Locale.ROOT);
    }
}
