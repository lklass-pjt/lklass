package com.lklass.domain.enrollment.dto;

import com.lklass.domain.enrollment.entity.Enrollment;
import com.lklass.domain.enrollment.entity.EnrollmentStatus;
import java.time.LocalDateTime;

public record EnrollmentApplyResult(
        Long id,
        Long courseId,
        Long userId,
        EnrollmentStatus status,
        LocalDateTime enrolledAt
) {

    public static EnrollmentApplyResult from(Enrollment enrollment) {
        return new EnrollmentApplyResult(
                enrollment.getId(),
                enrollment.getCourseId(),
                enrollment.getUserId(),
                enrollment.getStatus(),
                enrollment.getEnrolledAt()
        );
    }
}
