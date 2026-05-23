package com.campushub.wallet;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WalletFlowRepository extends JpaRepository<WalletFlow, Long> {

    List<WalletFlow> findByUserIdOrderByCreatedAtDesc(Long userId);

    Optional<WalletFlow> findByIdempotencyKey(String idempotencyKey);

    List<WalletFlow> findByBusinessTypeAndBusinessIdOrderByCreatedAtDesc(String businessType, Long businessId);

    List<WalletFlow> findTop300ByOrderByCreatedAtDesc();
}
