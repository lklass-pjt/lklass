package com.lklass.domain.course.service;

import com.lklass.domain.course.dto.CourseCreateResult;
import com.lklass.domain.course.dto.CourseQueryResult;
import com.lklass.domain.course.entity.Course;
import com.lklass.domain.course.entity.CourseStatus;
import com.lklass.domain.course.entity.CourseStatusChangedBy;
import com.lklass.domain.course.entity.CourseStatusChangeReason;
import com.lklass.domain.course.entity.CourseStatusHistory;
import com.lklass.domain.course.exception.CourseErrorCode;
import com.lklass.domain.course.repository.CourseRepository;
import com.lklass.domain.course.repository.CourseStatusHistoryRepository;
import com.lklass.domain.user.entity.User;
import com.lklass.domain.user.entity.UserRole;
import com.lklass.domain.user.exception.UserErrorCode;
import com.lklass.domain.user.repository.UserRepository;
import com.lklass.global.exception.BusinessException;
import com.lklass.global.logging.AppLog;
import com.lklass.global.security.AuthenticatedUser;
import java.math.BigDecimal;
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
public class CourseService {

    private static final Logger log = LoggerFactory.getLogger(CourseService.class);

    private final CourseRepository courseRepository;
    private final CourseStatusHistoryRepository courseStatusHistoryRepository;
    private final UserRepository userRepository;
    private final Clock clock;

    @PreAuthorize("@coursePermission.canCreateCourse(authentication, #requestedCreatorId)")
    @Transactional
    public CourseCreateResult createCourse(
            AuthenticatedUser actor,
            Long requestedCreatorId,
            String title,
            String description,
            BigDecimal price,
            int capacity,
            LocalDateTime enrollmentStartAt,
            LocalDateTime enrollmentEndAt,
            LocalDateTime courseStartAt,
            LocalDateTime courseEndAt
    ) {
        Long creatorId = resolveCreatorId(actor, requestedCreatorId);
        User creator = userRepository.findById(creatorId)
                .orElseThrow(() -> new BusinessException(UserErrorCode.USER_NOT_FOUND));
        if (creator.getRole() != UserRole.CREATOR) {
            throw new BusinessException(UserErrorCode.USER_NOT_CREATOR);
        }

        Course course = Course.create(
                creator.getId(),
                title,
                description,
                price,
                capacity,
                enrollmentStartAt,
                enrollmentEndAt,
                courseStartAt,
                courseEndAt
        );
        Course savedCourse = courseRepository.save(course);
        courseStatusHistoryRepository.save(CourseStatusHistory.record(
                savedCourse,
                null,
                CourseStatus.DRAFT,
                CourseStatusChangeReason.CREATED,
                LocalDateTime.now(clock),
                // changedBy는 Course 소유자가 아니라 실제 상태 변경 행위자를 기록한다.
                CourseStatusChangedBy.user(actor.userId())
        ));

        AppLog.info(
                log,
                "COURSE_CREATE_PROCESSED",
                "courseId={}, creatorId={}, actorId={}",
                savedCourse.getId(),
                savedCourse.getCreatorId(),
                actor.userId()
        );
        return CourseCreateResult.from(savedCourse);
    }

    @Transactional(readOnly = true)
    public Page<CourseQueryResult> getCourses(CourseStatus status, Pageable pageable) {
        return courseRepository.findAll(status, pageable);
    }

    @Transactional(readOnly = true)
    public CourseQueryResult getCourse(Long courseId) {
        return courseRepository.findResultById(courseId)
                .orElseThrow(() -> new BusinessException(CourseErrorCode.COURSE_NOT_FOUND));
    }

    @PreAuthorize("@coursePermission.canManageCourse(authentication, #courseId)")
    @Transactional
    public void openCourse(AuthenticatedUser actor, Long courseId, LocalDateTime enrollmentEndAt) {
        Course course = getCourseEntity(courseId);

        CourseStatus fromStatus = course.getStatus();
        course.openManually(LocalDateTime.now(clock), enrollmentEndAt);
        recordStatusHistory(
                course,
                fromStatus,
                course.getStatus(),
                CourseStatusChangeReason.MANUAL_OPENED,
                actor.userId()
        );
        AppLog.info(log, "COURSE_OPEN_PROCESSED", "courseId={}, actorId={}", course.getId(), actor.userId());
    }

    @PreAuthorize("@coursePermission.canManageCourse(authentication, #courseId)")
    @Transactional
    public void closeCourse(AuthenticatedUser actor, Long courseId) {
        Course course = getCourseEntity(courseId);

        CourseStatus fromStatus = course.getStatus();
        course.close();
        recordStatusHistory(
                course,
                fromStatus,
                course.getStatus(),
                CourseStatusChangeReason.MANUAL_CLOSED,
                actor.userId()
        );
        AppLog.info(log, "COURSE_CLOSE_PROCESSED", "courseId={}, actorId={}", course.getId(), actor.userId());
    }

    @PreAuthorize("@coursePermission.canManageCourse(authentication, #courseId)")
    @Transactional
    public void reserveCoursePublication(Long courseId) {
        Course course = getCourseEntity(courseId);

        course.reserveAutoPublish(LocalDateTime.now(clock));
        AppLog.info(log, "COURSE_PUBLICATION_RESERVE_PROCESSED", "courseId={}", course.getId());
    }

    @Transactional
    public int openReservedCourses() {
        LocalDateTime now = LocalDateTime.now(clock);
        List<Course> courses = courseRepository.findAutoOpenTargets(now);

        courses.forEach(course -> {
            CourseStatus fromStatus = course.getStatus();
            course.openAutomatically(now);
            recordStatusHistory(
                    course,
                    fromStatus,
                    course.getStatus(),
                    CourseStatusChangeReason.AUTO_OPENED,
                    null
            );
        });
        return courses.size();
    }

    @Transactional
    public int closeExpiredOpenCourses() {
        LocalDateTime now = LocalDateTime.now(clock);
        List<Course> courses = courseRepository.findAutoCloseTargets(now);

        courses.forEach(course -> {
            CourseStatus fromStatus = course.getStatus();
            course.closeAutomatically(now);
            recordStatusHistory(
                    course,
                    fromStatus,
                    course.getStatus(),
                    CourseStatusChangeReason.AUTO_CLOSED,
                    null
            );
        });
        return courses.size();
    }

    private Long resolveCreatorId(AuthenticatedUser actor, Long requestedCreatorId) {
        if (actor.role() == UserRole.ADMIN) {
            return requestedCreatorId;
        }
        return actor.userId();
    }

    private Course getCourseEntity(Long courseId) {
        return courseRepository.findById(courseId)
                .orElseThrow(() -> new BusinessException(CourseErrorCode.COURSE_NOT_FOUND));
    }

    private void recordStatusHistory(
            Course course,
            CourseStatus fromStatus,
            CourseStatus toStatus,
            CourseStatusChangeReason reason,
            Long actorId
    ) {
        courseStatusHistoryRepository.save(CourseStatusHistory.record(
                course,
                fromStatus,
                toStatus,
                reason,
                LocalDateTime.now(clock),
                actorId == null ? CourseStatusChangedBy.system() : CourseStatusChangedBy.user(actorId)
        ));
    }
}
