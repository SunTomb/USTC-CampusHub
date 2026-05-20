package com.campushub.task;

import jakarta.validation.constraints.NotBlank;

public record ReportTaskIssueRequest(@NotBlank String issueType, @NotBlank String description) {
}
