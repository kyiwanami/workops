package com.example.workops.admin.user.mapper;

import java.util.List;
import java.util.Optional;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.example.workops.admin.user.model.CompanySelectOption;
import com.example.workops.admin.user.model.DepartmentSelectOption;
import com.example.workops.admin.user.model.PermissionSetOption;
import com.example.workops.admin.user.model.UserDetail;
import com.example.workops.admin.user.model.UserEditTarget;
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

    Optional<UserEditTarget> findPlatformUserEditTarget(@Param("userId") Long userId);

    Optional<UserEditTarget> findTenantUserEditTargetByIdAndCompanyId(
            @Param("userId") Long userId,
            @Param("companyId") Long companyId);

    List<String> findPermissionSetCodesByUserId(@Param("userId") Long userId);

    List<CompanySelectOption> findActiveCompanies();

    List<DepartmentSelectOption> findActiveDepartments();

    List<DepartmentSelectOption> findActiveDepartmentsByCompanyId(@Param("companyId") Long companyId);

    Optional<Long> findActiveCompanyId(@Param("companyId") Long companyId);

    Optional<Long> findActiveDepartmentIdByCompanyId(
            @Param("departmentId") Long departmentId,
            @Param("companyId") Long companyId);

    List<PermissionSetOption> findPlatformPermissionSetOptions();

    List<PermissionSetOption> findTenantPermissionSetOptions();

    boolean existsPlatformUsername(@Param("username") String username);

    boolean existsPlatformEmail(@Param("email") String email);

    boolean existsTenantUsername(
            @Param("companyId") Long companyId,
            @Param("username") String username);

    boolean existsTenantEmail(
            @Param("companyId") Long companyId,
            @Param("email") String email);

    boolean existsPlatformEmailExcludingUser(
            @Param("userId") Long userId,
            @Param("email") String email);

    boolean existsTenantEmailExcludingUser(
            @Param("companyId") Long companyId,
            @Param("userId") Long userId,
            @Param("email") String email);

    int updatePlatformUserEditableFields(
            @Param("userId") Long userId,
            @Param("name") String name,
            @Param("email") String email,
            @Param("departmentId") Long departmentId,
            @Param("updatedBy") Long updatedBy);

    int updateTenantUserEditableFields(
            @Param("userId") Long userId,
            @Param("companyId") Long companyId,
            @Param("name") String name,
            @Param("email") String email,
            @Param("departmentId") Long departmentId,
            @Param("updatedBy") Long updatedBy);

    int deleteUserPermissionSets(@Param("userId") Long userId);

    int countActiveTenantManagersByCompanyId(@Param("companyId") Long companyId);

    int insertUser(
            @Param("companyId") Long companyId,
            @Param("departmentId") Long departmentId,
            @Param("cognitoSub") String cognitoSub,
            @Param("username") String username,
            @Param("name") String name,
            @Param("email") String email,
            @Param("actorType") String actorType,
            @Param("createdBy") Long createdBy,
            @Param("updatedBy") Long updatedBy);

    int insertUserPermissionSetByCode(
            @Param("userId") Long userId,
            @Param("permissionSetCode") String permissionSetCode);

    Long findLastInsertId();
}
