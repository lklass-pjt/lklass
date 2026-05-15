package com.lklass.domain.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.lklass.TestcontainersConfiguration;
import com.lklass.domain.auth.dto.AccessTokenResult;
import com.lklass.domain.auth.exception.AuthErrorCode;
import com.lklass.domain.user.entity.UserRole;
import com.lklass.domain.user.exception.UserErrorCode;
import com.lklass.domain.user.repository.UserRepository;
import com.lklass.global.exception.BusinessException;
import com.lklass.global.security.JwtTokenProvider;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.security.crypto.password.PasswordEncoder;

@Import({TestcontainersConfiguration.class, AuthServiceTest.FixedClockConfig.class})
@SpringBootTest
class AuthServiceTest {

    @Autowired
    private AuthService authService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Test
    @DisplayName("회원가입은 비밀번호를 암호화해 저장하고 가입한 사용자 Access Token을 반환한다")
    void signup() {
        // given
        LocalDateTime now = LocalDateTime.of(2026, 5, 16, 10, 0);
        String rawPassword = "password1234";

        // when
        AccessTokenResult result = authService.signup("creator@example.com", rawPassword, "크리에이터 A", UserRole.CREATOR);

        // then
        assertThat(result.accessToken()).isNotBlank();
        assertThat(userRepository.findByEmail("creator@example.com")).hasValueSatisfying(savedUser -> {
            assertThat(savedUser.getEmail()).isEqualTo("creator@example.com");
            assertThat(savedUser.getName()).isEqualTo("크리에이터 A");
            assertThat(savedUser.getRole()).isEqualTo(UserRole.CREATOR);
            assertThat(savedUser.getPasswordHash()).isNotEqualTo(rawPassword);
            assertThat(passwordEncoder.matches(rawPassword, savedUser.getPasswordHash())).isTrue();
            assertThat(savedUser.getCreatedAt()).isEqualTo(now);
            assertThat(savedUser.getUpdatedAt()).isEqualTo(now);
            assertThat(jwtTokenProvider.getUserId(result.accessToken())).isEqualTo(savedUser.getId());
            assertThat(jwtTokenProvider.getRole(result.accessToken())).isEqualTo(UserRole.CREATOR);
        });
    }

    @Test
    @DisplayName("이미 가입된 email로 회원가입하면 DUPLICATED_EMAIL 예외가 발생한다")
    void rejectDuplicatedEmail() {
        // given
        authService.signup("student@example.com", "password1234", "학생 A", UserRole.STUDENT);

        // when & then
        assertThatThrownBy(() -> authService.signup("student@example.com", "password5678", "학생 B", UserRole.STUDENT))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.errorCode()).isEqualTo(UserErrorCode.DUPLICATED_EMAIL)
                );
    }

    @Test
    @DisplayName("로그인은 email과 password가 일치하면 해당 사용자 Access Token을 반환한다")
    void login() {
        // given
        authService.signup("login-creator@example.com", "password1234", "로그인 크리에이터", UserRole.CREATOR);
        Long userId = userRepository.findByEmail("login-creator@example.com")
                .orElseThrow()
                .getId();

        // when
        AccessTokenResult result = authService.login("login-creator@example.com", "password1234");

        // then
        assertThat(result.accessToken()).isNotBlank();
        assertThat(jwtTokenProvider.getUserId(result.accessToken())).isEqualTo(userId);
        assertThat(jwtTokenProvider.getRole(result.accessToken())).isEqualTo(UserRole.CREATOR);
    }

    @Test
    @DisplayName("로그인 시 password가 일치하지 않으면 INVALID_CREDENTIALS 예외가 발생한다")
    void rejectLoginWithWrongPassword() {
        // given
        authService.signup("wrong-password@example.com", "password1234", "학생 B", UserRole.STUDENT);

        // when & then
        assertThatThrownBy(() -> authService.login("wrong-password@example.com", "wrong-password"))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.errorCode()).isEqualTo(AuthErrorCode.INVALID_CREDENTIALS)
                );
    }

    @Test
    @DisplayName("로그인 시 가입되지 않은 email이면 INVALID_CREDENTIALS 예외가 발생한다")
    void rejectLoginWithUnknownEmail() {
        // given
        String unknownEmail = "unknown@example.com";

        // when & then
        assertThatThrownBy(() -> authService.login(unknownEmail, "password1234"))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.errorCode()).isEqualTo(AuthErrorCode.INVALID_CREDENTIALS)
                );
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class FixedClockConfig {

        @Bean
        @Primary
        Clock fixedClock() {
            return Clock.fixed(
                    Instant.parse("2026-05-16T01:00:00Z"),
                    ZoneId.of("Asia/Seoul")
            );
        }
    }
}
