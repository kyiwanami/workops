package com.example.workops.asset.service;

import com.example.workops.asset.form.AssetForm;
import com.example.workops.asset.form.AssetStatusForm;
import com.example.workops.asset.mapper.AssetMapper;
import com.example.workops.asset.model.AssetDetail;
import com.example.workops.common.logging.OperationLogRecord;
import com.example.workops.common.logging.OperationLogger;
import com.example.workops.common.security.CurrentUserProvider;
import com.example.workops.common.security.LoginUserContext;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * 資産カタログの登録、編集、ステータス変更、論理削除を実行するService。
 *
 * <p>このServiceの更新系メソッドは、Spring Securityの権限判定に加えて、会社境界、 資産分類・部署・ステータスの選択可否、資産コードの会社内一意性を検証する。
 * 資産削除は物理削除ではなく {@code is_deleted = TRUE} の論理削除として扱う。
 */
@Service
public class AssetCommandService {

  private static final String ASSET_TARGET_TYPE = "ASSET";
  private static final String OPERATION_ASSET_CREATE = "ASSET_CREATE";
  private static final String OPERATION_ASSET_UPDATE = "ASSET_UPDATE";
  private static final String OPERATION_ASSET_STATUS_CHANGE = "ASSET_STATUS_CHANGE";
  private static final String OPERATION_ASSET_DELETE = "ASSET_DELETE";
  private static final String REASON_COMPANY_MISMATCH = "COMPANY_MISMATCH";
  private static final String REASON_INVALID_ASSET_CATEGORY = "INVALID_ASSET_CATEGORY";
  private static final String REASON_INVALID_DEPARTMENT = "INVALID_DEPARTMENT";
  private static final String REASON_INVALID_ASSET_STATUS = "INVALID_ASSET_STATUS";
  private static final String REASON_DUPLICATE_ASSET_CODE = "DUPLICATE_ASSET_CODE";
  private static final String EXCEPTION_RESPONSE_STATUS = "ResponseStatusException";

  private final CurrentUserProvider currentUserProvider;
  private final AssetMapper assetMapper;
  private final OperationLogger operationLogger;

  public AssetCommandService(
      CurrentUserProvider currentUserProvider,
      AssetMapper assetMapper,
      OperationLogger operationLogger) {
    this.currentUserProvider = currentUserProvider;
    this.assetMapper = assetMapper;
    this.operationLogger = operationLogger;
  }

