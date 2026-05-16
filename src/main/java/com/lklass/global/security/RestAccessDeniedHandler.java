package com.lklass.global.security;

import com.lklass.global.exception.GlobalErrorCode;
import com.lklass.global.logging.AppLog;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RestAccessDeniedHandler implements AccessDeniedHandler {

    private static final Logger log = LoggerFactory.getLogger(RestAccessDeniedHandler.class);

    private final SecurityErrorResponder securityErrorResponder;

    @Override
    public void handle(
            HttpServletRequest request,
            HttpServletResponse response,
            AccessDeniedException accessDeniedException
    ) throws IOException, ServletException {
        AppLog.warn(
                log,
                "ACCESS_DENIED",
                "method={}, uri={}, error={}",
                request.getMethod(),
                request.getRequestURI(),
                accessDeniedException.getClass().getSimpleName()
        );
        securityErrorResponder.write(response, GlobalErrorCode.FORBIDDEN);
    }
}
