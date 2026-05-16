package com.example.workops.admin.department.service;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.example.workops.admin.department.form.DepartmentForm;
import com.example.workops.admin.department.form.DepartmentSearchForm;
import com.example.workops.admin.department.mapper.DepartmentAdminMapper;
import com.example.workops.admin.department.model.DepartmentListItem;
import com.example.workops.admin.department.model.DepartmentListPage;
import com.example.workops.common.logging.OperationLogRecord;
import com.example.workops.common.logging.OperationLogger;
import com.example.workops.common.security.CurrentUserProvider;
import com.example.workops.common.security.LoginUserContext;

/**
 * PLATFORM_ADMINとTENANT_MANAGER向け部署管理ユースケースを扱うService。
 */
@Service
public class DepartmentAdminService {

    private static final String DEPARTMENT_TARGET_TYPE = "DEPARTMENT";
    private static final String OPERATION_DEPARTMENT_CREATE = "DEPARTMENT_CREATE";
    private static final String OPERATION_DEPARTMENT_UPDATE = "DEPARTMENT_UPDATE";
    private static final String OPERATION_DEPARTMENT_DELETE = "DEPARTMENT_DELETE";
    private static final String REASON_DUPLICATE_DEPARTMENT_CODE = "DUPLICATE_DEPARTMENT_CODE";
    private static final String REASON_DEPARTMENT_NOT_FOUND_OR_FORBIDDEN = "DEPARTMENT_NOT_FOUND_OR_FORBIDDEN";
    private static final String EXCEPTION_RESPONSE_STATUS = "ResponseStatusException";

    private final CurrentUserProvider currentUserProvider;
    private final DepartmentAdminMapper departmentAdminMapper;
    private final OperationLogger operationLogger;

