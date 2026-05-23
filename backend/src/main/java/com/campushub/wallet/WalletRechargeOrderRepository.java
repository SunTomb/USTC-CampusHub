package com.campushub.wallet;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WalletRechargeOrderRepository extends JpaRepository<WalletRechargeOrder, Long> {

    @EntityGraph(attributePaths = {"user", "reviewer"})
    Optional<WalletRechargeOrder> findByRechargeNo(String rechargeNo);

    @EntityGraph(attributePaths = {"user", "reviewer"})
    Optional<WalletRechargeOrder> findByPaymentOrderNo(String paymentOrderNo);

    @EntityGraph(attributePaths = {"user", "reviewer"})
    List<WalletRechargeOrder> findByUserIdOrderByCreatedAtDesc(Long userId);

    @EntityGraph(attributePaths = {"user", "reviewer"})
    List<WalletRechargeOrder> findTop200ByOrderByCreatedAtDesc();

    @EntityGraph(attributePaths = {"user", "reviewer"})
    List<WalletRechargeOrder> findTop200ByStatusOrderByCreatedAtDesc(String status);
}
