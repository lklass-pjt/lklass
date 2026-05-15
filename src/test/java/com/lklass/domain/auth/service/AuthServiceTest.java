package com.lklass.domain.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.lklass.TestcontainersConfiguration;
import com.lklass.domain.user.entity.UserRole;
import com.lklass.domain.user.exception.UserErrorCode;
import com.lklass.domain.user.repository.UserRepository;
import com.lklass.global.exception.BusinessException;
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

    @Test
    @DisplayName("회원가입은 비밀번호를 암호화하고 선택한 역할과 함께 User를 저장한다")
    void signup() {
        // given
        LocalDateTime now = LocalDateTime.of(2026, 5, 16, 10, 0);
        String rawPassword = "password1234";

        // when
        Long userId = authService.signup("creator@example.com", rawPassword, "크리에이터 A", UserRole.CREATOR);

        // then
        assertThat(userId).isNotNull();
        assertThat(userRepository.findByEmail("creator@example.com")).hasValueSatisfying(savedUser -> {
            assertThat(savedUser.getId()).isEqualTo(userId);
            assertThat(savedUser.getEmail()).isEqualTo("creator@example.com");
            assertThat(savedUser.getName()).isEqualTo("크리에이터 A");
            assertThat(savedUser.getRole()).isEqualTo(UserRole.CREATOR);
            assertThat(savedUser.getPasswordHash()).isNotEqualTo(rawPassword);
            assertThat(passwordEncoder.matches(rawPassword, savedUser.getPasswordHash())).isTrue();
            assertThat(savedUser.getCreatedAt()).isEqualTo(now);
            assertThat(savedUser.getUpdatedAt()).isEqualTo(now);
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
