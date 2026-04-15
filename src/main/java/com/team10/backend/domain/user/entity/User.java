package com.team10.backend.domain.user.entity;

import com.team10.backend.domain.user.dto.AuthRegisterRequest;
import com.team10.backend.domain.user.enums.Role;
import com.team10.backend.domain.user.enums.UserStatus;
import com.team10.backend.global.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Table(name = "users")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User extends BaseEntity {

    @Column(name = "image_url")
    private String imageUrl;

    @Column(nullable = false, unique = true, length = 255)
    private String email;

    @Column(nullable = false, length = 255)
    private String password;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(nullable = false, unique = true, length = 255)
    private String nickname;

    @Column(name = "phone_number", nullable = false, length = 255)
    private String phoneNumber;

    @Column(nullable = false, length = 255)
    private String address;

    @Enumerated(EnumType.STRING)
    @Column(name = "user_status", nullable = false)
    private UserStatus userStatus;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    public static User create(AuthRegisterRequest request, String hashedPassword) {
        return User.builder()
                .email(request.email())
                .password(hashedPassword)
                .name(request.name())
                .nickname(request.nickname())
                .phoneNumber(request.phoneNumber())
                .address(request.address())
                .userStatus(UserStatus.ACTIVE)
                .role(request.role())
                .build();
    }

}
