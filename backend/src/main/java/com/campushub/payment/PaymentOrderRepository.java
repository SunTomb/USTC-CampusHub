package com.campushub.payment;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentOrderRepository extends JpaRepository<PaymentOrder, Long> {

    @EntityGraph(attributePaths = "payer")
    Optional<PaymentOrder> findByOrderNo(String orderNo);

    @EntityGraph(attributePaths = "payer")
    Optional<PaymentOrder> findByBusinessTypeAndBusinessIdAndStatus(String businessType, Long businessId, String status);

    @EntityGraph(attributePaths = "payer")
    List<PaymentOrder> findByStatusOrderByCreatedAtDesc(String status);

    @EntityGraph(attributePaths = "payer")
    List<PaymentOrder> findTop200ByOrderByCreatedAtDesc();
}
