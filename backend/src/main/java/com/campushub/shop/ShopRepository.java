package com.campushub.shop;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ShopRepository extends JpaRepository<Shop, Long> {

    @Override
    @EntityGraph(attributePaths = "owner")
    Optional<Shop> findById(Long id);

    @EntityGraph(attributePaths = "owner")
    List<Shop> findByStatusOrderByRatingDesc(String status);

    @EntityGraph(attributePaths = "owner")
    Optional<Shop> findByOwnerId(Long ownerId);
}
