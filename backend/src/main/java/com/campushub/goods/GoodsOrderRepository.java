package com.campushub.goods;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface GoodsOrderRepository extends JpaRepository<GoodsOrder, Long> {

    boolean existsByGoodsIdAndBuyerId(Long goodsId, Long buyerId);

    @Query("select (count(o) > 0) from GoodsOrder o where o.goods.id = :id and (o.status in ('PAID','COMPLETED') or o.escrowStatus in ('FROZEN','DISPUTED','RELEASED'))")
    boolean hasCommittedOrder(@Param("id") Long id);

    @Query("select (count(o) > 0) from GoodsOrder o where o.goods.id=:id and o.status not in ('COMPLETED','CANCELED','CANCELLED','CLOSED','FAILED','EXPIRED')")
    boolean hasUnfinishedOrder(@Param("id") Long id);

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
