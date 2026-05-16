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
import com.lklass.global.security.AuthenticatedUser;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CourseService {

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
                CourseStatusChangedBy.user(actor.userId())
        ));

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

    private Long resolveCreatorId(AuthenticatedUser actor, Long requestedCreatorId) {
        if (actor.role() == UserRole.ADMIN) {
            return requestedCreatorId;
        }
        return actor.userId();
    }
}
