package com.campushub.shop;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ServiceOrderRepository extends JpaRepository<ServiceOrder, Long> {

    @Override
    @EntityGraph(attributePaths = {"serviceItem", "serviceItem.shop", "serviceItem.shop.owner", "customer", "provider", "serviceFeeRecord"})
    Optional<ServiceOrder> findById(Long id);

    @Override
    @EntityGraph(attributePaths = {"serviceItem", "serviceItem.shop", "customer", "provider", "serviceFeeRecord"})
    List<ServiceOrder> findAll();

    @EntityGraph(attributePaths = {"serviceItem", "serviceItem.shop", "customer", "provider", "serviceFeeRecord"})
    List<ServiceOrder> findByCustomerIdOrderByCreatedAtDesc(Long customerId);

    @EntityGraph(attributePaths = {"serviceItem", "serviceItem.shop", "customer", "provider", "serviceFeeRecord"})
    List<ServiceOrder> findByProviderIdOrderByCreatedAtDesc(Long providerId);

    @EntityGraph(attributePaths = {"serviceItem", "serviceItem.shop", "customer", "provider", "serviceFeeRecord"})
    List<ServiceOrder> findByServiceItemShopIdOrderByCreatedAtDesc(Long shopId);

    boolean existsByServiceItemShopIdAndCustomerId(Long shopId, Long customerId);
}
