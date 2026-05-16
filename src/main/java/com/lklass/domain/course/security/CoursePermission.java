package com.lklass.domain.course.security;

import com.lklass.domain.course.repository.CourseRepository;
import com.lklass.domain.user.entity.UserRole;
import com.lklass.global.security.AuthenticatedUser;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CoursePermission {

    private final CourseRepository courseRepository;

    public boolean canCreateCourse(Authentication authentication, Long requestedCreatorId) {
        if (authentication == null || !(authentication.getPrincipal() instanceof AuthenticatedUser actor)) {
            return false;
        }

        if (actor.role() == UserRole.CREATOR) {
            return requestedCreatorId == null || requestedCreatorId.equals(actor.userId());
        }

        if (actor.role() == UserRole.ADMIN) {
            return requestedCreatorId != null;
        }

        return false;
    }

    public boolean canManageCourse(Authentication authentication, Long courseId) {
        if (authentication == null || !(authentication.getPrincipal() instanceof AuthenticatedUser actor)) {
            return false;
        }

        if (actor.role() == UserRole.ADMIN) {
            return true;
        }

        if (actor.role() == UserRole.CREATOR) {
            // Method security는 트랜잭션 밖에서 실행될 수 있어 소유 여부만 가벼운 exists 쿼리로 확인한다.
            return courseRepository.existsByIdAndCreatorId(courseId, actor.userId());
        }

        return false;
    }
}
