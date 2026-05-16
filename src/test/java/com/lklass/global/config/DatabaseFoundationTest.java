package com.lklass.global.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.lklass.TestcontainersConfiguration;
import com.lklass.domain.course.scheduler.CourseStatusScheduler;
import com.lklass.domain.enrollment.scheduler.EnrollmentExpirationScheduler;
import com.lklass.global.config.properties.EnrollmentPolicyProperties;
import com.lklass.global.config.properties.JwtProperties;
import com.lklass.global.config.properties.SchedulerProperties;
import java.time.Duration;
import java.util.List;
import net.javacrumbs.shedlock.core.LockProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;

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

    @Autowired
    private Environment environment;

    @Autowired
    private ApplicationContext applicationContext;

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
    @DisplayName("SchedulerLockConfig는 Spring scheduling을 활성화한다")
    void enableScheduling() {
        // when
        EnableScheduling enableScheduling = SchedulerLockConfig.class.getAnnotation(EnableScheduling.class);

        // then
        assertThat(enableScheduling).isNotNull();
    }

    @Test
    @DisplayName("테스트 컨텍스트는 백그라운드 스케줄러 실행을 비활성화한다")
    void disableSchedulingInTestContext() {
        // given
        // src/test/resources/application.yaml의 테스트 전용 설정을 로드한다.

        // when
        boolean schedulingEnabled = environment.getProperty(
                "spring.task.scheduling.enabled",
                Boolean.class,
                true
        );

        // then
        assertThat(schedulingEnabled).isFalse();
        assertThat(applicationContext.getBeansOfType(CourseStatusScheduler.class)).isEmpty();
        assertThat(applicationContext.getBeansOfType(EnrollmentExpirationScheduler.class)).isEmpty();
    }

    @Test
    @DisplayName("Flyway는 Course 자동 상태 동기화 대상 조회 인덱스를 생성한다")
    void createCourseSchedulerIndexes() {
        // given
        List<String> indexNames = List.of(
                "idx_courses_auto_open_targets",
                "idx_courses_auto_close_targets"
        );

        // when & then
        indexNames.forEach(indexName -> {
            Integer indexCount = jdbcTemplate.queryForObject(
                    """
                            select count(*)
                              from information_schema.statistics
                             where table_schema = database()
                               and table_name = 'courses'
                               and index_name = ?
                            """,
                    Integer.class,
                    indexName
            );

            assertThat(indexCount).isGreaterThan(0);
        });
    }

    @Test
    @DisplayName("Flyway는 Enrollment 목록 조회와 PENDING 만료 대상 조회 인덱스를 생성한다")
    void createEnrollmentQueryIndexes() {
        // given
        List<String> indexNames = List.of(
                "idx_enrollments_user_created_at",
                "idx_enrollments_course_created_at",
                "idx_enrollments_pending_expiration"
        );

        // when & then
        indexNames.forEach(indexName -> {
            Integer indexCount = jdbcTemplate.queryForObject(
                    """
                            select count(*)
                              from information_schema.statistics
                             where table_schema = database()
                               and table_name = 'enrollments'
                               and index_name = ?
                            """,
                    Integer.class,
                    indexName
            );

            assertThat(indexCount).isGreaterThan(0);
        });
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
        assertThat(secret).isEqualTo("local-dev-secret-change-me-32bytes-minimum");
        assertThat(accessTokenTtl).isEqualTo(Duration.ofHours(1));
    }
}
