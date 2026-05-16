package com.lklass.domain.course.repository;

import com.lklass.domain.course.entity.CourseStatusHistory;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class CourseStatusHistoryRepository {

    private final CourseStatusHistoryJpaRepository courseStatusHistoryJpaRepository;

    public CourseStatusHistory save(CourseStatusHistory courseStatusHistory) {
        return courseStatusHistoryJpaRepository.save(courseStatusHistory);
    }

    public List<CourseStatusHistory> findAllByCourseId(Long courseId) {
        return courseStatusHistoryJpaRepository.findAllByCourse_IdOrderByChangedAtAsc(courseId);
    }
}
