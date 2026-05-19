package com.example.workops.request.service;

import java.util.List;
import java.util.Objects;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.example.workops.common.security.CurrentUserProvider;
import com.example.workops.common.security.LoginUserContext;
import com.example.workops.common.security.PermissionSetCode;
import com.example.workops.request.mapper.RequestMapper;
import com.example.workops.request.model.RequestAssetOption;
import com.example.workops.request.model.RequestDetail;
import com.example.workops.request.model.RequestListItem;
import com.example.workops.request.model.RequestTypeOption;

/**
 * 申請管理の参照系ユースケースと画面操作可否を扱うService。
 *
 * <p>参照系の会社境界は、現在ユーザーの {@code companyId} をMapper条件へ渡して守る。
 * {@code can...} メソッドは画面表示用の補助判定であり、更新系の最終判定は
 * {@link RequestCommandService} 側で再度実施する。</p>
 */
@Service
public class RequestQueryService {

    private static final String DRAFT_STATUS_CODE = "DRAFT";
    private static final String SUBMITTED_STATUS_CODE = "SUBMITTED";

    private final CurrentUserProvider currentUserProvider;
    private final RequestMapper requestMapper;

    public RequestQueryService(CurrentUserProvider currentUserProvider, RequestMapper requestMapper) {
        this.currentUserProvider = currentUserProvider;
        this.requestMapper = requestMapper;
    }

    /**
     * 現在ユーザーの会社に属する申請一覧を取得する。
     *
     * @return 会社境界で絞り込まれた申請一覧
     */
    public List<RequestListItem> findList() {
        LoginUserContext currentUser = currentUserProvider.requireCurrentUser();
        return requestMapper.findListByCompanyId(currentUser.companyId());
    }

    /**
     * 現在ユーザーの会社に属する申請詳細を取得する。
     *
     * @param id 申請ID
     * @return 申請詳細
     * @throws ResponseStatusException 対象申請が存在しない、または他社申請の場合
     */
    public RequestDetail findDetail(Long id) {
        LoginUserContext currentUser = currentUserProvider.requireCurrentUser();
        return requestMapper.findDetailByIdAndCompanyId(id, currentUser.companyId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "申請が見つかりません。"));
    }

    /**
     * 申請作成・編集で選択できる未削除の申請種別を取得する。
     *
     * @return 現在ユーザーの会社に属する申請種別選択肢
     */
    public List<RequestTypeOption> findRequestTypeOptions() {
        LoginUserContext currentUser = currentUserProvider.requireCurrentUser();
        return requestMapper.findRequestTypeOptionsByCompanyId(currentUser.companyId());
    }

    /**
     * 申請作成・編集で任意に紐づけられる未削除資産を取得する。
     *
     * @return 現在ユーザーの会社に属する資産選択肢
     */
    public List<RequestAssetOption> findAssetOptions() {
        LoginUserContext currentUser = currentUserProvider.requireCurrentUser();
        return requestMapper.findAssetOptionsByCompanyId(currentUser.companyId());
    }

    /**
     * 現在ユーザーが申請下書きを作成できるか判定する。
     *
     * @return {@code TENANT_EDITOR} または {@code TENANT_MANAGER} を持つ場合はtrue
     */
    public boolean canCreateDraft() {
        return currentUserProvider.currentUser()
                .map(this::hasApplicantOperationPermission)
                .orElse(false);
    }

    /**
     * 申請詳細画面で編集操作を表示してよいか判定する。
     *
     * @param requestDetail 判定対象の申請
     * @return 申請者本人がDRAFT申請を編集できる場合はtrue
     */
    public boolean canEditDraft(RequestDetail requestDetail) {
        return currentUserProvider.currentUser()
                .map(currentUser -> hasApplicantOperationPermission(currentUser)
                        && Objects.equals(requestDetail.requesterUserId(), currentUser.userId())
                        && DRAFT_STATUS_CODE.equals(requestDetail.statusCode()))
                .orElse(false);
    }

    /**
     * 申請詳細画面で提出操作を表示してよいか判定する。
     *
     * @param requestDetail 判定対象の申請
     * @return 申請者本人がDRAFT申請を提出できる場合はtrue
     */
    public boolean canSubmit(RequestDetail requestDetail) {
        return currentUserProvider.currentUser()
                .map(currentUser -> hasApplicantOperationPermission(currentUser)
                        && Objects.equals(requestDetail.requesterUserId(), currentUser.userId())
                        && DRAFT_STATUS_CODE.equals(requestDetail.statusCode()))
                .orElse(false);
    }

    /**
     * 申請詳細画面で取下げ操作を表示してよいか判定する。
     *
     * @param requestDetail 判定対象の申請
     * @return 申請者本人がSUBMITTED申請を取下げできる場合はtrue
     */
    public boolean canWithdraw(RequestDetail requestDetail) {
        return currentUserProvider.currentUser()
                .map(currentUser -> hasApplicantOperationPermission(currentUser)
                        && Objects.equals(requestDetail.requesterUserId(), currentUser.userId())
                        && SUBMITTED_STATUS_CODE.equals(requestDetail.statusCode()))
                .orElse(false);
    }

    /**
     * 申請詳細画面でレビュー操作を表示してよいか判定する。
     *
     * @param requestDetail 判定対象の申請
     * @return {@code TENANT_MANAGER} がSUBMITTED申請をレビューできる場合はtrue
     */
    public boolean canReview(RequestDetail requestDetail) {
        return currentUserProvider.currentUser()
                .map(currentUser -> hasManagerPermission(currentUser)
                        && SUBMITTED_STATUS_CODE.equals(requestDetail.statusCode()))
                .orElse(false);
    }

    private boolean hasApplicantOperationPermission(LoginUserContext currentUser) {
        return currentUser.permissionSets()
                .stream()
                .anyMatch(permissionSet -> PermissionSetCode.TENANT_EDITOR.name().equals(permissionSet.code())
                        || PermissionSetCode.TENANT_MANAGER.name().equals(permissionSet.code()));
    }

    private boolean hasManagerPermission(LoginUserContext currentUser) {
        return currentUser.permissionSets()
                .stream()
                .anyMatch(permissionSet -> PermissionSetCode.TENANT_MANAGER.name().equals(permissionSet.code()));
    }
}
