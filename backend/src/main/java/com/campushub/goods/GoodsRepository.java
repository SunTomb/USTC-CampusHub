package com.campushub.goods;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GoodsRepository extends JpaRepository<Goods, Long> {

    List<Goods> findByStatusOrderByCreatedAtDesc(String status);
}
