package com.team10.backend.domain.user.service;

import com.team10.backend.domain.user.dto.SellerPublicResponse;
import com.team10.backend.domain.user.dto.SellerResponse;
import com.team10.backend.domain.user.dto.SellerUpdateRequest;
import com.team10.backend.domain.user.dto.UserResponse;
import com.team10.backend.domain.user.dto.UserUpdateRequest;
import com.team10.backend.domain.user.entity.SellerInfo;
import com.team10.backend.domain.user.entity.User;
import com.team10.backend.domain.user.enums.Role;
import com.team10.backend.domain.user.repository.UserRepository;
import com.team10.backend.global.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static com.team10.backend.global.exception.ErrorCode.NOT_SELLER;
import static com.team10.backend.global.exception.ErrorCode.USER_NOT_FOUND;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public UserResponse getUserProfile(Long userId) {
        User user = getUserEntity(userId);

        return UserResponse.from(user);
    }

    @Transactional(readOnly = true)
    public SellerResponse getSellerProfile(Long userId) {
        User user = getUserEntity(userId);

        validateSellerRole(user);

        return SellerResponse.from(user);
    }

    @Transactional(readOnly = true)
    public SellerPublicResponse getSellerPublicProfile(Long id) {
        User user = getUserEntity(id);

        return SellerPublicResponse.from(user);
    }

    @Transactional
    public UserResponse updateMyUserProfile(Long id, UserUpdateRequest request) {
        User user = getUserEntity(id);
        user.updateUserInfo(
                request.nickname(),
                request.phoneNumber(),
                request.address()
                );

        return UserResponse.from(user);
    }

    @Transactional
    public SellerResponse updateMySellerProfile(Long id, SellerUpdateRequest request) {
        User user = getUserEntity(id);

        validateSellerRole(user);

        user.updateUserInfo(
                request.nickname(),
                request.phoneNumber(),
                request.address()
        );

        SellerInfo sellerInfo = user.getSellerInfo();
        sellerInfo.updateSellerInfo(
                request.bio(),
                request.businessNumber()
        );

        return SellerResponse.from(user);
    }

    private User getUserEntity(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(USER_NOT_FOUND));
    }

    private void validateSellerRole(User user) {
        if(user.getRole() != Role.SELLER) {
            throw new BusinessException(NOT_SELLER);
        }
    }
}
