package com.campushub.wallet;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WalletFlowRepository extends JpaRepository<WalletFlow, Long> {

    @EntityGraph(attributePaths = {"user", "counterpartyUser", "operator", "walletAccount"})
    List<WalletFlow> findByUserIdOrderByCreatedAtDesc(Long userId);

    Optional<WalletFlow> findByIdempotencyKey(String idempotencyKey);

    List<WalletFlow> findByBusinessTypeAndBusinessIdOrderByCreatedAtDesc(String businessType, Long businessId);

    List<WalletFlow> findTop300ByOrderByCreatedAtDesc();
}
