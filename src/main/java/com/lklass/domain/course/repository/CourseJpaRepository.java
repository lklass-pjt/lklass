package com.lklass.domain.course.repository;

import com.lklass.domain.course.entity.Course;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CourseJpaRepository extends JpaRepository<Course, Long> {
}
