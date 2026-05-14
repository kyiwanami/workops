package com.example.workops.asset.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.web.server.ResponseStatusException;

import com.example.workops.asset.form.AssetForm;
import com.example.workops.asset.form.AssetStatusForm;
import com.example.workops.asset.mapper.AssetMapper;
import com.example.workops.asset.model.AssetDetail;
import com.example.workops.common.security.CurrentUserProvider;
import com.example.workops.common.security.LoginUserContext;
import com.example.workops.common.security.PermissionSetContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringJUnitConfig(AssetCommandServiceTests.AssetCommandServiceTestConfig.class)
class AssetCommandServiceTests {

    private static final Long ASSET_ID = 100L;
    private static final Long COMPANY_ID = 1L;
    private static final Long EDITOR_USER_ID = 2L;
    private static final Long MANAGER_USER_ID = 3L;
    private static final Long ASSET_CATEGORY_VALUE_ID = 1L;
    private static final Long DEPARTMENT_ID = 2L;

    @Autowired
    private AssetCommandService assetCommandService;

    @Autowired
    private AssetMapper assetMapper;

    @BeforeEach
    void setUp() {
        reset(assetMapper);
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void editorCanCreateAsset() {
        signIn(EDITOR_USER_ID, COMPANY_ID, permission("TENANT_EDITOR", "編集者"));
        when(assetMapper.existsAssetCategoryByIdAndCompanyId(ASSET_CATEGORY_VALUE_ID, COMPANY_ID)).thenReturn(true);
        when(assetMapper.existsDepartmentByIdAndCompanyId(DEPARTMENT_ID, COMPANY_ID)).thenReturn(true);
        when(assetMapper.existsStatusCode("AVAILABLE")).thenReturn(true);
        when(assetMapper.existsAssetCodeByCompanyId("KTHM-TEST-001", COMPANY_ID)).thenReturn(false);
        when(assetMapper.findLastInsertId()).thenReturn(200L);

        Long createdId = assetCommandService.createAsset(assetForm());

        assertThat(createdId).isEqualTo(200L);
        verify(assetMapper).insertAsset(
                COMPANY_ID,
                ASSET_CATEGORY_VALUE_ID,
                DEPARTMENT_ID,
                "KTHM-TEST-001",
                "テスト資産",
                "AVAILABLE",
                "備考",
                EDITOR_USER_ID,
                EDITOR_USER_ID);
    }

    @Test
    void editorCanUpdateAsset() {
        signIn(EDITOR_USER_ID, COMPANY_ID, permission("TENANT_EDITOR", "編集者"));
        when(assetMapper.findDetailByIdAndCompanyId(ASSET_ID, COMPANY_ID))
                .thenReturn(Optional.of(assetDetail()));
        when(assetMapper.existsAssetCategoryByIdAndCompanyId(ASSET_CATEGORY_VALUE_ID, COMPANY_ID)).thenReturn(true);
        when(assetMapper.existsDepartmentByIdAndCompanyId(DEPARTMENT_ID, COMPANY_ID)).thenReturn(true);
        when(assetMapper.existsStatusCode("AVAILABLE")).thenReturn(true);
        when(assetMapper.existsOtherAssetCodeByCompanyId(ASSET_ID, "KTHM-TEST-001", COMPANY_ID)).thenReturn(false);

        assetCommandService.updateAsset(ASSET_ID, assetForm());

        verify(assetMapper).updateAssetByIdAndCompanyId(
                ASSET_ID,
                COMPANY_ID,
                ASSET_CATEGORY_VALUE_ID,
                DEPARTMENT_ID,
                "KTHM-TEST-001",
                "テスト資産",
                "AVAILABLE",
                "備考",
                EDITOR_USER_ID);
    }

    @Test
    void editorCanUpdateStatus() {
        signIn(EDITOR_USER_ID, COMPANY_ID, permission("TENANT_EDITOR", "編集者"));
        when(assetMapper.findDetailByIdAndCompanyId(ASSET_ID, COMPANY_ID))
                .thenReturn(Optional.of(assetDetail()));
        when(assetMapper.existsStatusCode("REPAIRING")).thenReturn(true);

        assetCommandService.updateStatus(ASSET_ID, new AssetStatusForm("REPAIRING"));

        verify(assetMapper).updateAssetStatusByIdAndCompanyId(
                ASSET_ID,
                COMPANY_ID,
                "REPAIRING",
                EDITOR_USER_ID);
    }

    @Test
    void managerCanDeleteAsset() {
        signIn(MANAGER_USER_ID, COMPANY_ID, permission("TENANT_MANAGER", "管理者"));
        when(assetMapper.findDetailByIdAndCompanyId(ASSET_ID, COMPANY_ID))
                .thenReturn(Optional.of(assetDetail()));

        assetCommandService.deleteAsset(ASSET_ID);

        verify(assetMapper).logicalDeleteAssetByIdAndCompanyId(ASSET_ID, COMPANY_ID, MANAGER_USER_ID);
    }

    @Test
    void viewerCannotExecuteAssetMutations() {
        signIn(EDITOR_USER_ID, COMPANY_ID, permission("TENANT_VIEWER", "閲覧者"));

        assertThatThrownBy(() -> assetCommandService.createAsset(assetForm()))
                .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> assetCommandService.updateAsset(ASSET_ID, assetForm()))
                .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> assetCommandService.updateStatus(ASSET_ID, new AssetStatusForm("AVAILABLE")))
                .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> assetCommandService.deleteAsset(ASSET_ID))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void editorCannotDeleteAsset() {
        signIn(EDITOR_USER_ID, COMPANY_ID, permission("TENANT_EDITOR", "編集者"));

        assertThatThrownBy(() -> assetCommandService.deleteAsset(ASSET_ID))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void otherCompanyOrDeletedAssetReturnsNotFoundForUpdateStatusAndDelete() {
        signIn(MANAGER_USER_ID, COMPANY_ID, permission("TENANT_MANAGER", "管理者"));
        when(assetMapper.findDetailByIdAndCompanyId(ASSET_ID, COMPANY_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> assetCommandService.updateAsset(ASSET_ID, assetForm()))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        exception -> assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
        assertThatThrownBy(() -> assetCommandService.updateStatus(ASSET_ID, new AssetStatusForm("AVAILABLE")))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        exception -> assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
        assertThatThrownBy(() -> assetCommandService.deleteAsset(ASSET_ID))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        exception -> assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
    }

    @Test
    void invalidAssetCategoryIsRejected() {
        signIn(EDITOR_USER_ID, COMPANY_ID, permission("TENANT_EDITOR", "編集者"));
        when(assetMapper.existsAssetCategoryByIdAndCompanyId(ASSET_CATEGORY_VALUE_ID, COMPANY_ID)).thenReturn(false);

        assertThatThrownBy(() -> assetCommandService.createAsset(assetForm()))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        exception -> assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST));
    }

    @Test
    void invalidDepartmentIsRejected() {
        signIn(EDITOR_USER_ID, COMPANY_ID, permission("TENANT_EDITOR", "編集者"));
        when(assetMapper.existsAssetCategoryByIdAndCompanyId(ASSET_CATEGORY_VALUE_ID, COMPANY_ID)).thenReturn(true);
        when(assetMapper.existsDepartmentByIdAndCompanyId(DEPARTMENT_ID, COMPANY_ID)).thenReturn(false);

        assertThatThrownBy(() -> assetCommandService.createAsset(assetForm()))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        exception -> assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST));
    }

    @Test
    void invalidStatusIsRejected() {
        signIn(EDITOR_USER_ID, COMPANY_ID, permission("TENANT_EDITOR", "編集者"));
        when(assetMapper.existsAssetCategoryByIdAndCompanyId(ASSET_CATEGORY_VALUE_ID, COMPANY_ID)).thenReturn(true);
        when(assetMapper.existsDepartmentByIdAndCompanyId(DEPARTMENT_ID, COMPANY_ID)).thenReturn(true);
        when(assetMapper.existsStatusCode("AVAILABLE")).thenReturn(false);

        assertThatThrownBy(() -> assetCommandService.createAsset(assetForm()))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        exception -> assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST));
    }

    @Test
    void duplicateCodeIsRejected() {
        signIn(EDITOR_USER_ID, COMPANY_ID, permission("TENANT_EDITOR", "編集者"));
        when(assetMapper.existsAssetCategoryByIdAndCompanyId(ASSET_CATEGORY_VALUE_ID, COMPANY_ID)).thenReturn(true);
        when(assetMapper.existsDepartmentByIdAndCompanyId(DEPARTMENT_ID, COMPANY_ID)).thenReturn(true);
        when(assetMapper.existsStatusCode("AVAILABLE")).thenReturn(true);
        when(assetMapper.existsAssetCodeByCompanyId("KTHM-TEST-001", COMPANY_ID)).thenReturn(true);

        assertThatThrownBy(() -> assetCommandService.createAsset(assetForm()))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        exception -> assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST));
    }

    private void signIn(Long userId, Long companyId, PermissionSetContext permissionSet) {
        LoginUserContext loginUserContext = new LoginUserContext(
                userId,
                "test-user",
                "test-user@example.local",
                "TENANT",
                companyId,
                List.of(permissionSet));
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                loginUserContext,
                null,
                List.of(new SimpleGrantedAuthority(permissionSet.code()))));
    }

    private PermissionSetContext permission(String code, String name) {
        return new PermissionSetContext(code, name);
    }

    private AssetForm assetForm() {
        return new AssetForm(
                "KTHM-TEST-001",
                "テスト資産",
                ASSET_CATEGORY_VALUE_ID,
                DEPARTMENT_ID,
                "AVAILABLE",
                "備考");
    }

    private AssetDetail assetDetail() {
        return new AssetDetail(
                ASSET_ID,
                ASSET_CATEGORY_VALUE_ID,
                DEPARTMENT_ID,
                "KTHM-TEST-001",
                "テスト資産",
                "NOTE_PC",
                "ノートPC",
                false,
                "情報システム部",
                "AVAILABLE",
                "利用可能",
                "備考",
                LocalDateTime.of(2026, 5, 1, 9, 0),
                LocalDateTime.of(2026, 5, 1, 9, 0));
    }

    @Configuration
    @EnableMethodSecurity
    static class AssetCommandServiceTestConfig {

        @Bean
        CurrentUserProvider currentUserProvider() {
            return new CurrentUserProvider();
        }

        @Bean
        AssetMapper assetMapper() {
            return mock(AssetMapper.class);
        }

        @Bean
        AssetCommandService assetCommandService(
                CurrentUserProvider currentUserProvider,
                AssetMapper assetMapper) {
            return new AssetCommandService(currentUserProvider, assetMapper);
        }
    }
}
