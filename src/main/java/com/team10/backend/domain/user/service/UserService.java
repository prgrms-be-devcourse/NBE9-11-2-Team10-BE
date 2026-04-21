package com.team10.backend.domain.user.service;

import com.team10.backend.domain.user.dto.UserResponse;
import com.team10.backend.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    public UserResponse getUser() {

        return null;
    }
}
