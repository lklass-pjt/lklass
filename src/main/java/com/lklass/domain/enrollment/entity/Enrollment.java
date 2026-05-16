package com.lklass.domain.enrollment.entity;

import com.lklass.domain.enrollment.exception.EnrollmentErrorCode;
import com.lklass.global.entity.BaseTimeEntity;
import com.lklass.global.exception.BusinessException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Duration;
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

    public void confirm(LocalDateTime confirmedAt) {
        Objects.requireNonNull(confirmedAt, "confirmedAt must not be null");
        if (status != EnrollmentStatus.PENDING) {
            throw new BusinessException(EnrollmentErrorCode.INVALID_ENROLLMENT_STATUS);
        }
        this.status = EnrollmentStatus.CONFIRMED;
        this.confirmedAt = confirmedAt;
    }

    public void cancel(LocalDateTime cancelledAt, Duration cancellationPeriod) {
        LocalDateTime checkedCancelledAt = Objects.requireNonNull(cancelledAt, "cancelledAt must not be null");
        Duration checkedCancellationPeriod = Objects.requireNonNull(cancellationPeriod, "cancellationPeriod must not be null");

        if (status == EnrollmentStatus.PENDING) {
            cancelAt(checkedCancelledAt);
            return;
        }
        if (status == EnrollmentStatus.CONFIRMED) {
            if (checkedCancelledAt.isAfter(confirmedAt.plus(checkedCancellationPeriod))) {
                throw new BusinessException(EnrollmentErrorCode.CANCELLATION_PERIOD_EXPIRED);
            }
            cancelAt(checkedCancelledAt);
            return;
        }
        throw new BusinessException(EnrollmentErrorCode.INVALID_ENROLLMENT_STATUS);
    }

    public void expire(LocalDateTime expiredAt) {
        Objects.requireNonNull(expiredAt, "expiredAt must not be null");
        if (status != EnrollmentStatus.PENDING) {
            throw new BusinessException(EnrollmentErrorCode.INVALID_ENROLLMENT_STATUS);
        }
        cancelAt(expiredAt);
    }

    private void cancelAt(LocalDateTime cancelledAt) {
        this.status = EnrollmentStatus.CANCELLED;
        this.cancelledAt = cancelledAt;
    }
}
