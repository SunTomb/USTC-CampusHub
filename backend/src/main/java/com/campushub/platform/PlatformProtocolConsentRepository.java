package com.campushub.platform;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlatformProtocolConsentRepository extends JpaRepository<PlatformProtocolConsent, Long> {

    Optional<PlatformProtocolConsent> findByUserIdAndRoleTypeAndAgreementVersionId(
            Long userId, String roleType, Long agreementVersionId);
}
