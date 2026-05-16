package com.lklass.domain.user.exception;

import com.lklass.global.exception.ErrorCode;
import org.springframework.http.HttpStatus;

public enum UserErrorCode implements ErrorCode {
    DUPLICATED_EMAIL(HttpStatus.CONFLICT, "USER_DUPLICATED_EMAIL", "Email is already registered."),
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "User was not found."),
    USER_NOT_CREATOR(HttpStatus.BAD_REQUEST, "USER_NOT_CREATOR", "User is not a creator.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;

    UserErrorCode(HttpStatus httpStatus, String code, String message) {
        this.httpStatus = httpStatus;
        this.code = code;
        this.message = message;
    }

    @Override
    public String code() {
        return code;
    }

    @Override
    public String message() {
        return message;
    }

    @Override
    public HttpStatus httpStatus() {
        return httpStatus;
    }
}
