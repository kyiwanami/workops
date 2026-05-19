# M10-05 ユーザー編集 / 権限割当変更

## 目的

作成済みユーザーの表示名、email、所属部署、権限セットを変更できるようにする。
`actor_type` と `company_id` は作成後変更不可とし、TENANT_MANAGER 数が 0 になる権限変更は更新後 count による業務バリデーションで拒否する。

## M10 共通方針

- M10 の範囲は、初期 TENANT_MANAGER 作成、ユーザー作成、権限割当・変更、local Cognito Fake Bean、疑似 `cognito_sub` 発行・登録に限定する
- M10 では、AWS SDK for Java 2.x による Cognito `AdminCreateUser` 呼び出し境界まで扱う
- M10 で扱う実 Cognito API は `AdminCreateUser` だけに限定する
- Cognito Hosted UI 本格確認、PLATFORM / TENANT App Client 分離、Cognito Trigger、Pre Token Generation、初回ログイン時の WorkOps ユーザー自動作成は Phase 2 で扱う
- M9 では会社・部署管理を扱い、M10 で会社作成フローへ初期 TENANT_MANAGER 作成を後付け連携する
- 最終仕様として、会社作成時に初期 TENANT_MANAGER を必ず 1 人作る
- ユーザー作成後に編集できる範囲は、表示名、email、所属部署、権限セットだけとする
- `actor_type` と `company_id` は作成後変更不可とする
- TENANT_MANAGER 数が 0 になる権限変更は、認可ではなく、更新後 count による業務バリデーションで拒否する
- 削除済み部署に所属するユーザーは、ユーザー一覧・詳細で `部署名（削除済み）` と表示する
- Git commit は実行しない

## 対応範囲

- ユーザー編集画面を作成する
- ユーザー編集処理を作成する
- 編集可能項目は表示名、email、所属部署、権限セットだけとする
- `actor_type`、`company_id`、`cognito_sub`、username は更新対象にしない
- PLATFORM_ADMIN は PLATFORM ユーザーと全テナントの TENANT ユーザーを編集できる
- PLATFORM_ADMIN は `PLATFORM_ADMIN`、`TENANT_MANAGER`、`TENANT_EDITOR`、`TENANT_VIEWER` を許可範囲内で割当・変更できる
- TENANT_MANAGER は自社 TENANT ユーザーだけ編集できる
- TENANT_MANAGER は `TENANT_VIEWER`、`TENANT_EDITOR`、`TENANT_MANAGER` だけ割当・変更できる
- TENANT_MANAGER は PLATFORM ユーザー、他社ユーザー、`PLATFORM_ADMIN` を扱えない
- 所属部署の選択肢には対象会社の未削除部署だけを表示する
- 所属部署は任意とする
- 削除済み部署 ID が POST された場合は画面入力エラーとして拒否する
- 他社部署 ID が POST された場合は画面入力エラーとして拒否する
- 対象会社の TENANT_MANAGER 数が 0 になる権限変更は、更新後 count による業務バリデーションで拒否する
- TENANT_MANAGER 数 0 件化チェックは認可ではなく、会社内管理者を 0 人にしないための業務整合性チェックとして実装する
- 権限セット変更は、対象ユーザーの既存割当を許可範囲内の新しい割当へ置き換える

## 対応ファイル

- `apps/web/src/main/java/com/example/workops/admin/user/web/UserAdminController.java`
- `apps/web/src/main/java/com/example/workops/admin/user/form/UserEditForm.java`
- `apps/web/src/main/java/com/example/workops/admin/user/service/UserAdminService.java`
- `apps/web/src/main/java/com/example/workops/admin/user/mapper/UserAdminMapper.java`
- `apps/web/src/main/java/com/example/workops/admin/user/model/*.java`
- `apps/web/src/main/resources/mapper/admin/user/UserAdminMapper.xml`
- `apps/web/src/main/resources/templates/admin/user/user-edit.html`
- `apps/web/src/test/java/com/example/workops/admin/user/**/*.java`
- `docs/implementation/mvp/agent-tasks/M10/M10-05-user-edit-permission-change.md`

