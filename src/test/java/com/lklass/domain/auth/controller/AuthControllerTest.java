package com.lklass.domain.auth.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lklass.domain.auth.config.AuthSecurityConfigurer;
import com.lklass.domain.auth.dto.AccessTokenResult;
import com.lklass.domain.auth.exception.AuthErrorCode;
import com.lklass.domain.auth.service.AuthService;
import com.lklass.domain.user.entity.UserRole;
import com.lklass.domain.user.exception.UserErrorCode;
import com.lklass.global.config.SecurityConfig;
import com.lklass.global.exception.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@Import({SecurityConfig.class, AuthSecurityConfigurer.class})
@WebMvcTest(controllers = AuthController.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private AuthService authService;

    @Test
    @DisplayName("회원가입 API는 요청을 검증한 뒤 Access Token을 공통 성공 응답으로 반환한다")
    void signup() throws Exception {
        // given
        SignupRequestFixture request = new SignupRequestFixture(
                "creator@example.com",
                "password1234",
                "크리에이터 A",
                UserRole.CREATOR
        );
        when(authService.signup(
                eq("creator@example.com"),
                eq("password1234"),
                eq("크리에이터 A"),
                eq(UserRole.CREATOR)
        )).thenReturn(new AccessTokenResult("signup-token"));

        // when & then
        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").value("signup-token"));
    }

    @Test
    @DisplayName("로그인 API는 email과 password가 일치하면 Access Token을 공통 성공 응답으로 반환한다")
    void login() throws Exception {
        // given
        LoginRequestFixture request = new LoginRequestFixture("student@example.com", "password1234");
        when(authService.login("student@example.com", "password1234"))
                .thenReturn(new AccessTokenResult("login-token"));

        // when & then
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").value("login-token"));
    }

    @Test
    @DisplayName("회원가입 API는 잘못된 email 형식이면 VALIDATION_ERROR 응답을 반환한다")
    void rejectInvalidSignupRequest() throws Exception {
        // given
        SignupRequestFixture request = new SignupRequestFixture(
                "invalid-email",
                "password1234",
                "크리에이터 A",
                UserRole.CREATOR
        );

        // when & then
        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value("email: email 형식이 올바르지 않습니다."));
    }

    @Test
    @DisplayName("회원가입 API는 존재하지 않는 role이면 VALIDATION_ERROR 응답을 반환한다")
    void rejectUnknownSignupRole() throws Exception {
        // given
        String request = """
                {
                  "email": "student@example.com",
                  "password": "password1234",
                  "name": "학생 A",
                  "role": "UNKNOWN"
                }
                """;

        // when & then
        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value("request body: 요청 본문을 읽을 수 없습니다."));
    }

    @Test
    @DisplayName("회원가입 API는 중복 email이면 DUPLICATED_EMAIL 응답을 반환한다")
    void rejectDuplicatedEmail() throws Exception {
        // given
        SignupRequestFixture request = new SignupRequestFixture(
                "duplicated@example.com",
                "password1234",
                "학생 A",
                UserRole.STUDENT
        );
        when(authService.signup(any(), any(), any(), any()))
                .thenThrow(new BusinessException(UserErrorCode.DUPLICATED_EMAIL));

        // when & then
        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value(UserErrorCode.DUPLICATED_EMAIL.code()));
    }

    @Test
    @DisplayName("로그인 API는 인증 정보가 틀리면 INVALID_CREDENTIALS 응답을 반환한다")
    void rejectInvalidCredentials() throws Exception {
        // given
        LoginRequestFixture request = new LoginRequestFixture("student@example.com", "wrong-password");
        when(authService.login("student@example.com", "wrong-password"))
                .thenThrow(new BusinessException(AuthErrorCode.INVALID_CREDENTIALS));

        // when & then
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value(AuthErrorCode.INVALID_CREDENTIALS.code()));
    }

    @Test
    @DisplayName("로그인 API는 password가 비어 있으면 VALIDATION_ERROR와 구체적인 메시지를 반환한다")
    void rejectBlankLoginPassword() throws Exception {
        // given
        LoginRequestFixture request = new LoginRequestFixture("student@example.com", "");

        // when & then
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value("password: password는 필수입니다."));
    }

    @Test
    @DisplayName("AuthSecurityConfigurer에 등록되지 않은 API는 인증 없이는 401로 차단된다")
    void rejectUnauthenticatedUnknownApi() throws Exception {
        // given
        // SecurityConfig의 fallback anyRequest().authenticated() 정책을 사용한다.

        // when & then
        mockMvc.perform(get("/api/courses"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser
    @DisplayName("AuthSecurityConfigurer에 등록되지 않은 API는 인증되면 security filter를 통과한다")
    void allowAuthenticatedUnknownApi() throws Exception {
        // given
        // Security filter 통과 후 실제 컨트롤러가 없으면 404로 응답한다.

        // when & then
        mockMvc.perform(get("/api/courses"))
                .andExpect(status().isNotFound());
    }

    private record SignupRequestFixture(
            String email,
            String password,
            String name,
            UserRole role
    ) {
    }

    private record LoginRequestFixture(
            String email,
            String password
    ) {
    }
}
