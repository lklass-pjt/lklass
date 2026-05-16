package com.lklass.domain.enrollment.repository;

import com.lklass.domain.enrollment.entity.ActiveEnrollment;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ActiveEnrollmentJpaRepository extends JpaRepository<ActiveEnrollment, Long> {

    boolean existsByCourseIdAndUserId(Long courseId, Long userId);
}
