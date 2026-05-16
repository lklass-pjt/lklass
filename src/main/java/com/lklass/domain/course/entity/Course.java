package com.lklass.domain.course.entity;

import com.lklass.global.entity.BaseTimeEntity;
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

    private static String requireNotBlank(String value, String fieldName) {
        String checkedValue = Objects.requireNonNull(value, fieldName + " must not be null");
        if (checkedValue.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return checkedValue;
    }
}