    public DepartmentAdminService(
            CurrentUserProvider currentUserProvider,
            DepartmentAdminMapper departmentAdminMapper,
            OperationLogger operationLogger) {
        this.currentUserProvider = currentUserProvider;
        this.departmentAdminMapper = departmentAdminMapper;
        this.operationLogger = operationLogger;
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('PLATFORM_ADMIN')")
    public DepartmentListPage findPlatformDepartmentList(
            Long companyId,
            DepartmentSearchForm departmentSearchForm) {
        return findDepartmentListPage(companyId, departmentSearchForm, true);
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('TENANT_MANAGER')")
    public DepartmentListPage findTenantDepartmentList(DepartmentSearchForm departmentSearchForm) {
        LoginUserContext currentUser = currentUserProvider.requireCurrentUser();
        return findDepartmentListPage(currentUser.companyId(), departmentSearchForm, true);
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('PLATFORM_ADMIN')")
    public DepartmentListPage findPlatformDepartmentFormPage(Long companyId) {
        return findDepartmentListPage(companyId, new DepartmentSearchForm(false), false);
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('TENANT_MANAGER')")
    public DepartmentListPage findTenantDepartmentFormPage() {
        LoginUserContext currentUser = currentUserProvider.requireCurrentUser();
        return findDepartmentListPage(currentUser.companyId(), new DepartmentSearchForm(false), false);
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('PLATFORM_ADMIN')")
    public DepartmentListItem findPlatformDepartmentForEdit(Long companyId, Long departmentId) {
        Long activeCompanyId = findActiveCompanyId(companyId);
        return findActiveDepartment(activeCompanyId, departmentId, null, null);
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('TENANT_MANAGER')")
    public DepartmentListItem findTenantDepartmentForEdit(Long departmentId) {
        LoginUserContext currentUser = currentUserProvider.requireCurrentUser();
        Long activeCompanyId = findActiveCompanyId(currentUser.companyId());
        return findActiveDepartment(activeCompanyId, departmentId, null, null);
    }

    @Transactional
    @PreAuthorize("hasAuthority('PLATFORM_ADMIN')")
    public Long createPlatformDepartment(Long companyId, DepartmentForm departmentForm) {
        LoginUserContext currentUser = currentUserProvider.requireCurrentUser();
        Long activeCompanyId = findActiveCompanyId(companyId);
        return createDepartment(activeCompanyId, departmentForm, currentUser);
    }

    @Transactional
    @PreAuthorize("hasAuthority('TENANT_MANAGER')")
    public Long createTenantDepartment(DepartmentForm departmentForm) {
        LoginUserContext currentUser = currentUserProvider.requireCurrentUser();
        Long activeCompanyId = findActiveCompanyId(currentUser.companyId());
        return createDepartment(activeCompanyId, departmentForm, currentUser);
    }

    @Transactional
    @PreAuthorize("hasAuthority('PLATFORM_ADMIN')")
    public void updatePlatformDepartment(Long companyId, Long departmentId, DepartmentForm departmentForm) {
        LoginUserContext currentUser = currentUserProvider.requireCurrentUser();
        Long activeCompanyId = findActiveCompanyId(companyId);
        updateDepartment(activeCompanyId, departmentId, departmentForm, currentUser);
    }

    @Transactional
    @PreAuthorize("hasAuthority('TENANT_MANAGER')")
    public void updateTenantDepartment(Long departmentId, DepartmentForm departmentForm) {
        LoginUserContext currentUser = currentUserProvider.requireCurrentUser();
        Long activeCompanyId = findActiveCompanyId(currentUser.companyId());
        updateDepartment(activeCompanyId, departmentId, departmentForm, currentUser);
    }

    @Transactional
    @PreAuthorize("hasAuthority('PLATFORM_ADMIN')")
    public void deletePlatformDepartment(Long companyId, Long departmentId) {
        LoginUserContext currentUser = currentUserProvider.requireCurrentUser();
        Long activeCompanyId = findActiveCompanyId(companyId);
        deleteDepartment(activeCompanyId, departmentId, currentUser);
    }

    @Transactional
    @PreAuthorize("hasAuthority('TENANT_MANAGER')")
    public void deleteTenantDepartment(Long departmentId) {
        LoginUserContext currentUser = currentUserProvider.requireCurrentUser();
        Long activeCompanyId = findActiveCompanyId(currentUser.companyId());
        deleteDepartment(activeCompanyId, departmentId, currentUser);
    }

    private Long createDepartment(Long companyId, DepartmentForm departmentForm, LoginUserContext currentUser) {
        assertUniqueDepartmentCode(companyId, departmentForm.code(), currentUser);
        departmentAdminMapper.insertDepartment(
                companyId,
                departmentForm.code(),
                departmentForm.name(),
                currentUser.userId(),
                currentUser.userId());
        Long departmentId = departmentAdminMapper.findLastInsertId();
        logSuccess(currentUser, OPERATION_DEPARTMENT_CREATE, departmentId);
        return departmentId;
    }

    private void updateDepartment(
            Long companyId,
            Long departmentId,
            DepartmentForm departmentForm,
            LoginUserContext currentUser) {
        findActiveDepartment(companyId, departmentId, currentUser, OPERATION_DEPARTMENT_UPDATE);
        departmentAdminMapper.updateActiveDepartmentNameByIdAndCompanyId(
                departmentId,
                companyId,
                departmentForm.name(),
                currentUser.userId());
        logSuccess(currentUser, OPERATION_DEPARTMENT_UPDATE, departmentId);
    }

    private void deleteDepartment(Long companyId, Long departmentId, LoginUserContext currentUser) {
        findActiveDepartment(companyId, departmentId, currentUser, OPERATION_DEPARTMENT_DELETE);
        departmentAdminMapper.logicalDeleteActiveDepartmentByIdAndCompanyId(
                departmentId,
                companyId,
                currentUser.userId());
        logSuccess(currentUser, OPERATION_DEPARTMENT_DELETE, departmentId);
    }

    private DepartmentListPage findDepartmentListPage(
            Long companyId,
            DepartmentSearchForm departmentSearchForm,
            boolean includeDepartments) {
        Long activeCompanyId = findActiveCompanyId(companyId);
        String companyCode = departmentAdminMapper.findActiveCompanyCodeById(activeCompanyId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "会社が見つかりません。"));
        String companyName = departmentAdminMapper.findActiveCompanyNameById(activeCompanyId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "会社が見つかりません。"));
        List<DepartmentListItem> departments = List.of();
        if (includeDepartments) {
            departments = departmentAdminMapper.findDepartmentsByCompanyIdAndSearchForm(
                    activeCompanyId,
                    departmentSearchForm);
        }
        return new DepartmentListPage(
                activeCompanyId,
                companyCode,
                companyName,
                departmentSearchForm.showDeleted(),
                departments);
    }

    private Long findActiveCompanyId(Long companyId) {
        return departmentAdminMapper.findActiveCompanyId(companyId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "会社が見つかりません。"));
    }

    private DepartmentListItem findActiveDepartment(
            Long companyId,
            Long departmentId,
            LoginUserContext currentUser,
            String operation) {
        return departmentAdminMapper.findActiveDepartmentByIdAndCompanyId(departmentId, companyId)
                .orElseThrow(() -> {
                    if (currentUser != null) {
                        logRejected(
                                currentUser,
                                operation,
                                departmentId,
                                REASON_DEPARTMENT_NOT_FOUND_OR_FORBIDDEN);
                    }
                    return new ResponseStatusException(HttpStatus.NOT_FOUND, "部署が見つかりません。");
                });
    }

    private void assertUniqueDepartmentCode(Long companyId, String code, LoginUserContext currentUser) {
        if (departmentAdminMapper.existsDepartmentCodeByCompanyId(companyId, code)) {
            logRejected(currentUser, OPERATION_DEPARTMENT_CREATE, null, REASON_DUPLICATE_DEPARTMENT_CODE);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "部署コードは既に使用されています。");
        }
    }

    private void logSuccess(LoginUserContext currentUser, String operation, Long departmentId) {
        operationLogger.logSuccess(new OperationLogRecord(
                currentUser,
                operation,
                DEPARTMENT_TARGET_TYPE,
                departmentId,
                null,
                false,
                null));
    }

    private void logRejected(
            LoginUserContext currentUser,
            String operation,
            Long departmentId,
            String reasonCode) {
        operationLogger.logRejected(new OperationLogRecord(
                currentUser,
                operation,
                DEPARTMENT_TARGET_TYPE,
                departmentId,
                reasonCode,
                false,
                EXCEPTION_RESPONSE_STATUS));
    }
}
