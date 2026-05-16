package com.lklass.domain.enrollment.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.lklass.domain.course.exception.CourseErrorCode;
import com.lklass.domain.enrollment.dto.EnrollmentApplyResult;
import com.lklass.domain.enrollment.entity.EnrollmentStatus;
import com.lklass.domain.enrollment.exception.EnrollmentErrorCode;
import com.lklass.domain.enrollment.service.EnrollmentService;
import com.lklass.domain.user.entity.UserRole;
import com.lklass.domain.user.exception.UserErrorCode;
import com.lklass.global.config.SecurityConfig;
import com.lklass.global.exception.BusinessException;
import com.lklass.global.exception.GlobalErrorCode;
import com.lklass.global.security.AuthenticatedUser;
import com.lklass.global.security.JwtAuthenticationFilter;
import com.lklass.global.security.JwtTokenProvider;
import com.lklass.global.security.RestAccessDeniedHandler;
import com.lklass.global.security.RestAuthenticationEntryPoint;
import com.lklass.global.security.SecurityErrorResponder;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.servlet.HandlerExceptionResolver;

@Import({
        SecurityConfig.class,
        SecurityErrorResponder.class,
        RestAuthenticationEntryPoint.class,
        RestAccessDeniedHandler.class,
        EnrollmentControllerTest.TestSecurityFilterConfig.class
})
@WebMvcTest(controllers = EnrollmentController.class)
class EnrollmentControllerTest {

    private static final LocalDateTime ENROLLED_AT = LocalDateTime.of(2026, 5, 16, 10, 0);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private EnrollmentService enrollmentService;

