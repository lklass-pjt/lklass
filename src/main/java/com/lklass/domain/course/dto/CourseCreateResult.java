package com.lklass.domain.course.dto;

import com.lklass.domain.course.entity.Course;
import com.lklass.domain.course.entity.CourseStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CourseCreateResult(
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

    public static CourseCreateResult from(Course course) {
        return new CourseCreateResult(
                course.getId(),
                course.getCreatorId(),
                course.getTitle(),
                course.getDescription(),
                course.getPrice().getAmount(),
                course.getCapacity().getValue(),
                course.getOccupiedCount(),
                course.getStatus(),
                course.isAutoPublishEnabled(),
                course.getEnrollmentPeriod().getStartAt(),
                course.getEnrollmentPeriod().getEndAt(),
                course.getCoursePeriod().getStartAt(),
                course.getCoursePeriod().getEndAt()
        );
    }
}
