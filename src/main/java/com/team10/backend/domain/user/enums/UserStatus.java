package com.team10.backend.domain.user.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum UserStatus {
    ACTIVE("활성", "정상적으로 서비스를 이용 중인 상태"),
    BANNED("이용정지", "부적절한 행위로 이용 정지된 상태"),
    DELETED("탈퇴", "사용자가 자진 탈퇴하여 계정이 비활성화된 상태")
    ;

    private final String title;
    private final String description;
}
