package com.campushub.shop;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ServiceOrderRepository extends JpaRepository<ServiceOrder, Long> {

    @Override
    @EntityGraph(attributePaths = {"serviceItem", "customer", "provider"})
    Optional<ServiceOrder> findById(Long id);

    @Override
    @EntityGraph(attributePaths = {"serviceItem", "customer", "provider"})
    List<ServiceOrder> findAll();

    @EntityGraph(attributePaths = {"serviceItem", "customer", "provider"})
    List<ServiceOrder> findByCustomerIdOrderByCreatedAtDesc(Long customerId);

    @EntityGraph(attributePaths = {"serviceItem", "customer", "provider"})
    List<ServiceOrder> findByProviderIdOrderByCreatedAtDesc(Long providerId);
}
