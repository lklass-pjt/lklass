package com.lklass.domain.enrollment.entity;

import com.lklass.global.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.Objects;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "enrollments")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Enrollment extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "course_id", nullable = false)
    private Long courseId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EnrollmentStatus status;

    @Column(name = "enrolled_at", nullable = false)
    private LocalDateTime enrolledAt;

    @Column(name = "confirmed_at")
    private LocalDateTime confirmedAt;

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    private Enrollment(Long courseId, Long userId, LocalDateTime enrolledAt) {
        this.courseId = Objects.requireNonNull(courseId, "courseId must not be null");
        this.userId = Objects.requireNonNull(userId, "userId must not be null");
        this.status = EnrollmentStatus.PENDING;
        this.enrolledAt = Objects.requireNonNull(enrolledAt, "enrolledAt must not be null");
    }

    public static Enrollment create(Long courseId, Long userId, LocalDateTime enrolledAt) {
        return new Enrollment(courseId, userId, enrolledAt);
    }
}
