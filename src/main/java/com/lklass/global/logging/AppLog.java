package com.lklass.global.logging;

import com.lklass.global.exception.ErrorCode;
import org.slf4j.Logger;

public final class AppLog {

    private AppLog() {
    }

    // 공통 로그 포맷을 모아두어 예외 처리 코드가 로깅 문구에 끌려다니지 않게 한다.
    public static void businessException(Logger log, ErrorCode errorCode, Exception exception) {
        log.warn(
                "[Business Exception] code={}, status={}, message={}",
                errorCode.code(),
                errorCode.httpStatus().value(),
                exception.getMessage(),
                exception
        );
    }

    public static void validationFailed(Logger log, ErrorCode errorCode, int errorCount, String message) {
        log.warn("[Validation Failed] code={}, errorCount={}, message={}", errorCode.code(), errorCount, message);
    }

    public static void unhandledException(Logger log, ErrorCode errorCode, Exception exception) {
        log.error("[Unhandled Exception] code={}", errorCode.code(), exception);
    }

    public static void debug(Logger log, String eventCode, String message, Object... args) {
        log.debug("[{}] " + message, prependEventCode(eventCode, args));
    }

    public static void info(Logger log, String eventCode, String message, Object... args) {
        log.info("[{}] " + message, prependEventCode(eventCode, args));
    }

    public static void warn(Logger log, String eventCode, String message, Object... args) {
        log.warn("[{}] " + message, prependEventCode(eventCode, args));
    }

    public static void error(Logger log, String eventCode, String message, Throwable throwable) {
        log.error("[{}] {}", eventCode, message, throwable);
    }

    private static Object[] prependEventCode(String eventCode, Object[] args) {
        Object[] arguments = new Object[args.length + 1];
        arguments[0] = eventCode;
        System.arraycopy(args, 0, arguments, 1, args.length);
        return arguments;
    }
}
