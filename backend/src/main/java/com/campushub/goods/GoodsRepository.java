package com.campushub.goods;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GoodsRepository extends JpaRepository<Goods, Long> {

    @Override
    @EntityGraph(attributePaths = "seller")
    Optional<Goods> findById(Long id);

    @EntityGraph(attributePaths = "seller")
    List<Goods> findByStatusOrderByCreatedAtDesc(String status);
}
