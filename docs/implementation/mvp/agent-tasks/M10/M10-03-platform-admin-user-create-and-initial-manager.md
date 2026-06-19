# M10-03 PLATFORM_ADMIN ユーザー作成 / 会社作成時初期 TENANT_MANAGER 連携

## 目的

PLATFORM_ADMIN が PLATFORM ユーザーと任意会社の TENANT ユーザーを作成できるようにする。
また、M9 で作成済みの会社作成フローへ初期 TENANT_MANAGER 作成を後付け連携し、最終仕様として会社作成時に初期 TENANT_MANAGER を必ず 1 人作る。

## M10 共通方針

- M10 の範囲は、初期 TENANT_MANAGER 作成、ユーザー作成、権限割当・変更、local Cognito Fake Bean、疑似 `cognito_sub` 発行・登録に限定する
- M10 は local Fake Bean に加えて AWS SDK for Java 2.x による Cognito `AdminCreateUser` 呼び出し境界を扱う
- M10 で扱う実 Cognito API は `AdminCreateUser` のみに限定する
- Cognito Hosted UI 本格確認、PLATFORM / TENANT App Client 分離、Cognito Trigger、Pre Token Generation、初回ログイン時の WorkOps ユーザー自動作成は Phase 2 で扱う
- M9 では会社・部署管理を扱い、M10 で会社作成フローへ初期 TENANT_MANAGER 作成を後付け連携する
- 最終仕様として、会社作成時に初期 TENANT_MANAGER を必ず 1 人作る
- ユーザー作成後に編集できる範囲は、表示名、email、所属部署、権限セットだけとする
- `actor_type` と `company_id` は作成後変更不可とする
- TENANT_MANAGER 数が 0 になる権限変更は、認可ではなく、更新後 count による業務バリデーションで拒否する
- 削除済み部署に所属するユーザーは、ユーザー一覧・詳細で `部署名（削除済み）` と表示する

## 対応範囲

- PLATFORM_ADMIN による PLATFORM ユーザー作成を作成する
- PLATFORM_ADMIN による任意会社の TENANT ユーザー作成を作成する
- 会社作成画面へ初期 TENANT_MANAGER 入力欄を追加する
- 会社作成時の入力項目として、会社コード、会社名、初期 TENANT_MANAGER の username、表示名、email、所属部署を扱う
- 初期 TENANT_MANAGER の所属部署は任意とし、作成会社内の部署は会社作成時点では存在しないため未設定で登録する
- 会社作成トランザクション内で、会社作成、会社別初期マスタ投入、初期 TENANT_MANAGER 作成、TENANT_MANAGER 権限割当を完了する
- 会社作成時に初期 TENANT_MANAGER 作成が失敗した場合、会社作成全体を完了させない
- 作成ユーザーには M10-01 の `CognitoUserProvisioner` が返す `cognito_sub` を登録する
- local profile では Fake Bean の疑似 `cognito_sub` を登録する
- local なしでは Cognito `AdminCreateUser` の response から取り出した `sub` を登録する
- PLATFORM ユーザーは `actor_type = 'PLATFORM'`、`company_id = NULL` で登録する
- TENANT ユーザーは `actor_type = 'TENANT'`、対象会社の `company_id` で登録する
- PLATFORM_ADMIN は PLATFORM ユーザーに `PLATFORM_ADMIN` を割り当てできる
- PLATFORM_ADMIN は TENANT ユーザーに `TENANT_VIEWER`、`TENANT_EDITOR`、`TENANT_MANAGER` を割り当てできる
- Controller と Service の public メソッドには `@PreAuthorize("hasAuthority('PLATFORM_ADMIN')")` を付ける

## 対応ファイル

- `apps/web/src/main/java/com/example/workops/admin/company/form/CompanyForm.java`
- `apps/web/src/main/java/com/example/workops/admin/company/service/CompanyAdminService.java`
- `apps/web/src/main/java/com/example/workops/admin/company/web/CompanyAdminController.java`
- `apps/web/src/main/java/com/example/workops/admin/user/form/UserForm.java`
- `apps/web/src/main/java/com/example/workops/admin/user/service/UserAdminService.java`
- `apps/web/src/main/java/com/example/workops/admin/user/mapper/UserAdminMapper.java`
- `apps/web/src/main/java/com/example/workops/admin/user/model/*.java`
- `apps/web/src/main/resources/mapper/admin/company/CompanyAdminMapper.xml`
- `apps/web/src/main/resources/mapper/admin/user/UserAdminMapper.xml`
- `apps/web/src/main/resources/templates/admin/company/company-form.html`
- `apps/web/src/main/resources/templates/admin/user/user-form.html`
- `apps/web/src/test/java/com/example/workops/admin/company/**/*.java`
- `apps/web/src/test/java/com/example/workops/admin/user/**/*.java`
- `docs/implementation/mvp/agent-tasks/M10/M10-03-platform-admin-user-create-and-initial-manager.md`

## 除外範囲

- TENANT_MANAGER による自社ユーザー作成
- ユーザー作成後の編集
- 権限変更単独画面
- ユーザー無効化
- Cognito Hosted UI 本格確認
- PLATFORM / TENANT App Client 分離
- Cognito Trigger
- Pre Token Generation
- 初回ログイン時の WorkOps ユーザー自動作成
- Cognito 側ユーザー属性変更

## 完了条件

PLATFORM_ADMIN が PLATFORM ユーザーを作成できる。
PLATFORM_ADMIN が任意会社の TENANT ユーザーを作成できる。
会社作成時に初期 TENANT_MANAGER が必ず 1 人作成される。
会社作成時に、会社、会社別初期マスタ、初期 TENANT_MANAGER、TENANT_MANAGER 権限割当が同一トランザクションで成立する。
会社作成時に初期 TENANT_MANAGER 作成が失敗した場合、会社作成全体が完了しない。
作成ユーザーには `CognitoUserProvisioner` が返した `cognito_sub` が登録される。

