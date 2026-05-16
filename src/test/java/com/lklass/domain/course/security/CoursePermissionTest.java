package com.lklass.domain.course.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.lklass.domain.user.entity.UserRole;
import com.lklass.global.security.AuthenticatedUser;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

class CoursePermissionTest {

    private final CoursePermission coursePermission = new CoursePermission();

    @Test
    @DisplayName("CREATOR는 creatorId가 없으면 본인 Course 생성으로 간주되어 허용된다")
    void allowCreatorWithoutRequestedCreatorId() {
        // given
        Authentication authentication = authentication(1L, UserRole.CREATOR);

        // when
        boolean allowed = coursePermission.canCreateCourse(authentication, null);

        // then
        assertThat(allowed).isTrue();
    }

    @Test
    @DisplayName("CREATOR는 본인 creatorId로 Course를 생성할 수 있다")
    void allowCreatorWithOwnCreatorId() {
        // given
        Authentication authentication = authentication(1L, UserRole.CREATOR);

        // when
        boolean allowed = coursePermission.canCreateCourse(authentication, 1L);

        // then
        assertThat(allowed).isTrue();
    }

    @Test
    @DisplayName("CREATOR는 다른 creatorId로 Course를 생성할 수 없다")
    void rejectCreatorWithOtherCreatorId() {
        // given
        Authentication authentication = authentication(1L, UserRole.CREATOR);

        // when
        boolean allowed = coursePermission.canCreateCourse(authentication, 2L);

        // then
        assertThat(allowed).isFalse();
    }

    @Test
    @DisplayName("ADMIN은 creatorId가 있으면 Course를 생성할 수 있다")
    void allowAdminWithRequestedCreatorId() {
        // given
        Authentication authentication = authentication(99L, UserRole.ADMIN);

        // when
        boolean allowed = coursePermission.canCreateCourse(authentication, 1L);

        // then
        assertThat(allowed).isTrue();
    }

    @Test
    @DisplayName("ADMIN은 creatorId가 없으면 Course를 생성할 수 없다")
    void rejectAdminWithoutRequestedCreatorId() {
        // given
        Authentication authentication = authentication(99L, UserRole.ADMIN);

        // when
        boolean allowed = coursePermission.canCreateCourse(authentication, null);

        // then
        assertThat(allowed).isFalse();
    }

    @Test
    @DisplayName("STUDENT는 Course를 생성할 수 없다")
    void rejectStudent() {
        // given
        Authentication authentication = authentication(3L, UserRole.STUDENT);

        // when
        boolean allowed = coursePermission.canCreateCourse(authentication, null);

        // then
        assertThat(allowed).isFalse();
    }

    @Test
    @DisplayName("인증 정보가 없으면 Course를 생성할 수 없다")
    void rejectMissingAuthentication() {
        // when
        boolean allowed = coursePermission.canCreateCourse(null, null);

        // then
        assertThat(allowed).isFalse();
    }

    @Test
    @DisplayName("principal 타입이 AuthenticatedUser가 아니면 Course를 생성할 수 없다")
    void rejectUnexpectedPrincipalType() {
        // given
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                "not-authenticated-user",
                "access-token",
                List.of(new SimpleGrantedAuthority("ROLE_CREATOR"))
        );

        // when
        boolean allowed = coursePermission.canCreateCourse(authentication, null);

        // then
        assertThat(allowed).isFalse();
    }

    private Authentication authentication(Long userId, UserRole role) {
        return new UsernamePasswordAuthenticationToken(
                new AuthenticatedUser(userId, role),
                "access-token",
                List.of(new SimpleGrantedAuthority("ROLE_" + role.name()))
        );
    }
}
