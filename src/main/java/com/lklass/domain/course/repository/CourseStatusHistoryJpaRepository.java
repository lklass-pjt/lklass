package com.lklass.domain.course.repository;

import com.lklass.domain.course.entity.CourseStatusHistory;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CourseStatusHistoryJpaRepository extends JpaRepository<CourseStatusHistory, Long> {

    List<CourseStatusHistory> findAllByCourse_IdOrderByChangedAtAsc(Long courseId);
}
