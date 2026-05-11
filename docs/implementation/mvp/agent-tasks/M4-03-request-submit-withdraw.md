# M4-03 申請提出 / 取下げ

## 目的

申請者本人が DRAFT の申請を提出し、SUBMITTED の申請を取下げできるようにする。

## M4 共通方針

- SUBMITTED の取下げは申請者本人のみ許可する
- TENANT_MANAGER は、自分の申請について TENANT_EDITOR 相当の作成・編集・提出・取下げができる
- 再提出時も `review_comment` はクリアしない
- 操作可否は本人条件・権限・ステータスで制御する
- 会社境界は authority ではなく Mapper SQL の `company_id` 条件で守る
- M4 では資産 CRUD、資産ステータス変更、申請承認による資産ステータス自動変更には踏み込まない
- Mapper / Testcontainers 強化は M8 に寄せる

## 対応範囲

- 申請提出操作を実装する
- 申請取下げ操作を実装する
- 提出時は `DRAFT -> SUBMITTED` に遷移する
- 提出時は `submitted_at` に現在日時を設定する
- 提出時は `updated_by` に現在ユーザーの `userId` を設定する
- 提出時は `review_comment` を更新しない
- 再提出時も既存の `review_comment` をクリアしない
- 取下げ時は `SUBMITTED -> WITHDRAWN` に遷移する
- 取下げ時は `updated_by` に現在ユーザーの `userId` を設定する
- 提出は申請者本人かつ `status_code = 'DRAFT'` の申請だけ許可する
- 取下げは申請者本人かつ `status_code = 'SUBMITTED'` の申請だけ許可する
- TENANT_EDITOR / TENANT_MANAGER は自分の申請を提出・取下げできる
- TENANT_VIEWER は提出・取下げできない
- Mapper SQL の更新では `id + company_id` を条件にする
- 操作後は申請詳細へリダイレクトする
- 操作不可の場合は Spring Security 標準の `AccessDeniedException` または業務上の状態不正例外で拒否する

## 対応ファイル

- `apps/web/src/main/java/com/example/workops/request/web/RequestController.java`
- `apps/web/src/main/java/com/example/workops/request/service/RequestCommandService.java`
- `apps/web/src/main/java/com/example/workops/request/mapper/RequestMapper.java`
- `apps/web/src/main/java/com/example/workops/request/model/RequestDetail.java`
- `apps/web/src/main/resources/mapper/request/RequestMapper.xml`
- `apps/web/src/main/resources/templates/request/detail.html`
- `docs/implementation/mvp/agent-tasks/M4-03-request-submit-withdraw.md`

## 除外範囲

- 申請作成
- 申請編集
- 承認
- 却下
- 差戻し
- 申請履歴テーブル
- 監査ログテーブル
- 資産 CRUD
- 資産ステータス変更
- 申請承認による資産ステータス自動変更
- Mapper / Testcontainers 強化

## 完了条件

申請者本人が DRAFT の申請を SUBMITTED に提出できる。
申請者本人が SUBMITTED の申請を WITHDRAWN に取下げできる。
TENANT_MANAGER は自分の申請について提出・取下げできる。
TENANT_VIEWER、他人の申請、ステータス不一致の申請は提出・取下げできない。

## 確認方法

- `cd apps/web && .\mvnw.cmd test`
- local profile の TENANT_EDITOR で自分の DRAFT 申請を提出できることを確認する
- local profile の TENANT_MANAGER で自分の DRAFT 申請を提出できることを確認する
- 自分の SUBMITTED 申請を取下げできることを確認する
- 再提出時に `review_comment` がクリアされないことを確認する
- TENANT_VIEWER が提出・取下げできないことを確認する
- 他人の申請を提出・取下げできないことを確認する
- DRAFT 以外の申請を提出できないことを確認する
- SUBMITTED 以外の申請を取下げできないことを確認する

## 実装時の記録

### 実装方針

- 提出・取下げは `RequestCommandService` に集約し、現在ユーザーは `CurrentUserProvider.requireCurrentUser()` から取得する。
- Controller の提出・取下げ POST は `TENANT_EDITOR` / `TENANT_MANAGER` のみ許可する。
- 申請は `id + company_id` で取得し、申請者本人かつ対象ステータスの場合だけ状態遷移できる。
- 業務操作ごとの前提条件は `assertSubmittableDraft` / `assertWithdrawableSubmitted` のように操作名が分かるメソッドで確認する。
- MVP では Service の事前取得・業務チェックを正とし、更新件数による競合対策は入れない。
- 他社または存在しない申請は `ResponseStatusException(HttpStatus.NOT_FOUND)`、他人またはステータス不一致は `AccessDeniedException` とする。
- 提出時は `DRAFT -> SUBMITTED`、取下げ時は `SUBMITTED -> WITHDRAWN` とする。
- 提出時は `submitted_at` を現在日時に更新し、`review_comment` は更新しない。
- 取下げ時は `submitted_at` と `review_comment` を更新しない。
- 操作完了通知は、SSR の flash message を Bootstrap Toast として表示する。

### 変更ファイル

- `apps/web/src/main/java/com/example/workops/request/web/RequestController.java`
- `apps/web/src/main/java/com/example/workops/request/service/RequestCommandService.java`
- `apps/web/src/main/java/com/example/workops/request/service/RequestQueryService.java`
- `apps/web/src/main/java/com/example/workops/request/mapper/RequestMapper.java`
- `apps/web/src/main/resources/mapper/request/RequestMapper.xml`
- `apps/web/src/main/resources/templates/request/detail.html`
- `docs/implementation/mvp/agent-tasks/M4-01-request-list-detail.md`
- `docs/implementation/mvp/agent-tasks/M4-03-request-submit-withdraw.md`

### 実装結果

- `POST /requests/{id}/submit` を追加した。
- `POST /requests/{id}/withdraw` を追加した。
- 提出時は Service で現在日時を作り、`status_code = 'SUBMITTED'`、`submitted_at = submittedAt`、`updated_by = currentUser.userId` に更新する。
- 取下げ時は `status_code = 'WITHDRAWN'`、`updated_by = currentUser.userId` に更新する。
- Mapper SQL の提出・取下げ更新では、`id`、`company_id`、`requester_user_id`、現在ステータスを条件にした。
- 詳細画面に、操作可能な場合だけ「提出」または「取下げ」ボタンを表示するようにした。
- `RequestQueryService` に `canSubmit` / `canWithdraw` を追加し、詳細画面の表示制御に使った。
- M4-01 の実装時記録に残っていたテンプレ文を削除し、M4-02 と同じ体裁に揃えた。

### 確認結果

- `cd apps/web && .\mvnw.cmd test` 成功。
- local profile の画面操作確認は、ユーザー操作で確認するため未実施。

### 残課題

- 承認、却下、差戻しは M4-04 で扱う。
- Service 状態遷移テストは M4-05 で扱う。
- Mapper / Testcontainers 強化は M8 で扱う。
