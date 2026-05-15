package com.lklass.domain.user.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.lklass.TestcontainersConfiguration;
import com.lklass.domain.user.exception.UserErrorCode;
import com.lklass.domain.user.repository.UserJpaRepository;
import com.lklass.domain.user.repository.UserRepository;
import com.lklass.global.exception.BusinessException;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.dao.DataIntegrityViolationException;

@Import({TestcontainersConfiguration.class, UserPersistenceTest.FixedClockConfig.class})
@SpringBootTest
class UserPersistenceTest {

    @Autowired
    private UserJpaRepository userJpaRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    @DisplayName("User는 email, passwordHash, name, role과 생성/수정 시각을 저장하고 조회할 수 있다")
    void saveAndFindUser() {
        // given
        LocalDateTime now = LocalDateTime.of(2026, 5, 16, 10, 0);
        User user = User.create("student@example.com", "encoded-password", "학생 A", UserRole.STUDENT);

        // when
        User savedUser = userJpaRepository.saveAndFlush(user);
        Optional<User> foundUser = userJpaRepository.findByEmail("student@example.com");

        // then
        assertThat(savedUser.getId()).isNotNull();
        assertThat(savedUser.getEmail()).isEqualTo("student@example.com");
        assertThat(savedUser.getPasswordHash()).isEqualTo("encoded-password");
        assertThat(savedUser.getName()).isEqualTo("학생 A");
        assertThat(savedUser.getRole()).isEqualTo(UserRole.STUDENT);
        assertThat(savedUser.getCreatedAt()).isEqualTo(now);
        assertThat(savedUser.getUpdatedAt()).isEqualTo(now);
        assertThat(foundUser).isPresent();
        assertThat(foundUser.get().getId()).isEqualTo(savedUser.getId());
        assertThat(foundUser.get().getEmail()).isEqualTo("student@example.com");
        assertThat(userJpaRepository.existsByEmail("student@example.com")).isTrue();
    }

    @Test
    @DisplayName("User email은 unique 제약으로 중복 저장할 수 없다")
    void rejectDuplicateEmail() {
        // given
        User firstUser = User.create("duplicated@example.com", "encoded-password-1", "학생 A", UserRole.STUDENT);
        User secondUser = User.create("duplicated@example.com", "encoded-password-2", "학생 B", UserRole.CREATOR);
        userJpaRepository.saveAndFlush(firstUser);

        // when & then
        assertThatThrownBy(() -> userJpaRepository.saveAndFlush(secondUser))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("UserRepository는 DB email unique 제약 오류를 DUPLICATED_EMAIL 예외로 변환한다")
    void convertEmailUniqueConstraintViolationToBusinessException() {
        // given
        User firstUser = User.create("race@example.com", "encoded-password-1", "학생 A", UserRole.STUDENT);
        User secondUser = User.create("race@example.com", "encoded-password-2", "학생 B", UserRole.CREATOR);
        userRepository.save(firstUser);

        // when & then
        assertThatThrownBy(() -> userRepository.save(secondUser))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.errorCode()).isEqualTo(UserErrorCode.DUPLICATED_EMAIL)
                );
    }

    @Test
    @DisplayName("User는 필수 필드가 null이면 생성할 수 없다")
    void rejectNullRequiredFields() {
        // given
        String email = "student-null@example.com";
        String passwordHash = "encoded-password";
        String name = "학생 A";
        UserRole role = UserRole.STUDENT;

        // when & then
        assertThatThrownBy(() -> User.create(null, passwordHash, name, role))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("email must not be null");
        assertThatThrownBy(() -> User.create(email, null, name, role))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("passwordHash must not be null");
        assertThatThrownBy(() -> User.create(email, passwordHash, null, role))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("name must not be null");
        assertThatThrownBy(() -> User.create(email, passwordHash, name, null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("role must not be null");
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
