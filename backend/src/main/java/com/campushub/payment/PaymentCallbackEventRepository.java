package com.campushub.payment;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentCallbackEventRepository extends JpaRepository<PaymentCallbackEvent, Long> {
    Optional<PaymentCallbackEvent> findByEventId(String eventId);
    List<PaymentCallbackEvent> findTop200ByOrderByCreatedAtDesc();
}
