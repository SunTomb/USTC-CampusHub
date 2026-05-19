package com.campushub.shop;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ServiceOrderRepository extends JpaRepository<ServiceOrder, Long> {

    List<ServiceOrder> findByCustomerIdOrderByCreatedAtDesc(Long customerId);

    List<ServiceOrder> findByProviderIdOrderByCreatedAtDesc(Long providerId);
}
