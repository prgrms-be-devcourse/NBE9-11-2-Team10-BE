package com.team10.backend.domain.user.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum Role {
    BUYER("ROLE_USER", "일반 사용자"),
    SELLER("ROLE_SELLER", "판매자")
    ;

    private final String key;
    private final String title;
}
