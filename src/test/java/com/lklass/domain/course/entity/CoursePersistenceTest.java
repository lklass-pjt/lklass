package com.lklass.domain.course.entity;

import static org.assertj.core.api.Assertions.assertThat;

import com.lklass.TestcontainersConfiguration;
import com.lklass.domain.user.entity.User;
import com.lklass.domain.user.entity.UserRole;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.transaction.annotation.Transactional;

@Import({TestcontainersConfiguration.class, CoursePersistenceTest.FixedClockConfig.class})
@SpringBootTest
@Transactional
class CoursePersistenceTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 5, 16, 10, 0);
    private static final LocalDateTime ENROLLMENT_START_AT = LocalDateTime.of(2026, 5, 20, 10, 0);
    private static final LocalDateTime ENROLLMENT_END_AT = LocalDateTime.of(2026, 5, 27, 18, 0);
    private static final LocalDateTime COURSE_START_AT = LocalDateTime.of(2026, 6, 1, 9, 0);
    private static final LocalDateTime COURSE_END_AT = LocalDateTime.of(2026, 6, 30, 18, 0);

    @Autowired
    private EntityManager entityManager;

    @Test
    @DisplayName("Course는 Flyway 스키마와 JPA 매핑으로 저장하고 조회할 수 있다")
    void saveAndFindCourse() {
        // given
        User creator = persistCreator("creator-course@example.com");
        Course course = createCourse(creator.getId());

        // when
        entityManager.persist(course);
        entityManager.flush();
        entityManager.clear();

        Course foundCourse = entityManager.find(Course.class, course.getId());

        // then
        assertThat(foundCourse.getId()).isNotNull();
        assertThat(foundCourse.getCreatorId()).isEqualTo(creator.getId());
        assertThat(foundCourse.getTitle()).isEqualTo("스프링 입문");
        assertThat(foundCourse.getDescription()).isEqualTo("스프링 부트와 JPA 기초 강의");
        assertThat(foundCourse.getPrice().getAmount()).isEqualByComparingTo("10000.00");
        assertThat(foundCourse.getCapacity().getValue()).isEqualTo(30);
        assertThat(foundCourse.getOccupiedCount()).isZero();
        assertThat(foundCourse.getStatus()).isEqualTo(CourseStatus.DRAFT);
        assertThat(foundCourse.isAutoPublishEnabled()).isFalse();
        assertThat(foundCourse.getEnrollmentPeriod().getStartAt()).isEqualTo(ENROLLMENT_START_AT);
        assertThat(foundCourse.getEnrollmentPeriod().getEndAt()).isEqualTo(ENROLLMENT_END_AT);
        assertThat(foundCourse.getCoursePeriod().getStartAt()).isEqualTo(COURSE_START_AT);
        assertThat(foundCourse.getCoursePeriod().getEndAt()).isEqualTo(COURSE_END_AT);
        assertThat(foundCourse.getCreatedAt()).isEqualTo(NOW);
        assertThat(foundCourse.getUpdatedAt()).isEqualTo(NOW);
    }

    @Test
    @DisplayName("CourseStatusHistory는 Course FK와 변경 이력 값을 저장하고 조회할 수 있다")
    void saveAndFindCourseStatusHistory() {
        // given
        User creator = persistCreator("creator-history@example.com");
        Course course = createCourse(creator.getId());
        entityManager.persist(course);
        CourseStatusHistory history = CourseStatusHistory.record(
                course,
                null,
                CourseStatus.DRAFT,
                CourseStatusChangeReason.CREATED,
                NOW,
                CourseStatusChangedBy.user(creator.getId())
        );

        // when
        entityManager.persist(history);
        entityManager.flush();
        entityManager.clear();

        CourseStatusHistory foundHistory = entityManager.find(CourseStatusHistory.class, history.getId());

        // then
        assertThat(foundHistory.getId()).isNotNull();
        assertThat(foundHistory.getCourse().getId()).isEqualTo(course.getId());
        assertThat(foundHistory.getFromStatus()).isNull();
        assertThat(foundHistory.getToStatus()).isEqualTo(CourseStatus.DRAFT);
        assertThat(foundHistory.getReason()).isEqualTo(CourseStatusChangeReason.CREATED);
        assertThat(foundHistory.getChangedAt()).isEqualTo(NOW);
        assertThat(foundHistory.getChangedBy().getValue()).isEqualTo("USER:" + creator.getId());
    }

    private User persistCreator(String email) {
        User creator = User.create(email, "encoded-password", "강사 A", UserRole.CREATOR);
        entityManager.persist(creator);
        entityManager.flush();
        return creator;
    }

    private Course createCourse(Long creatorId) {
        return Course.create(
                creatorId,
                "스프링 입문",
                "스프링 부트와 JPA 기초 강의",
                new BigDecimal("10000.00"),
                30,
                ENROLLMENT_START_AT,
                ENROLLMENT_END_AT,
                COURSE_START_AT,
                COURSE_END_AT
        );
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
