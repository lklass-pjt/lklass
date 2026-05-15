package com.lklass.domain.auth.dto;

import com.lklass.domain.user.entity.UserRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record SignupRequest(
        @NotBlank(message = "email은 필수입니다.")
        @Email(message = "email 형식이 올바르지 않습니다.")
        String email,

        @NotBlank(message = "password는 필수입니다.")
        String password,

        @NotBlank(message = "name은 필수입니다.")
        String name,

        @NotNull(message = "role은 필수입니다.")
        UserRole role
) {
}
