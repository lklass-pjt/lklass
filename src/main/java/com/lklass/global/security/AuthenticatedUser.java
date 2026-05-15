package com.lklass.global.security;

import com.lklass.domain.user.entity.UserRole;

public record AuthenticatedUser(
        Long userId,
        UserRole role
) {
}
