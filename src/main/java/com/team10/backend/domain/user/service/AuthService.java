package com.team10.backend.domain.user.service;

import com.team10.backend.domain.user.dto.AuthRegisterRequest;
import com.team10.backend.domain.user.dto.AuthRegisterResponse;
import com.team10.backend.domain.user.entity.User;
import com.team10.backend.domain.user.repository.AuthRepository;
import com.team10.backend.global.exception.BusinessException;
import com.team10.backend.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthRepository authRepository;

    @Transactional
    public AuthRegisterResponse register(AuthRegisterRequest request) {
        validateDuplicate(request);

        // TODO: 비밀번호 암호화

        User savedUser = authRepository.save(User.create(request));
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
