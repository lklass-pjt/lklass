package com.lklass.domain.auth.controller;

import com.lklass.domain.auth.dto.AccessTokenResult;
import com.lklass.domain.auth.dto.LoginRequest;
import com.lklass.domain.auth.dto.SignupRequest;
import com.lklass.domain.auth.service.AuthService;
import com.lklass.global.common.CommonResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/signup")
    public CommonResponse<AccessTokenResult> signup(@Valid @RequestBody SignupRequest request) {
        return CommonResponse.success(
                authService.signup(request.email(), request.password(), request.name(), request.role())
        );
    }

    @PostMapping("/login")
    public CommonResponse<AccessTokenResult> login(@Valid @RequestBody LoginRequest request) {
        return CommonResponse.success(authService.login(request.email(), request.password()));
    }
}
