package com.campushub.shop;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ServiceOrderSummary(
        Long id,
        String orderNo,
        Long serviceItemId,
        String serviceItemTitle,
        Long shopId,
        String shopName,
        Long customerId,
        String customerNickname,
        Long providerId,
        String providerNickname,
        LocalDateTime appointmentTime,
        BigDecimal amount,
        BigDecimal serviceFee,
        String status,
        String note,
        String contactSnapshot,
        String cancelReason,
        Long serviceFeeId,
        LocalDateTime createdAt,
        LocalDateTime paidAt,
        LocalDateTime completedAt,
        LocalDateTime canceledAt) {

    public static ServiceOrderSummary from(ServiceOrder order) {
        return new ServiceOrderSummary(
                order.getId(),
                order.getOrderNo(),
                order.getServiceItem().getId(),
                order.getServiceItem().getTitle(),
                order.getServiceItem().getShop().getId(),
                order.getServiceItem().getShop().getName(),
                order.getCustomer().getId(),
                order.getCustomer().getNickname(),
                order.getProvider().getId(),
                order.getProvider().getNickname(),
                order.getAppointmentTime(),
                order.getAmount(),
                order.getServiceFee(),
                order.getStatus(),
                order.getNote(),
                order.getContactSnapshot(),
                order.getCancelReason(),
                order.getServiceFeeRecord() == null ? null : order.getServiceFeeRecord().getId(),
                order.getCreatedAt(),
                order.getPaidAt(),
                order.getCompletedAt(),
                order.getCanceledAt());
    }
}
