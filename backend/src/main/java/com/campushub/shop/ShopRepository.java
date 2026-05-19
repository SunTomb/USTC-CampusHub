package com.campushub.shop;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ShopRepository extends JpaRepository<Shop, Long> {

    List<Shop> findByStatusOrderByRatingDesc(String status);
}
