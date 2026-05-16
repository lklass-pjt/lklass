package com.lklass.domain.enrollment.entity;

import com.lklass.global.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.util.Objects;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "active_enrollments")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ActiveEnrollment extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "course_id", nullable = false)
    private Long courseId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    // MySQL에서 partial unique index를 쓰지 않고 활성 신청(PENDING/CONFIRMED) 중복을 막기 위한 점유 row다.
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "enrollment_id", nullable = false)
    private Enrollment enrollment;

    private ActiveEnrollment(Enrollment enrollment) {
        this.enrollment = Objects.requireNonNull(enrollment, "enrollment must not be null");
        this.courseId = Objects.requireNonNull(enrollment.getCourseId(), "courseId must not be null");
        this.userId = Objects.requireNonNull(enrollment.getUserId(), "userId must not be null");
    }

    public static ActiveEnrollment create(Enrollment enrollment) {
        return new ActiveEnrollment(enrollment);
    }
}
