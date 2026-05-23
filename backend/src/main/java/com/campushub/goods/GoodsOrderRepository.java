package com.campushub.goods;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GoodsOrderRepository extends JpaRepository<GoodsOrder, Long> {

    @Override
    @EntityGraph(attributePaths = {"goods", "buyer", "seller"})
    Optional<GoodsOrder> findById(Long id);

    @Override
    @EntityGraph(attributePaths = {"goods", "buyer", "seller"})
    List<GoodsOrder> findAll();

    @EntityGraph(attributePaths = {"goods", "buyer", "seller"})
    List<GoodsOrder> findByBuyerIdOrderByCreatedAtDesc(Long buyerId);

    @EntityGraph(attributePaths = {"goods", "buyer", "seller"})
    List<GoodsOrder> findBySellerIdOrderByCreatedAtDesc(Long sellerId);

    @EntityGraph(attributePaths = {"goods", "buyer", "seller"})
    List<GoodsOrder> findTop200ByTradeModeOrderByCreatedAtDesc(String tradeMode);

    @EntityGraph(attributePaths = {"goods", "buyer", "seller"})
    List<GoodsOrder> findTop200ByEscrowStatusOrderByCreatedAtDesc(String escrowStatus);
}
