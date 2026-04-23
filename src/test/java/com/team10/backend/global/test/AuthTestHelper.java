package com.team10.backend.global.test;

import com.team10.backend.domain.user.entity.User;
import com.team10.backend.global.security.CustomUserPrincipal;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

public class AuthTestHelper {
    public static void setAuth(User user) {
        CustomUserPrincipal principal =
                new CustomUserPrincipal(user.getId(), user.getRole());

        var auth = new UsernamePasswordAuthenticationToken(
                principal,
                null,
                List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()))
        );

        SecurityContextHolder.getContext().setAuthentication(auth);
    }
}
