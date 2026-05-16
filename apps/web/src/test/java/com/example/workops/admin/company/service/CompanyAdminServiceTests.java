package com.example.workops.admin.company.service;

import java.lang.reflect.RecordComponent;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
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

import com.example.workops.admin.company.form.CompanyForm;
import com.example.workops.admin.company.mapper.CompanyAdminMapper;
import com.example.workops.common.logging.OperationLogRecord;
import com.example.workops.common.logging.OperationLogger;
import com.example.workops.common.security.CurrentUserProvider;
import com.example.workops.common.security.LoginUserContext;
import com.example.workops.common.security.PermissionSetContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@SpringJUnitConfig(CompanyAdminServiceTests.CompanyAdminServiceTestConfig.class)
class CompanyAdminServiceTests {

    private static final Long PLATFORM_USER_ID = 7L;
    private static final Long CREATED_COMPANY_ID = 30L;
    private static final Long ASSET_CATEGORY_MASTER_ID = 1L;
    private static final Long REQUEST_TYPE_MASTER_ID = 2L;

    @Autowired
    private CompanyAdminService companyAdminService;

    @Autowired
    private CompanyAdminMapper companyAdminMapper;

    @Autowired
    private OperationLogger operationLogger;

    @BeforeEach
    void setUp() {
        reset(companyAdminMapper, operationLogger);
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void platformAdminCanCreateCompanyAndInitializeTenantMasterValues() {
        signIn(PLATFORM_USER_ID, null, "PLATFORM", permission("PLATFORM_ADMIN", "WorkOps管理者"));
        when(companyAdminMapper.existsCompanyCode("NEW_COMPANY")).thenReturn(false);
        when(companyAdminMapper.findLastInsertId()).thenReturn(CREATED_COMPANY_ID);
        when(companyAdminMapper.findActiveGenericMasterIdByCode("ASSET_CATEGORY"))
                .thenReturn(Optional.of(ASSET_CATEGORY_MASTER_ID));
        when(companyAdminMapper.findActiveGenericMasterIdByCode("REQUEST_TYPE"))
                .thenReturn(Optional.of(REQUEST_TYPE_MASTER_ID));

        Long createdCompanyId = companyAdminService.create(companyForm());

        assertThat(createdCompanyId).isEqualTo(CREATED_COMPANY_ID);
        verify(companyAdminMapper).insertCompany(
                "NEW_COMPANY",
                "新会社",
                PLATFORM_USER_ID,
                PLATFORM_USER_ID);
        verify(companyAdminMapper, times(6)).insertGenericMasterValue(
                eq(ASSET_CATEGORY_MASTER_ID),
                eq(CREATED_COMPANY_ID),
                any(),
                any(),
                any(),
                eq(PLATFORM_USER_ID),
                eq(PLATFORM_USER_ID));
        verify(companyAdminMapper, times(3)).insertGenericMasterValue(
                eq(REQUEST_TYPE_MASTER_ID),
                eq(CREATED_COMPANY_ID),
                any(),
                any(),
                any(),
                eq(PLATFORM_USER_ID),
                eq(PLATFORM_USER_ID));
        assertSuccessLog(CREATED_COMPANY_ID);
    }

    @Test
    void duplicateCompanyCodeIsRejectedBeforeCompanyInsertAndTenantInitialization() {
        signIn(PLATFORM_USER_ID, null, "PLATFORM", permission("PLATFORM_ADMIN", "WorkOps管理者"));
        when(companyAdminMapper.existsCompanyCode("NEW_COMPANY")).thenReturn(true);

        assertThatThrownBy(() -> companyAdminService.create(companyForm()))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        exception -> assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST));
        verify(companyAdminMapper, never()).insertCompany(
                "NEW_COMPANY",
                "新会社",
                PLATFORM_USER_ID,
                PLATFORM_USER_ID);
        verify(companyAdminMapper, never()).insertGenericMasterValue(
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any());
        assertRejectedLog();
    }

    @Test
    void missingGenericMasterReturnsInternalServerError() {
        signIn(PLATFORM_USER_ID, null, "PLATFORM", permission("PLATFORM_ADMIN", "WorkOps管理者"));
        when(companyAdminMapper.existsCompanyCode("NEW_COMPANY")).thenReturn(false);
        when(companyAdminMapper.findLastInsertId()).thenReturn(CREATED_COMPANY_ID);
        when(companyAdminMapper.findActiveGenericMasterIdByCode("ASSET_CATEGORY")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> companyAdminService.create(companyForm()))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        exception -> assertThat(exception.getStatusCode())
                                .isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR));
        verify(companyAdminMapper).insertCompany(
                "NEW_COMPANY",
                "新会社",
                PLATFORM_USER_ID,
                PLATFORM_USER_ID);
        verify(companyAdminMapper, never()).insertGenericMasterValue(
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any());
        verifyNoInteractions(operationLogger);
    }

    @Test
    void tenantManagerCannotCreateCompany() {
        signIn(3L, 1L, "TENANT", permission("TENANT_MANAGER", "管理者"));

        assertThatThrownBy(() -> companyAdminService.create(companyForm()))
                .isInstanceOf(AccessDeniedException.class);
        verifyNoInteractions(companyAdminMapper);
        verifyNoInteractions(operationLogger);
    }

    private void assertSuccessLog(Long targetId) {
        ArgumentCaptor<OperationLogRecord> captor = ArgumentCaptor.forClass(OperationLogRecord.class);
        verify(operationLogger).logSuccess(captor.capture());
        OperationLogRecord record = captor.getValue();
        assertThat(record.loginUserContext().userId()).isEqualTo(PLATFORM_USER_ID);
        assertThat(record.loginUserContext().companyId()).isNull();
        assertThat(record.operation()).isEqualTo("COMPANY_CREATE");
        assertThat(record.targetType()).isEqualTo("COMPANY");
        assertThat(record.targetId()).isEqualTo(targetId);
        assertThat(record.reasonCode()).isNull();
        assertThat(record.reasonCommentPresent()).isFalse();
        assertThat(record.exceptionType()).isNull();
        assertThat(record.getClass().getRecordComponents())
                .extracting(RecordComponent::getName)
                .doesNotContain("companyName", "name", "email", "content");
    }

    private void assertRejectedLog() {
        ArgumentCaptor<OperationLogRecord> captor = ArgumentCaptor.forClass(OperationLogRecord.class);
        verify(operationLogger).logRejected(captor.capture());
        OperationLogRecord record = captor.getValue();
        assertThat(record.operation()).isEqualTo("COMPANY_CREATE");
        assertThat(record.targetType()).isEqualTo("COMPANY");
        assertThat(record.targetId()).isNull();
        assertThat(record.reasonCode()).isEqualTo("DUPLICATE_COMPANY_CODE");
        assertThat(record.reasonCommentPresent()).isFalse();
        assertThat(record.exceptionType()).isEqualTo("ResponseStatusException");
    }

    private void signIn(Long userId, Long companyId, String actorType, PermissionSetContext permissionSet) {
        LoginUserContext loginUserContext = new LoginUserContext(
                userId,
                "test-user",
                "test-user@example.local",
                actorType,
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

    private CompanyForm companyForm() {
        return new CompanyForm("NEW_COMPANY", "新会社");
    }

    @Configuration
    @EnableMethodSecurity
    static class CompanyAdminServiceTestConfig {

        @Bean
        CurrentUserProvider currentUserProvider() {
            return new CurrentUserProvider();
        }

        @Bean
        CompanyAdminMapper companyAdminMapper() {
            return mock(CompanyAdminMapper.class);
        }

        @Bean
        OperationLogger operationLogger() {
            return mock(OperationLogger.class);
        }

        @Bean
        TenantInitializationService tenantInitializationService(CompanyAdminMapper companyAdminMapper) {
            return new TenantInitializationService(companyAdminMapper);
        }

        @Bean
        CompanyAdminService companyAdminService(
                CurrentUserProvider currentUserProvider,
                CompanyAdminMapper companyAdminMapper,
                TenantInitializationService tenantInitializationService,
                OperationLogger operationLogger) {
            return new CompanyAdminService(
                    currentUserProvider,
                    companyAdminMapper,
                    tenantInitializationService,
                    operationLogger);
        }
    }
}
