package com.lklass.domain.enrollment.entity;

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
public class EnrollmentStatusChangedBy {

    private static final String SYSTEM = "SYSTEM";
    private static final String USER_PREFIX = "USER:";

    private String value;

    private EnrollmentStatusChangedBy(String value) {
        this.value = Objects.requireNonNull(value, "changedBy must not be null");
    }

    public static EnrollmentStatusChangedBy user(Long userId) {
        Objects.requireNonNull(userId, "userId must not be null");
        return new EnrollmentStatusChangedBy(USER_PREFIX + userId);
    }

    public static EnrollmentStatusChangedBy system() {
        return new EnrollmentStatusChangedBy(SYSTEM);
    }
}
