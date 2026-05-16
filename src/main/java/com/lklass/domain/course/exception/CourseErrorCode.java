package com.lklass.domain.course.exception;

import com.lklass.global.exception.ErrorCode;
import org.springframework.http.HttpStatus;

public enum CourseErrorCode implements ErrorCode {

    COURSE_NOT_FOUND(HttpStatus.NOT_FOUND, "COURSE_NOT_FOUND", "Course was not found."),
    ENROLLMENT_CLOSED(HttpStatus.BAD_REQUEST, "ENROLLMENT_CLOSED", "Course enrollment has already closed."),
    INVALID_ENROLLMENT_PERIOD(
            HttpStatus.BAD_REQUEST,
            "INVALID_ENROLLMENT_PERIOD",
            "Course enrollment period is invalid."
    ),
    INVALID_COURSE_STATUS_TRANSITION(
            HttpStatus.BAD_REQUEST,
            "INVALID_COURSE_STATUS_TRANSITION",
            "Course status transition is invalid."
    );

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;

    CourseErrorCode(HttpStatus httpStatus, String code, String message) {
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
