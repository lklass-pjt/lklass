package com.lklass.global.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record CommonResponse<T>(
        // Java record 내부 이름은 isSuccess지만 API 응답은 success로 고정한다.
        @JsonProperty("success")
        boolean isSuccess,
        T data,
        String code,
        String message,
        String traceId
) {

    // 성공 응답은 null 에러 필드를 숨겨 API payload를 작게 유지한다.
    public static <T> CommonResponse<T> success(T data) {
        return new CommonResponse<>(true, data, null, null, null);
    }

    public static CommonResponse<Void> success() {
        return new CommonResponse<>(true, null, null, null, null);
    }

    // traceId는 실패 응답과 서버 로그를 연결하기 위한 운영용 단서다.
    public static <T> CommonResponse<T> fail(String code, String message, String traceId) {
        return new CommonResponse<>(false, null, code, message, traceId);
    }
}
