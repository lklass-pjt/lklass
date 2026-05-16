package com.lklass.domain.enrollment.dto;

import com.lklass.domain.enrollment.entity.EnrollmentStatus;
import java.time.LocalDateTime;

public record EnrollmentQueryResult(
        Long id,
        Long courseId,
        String courseTitle,
        Long userId,
        String userName,
        String userEmail,
        EnrollmentStatus status,
        LocalDateTime enrolledAt,
        LocalDateTime confirmedAt,
        LocalDateTime cancelledAt
) {
}
