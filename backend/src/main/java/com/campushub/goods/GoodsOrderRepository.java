package com.campushub.goods;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GoodsOrderRepository extends JpaRepository<GoodsOrder, Long> {

    List<GoodsOrder> findByBuyerIdOrderByCreatedAtDesc(Long buyerId);

    List<GoodsOrder> findBySellerIdOrderByCreatedAtDesc(Long sellerId);
}
