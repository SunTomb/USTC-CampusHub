package com.campushub.wallet;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WalletFlowRepository extends JpaRepository<WalletFlow, Long> {

    List<WalletFlow> findByUserIdOrderByCreatedAtDesc(Long userId);
}
