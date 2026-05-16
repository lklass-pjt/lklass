package com.lklass.domain.course.repository;

import com.lklass.domain.course.entity.Course;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class CourseRepository {

    private final CourseJpaRepository courseJpaRepository;

    public Course save(Course course) {
        return courseJpaRepository.save(course);
    }

    public Optional<Course> findById(Long courseId) {
        return courseJpaRepository.findById(courseId);
    }
}
