package com.lklass.domain.enrollment.entity;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.Objects;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "enrollment_status_histories")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class EnrollmentStatusHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "enrollment_id", nullable = false)
    private Enrollment enrollment;

    @Enumerated(EnumType.STRING)
    @Column(name = "from_status", length = 20)
    private EnrollmentStatus fromStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "to_status", nullable = false, length = 20)
    private EnrollmentStatus toStatus;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private EnrollmentStatusChangeReason reason;

    @Column(name = "changed_at", nullable = false)
    private LocalDateTime changedAt;

    @Embedded
    @AttributeOverride(name = "value", column = @Column(name = "changed_by", nullable = false, length = 100))
    private EnrollmentStatusChangedBy changedBy;

    private EnrollmentStatusHistory(
            Enrollment enrollment,
            EnrollmentStatus fromStatus,
            EnrollmentStatus toStatus,
            EnrollmentStatusChangeReason reason,
            LocalDateTime changedAt,
            EnrollmentStatusChangedBy changedBy
    ) {
        this.enrollment = Objects.requireNonNull(enrollment, "enrollment must not be null");
        this.fromStatus = fromStatus;
        this.toStatus = Objects.requireNonNull(toStatus, "toStatus must not be null");
        this.reason = Objects.requireNonNull(reason, "reason must not be null");
        this.changedAt = Objects.requireNonNull(changedAt, "changedAt must not be null");
        this.changedBy = Objects.requireNonNull(changedBy, "changedBy must not be null");
    }

    public static EnrollmentStatusHistory record(
            Enrollment enrollment,
            EnrollmentStatus fromStatus,
            EnrollmentStatus toStatus,
            EnrollmentStatusChangeReason reason,
            LocalDateTime changedAt,
            EnrollmentStatusChangedBy changedBy
    ) {
        return new EnrollmentStatusHistory(enrollment, fromStatus, toStatus, reason, changedAt, changedBy);
    }
}
