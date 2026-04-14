package com.team10.backend.domain.user.service;

import com.team10.backend.domain.user.dto.AuthRegisterRequest;
import com.team10.backend.domain.user.dto.AuthRegisterResponse;
import com.team10.backend.domain.user.entity.User;
import com.team10.backend.domain.user.repository.AuthRepository;
import com.team10.backend.global.exception.BusinessException;
import com.team10.backend.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthRepository authRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public AuthRegisterResponse register(AuthRegisterRequest request) {
        validateDuplicate(request);

        String hashedPassword = passwordEncoder.encode(request.password());

        User savedUser = authRepository.save(User.create(request, hashedPassword));
        return AuthRegisterResponse.from(savedUser);
    }

    private void validateDuplicate(AuthRegisterRequest request) {
        if(authRepository.existsByEmail(request.email())) {
            throw new BusinessException(ErrorCode.DUPLICATE_EMAIL);
        }
        if(authRepository.existsByNickname(request.nickname())) {
            throw new BusinessException(ErrorCode.DUPLICATE_NICKNAME);
        }
    }

}
