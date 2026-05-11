# M4-04 承認 / 却下 / 差戻し

## 目的

TENANT_MANAGER が SUBMITTED の申請を承認・却下・差戻しできるようにする。

## M4 共通方針

- TENANT_MANAGER は、自分の申請も承認・却下・差戻しできる
- 差戻しは `SUBMITTED -> DRAFT` とする
- 却下理由・差戻し理由は同じ `review_comment` に入れる
- 再提出時も `review_comment` はクリアしない
- 操作可否は本人条件・権限・ステータスで制御する
- 会社境界は authority ではなく Mapper SQL の `company_id` 条件で守る
- M4 では資産 CRUD、資産ステータス変更、申請承認による資産ステータス自動変更には踏み込まない
- Mapper / Testcontainers 強化は M8 に寄せる

## 対応範囲

- 承認操作を実装する
- 却下操作を実装する
- 差戻し操作を実装する
- 承認時は `SUBMITTED -> APPROVED` に遷移する
- 却下時は `SUBMITTED -> REJECTED` に遷移する
- 差戻し時は `SUBMITTED -> DRAFT` に遷移する
- 却下時は入力された理由を `review_comment` に保存する
- 差戻し時は入力された理由を `review_comment` に保存する
- 承認時は `review_comment` を更新しない
- 承認・却下・差戻し時は `updated_by` に現在ユーザーの `userId` を設定する
- 承認・却下・差戻しは TENANT_MANAGER のみ許可する
- TENANT_MANAGER は自分の申請も承認・却下・差戻しできる
- 承認・却下・差戻しは `status_code = 'SUBMITTED'` の申請だけ許可する
- TENANT_VIEWER / TENANT_EDITOR は承認・却下・差戻しできない
- Mapper SQL の更新では `id + company_id` を条件にする
- 操作後は申請詳細へリダイレクトする
- 操作不可の場合は Spring Security 標準の `AccessDeniedException` または業務上の状態不正例外で拒否する

## 対応ファイル

- `apps/web/src/main/java/com/example/workops/request/web/RequestController.java`
- `apps/web/src/main/java/com/example/workops/request/form/RequestReviewForm.java`
- `apps/web/src/main/java/com/example/workops/request/service/RequestCommandService.java`
- `apps/web/src/main/java/com/example/workops/request/mapper/RequestMapper.java`
- `apps/web/src/main/java/com/example/workops/request/model/RequestDetail.java`
- `apps/web/src/main/resources/mapper/request/RequestMapper.xml`
- `apps/web/src/main/resources/templates/request/detail.html`
- `apps/web/src/main/resources/templates/request/review-form.html`
- `docs/implementation/mvp/agent-tasks/M4-04-request-review-actions.md`

## 除外範囲

- 申請作成
- 申請編集
- 申請提出
- 申請取下げ
- 複雑な承認ルート設定
- 外部承認連携
- メール通知
- 申請履歴テーブル
- 監査ログテーブル
- 資産 CRUD
- 資産ステータス変更
- 申請承認による資産ステータス自動変更
- Mapper / Testcontainers 強化

## 完了条件

TENANT_MANAGER が SUBMITTED の申請を APPROVED / REJECTED / DRAFT に遷移できる。
TENANT_MANAGER は自分の申請も承認・却下・差戻しできる。
却下理由・差戻し理由は `review_comment` に保存される。
TENANT_VIEWER / TENANT_EDITOR、ステータス不一致の申請、他社申請は承認・却下・差戻しできない。

## 確認方法

- `cd apps/web && .\mvnw.cmd test`
- local profile の TENANT_MANAGER で SUBMITTED 申請を承認できることを確認する
- local profile の TENANT_MANAGER で SUBMITTED 申請を却下できることを確認する
- local profile の TENANT_MANAGER で SUBMITTED 申請を差戻しできることを確認する
- TENANT_MANAGER が自分の SUBMITTED 申請を承認・却下・差戻しできることを確認する
- 却下理由・差戻し理由が `review_comment` として詳細画面に表示されることを確認する
- TENANT_VIEWER / TENANT_EDITOR が承認・却下・差戻しできないことを確認する
- SUBMITTED 以外の申請を承認・却下・差戻しできないことを確認する

## 実装時の記録

### 実装方針

- 承認・却下・差戻しは `RequestCommandService` に集約し、現在ユーザーは `CurrentUserProvider.requireCurrentUser()` から取得する。
- Controller の承認・却下・差戻し系ルートは `TENANT_MANAGER` のみ許可する。
- 申請は `id + company_id` で取得し、`SUBMITTED` の場合だけ承認・却下・差戻しできる。
- TENANT_MANAGER は自分の申請も承認・却下・差戻しできるため、申請者本人かどうかでは拒否しない。
- 他社または存在しない申請は `ResponseStatusException(HttpStatus.NOT_FOUND)`、`SUBMITTED` 以外は `AccessDeniedException` とする。
- 業務操作ごとの前提条件は `assertApprovableSubmitted` / `assertRejectableSubmitted` / `assertRemandableSubmitted` のように操作名が分かるメソッドで確認する。
- MVP では Service の事前取得・業務チェックを正とし、更新件数による競合対策は入れない。
- 却下理由・差戻し理由は同じ `review_comment` に保存する。
- 承認時は `review_comment` を更新しない。
- 操作完了通知は、SSR の flash message を Bootstrap Toast として表示する。

### 変更ファイル

- `apps/web/src/main/java/com/example/workops/request/web/RequestController.java`
- `apps/web/src/main/java/com/example/workops/request/form/RequestReviewForm.java`
- `apps/web/src/main/java/com/example/workops/request/service/RequestCommandService.java`
- `apps/web/src/main/java/com/example/workops/request/service/RequestQueryService.java`
- `apps/web/src/main/java/com/example/workops/request/mapper/RequestMapper.java`
- `apps/web/src/main/resources/mapper/request/RequestMapper.xml`
- `apps/web/src/main/resources/templates/request/detail.html`
- `apps/web/src/main/resources/templates/request/review-form.html`
- `docs/implementation/mvp/agent-tasks/M4-04-request-review-actions.md`

### 実装結果

- `POST /requests/{id}/approve` を追加した。
- `GET /requests/{id}/reject`、`POST /requests/{id}/reject` を追加した。
- `GET /requests/{id}/remand`、`POST /requests/{id}/remand` を追加した。
- 承認時は `status_code = 'APPROVED'`、`updated_by = currentUser.userId` に更新する。
- 却下時は `status_code = 'REJECTED'`、`review_comment = form.reviewComment`、`updated_by = currentUser.userId` に更新する。
- 差戻し時は `status_code = 'DRAFT'`、`review_comment = form.reviewComment`、`updated_by = currentUser.userId` に更新する。
- Mapper SQL の承認・却下・差戻し更新では、`id`、`company_id`、`status_code = 'SUBMITTED'` を条件にした。
- `RequestReviewForm` を追加し、`reviewComment` を必須・1000文字以内にした。
- `RequestReviewForm` は record とし、手書き getter / setter は作らない。
- 却下・差戻し理由入力用の `request/review-form.html` を追加した。
- 詳細画面に、`canReview` が true の場合だけ「承認」「却下」「差戻し」を表示するようにした。
- `RequestQueryService` に `canReview` を追加し、詳細画面の表示制御に使った。

### 確認結果

- `cd apps/web && .\mvnw.cmd test` 成功。
- local profile の画面操作確認は、ユーザー操作で確認するため未実施。

### 残課題

- Service 状態遷移テストは M4-05 で扱う。
- Mapper / Testcontainers 強化は M8 で扱う。
