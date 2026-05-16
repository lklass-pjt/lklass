package com.lklass.domain.course.dto;

import com.lklass.domain.course.entity.CourseStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CourseQueryResult(
        Long id,
        Long creatorId,
        String creatorName,
        String title,
        String description,
        BigDecimal price,
        int capacity,
        int occupiedCount,
        CourseStatus status,
        boolean autoPublishEnabled,
        LocalDateTime enrollmentStartAt,
        LocalDateTime enrollmentEndAt,
        LocalDateTime courseStartAt,
        LocalDateTime courseEndAt
) {
}
