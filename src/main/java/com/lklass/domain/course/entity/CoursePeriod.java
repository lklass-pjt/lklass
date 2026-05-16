package com.lklass.domain.course.entity;

import jakarta.persistence.Embeddable;
import java.time.LocalDateTime;
import java.util.Objects;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Embeddable
@EqualsAndHashCode
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CoursePeriod {

    private LocalDateTime startAt;
    private LocalDateTime endAt;

    private CoursePeriod(LocalDateTime startAt, LocalDateTime endAt, String message) {
        this.startAt = Objects.requireNonNull(startAt, "period startAt must not be null");
        this.endAt = Objects.requireNonNull(endAt, "period endAt must not be null");
        validate(this.startAt, this.endAt, message);
    }

    public static CoursePeriod enrollment(LocalDateTime startAt, LocalDateTime endAt) {
        return new CoursePeriod(startAt, endAt, "enrollmentStartAt must be before enrollmentEndAt");
    }

    public static CoursePeriod course(LocalDateTime startAt, LocalDateTime endAt) {
        return new CoursePeriod(startAt, endAt, "courseStartAt must be before courseEndAt");
    }

    private static void validate(LocalDateTime startAt, LocalDateTime endAt, String message) {
        if (!startAt.isBefore(endAt)) {
            throw new IllegalArgumentException(message);
        }
    }
}
