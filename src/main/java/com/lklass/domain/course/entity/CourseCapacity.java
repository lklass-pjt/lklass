package com.lklass.domain.course.entity;

import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Embeddable
@EqualsAndHashCode
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CourseCapacity {

    private int value;

    private CourseCapacity(int value) {
        this.value = validate(value);
    }

    public static CourseCapacity of(int value) {
        return new CourseCapacity(value);
    }

    private static int validate(int value) {
        if (value < 1) {
            throw new IllegalArgumentException("capacity must be greater than or equal to 1");
        }
        return value;
    }
}
