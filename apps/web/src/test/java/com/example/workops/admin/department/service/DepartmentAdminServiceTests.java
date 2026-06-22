package com.example.workops.admin.department.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.example.workops.admin.department.form.DepartmentForm;
import com.example.workops.admin.department.form.DepartmentSearchForm;
import com.example.workops.admin.department.mapper.DepartmentAdminMapper;
import com.example.workops.admin.department.model.DepartmentListItem;
import com.example.workops.admin.department.model.DepartmentListPage;
import com.example.workops.common.logging.OperationLogRecord;
import com.example.workops.common.logging.OperationLogger;
import com.example.workops.common.security.CurrentUserProvider;
import com.example.workops.common.security.LoginUserContext;
import com.example.workops.common.security.PermissionSetContext;
import java.lang.reflect.RecordComponent;
import java.time.LocalDateTime;
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

@SpringJUnitConfig(DepartmentAdminServiceTests.DepartmentAdminServiceTestConfig.class)
class DepartmentAdminServiceTests {

  private static final Long COMPANY_ID = 1L;
  private static final Long OTHER_COMPANY_ID = 2L;
  private static final Long PLATFORM_USER_ID = 7L;
  private static final Long MANAGER_USER_ID = 3L;
  private static final Long DEPARTMENT_ID = 20L;

  @Autowired private DepartmentAdminService departmentAdminService;

  @Autowired private DepartmentAdminMapper departmentAdminMapper;

  @Autowired private OperationLogger operationLogger;

