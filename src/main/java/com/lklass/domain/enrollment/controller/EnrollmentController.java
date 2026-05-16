package com.lklass.domain.enrollment.controller;

import com.lklass.domain.enrollment.dto.EnrollmentApplyResponse;
import com.lklass.domain.enrollment.dto.EnrollmentApplyResult;
import com.lklass.domain.enrollment.dto.EnrollmentQueryResult;
import com.lklass.domain.enrollment.service.EnrollmentService;
import com.lklass.global.common.CommonResponse;
import com.lklass.global.common.PageResponse;
import com.lklass.global.security.AuthenticatedUser;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class EnrollmentController {

    private final EnrollmentService enrollmentService;

    @PostMapping("/courses/{courseId}/enrollments")
    public CommonResponse<EnrollmentApplyResponse> applyEnrollment(
            @AuthenticationPrincipal AuthenticatedUser actor,
            @PathVariable Long courseId
    ) {
        EnrollmentApplyResult result = enrollmentService.apply(actor, courseId);
        return CommonResponse.success(EnrollmentApplyResponse.from(result));
    }

    @PostMapping("/enrollments/{enrollmentId}/confirm-payment")
    public CommonResponse<Void> confirmPayment(
            @AuthenticationPrincipal AuthenticatedUser actor,
            @PathVariable Long enrollmentId
    ) {
        enrollmentService.confirmPayment(actor, enrollmentId);
        return CommonResponse.success();
    }

    @PostMapping("/enrollments/{enrollmentId}/cancel")
    public CommonResponse<Void> cancelEnrollment(
            @AuthenticationPrincipal AuthenticatedUser actor,
            @PathVariable Long enrollmentId
    ) {
        enrollmentService.cancel(actor, enrollmentId);
        return CommonResponse.success();
    }

    @GetMapping("/me/enrollments")
    public CommonResponse<PageResponse<EnrollmentQueryResult>> getMyEnrollments(
            @AuthenticationPrincipal AuthenticatedUser actor,
            Pageable pageable
    ) {
        Page<EnrollmentQueryResult> result = enrollmentService.getMyEnrollments(actor, pageable);
        return CommonResponse.success(PageResponse.from(result));
    }

    @GetMapping("/courses/{courseId}/students")
    public CommonResponse<PageResponse<EnrollmentQueryResult>> getCourseStudents(
            @PathVariable Long courseId,
            Pageable pageable
    ) {
        Page<EnrollmentQueryResult> result = enrollmentService.getCourseStudents(courseId, pageable);
        return CommonResponse.success(PageResponse.from(result));
    }
}
