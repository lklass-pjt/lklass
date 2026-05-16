package com.lklass.domain.course.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.lklass.TestcontainersConfiguration;
import com.lklass.domain.course.entity.Course;
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

@SpringBootTest
@Transactional
@Import({TestcontainersConfiguration.class, CourseCapacityRepositoryTest.FixedClockConfig.class})
class CourseCapacityRepositoryTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 5, 16, 10, 0);
    private static final LocalDateTime ENROLLMENT_END_AT = NOW.plusDays(7);

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    @DisplayName("OPEN 상태이고 모집 기간 안이며 정원이 남아 있으면 좌석을 원자적으로 확보한다")
    void tryOccupySeat() {
        // given
        Course course = persistOpenCourse(2);

        // when
        boolean occupied = courseRepository.tryOccupySeat(course.getId(), NOW);
        entityManager.clear();

        // then
        Course found = entityManager.find(Course.class, course.getId());
        assertThat(occupied).isTrue();
        assertThat(found.getOccupiedCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("정원이 이미 찬 Course는 좌석 확보에 실패하고 occupiedCount를 증가시키지 않는다")
    void failToOccupySeatWhenCapacityFull() {
        // given
        Course course = persistOpenCourse(1);
        assertThat(courseRepository.tryOccupySeat(course.getId(), NOW)).isTrue();

        // when
        boolean occupied = courseRepository.tryOccupySeat(course.getId(), NOW);
        entityManager.clear();

        // then
        Course found = entityManager.find(Course.class, course.getId());
        assertThat(occupied).isFalse();
        assertThat(found.getOccupiedCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("OPEN 상태가 아니거나 모집 기간 밖이면 좌석 확보에 실패한다")
    void failToOccupySeatWhenCourseIsNotOpenOrEnrollmentPeriodIsInvalid() {
        // given
        Course draftCourse = persistDraftCourse(2);
        Course closedByPeriodCourse = persistOpenCourse(2);

        // when
        boolean draftOccupied = courseRepository.tryOccupySeat(draftCourse.getId(), NOW);
        boolean expiredOccupied = courseRepository.tryOccupySeat(closedByPeriodCourse.getId(), ENROLLMENT_END_AT);
        entityManager.clear();

        // then
        assertThat(draftOccupied).isFalse();
        assertThat(expiredOccupied).isFalse();
        assertThat(entityManager.find(Course.class, draftCourse.getId()).getOccupiedCount()).isZero();
        assertThat(entityManager.find(Course.class, closedByPeriodCourse.getId()).getOccupiedCount()).isZero();
    }

    @Test
    @DisplayName("모집 시작 전이면 좌석 확보에 실패한다")
    void failToOccupySeatBeforeEnrollmentStartAt() {
        // given
        Course course = persistOpenCourse(2);

        // when
        boolean occupied = courseRepository.tryOccupySeat(course.getId(), NOW.minusDays(2));
        entityManager.clear();

        // then
        Course found = entityManager.find(Course.class, course.getId());
        assertThat(occupied).isFalse();
        assertThat(found.getOccupiedCount()).isZero();
    }

    @Test
    @DisplayName("CLOSED Course는 좌석 확보에 실패한다")
    void failToOccupySeatWhenCourseIsClosed() {
        // given
        Course course = persistOpenCourse(2);
        course.close();
        entityManager.flush();
        entityManager.clear();

        // when
        boolean occupied = courseRepository.tryOccupySeat(course.getId(), NOW);
        entityManager.clear();

        // then
        Course found = entityManager.find(Course.class, course.getId());
        assertThat(occupied).isFalse();
        assertThat(found.getOccupiedCount()).isZero();
    }

    @Test
    @DisplayName("존재하지 않는 Course 좌석 확보와 반납은 실패한다")
    void failToOccupyOrReleaseSeatForUnknownCourse() {
        // given
        Long unknownCourseId = 999_999L;

        // when
        boolean occupied = courseRepository.tryOccupySeat(unknownCourseId, NOW);
        boolean released = courseRepository.releaseSeat(unknownCourseId);

        // then
        assertThat(occupied).isFalse();
        assertThat(released).isFalse();
    }

    @Test
    @DisplayName("좌석을 여러 번 확보하면 성공 횟수만큼 occupiedCount가 증가한다")
    void occupySeatMultipleTimes() {
        // given
        Course course = persistOpenCourse(3);

        // when
        boolean firstOccupied = courseRepository.tryOccupySeat(course.getId(), NOW);
        boolean secondOccupied = courseRepository.tryOccupySeat(course.getId(), NOW);
        entityManager.clear();

        // then
        Course found = entityManager.find(Course.class, course.getId());
        assertThat(firstOccupied).isTrue();
        assertThat(secondOccupied).isTrue();
        assertThat(found.getOccupiedCount()).isEqualTo(2);
    }

    @Test
    @DisplayName("확보된 좌석을 반납하면 occupiedCount를 1 감소시키고 0 미만으로 내려가지 않는다")
    void releaseSeat() {
        // given
        Course course = persistOpenCourse(2);
        assertThat(courseRepository.tryOccupySeat(course.getId(), NOW)).isTrue();

        // when
        boolean released = courseRepository.releaseSeat(course.getId());
        boolean releasedAgain = courseRepository.releaseSeat(course.getId());
        entityManager.clear();

        // then
        Course found = entityManager.find(Course.class, course.getId());
        assertThat(released).isTrue();
        assertThat(releasedAgain).isFalse();
        assertThat(found.getOccupiedCount()).isZero();
    }

    private Course persistOpenCourse(int capacity) {
        Course course = persistDraftCourse(capacity);
        course.openManually(NOW, ENROLLMENT_END_AT);
        entityManager.flush();
        entityManager.clear();
        return entityManager.find(Course.class, course.getId());
    }

    private Course persistDraftCourse(int capacity) {
        User creator = User.create(
                "creator-" + capacity + "-" + System.nanoTime() + "@lklass.com",
                "{noop}password",
                "강사",
                UserRole.CREATOR
        );
        entityManager.persist(creator);
        entityManager.flush();

        Course course = Course.create(
                creator.getId(),
                "정원 테스트 강의",
                "정원 확보 쿼리를 검증하는 강의",
                new BigDecimal("10000"),
                capacity,
                NOW.minusDays(1),
                ENROLLMENT_END_AT,
                NOW.plusDays(10),
                NOW.plusDays(20)
        );
        entityManager.persist(course);
        entityManager.flush();
        return course;
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
