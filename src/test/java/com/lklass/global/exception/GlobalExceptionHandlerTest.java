package com.lklass.global.exception;

import static org.assertj.core.api.Assertions.assertThat;

import com.lklass.domain.course.entity.CourseStatus;
import com.lklass.global.common.CommonResponse;
import java.lang.reflect.Method;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler exceptionHandler = new GlobalExceptionHandler();

    @AfterEach
    void tearDown() {
        MDC.clear();
    }

    @Test
    @DisplayName("BusinessException은 ErrorCode의 HTTP status와 공통 실패 응답으로 변환된다")
    void handleBusinessException() {
        // given
        MDC.put(TraceContext.TRACE_ID_KEY, "trace-business");
        BusinessException exception = new BusinessException(GlobalErrorCode.FORBIDDEN);

        // when
        ResponseEntity<CommonResponse<Void>> response = exceptionHandler.handleBusinessException(exception);

        // then
        assertThat(response.getStatusCode()).isEqualTo(GlobalErrorCode.FORBIDDEN.httpStatus());
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isSuccess()).isFalse();
        assertThat(response.getBody().code()).isEqualTo(GlobalErrorCode.FORBIDDEN.code());
        assertThat(response.getBody().message()).isEqualTo(GlobalErrorCode.FORBIDDEN.message());
        assertThat(response.getBody().traceId()).isEqualTo("trace-business");
    }

    @Test
    @DisplayName("예상하지 못한 예외는 내부 상세를 숨기고 INTERNAL_SERVER_ERROR 응답으로 변환된다")
    void handleUnexpectedException() {
        // given
        MDC.put(TraceContext.TRACE_ID_KEY, "trace-unexpected");
        RuntimeException exception = new RuntimeException("database password leaked");

        // when
        ResponseEntity<CommonResponse<Void>> response = exceptionHandler.handleException(exception);

        // then
        assertThat(response.getStatusCode()).isEqualTo(GlobalErrorCode.INTERNAL_SERVER_ERROR.httpStatus());
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isSuccess()).isFalse();
        assertThat(response.getBody().code()).isEqualTo(GlobalErrorCode.INTERNAL_SERVER_ERROR.code());
        assertThat(response.getBody().message()).isEqualTo(GlobalErrorCode.INTERNAL_SERVER_ERROR.message());
        assertThat(response.getBody().message()).doesNotContain("database password leaked");
        assertThat(response.getBody().traceId()).isEqualTo("trace-unexpected");
    }

    @Test
    @DisplayName("validation 실패는 첫 번째 field error를 VALIDATION_ERROR 공통 실패 응답으로 변환한다")
    void handleValidationException() throws Exception {
        // given
        MDC.put(TraceContext.TRACE_ID_KEY, "trace-validation");
        BindingResult bindingResult = new BeanPropertyBindingResult(new CreateRequest(null), "createRequest");
        bindingResult.addError(new FieldError("createRequest", "title", "must not be blank"));
        MethodParameter methodParameter = methodParameterForCreateRequest();
        MethodArgumentNotValidException exception = new MethodArgumentNotValidException(methodParameter, bindingResult);

        // when
        ResponseEntity<CommonResponse<Void>> response = exceptionHandler.handleValidationException(exception);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isSuccess()).isFalse();
        assertThat(response.getBody().code()).isEqualTo(GlobalErrorCode.VALIDATION_ERROR.code());
        assertThat(response.getBody().message()).isEqualTo("title: must not be blank");
        assertThat(response.getBody().traceId()).isEqualTo("trace-validation");
    }

    @Test
    @DisplayName("요청 파라미터 타입 변환 실패는 VALIDATION_ERROR 공통 실패 응답으로 변환된다")
    void handleTypeMismatch() throws Exception {
        // given
        MDC.put(TraceContext.TRACE_ID_KEY, "trace-type-mismatch");
        MethodArgumentTypeMismatchException exception = new MethodArgumentTypeMismatchException(
                "INVALID",
                CourseStatus.class,
                "status",
                methodParameterForStatusRequest(),
                new IllegalArgumentException("No enum constant")
        );

        // when
        ResponseEntity<CommonResponse<Void>> response = exceptionHandler.handleTypeMismatch(exception);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isSuccess()).isFalse();
        assertThat(response.getBody().code()).isEqualTo(GlobalErrorCode.VALIDATION_ERROR.code());
        assertThat(response.getBody().message()).isEqualTo("status: Invalid request value.");
        assertThat(response.getBody().traceId()).isEqualTo("trace-type-mismatch");
    }

    private MethodParameter methodParameterForCreateRequest() throws NoSuchMethodException {
        Method method = ValidationTestController.class.getDeclaredMethod("create", CreateRequest.class);
        return new MethodParameter(method, 0);
    }

    private MethodParameter methodParameterForStatusRequest() throws NoSuchMethodException {
        Method method = ValidationTestController.class.getDeclaredMethod("list", CourseStatus.class);
        return new MethodParameter(method, 0);
    }

    private record CreateRequest(String title) {
    }

    private static class ValidationTestController {

        @SuppressWarnings("unused")
        void create(@RequestBody CreateRequest request) {
        }

        @SuppressWarnings("unused")
        void list(@RequestParam CourseStatus status) {
        }
    }
}
