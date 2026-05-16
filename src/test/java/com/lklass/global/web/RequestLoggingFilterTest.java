package com.lklass.global.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class RequestLoggingFilterTest {

    private final RequestLoggingFilter requestLoggingFilter = new RequestLoggingFilter();
    private final Logger logger = (Logger) LoggerFactory.getLogger(RequestLoggingFilter.class);
    private ListAppender<ILoggingEvent> listAppender;

    @BeforeEach
    void setUp() {
        listAppender = new ListAppender<>();
        listAppender.start();
        logger.addAppender(listAppender);
    }

    @AfterEach
    void tearDown() {
        logger.detachAppender(listAppender);
        listAppender.stop();
    }

    @Test
    @DisplayName("RequestLoggingFilter는 요청 처리가 성공하면 method, uri, status, elapsedMs를 INFO 로그로 남긴다")
    void logHttpRequestWhenFilterChainSucceeds() throws Exception {
        // given
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/courses");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain filterChain = (servletRequest, servletResponse) ->
                ((MockHttpServletResponse) servletResponse).setStatus(201);

        // when
        requestLoggingFilter.doFilter(request, response, filterChain);

        // then
        assertThat(listAppender.list)
                .extracting(ILoggingEvent::getFormattedMessage)
                .anySatisfy(message -> {
                    assertThat(message).contains("[HTTP_REQUEST]");
                    assertThat(message).contains("method=POST");
                    assertThat(message).contains("uri=/api/courses");
                    assertThat(message).contains("status=201");
                    assertThat(message).contains("elapsedMs=");
                });
    }

    @Test
    @DisplayName("RequestLoggingFilter는 요청 처리 중 예외가 발생하면 실패 로그를 남기고 예외를 그대로 전파한다")
    void logHttpRequestFailureWhenFilterChainThrowsException() {
        // given
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/courses/1");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain filterChain = (servletRequest, servletResponse) -> {
            throw new ServletException("chain failed");
        };

        // when & then
        assertThatThrownBy(() -> requestLoggingFilter.doFilter(request, response, filterChain))
                .isInstanceOf(ServletException.class)
                .hasMessage("chain failed");
        assertThat(listAppender.list)
                .extracting(ILoggingEvent::getFormattedMessage)
                .anySatisfy(message -> {
                    assertThat(message).contains("[HTTP_REQUEST_FAILED]");
                    assertThat(message).contains("method=GET");
                    assertThat(message).contains("uri=/api/courses/1");
                    assertThat(message).contains("elapsedMs=");
                    assertThat(message).contains("error=ServletException");
                });
    }
}