    @Test
    @DisplayName("인증된 STUDENT가 Course 수강 신청 API를 호출하면 PENDING 신청 응답을 반환한다")
    void applyEnrollment() throws Exception {
        // given
        mockToken("student-token", 3L, UserRole.STUDENT);
        when(enrollmentService.apply(any(AuthenticatedUser.class), eq(100L)))
                .thenReturn(result(1L, 100L, 3L));

        // when & then
        mockMvc.perform(post("/api/courses/100/enrollments")
                        .header("Authorization", "Bearer student-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1L))
                .andExpect(jsonPath("$.data.courseId").value(100L))
                .andExpect(jsonPath("$.data.userId").value(3L))
                .andExpect(jsonPath("$.data.status").value(EnrollmentStatus.PENDING.name()))
                .andExpect(jsonPath("$.data.enrolledAt").value("2026-05-16T10:00:00"));
        verify(enrollmentService).apply(any(AuthenticatedUser.class), eq(100L));
    }

    @Test
    @DisplayName("수강 신청 API는 인증이 없으면 401 공통 실패 응답을 반환한다")
    void rejectUnauthenticatedEnrollmentApply() throws Exception {
        // when & then
        mockMvc.perform(post("/api/courses/100/enrollments"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value(GlobalErrorCode.UNAUTHORIZED.code()));
        verify(enrollmentService, never()).apply(any(), any());
    }

    @Test
    @DisplayName("수강 신청 API는 STUDENT 권한이 아니면 403 공통 실패 응답을 반환한다")
    void rejectForbiddenEnrollmentApply() throws Exception {
        // given
        mockToken("creator-token", 1L, UserRole.CREATOR);
        doThrow(new AccessDeniedException("Access Denied"))
                .when(enrollmentService)
                .apply(any(AuthenticatedUser.class), eq(100L));

        // when & then
        mockMvc.perform(post("/api/courses/100/enrollments")
                        .header("Authorization", "Bearer creator-token"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value(GlobalErrorCode.FORBIDDEN.code()));
    }

    @Test
    @DisplayName("수강 신청 API는 이미 활성 신청이 있으면 409 공통 실패 응답을 반환한다")
    void rejectAlreadyEnrolled() throws Exception {
        // given
        mockToken("student-token", 3L, UserRole.STUDENT);
        doThrow(new BusinessException(EnrollmentErrorCode.ALREADY_ENROLLED))
                .when(enrollmentService)
                .apply(any(AuthenticatedUser.class), eq(100L));

        // when & then
        mockMvc.perform(post("/api/courses/100/enrollments")
                        .header("Authorization", "Bearer student-token"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value(EnrollmentErrorCode.ALREADY_ENROLLED.code()));
    }

    @Test
    @DisplayName("수강 신청 API는 Course 정원이 초과되면 409 공통 실패 응답을 반환한다")
    void rejectCapacityExceeded() throws Exception {
        // given
        mockToken("student-token", 3L, UserRole.STUDENT);
        doThrow(new BusinessException(EnrollmentErrorCode.CAPACITY_EXCEEDED))
                .when(enrollmentService)
                .apply(any(AuthenticatedUser.class), eq(100L));

        // when & then
        mockMvc.perform(post("/api/courses/100/enrollments")
                        .header("Authorization", "Bearer student-token"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value(EnrollmentErrorCode.CAPACITY_EXCEEDED.code()));
    }

    @Test
    @DisplayName("수강 신청 API는 신청 가능한 Course가 아니면 400 공통 실패 응답을 반환한다")
    void rejectUnavailableEnrollment() throws Exception {
        // given
        mockToken("student-token", 3L, UserRole.STUDENT);
        doThrow(new BusinessException(EnrollmentErrorCode.ENROLLMENT_NOT_AVAILABLE))
                .when(enrollmentService)
                .apply(any(AuthenticatedUser.class), eq(100L));

        // when & then
        mockMvc.perform(post("/api/courses/100/enrollments")
                        .header("Authorization", "Bearer student-token"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value(EnrollmentErrorCode.ENROLLMENT_NOT_AVAILABLE.code()));
    }

    @Test
    @DisplayName("수강 신청 API는 Course가 없으면 404 공통 실패 응답을 반환한다")
    void rejectUnknownCourse() throws Exception {
        // given
        mockToken("student-token", 3L, UserRole.STUDENT);
        doThrow(new BusinessException(CourseErrorCode.COURSE_NOT_FOUND))
                .when(enrollmentService)
                .apply(any(AuthenticatedUser.class), eq(999_999L));

        // when & then
        mockMvc.perform(post("/api/courses/999999/enrollments")
                        .header("Authorization", "Bearer student-token"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value(CourseErrorCode.COURSE_NOT_FOUND.code()));
    }

    @Test
    @DisplayName("수강 신청 API는 사용자가 없으면 404 공통 실패 응답을 반환한다")
    void rejectUnknownUser() throws Exception {
        // given
        mockToken("student-token", 999_999L, UserRole.STUDENT);
        doThrow(new BusinessException(UserErrorCode.USER_NOT_FOUND))
                .when(enrollmentService)
                .apply(any(AuthenticatedUser.class), eq(100L));

        // when & then
        mockMvc.perform(post("/api/courses/100/enrollments")
                        .header("Authorization", "Bearer student-token"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value(UserErrorCode.USER_NOT_FOUND.code()));
    }

    @Test
    @DisplayName("수강 신청 API는 courseId가 숫자가 아니면 400 validation 응답을 반환하고 서비스를 호출하지 않는다")
    void rejectInvalidCourseId() throws Exception {
        // given
        mockToken("student-token", 3L, UserRole.STUDENT);

        // when & then
        mockMvc.perform(post("/api/courses/not-a-number/enrollments")
                        .header("Authorization", "Bearer student-token"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value(GlobalErrorCode.VALIDATION_ERROR.code()));
        verify(enrollmentService, never()).apply(any(), any());
    }

    private void mockToken(String token, Long userId, UserRole role) {
        when(jwtTokenProvider.getUserId(token)).thenReturn(userId);
        when(jwtTokenProvider.getRole(token)).thenReturn(role);
    }

    private EnrollmentApplyResult result(Long enrollmentId, Long courseId, Long userId) {
        return new EnrollmentApplyResult(
                enrollmentId,
                courseId,
                userId,
                EnrollmentStatus.PENDING,
                ENROLLED_AT
        );
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class TestSecurityFilterConfig {

        @Bean
        JwtAuthenticationFilter jwtAuthenticationFilter(
                JwtTokenProvider jwtTokenProvider,
                HandlerExceptionResolver handlerExceptionResolver
        ) {
            return new JwtAuthenticationFilter(jwtTokenProvider, handlerExceptionResolver);
        }

        @Bean
        JwtTokenProvider jwtTokenProvider() {
            return org.mockito.Mockito.mock(JwtTokenProvider.class);
        }
    }
}
