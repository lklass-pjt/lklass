package com.lklass.domain.enrollment.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.lklass.TestcontainersConfiguration;
import com.lklass.domain.course.entity.Course;
import com.lklass.domain.user.entity.User;
import com.lklass.domain.user.entity.UserRole;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceException;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
@Import({TestcontainersConfiguration.class, EnrollmentPersistenceTest.FixedClockConfig.class})
class EnrollmentPersistenceTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 5, 16, 10, 0);

    @Autowired
    private EntityManager entityManager;

    @Test
    @DisplayName("수강 신청을 저장하면 기본 상태는 PENDING이고 신청 시각과 대상 Course/User를 보존한다")
    void saveEnrollment() {
        // given
        User creator = persistUser("creator@lklass.com", UserRole.CREATOR);
        User student = persistUser("student@lklass.com", UserRole.STUDENT);
        Course course = persistCourse(creator);
        Enrollment enrollment = Enrollment.create(course.getId(), student.getId(), NOW);

        // when
        entityManager.persist(enrollment);
        entityManager.flush();
        entityManager.clear();

        // then
        Enrollment found = entityManager.find(Enrollment.class, enrollment.getId());
        assertThat(found.getCourseId()).isEqualTo(course.getId());
        assertThat(found.getUserId()).isEqualTo(student.getId());
        assertThat(found.getStatus()).isEqualTo(EnrollmentStatus.PENDING);
        assertThat(found.getEnrolledAt()).isEqualTo(NOW);
        assertThat(found.getConfirmedAt()).isNull();
        assertThat(found.getCancelledAt()).isNull();
        assertThat(found.getCreatedAt()).isEqualTo(NOW);
        assertThat(found.getUpdatedAt()).isEqualTo(NOW);
    }

    @Test
    @DisplayName("ActiveEnrollment은 같은 Course와 User 조합을 하나만 허용해 중복 활성 신청을 막는다")
    void activeEnrollmentUniqueConstraint() {
        // given
        User creator = persistUser("creator-active@lklass.com", UserRole.CREATOR);
        User student = persistUser("student-active@lklass.com", UserRole.STUDENT);
        Course course = persistCourse(creator);
        Enrollment firstEnrollment = persistEnrollment(course, student, NOW);
        Enrollment secondEnrollment = persistEnrollment(course, student, NOW.plusMinutes(1));
        entityManager.persist(ActiveEnrollment.create(firstEnrollment));
        entityManager.flush();

        // when & then
        assertThatThrownBy(() -> {
            entityManager.persist(ActiveEnrollment.create(secondEnrollment));
            entityManager.flush();
        })
                .isInstanceOf(PersistenceException.class);
    }

    @Test
    @DisplayName("ActiveEnrollment은 같은 Enrollment를 중복 점유할 수 없다")
    void activeEnrollmentUniqueEnrollmentConstraint() {
        // given
        User creator = persistUser("creator-active-enrollment@lklass.com", UserRole.CREATOR);
        User student = persistUser("student-active-enrollment@lklass.com", UserRole.STUDENT);
        Course course = persistCourse(creator);
        Enrollment enrollment = persistEnrollment(course, student, NOW);
        entityManager.persist(ActiveEnrollment.create(enrollment));
        entityManager.flush();

        // when & then
        assertThatThrownBy(() -> {
            entityManager.persist(ActiveEnrollment.create(enrollment));
            entityManager.flush();
        })
                .isInstanceOf(PersistenceException.class);
    }

    @Test
    @DisplayName("ActiveEnrollment은 같은 Course라도 User가 다르면 각각 저장할 수 있다")
    void allowActiveEnrollmentsForDifferentUsersInSameCourse() {
        // given
        User creator = persistUser("creator-different-users@lklass.com", UserRole.CREATOR);
        User firstStudent = persistUser("student-a@lklass.com", UserRole.STUDENT);
        User secondStudent = persistUser("student-b@lklass.com", UserRole.STUDENT);
        Course course = persistCourse(creator);
        Enrollment firstEnrollment = persistEnrollment(course, firstStudent, NOW);
        Enrollment secondEnrollment = persistEnrollment(course, secondStudent, NOW.plusMinutes(1));

        // when
        entityManager.persist(ActiveEnrollment.create(firstEnrollment));
        entityManager.persist(ActiveEnrollment.create(secondEnrollment));
        entityManager.flush();

        // then
        Long activeEnrollmentCount = entityManager.createQuery(
                "select count(a) from ActiveEnrollment a where a.courseId = :courseId",
                Long.class
        ).setParameter("courseId", course.getId()).getSingleResult();
        assertThat(activeEnrollmentCount).isEqualTo(2);
    }

    @Test
    @DisplayName("ActiveEnrollment은 같은 User라도 Course가 다르면 각각 저장할 수 있다")
    void allowActiveEnrollmentsForSameUserInDifferentCourses() {
        // given
        User creator = persistUser("creator-different-courses@lklass.com", UserRole.CREATOR);
        User student = persistUser("student-different-courses@lklass.com", UserRole.STUDENT);
        Course firstCourse = persistCourse(creator);
        Course secondCourse = persistCourse(creator);
        Enrollment firstEnrollment = persistEnrollment(firstCourse, student, NOW);
        Enrollment secondEnrollment = persistEnrollment(secondCourse, student, NOW.plusMinutes(1));

        // when
        entityManager.persist(ActiveEnrollment.create(firstEnrollment));
        entityManager.persist(ActiveEnrollment.create(secondEnrollment));
        entityManager.flush();

        // then
        Long activeEnrollmentCount = entityManager.createQuery(
                "select count(a) from ActiveEnrollment a where a.userId = :userId",
                Long.class
        ).setParameter("userId", student.getId()).getSingleResult();
        assertThat(activeEnrollmentCount).isEqualTo(2);
    }

    @Test
    @DisplayName("수강 신청 상태 이력은 상태 변경 사유와 변경 주체를 append-only 기록으로 저장한다")
    void saveEnrollmentStatusHistory() {
        // given
        User creator = persistUser("creator-history@lklass.com", UserRole.CREATOR);
        User student = persistUser("student-history@lklass.com", UserRole.STUDENT);
        Course course = persistCourse(creator);
        Enrollment enrollment = persistEnrollment(course, student, NOW);
        EnrollmentStatusHistory history = EnrollmentStatusHistory.record(
                enrollment,
                null,
                EnrollmentStatus.PENDING,
                EnrollmentStatusChangeReason.CREATED,
                NOW,
                EnrollmentStatusChangedBy.user(student.getId())
        );

        // when
        entityManager.persist(history);
        entityManager.flush();
        entityManager.clear();

        // then
        EnrollmentStatusHistory found = entityManager.find(EnrollmentStatusHistory.class, history.getId());
        assertThat(found.getEnrollment().getId()).isEqualTo(enrollment.getId());
        assertThat(found.getFromStatus()).isNull();
        assertThat(found.getToStatus()).isEqualTo(EnrollmentStatus.PENDING);
        assertThat(found.getReason()).isEqualTo(EnrollmentStatusChangeReason.CREATED);
        assertThat(found.getChangedAt()).isEqualTo(NOW);
        assertThat(found.getChangedBy()).isEqualTo(EnrollmentStatusChangedBy.user(student.getId()));
    }

    @Test
    @DisplayName("수강 신청 상태 이력은 SYSTEM 변경 주체를 저장할 수 있다")
    void saveEnrollmentStatusHistoryChangedBySystem() {
        // given
        User creator = persistUser("creator-system-history@lklass.com", UserRole.CREATOR);
        User student = persistUser("student-system-history@lklass.com", UserRole.STUDENT);
        Course course = persistCourse(creator);
        Enrollment enrollment = persistEnrollment(course, student, NOW);
        EnrollmentStatusHistory history = EnrollmentStatusHistory.record(
                enrollment,
                EnrollmentStatus.PENDING,
                EnrollmentStatus.CANCELLED,
                EnrollmentStatusChangeReason.EXPIRED,
                NOW.plusMinutes(30),
                EnrollmentStatusChangedBy.system()
        );

        // when
        entityManager.persist(history);
        entityManager.flush();
        entityManager.clear();

        // then
        EnrollmentStatusHistory found = entityManager.find(EnrollmentStatusHistory.class, history.getId());
        assertThat(found.getChangedBy()).isEqualTo(EnrollmentStatusChangedBy.system());
        assertThat(found.getReason()).isEqualTo(EnrollmentStatusChangeReason.EXPIRED);
    }

    @Test
    @DisplayName("Flyway는 수강 신청 기본 테이블을 생성한다")
    void createEnrollmentTables() {
        // given
        List<String> tableNames = List.of(
                "enrollments",
                "active_enrollments",
                "enrollment_status_histories"
        );

        // when & then
        tableNames.forEach(tableName -> {
            Number tableCount = (Number) entityManager.createNativeQuery(
                    """
                            select count(*)
                              from information_schema.tables
                             where table_schema = database()
                               and table_name = ?
                            """
            ).setParameter(1, tableName).getSingleResult();

            assertThat(tableCount.longValue()).isEqualTo(1L);
        });
    }

    private Enrollment persistEnrollment(Course course, User student, LocalDateTime enrolledAt) {
        Enrollment enrollment = Enrollment.create(course.getId(), student.getId(), enrolledAt);
        entityManager.persist(enrollment);
        return enrollment;
    }

    private Course persistCourse(User creator) {
        Course course = Course.create(
                creator.getId(),
                "Spring Boot 수강 신청",
                "수강 신청 동시성 제어를 연습하는 강의",
                new BigDecimal("10000"),
                30,
                NOW.plusDays(1),
                NOW.plusDays(10),
                NOW.plusDays(20),
                NOW.plusDays(50)
        );
        entityManager.persist(course);
        return course;
    }

    private User persistUser(String email, UserRole role) {
        User user = User.create(email, "{noop}password", "테스트 사용자", role);
        entityManager.persist(user);
        entityManager.flush();
        return user;
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
