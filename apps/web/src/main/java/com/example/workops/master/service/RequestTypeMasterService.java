package com.example.workops.master.service;

import com.example.workops.common.logging.OperationLogRecord;
import com.example.workops.common.logging.OperationLogger;
import com.example.workops.common.security.CurrentUserProvider;
import com.example.workops.common.security.LoginUserContext;
import com.example.workops.master.form.RequestTypeMasterForm;
import com.example.workops.master.form.RequestTypeMasterSearchForm;
import com.example.workops.master.mapper.RequestTypeMasterMapper;
import com.example.workops.master.model.RequestTypeMasterDetail;
import com.example.workops.master.model.RequestTypeMasterListItem;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * 会社別の申請種別マスタ管理ユースケースを扱うService。
 *
 * <p>申請種別は {@code generic_master_values} の会社別値として管理する。削除は物理削除ではなく {@code is_deleted = TRUE}
 * の論理削除であり、削除済みコードの再利用は許可しない。
 */
@Service
public class RequestTypeMasterService {

  private static final String GENERIC_MASTER_VALUE_TARGET_TYPE = "GENERIC_MASTER_VALUE";
  private static final String OPERATION_REQUEST_TYPE_CREATE = "REQUEST_TYPE_CREATE";
  private static final String OPERATION_REQUEST_TYPE_UPDATE = "REQUEST_TYPE_UPDATE";
  private static final String OPERATION_REQUEST_TYPE_DELETE = "REQUEST_TYPE_DELETE";
  private static final String OPERATION_REQUEST_TYPE_RESTORE = "REQUEST_TYPE_RESTORE";
  private static final String REASON_DUPLICATE_MASTER_VALUE_CODE = "DUPLICATE_MASTER_VALUE_CODE";
  private static final String REASON_MASTER_VALUE_NOT_FOUND_OR_FORBIDDEN =
      "MASTER_VALUE_NOT_FOUND_OR_FORBIDDEN";
  private static final String REASON_MASTER_VALUE_NOT_FOUND_OR_NOT_DELETED =
      "MASTER_VALUE_NOT_FOUND_OR_NOT_DELETED";
  private static final String EXCEPTION_RESPONSE_STATUS = "ResponseStatusException";

  private final CurrentUserProvider currentUserProvider;
  private final RequestTypeMasterMapper requestTypeMasterMapper;
  private final OperationLogger operationLogger;

  public RequestTypeMasterService(
      CurrentUserProvider currentUserProvider,
      RequestTypeMasterMapper requestTypeMasterMapper,
      OperationLogger operationLogger) {
    this.currentUserProvider = currentUserProvider;
    this.requestTypeMasterMapper = requestTypeMasterMapper;
    this.operationLogger = operationLogger;
  }

  /**
   * 現在ユーザーの会社に属する申請種別一覧を取得する。
   *
   * @param requestTypeMasterSearchForm 削除済み表示条件を含む検索フォーム
   * @return 会社境界で絞り込まれた申請種別一覧
   */
  @Transactional(readOnly = true)
  @PreAuthorize("hasAuthority('TENANT_MANAGER')")
  public List<RequestTypeMasterListItem> findList(
      RequestTypeMasterSearchForm requestTypeMasterSearchForm) {
    LoginUserContext currentUser = currentUserProvider.requireCurrentUser();
    return requestTypeMasterMapper.findListByCompanyIdAndSearchForm(
        currentUser.companyId(), requestTypeMasterSearchForm);
  }