  @BeforeEach
  void setUp() {
    reset(departmentAdminMapper, operationLogger);
    SecurityContextHolder.clearContext();
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void platformAdminCanFindDepartmentsByCompanyId() {
    signIn(PLATFORM_USER_ID, null, "PLATFORM", permission("PLATFORM_ADMIN", "WorkOps管理者"));
    DepartmentListItem department = departmentListItem();
    givenActiveCompany(COMPANY_ID);
    DepartmentSearchForm departmentSearchForm = new DepartmentSearchForm(false);
    when(departmentAdminMapper.findDepartmentsByCompanyIdAndSearchForm(
            COMPANY_ID, departmentSearchForm))
        .thenReturn(List.of(department));

    DepartmentListPage result =
        departmentAdminService.findPlatformDepartmentList(COMPANY_ID, departmentSearchForm);

    assertCompanyPage(result, COMPANY_ID);
    assertThat(result.showDeleted()).isFalse();
    assertThat(result.departments()).containsExactly(department);
    verify(departmentAdminMapper)
        .findDepartmentsByCompanyIdAndSearchForm(COMPANY_ID, departmentSearchForm);
  }

  @Test
  void platformAdminCanCreateDepartmentForSpecifiedCompany() {
    signIn(PLATFORM_USER_ID, null, "PLATFORM", permission("PLATFORM_ADMIN", "WorkOps管理者"));
    givenActiveCompany(OTHER_COMPANY_ID);
    when(departmentAdminMapper.existsDepartmentCodeByCompanyId(OTHER_COMPANY_ID, "HR"))
        .thenReturn(false);
    when(departmentAdminMapper.findLastInsertId()).thenReturn(DEPARTMENT_ID);

    Long result =
        departmentAdminService.createPlatformDepartment(OTHER_COMPANY_ID, departmentForm());

    assertThat(result).isEqualTo(DEPARTMENT_ID);
    verify(departmentAdminMapper)
        .insertDepartment(OTHER_COMPANY_ID, "HR", "人事部", PLATFORM_USER_ID, PLATFORM_USER_ID);
    assertSuccessLog(PLATFORM_USER_ID, null, DEPARTMENT_ID);
  }

  @Test
  void tenantManagerCanFindOwnCompanyDepartments() {
    signIn(MANAGER_USER_ID, COMPANY_ID, "TENANT", permission("TENANT_MANAGER", "管理者"));
    DepartmentListItem department = departmentListItem();
    givenActiveCompany(COMPANY_ID);
    DepartmentSearchForm departmentSearchForm = new DepartmentSearchForm(true);
    when(departmentAdminMapper.findDepartmentsByCompanyIdAndSearchForm(
            COMPANY_ID, departmentSearchForm))
        .thenReturn(List.of(department));

    DepartmentListPage result =
        departmentAdminService.findTenantDepartmentList(departmentSearchForm);

    assertCompanyPage(result, COMPANY_ID);
    assertThat(result.showDeleted()).isTrue();
    assertThat(result.departments()).containsExactly(department);
    verify(departmentAdminMapper)
        .findDepartmentsByCompanyIdAndSearchForm(COMPANY_ID, departmentSearchForm);
  }

  @Test
  void tenantManagerCanCreateDepartmentForOwnCompany() {
    signIn(MANAGER_USER_ID, COMPANY_ID, "TENANT", permission("TENANT_MANAGER", "管理者"));
    givenActiveCompany(COMPANY_ID);
    when(departmentAdminMapper.existsDepartmentCodeByCompanyId(COMPANY_ID, "HR")).thenReturn(false);
    when(departmentAdminMapper.findLastInsertId()).thenReturn(DEPARTMENT_ID);

    Long result = departmentAdminService.createTenantDepartment(departmentForm());

    assertThat(result).isEqualTo(DEPARTMENT_ID);
    verify(departmentAdminMapper)
        .insertDepartment(COMPANY_ID, "HR", "人事部", MANAGER_USER_ID, MANAGER_USER_ID);
    assertSuccessLog(MANAGER_USER_ID, COMPANY_ID, DEPARTMENT_ID);
  }

  @Test
  void duplicateDepartmentCodeIsRejectedBeforeInsert() {
    signIn(MANAGER_USER_ID, COMPANY_ID, "TENANT", permission("TENANT_MANAGER", "管理者"));
    givenActiveCompany(COMPANY_ID);
    when(departmentAdminMapper.existsDepartmentCodeByCompanyId(COMPANY_ID, "HR")).thenReturn(true);

    assertThatThrownBy(() -> departmentAdminService.createTenantDepartment(departmentForm()))
        .isInstanceOfSatisfying(
            ResponseStatusException.class,
            exception -> assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST));
    verify(departmentAdminMapper, never())
        .insertDepartment(COMPANY_ID, "HR", "人事部", MANAGER_USER_ID, MANAGER_USER_ID);
    assertRejectedLog();
  }

  @Test
  void platformAdminCanUpdateDepartmentForSpecifiedCompany() {
    signIn(PLATFORM_USER_ID, null, "PLATFORM", permission("PLATFORM_ADMIN", "WorkOps管理者"));
    givenActiveCompany(OTHER_COMPANY_ID);
    when(departmentAdminMapper.findActiveDepartmentByIdAndCompanyId(
            DEPARTMENT_ID, OTHER_COMPANY_ID))
        .thenReturn(Optional.of(departmentListItem()));

    departmentAdminService.updatePlatformDepartment(
        OTHER_COMPANY_ID, DEPARTMENT_ID, departmentForm());

    verify(departmentAdminMapper)
        .updateActiveDepartmentNameByIdAndCompanyId(
            DEPARTMENT_ID, OTHER_COMPANY_ID, "人事部", PLATFORM_USER_ID);
    assertSuccessLog("DEPARTMENT_UPDATE", PLATFORM_USER_ID, null, DEPARTMENT_ID);
  }

