package com.lklass.domain.enrollment.repository;

import com.lklass.domain.enrollment.entity.Enrollment;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EnrollmentJpaRepository extends JpaRepository<Enrollment, Long> {
}