  /**
   * 編集対象として未削除の申請種別を取得する。
   *
   * @param id 申請種別マスタ値ID
   * @return 編集対象の申請種別
   * @throws ResponseStatusException 対象が存在しない、他社値、または削除済みの場合
   */
  @Transactional(readOnly = true)
  @PreAuthorize("hasAuthority('TENANT_MANAGER')")
  public RequestTypeMasterDetail findForEdit(Long id) {
    LoginUserContext currentUser = currentUserProvider.requireCurrentUser();
    return requestTypeMasterMapper
        .findActiveByIdAndCompanyId(id, currentUser.companyId())
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "申請種別が見つかりません。"));
  }

  /**
   * 現在ユーザーの会社に申請種別を追加する。
   *
   * @param requestTypeMasterForm 入力済みの申請種別フォーム
   * @throws ResponseStatusException コードが同じ会社内で使用済みの場合
   */
  @Transactional
  @PreAuthorize("hasAuthority('TENANT_MANAGER')")
  public void create(RequestTypeMasterForm requestTypeMasterForm) {
    LoginUserContext currentUser = currentUserProvider.requireCurrentUser();
    assertUniqueCodeForCreate(currentUser, requestTypeMasterForm.code());

    requestTypeMasterMapper.insertRequestType(
        findRequestTypeMasterId(),
        currentUser.companyId(),
        requestTypeMasterForm.code(),
        requestTypeMasterForm.name(),
        requestTypeMasterForm.sortOrder(),
        currentUser.userId(),
        currentUser.userId());
    Long createdId = requestTypeMasterMapper.findLastInsertId();
    logSuccess(currentUser, OPERATION_REQUEST_TYPE_CREATE, createdId);
  }

  /**
   * 未削除の申請種別の名称と表示順を更新する。
   *
   * @param id 申請種別マスタ値ID
   * @param requestTypeMasterForm 更新後の申請種別フォーム
   * @throws ResponseStatusException 対象が存在しない、他社値、または削除済みの場合
   */
  @Transactional
  @PreAuthorize("hasAuthority('TENANT_MANAGER')")
  public void update(Long id, RequestTypeMasterForm requestTypeMasterForm) {
    LoginUserContext currentUser = currentUserProvider.requireCurrentUser();
    findActiveForOperation(id, currentUser, OPERATION_REQUEST_TYPE_UPDATE);

    requestTypeMasterMapper.updateActiveByIdAndCompanyId(
        id,
        currentUser.companyId(),
        requestTypeMasterForm.name(),
        requestTypeMasterForm.sortOrder(),
        currentUser.userId());
    logSuccess(currentUser, OPERATION_REQUEST_TYPE_UPDATE, id);
  }

  /**
   * 未削除の申請種別を論理削除する。
   *
   * @param id 申請種別マスタ値ID
   * @throws ResponseStatusException 対象が存在しない、他社値、または削除済みの場合
   */
  @Transactional
  @PreAuthorize("hasAuthority('TENANT_MANAGER')")
  public void delete(Long id) {
    LoginUserContext currentUser = currentUserProvider.requireCurrentUser();
    findActiveForOperation(id, currentUser, OPERATION_REQUEST_TYPE_DELETE);

    requestTypeMasterMapper.logicalDeleteActiveByIdAndCompanyId(
        id, currentUser.companyId(), currentUser.userId());
    logSuccess(currentUser, OPERATION_REQUEST_TYPE_DELETE, id);
  }

  /**
   * 削除済みの申請種別を復活させる。
   *
   * @param id 申請種別マスタ値ID
   * @throws ResponseStatusException 対象が存在しない、他社値、または未削除の場合
   */
  @Transactional
  @PreAuthorize("hasAuthority('TENANT_MANAGER')")
  public void restore(Long id) {
    LoginUserContext currentUser = currentUserProvider.requireCurrentUser();
    findDeletedForOperation(id, currentUser, OPERATION_REQUEST_TYPE_RESTORE);

    requestTypeMasterMapper.restoreDeletedByIdAndCompanyId(
        id, currentUser.companyId(), currentUser.userId());
    logSuccess(currentUser, OPERATION_REQUEST_TYPE_RESTORE, id);
  }

  /**
   * 申請種別作成時にコードが現在ユーザーの会社内で重複するか判定する。
   *
   * @param code 申請種別コード
   * @return 未削除・削除済みを問わず同じ会社内で使用済みの場合はtrue
   */
  @Transactional(readOnly = true)
  @PreAuthorize("hasAuthority('TENANT_MANAGER')")
  public boolean isDuplicateCodeForCreate(String code) {
    LoginUserContext currentUser = currentUserProvider.requireCurrentUser();
    return requestTypeMasterMapper.existsCodeByCompanyId(currentUser.companyId(), code);
  }

  private Long findRequestTypeMasterId() {
    return requestTypeMasterMapper
        .findRequestTypeMasterId()
        .orElseThrow(
            () ->
                new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR, "申請種別マスタ種別が見つかりません。"));
  }

  private RequestTypeMasterDetail findActiveForOperation(
      Long id, LoginUserContext currentUser, String operation) {
    return requestTypeMasterMapper
        .findActiveByIdAndCompanyId(id, currentUser.companyId())
        .orElseThrow(
            () -> {
              logRejected(currentUser, operation, id, REASON_MASTER_VALUE_NOT_FOUND_OR_FORBIDDEN);
              return new ResponseStatusException(HttpStatus.NOT_FOUND, "申請種別が見つかりません。");
            });
  }

  private RequestTypeMasterDetail findDeletedForOperation(
      Long id, LoginUserContext currentUser, String operation) {
    return requestTypeMasterMapper
        .findDeletedByIdAndCompanyId(id, currentUser.companyId())
        .orElseThrow(
            () -> {
              logRejected(currentUser, operation, id, REASON_MASTER_VALUE_NOT_FOUND_OR_NOT_DELETED);
              return new ResponseStatusException(HttpStatus.NOT_FOUND, "申請種別が見つかりません。");
            });
  }

  private void assertUniqueCodeForCreate(LoginUserContext currentUser, String code) {
    if (requestTypeMasterMapper.existsCodeByCompanyId(currentUser.companyId(), code)) {
      logRejected(
          currentUser, OPERATION_REQUEST_TYPE_CREATE, null, REASON_DUPLICATE_MASTER_VALUE_CODE);
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "申請種別コードは既に使用されています。");
    }
  }

  private void logSuccess(LoginUserContext currentUser, String operation, Long targetId) {
    operationLogger.logSuccess(
        new OperationLogRecord(
            currentUser, operation, GENERIC_MASTER_VALUE_TARGET_TYPE, targetId, null, false, null));
  }

  private void logRejected(
      LoginUserContext currentUser, String operation, Long targetId, String reasonCode) {
    operationLogger.logRejected(
        new OperationLogRecord(
            currentUser,
            operation,
            GENERIC_MASTER_VALUE_TARGET_TYPE,
            targetId,
            reasonCode,
            false,
            EXCEPTION_RESPONSE_STATUS));
  }
}
