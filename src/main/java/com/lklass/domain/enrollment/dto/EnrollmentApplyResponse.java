package com.lklass.domain.enrollment.dto;

import com.lklass.domain.enrollment.entity.EnrollmentStatus;
import java.time.LocalDateTime;

public record EnrollmentApplyResponse(
        Long id,
        Long courseId,
        Long userId,
        EnrollmentStatus status,
        LocalDateTime enrolledAt
) {

    public static EnrollmentApplyResponse from(EnrollmentApplyResult result) {
        return new EnrollmentApplyResponse(
                result.id(),
                result.courseId(),
                result.userId(),
                result.status(),
                result.enrolledAt()
        );
    }
}
