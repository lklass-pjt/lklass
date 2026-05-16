package com.lklass.domain.user.repository;

import com.lklass.domain.user.entity.User;
import com.lklass.domain.user.exception.UserErrorCode;
import com.lklass.global.exception.BusinessException;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class UserRepository {

    private static final String EMAIL_UNIQUE_CONSTRAINT_NAME = "uk_users_email";

    private final UserJpaRepository userJpaRepository;

    public User save(User user) {
        try {
            return userJpaRepository.saveAndFlush(user);
        } catch (DataIntegrityViolationException exception) {
            if (hasConstraintName(exception, EMAIL_UNIQUE_CONSTRAINT_NAME)) {
                throw new BusinessException(UserErrorCode.DUPLICATED_EMAIL);
            }
            throw exception;
        }
    }

    public boolean existsByEmail(String email) {
        return userJpaRepository.existsByEmail(email);
    }

    public Optional<User> findByEmail(String email) {
        return userJpaRepository.findByEmail(email);
    }

    public Optional<User> findById(Long userId) {
        return userJpaRepository.findById(userId);
    }

    private boolean hasConstraintName(Throwable throwable, String constraintName) {
        Throwable current = throwable;
        while (current != null) {
            String message = current.getMessage();
            if (message != null && message.contains(constraintName)) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }
}
