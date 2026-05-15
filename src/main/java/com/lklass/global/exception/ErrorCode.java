package com.lklass.global.exception;

import org.springframework.http.HttpStatus;

public interface ErrorCode {

    String code();

    String message();

    HttpStatus httpStatus();
}
