package com.lklass.domain.course.entity;

import jakarta.persistence.Embeddable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Embeddable
@EqualsAndHashCode
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CoursePrice {

    private static final int SCALE = 2;

    private BigDecimal amount;

    private CoursePrice(BigDecimal amount) {
        this.amount = validate(amount);
    }

    public static CoursePrice of(BigDecimal amount) {
        return new CoursePrice(amount);
    }

    private static BigDecimal validate(BigDecimal amount) {
        BigDecimal checkedAmount = Objects.requireNonNull(amount, "price must not be null");
        if (checkedAmount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("price must be greater than or equal to 0");
        }

        try {
            return checkedAmount.setScale(SCALE, RoundingMode.UNNECESSARY);
        } catch (ArithmeticException exception) {
            throw new IllegalArgumentException("price scale must be less than or equal to 2");
        }
    }
}