  @Test
  void tenantManagerCanUpdateOwnCompanyDepartment() {
    signIn(MANAGER_USER_ID, COMPANY_ID, "TENANT", permission("TENANT_MANAGER", "管理者"));
    givenActiveCompany(COMPANY_ID);
    when(departmentAdminMapper.findActiveDepartmentByIdAndCompanyId(DEPARTMENT_ID, COMPANY_ID))
        .thenReturn(Optional.of(departmentListItem()));

    departmentAdminService.updateTenantDepartment(DEPARTMENT_ID, departmentForm());

    verify(departmentAdminMapper)
        .updateActiveDepartmentNameByIdAndCompanyId(
            DEPARTMENT_ID, COMPANY_ID, "人事部", MANAGER_USER_ID);
    assertSuccessLog("DEPARTMENT_UPDATE", MANAGER_USER_ID, COMPANY_ID, DEPARTMENT_ID);
  }

  @Test
  void platformAdminCanDeleteDepartmentForSpecifiedCompany() {
    signIn(PLATFORM_USER_ID, null, "PLATFORM", permission("PLATFORM_ADMIN", "WorkOps管理者"));
    givenActiveCompany(OTHER_COMPANY_ID);
    when(departmentAdminMapper.findActiveDepartmentByIdAndCompanyId(
            DEPARTMENT_ID, OTHER_COMPANY_ID))
        .thenReturn(Optional.of(departmentListItem()));

    departmentAdminService.deletePlatformDepartment(OTHER_COMPANY_ID, DEPARTMENT_ID);

    verify(departmentAdminMapper)
        .logicalDeleteActiveDepartmentByIdAndCompanyId(
            DEPARTMENT_ID, OTHER_COMPANY_ID, PLATFORM_USER_ID);
    assertSuccessLog("DEPARTMENT_DELETE", PLATFORM_USER_ID, null, DEPARTMENT_ID);
  }

  @Test
  void tenantManagerCanDeleteOwnCompanyDepartmentEvenWithActiveUsers() {
    signIn(MANAGER_USER_ID, COMPANY_ID, "TENANT", permission("TENANT_MANAGER", "管理者"));
    givenActiveCompany(COMPANY_ID);
    when(departmentAdminMapper.findActiveDepartmentByIdAndCompanyId(DEPARTMENT_ID, COMPANY_ID))
        .thenReturn(Optional.of(departmentListItem()));

    departmentAdminService.deleteTenantDepartment(DEPARTMENT_ID);

    verify(departmentAdminMapper)
        .logicalDeleteActiveDepartmentByIdAndCompanyId(DEPARTMENT_ID, COMPANY_ID, MANAGER_USER_ID);
    assertSuccessLog("DEPARTMENT_DELETE", MANAGER_USER_ID, COMPANY_ID, DEPARTMENT_ID);
  }

