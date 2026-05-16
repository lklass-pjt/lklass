package com.lklass.global.logging;

import static org.assertj.core.api.Assertions.assertThat;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.lklass.global.exception.BusinessException;
import com.lklass.global.exception.GlobalErrorCode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

class AppLogTest {

    private final Logger logger = (Logger) LoggerFactory.getLogger(AppLogTest.class);
    private ListAppender<ILoggingEvent> listAppender;

    @BeforeEach
    void setUp() {
        logger.setLevel(Level.DEBUG);
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
    @DisplayName("info는 이벤트 코드와 placeholder 인자를 공통 포맷으로 남긴다")
    void logInfoWithEventCodeAndArguments() {
        // when
        AppLog.info(logger, "COURSE_CREATED", "courseId={}, actorId={}", 1L, 2L);

        // then
        assertThat(singleEvent().getLevel()).isEqualTo(Level.INFO);
        assertThat(singleEvent().getFormattedMessage())
                .isEqualTo("[COURSE_CREATED] courseId=1, actorId=2");
    }

    @Test
    @DisplayName("debug는 이벤트 코드와 placeholder 인자를 공통 포맷으로 남긴다")
    void logDebugWithEventCodeAndArguments() {
        // when
        AppLog.debug(logger, "AUTHENTICATION_REQUIRED", "method={}, uri={}", "GET", "/api/me");

        // then
        assertThat(singleEvent().getLevel()).isEqualTo(Level.DEBUG);
        assertThat(singleEvent().getFormattedMessage())
                .isEqualTo("[AUTHENTICATION_REQUIRED] method=GET, uri=/api/me");
    }

    @Test
    @DisplayName("warn은 이벤트 코드와 placeholder 인자를 공통 포맷으로 남긴다")
    void logWarnWithEventCodeAndArguments() {
        // when
        AppLog.warn(logger, "ACCESS_DENIED", "method={}, uri={}, error={}", "POST", "/api/courses", "Denied");

        // then
        assertThat(singleEvent().getLevel()).isEqualTo(Level.WARN);
        assertThat(singleEvent().getFormattedMessage())
                .isEqualTo("[ACCESS_DENIED] method=POST, uri=/api/courses, error=Denied");
    }

    @Test
    @DisplayName("error는 이벤트 코드와 메시지, throwable을 함께 남긴다")
    void logErrorWithThrowable() {
        // given
        IllegalStateException exception = new IllegalStateException("boom");

        // when
        AppLog.error(logger, "COMMAND_FAILED", "unexpected failure", exception);

        // then
        ILoggingEvent event = singleEvent();
        assertThat(event.getLevel()).isEqualTo(Level.ERROR);
        assertThat(event.getFormattedMessage()).isEqualTo("[COMMAND_FAILED] unexpected failure");
        assertThat(event.getThrowableProxy()).isNotNull();
        assertThat(event.getThrowableProxy().getClassName()).isEqualTo(IllegalStateException.class.getName());
        assertThat(event.getThrowableProxy().getMessage()).isEqualTo("boom");
    }

    @Test
    @DisplayName("businessException은 errorCode와 HTTP status, 예외 메시지를 WARN 로그로 남긴다")
    void logBusinessException() {
        // given
        BusinessException exception = new BusinessException(GlobalErrorCode.FORBIDDEN);

        // when
        AppLog.businessException(logger, GlobalErrorCode.FORBIDDEN, exception);

        // then
        ILoggingEvent event = singleEvent();
        assertThat(event.getLevel()).isEqualTo(Level.WARN);
        assertThat(event.getFormattedMessage())
                .isEqualTo("[Business Exception] code=FORBIDDEN, status=403, message=Access is denied.");
        assertThat(event.getThrowableProxy()).isNotNull();
    }

    @Test
    @DisplayName("validationFailed는 errorCode와 에러 개수, 메시지를 WARN 로그로 남긴다")
    void logValidationFailed() {
        // when
        AppLog.validationFailed(logger, GlobalErrorCode.VALIDATION_ERROR, 2, "title: must not be blank");

        // then
        assertThat(singleEvent().getLevel()).isEqualTo(Level.WARN);
        assertThat(singleEvent().getFormattedMessage())
                .isEqualTo("[Validation Failed] code=VALIDATION_ERROR, errorCount=2, message=title: must not be blank");
    }

    @Test
    @DisplayName("unhandledException은 errorCode와 throwable을 ERROR 로그로 남긴다")
    void logUnhandledException() {
        // given
        RuntimeException exception = new RuntimeException("database error");

        // when
        AppLog.unhandledException(logger, GlobalErrorCode.INTERNAL_SERVER_ERROR, exception);

        // then
        ILoggingEvent event = singleEvent();
        assertThat(event.getLevel()).isEqualTo(Level.ERROR);
        assertThat(event.getFormattedMessage()).isEqualTo("[Unhandled Exception] code=INTERNAL_SERVER_ERROR");
        assertThat(event.getThrowableProxy()).isNotNull();
        assertThat(event.getThrowableProxy().getClassName()).isEqualTo(RuntimeException.class.getName());
        assertThat(event.getThrowableProxy().getMessage()).isEqualTo("database error");
    }

    private ILoggingEvent singleEvent() {
        assertThat(listAppender.list).hasSize(1);
        return listAppender.list.getFirst();
    }
}
