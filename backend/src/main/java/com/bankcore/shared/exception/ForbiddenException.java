package com.bankcore.shared.exception;

import com.bankcore.shared.error.ErrorCode;
import org.springframework.http.HttpStatus;

public class ForbiddenException extends BusinessException {
    public ForbiddenException(ErrorCode code) {
        super(code, HttpStatus.FORBIDDEN);
    }

    public ForbiddenException(ErrorCode code, String message) {
        super(code, HttpStatus.FORBIDDEN, message);
    }
}