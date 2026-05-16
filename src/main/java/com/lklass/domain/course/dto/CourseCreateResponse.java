package com.lklass.domain.course.dto;

import com.lklass.domain.course.entity.CourseStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CourseCreateResponse(
        Long id,
        Long creatorId,
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

    public static CourseCreateResponse from(CourseCreateResult result) {
        return new CourseCreateResponse(
                result.id(),
                result.creatorId(),
                result.title(),
                result.description(),
                result.price(),
                result.capacity(),
                result.occupiedCount(),
                result.status(),
                result.autoPublishEnabled(),
                result.enrollmentStartAt(),
                result.enrollmentEndAt(),
                result.courseStartAt(),
                result.courseEndAt()
        );
    }
}
