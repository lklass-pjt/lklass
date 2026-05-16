package com.lklass.domain.enrollment.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.lklass.domain.enrollment.exception.EnrollmentErrorCode;
import com.lklass.global.exception.BusinessException;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class EnrollmentEntityTest {

    private static final LocalDateTime ENROLLED_AT = LocalDateTime.of(2026, 5, 16, 10, 0);
    private static final LocalDateTime CONFIRMED_AT = LocalDateTime.of(2026, 5, 16, 10, 5);

    @Test
    @DisplayName("PENDING 신청을 confirm하면 CONFIRMED 상태와 결제 확정 시각을 저장한다")
    void confirmPendingEnrollment() {
        // given
        Enrollment enrollment = Enrollment.create(1L, 2L, ENROLLED_AT);

        // when
        enrollment.confirm(CONFIRMED_AT);

        // then
        assertThat(enrollment.getStatus()).isEqualTo(EnrollmentStatus.CONFIRMED);
        assertThat(enrollment.getConfirmedAt()).isEqualTo(CONFIRMED_AT);
        assertThat(enrollment.getEnrolledAt()).isEqualTo(ENROLLED_AT);
        assertThat(enrollment.getCancelledAt()).isNull();
    }

    @Test
    @DisplayName("confirm 시 결제 확정 시각이 없으면 실패하고 상태를 변경하지 않는다")
    void rejectNullConfirmedAt() {
        // given
        Enrollment enrollment = Enrollment.create(1L, 2L, ENROLLED_AT);

        // when & then
        assertThatThrownBy(() -> enrollment.confirm(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("confirmedAt must not be null");
        assertThat(enrollment.getStatus()).isEqualTo(EnrollmentStatus.PENDING);
        assertThat(enrollment.getConfirmedAt()).isNull();
    }

    @Test
    @DisplayName("이미 CONFIRMED 상태인 신청을 다시 confirm하면 INVALID_ENROLLMENT_STATUS 예외가 발생한다")
    void rejectAlreadyConfirmedEnrollment() {
        // given
        Enrollment enrollment = Enrollment.create(1L, 2L, ENROLLED_AT);
        enrollment.confirm(CONFIRMED_AT);

        // when & then
        assertThatThrownBy(() -> enrollment.confirm(CONFIRMED_AT.plusMinutes(1)))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.errorCode()).isEqualTo(EnrollmentErrorCode.INVALID_ENROLLMENT_STATUS)
                );
        assertThat(enrollment.getStatus()).isEqualTo(EnrollmentStatus.CONFIRMED);
        assertThat(enrollment.getConfirmedAt()).isEqualTo(CONFIRMED_AT);
    }
}
