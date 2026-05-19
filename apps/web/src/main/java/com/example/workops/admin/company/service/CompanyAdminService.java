package com.example.workops.admin.company.service;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.example.workops.admin.company.form.CompanyForm;
import com.example.workops.admin.company.form.CompanySearchForm;
import com.example.workops.admin.company.mapper.CompanyAdminMapper;
import com.example.workops.admin.company.model.CompanyDetail;
import com.example.workops.admin.company.model.CompanyListItem;
import com.example.workops.admin.user.service.UserAdminService;
import com.example.workops.common.logging.OperationLogRecord;
import com.example.workops.common.logging.OperationLogger;
import com.example.workops.common.security.CurrentUserProvider;
import com.example.workops.common.security.LoginUserContext;

/**
 * PLATFORM_ADMIN向け会社管理ユースケースを扱うService。
 */
@Service
public class CompanyAdminService {

    private static final String COMPANY_TARGET_TYPE = "COMPANY";
    private static final String OPERATION_COMPANY_CREATE = "COMPANY_CREATE";
    private static final String REASON_DUPLICATE_COMPANY_CODE = "DUPLICATE_COMPANY_CODE";
    private static final String EXCEPTION_RESPONSE_STATUS = "ResponseStatusException";

    private final CurrentUserProvider currentUserProvider;
    private final CompanyAdminMapper companyAdminMapper;
    private final TenantInitializationService tenantInitializationService;
    private final UserAdminService userAdminService;
    private final OperationLogger operationLogger;

    public CompanyAdminService(
            CurrentUserProvider currentUserProvider,
            CompanyAdminMapper companyAdminMapper,
            TenantInitializationService tenantInitializationService,
            UserAdminService userAdminService,
            OperationLogger operationLogger) {
        this.currentUserProvider = currentUserProvider;
        this.companyAdminMapper = companyAdminMapper;
        this.tenantInitializationService = tenantInitializationService;
        this.userAdminService = userAdminService;
        this.operationLogger = operationLogger;
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('PLATFORM_ADMIN')")
    public List<CompanyListItem> findCompanies(CompanySearchForm companySearchForm) {
        return companyAdminMapper.findCompaniesBySearchForm(companySearchForm);
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('PLATFORM_ADMIN')")
    public CompanyDetail findCompanyDetail(Long companyId) {
        return companyAdminMapper.findCompanyDetailById(companyId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "会社が見つかりません。"));
    }

    @Transactional
    @PreAuthorize("hasAuthority('PLATFORM_ADMIN')")
    public Long create(CompanyForm companyForm) {
        LoginUserContext currentUser = currentUserProvider.requireCurrentUser();
        assertUniqueCompanyCode(currentUser, companyForm.code());

        companyAdminMapper.insertCompany(
                companyForm.code(),
                companyForm.name(),
                currentUser.userId(),
                currentUser.userId());
        Long companyId = companyAdminMapper.findLastInsertId();
        tenantInitializationService.initializeTenant(companyId, currentUser.userId());
        userAdminService.createInitialTenantManager(
                companyId,
                companyForm.initialTenantManagerUsername(),
                companyForm.initialTenantManagerName(),
                companyForm.initialTenantManagerEmail());
        logSuccess(currentUser, companyId);
        return companyId;
    }

    private void assertUniqueCompanyCode(LoginUserContext currentUser, String code) {
        if (companyAdminMapper.existsCompanyCode(code)) {
            logRejected(currentUser);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "会社コードは既に使用されています。");
        }
    }

    private void logSuccess(LoginUserContext currentUser, Long companyId) {
        operationLogger.logSuccess(new OperationLogRecord(
                currentUser,
                OPERATION_COMPANY_CREATE,
                COMPANY_TARGET_TYPE,
                companyId,
                null,
                false,
                null));
    }

    private void logRejected(LoginUserContext currentUser) {
        operationLogger.logRejected(new OperationLogRecord(
                currentUser,
                OPERATION_COMPANY_CREATE,
                COMPANY_TARGET_TYPE,
                null,
                REASON_DUPLICATE_COMPANY_CODE,
                false,
                EXCEPTION_RESPONSE_STATUS));
    }
}
