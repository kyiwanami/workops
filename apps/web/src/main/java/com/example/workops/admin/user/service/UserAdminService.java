package com.example.workops.admin.user.service;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.example.workops.admin.user.mapper.UserAdminMapper;
import com.example.workops.admin.user.model.UserDetail;
import com.example.workops.admin.user.model.UserListItem;
import com.example.workops.common.security.CurrentUserProvider;
import com.example.workops.common.security.LoginUserContext;

/**
 * PLATFORM_ADMINとTENANT_MANAGER向けユーザー参照ユースケースを扱うService。
 */
@Service
public class UserAdminService {

    private final CurrentUserProvider currentUserProvider;
    private final UserAdminMapper userAdminMapper;

    public UserAdminService(
            CurrentUserProvider currentUserProvider,
            UserAdminMapper userAdminMapper) {
        this.currentUserProvider = currentUserProvider;
        this.userAdminMapper = userAdminMapper;
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('PLATFORM_ADMIN')")
    public List<UserListItem> findPlatformUsers() {
        return userAdminMapper.findPlatformUsers();
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('PLATFORM_ADMIN')")
    public UserDetail findPlatformUserDetail(Long userId) {
        return userAdminMapper.findPlatformUserById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "ユーザーが見つかりません。"));
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('TENANT_MANAGER')")
    public List<UserListItem> findTenantUsers() {
        LoginUserContext currentUser = currentUserProvider.requireCurrentUser();
        return userAdminMapper.findTenantUsersByCompanyId(currentUser.companyId());
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('TENANT_MANAGER')")
    public UserDetail findTenantUserDetail(Long userId) {
        LoginUserContext currentUser = currentUserProvider.requireCurrentUser();
        return userAdminMapper.findTenantUserByIdAndCompanyId(userId, currentUser.companyId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "ユーザーが見つかりません。"));
    }
}
