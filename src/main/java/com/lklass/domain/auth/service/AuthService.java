package com.lklass.domain.auth.service;

import com.lklass.domain.user.entity.User;
import com.lklass.domain.user.entity.UserRole;
import com.lklass.domain.user.exception.UserErrorCode;
import com.lklass.domain.user.repository.UserRepository;
import com.lklass.global.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public Long signup(String email, String rawPassword, String name, UserRole role) {
        if (userRepository.existsByEmail(email)) {
            throw new BusinessException(UserErrorCode.DUPLICATED_EMAIL);
        }

        // 원문 비밀번호가 DB나 API 응답으로 나가지 않도록 저장 직전에 해시로 변환한다.
        String passwordHash = passwordEncoder.encode(rawPassword);
        User user = User.create(email, passwordHash, name, role);

        return userRepository.save(user).getId();
    }
}
