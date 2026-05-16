package com.lklass.domain.enrollment.service;

import com.lklass.domain.course.entity.Course;
import com.lklass.domain.course.entity.CourseStatus;
import com.lklass.domain.course.exception.CourseErrorCode;
import com.lklass.domain.course.repository.CourseRepository;
import com.lklass.domain.enrollment.dto.EnrollmentApplyResult;
import com.lklass.domain.enrollment.dto.EnrollmentQueryResult;
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
import com.lklass.global.config.properties.EnrollmentPolicyProperties;
import com.lklass.global.exception.BusinessException;
import com.lklass.global.logging.AppLog;
import com.lklass.global.security.AuthenticatedUser;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class EnrollmentService {

    private static final Logger log = LoggerFactory.getLogger(EnrollmentService.class);

    private final EnrollmentRepository enrollmentRepository;
    private final CourseRepository courseRepository;
    private final UserRepository userRepository;
    private final EnrollmentPolicyProperties enrollmentPolicyProperties;
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

        AppLog.info(
                log,
                "ENROLLMENT_APPLY_PROCESSED",
                "enrollmentId={}, courseId={}, userId={}",
                enrollment.getId(),
                courseId,
                actor.userId()
        );
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
        AppLog.info(log, "PAYMENT_CONFIRM_PROCESSED", "enrollmentId={}, userId={}", enrollment.getId(), actor.userId());
    }

    @PreAuthorize("hasRole('STUDENT')")
    @Transactional
    public void cancel(AuthenticatedUser actor, Long enrollmentId) {
        Enrollment enrollment = getOwnedEnrollment(actor, enrollmentId);
        EnrollmentStatus fromStatus = enrollment.getStatus();
        LocalDateTime now = LocalDateTime.now(clock);

        enrollment.cancel(now, enrollmentPolicyProperties.cancellationPeriod());
        enrollmentRepository.deleteActiveEnrollment(enrollment.getId());
        courseRepository.releaseSeat(enrollment.getCourseId());
        enrollmentRepository.saveStatusHistory(EnrollmentStatusHistory.record(
                enrollment,
                fromStatus,
                enrollment.getStatus(),
                EnrollmentStatusChangeReason.CANCELLED,
                now,
                EnrollmentStatusChangedBy.user(actor.userId())
        ));
        AppLog.info(
                log,
                "ENROLLMENT_CANCEL_PROCESSED",
                "enrollmentId={}, courseId={}, userId={}",
                enrollment.getId(),
                enrollment.getCourseId(),
                actor.userId()
        );
    }

    @PreAuthorize("hasRole('STUDENT')")
    @Transactional(readOnly = true)
    public Page<EnrollmentQueryResult> getMyEnrollments(AuthenticatedUser actor, Pageable pageable) {
        return enrollmentRepository.findMyEnrollments(actor.userId(), pageable);
    }

    @PreAuthorize("@coursePermission.canManageCourse(authentication, #courseId)")
    @Transactional(readOnly = true)
    public Page<EnrollmentQueryResult> getCourseStudents(Long courseId, Pageable pageable) {
        return enrollmentRepository.findCourseStudents(courseId, pageable);
    }

    @Transactional
    public int expirePendingPayments() {
        LocalDateTime now = LocalDateTime.now(clock);
        LocalDateTime expiredBefore = now.minus(enrollmentPolicyProperties.pendingPaymentTtl());
        List<Enrollment> enrollments = enrollmentRepository.findPendingPaymentExpirationTargets(expiredBefore);

        enrollments.forEach(enrollment -> {
            EnrollmentStatus fromStatus = enrollment.getStatus();
            enrollment.expire(now);
            enrollmentRepository.deleteActiveEnrollment(enrollment.getId());
            courseRepository.releaseSeat(enrollment.getCourseId());
            enrollmentRepository.saveStatusHistory(EnrollmentStatusHistory.record(
                    enrollment,
                    fromStatus,
                    enrollment.getStatus(),
                    EnrollmentStatusChangeReason.EXPIRED,
                    now,
                    EnrollmentStatusChangedBy.system()
            ));
        });
        return enrollments.size();
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

    private Enrollment getOwnedEnrollment(AuthenticatedUser actor, Long enrollmentId) {
        Enrollment enrollment = enrollmentRepository.findById(enrollmentId)
                .orElseThrow(() -> new BusinessException(EnrollmentErrorCode.ENROLLMENT_NOT_FOUND));
        if (!enrollment.getUserId().equals(actor.userId())) {
            throw new BusinessException(EnrollmentErrorCode.ENROLLMENT_NOT_FOUND);
        }
        return enrollment;
    }
}
