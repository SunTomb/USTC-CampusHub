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
                task.getPublisher().getNickname());
    }
}
