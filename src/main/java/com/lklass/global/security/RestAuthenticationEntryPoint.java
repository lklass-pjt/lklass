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
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private static final Logger log = LoggerFactory.getLogger(RestAuthenticationEntryPoint.class);

    private final SecurityErrorResponder securityErrorResponder;

    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authException
    ) throws IOException, ServletException {
        AppLog.debug(
                log,
                "AUTHENTICATION_REQUIRED",
                "method={}, uri={}, error={}",
                request.getMethod(),
                request.getRequestURI(),
                authException.getClass().getSimpleName()
        );
        securityErrorResponder.write(response, GlobalErrorCode.UNAUTHORIZED);
    }
}
