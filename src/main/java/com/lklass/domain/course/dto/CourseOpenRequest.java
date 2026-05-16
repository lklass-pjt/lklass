package com.lklass.domain.course.dto;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

public record CourseOpenRequest(
        @NotNull(message = "enrollmentEndAt은 필수입니다.")
        LocalDateTime enrollmentEndAt
) {
}
