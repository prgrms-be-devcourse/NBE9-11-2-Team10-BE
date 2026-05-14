package com.team10.backend.domain.user.entity;

import com.team10.backend.domain.auth.dto.AuthRegisterRequest;
import com.team10.backend.domain.user.enums.Role;
import com.team10.backend.domain.user.enums.UserStatus;
import com.team10.backend.global.entity.BaseEntity;
import jakarta.persistence.*;

@Entity
@Table(name = "users")
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

    @OneToOne(mappedBy = "user",
            orphanRemoval = true,
            cascade = CascadeType.ALL)
    private SellerInfo sellerInfo;

    public static User create(AuthRegisterRequest request,
                              String hashedPassword,
                              Role role
    ) {
        return User.builder()
                .email(request.email())
                .password(hashedPassword)
                .name(request.name())
                .nickname(request.nickname())
                .phoneNumber(request.phoneNumber())
                .address(request.address())
                .userStatus(UserStatus.ACTIVE)
                .role(role)
                .build();
    }

    public void attachSellerInfo(SellerInfo sellerInfo) {
        this.sellerInfo = sellerInfo;
        sellerInfo.linkUser(this);
    }

    public void updateUserInfo(String nickname, String phoneNumber, String address) {
        this.nickname = nickname;
        this.phoneNumber = phoneNumber;
        this.address = address;
    }

    public void updateProfileImage(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getImageUrl() {
        return this.imageUrl;
    }

    public String getEmail() {
        return this.email;
    }

    public String getPassword() {
        return this.password;
    }

    public String getName() {
        return this.name;
    }

    public String getNickname() {
        return this.nickname;
    }

    public String getPhoneNumber() {
        return this.phoneNumber;
    }

    public String getAddress() {
        return this.address;
    }

    public UserStatus getUserStatus() {
        return this.userStatus;
    }

    public Role getRole() {
        return this.role;
    }

    public SellerInfo getSellerInfo() {
        return this.sellerInfo;
    }

    public User(String imageUrl, String email, String password, String name, String nickname, String phoneNumber, String address, UserStatus userStatus, Role role, SellerInfo sellerInfo) {
        this.imageUrl = imageUrl;
        this.email = email;
        this.password = password;
        this.name = name;
        this.nickname = nickname;
        this.phoneNumber = phoneNumber;
        this.address = address;
        this.userStatus = userStatus;
        this.role = role;
        this.sellerInfo = sellerInfo;
    }

    public User() {
    }

    public static class UserBuilder {
        private String imageUrl;
        private String email;
        private String password;
        private String name;
        private String nickname;
        private String phoneNumber;
        private String address;
        private UserStatus userStatus;
        private Role role;
        private SellerInfo sellerInfo;

        UserBuilder() {
        }

        public UserBuilder imageUrl(String imageUrl) {
            this.imageUrl = imageUrl;
            return this;
        }

        public UserBuilder email(String email) {
            this.email = email;
            return this;
        }

        public UserBuilder password(String password) {
            this.password = password;
            return this;
        }

        public UserBuilder name(String name) {
            this.name = name;
            return this;
        }

        public UserBuilder nickname(String nickname) {
            this.nickname = nickname;
            return this;
        }

        public UserBuilder phoneNumber(String phoneNumber) {
            this.phoneNumber = phoneNumber;
            return this;
        }

        public UserBuilder address(String address) {
            this.address = address;
            return this;
        }

        public UserBuilder userStatus(UserStatus userStatus) {
            this.userStatus = userStatus;
            return this;
        }

        public UserBuilder role(Role role) {
            this.role = role;
            return this;
        }

        public UserBuilder sellerInfo(SellerInfo sellerInfo) {
            this.sellerInfo = sellerInfo;
            return this;
        }

        public User build() {
            return new User(this.imageUrl, this.email, this.password, this.name, this.nickname, this.phoneNumber, this.address, this.userStatus, this.role, this.sellerInfo);
        }

        public String toString() {
            return "User.UserBuilder(imageUrl=" + this.imageUrl + ", email=" + this.email + ", password=" + this.password + ", name=" + this.name + ", nickname=" + this.nickname + ", phoneNumber=" + this.phoneNumber + ", address=" + this.address + ", userStatus=" + this.userStatus + ", role=" + this.role + ", sellerInfo=" + this.sellerInfo + ")";
        }
    }

    public static UserBuilder builder() {
        return new UserBuilder();
    }
}
