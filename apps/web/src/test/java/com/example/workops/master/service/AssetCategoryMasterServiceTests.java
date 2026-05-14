package com.example.workops.master.service;

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

import com.example.workops.common.security.CurrentUserProvider;
import com.example.workops.common.security.LoginUserContext;
import com.example.workops.common.security.PermissionSetContext;
import com.example.workops.master.form.AssetCategoryMasterForm;
import com.example.workops.master.form.AssetCategoryMasterSearchForm;
import com.example.workops.master.mapper.AssetCategoryMasterMapper;
import com.example.workops.master.model.AssetCategoryMasterDetail;
import com.example.workops.master.model.AssetCategoryMasterListItem;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@SpringJUnitConfig(AssetCategoryMasterServiceTests.AssetCategoryMasterServiceTestConfig.class)
class AssetCategoryMasterServiceTests {

    private static final Long COMPANY_ID = 1L;
    private static final Long MANAGER_USER_ID = 3L;
    private static final Long MASTER_VALUE_ID = 10L;
    private static final Long GENERIC_MASTER_ID = 20L;

    @Autowired
    private AssetCategoryMasterService assetCategoryMasterService;

    @Autowired
    private AssetCategoryMasterMapper assetCategoryMasterMapper;

    @BeforeEach
    void setUp() {
        reset(assetCategoryMasterMapper);
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void managerCanFindList() {
        signIn(MANAGER_USER_ID, COMPANY_ID, permission("TENANT_MANAGER", "管理者"));
        AssetCategoryMasterSearchForm searchForm = new AssetCategoryMasterSearchForm(true);
        AssetCategoryMasterListItem item = assetCategoryMasterListItem(false);
        when(assetCategoryMasterMapper.findListByCompanyIdAndSearchForm(COMPANY_ID, searchForm))
                .thenReturn(List.of(item));

        List<AssetCategoryMasterListItem> result = assetCategoryMasterService.findList(searchForm);

        assertThat(result).containsExactly(item);
        verify(assetCategoryMasterMapper).findListByCompanyIdAndSearchForm(COMPANY_ID, searchForm);
    }

    @Test
    void managerCanCreateAssetCategory() {
        signIn(MANAGER_USER_ID, COMPANY_ID, permission("TENANT_MANAGER", "管理者"));
        when(assetCategoryMasterMapper.existsCodeByCompanyId(COMPANY_ID, "LAPTOP")).thenReturn(false);
        when(assetCategoryMasterMapper.findAssetCategoryMasterId()).thenReturn(Optional.of(GENERIC_MASTER_ID));

        assetCategoryMasterService.create(assetCategoryMasterForm());

        verify(assetCategoryMasterMapper).insertAssetCategory(
                GENERIC_MASTER_ID,
                COMPANY_ID,
                "LAPTOP",
                "ノートPC",
                10,
                MANAGER_USER_ID,
                MANAGER_USER_ID);
    }

    @Test
    void managerCanUpdateActiveAssetCategory() {
        signIn(MANAGER_USER_ID, COMPANY_ID, permission("TENANT_MANAGER", "管理者"));
        when(assetCategoryMasterMapper.findActiveByIdAndCompanyId(MASTER_VALUE_ID, COMPANY_ID))
                .thenReturn(Optional.of(assetCategoryMasterDetail(false)));

        assetCategoryMasterService.update(MASTER_VALUE_ID, assetCategoryMasterForm());

        verify(assetCategoryMasterMapper).updateActiveByIdAndCompanyId(
                MASTER_VALUE_ID,
                COMPANY_ID,
                "ノートPC",
                10,
                MANAGER_USER_ID);
    }

    @Test
    void managerCanDeleteActiveAssetCategory() {
        signIn(MANAGER_USER_ID, COMPANY_ID, permission("TENANT_MANAGER", "管理者"));
        when(assetCategoryMasterMapper.findActiveByIdAndCompanyId(MASTER_VALUE_ID, COMPANY_ID))
                .thenReturn(Optional.of(assetCategoryMasterDetail(false)));

        assetCategoryMasterService.delete(MASTER_VALUE_ID);

        verify(assetCategoryMasterMapper).logicalDeleteActiveByIdAndCompanyId(
                MASTER_VALUE_ID,
                COMPANY_ID,
                MANAGER_USER_ID);
    }

    @Test
    void managerCanRestoreDeletedAssetCategory() {
        signIn(MANAGER_USER_ID, COMPANY_ID, permission("TENANT_MANAGER", "管理者"));
        when(assetCategoryMasterMapper.findDeletedByIdAndCompanyId(MASTER_VALUE_ID, COMPANY_ID))
                .thenReturn(Optional.of(assetCategoryMasterDetail(true)));

        assetCategoryMasterService.restore(MASTER_VALUE_ID);

        verify(assetCategoryMasterMapper).restoreDeletedByIdAndCompanyId(
                MASTER_VALUE_ID,
                COMPANY_ID,
                MANAGER_USER_ID);
    }

    @Test
    void duplicateCodeIsRejectedForCreate() {
        signIn(MANAGER_USER_ID, COMPANY_ID, permission("TENANT_MANAGER", "管理者"));
        when(assetCategoryMasterMapper.existsCodeByCompanyId(COMPANY_ID, "LAPTOP")).thenReturn(true);

        assertThatThrownBy(() -> assetCategoryMasterService.create(assetCategoryMasterForm()))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        exception -> assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST));
        verify(assetCategoryMasterMapper, never()).insertAssetCategory(
                GENERIC_MASTER_ID,
                COMPANY_ID,
                "LAPTOP",
                "ノートPC",
                10,
                MANAGER_USER_ID,
                MANAGER_USER_ID);
    }

    @Test
    void missingAssetCategoryMasterReturnsInternalServerError() {
        signIn(MANAGER_USER_ID, COMPANY_ID, permission("TENANT_MANAGER", "管理者"));
        when(assetCategoryMasterMapper.existsCodeByCompanyId(COMPANY_ID, "LAPTOP")).thenReturn(false);
        when(assetCategoryMasterMapper.findAssetCategoryMasterId()).thenReturn(Optional.empty());

        assertThatThrownBy(() -> assetCategoryMasterService.create(assetCategoryMasterForm()))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        exception -> assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR));
        verify(assetCategoryMasterMapper, never()).insertAssetCategory(
                GENERIC_MASTER_ID,
                COMPANY_ID,
                "LAPTOP",
                "ノートPC",
                10,
                MANAGER_USER_ID,
                MANAGER_USER_ID);
    }

    @Test
    void inactiveOrOtherCompanyAssetCategoryCannotBeUpdatedOrDeleted() {
        signIn(MANAGER_USER_ID, COMPANY_ID, permission("TENANT_MANAGER", "管理者"));
        when(assetCategoryMasterMapper.findActiveByIdAndCompanyId(MASTER_VALUE_ID, COMPANY_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> assetCategoryMasterService.update(MASTER_VALUE_ID, assetCategoryMasterForm()))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        exception -> assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
        assertThatThrownBy(() -> assetCategoryMasterService.delete(MASTER_VALUE_ID))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        exception -> assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
        verify(assetCategoryMasterMapper, never()).updateActiveByIdAndCompanyId(
                MASTER_VALUE_ID,
                COMPANY_ID,
                "ノートPC",
                10,
                MANAGER_USER_ID);
        verify(assetCategoryMasterMapper, never()).logicalDeleteActiveByIdAndCompanyId(
                MASTER_VALUE_ID,
                COMPANY_ID,
                MANAGER_USER_ID);
    }

    @Test
    void activeOrOtherCompanyAssetCategoryCannotBeRestored() {
        signIn(MANAGER_USER_ID, COMPANY_ID, permission("TENANT_MANAGER", "管理者"));
        when(assetCategoryMasterMapper.findDeletedByIdAndCompanyId(MASTER_VALUE_ID, COMPANY_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> assetCategoryMasterService.restore(MASTER_VALUE_ID))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        exception -> assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
        verify(assetCategoryMasterMapper, never()).restoreDeletedByIdAndCompanyId(
                MASTER_VALUE_ID,
                COMPANY_ID,
                MANAGER_USER_ID);
    }

    @Test
    void viewerCannotExecuteAssetCategoryMasterOperations() {
        signIn(4L, COMPANY_ID, permission("TENANT_VIEWER", "閲覧者"));

        assertThatThrownBy(() -> assetCategoryMasterService.findList(new AssetCategoryMasterSearchForm(false)))
                .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> assetCategoryMasterService.create(assetCategoryMasterForm()))
                .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> assetCategoryMasterService.update(MASTER_VALUE_ID, assetCategoryMasterForm()))
                .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> assetCategoryMasterService.delete(MASTER_VALUE_ID))
                .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> assetCategoryMasterService.restore(MASTER_VALUE_ID))
                .isInstanceOf(AccessDeniedException.class);
        verifyNoInteractions(assetCategoryMasterMapper);
    }

    @Test
    void editorCannotExecuteAssetCategoryMasterOperations() {
        signIn(5L, COMPANY_ID, permission("TENANT_EDITOR", "編集者"));

        assertThatThrownBy(() -> assetCategoryMasterService.findList(new AssetCategoryMasterSearchForm(false)))
                .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> assetCategoryMasterService.create(assetCategoryMasterForm()))
                .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> assetCategoryMasterService.update(MASTER_VALUE_ID, assetCategoryMasterForm()))
                .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> assetCategoryMasterService.delete(MASTER_VALUE_ID))
                .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> assetCategoryMasterService.restore(MASTER_VALUE_ID))
                .isInstanceOf(AccessDeniedException.class);
        verifyNoInteractions(assetCategoryMasterMapper);
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

    private AssetCategoryMasterForm assetCategoryMasterForm() {
        return new AssetCategoryMasterForm("LAPTOP", "ノートPC", 10);
    }

    private AssetCategoryMasterDetail assetCategoryMasterDetail(Boolean isDeleted) {
        return new AssetCategoryMasterDetail(
                MASTER_VALUE_ID,
                "LAPTOP",
                "ノートPC",
                10,
                isDeleted,
                LocalDateTime.of(2026, 5, 1, 9, 0),
                LocalDateTime.of(2026, 5, 1, 9, 0));
    }

    private AssetCategoryMasterListItem assetCategoryMasterListItem(Boolean isDeleted) {
        return new AssetCategoryMasterListItem(
                MASTER_VALUE_ID,
                "LAPTOP",
                "ノートPC",
                10,
                isDeleted,
                LocalDateTime.of(2026, 5, 1, 9, 0),
                LocalDateTime.of(2026, 5, 1, 9, 0));
    }

    @Configuration
    @EnableMethodSecurity
    static class AssetCategoryMasterServiceTestConfig {

        @Bean
        CurrentUserProvider currentUserProvider() {
            return new CurrentUserProvider();
        }

        @Bean
        AssetCategoryMasterMapper assetCategoryMasterMapper() {
            return mock(AssetCategoryMasterMapper.class);
        }

        @Bean
        AssetCategoryMasterService assetCategoryMasterService(
                CurrentUserProvider currentUserProvider,
                AssetCategoryMasterMapper assetCategoryMasterMapper) {
            return new AssetCategoryMasterService(currentUserProvider, assetCategoryMasterMapper);
        }
    }
}
