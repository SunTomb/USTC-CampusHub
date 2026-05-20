package com.campushub.task;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record RewardTaskSummary(
        Long id,
        String title,
        String description,
        BigDecimal rewardAmount,
        BigDecimal depositAmount,
        String taskLocation,
        LocalDateTime deadline,
        String status,
        String acceptanceMode,
        String originZone,
        String destinationZone,
        String originDetail,
        String destinationDetail,
        String workflowStatus,
        String verificationMode,
        Long acceptedApplicationId,
        Long publisherId,
        String publisherNickname) {

    public static RewardTaskSummary from(RewardTask task) {
        return new RewardTaskSummary(
                task.getId(),
                task.getTitle(),
                task.getDescription(),
                task.getRewardAmount(),
                task.getDepositAmount(),
                task.getTaskLocation(),
                task.getDeadline(),
                task.getStatus(),
                task.getAcceptanceMode(),
                task.getOriginZone(),
                task.getDestinationZone(),
                task.getOriginDetail(),
                task.getDestinationDetail(),
                task.getWorkflowStatus(),
                task.getVerificationMode(),
                task.getAcceptedApplication() == null ? null : task.getAcceptedApplication().getId(),
                task.getPublisher().getId(),
                task.getPublisher().getNickname());
    }
}
