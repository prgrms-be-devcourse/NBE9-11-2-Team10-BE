package com.team10.backend.domain.auth.service;

import com.team10.backend.domain.auth.dto.AuthRegisterRequest;
import com.team10.backend.domain.auth.dto.AuthRegisterResponse;
import com.team10.backend.domain.auth.dto.DuplicateCheckResponse;
import com.team10.backend.domain.auth.dto.LoginRequest;
import com.team10.backend.domain.auth.dto.LoginResponse;
import com.team10.backend.domain.auth.dto.LoginResult;
import com.team10.backend.domain.user.entity.User;
import com.team10.backend.domain.user.enums.DuplicateType;
import com.team10.backend.domain.user.repository.UserRepository;
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

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenProvider tokenProvider;
    private final RefreshTokenService refreshTokenService;

    @Transactional
    public AuthRegisterResponse register(AuthRegisterRequest request) {
        validateDuplicateUser(request);

        String encodedPassword = passwordEncoder.encode(request.password());

        User user = User.create(request, encodedPassword);
        User savedUser = userRepository.save(user);

        return AuthRegisterResponse.from(savedUser);
    }

    private void validateDuplicateUser(AuthRegisterRequest request) {
        if(userRepository.existsByEmail(request.email())) {
            throw new BusinessException(ErrorCode.DUPLICATE_EMAIL);
        }
        if(userRepository.existsByNickname(request.nickname())) {
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
            case EMAIL -> !userRepository.existsByEmail(value);
            case NICKNAME -> !userRepository.existsByNickname(value);
        };
        return new DuplicateCheckResponse(type, value, available);
    }

    public LoginResult login(LoginRequest request) {
        User user = authenticate(request);
        String accessToken = tokenProvider.generateToken(user.getId(), user.getRole());
        String refreshToken = refreshTokenService.createRefreshToken(user);

        LoginResponse response = LoginResponse.from(user);
        return new LoginResult(response, accessToken, refreshToken);
    }

    private User authenticate(LoginRequest request) {
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new BusinessException(LOGIN_FAILED));

        if(!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new BusinessException(LOGIN_FAILED);
        }

        return user;
    }

}
