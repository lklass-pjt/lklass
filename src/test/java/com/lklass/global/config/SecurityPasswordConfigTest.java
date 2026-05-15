package com.lklass.global.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.security.crypto.password.PasswordEncoder;

@SpringJUnitConfig(SecurityPasswordConfig.class)
class SecurityPasswordConfigTest {

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    @DisplayName("PasswordEncoder는 raw password를 BCrypt hash로 변환하고 matches로 검증할 수 있다")
    void encodePasswordWithBcrypt() {
        // given
        String rawPassword = "password1234";

        // when
        String firstHash = passwordEncoder.encode(rawPassword);
        String secondHash = passwordEncoder.encode(rawPassword);

        // then
        assertThat(firstHash).isNotEqualTo(rawPassword);
        assertThat(secondHash).isNotEqualTo(rawPassword);
        assertThat(firstHash).isNotEqualTo(secondHash);
        assertThat(firstHash).startsWith("$2");
        assertThat(passwordEncoder.matches(rawPassword, firstHash)).isTrue();
        assertThat(passwordEncoder.matches(rawPassword, secondHash)).isTrue();
    }
}
