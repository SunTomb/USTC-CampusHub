package com.campushub.task;

import java.time.LocalDateTime;

public record CompletionCodeResponse(String completionCode, LocalDateTime expiresAt) {
}
