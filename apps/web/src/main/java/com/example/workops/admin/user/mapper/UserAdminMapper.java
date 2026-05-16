package com.example.workops.admin.user.mapper;

import java.util.List;
import java.util.Optional;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.example.workops.admin.user.model.UserDetail;
import com.example.workops.admin.user.model.UserListItem;

/**
 * ユーザー一覧・詳細表示用のSQLを実行するMapper。
 */
@Mapper
public interface UserAdminMapper {

    List<UserListItem> findPlatformUsers();

    Optional<UserDetail> findPlatformUserById(@Param("userId") Long userId);

    List<UserListItem> findTenantUsersByCompanyId(@Param("companyId") Long companyId);

    Optional<UserDetail> findTenantUserByIdAndCompanyId(
            @Param("userId") Long userId,
            @Param("companyId") Long companyId);
}
