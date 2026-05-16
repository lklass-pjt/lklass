package com.lklass.domain.enrollment.repository;

import com.lklass.domain.enrollment.entity.EnrollmentStatusHistory;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EnrollmentStatusHistoryJpaRepository extends JpaRepository<EnrollmentStatusHistory, Long> {

    List<EnrollmentStatusHistory> findAllByEnrollment_IdOrderByChangedAtAsc(Long enrollmentId);
}
