package com.campushub.shop;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ServiceItemRepository extends JpaRepository<ServiceItem, Long> {

    @Override
    @EntityGraph(attributePaths = {"shop", "shop.owner"})
    Optional<ServiceItem> findById(Long id);

    @Override
    @EntityGraph(attributePaths = {"shop", "shop.owner"})
    List<ServiceItem> findAll();

    @EntityGraph(attributePaths = {"shop", "shop.owner"})
    List<ServiceItem> findByStatusOrderByCreatedAtDesc(String status);

    @EntityGraph(attributePaths = {"shop", "shop.owner"})
    List<ServiceItem> findByShopIdAndStatusOrderByCreatedAtDesc(Long shopId, String status);

    @EntityGraph(attributePaths = {"shop", "shop.owner"})
    List<ServiceItem> findByShopIdOrderByCreatedAtDesc(Long shopId);
}
