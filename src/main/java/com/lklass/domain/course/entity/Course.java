package com.lklass.domain.course.entity;

import com.lklass.domain.course.exception.CourseErrorCode;
import com.lklass.global.entity.BaseTimeEntity;
import com.lklass.global.exception.BusinessException;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "courses")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Course extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "creator_id", nullable = false)
    private Long creatorId;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Embedded
    @AttributeOverride(name = "amount", column = @Column(name = "price", nullable = false, precision = 12, scale = 2))
    private CoursePrice price;

    @Embedded
    @AttributeOverride(name = "value", column = @Column(name = "capacity", nullable = false))
    private CourseCapacity capacity;

    @Column(name = "occupied_count", nullable = false)
    private int occupiedCount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CourseStatus status;

    @Column(name = "auto_publish_enabled", nullable = false)
    private boolean autoPublishEnabled;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "startAt", column = @Column(name = "enrollment_start_at", nullable = false)),
            @AttributeOverride(name = "endAt", column = @Column(name = "enrollment_end_at", nullable = false))
    })
    private CoursePeriod enrollmentPeriod;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "startAt", column = @Column(name = "course_start_at", nullable = false)),
            @AttributeOverride(name = "endAt", column = @Column(name = "course_end_at", nullable = false))
    })
    private CoursePeriod coursePeriod;

    private Course(
            Long creatorId,
            String title,
            String description,
            BigDecimal price,
            int capacity,
            LocalDateTime enrollmentStartAt,
            LocalDateTime enrollmentEndAt,
            LocalDateTime courseStartAt,
            LocalDateTime courseEndAt
    ) {
        this.creatorId = Objects.requireNonNull(creatorId, "creatorId must not be null");
        this.title = requireNotBlank(title, "title");
        this.description = requireNotBlank(description, "description");
        this.price = CoursePrice.of(price);
        this.capacity = CourseCapacity.of(capacity);
        this.occupiedCount = 0;
        this.status = CourseStatus.DRAFT;
        this.autoPublishEnabled = false;
        this.enrollmentPeriod = CoursePeriod.enrollment(enrollmentStartAt, enrollmentEndAt);
        this.coursePeriod = CoursePeriod.course(courseStartAt, courseEndAt);
    }

    public static Course create(
            Long creatorId,
            String title,
            String description,
            BigDecimal price,
            int capacity,
            LocalDateTime enrollmentStartAt,
            LocalDateTime enrollmentEndAt,
            LocalDateTime courseStartAt,
            LocalDateTime courseEndAt
    ) {
        return new Course(
                creatorId,
                title,
                description,
                price,
                capacity,
                enrollmentStartAt,
                enrollmentEndAt,
                courseStartAt,
                courseEndAt
        );
    }

    public void openManually(LocalDateTime now, LocalDateTime enrollmentEndAt) {
        if (status != CourseStatus.DRAFT) {
            throw new BusinessException(CourseErrorCode.INVALID_COURSE_STATUS_TRANSITION);
        }
        // 수동 OPEN은 운영자가 지금 모집을 시작하는 행위이므로 모집 시작 시각을 현재 시각으로 재설정한다.
        if (!Objects.requireNonNull(now, "now must not be null")
                .isBefore(Objects.requireNonNull(enrollmentEndAt, "enrollmentEndAt must not be null"))) {
            throw new BusinessException(CourseErrorCode.ENROLLMENT_CLOSED);
        }
        if (!enrollmentEndAt.isBefore(coursePeriod.getStartAt())) {
            throw new BusinessException(CourseErrorCode.INVALID_ENROLLMENT_PERIOD);
        }
        enrollmentPeriod = CoursePeriod.enrollment(now, enrollmentEndAt);
        status = CourseStatus.OPEN;
    }

    public void close() {
        if (status != CourseStatus.OPEN) {
            throw new BusinessException(CourseErrorCode.INVALID_COURSE_STATUS_TRANSITION);
        }
        status = CourseStatus.CLOSED;
    }

    private static String requireNotBlank(String value, String fieldName) {
        String checkedValue = Objects.requireNonNull(value, fieldName + " must not be null");
        if (checkedValue.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return checkedValue;
    }
}
