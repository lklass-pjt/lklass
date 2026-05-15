package com.lklass.domain.auth.service;

import com.lklass.domain.auth.dto.AccessTokenResult;
import com.lklass.domain.auth.exception.AuthErrorCode;
import com.lklass.domain.user.entity.User;
import com.lklass.domain.user.entity.UserRole;
import com.lklass.domain.user.exception.UserErrorCode;
import com.lklass.domain.user.repository.UserRepository;
import com.lklass.global.exception.BusinessException;
import com.lklass.global.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    @Transactional
    public AccessTokenResult signup(String email, String rawPassword, String name, UserRole role) {
        if (userRepository.existsByEmail(email)) {
            throw new BusinessException(UserErrorCode.DUPLICATED_EMAIL);
        }

        // 원문 비밀번호가 DB나 API 응답으로 나가지 않도록 저장 직전에 해시로 변환한다.
        String passwordHash = passwordEncoder.encode(rawPassword);
        User user = User.create(email, passwordHash, name, role);
        User savedUser = userRepository.save(user);

        return issueAccessToken(savedUser);
    }

    @Transactional(readOnly = true)
    public AccessTokenResult login(String email, String rawPassword) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException(AuthErrorCode.INVALID_CREDENTIALS));

        if (!passwordEncoder.matches(rawPassword, user.getPasswordHash())) {
            throw new BusinessException(AuthErrorCode.INVALID_CREDENTIALS);
        }

        return issueAccessToken(user);
    }

    private AccessTokenResult issueAccessToken(User user) {
        return new AccessTokenResult(jwtTokenProvider.createAccessToken(user.getId(), user.getRole()));
    }
}
