package com.lklass.domain.enrollment.scheduler;

import com.lklass.domain.enrollment.service.EnrollmentService;
import com.lklass.global.logging.AppLog;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@ConditionalOnProperty(name = "spring.task.scheduling.enabled", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
public class EnrollmentExpirationScheduler {

    private final EnrollmentService enrollmentService;

    @Scheduled(cron = "${lklass.scheduler.pending-expiration-cron}")
    @SchedulerLock(name = "enrollmentExpirationScheduler.expirePendingPayments")
    public void expirePendingPayments() {
        int expiredCount = enrollmentService.expirePendingPayments();
        if (expiredCount > 0) {
            AppLog.info(log, "PENDING_ENROLLMENT_EXPIRED", "count=" + expiredCount);
        }
    }
}
