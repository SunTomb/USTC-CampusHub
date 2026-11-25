package com.campushub.goods;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface GoodsRepository extends JpaRepository<Goods, Long> {

    @Override
    @EntityGraph(attributePaths = {"seller", "soldToUser"})
    Optional<Goods> findById(Long id);

    @Override
    @EntityGraph(attributePaths = {"seller", "soldToUser"})
    List<Goods> findAll();

    @EntityGraph(attributePaths = "seller")
    List<Goods> findByStatusOrderByCreatedAtDesc(String status);

    @EntityGraph(attributePaths = "seller")
    List<Goods> findBySellerIdOrderByCreatedAtDesc(Long sellerId);

    @Modifying
    @Query("update Goods g set g.viewCount = g.viewCount + 1 where g.id = :id and g.status <> 'DELETED'")
    int incrementViews(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select g from Goods g where g.id = :id")
    Optional<Goods> findByIdForUpdate(@Param("id") Long id);
}
