package com.campushub.goods;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GoodsIntentRepository extends JpaRepository<GoodsIntent, Long> {

    @EntityGraph(attributePaths = {"goods", "buyer", "seller", "serviceFee"})
    Optional<GoodsIntent> findByGoodsIdAndBuyerId(Long goodsId, Long buyerId);

    @EntityGraph(attributePaths = {"goods", "buyer", "seller", "serviceFee"})
    List<GoodsIntent> findByGoodsIdOrderByCreatedAtDesc(Long goodsId);
}
