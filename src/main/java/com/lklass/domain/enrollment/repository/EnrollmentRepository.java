package com.lklass.domain.enrollment.repository;

import com.lklass.domain.enrollment.entity.ActiveEnrollment;
import com.lklass.domain.enrollment.entity.Enrollment;
import com.lklass.domain.enrollment.entity.EnrollmentStatusHistory;
import com.lklass.domain.enrollment.exception.EnrollmentErrorCode;
import com.lklass.global.exception.BusinessException;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class EnrollmentRepository {

    private static final String ACTIVE_COURSE_USER_UNIQUE_CONSTRAINT_NAME = "uk_active_enrollments_course_user";
    private static final String ACTIVE_ENROLLMENT_UNIQUE_CONSTRAINT_NAME = "uk_active_enrollments_enrollment";

    private final EnrollmentJpaRepository enrollmentJpaRepository;
    private final ActiveEnrollmentJpaRepository activeEnrollmentJpaRepository;
    private final EnrollmentStatusHistoryJpaRepository enrollmentStatusHistoryJpaRepository;

    public Enrollment save(Enrollment enrollment) {
        return enrollmentJpaRepository.save(enrollment);
    }

    public ActiveEnrollment saveActiveEnrollment(ActiveEnrollment activeEnrollment) {
        try {
            return activeEnrollmentJpaRepository.saveAndFlush(activeEnrollment);
        } catch (DataIntegrityViolationException exception) {
            if (hasConstraintName(exception, ACTIVE_COURSE_USER_UNIQUE_CONSTRAINT_NAME)
                    || hasConstraintName(exception, ACTIVE_ENROLLMENT_UNIQUE_CONSTRAINT_NAME)) {
                throw new BusinessException(EnrollmentErrorCode.ALREADY_ENROLLED);
            }
            throw exception;
        }
    }

    public EnrollmentStatusHistory saveStatusHistory(EnrollmentStatusHistory enrollmentStatusHistory) {
        return enrollmentStatusHistoryJpaRepository.save(enrollmentStatusHistory);
    }

    public Optional<Enrollment> findById(Long enrollmentId) {
        return enrollmentJpaRepository.findById(enrollmentId);
    }

    public boolean existsActiveEnrollment(Long courseId, Long userId) {
        return activeEnrollmentJpaRepository.existsByCourseIdAndUserId(courseId, userId);
    }

    public boolean deleteActiveEnrollment(Long enrollmentId) {
        return activeEnrollmentJpaRepository.deleteByEnrollment_Id(enrollmentId) == 1;
    }

    public List<EnrollmentStatusHistory> findStatusHistories(Long enrollmentId) {
        return enrollmentStatusHistoryJpaRepository.findAllByEnrollment_IdOrderByChangedAtAsc(enrollmentId);
    }

    private boolean hasConstraintName(Throwable throwable, String constraintName) {
        Throwable current = throwable;
        while (current != null) {
            String message = current.getMessage();
            if (message != null && message.contains(constraintName)) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }
}
