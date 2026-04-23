package com.team10.backend.domain.user.unit;

import com.team10.backend.domain.image.service.ImageUploadService;
import com.team10.backend.domain.user.dto.ProfileImageUpdateRequest;
import com.team10.backend.domain.user.dto.SellerResponse;
import com.team10.backend.domain.user.dto.SellerUpdateRequest;
import com.team10.backend.domain.user.dto.UserResponse;
import com.team10.backend.domain.user.dto.UserUpdateRequest;
import com.team10.backend.domain.user.entity.User;
import com.team10.backend.domain.user.repository.UserRepository;
import com.team10.backend.domain.user.service.UserService;
import com.team10.backend.global.exception.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Optional;

import static com.team10.backend.global.exception.ErrorCode.NOT_SELLER;
import static com.team10.backend.global.exception.ErrorCode.USER_NOT_FOUND;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private ImageUploadService imageUploadService;

    @InjectMocks
    private UserService userService;

    @Test
    @DisplayName("사용자 개인정보 조회 - 성공")
    void getUserProfile_success() {
        // given
        User user = UserTestFixture.createBuyer();

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        // when
        UserResponse response = userService.getUserProfile(1L);

        // then
        assertNotNull(response);
        assertEquals(user.getName(), response.name());
    }

    @Test
    @DisplayName("판매자 개인정보 조회 - 성공")
    void getSellerProfile_success() {
        // given
        User user = UserTestFixture.createSeller();

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        // when
        SellerResponse response = userService.getSellerProfile(1L);

        // then
        assertNotNull(response);
        assertEquals(user.getName(), response.name());
    }

    @Test
    @DisplayName("판매자 정보 조회 - 실패 (판매자가 아닌 경우)")
    void getSellerProfile_fail_notSeller() {
        // given
        User user = UserTestFixture.createBuyer();

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        // when & then
        BusinessException ex = assertThrows(BusinessException.class,
                () -> userService.getSellerProfile(1L));

        assertEquals(NOT_SELLER, ex.getErrorCode());
    }

    @Test
    @DisplayName("사용자 개인정보 수정 - 성공")
    void updateMyUserProfile_success() {
        // given
        User user = UserTestFixture.createBuyer();
        user.updateProfileImage("https://old-image.test/profile.jpg");

        UserUpdateRequest request = new UserUpdateRequest(
                "새로운닉네임",
                "010-9999-9999",
                "부산"
        );

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        // when
        UserResponse response = userService.updateMyUserProfile(1L, request);

        // then
        assertNotNull(response);
        assertEquals("새로운닉네임", response.nickname());
        assertEquals("010-9999-9999", response.phoneNumber());
        assertEquals("부산", response.address());
    }

    @Test
    @DisplayName("사용자 프로필 이미지 수정 - 성공")
    void updateMyUserProfileImage_success() {
        User user = UserTestFixture.createBuyer();
        user.updateProfileImage("https://old-image.test/profile.jpg");

        ProfileImageUpdateRequest request =
                new ProfileImageUpdateRequest("https://new-image.test/profile.jpg");

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        UserResponse response = userService.updateMyProfileImage(1L, request);

        assertEquals("https://new-image.test/profile.jpg", response.imageUrl());
        verify(imageUploadService).deleteIfManaged("https://old-image.test/profile.jpg");
    }

    @Test
    @DisplayName("사용자 프로필 이미지 삭제 - 성공")
    void deleteMyUserProfileImage_success() {
        User user = UserTestFixture.createBuyer();
        user.updateProfileImage("https://old-image.test/profile.jpg");

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        UserResponse response = userService.deleteMyProfileImage(1L);

        assertNull(response.imageUrl());
        verify(imageUploadService).deleteIfManaged("https://old-image.test/profile.jpg");
    }

    @Test
    @DisplayName("판매자 개인정보 수정 - 성공")
    void updateMySellerProfile_success() {
        // given
        User user = UserTestFixture.createSeller();

        SellerUpdateRequest request = new SellerUpdateRequest(
                "새로운판매자",
                "010-8888-8888",
                "대구",
                "새로운 인사말입니다.",
                "999-999-99999"
        );

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        // when
        SellerResponse response = userService.updateMySellerProfile(1L, request);

        // then
        assertNotNull(response);
        assertEquals("새로운판매자", response.nickname());
        assertEquals("대구", response.address());
        assertEquals("새로운 인사말입니다.", response.bio());
        assertEquals("999-999-99999", response.businessNumber());
    }

    @Test
    @DisplayName("존재하지 않는 사용자인 경우")
    void getUserEntity_fail_notFound() {
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class,
                () -> userService.getUserProfile(1L));

        assertEquals(USER_NOT_FOUND, ex.getErrorCode());
    }

}
