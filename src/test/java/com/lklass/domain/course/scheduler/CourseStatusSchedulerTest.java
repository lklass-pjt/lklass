package com.lklass.domain.course.scheduler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.lklass.domain.course.service.CourseService;
import java.lang.reflect.Method;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;

class CourseStatusSchedulerTest {

    @Test
    @DisplayName("CourseStatusScheduler는 자동 OPEN과 자동 CLOSED 유스케이스를 함께 호출한다")
    void synchronizeCourseStatuses() {
        // given
        CourseService courseService = mock(CourseService.class);
        when(courseService.openReservedCourses()).thenReturn(2);
        when(courseService.closeExpiredOpenCourses()).thenReturn(1);
        CourseStatusScheduler scheduler = new CourseStatusScheduler(courseService);

        // when
        scheduler.synchronizeCourseStatuses();

        // then
        verify(courseService).openReservedCourses();
        verify(courseService).closeExpiredOpenCourses();
    }

    @Test
    @DisplayName("CourseStatusScheduler는 자동 변경 대상이 없어도 두 유스케이스를 정상 호출한다")
    void synchronizeCourseStatusesWithNoTargets() {
        // given
        CourseService courseService = mock(CourseService.class);
        when(courseService.openReservedCourses()).thenReturn(0);
        when(courseService.closeExpiredOpenCourses()).thenReturn(0);
        CourseStatusScheduler scheduler = new CourseStatusScheduler(courseService);

        // when
        scheduler.synchronizeCourseStatuses();

        // then
        verify(courseService).openReservedCourses();
        verify(courseService).closeExpiredOpenCourses();
    }

    @Test
    @DisplayName("CourseStatusScheduler는 자동 OPEN 이후 자동 CLOSED 순서로 상태를 동기화한다")
    void synchronizeCourseStatusesInOrder() {
        // given
        CourseService courseService = mock(CourseService.class);
        CourseStatusScheduler scheduler = new CourseStatusScheduler(courseService);

        // when
        scheduler.synchronizeCourseStatuses();

        // then
        InOrder inOrder = inOrder(courseService);
        inOrder.verify(courseService).openReservedCourses();
        inOrder.verify(courseService).closeExpiredOpenCourses();
    }

    @Test
    @DisplayName("CourseStatusScheduler의 상태 동기화 작업은 yml cron과 ShedLock 설정으로 실행된다")
    void configureSynchronizeCourseStatusesSchedule() throws NoSuchMethodException {
        // given
        Method method = CourseStatusScheduler.class.getDeclaredMethod("synchronizeCourseStatuses");

        // when
        Scheduled scheduled = method.getAnnotation(Scheduled.class);
        SchedulerLock schedulerLock = method.getAnnotation(SchedulerLock.class);

        // then
        assertThat(scheduled).isNotNull();
        assertThat(scheduled.cron()).isEqualTo("${lklass.scheduler.course-status-cron}");
        assertThat(schedulerLock).isNotNull();
        assertThat(schedulerLock.name()).isEqualTo("courseStatusScheduler.synchronizeCourseStatuses");
    }

    @Test
    @DisplayName("CourseStatusScheduler는 scheduling이 활성화된 경우에만 bean으로 등록된다")
    void configureConditionalSchedulingBean() {
        // when
        ConditionalOnProperty conditionalOnProperty = CourseStatusScheduler.class
                .getAnnotation(ConditionalOnProperty.class);

        // then
        assertThat(conditionalOnProperty).isNotNull();
        assertThat(conditionalOnProperty.name()).containsExactly("spring.task.scheduling.enabled");
        assertThat(conditionalOnProperty.havingValue()).isEqualTo("true");
        assertThat(conditionalOnProperty.matchIfMissing()).isTrue();
    }
}
