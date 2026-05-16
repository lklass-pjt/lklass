package com.lklass.domain.enrollment.exception;

import com.lklass.global.exception.ErrorCode;
import org.springframework.http.HttpStatus;

public enum EnrollmentErrorCode implements ErrorCode {

    ALREADY_ENROLLED(HttpStatus.CONFLICT, "ALREADY_ENROLLED", "User already has an active enrollment."),
    CAPACITY_EXCEEDED(HttpStatus.CONFLICT, "CAPACITY_EXCEEDED", "Course capacity has been exceeded."),
    ENROLLMENT_NOT_AVAILABLE(HttpStatus.BAD_REQUEST, "ENROLLMENT_NOT_AVAILABLE", "Course is not available for enrollment."),
    ENROLLMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "ENROLLMENT_NOT_FOUND", "Enrollment was not found."),
    INVALID_ENROLLMENT_STATUS(HttpStatus.BAD_REQUEST, "INVALID_ENROLLMENT_STATUS", "Enrollment status transition is invalid."),
    CANCELLATION_PERIOD_EXPIRED(
            HttpStatus.BAD_REQUEST,
            "CANCELLATION_PERIOD_EXPIRED",
            "Enrollment cancellation period has expired."
    );

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
