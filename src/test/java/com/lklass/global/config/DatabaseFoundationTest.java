package com.lklass.global.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.lklass.TestcontainersConfiguration;
import com.lklass.global.config.properties.EnrollmentPolicyProperties;
import com.lklass.global.config.properties.JwtProperties;
import com.lklass.global.config.properties.SchedulerProperties;
import java.time.Duration;
import net.javacrumbs.shedlock.core.LockProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
class DatabaseFoundationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private LockProvider lockProvider;

    @Autowired
    private EnrollmentPolicyProperties enrollmentPolicyProperties;

    @Autowired
    private SchedulerProperties schedulerProperties;

    @Autowired
    private JwtProperties jwtProperties;

    @Test
    @DisplayName("Flyway는 Testcontainers MySQL에 V1 migration을 적용하고 shedlock 테이블을 생성한다")
    void applyFlywayMigrationAndCreateShedlockTable() {
        // given
        String tableName = "shedlock";

        // when
        Integer tableCount = jdbcTemplate.queryForObject(
                """
                        select count(*)
                          from information_schema.tables
                         where table_schema = database()
                           and table_name = ?
                        """,
                Integer.class,
                tableName
        );

        // then
        assertThat(tableCount).isEqualTo(1);
    }

    @Test
    @DisplayName("SchedulerLockConfig는 ShedLock이 DB 락을 사용할 수 있도록 LockProvider bean을 등록한다")
    void registerJdbcLockProvider() {
        // given
        // Spring Boot test context에 SchedulerLockConfig와 datasource를 함께 로드한다.

        // when
        LockProvider injectedLockProvider = lockProvider;

        // then
        assertThat(injectedLockProvider).isNotNull();
    }

    @Test
    @DisplayName("EnrollmentPolicyProperties는 결제 대기 만료와 결제 후 취소 가능 기간을 Duration으로 바인딩한다")
    void bindEnrollmentPolicyProperties() {
        // given
        // application.yaml의 lklass.policy 설정을 Spring Boot가 바인딩한다.

        // when
        Duration pendingPaymentTtl = enrollmentPolicyProperties.pendingPaymentTtl();
        Duration cancellationPeriod = enrollmentPolicyProperties.cancellationPeriod();

        // then
        assertThat(pendingPaymentTtl).isEqualTo(Duration.ofMinutes(30));
        assertThat(cancellationPeriod).isEqualTo(Duration.ofDays(7));
    }

    @Test
    @DisplayName("SchedulerProperties는 Course 상태 전환과 PENDING 만료 스케줄 cron을 바인딩한다")
    void bindSchedulerProperties() {
        // given
        // application.yaml의 lklass.scheduler 설정을 Spring Boot가 바인딩한다.

        // when
        String courseStatusCron = schedulerProperties.courseStatusCron();
        String pendingExpirationCron = schedulerProperties.pendingExpirationCron();

        // then
        assertThat(courseStatusCron).isEqualTo("0 * * * * *");
        assertThat(pendingExpirationCron).isEqualTo("0 * * * * *");
    }

    @Test
    @DisplayName("JwtProperties는 JWT secret과 access token TTL을 바인딩한다")
    void bindJwtProperties() {
        // given
        // application.yaml의 lklass.jwt 설정을 Spring Boot가 바인딩한다.

        // when
        String secret = jwtProperties.secret();
        Duration accessTokenTtl = jwtProperties.accessTokenTtl();

        // then
        assertThat(secret).isEqualTo("local-dev-secret-change-me");
        assertThat(accessTokenTtl).isEqualTo(Duration.ofHours(1));
    }
}
