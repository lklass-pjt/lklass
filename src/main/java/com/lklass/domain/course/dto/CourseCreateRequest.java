package com.lklass.domain.course.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CourseCreateRequest(
        Long creatorId,

        @NotBlank(message = "title은 필수입니다.")
        String title,

        @NotBlank(message = "description은 필수입니다.")
        String description,

        @NotNull(message = "price는 필수입니다.")
        @DecimalMin(value = "0.00", message = "price는 0 이상이어야 합니다.")
        @Digits(integer = 10, fraction = 2, message = "price는 정수 10자리, 소수 2자리까지 입력할 수 있습니다.")
        BigDecimal price,

        @Min(value = 1, message = "capacity는 1 이상이어야 합니다.")
        int capacity,

        @NotNull(message = "enrollmentStartAt은 필수입니다.")
        LocalDateTime enrollmentStartAt,

        @NotNull(message = "enrollmentEndAt은 필수입니다.")
        LocalDateTime enrollmentEndAt,

        @NotNull(message = "courseStartAt은 필수입니다.")
        LocalDateTime courseStartAt,

        @NotNull(message = "courseEndAt은 필수입니다.")
        LocalDateTime courseEndAt
) {

    @AssertTrue(message = "enrollmentStartAt은 enrollmentEndAt보다 이전이어야 합니다.")
    public boolean isEnrollmentPeriodValid() {
        if (enrollmentStartAt == null || enrollmentEndAt == null) {
            return true;
        }
        return enrollmentStartAt.isBefore(enrollmentEndAt);
    }

    @AssertTrue(message = "courseStartAt은 courseEndAt보다 이전이어야 합니다.")
    public boolean isCoursePeriodValid() {
        if (courseStartAt == null || courseEndAt == null) {
            return true;
        }
        return courseStartAt.isBefore(courseEndAt);
    }
}
