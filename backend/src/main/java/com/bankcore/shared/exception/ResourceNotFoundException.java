package com.bankcore.shared.exception;

import com.bankcore.shared.error.ErrorCode;
import org.springframework.http.HttpStatus;

public class ResourceNotFoundException extends BusinessException {
    public ResourceNotFoundException(ErrorCode code) {
        super(code, HttpStatus.NOT_FOUND);
    }

    public ResourceNotFoundException(ErrorCode code, String message) {
        super(code, HttpStatus.NOT_FOUND, message);
    }
}