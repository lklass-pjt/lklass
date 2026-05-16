package com.lklass.domain.course.entity;

import jakarta.persistence.Embeddable;
import java.util.Objects;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Embeddable
@EqualsAndHashCode
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CourseStatusChangedBy {

    private static final String SYSTEM = "SYSTEM";
    private static final String USER_PREFIX = "USER:";

    private String value;

    private CourseStatusChangedBy(String value) {
        this.value = Objects.requireNonNull(value, "changedBy must not be null");
    }

    public static CourseStatusChangedBy user(Long userId) {
        Objects.requireNonNull(userId, "userId must not be null");
        return new CourseStatusChangedBy(USER_PREFIX + userId);
    }

    public static CourseStatusChangedBy system() {
        return new CourseStatusChangedBy(SYSTEM);
    }
}