## 確認方法

- `cd apps/web && .\mvnw.cmd test`
- local profile の PLATFORM_ADMIN で会社作成画面が初期 TENANT_MANAGER 入力欄を表示することを確認する
- local profile の PLATFORM_ADMIN で会社を作成し、会社、会社別初期マスタ、初期 TENANT_MANAGER、TENANT_MANAGER 権限割当が登録されることを確認する
- local profile の PLATFORM_ADMIN で PLATFORM ユーザーを作成できることを確認する
- local profile の PLATFORM_ADMIN で任意会社の TENANT ユーザーを作成できることを確認する
- 作成ユーザーに `CognitoUserProvisioner` が返した `cognito_sub` が登録されることを確認する

## 実装時の記録

### 実装方針

- PLATFORM_ADMIN のみが `/admin/users/new` と `/admin/users` でユーザー作成できる
- ユーザー作成は M10-01 の `CognitoUserProvisioner` を必ず通し、返却 `cognito_sub` を `users.cognito_sub` に登録する
- Cognito 作成前に actor_type、会社、部署、権限セット、username/email 重複を検証する
- Cognito 作成に失敗した場合は DB 登録へ進まない
- DB 登録に失敗した場合の Cognito 側補償削除は M10 では実装しない
- 会社作成では、会社、会社別初期マスタ、初期 TENANT_MANAGER、TENANT_MANAGER 権限割当を同一トランザクションで実行する
- 実 Cognito の `AdminCreateUser` 送信を伴う E2E はユーザーが手動で行う

### 変更ファイル

- `apps/web/src/main/java/com/example/workops/admin/company/form/CompanyForm.java`
- `apps/web/src/main/java/com/example/workops/admin/company/service/CompanyAdminService.java`
- `apps/web/src/main/java/com/example/workops/admin/company/web/CompanyAdminController.java`
- `apps/web/src/main/java/com/example/workops/admin/user/form/UserForm.java`
- `apps/web/src/main/java/com/example/workops/admin/user/mapper/UserAdminMapper.java`
- `apps/web/src/main/java/com/example/workops/admin/user/model/CompanySelectOption.java`
- `apps/web/src/main/java/com/example/workops/admin/user/model/DepartmentSelectOption.java`
- `apps/web/src/main/java/com/example/workops/admin/user/model/PermissionSetOption.java`
- `apps/web/src/main/java/com/example/workops/admin/user/model/UserCreationPage.java`
- `apps/web/src/main/java/com/example/workops/admin/user/service/UserAdminService.java`
- `apps/web/src/main/java/com/example/workops/admin/user/web/UserAdminController.java`
- `apps/web/src/main/resources/mapper/admin/user/UserAdminMapper.xml`
- `apps/web/src/main/resources/templates/admin/company/company-form.html`
- `apps/web/src/main/resources/templates/admin/user/user-form.html`
- `apps/web/src/main/resources/templates/admin/user/user-list.html`
- `apps/web/src/test/java/com/example/workops/admin/company/service/CompanyAdminServiceTests.java`
- `apps/web/src/test/java/com/example/workops/admin/user/service/UserAdminServiceTests.java`
- `apps/web/src/test/java/com/example/workops/integration/UserAdminMapperIntegrationTests.java`
- `docs/implementation/mvp/agent-tasks/M10/M10-03-platform-admin-user-create-and-initial-manager.md`

### 実装結果

- PLATFORM_ADMIN 用のユーザー作成画面と POST 処理を追加した
- PLATFORM ユーザーは `company_id = NULL`、`department_id = NULL`、`PLATFORM_ADMIN` 権限のみで作成する
- TENANT ユーザーは active company 必須、active department 任意、TENANT 権限セットのみで作成する
- username/email の重複は PLATFORM または会社スコープで事前検証する
- ユーザー作成時に `CognitoUserProvisioner` を呼び、返却 `cognito_sub` を `users` に登録する
- ユーザー登録と権限セット登録を同一 DB トランザクションで実行する
- 会社作成フォームに初期 TENANT_MANAGER の username、表示名、email を追加した
- 会社作成時に初期 TENANT_MANAGER を `department_id = NULL`、`TENANT_MANAGER` 権限で作成する
- `/admin/users` に新規作成ボタンを追加した

### 確認結果

- `cd apps/web && .\mvnw.cmd test`
  - `Tests run: 159, Failures: 0, Errors: 0, Skipped: 0`
- Service テストで PLATFORM ユーザー作成、TENANT ユーザー作成、初期 TENANT_MANAGER 作成、権限セット不整合、他社部署指定、username/email 重複、Cognito 作成失敗時の DB 未登録を確認した
- Company Service テストで、会社作成時に初期 TENANT_MANAGER 作成が呼ばれること、初期 TENANT_MANAGER 作成失敗時に会社作成成功ログへ進まないことを確認した
- Mapper 統合テストで users insert、user_permission_sets insert、権限セット lookup、PLATFORM / TENANT スコープの username/email 重複検出を確認した

### 残課題

- TENANT_MANAGER による自社ユーザー作成は M10-04 で対応済み
- ユーザー編集と権限変更は M10-05 で対応済み
- ユーザー無効化・削除は M10 範囲外
- 実 Cognito の `AdminCreateUser` を伴う画面 E2E はユーザー確認対象
- Hosted UI 本格確認、PLATFORM / TENANT App Client 分離、Cognito Trigger、Pre Token Generation、初回ログイン時の WorkOps ユーザー自動作成は Phase 2 で扱う
