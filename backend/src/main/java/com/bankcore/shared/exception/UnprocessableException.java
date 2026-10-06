package com.bankcore.shared.exception;

import com.bankcore.shared.error.ErrorCode;
import org.springframework.http.HttpStatus;

public class UnprocessableException extends BusinessException {
    public UnprocessableException(ErrorCode code) {
        super(code, HttpStatus.UNPROCESSABLE_ENTITY);
    }

    public UnprocessableException(ErrorCode code, String message) {
        super(code, HttpStatus.UNPROCESSABLE_ENTITY, message);
    }
}