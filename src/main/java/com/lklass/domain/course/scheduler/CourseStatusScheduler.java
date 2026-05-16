package com.lklass.domain.course.scheduler;

import com.lklass.domain.course.service.CourseService;
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
public class CourseStatusScheduler {

    private final CourseService courseService;

    @Scheduled(cron = "${lklass.scheduler.course-status-cron}")
    @SchedulerLock(name = "courseStatusScheduler.synchronizeCourseStatuses")
    public void synchronizeCourseStatuses() {
        int openedCount = courseService.openReservedCourses();
        if (openedCount > 0) {
            AppLog.info(log, "COURSE_AUTO_OPENED", "count={}", openedCount);
        }
        int closedCount = courseService.closeExpiredOpenCourses();
        if (closedCount > 0) {
            AppLog.info(log, "COURSE_AUTO_CLOSED", "count={}", closedCount);
        }
    }
}
