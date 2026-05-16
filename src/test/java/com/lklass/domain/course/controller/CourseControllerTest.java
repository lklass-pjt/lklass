package com.lklass.domain.course.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.lklass.domain.course.dto.CourseCreateRequest;
import com.lklass.domain.course.dto.CourseCreateResult;
import com.lklass.domain.course.entity.CourseStatus;
import com.lklass.domain.course.service.CourseService;
import com.lklass.domain.user.entity.UserRole;
import com.lklass.global.config.SecurityConfig;
import com.lklass.global.exception.GlobalErrorCode;
import com.lklass.global.security.AuthenticatedUser;
import com.lklass.global.security.JwtAuthenticationFilter;
import com.lklass.global.security.JwtTokenProvider;
import com.lklass.global.security.RestAccessDeniedHandler;
import com.lklass.global.security.RestAuthenticationEntryPoint;
import com.lklass.global.security.SecurityErrorResponder;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.servlet.HandlerExceptionResolver;
import tools.jackson.databind.ObjectMapper;

@Import({
        SecurityConfig.class,
        SecurityErrorResponder.class,
        RestAuthenticationEntryPoint.class,
        RestAccessDeniedHandler.class,
        CourseControllerTest.TestSecurityFilterConfig.class
})
@WebMvcTest(controllers = CourseController.class)
class CourseControllerTest {

    private static final LocalDateTime ENROLLMENT_START_AT = LocalDateTime.of(2026, 5, 20, 10, 0);
    private static final LocalDateTime ENROLLMENT_END_AT = LocalDateTime.of(2026, 5, 27, 18, 0);
    private static final LocalDateTime COURSE_START_AT = LocalDateTime.of(2026, 6, 1, 9, 0);
    private static final LocalDateTime COURSE_END_AT = LocalDateTime.of(2026, 6, 30, 18, 0);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private CourseService courseService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("인증된 CREATOR가 정상 요청하면 Course 생성 API가 성공 응답을 반환한다")
    void createCourse() throws Exception {
        // given
        mockToken("creator-token", 1L, UserRole.CREATOR);
        when(courseService.createCourse(
                any(AuthenticatedUser.class),
                isNull(),
                eq("스프링 입문"),
                eq("스프링 부트와 JPA 기초 강의"),
                eq(new BigDecimal("10000")),
                eq(30),
                eq(ENROLLMENT_START_AT),
                eq(ENROLLMENT_END_AT),
                eq(COURSE_START_AT),
                eq(COURSE_END_AT)
        )).thenReturn(result(100L, 1L, "스프링 입문"));
        CourseCreateRequest request = request(null, "스프링 입문");

        // when & then
        mockMvc.perform(post("/api/courses")
                        .header("Authorization", "Bearer creator-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(100L))
                .andExpect(jsonPath("$.data.creatorId").value(1L))
                .andExpect(jsonPath("$.data.title").value("스프링 입문"))
                .andExpect(jsonPath("$.data.status").value(CourseStatus.DRAFT.name()));
    }

    @Test
    @DisplayName("Course 생성 API는 인증이 없으면 401 공통 실패 응답을 반환한다")
    void rejectUnauthenticatedCourseCreation() throws Exception {
        // given
        CourseCreateRequest request = request(null, "미인증 등록 강의");

        // when & then
        mockMvc.perform(post("/api/courses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value(GlobalErrorCode.UNAUTHORIZED.code()));
        verify(courseService, never()).createCourse(
                any(),
                any(),
                anyString(),
                anyString(),
                any(),
                anyInt(),
                any(),
                any(),
                any(),
                any()
        );
    }

    @Test
    @DisplayName("Course 생성 API는 권한이 없으면 403 공통 실패 응답을 반환한다")
    void rejectForbiddenCourseCreation() throws Exception {
        // given
        mockToken("student-token", 3L, UserRole.STUDENT);
        when(courseService.createCourse(
                any(AuthenticatedUser.class),
                isNull(),
                anyString(),
                anyString(),
                any(),
                anyInt(),
                any(),
                any(),
                any(),
                any()
        )).thenThrow(new AccessDeniedException("Access Denied"));
        CourseCreateRequest request = request(null, "학생 등록 강의");

        // when & then
        mockMvc.perform(post("/api/courses")
                        .header("Authorization", "Bearer student-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value(GlobalErrorCode.FORBIDDEN.code()));
    }

    @Test
    @DisplayName("Course 생성 API는 요청 값이 올바르지 않으면 400 validation 응답을 반환한다")
    void rejectInvalidCourseCreateRequest() throws Exception {
        // given
        mockToken("creator-token", 1L, UserRole.CREATOR);
        CourseCreateRequest request = request(null, "");

        // when & then
        mockMvc.perform(post("/api/courses")
                        .header("Authorization", "Bearer creator-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value(GlobalErrorCode.VALIDATION_ERROR.code()))
                .andExpect(jsonPath("$.message").value("title: title은 필수입니다."));
        verify(courseService, never()).createCourse(
                any(),
                any(),
                anyString(),
                anyString(),
                any(),
                anyInt(),
                any(),
                any(),
                any(),
                any()
        );
    }

    private void mockToken(String token, Long userId, UserRole role) {
        when(jwtTokenProvider.getUserId(token)).thenReturn(userId);
        when(jwtTokenProvider.getRole(token)).thenReturn(role);
    }

    private CourseCreateResult result(Long courseId, Long creatorId, String title) {
        return new CourseCreateResult(
                courseId,
                creatorId,
                title,
                "스프링 부트와 JPA 기초 강의",
                new BigDecimal("10000.00"),
                30,
                0,
                CourseStatus.DRAFT,
                false,
                ENROLLMENT_START_AT,
                ENROLLMENT_END_AT,
                COURSE_START_AT,
                COURSE_END_AT
        );
    }

    private CourseCreateRequest request(Long creatorId, String title) {
        return new CourseCreateRequest(
                creatorId,
                title,
                "스프링 부트와 JPA 기초 강의",
                new BigDecimal("10000"),
                30,
                ENROLLMENT_START_AT,
                ENROLLMENT_END_AT,
                COURSE_START_AT,
                COURSE_END_AT
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