  @Test
  void missingOrDeletedDepartmentReturnsNotFoundForUpdateAndDelete() {
    signIn(MANAGER_USER_ID, COMPANY_ID, "TENANT", permission("TENANT_MANAGER", "管理者"));
    givenActiveCompany(COMPANY_ID);
    when(departmentAdminMapper.findActiveDepartmentByIdAndCompanyId(DEPARTMENT_ID, COMPANY_ID))
        .thenReturn(Optional.empty());

    assertThatThrownBy(
            () -> departmentAdminService.updateTenantDepartment(DEPARTMENT_ID, departmentForm()))
        .isInstanceOfSatisfying(
            ResponseStatusException.class,
            exception -> assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
    assertRejectedLog("DEPARTMENT_UPDATE", DEPARTMENT_ID, "DEPARTMENT_NOT_FOUND_OR_FORBIDDEN");

    reset(operationLogger);
    assertThatThrownBy(() -> departmentAdminService.deleteTenantDepartment(DEPARTMENT_ID))
        .isInstanceOfSatisfying(
            ResponseStatusException.class,
            exception -> assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
    assertRejectedLog("DEPARTMENT_DELETE", DEPARTMENT_ID, "DEPARTMENT_NOT_FOUND_OR_FORBIDDEN");
  }

  @Test
  void missingCompanyReturnsNotFound() {
    signIn(PLATFORM_USER_ID, null, "PLATFORM", permission("PLATFORM_ADMIN", "WorkOps管理者"));
    when(departmentAdminMapper.findActiveCompanyId(999L)).thenReturn(Optional.empty());

    assertThatThrownBy(
            () -> departmentAdminService.createPlatformDepartment(999L, departmentForm()))
        .isInstanceOfSatisfying(
            ResponseStatusException.class,
            exception -> assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
    verify(departmentAdminMapper, never())
        .insertDepartment(999L, "HR", "人事部", PLATFORM_USER_ID, PLATFORM_USER_ID);
    verifyNoInteractions(operationLogger);
  }

  @Test
  void tenantManagerCannotInvokePlatformMethods() {
    signIn(MANAGER_USER_ID, COMPANY_ID, "TENANT", permission("TENANT_MANAGER", "管理者"));

    assertThatThrownBy(
            () ->
                departmentAdminService.findPlatformDepartmentList(
                    OTHER_COMPANY_ID, new DepartmentSearchForm(false)))
        .isInstanceOf(AccessDeniedException.class);
    assertThatThrownBy(
            () ->
                departmentAdminService.createPlatformDepartment(OTHER_COMPANY_ID, departmentForm()))
        .isInstanceOf(AccessDeniedException.class);
    assertThatThrownBy(
            () ->
                departmentAdminService.updatePlatformDepartment(
                    OTHER_COMPANY_ID, DEPARTMENT_ID, departmentForm()))
        .isInstanceOf(AccessDeniedException.class);
    assertThatThrownBy(
            () -> departmentAdminService.deletePlatformDepartment(OTHER_COMPANY_ID, DEPARTMENT_ID))
        .isInstanceOf(AccessDeniedException.class);
    verifyNoInteractions(departmentAdminMapper);
    verifyNoInteractions(operationLogger);
  }

  @Test
  void viewerAndEditorCannotCreateDepartments() {
    signIn(1L, COMPANY_ID, "TENANT", permission("TENANT_VIEWER", "閲覧者"));
    assertThatThrownBy(() -> departmentAdminService.createTenantDepartment(departmentForm()))
        .isInstanceOf(AccessDeniedException.class);
    assertThatThrownBy(
            () -> departmentAdminService.updateTenantDepartment(DEPARTMENT_ID, departmentForm()))
        .isInstanceOf(AccessDeniedException.class);
    assertThatThrownBy(() -> departmentAdminService.deleteTenantDepartment(DEPARTMENT_ID))
        .isInstanceOf(AccessDeniedException.class);

    SecurityContextHolder.clearContext();
    signIn(2L, COMPANY_ID, "TENANT", permission("TENANT_EDITOR", "編集者"));
    assertThatThrownBy(() -> departmentAdminService.createTenantDepartment(departmentForm()))
        .isInstanceOf(AccessDeniedException.class);
    assertThatThrownBy(
            () -> departmentAdminService.updateTenantDepartment(DEPARTMENT_ID, departmentForm()))
        .isInstanceOf(AccessDeniedException.class);
    assertThatThrownBy(() -> departmentAdminService.deleteTenantDepartment(DEPARTMENT_ID))
        .isInstanceOf(AccessDeniedException.class);

    verifyNoInteractions(departmentAdminMapper);
    verifyNoInteractions(operationLogger);
  }

  private void assertSuccessLog(Long userId, Long companyId, Long departmentId) {
    assertSuccessLog("DEPARTMENT_CREATE", userId, companyId, departmentId);
  }

  private void assertSuccessLog(String operation, Long userId, Long companyId, Long departmentId) {
    ArgumentCaptor<OperationLogRecord> captor = ArgumentCaptor.forClass(OperationLogRecord.class);
    verify(operationLogger).logSuccess(captor.capture());
    OperationLogRecord record = captor.getValue();
    assertThat(record.loginUserContext().userId()).isEqualTo(userId);
    assertThat(record.loginUserContext().companyId()).isEqualTo(companyId);
    assertThat(record.operation()).isEqualTo(operation);
    assertThat(record.targetType()).isEqualTo("DEPARTMENT");
    assertThat(record.targetId()).isEqualTo(departmentId);
    assertThat(record.reasonCode()).isNull();
    assertThat(record.reasonCommentPresent()).isFalse();
    assertThat(record.exceptionType()).isNull();
    assertThat(record.getClass().getRecordComponents())
        .extracting(RecordComponent::getName)
        .doesNotContain("departmentName", "name", "email", "content");
  }

  private void assertRejectedLog() {
    assertRejectedLog("DEPARTMENT_CREATE", null, "DUPLICATE_DEPARTMENT_CODE");
  }

  private void assertRejectedLog(String operation, Long departmentId, String reasonCode) {
    ArgumentCaptor<OperationLogRecord> captor = ArgumentCaptor.forClass(OperationLogRecord.class);
    verify(operationLogger).logRejected(captor.capture());
    OperationLogRecord record = captor.getValue();
    assertThat(record.operation()).isEqualTo(operation);
    assertThat(record.targetType()).isEqualTo("DEPARTMENT");
    assertThat(record.targetId()).isEqualTo(departmentId);
    assertThat(record.reasonCode()).isEqualTo(reasonCode);
    assertThat(record.reasonCommentPresent()).isFalse();
    assertThat(record.exceptionType()).isEqualTo("ResponseStatusException");
  }

  private void signIn(
      Long userId, Long companyId, String actorType, PermissionSetContext permissionSet) {
    LoginUserContext loginUserContext =
        new LoginUserContext(
            userId,
            "test-user",
            "test-user@example.local",
            actorType,
            companyId,
            List.of(permissionSet));
    SecurityContextHolder.getContext()
        .setAuthentication(
            new UsernamePasswordAuthenticationToken(
                loginUserContext, null, List.of(new SimpleGrantedAuthority(permissionSet.code()))));
  }

  private PermissionSetContext permission(String code, String name) {
    return new PermissionSetContext(code, name);
  }

  private DepartmentForm departmentForm() {
    return new DepartmentForm("HR", "人事部");
  }

  private void givenActiveCompany(Long companyId) {
    when(departmentAdminMapper.findActiveCompanyId(companyId)).thenReturn(Optional.of(companyId));
    when(departmentAdminMapper.findActiveCompanyCodeById(companyId))
        .thenReturn(Optional.of("COMPANY_" + companyId));
    when(departmentAdminMapper.findActiveCompanyNameById(companyId))
        .thenReturn(Optional.of("会社" + companyId));
  }

  private void assertCompanyPage(DepartmentListPage result, Long companyId) {
    assertThat(result.companyId()).isEqualTo(companyId);
    assertThat(result.companyCode()).isEqualTo("COMPANY_" + companyId);
    assertThat(result.companyName()).isEqualTo("会社" + companyId);
  }

  private DepartmentListItem departmentListItem() {
    return new DepartmentListItem(
        DEPARTMENT_ID,
        "ADMIN",
        "総務部",
        false,
        2L,
        LocalDateTime.of(2026, 5, 1, 9, 0),
        LocalDateTime.of(2026, 5, 1, 9, 0));
  }

  @Configuration
  @EnableMethodSecurity
  static class DepartmentAdminServiceTestConfig {

    @Bean
    CurrentUserProvider currentUserProvider() {
      return new CurrentUserProvider();
    }

    @Bean
    DepartmentAdminMapper departmentAdminMapper() {
      return mock(DepartmentAdminMapper.class);
    }

    @Bean
    OperationLogger operationLogger() {
      return mock(OperationLogger.class);
    }

    @Bean
    DepartmentAdminService departmentAdminService(
        CurrentUserProvider currentUserProvider,
        DepartmentAdminMapper departmentAdminMapper,
        OperationLogger operationLogger) {
      return new DepartmentAdminService(
          currentUserProvider, departmentAdminMapper, operationLogger);
    }
  }
}
