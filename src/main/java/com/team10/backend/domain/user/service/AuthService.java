package com.team10.backend.domain.user.service;

import com.team10.backend.domain.user.dto.AuthRegisterRequest;
import com.team10.backend.domain.user.dto.AuthRegisterResponse;
import com.team10.backend.domain.user.dto.DuplicateCheckResponse;
import com.team10.backend.domain.user.dto.LoginRequest;
import com.team10.backend.domain.user.dto.LoginResponse;
import com.team10.backend.domain.user.entity.User;
import com.team10.backend.domain.user.enums.DuplicateType;
import com.team10.backend.domain.user.repository.AuthRepository;
import com.team10.backend.global.exception.BusinessException;
import com.team10.backend.global.exception.ErrorCode;
import com.team10.backend.global.security.TokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static com.team10.backend.global.exception.ErrorCode.INVALID_INPUT;
import static com.team10.backend.global.exception.ErrorCode.LOGIN_FAILED;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthRepository authRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenProvider tokenProvider;

    @Transactional
    public AuthRegisterResponse register(AuthRegisterRequest request) {
        validateDuplicateUser(request);

        String encodedPassword = passwordEncoder.encode(request.password());

        User user = User.create(request, encodedPassword);
        User savedUser = authRepository.save(user);

        return AuthRegisterResponse.from(savedUser);
    }

    private void validateDuplicateUser(AuthRegisterRequest request) {
        if(authRepository.existsByEmail(request.email())) {
            throw new BusinessException(ErrorCode.DUPLICATE_EMAIL);
        }
        if(authRepository.existsByNickname(request.nickname())) {
            throw new BusinessException(ErrorCode.DUPLICATE_NICKNAME);
        }
    }

    @Transactional(readOnly = true)
    public DuplicateCheckResponse checkDuplicate(DuplicateType type, String value) {
        if (value == null || value.isBlank()) {
            throw new BusinessException(INVALID_INPUT);
        }
        value = value.trim().toLowerCase();
        boolean available = switch (type) {
            case EMAIL -> !authRepository.existsByEmail(value);
            case NICKNAME -> !authRepository.existsByNickname(value);
        };
        return new DuplicateCheckResponse(type, value, available);
    }

    public LoginResponse login(LoginRequest request) {
        User user = authenticate(request);
        String accessToken = generateToken(user);

        return LoginResponse.from(user);
    }

    private User authenticate(LoginRequest request) {
        User user = authRepository.findByEmail(request.email())
                .orElseThrow(() -> new BusinessException(LOGIN_FAILED));

        if(!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new BusinessException(LOGIN_FAILED);
        }

        return user;
    }

    private String generateToken(User user) {
        return tokenProvider.generateToken(user.getId(), user.getRole());
    }

}
