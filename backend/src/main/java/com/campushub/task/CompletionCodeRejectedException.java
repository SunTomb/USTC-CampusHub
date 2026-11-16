package com.campushub.task;

import com.campushub.common.BusinessException;

public class CompletionCodeRejectedException extends BusinessException {

    public CompletionCodeRejectedException(String message) {
        super(message);
    }
}
