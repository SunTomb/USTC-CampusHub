package com.campushub.wallet;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WalletAccountRepository extends JpaRepository<WalletAccount, Long> {

    @Override
    @EntityGraph(attributePaths = "user")
    List<WalletAccount> findAll();

    @EntityGraph(attributePaths = "user")
    Optional<WalletAccount> findByUserId(Long userId);
}