## 除外範囲

- `actor_type` 変更
- `company_id` 変更
- `cognito_sub` 変更
- username 変更
- Cognito username 変更
- Cognito 側ユーザー属性変更
- ユーザー無効化
- 権限セット定義そのものの CRUD
- Cognito `AdminCreateUser` 以外の本物 API 呼び出し

## 完了条件

許可された範囲で表示名、email、所属部署、権限セットを変更できる。
`actor_type` と `company_id` は変更されない。
PLATFORM_ADMIN は許可された範囲で PLATFORM ユーザーと全テナントの TENANT ユーザーを変更できる。
TENANT_MANAGER は自社 TENANT ユーザーだけ変更できる。
TENANT_MANAGER は他社ユーザー、PLATFORM ユーザー、`PLATFORM_ADMIN` を扱えない。
対象会社の TENANT_MANAGER 数が 0 になる権限変更は拒否される。

## 確認方法

- `cd apps/web && .\mvnw.cmd test`
- local profile の PLATFORM_ADMIN で PLATFORM ユーザーと TENANT ユーザーを編集できることを確認する
- local profile の TENANT_MANAGER で自社 TENANT ユーザーを編集できることを確認する
- TENANT_MANAGER が他社ユーザー、PLATFORM ユーザー、`PLATFORM_ADMIN` を扱えないことを確認する
- `actor_type` と `company_id` が編集処理で変更されないことを確認する
- TENANT_MANAGER 数が 0 になる権限変更が拒否されることを確認する

## 実装時の記録

### 実装方針

- 編集用 POST は `name`、`email`、`departmentId`、`permissionSetCodes` だけを受け付ける
- `username`、`actor_type`、`company_id`、`cognito_sub` は編集画面で表示専用にし、更新処理では DB 上の対象ユーザーから判定する
- PLATFORM_ADMIN は `/admin/users/{userId}/edit`、TENANT_MANAGER は `/users/{userId}/edit` を使う
- TENANT_MANAGER の他社ユーザーおよび PLATFORM ユーザー編集は存在秘匿のため `404 NOT_FOUND` にする
- TENANT_MANAGER 数 0 件化は権限チェックではなく、権限置き換え後の count による業務バリデーションで `400 BAD_REQUEST` にする
- Cognito 側の email / username / 属性変更は行わない

### 実装結果

- `UserEditForm` と `UserEditTarget` を追加した
- `UserAdminController` に PLATFORM_ADMIN / TENANT_MANAGER 向け編集 GET / POST を追加した
- `UserAdminService` に編集フォーム取得、編集対象取得、更新処理、email 重複確認、TENANT_MANAGER 数確認を追加した
- `UserAdminMapper` と `UserAdminMapper.xml` に編集対象取得、権限コード取得、email 重複確認、ユーザー更新、権限置き換え、TENANT_MANAGER count を追加した
- `admin/user/user-edit.html` を追加し、詳細画面に編集ボタンを追加した
- Service テストと Mapper 統合テストへ M10-05 の確認観点を追加した

### 確認結果

- `cd apps/web && .\mvnw.cmd test`
  - Tests run: 174
  - Failures: 0
  - Errors: 0
  - Skipped: 0
- local profile 起動後、`http://localhost:8081/users/3/edit` が `200` を返し、ユーザー編集画面、`TENANT_MANAGER / 管理者`、`cognito_sub` が描画されることを確認した
- Chrome DevTools MCP はページ取得時に timeout したため、local 画面確認は `Invoke-WebRequest` で HTML 応答を確認した

### 残課題

- 実 Cognito 環境での E2E はユーザーが手動で実施する
- PLATFORM_ADMIN での画面操作確認は M10-06 で確認済み
