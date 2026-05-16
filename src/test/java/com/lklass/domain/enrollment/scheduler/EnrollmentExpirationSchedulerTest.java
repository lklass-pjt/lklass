package com.lklass.domain.enrollment.scheduler;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.lklass.domain.enrollment.service.EnrollmentService;
import java.lang.reflect.Method;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.scheduling.annotation.Scheduled;

class EnrollmentExpirationSchedulerTest {

    @Test
    @DisplayName("EnrollmentExpirationScheduler는 PENDING 결제 만료 유스케이스를 호출한다")
    void expirePendingPayments() {
        // given
        EnrollmentService enrollmentService = org.mockito.Mockito.mock(EnrollmentService.class);
        when(enrollmentService.expirePendingPayments()).thenReturn(3);
        EnrollmentExpirationScheduler scheduler = new EnrollmentExpirationScheduler(enrollmentService);

        // when
        scheduler.expirePendingPayments();

        // then
        verify(enrollmentService).expirePendingPayments();
    }

    @Test
    @DisplayName("EnrollmentExpirationScheduler는 yml cron과 ShedLock 설정으로 실행된다")
    void configureSchedulerAnnotations() throws Exception {
        // given
        Method method = EnrollmentExpirationScheduler.class.getDeclaredMethod("expirePendingPayments");

        // when
        Scheduled scheduled = method.getAnnotation(Scheduled.class);
        SchedulerLock schedulerLock = method.getAnnotation(SchedulerLock.class);

        // then
        org.assertj.core.api.Assertions.assertThat(scheduled).isNotNull();
        org.assertj.core.api.Assertions.assertThat(scheduled.cron())
                .isEqualTo("${lklass.scheduler.pending-expiration-cron}");
        org.assertj.core.api.Assertions.assertThat(schedulerLock).isNotNull();
        org.assertj.core.api.Assertions.assertThat(schedulerLock.name())
                .isEqualTo("enrollmentExpirationScheduler.expirePendingPayments");
    }
}
