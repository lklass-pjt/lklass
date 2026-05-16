package com.lklass.domain.enrollment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.lklass.TestcontainersConfiguration;
import com.lklass.domain.course.entity.Course;
import com.lklass.domain.course.entity.CourseStatus;
import com.lklass.domain.course.exception.CourseErrorCode;
import com.lklass.domain.course.repository.CourseRepository;
import com.lklass.domain.enrollment.dto.EnrollmentApplyResult;
import com.lklass.domain.enrollment.entity.Enrollment;
import com.lklass.domain.enrollment.entity.EnrollmentStatus;
import com.lklass.domain.enrollment.entity.EnrollmentStatusChangeReason;
import com.lklass.domain.enrollment.entity.EnrollmentStatusChangedBy;
import com.lklass.domain.enrollment.exception.EnrollmentErrorCode;
import com.lklass.domain.enrollment.repository.EnrollmentRepository;
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
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

@SpringBootTest
@Import({TestcontainersConfiguration.class, EnrollmentServiceTest.FixedClockConfig.class})
class EnrollmentServiceTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 5, 16, 10, 0);
    private static final LocalDateTime ENROLLMENT_END_AT = NOW.plusDays(7);

    @Autowired
    private EnrollmentService enrollmentService;

    @Autowired
    private EnrollmentRepository enrollmentRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private UserRepository userRepository;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("STUDENT가 OPEN Course에 신청하면 좌석을 확보하고 PENDING 신청과 활성 신청, 상태 이력을 저장한다")
    void applyEnrollment() {
        // given
        User student = saveUser("student-apply@example.com", UserRole.STUDENT);
        Course course = saveOpenCourse(2);
        AuthenticatedUser actor = authenticate(student);

        // when
        EnrollmentApplyResult result = enrollmentService.apply(actor, course.getId());

        // then
        assertThat(result.id()).isNotNull();
        assertThat(result.courseId()).isEqualTo(course.getId());
        assertThat(result.userId()).isEqualTo(student.getId());
        assertThat(result.status()).isEqualTo(EnrollmentStatus.PENDING);
        assertThat(result.enrolledAt()).isEqualTo(NOW);
        assertThat(courseRepository.findById(course.getId())).hasValueSatisfying(found ->
                assertThat(found.getOccupiedCount()).isEqualTo(1)
        );
        assertThat(enrollmentRepository.existsActiveEnrollment(course.getId(), student.getId())).isTrue();
        assertThat(enrollmentRepository.findStatusHistories(result.id()))
                .singleElement()
                .satisfies(history -> {
                    assertThat(history.getFromStatus()).isNull();
                    assertThat(history.getToStatus()).isEqualTo(EnrollmentStatus.PENDING);
                    assertThat(history.getReason()).isEqualTo(EnrollmentStatusChangeReason.CREATED);
                    assertThat(history.getChangedAt()).isEqualTo(NOW);
                    assertThat(history.getChangedBy()).isEqualTo(EnrollmentStatusChangedBy.user(student.getId()));
                });
    }

    @Test
    @DisplayName("PENDING 신청의 결제를 확정하면 CONFIRMED 상태와 결제 확정 시각, 상태 이력이 저장된다")
    void confirmPayment() {
        // given
        User student = saveUser("student-confirm@example.com", UserRole.STUDENT);
        Course course = saveOpenCourse(2);
        AuthenticatedUser actor = authenticate(student);
        EnrollmentApplyResult applied = enrollmentService.apply(actor, course.getId());

        // when
        enrollmentService.confirmPayment(actor, applied.id());

        // then
        Enrollment found = enrollmentRepository.findById(applied.id()).orElseThrow();
        assertThat(found.getStatus()).isEqualTo(EnrollmentStatus.CONFIRMED);
        assertThat(found.getConfirmedAt()).isEqualTo(NOW);
        assertThat(found.getCancelledAt()).isNull();
        assertThat(enrollmentRepository.existsActiveEnrollment(course.getId(), student.getId())).isTrue();
        assertThat(courseRepository.findById(course.getId())).hasValueSatisfying(foundCourse ->
                assertThat(foundCourse.getOccupiedCount()).isEqualTo(1)
        );
        assertThat(enrollmentRepository.findStatusHistories(applied.id()))
                .extracting(history -> history.getReason())
                .containsExactly(
                        EnrollmentStatusChangeReason.CREATED,
                        EnrollmentStatusChangeReason.PAYMENT_CONFIRMED
                );
        assertThat(enrollmentRepository.findStatusHistories(applied.id()).getLast())
                .satisfies(history -> {
                    assertThat(history.getFromStatus()).isEqualTo(EnrollmentStatus.PENDING);
                    assertThat(history.getToStatus()).isEqualTo(EnrollmentStatus.CONFIRMED);
                    assertThat(history.getChangedAt()).isEqualTo(NOW);
                    assertThat(history.getChangedBy()).isEqualTo(EnrollmentStatusChangedBy.user(student.getId()));
                });
    }

    @Test
    @DisplayName("존재하지 않는 신청의 결제를 확정하면 ENROLLMENT_NOT_FOUND 예외가 발생한다")
    void rejectUnknownEnrollmentConfirmation() {
        // given
        User student = saveUser("student-confirm-unknown@example.com", UserRole.STUDENT);
        AuthenticatedUser actor = authenticate(student);

        // when & then
        assertThatThrownBy(() -> enrollmentService.confirmPayment(actor, 999_999L))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.errorCode()).isEqualTo(EnrollmentErrorCode.ENROLLMENT_NOT_FOUND)
                );
    }

    @Test
    @DisplayName("다른 사용자의 신청 결제를 확정하면 존재 여부를 숨기기 위해 ENROLLMENT_NOT_FOUND 예외가 발생한다")
    void rejectOtherUsersEnrollmentConfirmation() {
        // given
        User owner = saveUser("student-confirm-owner@example.com", UserRole.STUDENT);
        User otherStudent = saveUser("student-confirm-other@example.com", UserRole.STUDENT);
        Course course = saveOpenCourse(2);
        EnrollmentApplyResult applied = enrollmentService.apply(authenticate(owner), course.getId());
        AuthenticatedUser actor = authenticate(otherStudent);

        // when & then
        assertThatThrownBy(() -> enrollmentService.confirmPayment(actor, applied.id()))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.errorCode()).isEqualTo(EnrollmentErrorCode.ENROLLMENT_NOT_FOUND)
                );
        Enrollment found = enrollmentRepository.findById(applied.id()).orElseThrow();
        assertThat(found.getStatus()).isEqualTo(EnrollmentStatus.PENDING);
        assertThat(found.getConfirmedAt()).isNull();
    }

    @Test
    @DisplayName("이미 CONFIRMED 상태인 신청의 결제를 다시 확정하면 INVALID_ENROLLMENT_STATUS 예외가 발생한다")
    void rejectAlreadyConfirmedEnrollmentConfirmation() {
        // given
        User student = saveUser("student-confirm-again@example.com", UserRole.STUDENT);
        Course course = saveOpenCourse(2);
        AuthenticatedUser actor = authenticate(student);
        EnrollmentApplyResult applied = enrollmentService.apply(actor, course.getId());
        enrollmentService.confirmPayment(actor, applied.id());

        // when & then
        assertThatThrownBy(() -> enrollmentService.confirmPayment(actor, applied.id()))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.errorCode()).isEqualTo(EnrollmentErrorCode.INVALID_ENROLLMENT_STATUS)
                );
        assertThat(enrollmentRepository.findStatusHistories(applied.id()))
                .hasSize(2);
    }

    @Test
    @DisplayName("서로 다른 STUDENT가 같은 Course에 신청하면 각자 활성 신청이 생성되고 좌석 수가 누적 증가한다")
    void applyEnrollmentByDifferentStudentsInSameCourse() {
        // given
        User firstStudent = saveUser("student-same-course-first@example.com", UserRole.STUDENT);
        User secondStudent = saveUser("student-same-course-second@example.com", UserRole.STUDENT);
        Course course = saveOpenCourse(2);

        // when
        EnrollmentApplyResult firstResult = enrollmentService.apply(authenticate(firstStudent), course.getId());
        EnrollmentApplyResult secondResult = enrollmentService.apply(authenticate(secondStudent), course.getId());

        // then
        assertThat(firstResult.status()).isEqualTo(EnrollmentStatus.PENDING);
        assertThat(secondResult.status()).isEqualTo(EnrollmentStatus.PENDING);
        assertThat(enrollmentRepository.existsActiveEnrollment(course.getId(), firstStudent.getId())).isTrue();
        assertThat(enrollmentRepository.existsActiveEnrollment(course.getId(), secondStudent.getId())).isTrue();
        assertThat(courseRepository.findById(course.getId())).hasValueSatisfying(found ->
                assertThat(found.getOccupiedCount()).isEqualTo(2)
        );
    }

    @Test
    @DisplayName("STUDENT가 아닌 사용자는 수강 신청 서비스 진입 전에 권한 거부된다")
    void rejectNonStudent() {
        // given
        User creator = saveUser("creator-apply@example.com", UserRole.CREATOR);
        Course course = saveOpenCourse(2);
        AuthenticatedUser actor = authenticate(creator);

        // when & then
        assertThatThrownBy(() -> enrollmentService.apply(actor, course.getId()))
                .isInstanceOf(AccessDeniedException.class);
        assertThat(courseRepository.findById(course.getId())).hasValueSatisfying(found ->
                assertThat(found.getOccupiedCount()).isZero()
        );
    }

    @Test
    @DisplayName("이미 활성 신청이 있는 사용자가 같은 Course에 다시 신청하면 ALREADY_ENROLLED 예외가 발생한다")
    void rejectDuplicateActiveEnrollment() {
        // given
        User student = saveUser("student-duplicate@example.com", UserRole.STUDENT);
        Course course = saveOpenCourse(2);
        AuthenticatedUser actor = authenticate(student);
        enrollmentService.apply(actor, course.getId());

        // when & then
        assertThatThrownBy(() -> enrollmentService.apply(actor, course.getId()))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.errorCode()).isEqualTo(EnrollmentErrorCode.ALREADY_ENROLLED)
                );
        assertThat(courseRepository.findById(course.getId())).hasValueSatisfying(found ->
                assertThat(found.getOccupiedCount()).isEqualTo(1)
        );
    }

    @Test
    @DisplayName("정원이 모두 찬 Course에 신청하면 CAPACITY_EXCEEDED 예외가 발생하고 좌석 수는 증가하지 않는다")
    void rejectCapacityExceeded() {
        // given
        User firstStudent = saveUser("student-capacity-first@example.com", UserRole.STUDENT);
        User secondStudent = saveUser("student-capacity-second@example.com", UserRole.STUDENT);
        Course course = saveOpenCourse(1);
        enrollmentService.apply(authenticate(firstStudent), course.getId());
        AuthenticatedUser secondActor = authenticate(secondStudent);

        // when & then
        assertThatThrownBy(() -> enrollmentService.apply(secondActor, course.getId()))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.errorCode()).isEqualTo(EnrollmentErrorCode.CAPACITY_EXCEEDED)
                );
        assertThat(courseRepository.findById(course.getId())).hasValueSatisfying(found ->
                assertThat(found.getOccupiedCount()).isEqualTo(1)
        );
        assertThat(enrollmentRepository.existsActiveEnrollment(course.getId(), secondStudent.getId())).isFalse();
    }

    @Test
    @DisplayName("DRAFT Course에 신청하면 ENROLLMENT_NOT_AVAILABLE 예외가 발생한다")
    void rejectDraftCourse() {
        // given
        User student = saveUser("student-draft@example.com", UserRole.STUDENT);
        Course course = saveDraftCourse(2);
        AuthenticatedUser actor = authenticate(student);

        // when & then
        assertThatThrownBy(() -> enrollmentService.apply(actor, course.getId()))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.errorCode()).isEqualTo(EnrollmentErrorCode.ENROLLMENT_NOT_AVAILABLE)
                );
        assertThat(courseRepository.findById(course.getId())).hasValueSatisfying(found ->
                assertThat(found.getOccupiedCount()).isZero()
        );
    }

    @Test
    @DisplayName("CLOSED Course에 신청하면 ENROLLMENT_NOT_AVAILABLE 예외가 발생한다")
    void rejectClosedCourse() {
        // given
        User student = saveUser("student-closed@example.com", UserRole.STUDENT);
        Course course = saveClosedCourse(2);
        AuthenticatedUser actor = authenticate(student);

        // when & then
        assertThatThrownBy(() -> enrollmentService.apply(actor, course.getId()))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.errorCode()).isEqualTo(EnrollmentErrorCode.ENROLLMENT_NOT_AVAILABLE)
                );
        assertThat(courseRepository.findById(course.getId())).hasValueSatisfying(found ->
                assertThat(found.getOccupiedCount()).isZero()
        );
    }

    @Test
    @DisplayName("모집 시작 전 OPEN Course에 신청하면 ENROLLMENT_NOT_AVAILABLE 예외가 발생한다")
    void rejectCourseBeforeEnrollmentStart() {
        // given
        User student = saveUser("student-before-start@example.com", UserRole.STUDENT);
        Course course = saveOpenCourseBeforeEnrollmentStart(2);
        AuthenticatedUser actor = authenticate(student);

        // when & then
        assertThatThrownBy(() -> enrollmentService.apply(actor, course.getId()))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.errorCode()).isEqualTo(EnrollmentErrorCode.ENROLLMENT_NOT_AVAILABLE)
                );
        assertThat(courseRepository.findById(course.getId())).hasValueSatisfying(found ->
                assertThat(found.getOccupiedCount()).isZero()
        );
    }

    @Test
    @DisplayName("모집 마감 시각 이후 OPEN Course에 신청하면 ENROLLMENT_NOT_AVAILABLE 예외가 발생한다")
    void rejectCourseAfterEnrollmentEnd() {
        // given
        User student = saveUser("student-after-end@example.com", UserRole.STUDENT);
        Course course = saveExpiredOpenCourse(2);
        AuthenticatedUser actor = authenticate(student);

        // when & then
        assertThatThrownBy(() -> enrollmentService.apply(actor, course.getId()))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.errorCode()).isEqualTo(EnrollmentErrorCode.ENROLLMENT_NOT_AVAILABLE)
                );
        assertThat(courseRepository.findById(course.getId())).hasValueSatisfying(found ->
                assertThat(found.getOccupiedCount()).isZero()
        );
    }

    @Test
    @DisplayName("존재하지 않는 Course에 신청하면 COURSE_NOT_FOUND 예외가 발생한다")
    void rejectUnknownCourse() {
        // given
        User student = saveUser("student-unknown-course@example.com", UserRole.STUDENT);
        AuthenticatedUser actor = authenticate(student);

        // when & then
        assertThatThrownBy(() -> enrollmentService.apply(actor, 999_999L))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.errorCode()).isEqualTo(CourseErrorCode.COURSE_NOT_FOUND)
                );
    }

    @Test
    @DisplayName("존재하지 않는 사용자로 신청하면 USER_NOT_FOUND 예외가 발생한다")
    void rejectUnknownUser() {
        // given
        Course course = saveOpenCourse(2);
        AuthenticatedUser actor = authenticate(999_999L, UserRole.STUDENT);

        // when & then
        assertThatThrownBy(() -> enrollmentService.apply(actor, course.getId()))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.errorCode()).isEqualTo(UserErrorCode.USER_NOT_FOUND)
                );
        assertThat(courseRepository.findById(course.getId())).hasValueSatisfying(found ->
                assertThat(found.getOccupiedCount()).isZero()
        );
    }

    private Course saveOpenCourse(int capacity) {
        Course course = saveDraftCourse(capacity);
        course.openManually(NOW, ENROLLMENT_END_AT);
        return courseRepository.save(course);
    }

    private Course saveClosedCourse(int capacity) {
        Course course = saveOpenCourse(capacity);
        course.close();
        return courseRepository.save(course);
    }

    private Course saveOpenCourseBeforeEnrollmentStart(int capacity) {
        Course course = createCourse(
                capacity,
                NOW.plusDays(1),
                ENROLLMENT_END_AT,
                NOW.plusDays(10),
                NOW.plusDays(20)
        );
        ReflectionTestUtils.setField(course, "status", CourseStatus.OPEN);
        return courseRepository.save(course);
    }

    private Course saveExpiredOpenCourse(int capacity) {
        Course course = createCourse(
                capacity,
                NOW.minusDays(7),
                NOW,
                NOW.plusDays(10),
                NOW.plusDays(20)
        );
        course.openManually(NOW.minusDays(7), NOW);
        return courseRepository.save(course);
    }

    private Course saveDraftCourse(int capacity) {
        return createCourse(
                capacity,
                NOW.minusDays(1),
                ENROLLMENT_END_AT,
                NOW.plusDays(10),
                NOW.plusDays(20)
        );
    }

    private Course createCourse(
            int capacity,
            LocalDateTime enrollmentStartAt,
            LocalDateTime enrollmentEndAt,
            LocalDateTime courseStartAt,
            LocalDateTime courseEndAt
    ) {
        User creator = saveUser("creator-course-" + capacity + "-" + System.nanoTime() + "@example.com", UserRole.CREATOR);
        return courseRepository.save(Course.create(
                creator.getId(),
                "수강 신청 테스트 강의",
                "수강 신청 서비스 흐름을 검증하는 강의",
                new BigDecimal("10000"),
                capacity,
                enrollmentStartAt,
                enrollmentEndAt,
                courseStartAt,
                courseEndAt
        ));
    }

    private User saveUser(String email, UserRole role) {
        return userRepository.save(User.create(email, "encoded-password", "테스트 사용자", role));
    }

    private AuthenticatedUser authenticate(User user) {
        return authenticate(user.getId(), user.getRole());
    }

    private AuthenticatedUser authenticate(Long userId, UserRole role) {
        AuthenticatedUser actor = new AuthenticatedUser(userId, role);
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                actor,
                null,
                List.of(new SimpleGrantedAuthority("ROLE_" + role.name()))
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);
        return actor;
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class FixedClockConfig {

        @Bean
        @Primary
        Clock fixedClock() {
            return Clock.fixed(Instant.parse("2026-05-16T01:00:00Z"), ZoneId.of("Asia/Seoul"));
        }
    }
}
