package com.lklass.domain.enrollment.controller;

import com.lklass.domain.enrollment.dto.EnrollmentApplyResponse;
import com.lklass.domain.enrollment.dto.EnrollmentApplyResult;
import com.lklass.domain.enrollment.service.EnrollmentService;
import com.lklass.global.common.CommonResponse;
import com.lklass.global.security.AuthenticatedUser;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/courses/{courseId}/enrollments")
public class EnrollmentController {

    private final EnrollmentService enrollmentService;

    @PostMapping
    public CommonResponse<EnrollmentApplyResponse> applyEnrollment(
            @AuthenticationPrincipal AuthenticatedUser actor,
            @PathVariable Long courseId
    ) {
        EnrollmentApplyResult result = enrollmentService.apply(actor, courseId);
        return CommonResponse.success(EnrollmentApplyResponse.from(result));
    }
}
