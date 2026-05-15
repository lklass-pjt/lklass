package com.lklass.global.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.lklass.domain.auth.exception.AuthErrorCode;
import com.lklass.domain.user.entity.UserRole;
import com.lklass.global.config.properties.JwtProperties;
import com.lklass.global.exception.BusinessException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class JwtTokenProviderTest {

    private static final String SECRET = "test-secret-key-for-jjwt-hs256-32bytes";
    private static final ZoneId ZONE_ID = ZoneId.of("Asia/Seoul");

    @Test
    @DisplayName("Access Token을 발급하면 userId와 role을 다시 꺼낼 수 있다")
    void createAndParseAccessToken() {
        // given
        JwtTokenProvider jwtTokenProvider = tokenProviderAt("2026-05-16T01:00:00Z", Duration.ofHours(1));

        // when
        String accessToken = jwtTokenProvider.createAccessToken(1L, UserRole.CREATOR);

        // then
        assertThat(accessToken).isNotBlank();
        assertThat(jwtTokenProvider.getUserId(accessToken)).isEqualTo(1L);
        assertThat(jwtTokenProvider.getRole(accessToken)).isEqualTo(UserRole.CREATOR);
    }

    @Test
    @DisplayName("만료 시간이 지난 Access Token을 읽으면 EXPIRED_TOKEN 예외가 발생한다")
    void rejectExpiredAccessToken() {
        // given
        JwtTokenProvider issuer = tokenProviderAt("2026-05-16T01:00:00Z", Duration.ofMinutes(30));
        JwtTokenProvider verifier = tokenProviderAt("2026-05-16T01:31:00Z", Duration.ofMinutes(30));
        String expiredToken = issuer.createAccessToken(1L, UserRole.STUDENT);

        // when & then
        assertThatThrownBy(() -> verifier.getUserId(expiredToken))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.errorCode()).isEqualTo(AuthErrorCode.EXPIRED_TOKEN)
                );
    }

    @Test
    @DisplayName("서명이 변경된 Access Token을 읽으면 INVALID_TOKEN 예외가 발생한다")
    void rejectInvalidAccessToken() {
        // given
        JwtTokenProvider jwtTokenProvider = tokenProviderAt("2026-05-16T01:00:00Z", Duration.ofHours(1));
        String accessToken = jwtTokenProvider.createAccessToken(1L, UserRole.ADMIN);
        String invalidToken = accessToken + "tampered";

        // when & then
        assertThatThrownBy(() -> jwtTokenProvider.getRole(invalidToken))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.errorCode()).isEqualTo(AuthErrorCode.INVALID_TOKEN)
                );
    }

    private JwtTokenProvider tokenProviderAt(String instant, Duration accessTokenTtl) {
        JwtProperties jwtProperties = new JwtProperties(SECRET, accessTokenTtl);
        Clock fixedClock = Clock.fixed(Instant.parse(instant), ZONE_ID);
        return new JwtTokenProvider(jwtProperties, fixedClock);
    }
}
