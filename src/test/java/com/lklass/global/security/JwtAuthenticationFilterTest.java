package com.lklass.global.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.lklass.domain.auth.exception.AuthErrorCode;
import com.lklass.domain.user.entity.UserRole;
import com.lklass.global.exception.BusinessException;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.servlet.HandlerExceptionResolver;

class JwtAuthenticationFilterTest {

    private final JwtTokenProvider jwtTokenProvider = mock(JwtTokenProvider.class);
    private final HandlerExceptionResolver handlerExceptionResolver = mock(HandlerExceptionResolver.class);
    private final JwtAuthenticationFilter filter =
            new JwtAuthenticationFilter(jwtTokenProvider, handlerExceptionResolver);

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Bearer Access Token이 유효하면 SecurityContext에 인증 사용자와 권한을 저장한다")
    void authenticateWithValidBearerToken() throws Exception {
        // given
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer valid-token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain filterChain = mock(FilterChain.class);
        when(jwtTokenProvider.getUserId("valid-token")).thenReturn(1L);
        when(jwtTokenProvider.getRole("valid-token")).thenReturn(UserRole.CREATOR);

        // when
        filter.doFilter(request, response, filterChain);

        // then
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertThat(authentication).isNotNull();
        assertThat(authentication.getPrincipal()).isEqualTo(new AuthenticatedUser(1L, UserRole.CREATOR));
        assertThat(authentication.getAuthorities())
                .extracting("authority")
                .containsExactly("ROLE_CREATOR");
        verify(filterChain).doFilter(request, response);
        verify(handlerExceptionResolver, never()).resolveException(any(), any(), isNull(), any());
    }

    @Test
    @DisplayName("Authorization 헤더가 없으면 인증을 만들지 않고 다음 필터로 진행한다")
    void continueWithoutAuthorizationHeader() throws Exception {
        // given
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain filterChain = mock(FilterChain.class);

        // when
        filter.doFilter(request, response, filterChain);

        // then
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(request, response);
        verify(jwtTokenProvider, never()).getUserId(any());
        verify(handlerExceptionResolver, never()).resolveException(any(), any(), isNull(), any());
    }

    @Test
    @DisplayName("Authorization 헤더가 Bearer 형식이 아니면 인증을 만들지 않고 다음 필터로 진행한다")
    void continueWithNonBearerAuthorizationHeader() throws Exception {
        // given
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Basic encoded-credential");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain filterChain = mock(FilterChain.class);

        // when
        filter.doFilter(request, response, filterChain);

        // then
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(request, response);
        verify(jwtTokenProvider, never()).getUserId(any());
        verify(handlerExceptionResolver, never()).resolveException(any(), any(), isNull(), any());
    }

    @Test
    @DisplayName("Access Token이 잘못되면 SecurityContext를 비우고 GlobalExceptionHandler로 처리를 위임한다")
    void delegateInvalidTokenException() throws Exception {
        // given
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer invalid-token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain filterChain = mock(FilterChain.class);
        BusinessException exception = new BusinessException(AuthErrorCode.INVALID_TOKEN);
        when(jwtTokenProvider.getUserId("invalid-token")).thenThrow(exception);

        // when
        filter.doFilter(request, response, filterChain);

        // then
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain, never()).doFilter(request, response);
        verify(handlerExceptionResolver).resolveException(request, response, null, exception);
    }
}
