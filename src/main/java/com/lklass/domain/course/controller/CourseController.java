package com.lklass.domain.course.controller;

import com.lklass.domain.course.dto.CourseCreateRequest;
import com.lklass.domain.course.dto.CourseCreateResponse;
import com.lklass.domain.course.dto.CourseCreateResult;
import com.lklass.domain.course.service.CourseService;
import com.lklass.global.common.CommonResponse;
import com.lklass.global.security.AuthenticatedUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
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
}
