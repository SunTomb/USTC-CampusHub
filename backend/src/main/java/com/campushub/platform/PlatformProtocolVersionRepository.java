package com.campushub.platform;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlatformProtocolVersionRepository extends JpaRepository<PlatformProtocolVersion, Long> {

    Optional<PlatformProtocolVersion> findFirstByAgreementCodeAndStatusAndEffectiveAtLessThanEqualOrderByEffectiveAtDesc(
            String agreementCode, String status, java.time.LocalDateTime now);

    Optional<PlatformProtocolVersion> findByAgreementCodeAndVersion(String agreementCode, String version);
}
