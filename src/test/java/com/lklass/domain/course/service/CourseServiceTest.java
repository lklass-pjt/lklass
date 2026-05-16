package com.lklass.domain.course.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.lklass.TestcontainersConfiguration;
import com.lklass.domain.course.dto.CourseCreateResult;
import com.lklass.domain.course.dto.CourseQueryResult;
import com.lklass.domain.course.entity.CourseStatus;
import com.lklass.domain.course.entity.CourseStatusChangedBy;
import com.lklass.domain.course.entity.CourseStatusChangeReason;
import com.lklass.domain.course.exception.CourseErrorCode;
import com.lklass.domain.course.repository.CourseRepository;
import com.lklass.domain.course.repository.CourseStatusHistoryRepository;
import com.lklass.domain.user.entity.User;
import com.lklass.domain.user.entity.UserRole;
import com.lklass.domain.user.exception.UserErrorCode;
import com.lklass.domain.user.repository.UserRepository;
import com.lklass.global.exception.BusinessException;
import com.lklass.global.security.AuthenticatedUser;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

@Import({TestcontainersConfiguration.class, CourseServiceTest.FixedClockConfig.class})
@SpringBootTest
class CourseServiceTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 5, 16, 10, 0);
    private static final LocalDateTime ENROLLMENT_START_AT = LocalDateTime.of(2026, 5, 20, 10, 0);
    private static final LocalDateTime ENROLLMENT_END_AT = LocalDateTime.of(2026, 5, 27, 18, 0);
    private static final LocalDateTime COURSE_START_AT = LocalDateTime.of(2026, 6, 1, 9, 0);
    private static final LocalDateTime COURSE_END_AT = LocalDateTime.of(2026, 6, 30, 18, 0);

    @Autowired
    private CourseService courseService;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private CourseStatusHistoryRepository courseStatusHistoryRepository;

    @Autowired
    private UserRepository userRepository;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("권한 통과 후 Course와 CREATED 상태 이력이 저장된다")
    void createCourseAndStatusHistory() {
        // given
        User creator = userRepository.save(User.create(
                "course-creator@example.com",
                "encoded-password",
                "크리에이터 A",
                UserRole.CREATOR
        ));
        AuthenticatedUser actor = authenticate(creator.getId(), UserRole.CREATOR);

        // when
        CourseCreateResult result = courseService.createCourse(
                actor,
                null,
                "스프링 입문",
                "스프링 부트와 JPA 기초 강의",
                new BigDecimal("10000"),
                30,
                ENROLLMENT_START_AT,
                ENROLLMENT_END_AT,
                COURSE_START_AT,
                COURSE_END_AT
        );

        // then
        assertThat(result.id()).isNotNull();
        assertThat(result.creatorId()).isEqualTo(creator.getId());
        assertThat(result.title()).isEqualTo("스프링 입문");
        assertThat(result.description()).isEqualTo("스프링 부트와 JPA 기초 강의");
        assertThat(result.price()).isEqualByComparingTo("10000.00");
        assertThat(result.capacity()).isEqualTo(30);
        assertThat(result.occupiedCount()).isZero();
        assertThat(result.status()).isEqualTo(CourseStatus.DRAFT);
        assertThat(result.autoPublishEnabled()).isFalse();
        assertThat(result.enrollmentStartAt()).isEqualTo(ENROLLMENT_START_AT);
        assertThat(result.enrollmentEndAt()).isEqualTo(ENROLLMENT_END_AT);
        assertThat(result.courseStartAt()).isEqualTo(COURSE_START_AT);
        assertThat(result.courseEndAt()).isEqualTo(COURSE_END_AT);
        assertThat(courseRepository.findById(result.id())).hasValueSatisfying(foundCourse -> {
            assertThat(foundCourse.getCreatorId()).isEqualTo(creator.getId());
            assertThat(foundCourse.getTitle()).isEqualTo("스프링 입문");
            assertThat(foundCourse.getDescription()).isEqualTo("스프링 부트와 JPA 기초 강의");
            assertThat(foundCourse.getPrice().getAmount()).isEqualByComparingTo("10000.00");
            assertThat(foundCourse.getCapacity().getValue()).isEqualTo(30);
            assertThat(foundCourse.getOccupiedCount()).isZero();
            assertThat(foundCourse.getStatus()).isEqualTo(CourseStatus.DRAFT);
            assertThat(foundCourse.isAutoPublishEnabled()).isFalse();
            assertThat(foundCourse.getCreatedAt()).isEqualTo(NOW);
            assertThat(foundCourse.getUpdatedAt()).isEqualTo(NOW);
        });
        assertThat(courseStatusHistoryRepository.findAllByCourseId(result.id()))
                .singleElement()
                .satisfies(history -> {
                    assertThat(history.getCourse().getId()).isEqualTo(result.id());
                    assertThat(history.getFromStatus()).isNull();
                    assertThat(history.getToStatus()).isEqualTo(CourseStatus.DRAFT);
                    assertThat(history.getReason()).isEqualTo(CourseStatusChangeReason.CREATED);
                    assertThat(history.getChangedAt()).isEqualTo(NOW);
                    assertThat(history.getChangedBy()).isEqualTo(CourseStatusChangedBy.user(creator.getId()));
                });
    }

    @Test
    @DisplayName("ADMIN이 CREATOR 명의로 Course를 대리 생성하면 상태 이력의 changedBy는 실제 행위자인 ADMIN으로 저장된다")
    void createCourseByAdminOnBehalfOfCreatorRecordsAdminAsChangedBy() {
        // given
        User admin = userRepository.save(User.create(
                "admin-create-course@example.com",
                "encoded-password",
                "관리자 A",
                UserRole.ADMIN
        ));
        User creator = userRepository.save(User.create(
                "delegated-creator@example.com",
                "encoded-password",
                "크리에이터 B",
                UserRole.CREATOR
        ));
        AuthenticatedUser actor = authenticate(admin.getId(), UserRole.ADMIN);

        // when
        CourseCreateResult result = courseService.createCourse(
                actor,
                creator.getId(),
                "관리자 대리 등록 강의",
                "관리자가 크리에이터 명의로 등록한 강의",
                new BigDecimal("15000"),
                20,
                ENROLLMENT_START_AT,
                ENROLLMENT_END_AT,
                COURSE_START_AT,
                COURSE_END_AT
        );

        // then
        assertThat(result.creatorId()).isEqualTo(creator.getId());
        assertThat(courseStatusHistoryRepository.findAllByCourseId(result.id()))
                .singleElement()
                .satisfies(history -> {
                    assertThat(history.getCourse().getId()).isEqualTo(result.id());
                    assertThat(history.getReason()).isEqualTo(CourseStatusChangeReason.CREATED);
                    assertThat(history.getChangedBy()).isEqualTo(CourseStatusChangedBy.user(admin.getId()));
                });
    }

    @Test
    @DisplayName("ADMIN이 지정한 creator가 없으면 USER_NOT_FOUND 예외가 발생한다")
    void rejectUnknownRequestedCreatorId() {
        // given
        User admin = userRepository.save(User.create(
                "admin-unknown-creator@example.com",
                "encoded-password",
                "관리자 A",
                UserRole.ADMIN
        ));
        AuthenticatedUser actor = authenticate(admin.getId(), UserRole.ADMIN);

        // when & then
        assertThatThrownBy(() -> courseService.createCourse(
                actor,
                999_999L,
                "스프링 입문",
                "스프링 부트와 JPA 기초 강의",
                new BigDecimal("10000"),
                30,
                ENROLLMENT_START_AT,
                ENROLLMENT_END_AT,
                COURSE_START_AT,
                COURSE_END_AT
        )).isInstanceOfSatisfying(BusinessException.class, exception ->
                assertThat(exception.errorCode()).isEqualTo(UserErrorCode.USER_NOT_FOUND)
        );
    }

    @Test
    @DisplayName("ADMIN이 지정한 사용자가 CREATOR가 아니면 USER_NOT_CREATOR 예외가 발생한다")
    void rejectRequestedCreatorWithNonCreatorRole() {
        // given
        User admin = userRepository.save(User.create(
                "admin-student-creator@example.com",
                "encoded-password",
                "관리자 A",
                UserRole.ADMIN
        ));
        User student = userRepository.save(User.create(
                "student-as-creator@example.com",
                "encoded-password",
                "학생 A",
                UserRole.STUDENT
        ));
        AuthenticatedUser actor = authenticate(admin.getId(), UserRole.ADMIN);

        // when & then
        assertThatThrownBy(() -> courseService.createCourse(
                actor,
                student.getId(),
                "스프링 입문",
                "스프링 부트와 JPA 기초 강의",
                new BigDecimal("10000"),
                30,
                ENROLLMENT_START_AT,
                ENROLLMENT_END_AT,
                COURSE_START_AT,
                COURSE_END_AT
        )).isInstanceOfSatisfying(BusinessException.class, exception ->
                assertThat(exception.errorCode()).isEqualTo(UserErrorCode.USER_NOT_CREATOR)
        );
    }

    @Test
    @DisplayName("@PreAuthorize는 권한 없는 서비스 호출을 대표적으로 차단한다")
    void rejectCourseCreationByPreAuthorize() {
        // given
        AuthenticatedUser actor = authenticate(3L, UserRole.STUDENT);

        // when & then
        assertThatThrownBy(() -> courseService.createCourse(
                actor,
                null,
                "스프링 입문",
                "스프링 부트와 JPA 기초 강의",
                new BigDecimal("10000"),
                30,
                ENROLLMENT_START_AT,
                ENROLLMENT_END_AT,
                COURSE_START_AT,
                COURSE_END_AT
        )).isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @DisplayName("Course 목록 조회는 페이지 정보와 현재 신청 인원을 포함한 Course 요약을 반환한다")
    void getCourses() {
        // given
        User creator = userRepository.save(User.create(
                "list-creator@example.com",
                "encoded-password",
                "목록 크리에이터",
                UserRole.CREATOR
        ));
        AuthenticatedUser actor = authenticate(creator.getId(), UserRole.CREATOR);
        CourseCreateResult firstCourse = createCourse(actor, null, "목록 조회 강의 A");
        CourseCreateResult secondCourse = createCourse(actor, null, "목록 조회 강의 B");
        PageRequest pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "id"));

        // when
        Page<CourseQueryResult> result = courseService.getCourses(null, pageable);

        // then
        assertThat(result.getNumber()).isZero();
        assertThat(result.getSize()).isEqualTo(10);
        assertThat(result.getContent())
                .extracting(CourseQueryResult::id)
                .contains(firstCourse.id(), secondCourse.id());
        assertThat(result.getContent())
                .filteredOn(course -> course.id().equals(firstCourse.id()))
                .singleElement()
                .satisfies(course -> {
                    assertThat(course.creatorId()).isEqualTo(creator.getId());
                    assertThat(course.creatorName()).isEqualTo("목록 크리에이터");
                    assertThat(course.title()).isEqualTo("목록 조회 강의 A");
                    assertThat(course.price()).isEqualByComparingTo("10000.00");
                    assertThat(course.capacity()).isEqualTo(30);
                    assertThat(course.occupiedCount()).isZero();
                    assertThat(course.status()).isEqualTo(CourseStatus.DRAFT);
                });
    }

    @Test
    @DisplayName("Course 목록 조회는 status 필터가 있으면 해당 상태의 Course만 반환한다")
    void getCoursesByStatus() {
        // given
        User creator = userRepository.save(User.create(
                "status-list-creator@example.com",
                "encoded-password",
                "상태 필터 크리에이터",
                UserRole.CREATOR
        ));
        AuthenticatedUser actor = authenticate(creator.getId(), UserRole.CREATOR);
        CourseCreateResult draftCourse = createCourse(actor, null, "상태 필터 강의");
        PageRequest pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "id"));

        // when
        Page<CourseQueryResult> result = courseService.getCourses(CourseStatus.DRAFT, pageable);

        // then
        assertThat(result.getContent())
                .extracting(CourseQueryResult::id)
                .contains(draftCourse.id());
        assertThat(result.getContent())
                .allSatisfy(course -> assertThat(course.status()).isEqualTo(CourseStatus.DRAFT));
    }

    @Test
    @DisplayName("Course 상세 조회는 현재 신청 인원을 포함한 Course 정보를 반환한다")
    void getCourse() {
        // given
        User creator = userRepository.save(User.create(
                "detail-creator@example.com",
                "encoded-password",
                "상세 크리에이터",
                UserRole.CREATOR
        ));
        AuthenticatedUser actor = authenticate(creator.getId(), UserRole.CREATOR);
        CourseCreateResult createdCourse = createCourse(actor, null, "상세 조회 강의");

        // when
        CourseQueryResult result = courseService.getCourse(createdCourse.id());

        // then
        assertThat(result.id()).isEqualTo(createdCourse.id());
        assertThat(result.creatorId()).isEqualTo(creator.getId());
        assertThat(result.creatorName()).isEqualTo("상세 크리에이터");
        assertThat(result.title()).isEqualTo("상세 조회 강의");
        assertThat(result.description()).isEqualTo("스프링 부트와 JPA 기초 강의");
        assertThat(result.price()).isEqualByComparingTo("10000.00");
        assertThat(result.capacity()).isEqualTo(30);
        assertThat(result.occupiedCount()).isZero();
        assertThat(result.status()).isEqualTo(CourseStatus.DRAFT);
        assertThat(result.autoPublishEnabled()).isFalse();
        assertThat(result.enrollmentStartAt()).isEqualTo(ENROLLMENT_START_AT);
        assertThat(result.enrollmentEndAt()).isEqualTo(ENROLLMENT_END_AT);
        assertThat(result.courseStartAt()).isEqualTo(COURSE_START_AT);
        assertThat(result.courseEndAt()).isEqualTo(COURSE_END_AT);
    }

    @Test
    @DisplayName("존재하지 않는 Course 상세 조회는 COURSE_NOT_FOUND 예외가 발생한다")
    void rejectUnknownCourseDetail() {
        // given
        Long unknownCourseId = 999_999L;

        // when & then
        assertThatThrownBy(() -> courseService.getCourse(unknownCourseId))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.errorCode()).isEqualTo(CourseErrorCode.COURSE_NOT_FOUND)
                );
    }

    private CourseCreateResult createCourse(AuthenticatedUser actor, Long requestedCreatorId, String title) {
        return courseService.createCourse(
                actor,
                requestedCreatorId,
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

    private AuthenticatedUser authenticate(Long userId, UserRole role) {
        AuthenticatedUser principal = new AuthenticatedUser(userId, role);
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                principal,
                "access-token",
                List.of(new SimpleGrantedAuthority("ROLE_" + role.name()))
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);
        return principal;
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class FixedClockConfig {

        @Bean
        @Primary
        Clock fixedClock() {
            return Clock.fixed(
                    Instant.parse("2026-05-16T01:00:00Z"),
                    ZoneId.of("Asia/Seoul")
            );
        }
    }
}
