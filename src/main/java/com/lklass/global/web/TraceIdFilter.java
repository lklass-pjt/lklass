package com.lklass.global.web;

import com.lklass.global.exception.TraceContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Order(Ordered.HIGHEST_PRECEDENCE)
@Component
public class TraceIdFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        // 클라이언트가 전달한 traceId가 있으면 이어받고, 없으면 서버에서 새로 만든다.
        String traceId = Optional.ofNullable(request.getHeader(TraceContext.TRACE_ID_HEADER))
                .filter(headerValue -> !headerValue.isBlank())
                .orElseGet(() -> UUID.randomUUID().toString());

        MDC.put(TraceContext.TRACE_ID_KEY, traceId);
        response.setHeader(TraceContext.TRACE_ID_HEADER, traceId);

        try {
            filterChain.doFilter(request, response);
        } finally {
            // WAS thread 재사용 시 다음 요청에 traceId가 섞이지 않도록 반드시 제거한다.
            MDC.remove(TraceContext.TRACE_ID_KEY);
        }
    }
}