  /**
   * 編集画面用に現在ユーザーの会社内の資産を取得する。
   *
   * @param id 資産ID
   * @return 編集対象の資産詳細
   * @throws ResponseStatusException 対象資産が存在しない、または他社資産の場合
   */
  @Transactional(readOnly = true)
  @PreAuthorize("hasAnyAuthority('TENANT_EDITOR','TENANT_MANAGER')")
  public AssetDetail findAssetForEdit(Long id) {
    LoginUserContext currentUser = currentUserProvider.requireCurrentUser();
    return assetMapper
        .findDetailByIdAndCompanyId(id, currentUser.companyId())
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "資産が見つかりません。"));
  }

  /**
   * ステータス変更画面用に現在ユーザーの会社内の資産を取得する。
   *
   * @param id 資産ID
   * @return ステータス変更対象の資産詳細
   * @throws ResponseStatusException 対象資産が存在しない、または他社資産の場合
   */
  @Transactional(readOnly = true)
  @PreAuthorize("hasAnyAuthority('TENANT_EDITOR','TENANT_MANAGER')")
  public AssetDetail findAssetForStatusChange(Long id) {
    LoginUserContext currentUser = currentUserProvider.requireCurrentUser();
    return assetMapper
        .findDetailByIdAndCompanyId(id, currentUser.companyId())
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "資産が見つかりません。"));
  }

  /**
   * 現在ユーザーの会社に資産を登録する。
   *
   * @param assetForm 入力済みの資産フォーム
   * @return 作成した資産ID
   * @throws ResponseStatusException 資産分類、部署、ステータス、資産コードが業務条件を満たさない場合
   */
  @Transactional
  @PreAuthorize("hasAnyAuthority('TENANT_EDITOR','TENANT_MANAGER')")
  public Long createAsset(AssetForm assetForm) {
    LoginUserContext currentUser = currentUserProvider.requireCurrentUser();
    assertSelectableAssetCategory(
        assetForm.assetCategoryValueId(), currentUser, OPERATION_ASSET_CREATE, null);
    assertSelectableDepartment(assetForm.departmentId(), currentUser, OPERATION_ASSET_CREATE, null);
    assertSelectableStatus(assetForm.statusCode(), currentUser, OPERATION_ASSET_CREATE, null);
    assertUniqueCodeForCreate(assetForm.code(), currentUser, OPERATION_ASSET_CREATE);

    assetMapper.insertAsset(
        currentUser.companyId(),
        assetForm.assetCategoryValueId(),
        assetForm.departmentId(),
        assetForm.code(),
        assetForm.name(),
        assetForm.statusCode(),
        assetForm.note(),
        currentUser.userId(),
        currentUser.userId());
    Long createdId = assetMapper.findLastInsertId();
    logSuccess(currentUser, OPERATION_ASSET_CREATE, createdId);
    return createdId;
  }

  /**
   * 現在ユーザーの会社内に存在する資産の編集可能項目を更新する。
   *
   * @param id 資産ID
   * @param assetForm 更新後の資産フォーム
   * @throws ResponseStatusException 対象資産が存在しない、または入力値が業務条件を満たさない場合
   */
  @Transactional
  @PreAuthorize("hasAnyAuthority('TENANT_EDITOR','TENANT_MANAGER')")
  public void updateAsset(Long id, AssetForm assetForm) {
    LoginUserContext currentUser = currentUserProvider.requireCurrentUser();
    findAssetForOperation(id, currentUser, OPERATION_ASSET_UPDATE);
    assertSelectableAssetCategory(
        assetForm.assetCategoryValueId(), currentUser, OPERATION_ASSET_UPDATE, id);
    assertSelectableDepartment(assetForm.departmentId(), currentUser, OPERATION_ASSET_UPDATE, id);
    assertSelectableStatus(assetForm.statusCode(), currentUser, OPERATION_ASSET_UPDATE, id);
    assertUniqueCodeForUpdate(id, assetForm.code(), currentUser, OPERATION_ASSET_UPDATE);

    assetMapper.updateAssetByIdAndCompanyId(
        id,
        currentUser.companyId(),
        assetForm.assetCategoryValueId(),
        assetForm.departmentId(),
        assetForm.code(),
        assetForm.name(),
        assetForm.statusCode(),
        assetForm.note(),
        currentUser.userId());
    logSuccess(currentUser, OPERATION_ASSET_UPDATE, id);
  }

  /**
   * 現在ユーザーの会社内に存在する資産のステータスを変更する。
   *
   * @param id 資産ID
   * @param assetStatusForm 変更後のステータスを含むフォーム
   * @throws ResponseStatusException 対象資産が存在しない、またはステータスコードが不正な場合
   */
  @Transactional
  @PreAuthorize("hasAnyAuthority('TENANT_EDITOR','TENANT_MANAGER')")
  public void updateStatus(Long id, AssetStatusForm assetStatusForm) {
    LoginUserContext currentUser = currentUserProvider.requireCurrentUser();
    findAssetForOperation(id, currentUser, OPERATION_ASSET_STATUS_CHANGE);
    assertSelectableStatus(
        assetStatusForm.statusCode(), currentUser, OPERATION_ASSET_STATUS_CHANGE, id);

    assetMapper.updateAssetStatusByIdAndCompanyId(
        id, currentUser.companyId(), assetStatusForm.statusCode(), currentUser.userId());
    logSuccess(currentUser, OPERATION_ASSET_STATUS_CHANGE, id);
  }

  /**
   * 現在ユーザーの会社内に存在する資産を論理削除する。
   *
   * @param id 資産ID
   * @throws ResponseStatusException 対象資産が存在しない、または他社資産の場合
   */
  @Transactional
  @PreAuthorize("hasAuthority('TENANT_MANAGER')")
  public void deleteAsset(Long id) {
    LoginUserContext currentUser = currentUserProvider.requireCurrentUser();
    findAssetForOperation(id, currentUser, OPERATION_ASSET_DELETE);

    assetMapper.logicalDeleteAssetByIdAndCompanyId(
        id, currentUser.companyId(), currentUser.userId());
    logSuccess(currentUser, OPERATION_ASSET_DELETE, id);
  }

  /**
   * 資産登録時に資産コードが現在ユーザーの会社内で重複するか判定する。
   *
   * @param code 資産コード
   * @return 未削除・削除済みを問わず同じ会社内で使用済みの場合はtrue
   */
  @Transactional(readOnly = true)
  @PreAuthorize("hasAnyAuthority('TENANT_EDITOR','TENANT_MANAGER')")
  public boolean isDuplicateCodeForCreate(String code) {
    LoginUserContext currentUser = currentUserProvider.requireCurrentUser();
    return assetMapper.existsAssetCodeByCompanyId(code, currentUser.companyId());
  }

  /**
   * 資産編集時に資産コードが現在ユーザーの会社内の別資産と重複するか判定する。
   *
   * @param id 編集中の資産ID
   * @param code 資産コード
   * @return 同じ会社内の別資産で使用済みの場合はtrue
   */
  @Transactional(readOnly = true)
  @PreAuthorize("hasAnyAuthority('TENANT_EDITOR','TENANT_MANAGER')")
  public boolean isDuplicateCodeForUpdate(Long id, String code) {
    LoginUserContext currentUser = currentUserProvider.requireCurrentUser();
    return assetMapper.existsOtherAssetCodeByCompanyId(id, code, currentUser.companyId());
  }

  private AssetDetail findAssetForOperation(
      Long id, LoginUserContext currentUser, String operation) {
    return assetMapper
        .findDetailByIdAndCompanyId(id, currentUser.companyId())
        .orElseThrow(
            () -> {
              logRejected(currentUser, operation, id, REASON_COMPANY_MISMATCH);
              return new ResponseStatusException(HttpStatus.NOT_FOUND, "資産が見つかりません。");
            });
  }

  private void assertSelectableAssetCategory(
      Long assetCategoryValueId, LoginUserContext currentUser, String operation, Long targetId) {
    if (!assetMapper.existsAssetCategoryByIdAndCompanyId(
        assetCategoryValueId, currentUser.companyId())) {
      logRejected(currentUser, operation, targetId, REASON_INVALID_ASSET_CATEGORY);
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "資産カテゴリが不正です。");
    }
  }

  private void assertSelectableDepartment(
      Long departmentId, LoginUserContext currentUser, String operation, Long targetId) {
    if (departmentId != null
        && !assetMapper.existsDepartmentByIdAndCompanyId(departmentId, currentUser.companyId())) {
      logRejected(currentUser, operation, targetId, REASON_INVALID_DEPARTMENT);
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "管理部署が不正です。");
    }
  }

  private void assertSelectableStatus(
      String statusCode, LoginUserContext currentUser, String operation, Long targetId) {
    if (!assetMapper.existsStatusCode(statusCode)) {
      logRejected(currentUser, operation, targetId, REASON_INVALID_ASSET_STATUS);
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "資産ステータスが不正です。");
    }
  }

  private void assertUniqueCodeForCreate(
      String code, LoginUserContext currentUser, String operation) {
    if (assetMapper.existsAssetCodeByCompanyId(code, currentUser.companyId())) {
      logRejected(currentUser, operation, null, REASON_DUPLICATE_ASSET_CODE);
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "資産コードは既に使用されています。");
    }
  }

  private void assertUniqueCodeForUpdate(
      Long id, String code, LoginUserContext currentUser, String operation) {
    if (assetMapper.existsOtherAssetCodeByCompanyId(id, code, currentUser.companyId())) {
      logRejected(currentUser, operation, id, REASON_DUPLICATE_ASSET_CODE);
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "資産コードは既に使用されています。");
    }
  }

  private void logSuccess(LoginUserContext currentUser, String operation, Long targetId) {
    operationLogger.logSuccess(
        new OperationLogRecord(
            currentUser, operation, ASSET_TARGET_TYPE, targetId, null, false, null));
  }

  private void logRejected(
      LoginUserContext currentUser, String operation, Long targetId, String reasonCode) {
    operationLogger.logRejected(
        new OperationLogRecord(
            currentUser,
            operation,
            ASSET_TARGET_TYPE,
            targetId,
            reasonCode,
            false,
            EXCEPTION_RESPONSE_STATUS));
  }
}
