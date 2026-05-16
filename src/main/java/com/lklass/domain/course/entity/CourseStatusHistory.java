package com.lklass.domain.course.entity;

import jakarta.persistence.Column;
import jakarta.persistence.AttributeOverride;
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
@Table(name = "course_status_histories")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CourseStatusHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @Enumerated(EnumType.STRING)
    @Column(name = "from_status", length = 20)
    private CourseStatus fromStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "to_status", nullable = false, length = 20)
    private CourseStatus toStatus;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private CourseStatusChangeReason reason;

    @Column(name = "changed_at", nullable = false)
    private LocalDateTime changedAt;

    @Embedded
    @AttributeOverride(name = "value", column = @Column(name = "changed_by", nullable = false, length = 100))
    private CourseStatusChangedBy changedBy;

    private CourseStatusHistory(
            Course course,
            CourseStatus fromStatus,
            CourseStatus toStatus,
            CourseStatusChangeReason reason,
            LocalDateTime changedAt,
            CourseStatusChangedBy changedBy
    ) {
        this.course = Objects.requireNonNull(course, "course must not be null");
        this.fromStatus = fromStatus;
        this.toStatus = Objects.requireNonNull(toStatus, "toStatus must not be null");
        this.reason = Objects.requireNonNull(reason, "reason must not be null");
        this.changedAt = Objects.requireNonNull(changedAt, "changedAt must not be null");
        this.changedBy = Objects.requireNonNull(changedBy, "changedBy must not be null");
    }

    public static CourseStatusHistory record(
            Course course,
            CourseStatus fromStatus,
            CourseStatus toStatus,
            CourseStatusChangeReason reason,
            LocalDateTime changedAt,
            CourseStatusChangedBy changedBy
    ) {
        return new CourseStatusHistory(course, fromStatus, toStatus, reason, changedAt, changedBy);
    }
}
