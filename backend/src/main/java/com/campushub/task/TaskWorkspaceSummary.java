package com.campushub.task;

import com.campushub.file.TaskDeliveryPhotoSummary;
import java.util.List;

public record TaskWorkspaceSummary(
        RewardTaskSummary task,
        TaskParticipantContact publisherContact,
        TaskParticipantContact runnerContact,
        List<TaskEventSummary> events,
        TaskPrivateDetails privateDetails,
        List<TaskDeliveryPhotoSummary> evidence,
        boolean canEdit,
        boolean canWithdraw) {
}
