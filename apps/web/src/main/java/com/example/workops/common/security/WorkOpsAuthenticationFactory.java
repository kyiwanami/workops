package com.example.workops.common.security;

import java.util.List;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

/**
 * DB由来のLoginUserContextをSpring SecurityのAuthenticationへ変換する。
 */
@Component
public class WorkOpsAuthenticationFactory {

    public Authentication create(LoginUserContext loginUserContext) {
        List<SimpleGrantedAuthority> authorities = loginUserContext.permissionSets()
                .stream()
                .map(permissionSet -> new SimpleGrantedAuthority(permissionSet.code()))
                .toList();

        return new UsernamePasswordAuthenticationToken(loginUserContext, null, authorities);
    }
}
