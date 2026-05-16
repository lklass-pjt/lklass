package com.lklass.domain.course.repository;

import com.lklass.domain.course.dto.CourseQueryResult;
import com.lklass.domain.course.entity.Course;
import com.lklass.domain.course.entity.CourseStatus;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

    public Page<CourseQueryResult> findAll(CourseStatus status, Pageable pageable) {
        return courseJpaRepository.findCourseResults(status, pageable);
    }

    public Optional<CourseQueryResult> findResultById(Long courseId) {
        return courseJpaRepository.findCourseResultById(courseId);
    }

    public boolean existsByIdAndCreatorId(Long courseId, Long creatorId) {
        return courseJpaRepository.existsByIdAndCreatorId(courseId, creatorId);
    }
}
