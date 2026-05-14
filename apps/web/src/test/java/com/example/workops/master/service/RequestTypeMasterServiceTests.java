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
import com.example.workops.master.form.RequestTypeMasterForm;
import com.example.workops.master.form.RequestTypeMasterSearchForm;
import com.example.workops.master.mapper.RequestTypeMasterMapper;
import com.example.workops.master.model.RequestTypeMasterDetail;
import com.example.workops.master.model.RequestTypeMasterListItem;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@SpringJUnitConfig(RequestTypeMasterServiceTests.RequestTypeMasterServiceTestConfig.class)
class RequestTypeMasterServiceTests {

    private static final Long COMPANY_ID = 1L;
    private static final Long MANAGER_USER_ID = 3L;
    private static final Long MASTER_VALUE_ID = 10L;
    private static final Long GENERIC_MASTER_ID = 20L;

    @Autowired
    private RequestTypeMasterService requestTypeMasterService;

    @Autowired
    private RequestTypeMasterMapper requestTypeMasterMapper;

    @BeforeEach
    void setUp() {
        reset(requestTypeMasterMapper);
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void managerCanFindList() {
        signIn(MANAGER_USER_ID, COMPANY_ID, permission("TENANT_MANAGER", "管理者"));
        RequestTypeMasterSearchForm searchForm = new RequestTypeMasterSearchForm(true);
        RequestTypeMasterListItem item = requestTypeMasterListItem(false);
        when(requestTypeMasterMapper.findListByCompanyIdAndSearchForm(COMPANY_ID, searchForm))
                .thenReturn(List.of(item));

        List<RequestTypeMasterListItem> result = requestTypeMasterService.findList(searchForm);

        assertThat(result).containsExactly(item);
        verify(requestTypeMasterMapper).findListByCompanyIdAndSearchForm(COMPANY_ID, searchForm);
    }

    @Test
    void managerCanCreateRequestType() {
        signIn(MANAGER_USER_ID, COMPANY_ID, permission("TENANT_MANAGER", "管理者"));
        when(requestTypeMasterMapper.existsCodeByCompanyId(COMPANY_ID, "PURCHASE")).thenReturn(false);
        when(requestTypeMasterMapper.findRequestTypeMasterId()).thenReturn(Optional.of(GENERIC_MASTER_ID));

        requestTypeMasterService.create(requestTypeMasterForm());

        verify(requestTypeMasterMapper).insertRequestType(
                GENERIC_MASTER_ID,
                COMPANY_ID,
                "PURCHASE",
                "購入申請",
                10,
                MANAGER_USER_ID,
                MANAGER_USER_ID);
    }

    @Test
    void managerCanUpdateActiveRequestType() {
        signIn(MANAGER_USER_ID, COMPANY_ID, permission("TENANT_MANAGER", "管理者"));
        when(requestTypeMasterMapper.findActiveByIdAndCompanyId(MASTER_VALUE_ID, COMPANY_ID))
                .thenReturn(Optional.of(requestTypeMasterDetail(false)));

        requestTypeMasterService.update(MASTER_VALUE_ID, requestTypeMasterForm());

        verify(requestTypeMasterMapper).updateActiveByIdAndCompanyId(
                MASTER_VALUE_ID,
                COMPANY_ID,
                "購入申請",
                10,
                MANAGER_USER_ID);
    }

    @Test
    void managerCanDeleteActiveRequestType() {
        signIn(MANAGER_USER_ID, COMPANY_ID, permission("TENANT_MANAGER", "管理者"));
        when(requestTypeMasterMapper.findActiveByIdAndCompanyId(MASTER_VALUE_ID, COMPANY_ID))
                .thenReturn(Optional.of(requestTypeMasterDetail(false)));

        requestTypeMasterService.delete(MASTER_VALUE_ID);

        verify(requestTypeMasterMapper).logicalDeleteActiveByIdAndCompanyId(
                MASTER_VALUE_ID,
                COMPANY_ID,
                MANAGER_USER_ID);
    }

    @Test
    void managerCanRestoreDeletedRequestType() {
        signIn(MANAGER_USER_ID, COMPANY_ID, permission("TENANT_MANAGER", "管理者"));
        when(requestTypeMasterMapper.findDeletedByIdAndCompanyId(MASTER_VALUE_ID, COMPANY_ID))
                .thenReturn(Optional.of(requestTypeMasterDetail(true)));

        requestTypeMasterService.restore(MASTER_VALUE_ID);

        verify(requestTypeMasterMapper).restoreDeletedByIdAndCompanyId(
                MASTER_VALUE_ID,
                COMPANY_ID,
                MANAGER_USER_ID);
    }

    @Test
    void duplicateCodeIsRejectedForCreate() {
        signIn(MANAGER_USER_ID, COMPANY_ID, permission("TENANT_MANAGER", "管理者"));
        when(requestTypeMasterMapper.existsCodeByCompanyId(COMPANY_ID, "PURCHASE")).thenReturn(true);

        assertThatThrownBy(() -> requestTypeMasterService.create(requestTypeMasterForm()))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        exception -> assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST));
        verify(requestTypeMasterMapper, never()).insertRequestType(
                GENERIC_MASTER_ID,
                COMPANY_ID,
                "PURCHASE",
                "購入申請",
                10,
                MANAGER_USER_ID,
                MANAGER_USER_ID);
    }

    @Test
    void missingRequestTypeMasterReturnsInternalServerError() {
        signIn(MANAGER_USER_ID, COMPANY_ID, permission("TENANT_MANAGER", "管理者"));
        when(requestTypeMasterMapper.existsCodeByCompanyId(COMPANY_ID, "PURCHASE")).thenReturn(false);
        when(requestTypeMasterMapper.findRequestTypeMasterId()).thenReturn(Optional.empty());

        assertThatThrownBy(() -> requestTypeMasterService.create(requestTypeMasterForm()))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        exception -> assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR));
        verify(requestTypeMasterMapper, never()).insertRequestType(
                GENERIC_MASTER_ID,
                COMPANY_ID,
                "PURCHASE",
                "購入申請",
                10,
                MANAGER_USER_ID,
                MANAGER_USER_ID);
    }

    @Test
    void inactiveOrOtherCompanyRequestTypeCannotBeUpdatedOrDeleted() {
        signIn(MANAGER_USER_ID, COMPANY_ID, permission("TENANT_MANAGER", "管理者"));
        when(requestTypeMasterMapper.findActiveByIdAndCompanyId(MASTER_VALUE_ID, COMPANY_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> requestTypeMasterService.update(MASTER_VALUE_ID, requestTypeMasterForm()))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        exception -> assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
        assertThatThrownBy(() -> requestTypeMasterService.delete(MASTER_VALUE_ID))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        exception -> assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
        verify(requestTypeMasterMapper, never()).updateActiveByIdAndCompanyId(
                MASTER_VALUE_ID,
                COMPANY_ID,
                "購入申請",
                10,
                MANAGER_USER_ID);
        verify(requestTypeMasterMapper, never()).logicalDeleteActiveByIdAndCompanyId(
                MASTER_VALUE_ID,
                COMPANY_ID,
                MANAGER_USER_ID);
    }

    @Test
    void activeOrOtherCompanyRequestTypeCannotBeRestored() {
        signIn(MANAGER_USER_ID, COMPANY_ID, permission("TENANT_MANAGER", "管理者"));
        when(requestTypeMasterMapper.findDeletedByIdAndCompanyId(MASTER_VALUE_ID, COMPANY_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> requestTypeMasterService.restore(MASTER_VALUE_ID))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        exception -> assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
        verify(requestTypeMasterMapper, never()).restoreDeletedByIdAndCompanyId(
                MASTER_VALUE_ID,
                COMPANY_ID,
                MANAGER_USER_ID);
    }

    @Test
    void viewerCannotExecuteRequestTypeMasterOperations() {
        signIn(4L, COMPANY_ID, permission("TENANT_VIEWER", "閲覧者"));

        assertThatThrownBy(() -> requestTypeMasterService.findList(new RequestTypeMasterSearchForm(false)))
                .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> requestTypeMasterService.create(requestTypeMasterForm()))
                .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> requestTypeMasterService.update(MASTER_VALUE_ID, requestTypeMasterForm()))
                .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> requestTypeMasterService.delete(MASTER_VALUE_ID))
                .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> requestTypeMasterService.restore(MASTER_VALUE_ID))
                .isInstanceOf(AccessDeniedException.class);
        verifyNoInteractions(requestTypeMasterMapper);
    }

    @Test
    void editorCannotExecuteRequestTypeMasterOperations() {
        signIn(5L, COMPANY_ID, permission("TENANT_EDITOR", "編集者"));

        assertThatThrownBy(() -> requestTypeMasterService.findList(new RequestTypeMasterSearchForm(false)))
                .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> requestTypeMasterService.create(requestTypeMasterForm()))
                .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> requestTypeMasterService.update(MASTER_VALUE_ID, requestTypeMasterForm()))
                .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> requestTypeMasterService.delete(MASTER_VALUE_ID))
                .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> requestTypeMasterService.restore(MASTER_VALUE_ID))
                .isInstanceOf(AccessDeniedException.class);
        verifyNoInteractions(requestTypeMasterMapper);
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

    private RequestTypeMasterForm requestTypeMasterForm() {
        return new RequestTypeMasterForm("PURCHASE", "購入申請", 10);
    }

    private RequestTypeMasterDetail requestTypeMasterDetail(Boolean isDeleted) {
        return new RequestTypeMasterDetail(
                MASTER_VALUE_ID,
                "PURCHASE",
                "購入申請",
                10,
                isDeleted,
                LocalDateTime.of(2026, 5, 1, 9, 0),
                LocalDateTime.of(2026, 5, 1, 9, 0));
    }

    private RequestTypeMasterListItem requestTypeMasterListItem(Boolean isDeleted) {
        return new RequestTypeMasterListItem(
                MASTER_VALUE_ID,
                "PURCHASE",
                "購入申請",
                10,
                isDeleted,
                LocalDateTime.of(2026, 5, 1, 9, 0),
                LocalDateTime.of(2026, 5, 1, 9, 0));
    }

    @Configuration
    @EnableMethodSecurity
    static class RequestTypeMasterServiceTestConfig {

        @Bean
        CurrentUserProvider currentUserProvider() {
            return new CurrentUserProvider();
        }

        @Bean
        RequestTypeMasterMapper requestTypeMasterMapper() {
            return mock(RequestTypeMasterMapper.class);
        }

        @Bean
        RequestTypeMasterService requestTypeMasterService(
                CurrentUserProvider currentUserProvider,
                RequestTypeMasterMapper requestTypeMasterMapper) {
            return new RequestTypeMasterService(currentUserProvider, requestTypeMasterMapper);
        }
    }
}
