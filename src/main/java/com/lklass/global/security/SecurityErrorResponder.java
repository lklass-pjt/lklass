package com.lklass.global.security;

import com.lklass.global.common.CommonResponse;
import com.lklass.global.exception.ErrorCode;
import com.lklass.global.exception.TraceContext;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.slf4j.MDC;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Component
@RequiredArgsConstructor
public class SecurityErrorResponder {

    private final ObjectMapper objectMapper;

    public void write(HttpServletResponse response, ErrorCode errorCode) throws IOException {
        String traceId = MDC.get(TraceContext.TRACE_ID_KEY);
        CommonResponse<Void> body = CommonResponse.fail(errorCode.code(), errorCode.message(), traceId);

        response.setStatus(errorCode.httpStatus().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(objectMapper.writeValueAsString(body));
    }
}
