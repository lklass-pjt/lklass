package com.lklass.domain.course.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.lklass.domain.course.exception.CourseErrorCode;
import com.lklass.global.exception.BusinessException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class CourseEntityTest {

    private static final LocalDateTime ENROLLMENT_START_AT = LocalDateTime.of(2026, 5, 20, 10, 0);
    private static final LocalDateTime ENROLLMENT_END_AT = LocalDateTime.of(2026, 5, 27, 18, 0);
    private static final LocalDateTime COURSE_START_AT = LocalDateTime.of(2026, 6, 1, 9, 0);
    private static final LocalDateTime COURSE_END_AT = LocalDateTime.of(2026, 6, 30, 18, 0);

    @Test
    @DisplayName("Course는 생성 시 DRAFT 상태와 0명의 현재 신청 인원을 가진다")
    void createDraftCourse() {
        // given
        Long creatorId = 1L;

        // when
        Course course = createCourse(creatorId);

        // then
        assertThat(course.getCreatorId()).isEqualTo(creatorId);
        assertThat(course.getTitle()).isEqualTo("스프링 입문");
        assertThat(course.getDescription()).isEqualTo("스프링 부트와 JPA 기초 강의");
        assertThat(course.getPrice().getAmount()).isEqualByComparingTo("10000.00");
        assertThat(course.getCapacity().getValue()).isEqualTo(30);
        assertThat(course.getOccupiedCount()).isZero();
        assertThat(course.getStatus()).isEqualTo(CourseStatus.DRAFT);
        assertThat(course.isAutoPublishEnabled()).isFalse();
        assertThat(course.getEnrollmentPeriod().getStartAt()).isEqualTo(ENROLLMENT_START_AT);
        assertThat(course.getEnrollmentPeriod().getEndAt()).isEqualTo(ENROLLMENT_END_AT);
        assertThat(course.getCoursePeriod().getStartAt()).isEqualTo(COURSE_START_AT);
        assertThat(course.getCoursePeriod().getEndAt()).isEqualTo(COURSE_END_AT);
    }

    @Test
    @DisplayName("Course는 필수 필드가 null이면 생성할 수 없다")
    void rejectNullRequiredFields() {
        // given
        Long creatorId = 1L;
        String title = "스프링 입문";
        String description = "스프링 부트와 JPA 기초 강의";
        BigDecimal price = new BigDecimal("10000.00");
        int capacity = 30;

        // when & then
        assertThatThrownBy(() -> Course.create(
                null, title, description, price, capacity,
                ENROLLMENT_START_AT, ENROLLMENT_END_AT, COURSE_START_AT, COURSE_END_AT
        )).isInstanceOf(NullPointerException.class)
                .hasMessage("creatorId must not be null");
        assertThatThrownBy(() -> Course.create(
                creatorId, null, description, price, capacity,
                ENROLLMENT_START_AT, ENROLLMENT_END_AT, COURSE_START_AT, COURSE_END_AT
        )).isInstanceOf(NullPointerException.class)
                .hasMessage("title must not be null");
        assertThatThrownBy(() -> Course.create(
                creatorId, title, null, price, capacity,
                ENROLLMENT_START_AT, ENROLLMENT_END_AT, COURSE_START_AT, COURSE_END_AT
        )).isInstanceOf(NullPointerException.class)
                .hasMessage("description must not be null");
        assertThatThrownBy(() -> Course.create(
                creatorId, title, description, null, capacity,
                ENROLLMENT_START_AT, ENROLLMENT_END_AT, COURSE_START_AT, COURSE_END_AT
        )).isInstanceOf(NullPointerException.class)
                .hasMessage("price must not be null");
    }

    @Test
    @DisplayName("Course는 가격이 0보다 작으면 생성할 수 없다")
    void rejectNegativePrice() {
        // given
        BigDecimal negativePrice = new BigDecimal("-1.00");

        // when & then
        assertThatThrownBy(() -> Course.create(
                1L, "스프링 입문", "스프링 부트와 JPA 기초 강의", negativePrice, 30,
                ENROLLMENT_START_AT, ENROLLMENT_END_AT, COURSE_START_AT, COURSE_END_AT
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessage("price must be greater than or equal to 0");
    }

    @Test
    @DisplayName("CoursePrice는 금액을 소수점 2자리로 정규화해 같은 금액을 같은 값으로 비교한다")
    void createCoursePrice() {
        // given
        CoursePrice price = CoursePrice.of(new BigDecimal("10000.0"));
        CoursePrice samePrice = CoursePrice.of(new BigDecimal("10000.00"));

        // then
        assertThat(price.getAmount()).isEqualByComparingTo("10000.00");
        assertThat(price.getAmount().scale()).isEqualTo(2);
        assertThat(price).isEqualTo(samePrice);
    }

    @Test
    @DisplayName("CoursePrice는 소수점 2자리를 초과한 가격이면 생성할 수 없다")
    void rejectPriceScaleOverTwo() {
        // given
        BigDecimal price = new BigDecimal("10000.001");

        // when & then
        assertThatThrownBy(() -> CoursePrice.of(price))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("price scale must be less than or equal to 2");
    }

    @Test
    @DisplayName("Course는 제목이나 설명이 공백이면 생성할 수 없다")
    void rejectBlankTitleAndDescription() {
        // given
        String blankTitle = "";
        String blankDescription = "   ";

        // when & then
        assertThatThrownBy(() -> Course.create(
                1L, blankTitle, "스프링 부트와 JPA 기초 강의", new BigDecimal("10000.00"), 30,
                ENROLLMENT_START_AT, ENROLLMENT_END_AT, COURSE_START_AT, COURSE_END_AT
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessage("title must not be blank");
        assertThatThrownBy(() -> Course.create(
                1L, "스프링 입문", blankDescription, new BigDecimal("10000.00"), 30,
                ENROLLMENT_START_AT, ENROLLMENT_END_AT, COURSE_START_AT, COURSE_END_AT
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessage("description must not be blank");
    }

    @Test
    @DisplayName("Course는 정원이 1보다 작으면 생성할 수 없다")
    void rejectInvalidCapacity() {
        // given
        int invalidCapacity = 0;

        // when & then
        assertThatThrownBy(() -> Course.create(
                1L, "스프링 입문", "스프링 부트와 JPA 기초 강의", new BigDecimal("10000.00"), invalidCapacity,
                ENROLLMENT_START_AT, ENROLLMENT_END_AT, COURSE_START_AT, COURSE_END_AT
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessage("capacity must be greater than or equal to 1");
    }

    @Test
    @DisplayName("CourseCapacity는 최소 정원 1명을 허용하고 값으로 동등성을 비교한다")
    void createCourseCapacity() {
        // when
        CourseCapacity capacity = CourseCapacity.of(1);
        CourseCapacity sameCapacity = CourseCapacity.of(1);

        // then
        assertThat(capacity.getValue()).isEqualTo(1);
        assertThat(capacity).isEqualTo(sameCapacity);
    }

    @Test
    @DisplayName("Course는 모집 시작 시각이 모집 종료 시각보다 빠르지 않으면 생성할 수 없다")
    void rejectInvalidEnrollmentPeriod() {
        // given
        LocalDateTime invalidEnrollmentEndAt = ENROLLMENT_START_AT;

        // when & then
        assertThatThrownBy(() -> Course.create(
                1L, "스프링 입문", "스프링 부트와 JPA 기초 강의", new BigDecimal("10000.00"), 30,
                ENROLLMENT_START_AT, invalidEnrollmentEndAt, COURSE_START_AT, COURSE_END_AT
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessage("enrollmentStartAt must be before enrollmentEndAt");
    }

    @Test
    @DisplayName("Course는 수강 시작 시각이 수강 종료 시각보다 빠르지 않으면 생성할 수 없다")
    void rejectInvalidCoursePeriod() {
        // given
        LocalDateTime invalidCourseEndAt = COURSE_START_AT;

        // when & then
        assertThatThrownBy(() -> Course.create(
                1L, "스프링 입문", "스프링 부트와 JPA 기초 강의", new BigDecimal("10000.00"), 30,
                ENROLLMENT_START_AT, ENROLLMENT_END_AT, COURSE_START_AT, invalidCourseEndAt
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessage("courseStartAt must be before courseEndAt");
    }

    @Test
    @DisplayName("CoursePeriod는 시작/종료 시각을 보존하고 값으로 동등성을 비교한다")
    void createCoursePeriod() {
        // when
        CoursePeriod enrollmentPeriod = CoursePeriod.enrollment(ENROLLMENT_START_AT, ENROLLMENT_END_AT);
        CoursePeriod sameEnrollmentPeriod = CoursePeriod.enrollment(ENROLLMENT_START_AT, ENROLLMENT_END_AT);
        CoursePeriod coursePeriod = CoursePeriod.course(COURSE_START_AT, COURSE_END_AT);

        // then
        assertThat(enrollmentPeriod.getStartAt()).isEqualTo(ENROLLMENT_START_AT);
        assertThat(enrollmentPeriod.getEndAt()).isEqualTo(ENROLLMENT_END_AT);
        assertThat(enrollmentPeriod).isEqualTo(sameEnrollmentPeriod);
        assertThat(coursePeriod.getStartAt()).isEqualTo(COURSE_START_AT);
        assertThat(coursePeriod.getEndAt()).isEqualTo(COURSE_END_AT);
    }

    @Test
    @DisplayName("CourseStatusHistory는 상태 변경 이력에 필요한 값을 보존한다")
    void recordCourseStatusHistory() {
        // given
        LocalDateTime changedAt = LocalDateTime.of(2026, 5, 20, 10, 0);
        Course course = createCourse(1L);

        // when
        CourseStatusHistory history = CourseStatusHistory.record(
                course,
                null,
                CourseStatus.DRAFT,
                CourseStatusChangeReason.CREATED,
                changedAt,
                CourseStatusChangedBy.user(1L)
        );

        // then
        assertThat(history.getCourse()).isEqualTo(course);
        assertThat(history.getFromStatus()).isNull();
        assertThat(history.getToStatus()).isEqualTo(CourseStatus.DRAFT);
        assertThat(history.getReason()).isEqualTo(CourseStatusChangeReason.CREATED);
        assertThat(history.getChangedAt()).isEqualTo(changedAt);
        assertThat(history.getChangedBy()).isEqualTo(CourseStatusChangedBy.user(1L));
    }

    @Test
    @DisplayName("Course는 수동 OPEN 시 모집 시작일을 현재 시각으로 바꾸고 요청한 모집 마감일로 OPEN 상태가 된다")
    void openDraftCourse() {
        // given
        LocalDateTime now = LocalDateTime.of(2026, 5, 16, 10, 0);
        LocalDateTime manualEnrollmentEndAt = LocalDateTime.of(2026, 5, 30, 18, 0);
        Course course = createCourse(1L);

        // when
        course.openManually(now, manualEnrollmentEndAt);

        // then
        assertThat(course.getStatus()).isEqualTo(CourseStatus.OPEN);
        assertThat(course.getEnrollmentPeriod().getStartAt()).isEqualTo(now);
        assertThat(course.getEnrollmentPeriod().getEndAt()).isEqualTo(manualEnrollmentEndAt);
    }

    @Test
    @DisplayName("Course는 OPEN에서 CLOSED로 상태를 변경할 수 있다")
    void closeOpenCourse() {
        // given
        LocalDateTime now = LocalDateTime.of(2026, 5, 16, 10, 0);
        LocalDateTime manualEnrollmentEndAt = LocalDateTime.of(2026, 5, 30, 18, 0);
        Course course = createCourse(1L);
        course.openManually(now, manualEnrollmentEndAt);

        // when
        course.close();

        // then
        assertThat(course.getStatus()).isEqualTo(CourseStatus.CLOSED);
    }

    @Test
    @DisplayName("Course는 DRAFT 상태에서 자동 게시 예약을 켤 수 있다")
    void reserveAutoPublish() {
        // given
        LocalDateTime now = LocalDateTime.of(2026, 5, 16, 10, 0);
        Course course = createCourse(1L);

        // when
        course.reserveAutoPublish(now);

        // then
        assertThat(course.isAutoPublishEnabled()).isTrue();
        assertThat(course.getStatus()).isEqualTo(CourseStatus.DRAFT);
    }

    @Test
    @DisplayName("Course는 DRAFT가 아니면 자동 게시 예약을 켤 수 없다")
    void rejectAutoPublishReservationForNonDraftCourse() {
        // given
        LocalDateTime now = LocalDateTime.of(2026, 5, 16, 10, 0);
        LocalDateTime manualEnrollmentEndAt = LocalDateTime.of(2026, 5, 30, 18, 0);
        Course course = createCourse(1L);
        course.openManually(now, manualEnrollmentEndAt);

        // when & then
        assertThatThrownBy(() -> course.reserveAutoPublish(now))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.errorCode())
                                .isEqualTo(CourseErrorCode.INVALID_COURSE_STATUS_TRANSITION)
                );
    }

    @Test
    @DisplayName("Course는 모집 마감이 지난 뒤에는 자동 게시 예약을 켤 수 없다")
    void rejectAutoPublishReservationAfterEnrollmentClosed() {
        // given
        LocalDateTime now = ENROLLMENT_END_AT;
        Course course = createCourse(1L);

        // when & then
        assertThatThrownBy(() -> course.reserveAutoPublish(now))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.errorCode()).isEqualTo(CourseErrorCode.ENROLLMENT_CLOSED)
                );
    }

    @Test
    @DisplayName("Course는 모집 마감일이 수강 시작일 이전이 아니면 자동 게시 예약을 켤 수 없다")
    void rejectAutoPublishReservationWithEnrollmentEndAtAfterCourseStartAt() {
        // given
        LocalDateTime now = LocalDateTime.of(2026, 5, 16, 10, 0);
        Course course = Course.create(
                1L,
                "스프링 입문",
                "스프링 부트와 JPA 기초 강의",
                new BigDecimal("10000.00"),
                30,
                ENROLLMENT_START_AT,
                COURSE_START_AT,
                COURSE_START_AT,
                COURSE_END_AT
        );

        // when & then
        assertThatThrownBy(() -> course.reserveAutoPublish(now))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.errorCode()).isEqualTo(CourseErrorCode.INVALID_ENROLLMENT_PERIOD)
                );
    }

    @Test
    @DisplayName("Course는 DRAFT에서 바로 CLOSED로 변경할 수 없다")
    void rejectCloseDraftCourse() {
        // given
        Course course = createCourse(1L);

        // when & then
        assertThatThrownBy(course::close)
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.errorCode())
                                .isEqualTo(CourseErrorCode.INVALID_COURSE_STATUS_TRANSITION)
                );
    }

    @Test
    @DisplayName("Course는 CLOSED에서 다시 OPEN으로 변경할 수 없다")
    void rejectOpenClosedCourse() {
        // given
        LocalDateTime now = LocalDateTime.of(2026, 5, 16, 10, 0);
        LocalDateTime manualEnrollmentEndAt = LocalDateTime.of(2026, 5, 30, 18, 0);
        Course course = createCourse(1L);
        course.openManually(now, manualEnrollmentEndAt);
        course.close();

        // when & then
        assertThatThrownBy(() -> course.openManually(now, manualEnrollmentEndAt))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.errorCode())
                                .isEqualTo(CourseErrorCode.INVALID_COURSE_STATUS_TRANSITION)
                );
    }

    @Test
    @DisplayName("Course는 수동 OPEN 모집 마감일이 현재 시각 이후가 아니면 열 수 없다")
    void rejectManualOpenWithPastEnrollmentEndAt() {
        // given
        LocalDateTime now = LocalDateTime.of(2026, 5, 16, 10, 0);
        Course course = createCourse(1L);

        // when & then
        assertThatThrownBy(() -> course.openManually(now, now))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.errorCode()).isEqualTo(CourseErrorCode.ENROLLMENT_CLOSED)
                );
    }

    @Test
    @DisplayName("Course는 수동 OPEN 모집 마감일이 수강 시작일 이전이 아니면 열 수 없다")
    void rejectManualOpenWithEnrollmentEndAtAfterCourseStartAt() {
        // given
        LocalDateTime now = LocalDateTime.of(2026, 5, 16, 10, 0);
        Course course = createCourse(1L);

        // when & then
        assertThatThrownBy(() -> course.openManually(now, COURSE_START_AT))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.errorCode()).isEqualTo(CourseErrorCode.INVALID_ENROLLMENT_PERIOD)
                );
    }

    @Test
    @DisplayName("CourseStatusChangedBy는 사용자와 시스템 변경 주체를 문자열 값으로 표현한다")
    void createCourseStatusChangedBy() {
        // when
        CourseStatusChangedBy userChangedBy = CourseStatusChangedBy.user(1L);
        CourseStatusChangedBy sameUserChangedBy = CourseStatusChangedBy.user(1L);
        CourseStatusChangedBy systemChangedBy = CourseStatusChangedBy.system();

        // then
        assertThat(userChangedBy.getValue()).isEqualTo("USER:1");
        assertThat(userChangedBy).isEqualTo(sameUserChangedBy);
        assertThat(systemChangedBy.getValue()).isEqualTo("SYSTEM");
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
}
