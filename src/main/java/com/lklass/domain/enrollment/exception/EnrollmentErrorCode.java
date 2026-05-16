package com.lklass.domain.enrollment.exception;

import com.lklass.global.exception.ErrorCode;
import org.springframework.http.HttpStatus;

public enum EnrollmentErrorCode implements ErrorCode {

    ALREADY_ENROLLED(HttpStatus.CONFLICT, "ALREADY_ENROLLED", "User already has an active enrollment."),
    CAPACITY_EXCEEDED(HttpStatus.CONFLICT, "CAPACITY_EXCEEDED", "Course capacity has been exceeded."),
    ENROLLMENT_NOT_AVAILABLE(HttpStatus.BAD_REQUEST, "ENROLLMENT_NOT_AVAILABLE", "Course is not available for enrollment.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;

    EnrollmentErrorCode(HttpStatus httpStatus, String code, String message) {
        this.httpStatus = httpStatus;
        this.code = code;
        this.message = message;
    }

    @Override
    public HttpStatus httpStatus() {
        return httpStatus;
    }

    @Override
    public String code() {
        return code;
    }

    @Override
    public String message() {
        return message;
    }
}
