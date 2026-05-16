package com.lklass.domain.course.security;

import com.lklass.domain.user.entity.UserRole;
import com.lklass.global.security.AuthenticatedUser;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

@Component
public class CoursePermission {

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
}
