package com.lklass.domain.course.controller;

import com.lklass.domain.course.dto.CourseCreateRequest;
import com.lklass.domain.course.dto.CourseCreateResponse;
import com.lklass.domain.course.dto.CourseCreateResult;
import com.lklass.domain.course.dto.CourseOpenRequest;
import com.lklass.domain.course.dto.CourseQueryResult;
import com.lklass.domain.course.entity.CourseStatus;
import com.lklass.domain.course.service.CourseService;
import com.lklass.global.common.CommonResponse;
import com.lklass.global.common.PageResponse;
import com.lklass.global.security.AuthenticatedUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/courses")
public class CourseController {

    private final CourseService courseService;

    @PostMapping
    public CommonResponse<CourseCreateResponse> createCourse(
            @AuthenticationPrincipal AuthenticatedUser actor,
            @Valid @RequestBody CourseCreateRequest request
    ) {
        CourseCreateResult result = courseService.createCourse(
                actor,
                request.creatorId(),
                request.title(),
                request.description(),
                request.price(),
                request.capacity(),
                request.enrollmentStartAt(),
                request.enrollmentEndAt(),
                request.courseStartAt(),
                request.courseEndAt()
        );

        return CommonResponse.success(CourseCreateResponse.from(result));
    }

    @GetMapping
    public CommonResponse<PageResponse<CourseQueryResult>> getCourses(
            @RequestParam(required = false) CourseStatus status,
            Pageable pageable
    ) {
        Page<CourseQueryResult> result = courseService.getCourses(status, pageable);
        return CommonResponse.success(PageResponse.from(result));
    }

    @GetMapping("/{courseId}")
    public CommonResponse<CourseQueryResult> getCourse(@PathVariable Long courseId) {
        return CommonResponse.success(courseService.getCourse(courseId));
    }

    @PatchMapping("/{courseId}/open")
    public CommonResponse<Void> openCourse(
            @AuthenticationPrincipal AuthenticatedUser actor,
            @PathVariable Long courseId,
            @Valid @RequestBody CourseOpenRequest request
    ) {
        courseService.openCourse(actor, courseId, request.enrollmentEndAt());
        return CommonResponse.success();
    }

    @PatchMapping("/{courseId}/close")
    public CommonResponse<Void> closeCourse(
            @AuthenticationPrincipal AuthenticatedUser actor,
            @PathVariable Long courseId
    ) {
        courseService.closeCourse(actor, courseId);
        return CommonResponse.success();
    }
}
