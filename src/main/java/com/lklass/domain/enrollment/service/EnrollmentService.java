package com.lklass.domain.enrollment.service;

import com.lklass.domain.course.entity.Course;
import com.lklass.domain.course.entity.CourseStatus;
import com.lklass.domain.course.exception.CourseErrorCode;
import com.lklass.domain.course.repository.CourseRepository;
import com.lklass.domain.enrollment.dto.EnrollmentApplyResult;
import com.lklass.domain.enrollment.entity.ActiveEnrollment;
import com.lklass.domain.enrollment.entity.Enrollment;
import com.lklass.domain.enrollment.entity.EnrollmentStatus;
import com.lklass.domain.enrollment.entity.EnrollmentStatusChangeReason;
import com.lklass.domain.enrollment.entity.EnrollmentStatusChangedBy;
import com.lklass.domain.enrollment.entity.EnrollmentStatusHistory;
import com.lklass.domain.enrollment.exception.EnrollmentErrorCode;
import com.lklass.domain.enrollment.repository.EnrollmentRepository;
import com.lklass.domain.user.exception.UserErrorCode;
import com.lklass.domain.user.repository.UserRepository;
import com.lklass.global.exception.BusinessException;
import com.lklass.global.security.AuthenticatedUser;
import java.time.Clock;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class EnrollmentService {

    private final EnrollmentRepository enrollmentRepository;
    private final CourseRepository courseRepository;
    private final UserRepository userRepository;
    private final Clock clock;

    @PreAuthorize("hasRole('STUDENT')")
    @Transactional
    public EnrollmentApplyResult apply(AuthenticatedUser actor, Long courseId) {
        userRepository.findById(actor.userId())
                .orElseThrow(() -> new BusinessException(UserErrorCode.USER_NOT_FOUND));

        if (enrollmentRepository.existsActiveEnrollment(courseId, actor.userId())) {
            throw new BusinessException(EnrollmentErrorCode.ALREADY_ENROLLED);
        }

        LocalDateTime now = LocalDateTime.now(clock);
        if (!courseRepository.tryOccupySeat(courseId, now)) {
            throw resolveEnrollmentFailure(courseId, now);
        }

        Enrollment enrollment = enrollmentRepository.save(Enrollment.create(courseId, actor.userId(), now));
        // ActiveEnrollment 저장이 unique 제약으로 실패하면 같은 트랜잭션의 좌석 확보도 함께 롤백된다.
        enrollmentRepository.saveActiveEnrollment(ActiveEnrollment.create(enrollment));
        enrollmentRepository.saveStatusHistory(EnrollmentStatusHistory.record(
                enrollment,
                null,
                EnrollmentStatus.PENDING,
                EnrollmentStatusChangeReason.CREATED,
                now,
                EnrollmentStatusChangedBy.user(actor.userId())
        ));

        return EnrollmentApplyResult.from(enrollment);
    }

    @PreAuthorize("hasRole('STUDENT')")
    @Transactional
    public void confirmPayment(AuthenticatedUser actor, Long enrollmentId) {
        Enrollment enrollment = enrollmentRepository.findById(enrollmentId)
                .orElseThrow(() -> new BusinessException(EnrollmentErrorCode.ENROLLMENT_NOT_FOUND));
        if (!enrollment.getUserId().equals(actor.userId())) {
            throw new BusinessException(EnrollmentErrorCode.ENROLLMENT_NOT_FOUND);
        }
        EnrollmentStatus fromStatus = enrollment.getStatus();
        LocalDateTime now = LocalDateTime.now(clock);

        enrollment.confirm(now);
        enrollmentRepository.saveStatusHistory(EnrollmentStatusHistory.record(
                enrollment,
                fromStatus,
                enrollment.getStatus(),
                EnrollmentStatusChangeReason.PAYMENT_CONFIRMED,
                now,
                EnrollmentStatusChangedBy.user(actor.userId())
        ));
    }

    private BusinessException resolveEnrollmentFailure(Long courseId, LocalDateTime now) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new BusinessException(CourseErrorCode.COURSE_NOT_FOUND));

        if (course.getStatus() != CourseStatus.OPEN
                || now.isBefore(course.getEnrollmentPeriod().getStartAt())
                || !now.isBefore(course.getEnrollmentPeriod().getEndAt())) {
            return new BusinessException(EnrollmentErrorCode.ENROLLMENT_NOT_AVAILABLE);
        }
        return new BusinessException(EnrollmentErrorCode.CAPACITY_EXCEEDED);
    }
}
