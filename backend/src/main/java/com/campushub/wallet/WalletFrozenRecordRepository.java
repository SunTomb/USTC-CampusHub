package com.campushub.wallet;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WalletFrozenRecordRepository extends JpaRepository<WalletFrozenRecord, Long> {

    Optional<WalletFrozenRecord> findByBusinessTypeAndBusinessIdAndStatus(String businessType, Long businessId, String status);

    @EntityGraph(attributePaths = "user")
    List<WalletFrozenRecord> findByUserIdOrderByFrozenAtDesc(Long userId);

    @EntityGraph(attributePaths = "user")
    List<WalletFrozenRecord> findTop200ByOrderByFrozenAtDesc();
}
