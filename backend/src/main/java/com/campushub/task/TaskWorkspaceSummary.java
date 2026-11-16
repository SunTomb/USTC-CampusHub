package com.campushub.task;

import java.util.List;

public record TaskWorkspaceSummary(
        RewardTaskSummary task,
        TaskParticipantContact publisherContact,
        TaskParticipantContact runnerContact,
        List<TaskEventSummary> events) {
}
