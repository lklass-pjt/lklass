package com.lklass.global.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

@SpringJUnitConfig(ClockConfig.class)
class ClockConfigTest {

    @Autowired
    private Clock clock;

    @Test
    @DisplayName("ClockConfig는 현재 시각 정책을 주입받아 사용할 수 있도록 Clock bean을 등록한다")
    void registerClockBean() {
        // given
        // ClockConfig만 로드한 Spring test context를 준비한다.

        // when
        Clock injectedClock = clock;

        // then
        assertThat(injectedClock).isNotNull();
        assertThat(injectedClock.getZone()).isEqualTo(Clock.systemDefaultZone().getZone());
    }
}
