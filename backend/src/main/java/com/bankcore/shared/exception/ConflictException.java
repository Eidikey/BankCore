package com.bankcore.shared.exception;

import com.bankcore.shared.error.ErrorCode;
import org.springframework.http.HttpStatus;

public class ConflictException extends BusinessException {
    public ConflictException(ErrorCode code) {
        super(code, HttpStatus.CONFLICT);
    }

    public ConflictException(ErrorCode code, String message) {
        super(code, HttpStatus.CONFLICT, message);
    }
}