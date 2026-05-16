package com.lklass.global.exception;

import com.lklass.global.common.CommonResponse;
import com.lklass.global.logging.AppLog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<CommonResponse<Void>> handleBusinessException(BusinessException exception) {
        ErrorCode errorCode = exception.errorCode();
        String traceId = MDC.get(TraceContext.TRACE_ID_KEY);

        AppLog.businessException(log, errorCode, exception);

        return ResponseEntity
                .status(errorCode.httpStatus())
                .body(CommonResponse.fail(errorCode.code(), exception.getMessage(), traceId));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<CommonResponse<Void>> handleValidationException(MethodArgumentNotValidException exception) {
        ErrorCode errorCode = GlobalErrorCode.VALIDATION_ERROR;
        String traceId = MDC.get(TraceContext.TRACE_ID_KEY);
        String message = exception.getBindingResult()
                .getFieldErrors()
                .stream()
                .findFirst()
                .map(fieldError -> fieldError.getField() + ": " + fieldError.getDefaultMessage())
                .orElse(errorCode.message());

        AppLog.validationFailed(log, errorCode, exception.getBindingResult().getErrorCount(), message);

        return ResponseEntity
                .status(errorCode.httpStatus())
                .body(CommonResponse.fail(errorCode.code(), message, traceId));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<CommonResponse<Void>> handleMessageNotReadable(HttpMessageNotReadableException exception) {
        ErrorCode errorCode = GlobalErrorCode.VALIDATION_ERROR;
        String traceId = MDC.get(TraceContext.TRACE_ID_KEY);
        String message = "request body: 요청 본문을 읽을 수 없습니다.";

        AppLog.warn(log, errorCode.code(), exception.getMessage());

        return ResponseEntity
                .status(errorCode.httpStatus())
                .body(CommonResponse.fail(errorCode.code(), message, traceId));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<CommonResponse<Void>> handleAccessDenied(AccessDeniedException exception) {
        ErrorCode errorCode = GlobalErrorCode.FORBIDDEN;
        String traceId = MDC.get(TraceContext.TRACE_ID_KEY);

        AppLog.warn(log, errorCode.code(), exception.getMessage());

        return ResponseEntity
                .status(errorCode.httpStatus())
                .body(CommonResponse.fail(errorCode.code(), errorCode.message(), traceId));
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<CommonResponse<Void>> handleMethodNotAllowed(
            HttpRequestMethodNotSupportedException exception
    ) {
        ErrorCode errorCode = GlobalErrorCode.METHOD_NOT_ALLOWED;
        String traceId = MDC.get(TraceContext.TRACE_ID_KEY);

        AppLog.warn(log, errorCode.code(), "method=" + exception.getMethod());

        return ResponseEntity
                .status(errorCode.httpStatus())
                .body(CommonResponse.fail(errorCode.code(), errorCode.message(), traceId));
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<CommonResponse<Void>> handleNoResourceFound(NoResourceFoundException exception) {
        ErrorCode errorCode = GlobalErrorCode.RESOURCE_NOT_FOUND;
        String traceId = MDC.get(TraceContext.TRACE_ID_KEY);

        AppLog.warn(log, errorCode.code(), "resourcePath=" + exception.getResourcePath());

        return ResponseEntity
                .status(errorCode.httpStatus())
                .body(CommonResponse.fail(errorCode.code(), errorCode.message(), traceId));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<CommonResponse<Void>> handleException(Exception exception) {
        ErrorCode errorCode = GlobalErrorCode.INTERNAL_SERVER_ERROR;
        String traceId = MDC.get(TraceContext.TRACE_ID_KEY);

        AppLog.unhandledException(log, errorCode, exception);

        // 예상하지 못한 예외의 내부 상세는 클라이언트에 노출하지 않는다.
        return ResponseEntity
                .status(errorCode.httpStatus())
                .body(CommonResponse.fail(errorCode.code(), errorCode.message(), traceId));
    }
}
