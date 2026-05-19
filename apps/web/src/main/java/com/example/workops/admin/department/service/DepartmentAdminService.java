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
 * {@code PLATFORM_ADMIN} と {@code TENANT_MANAGER} 向け部署管理ユースケースを扱うService。
 *
 * <p>部署コードは削除済み部署も含めて会社内一意とし、部署削除は物理削除ではなく
 * {@code departments.is_deleted = TRUE} の論理削除として扱う。TENANT_MANAGER導線では、
 * 現在ユーザーの会社だけを対象にする。</p>
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

    /**
     * PLATFORM_ADMINが指定会社の部署一覧を取得する。
     *
     * @param companyId 会社ID
     * @param departmentSearchForm 削除済み表示条件を含む検索フォーム
     * @return 指定会社の部署一覧ページ
     * @throws ResponseStatusException 対象会社が存在しない、または削除済みの場合
     */
    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('PLATFORM_ADMIN')")
    public DepartmentListPage findPlatformDepartmentList(
            Long companyId,
            DepartmentSearchForm departmentSearchForm) {
        return findDepartmentListPage(companyId, departmentSearchForm, true);
    }

    /**
     * TENANT_MANAGERが自社の部署一覧を取得する。
     *
     * @param departmentSearchForm 削除済み表示条件を含む検索フォーム
     * @return 現在ユーザーの会社の部署一覧ページ
     */
    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('TENANT_MANAGER')")
    public DepartmentListPage findTenantDepartmentList(DepartmentSearchForm departmentSearchForm) {
        LoginUserContext currentUser = currentUserProvider.requireCurrentUser();
        return findDepartmentListPage(currentUser.companyId(), departmentSearchForm, true);
    }

    /**
     * PLATFORM_ADMIN向けの部署登録フォーム表示に必要な会社情報を取得する。
     *
     * @param companyId 会社ID
     * @return 部署一覧を含まない部署ページ情報
     */
    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('PLATFORM_ADMIN')")
    public DepartmentListPage findPlatformDepartmentFormPage(Long companyId) {
        return findDepartmentListPage(companyId, new DepartmentSearchForm(false), false);
    }

    /**
     * TENANT_MANAGER向けの部署登録フォーム表示に必要な自社情報を取得する。
     *
     * @return 部署一覧を含まない自社部署ページ情報
     */
    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('TENANT_MANAGER')")
    public DepartmentListPage findTenantDepartmentFormPage() {
        LoginUserContext currentUser = currentUserProvider.requireCurrentUser();
        return findDepartmentListPage(currentUser.companyId(), new DepartmentSearchForm(false), false);
    }

    /**
     * PLATFORM_ADMINが指定会社の未削除部署を編集対象として取得する。
     *
     * @param companyId 会社ID
     * @param departmentId 部署ID
     * @return 編集対象の部署
     */
    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('PLATFORM_ADMIN')")
    public DepartmentListItem findPlatformDepartmentForEdit(Long companyId, Long departmentId) {
        Long activeCompanyId = findActiveCompanyId(companyId);
        return findActiveDepartment(activeCompanyId, departmentId, null, null);
    }

    /**
     * TENANT_MANAGERが自社の未削除部署を編集対象として取得する。
     *
     * @param departmentId 部署ID
     * @return 編集対象の部署
     */
    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('TENANT_MANAGER')")
    public DepartmentListItem findTenantDepartmentForEdit(Long departmentId) {
        LoginUserContext currentUser = currentUserProvider.requireCurrentUser();
        Long activeCompanyId = findActiveCompanyId(currentUser.companyId());
        return findActiveDepartment(activeCompanyId, departmentId, null, null);
    }

    /**
     * PLATFORM_ADMINが指定会社に部署を作成する。
     *
     * @param companyId 会社ID
     * @param departmentForm 入力済みの部署フォーム
     * @return 作成した部署ID
     * @throws ResponseStatusException 会社が存在しない、または部署コードが会社内で使用済みの場合
     */
    @Transactional
    @PreAuthorize("hasAuthority('PLATFORM_ADMIN')")
    public Long createPlatformDepartment(Long companyId, DepartmentForm departmentForm) {
        LoginUserContext currentUser = currentUserProvider.requireCurrentUser();
        Long activeCompanyId = findActiveCompanyId(companyId);
        return createDepartment(activeCompanyId, departmentForm, currentUser);
    }

    /**
     * TENANT_MANAGERが自社に部署を作成する。
     *
     * @param departmentForm 入力済みの部署フォーム
     * @return 作成した部署ID
     * @throws ResponseStatusException 部署コードが自社内で使用済みの場合
     */
    @Transactional
    @PreAuthorize("hasAuthority('TENANT_MANAGER')")
    public Long createTenantDepartment(DepartmentForm departmentForm) {
        LoginUserContext currentUser = currentUserProvider.requireCurrentUser();
        Long activeCompanyId = findActiveCompanyId(currentUser.companyId());
        return createDepartment(activeCompanyId, departmentForm, currentUser);
    }

    /**
     * PLATFORM_ADMINが指定会社の未削除部署名を更新する。
     *
     * @param companyId 会社ID
     * @param departmentId 部署ID
     * @param departmentForm 更新後の部署フォーム
     */
    @Transactional
    @PreAuthorize("hasAuthority('PLATFORM_ADMIN')")
    public void updatePlatformDepartment(Long companyId, Long departmentId, DepartmentForm departmentForm) {
        LoginUserContext currentUser = currentUserProvider.requireCurrentUser();
        Long activeCompanyId = findActiveCompanyId(companyId);
        updateDepartment(activeCompanyId, departmentId, departmentForm, currentUser);
    }

    /**
     * TENANT_MANAGERが自社の未削除部署名を更新する。
     *
     * @param departmentId 部署ID
     * @param departmentForm 更新後の部署フォーム
     */
    @Transactional
    @PreAuthorize("hasAuthority('TENANT_MANAGER')")
    public void updateTenantDepartment(Long departmentId, DepartmentForm departmentForm) {
        LoginUserContext currentUser = currentUserProvider.requireCurrentUser();
        Long activeCompanyId = findActiveCompanyId(currentUser.companyId());
        updateDepartment(activeCompanyId, departmentId, departmentForm, currentUser);
    }

    /**
     * PLATFORM_ADMINが指定会社の未削除部署を論理削除する。
     *
     * @param companyId 会社ID
     * @param departmentId 部署ID
     */
    @Transactional
    @PreAuthorize("hasAuthority('PLATFORM_ADMIN')")
    public void deletePlatformDepartment(Long companyId, Long departmentId) {
        LoginUserContext currentUser = currentUserProvider.requireCurrentUser();
        Long activeCompanyId = findActiveCompanyId(companyId);
        deleteDepartment(activeCompanyId, departmentId, currentUser);
    }

    /**
     * TENANT_MANAGERが自社の未削除部署を論理削除する。
     *
     * @param departmentId 部署ID
     */
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
