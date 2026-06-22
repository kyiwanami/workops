package com.example.workops.admin.company.service;

import com.example.workops.admin.company.form.CompanyEditForm;
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
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * {@code PLATFORM_ADMIN} 向け会社管理ユースケースを扱うService。
 *
 * <p>会社コードは削除済み会社も含めて全体一意とし、会社削除は物理削除ではなく {@code companies.is_deleted = TRUE}
 * の論理削除として扱う。会社作成時は会社別初期マスタ値と 初期 {@code TENANT_MANAGER} を同一トランザクションで作成する。
 */
@Service
public class CompanyAdminService {

  private static final String COMPANY_TARGET_TYPE = "COMPANY";
  private static final String OPERATION_COMPANY_CREATE = "COMPANY_CREATE";
  private static final String OPERATION_COMPANY_UPDATE = "COMPANY_UPDATE";
  private static final String OPERATION_COMPANY_DELETE = "COMPANY_DELETE";
  private static final String REASON_DUPLICATE_COMPANY_CODE = "DUPLICATE_COMPANY_CODE";
  private static final String REASON_COMPANY_NOT_FOUND_OR_FORBIDDEN =
      "COMPANY_NOT_FOUND_OR_FORBIDDEN";
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

  /**
   * 会社一覧を検索条件付きで取得する。
   *
   * @param companySearchForm 削除済み表示条件を含む検索フォーム
   * @return PLATFORM管理対象の会社一覧
   */
  @Transactional(readOnly = true)
  @PreAuthorize("hasAuthority('PLATFORM_ADMIN')")
  public List<CompanyListItem> findCompanies(CompanySearchForm companySearchForm) {
    return companyAdminMapper.findCompaniesBySearchForm(companySearchForm);
  }

  /**
   * 会社詳細を削除済み会社も含めて取得する。
   *
   * @param companyId 会社ID
   * @return 会社詳細と配下データ件数
   * @throws ResponseStatusException 対象会社が存在しない場合
   */
  @Transactional(readOnly = true)
  @PreAuthorize("hasAuthority('PLATFORM_ADMIN')")
  public CompanyDetail findCompanyDetail(Long companyId) {
    return companyAdminMapper
        .findCompanyDetailById(companyId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "会社が見つかりません。"));
  }

  /**
   * 未削除会社の編集フォーム初期値を取得する。
   *
   * @param companyId 会社ID
   * @return 会社編集フォーム
   * @throws ResponseStatusException 対象会社が存在しない、または削除済みの場合
   */
  @Transactional(readOnly = true)
  @PreAuthorize("hasAuthority('PLATFORM_ADMIN')")
  public CompanyEditForm findCompanyEditForm(Long companyId) {
    CompanyDetail company = findActiveCompany(companyId, null, null);
    return CompanyEditForm.from(company);
  }

  /**
   * 会社、会社別初期マスタ値、初期 {@code TENANT_MANAGER} を作成する。
   *
   * @param companyForm 会社情報と初期TENANT_MANAGER情報を含むフォーム
   * @return 作成した会社ID
   * @throws ResponseStatusException 会社コードが使用済み、または初期ユーザー作成条件を満たさない場合
   */
  @Transactional
  @PreAuthorize("hasAuthority('PLATFORM_ADMIN')")
  public Long create(CompanyForm companyForm) {
    LoginUserContext currentUser = currentUserProvider.requireCurrentUser();
    assertUniqueCompanyCode(currentUser, companyForm.code());

    companyAdminMapper.insertCompany(
        companyForm.code(), companyForm.name(), currentUser.userId(), currentUser.userId());
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

  /**
   * 未削除会社の会社名を更新する。
   *
   * @param companyId 会社ID
   * @param companyEditForm 更新後の会社編集フォーム
   * @throws ResponseStatusException 対象会社が存在しない、または削除済みの場合
   */
  @Transactional
  @PreAuthorize("hasAuthority('PLATFORM_ADMIN')")
  public void updateCompany(Long companyId, CompanyEditForm companyEditForm) {
    LoginUserContext currentUser = currentUserProvider.requireCurrentUser();
    findActiveCompany(companyId, currentUser, OPERATION_COMPANY_UPDATE);
    companyAdminMapper.updateActiveCompanyNameById(
        companyId, companyEditForm.name(), currentUser.userId());
    logSuccess(currentUser, OPERATION_COMPANY_UPDATE, companyId);
  }

  /**
   * 未削除会社を論理削除する。
   *
   * <p>部署、ユーザー、申請、資産、会社別マスタ値などの配下データは物理削除しない。
   *
   * @param companyId 会社ID
   * @throws ResponseStatusException 対象会社が存在しない、または削除済みの場合
   */
  @Transactional
  @PreAuthorize("hasAuthority('PLATFORM_ADMIN')")
  public void deleteCompany(Long companyId) {
    LoginUserContext currentUser = currentUserProvider.requireCurrentUser();
    findActiveCompany(companyId, currentUser, OPERATION_COMPANY_DELETE);
    companyAdminMapper.logicalDeleteActiveCompanyById(companyId, currentUser.userId());
    logSuccess(currentUser, OPERATION_COMPANY_DELETE, companyId);
  }

  private void assertUniqueCompanyCode(LoginUserContext currentUser, String code) {
    if (companyAdminMapper.existsCompanyCode(code)) {
      logRejected(currentUser);
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "会社コードは既に使用されています。");
    }
  }

  private CompanyDetail findActiveCompany(
      Long companyId, LoginUserContext currentUser, String operation) {
    return companyAdminMapper
        .findActiveCompanyDetailById(companyId)
        .orElseThrow(
            () -> {
              if (currentUser != null) {
                logRejected(
                    currentUser, operation, companyId, REASON_COMPANY_NOT_FOUND_OR_FORBIDDEN);
              }
              return new ResponseStatusException(HttpStatus.NOT_FOUND, "会社が見つかりません。");
            });
  }

  private void logSuccess(LoginUserContext currentUser, Long companyId) {
    logSuccess(currentUser, OPERATION_COMPANY_CREATE, companyId);
  }

  private void logSuccess(LoginUserContext currentUser, String operation, Long companyId) {
    operationLogger.logSuccess(
        new OperationLogRecord(
            currentUser, operation, COMPANY_TARGET_TYPE, companyId, null, false, null));
  }

  private void logRejected(LoginUserContext currentUser) {
    logRejected(currentUser, OPERATION_COMPANY_CREATE, null, REASON_DUPLICATE_COMPANY_CODE);
  }

  private void logRejected(
      LoginUserContext currentUser, String operation, Long companyId, String reasonCode) {
    operationLogger.logRejected(
        new OperationLogRecord(
            currentUser,
            operation,
            COMPANY_TARGET_TYPE,
            companyId,
            reasonCode,
            false,
            EXCEPTION_RESPONSE_STATUS));
  }
}
