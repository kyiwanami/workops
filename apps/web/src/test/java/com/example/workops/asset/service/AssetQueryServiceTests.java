package com.example.workops.asset.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.server.ResponseStatusException;

import com.example.workops.asset.form.AssetSearchForm;
import com.example.workops.asset.mapper.AssetMapper;
import com.example.workops.asset.model.AssetCategoryOption;
import com.example.workops.asset.model.AssetDepartmentOption;
import com.example.workops.asset.model.AssetDetail;
import com.example.workops.asset.model.AssetListItem;
import com.example.workops.asset.model.AssetStatusOption;
import com.example.workops.common.security.CurrentUserProvider;
import com.example.workops.common.security.LoginUserContext;
import com.example.workops.common.security.PermissionSetContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AssetQueryServiceTests {

    private static final Long ASSET_ID = 100L;
    private static final Long COMPANY_ID = 1L;
    private static final Long USER_ID = 2L;

    private AssetQueryService assetQueryService;
    private AssetMapper assetMapper;

    @BeforeEach
    void setUp() {
        assetMapper = mock(AssetMapper.class);
        assetQueryService = new AssetQueryService(new CurrentUserProvider(), assetMapper);
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void findListPassesCompanyIdAndSearchFormToMapper() {
        signIn(USER_ID, COMPANY_ID, permission("TENANT_VIEWER", "閲覧者"));
        AssetSearchForm assetSearchForm = new AssetSearchForm("KTHM", "ノートPC", 999L, 888L, "INVALID");
        when(assetMapper.findListByCompanyIdAndSearchForm(COMPANY_ID, assetSearchForm))
                .thenReturn(List.of(assetListItem()));

        List<AssetListItem> assets = assetQueryService.findList(assetSearchForm);

        assertThat(assets).hasSize(1);
        verify(assetMapper).findListByCompanyIdAndSearchForm(COMPANY_ID, assetSearchForm);
    }

    @Test
    void findDetailReturnsDetailOrNotFound() {
        signIn(USER_ID, COMPANY_ID, permission("TENANT_VIEWER", "閲覧者"));
        when(assetMapper.findDetailByIdAndCompanyId(ASSET_ID, COMPANY_ID))
                .thenReturn(Optional.of(assetDetail()));
        when(assetMapper.findDetailByIdAndCompanyId(999L, COMPANY_ID))
                .thenReturn(Optional.empty());

        AssetDetail assetDetail = assetQueryService.findDetail(ASSET_ID);

        assertThat(assetDetail.id()).isEqualTo(ASSET_ID);
        assertThatThrownBy(() -> assetQueryService.findDetail(999L))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        exception -> assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
    }

    @Test
    void optionQueriesUseCurrentCompanyId() {
        signIn(USER_ID, COMPANY_ID, permission("TENANT_EDITOR", "編集者"));
        when(assetMapper.findAssetCategoryOptionsByCompanyId(COMPANY_ID))
                .thenReturn(List.of(new AssetCategoryOption(1L, "NOTE_PC", "ノートPC")));
        when(assetMapper.findDepartmentOptionsByCompanyId(COMPANY_ID))
                .thenReturn(List.of(new AssetDepartmentOption(2L, "SYS", "情報システム部")));
        when(assetMapper.findStatusOptions())
                .thenReturn(List.of(new AssetStatusOption("AVAILABLE", "利用可能")));

        assertThat(assetQueryService.findAssetCategoryOptions()).hasSize(1);
        assertThat(assetQueryService.findDepartmentOptions()).hasSize(1);
        assertThat(assetQueryService.findStatusOptions()).hasSize(1);
        verify(assetMapper).findAssetCategoryOptionsByCompanyId(COMPANY_ID);
        verify(assetMapper).findDepartmentOptionsByCompanyId(COMPANY_ID);
        verify(assetMapper).findStatusOptions();
    }

    @Test
    void uiStateReflectsAssetPermissions() {
        signIn(USER_ID, COMPANY_ID, permission("TENANT_VIEWER", "閲覧者"));
        assertThat(assetQueryService.canCreateAsset()).isFalse();
        assertThat(assetQueryService.canEditAsset(assetDetail())).isFalse();
        assertThat(assetQueryService.canChangeStatus(assetDetail())).isFalse();
        assertThat(assetQueryService.canDeleteAsset(assetDetail())).isFalse();

        signIn(USER_ID, COMPANY_ID, permission("TENANT_EDITOR", "編集者"));
        assertThat(assetQueryService.canCreateAsset()).isTrue();
        assertThat(assetQueryService.canEditAsset(assetDetail())).isTrue();
        assertThat(assetQueryService.canChangeStatus(assetDetail())).isTrue();
        assertThat(assetQueryService.canDeleteAsset(assetDetail())).isFalse();

        signIn(USER_ID, COMPANY_ID, permission("TENANT_MANAGER", "管理者"));
        assertThat(assetQueryService.canCreateAsset()).isTrue();
        assertThat(assetQueryService.canEditAsset(assetDetail())).isTrue();
        assertThat(assetQueryService.canChangeStatus(assetDetail())).isTrue();
        assertThat(assetQueryService.canDeleteAsset(assetDetail())).isTrue();
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

    private AssetListItem assetListItem() {
        return new AssetListItem(
                ASSET_ID,
                "KTHM-TEST-001",
                "テスト資産",
                "NOTE_PC",
                "ノートPC",
                "情報システム部",
                "AVAILABLE",
                "利用可能",
                "備考",
                LocalDateTime.of(2026, 5, 1, 9, 0),
                LocalDateTime.of(2026, 5, 1, 9, 0));
    }

    private AssetDetail assetDetail() {
        return new AssetDetail(
                ASSET_ID,
                1L,
                2L,
                "KTHM-TEST-001",
                "テスト資産",
                "NOTE_PC",
                "ノートPC",
                "情報システム部",
                "AVAILABLE",
                "利用可能",
                "備考",
                LocalDateTime.of(2026, 5, 1, 9, 0),
                LocalDateTime.of(2026, 5, 1, 9, 0));
    }
}
