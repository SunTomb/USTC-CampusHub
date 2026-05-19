package com.campushub.shop;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ServiceItemRepository extends JpaRepository<ServiceItem, Long> {

    List<ServiceItem> findByStatusOrderByCreatedAtDesc(String status);

    List<ServiceItem> findByShopIdAndStatusOrderByCreatedAtDesc(Long shopId, String status);
}
