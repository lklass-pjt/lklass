package com.lklass.domain.course.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.lklass.domain.course.dto.CourseCreateRequest;
import com.lklass.domain.course.dto.CourseCreateResult;
import com.lklass.domain.course.dto.CourseQueryResult;
import com.lklass.domain.course.entity.CourseStatus;
import com.lklass.domain.course.exception.CourseErrorCode;
import com.lklass.domain.course.service.CourseService;
import com.lklass.domain.user.entity.UserRole;
import com.lklass.global.config.PageableConfig;
import com.lklass.global.config.SecurityConfig;
import com.lklass.global.exception.GlobalErrorCode;
import com.lklass.global.exception.BusinessException;
import com.lklass.global.security.AuthenticatedUser;
import com.lklass.global.security.JwtAuthenticationFilter;
import com.lklass.global.security.JwtTokenProvider;
import com.lklass.global.security.RestAccessDeniedHandler;
import com.lklass.global.security.RestAuthenticationEntryPoint;
import com.lklass.global.security.SecurityErrorResponder;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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
        PageableConfig.class,
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

    @Test
    @DisplayName("Course 목록 조회 API는 기본 페이지 정책과 강사 이름을 포함한 페이지 응답을 반환한다")
    void getCourses() throws Exception {
        // given
        mockToken("creator-token", 1L, UserRole.CREATOR);
        PageRequest servicePage = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt"));
        when(courseService.getCourses(isNull(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(
                        List.of(queryResult(100L, 1L, "크리에이터 A", "스프링 입문")),
                        servicePage,
                        1
                ));

        // when & then
        mockMvc.perform(get("/api/courses")
                        .header("Authorization", "Bearer creator-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.page").value(1))
                .andExpect(jsonPath("$.data.size").value(20))
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.totalPages").value(1))
                .andExpect(jsonPath("$.data.content[0].id").value(100L))
                .andExpect(jsonPath("$.data.content[0].creatorId").value(1L))
                .andExpect(jsonPath("$.data.content[0].creatorName").value("크리에이터 A"))
                .andExpect(jsonPath("$.data.content[0].title").value("스프링 입문"))
                .andExpect(jsonPath("$.data.content[0].occupiedCount").value(0));

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(courseService).getCourses(isNull(), pageableCaptor.capture());
        Pageable pageable = pageableCaptor.getValue();
        assertThat(pageable.getPageNumber()).isZero();
        assertThat(pageable.getPageSize()).isEqualTo(20);
        assertThat(pageable.getSort().getOrderFor("createdAt").getDirection()).isEqualTo(Sort.Direction.DESC);
    }

    @Test
    @DisplayName("Course 목록 조회 API는 status 필터와 1부터 시작하는 page 파라미터를 서비스에 전달한다")
    void getCoursesByStatus() throws Exception {
        // given
        mockToken("creator-token", 1L, UserRole.CREATOR);
        when(courseService.getCourses(eq(CourseStatus.DRAFT), any(Pageable.class)))
                .thenReturn(new PageImpl<>(
                        List.of(queryResult(100L, 1L, "크리에이터 A", "스프링 입문")),
                        PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt")),
                        1
                ));

        // when & then
        mockMvc.perform(get("/api/courses")
                        .header("Authorization", "Bearer creator-token")
                        .param("status", CourseStatus.DRAFT.name())
                        .param("page", "1")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.page").value(1))
                .andExpect(jsonPath("$.data.size").value(10))
                .andExpect(jsonPath("$.data.content[0].status").value(CourseStatus.DRAFT.name()));

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(courseService).getCourses(eq(CourseStatus.DRAFT), pageableCaptor.capture());
        Pageable pageable = pageableCaptor.getValue();
        assertThat(pageable.getPageNumber()).isZero();
        assertThat(pageable.getPageSize()).isEqualTo(10);
    }

    @Test
    @DisplayName("Course 목록 조회 API는 잘못된 status 값이면 400 validation 응답을 반환한다")
    void rejectInvalidCourseStatusFilter() throws Exception {
        // given
        mockToken("creator-token", 1L, UserRole.CREATOR);

        // when & then
        mockMvc.perform(get("/api/courses")
                        .header("Authorization", "Bearer creator-token")
                        .param("status", "INVALID"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value(GlobalErrorCode.VALIDATION_ERROR.code()));
        verify(courseService, never()).getCourses(any(), any());
    }

    @Test
    @DisplayName("Course 상세 조회 API는 강사 이름과 현재 신청 인원을 포함한 응답을 반환한다")
    void getCourse() throws Exception {
        // given
        mockToken("creator-token", 1L, UserRole.CREATOR);
        when(courseService.getCourse(100L)).thenReturn(queryResult(100L, 1L, "크리에이터 A", "스프링 입문"));

        // when & then
        mockMvc.perform(get("/api/courses/100")
                        .header("Authorization", "Bearer creator-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(100L))
                .andExpect(jsonPath("$.data.creatorId").value(1L))
                .andExpect(jsonPath("$.data.creatorName").value("크리에이터 A"))
                .andExpect(jsonPath("$.data.title").value("스프링 입문"))
                .andExpect(jsonPath("$.data.occupiedCount").value(0));
    }

    @Test
    @DisplayName("Course 상세 조회 API는 Course가 없으면 404 공통 실패 응답을 반환한다")
    void rejectUnknownCourseDetail() throws Exception {
        // given
        mockToken("creator-token", 1L, UserRole.CREATOR);
        when(courseService.getCourse(999_999L))
                .thenThrow(new BusinessException(CourseErrorCode.COURSE_NOT_FOUND));

        // when & then
        mockMvc.perform(get("/api/courses/999999")
                        .header("Authorization", "Bearer creator-token"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value(CourseErrorCode.COURSE_NOT_FOUND.code()));
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

    private CourseQueryResult queryResult(Long courseId, Long creatorId, String creatorName, String title) {
        return new CourseQueryResult(
                courseId,
                creatorId,
                creatorName,
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
