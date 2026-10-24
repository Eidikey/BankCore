package com.bankcore.shared.exception;

import com.bankcore.shared.error.ErrorCode;
import org.springframework.http.HttpStatus;

public class BusinessException extends RuntimeException {
    private final ErrorCode code;
    private final HttpStatus httpStatus;

    public BusinessException(ErrorCode code, HttpStatus httpStatus) {
        super(code.getDefaultMessage());
        this.code = code;
        this.httpStatus = httpStatus;
    }

    public BusinessException(ErrorCode code, HttpStatus httpStatus, String message) {
        super(message);
        this.code = code;
        this.httpStatus = httpStatus;
    }

    public ErrorCode getCode() {
        return code;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }

    public int getStatusValue() {
        return httpStatus.value();
    }
}