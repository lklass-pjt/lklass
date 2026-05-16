package com.lklass.domain.enrollment.repository;

import com.lklass.domain.enrollment.dto.EnrollmentQueryResult;
import com.lklass.domain.enrollment.entity.Enrollment;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EnrollmentJpaRepository extends JpaRepository<Enrollment, Long> {

    @Query("""
            select new com.lklass.domain.enrollment.dto.EnrollmentQueryResult(
                e.id,
                e.courseId,
                c.title,
                e.userId,
                u.name,
                u.email,
                e.status,
                e.enrolledAt,
                e.confirmedAt,
                e.cancelledAt
            )
              from Enrollment e
              join Course c on c.id = e.courseId
              join User u on u.id = e.userId
             where e.userId = :userId
            """)
    Page<EnrollmentQueryResult> findMyEnrollments(@Param("userId") Long userId, Pageable pageable);

    @Query("""
            select new com.lklass.domain.enrollment.dto.EnrollmentQueryResult(
                e.id,
                e.courseId,
                c.title,
                e.userId,
                u.name,
                u.email,
                e.status,
                e.enrolledAt,
                e.confirmedAt,
                e.cancelledAt
            )
              from Enrollment e
              join Course c on c.id = e.courseId
              join User u on u.id = e.userId
             where e.courseId = :courseId
            """)
    Page<EnrollmentQueryResult> findCourseStudents(@Param("courseId") Long courseId, Pageable pageable);

    @Query("""
            select e
              from Enrollment e
             where e.status = com.lklass.domain.enrollment.entity.EnrollmentStatus.PENDING
               and e.enrolledAt <= :expiredBefore
            """)
    List<Enrollment> findPendingPaymentExpirationTargets(@Param("expiredBefore") LocalDateTime expiredBefore);
}
