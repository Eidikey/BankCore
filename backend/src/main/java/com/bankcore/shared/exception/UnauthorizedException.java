package com.bankcore.shared.exception;

import com.bankcore.shared.error.ErrorCode;
import org.springframework.http.HttpStatus;

public class UnauthorizedException extends BusinessException {
    public UnauthorizedException(ErrorCode code) {
        super(code, HttpStatus.UNAUTHORIZED);
    }

    public UnauthorizedException(ErrorCode code, String message) {
        super(code, HttpStatus.UNAUTHORIZED, message);
    }
}