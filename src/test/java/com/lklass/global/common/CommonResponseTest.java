package com.lklass.global.common;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

class CommonResponseTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("성공 응답은 success=true와 data만 포함하고 에러 필드는 포함하지 않는다")
    void successResponseContainsDataOnly() throws Exception {
        // given
        CommonResponse<String> response = CommonResponse.success("ok");

        // when
        String json = objectMapper.writeValueAsString(response);

        // then
        assertThat(response.isSuccess()).isTrue();
        assertThat(response.data()).isEqualTo("ok");
        assertThat(response.code()).isNull();
        assertThat(json).contains("\"success\":true");
        assertThat(json).contains("\"data\":\"ok\"");
        assertThat(json).doesNotContain("traceId");
    }

    @Test
    @DisplayName("실패 응답은 success=false와 에러 코드, 메시지, traceId를 포함한다")
    void failResponseContainsErrorAndTraceId() throws Exception {
        // given
        CommonResponse<Void> response = CommonResponse.fail("ERROR_CODE", "message", "trace-1");

        // when
        String json = objectMapper.writeValueAsString(response);

        // then
        assertThat(response.isSuccess()).isFalse();
        assertThat(response.data()).isNull();
        assertThat(response.code()).isEqualTo("ERROR_CODE");
        assertThat(response.traceId()).isEqualTo("trace-1");
        assertThat(json).contains("\"success\":false");
        assertThat(json).contains("\"code\":\"ERROR_CODE\"");
        assertThat(json).contains("\"traceId\":\"trace-1\"");
        assertThat(json).doesNotContain("data");
    }
}
