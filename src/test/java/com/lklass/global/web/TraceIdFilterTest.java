package com.lklass.global.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.lklass.global.exception.TraceContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class TraceIdFilterTest {

    private final TraceIdFilter traceIdFilter = new TraceIdFilter();

    @Test
    @DisplayName("요청에 X-Trace-Id가 있으면 같은 값을 MDC와 응답 헤더에 사용한다")
    void useRequestTraceIdWhenHeaderExists() throws Exception {
        // given
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicReference<String> traceIdInChain = new AtomicReference<>();
        request.addHeader(TraceContext.TRACE_ID_HEADER, "trace-from-client");
        FilterChain filterChain = (servletRequest, servletResponse) ->
                traceIdInChain.set(MDC.get(TraceContext.TRACE_ID_KEY));

        // when
        traceIdFilter.doFilter(request, response, filterChain);

        // then
        assertThat(traceIdInChain.get()).isEqualTo("trace-from-client");
        assertThat(response.getHeader(TraceContext.TRACE_ID_HEADER)).isEqualTo("trace-from-client");
        assertThat(MDC.get(TraceContext.TRACE_ID_KEY)).isNull();
    }

    @Test
    @DisplayName("요청에 X-Trace-Id가 없으면 새 traceId를 생성해 MDC와 응답 헤더에 사용한다")
    void createTraceIdWhenHeaderDoesNotExist() throws Exception {
        // given
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicReference<String> traceIdInChain = new AtomicReference<>();
        FilterChain filterChain = (servletRequest, servletResponse) ->
                traceIdInChain.set(MDC.get(TraceContext.TRACE_ID_KEY));

        // when
        traceIdFilter.doFilter(request, response, filterChain);

        // then
        assertThat(traceIdInChain.get()).isNotBlank();
        assertThat(response.getHeader(TraceContext.TRACE_ID_HEADER)).isEqualTo(traceIdInChain.get());
        assertThat(MDC.get(TraceContext.TRACE_ID_KEY)).isNull();
    }

    @Test
    @DisplayName("filter chain에서 예외가 발생해도 MDC의 traceId를 제거한다")
    void clearMdcWhenFilterChainThrowsException() {
        // given
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        request.addHeader(TraceContext.TRACE_ID_HEADER, "trace-before-error");
        FilterChain filterChain = (servletRequest, servletResponse) -> {
            throw new ServletException("chain failed");
        };

        // when & then
        assertThatThrownBy(() -> traceIdFilter.doFilter(request, response, filterChain))
                .isInstanceOf(ServletException.class)
                .hasMessage("chain failed");
        assertThat(response.getHeader(TraceContext.TRACE_ID_HEADER)).isEqualTo("trace-before-error");
        assertThat(MDC.get(TraceContext.TRACE_ID_KEY)).isNull();
    }
}
