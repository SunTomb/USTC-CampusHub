package com.campushub.wallet;

import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WalletWithdrawalRequestRepository extends JpaRepository<WalletWithdrawalRequest, Long> {

    @EntityGraph(attributePaths = {"user", "reviewer"})
    List<WalletWithdrawalRequest> findByUserIdOrderByCreatedAtDesc(Long userId);

    @EntityGraph(attributePaths = {"user", "reviewer"})
    List<WalletWithdrawalRequest> findTop200ByOrderByCreatedAtDesc();

    @EntityGraph(attributePaths = {"user", "reviewer"})
    List<WalletWithdrawalRequest> findTop200ByStatusOrderByCreatedAtDesc(String status);
}
