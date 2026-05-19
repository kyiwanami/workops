# M10-04 TENANT_MANAGER 自社ユーザー作成

## 目的

TENANT_MANAGER が所属会社内の TENANT ユーザーを作成できるようにする。
TENANT_MANAGER は PLATFORM ユーザー、他社ユーザー、PLATFORM_ADMIN 権限を扱わない。

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
- Git commit は実行しない

## 対応範囲

- TENANT_MANAGER 向け自社ユーザー作成画面を作成する
- TENANT_MANAGER 向け自社ユーザー作成処理を作成する
- TENANT_MANAGER は現在ユーザーの `companyId` と一致する TENANT ユーザーだけを作成できる
- 作成ユーザーは `actor_type = 'TENANT'`、現在ユーザーの `companyId` で登録する
- 作成ユーザーには M10-01 の `CognitoUserProvisioner` が返す `cognito_sub` を登録する
- local profile では Fake Bean の疑似 `cognito_sub` を登録する
- local なしでは Cognito `AdminCreateUser` の response から取り出した `sub` を登録する
- TENANT_MANAGER は `TENANT_VIEWER`、`TENANT_EDITOR`、`TENANT_MANAGER` だけ割り当てできる
- TENANT_MANAGER は `PLATFORM_ADMIN` を割り当てできない
- 所属部署は任意とする
- 所属部署の選択肢には、自社の未削除部署だけを表示する
- 削除済み部署 ID が POST された場合は画面入力エラーとして拒否する
- 他社部署 ID が POST された場合は画面入力エラーとして拒否する
- Controller と Service の public メソッドには `@PreAuthorize("hasAuthority('TENANT_MANAGER')")` を付ける

## 対応ファイル

- `apps/web/src/main/java/com/example/workops/admin/user/web/UserAdminController.java`
- `apps/web/src/main/java/com/example/workops/admin/user/form/UserForm.java`
- `apps/web/src/main/java/com/example/workops/admin/user/service/UserAdminService.java`
- `apps/web/src/main/java/com/example/workops/admin/user/mapper/UserAdminMapper.java`
- `apps/web/src/main/java/com/example/workops/admin/user/model/*.java`
- `apps/web/src/main/resources/mapper/admin/user/UserAdminMapper.xml`
- `apps/web/src/main/resources/templates/admin/user/user-form.html`
- `apps/web/src/test/java/com/example/workops/admin/user/**/*.java`
- `docs/implementation/mvp/agent-tasks/M10/M10-04-tenant-manager-user-create.md`

## 除外範囲

- PLATFORM ユーザー作成
- 他社 TENANT ユーザー作成
- `PLATFORM_ADMIN` 権限割当
- ユーザー作成後の編集
- 権限変更単独画面
- ユーザー無効化
- `AdminCreateUser` 以外の Cognito 本物 API 呼び出し
- 実 Cognito の画面 E2E 実施

## 完了条件

TENANT_MANAGER が自社 TENANT ユーザーを作成できる。
TENANT_MANAGER が他社 TENANT ユーザーを作成できない。
TENANT_MANAGER が PLATFORM ユーザーを作成できない。
TENANT_MANAGER が `PLATFORM_ADMIN` を割り当てできない。
削除済み部署または他社部署を POST した場合は拒否される。
作成ユーザーには `CognitoUserProvisioner` が返した `cognito_sub` が登録される。

## 確認方法

- `cd apps/web && .\mvnw.cmd test`
- local profile の TENANT_MANAGER で自社ユーザーを作成できることを確認する
- local profile の TENANT_MANAGER で `TENANT_VIEWER`、`TENANT_EDITOR`、`TENANT_MANAGER` を割り当てできることを確認する
- TENANT_MANAGER による `PLATFORM_ADMIN` 割当が拒否されることを確認する
- TENANT_MANAGER による他社部署・削除済み部署の指定が拒否されることを確認する
- 作成ユーザーに `CognitoUserProvisioner` が返した `cognito_sub` が登録されることを確認する

## 実装時の記録

### 実装方針

- TENANT_MANAGER 用に `/users/new` と `/users` を追加する
- TENANT_MANAGER の作成処理ではフォームの `actorType` と `companyId` を使わず、現在ユーザーの `companyId` と `actor_type = 'TENANT'` で固定する
- TENANT_MANAGER 用フォームでは会社、actor_type、PLATFORM 権限セットを表示しない
- 所属部署は現在ユーザーの会社に属する未削除部署だけを選択肢にする
- 権限セットは `TENANT_VIEWER`、`TENANT_EDITOR`、`TENANT_MANAGER` だけを DB から取得して表示する
- ユーザー作成は M10-01 の `CognitoUserProvisioner` を必ず通し、返却 `cognito_sub` を `users.cognito_sub` に登録する
- 実 Cognito の `AdminCreateUser` 送信を伴う E2E はユーザーが手動で行う

### 変更ファイル

- `apps/web/src/main/java/com/example/workops/admin/user/mapper/UserAdminMapper.java`
- `apps/web/src/main/java/com/example/workops/admin/user/service/UserAdminService.java`
- `apps/web/src/main/java/com/example/workops/admin/user/web/UserAdminController.java`
- `apps/web/src/main/resources/mapper/admin/user/UserAdminMapper.xml`
- `apps/web/src/main/resources/templates/admin/user/user-form.html`
- `apps/web/src/main/resources/templates/admin/user/user-list.html`
- `apps/web/src/test/java/com/example/workops/admin/user/service/UserAdminServiceTests.java`
- `apps/web/src/test/java/com/example/workops/integration/UserAdminMapperIntegrationTests.java`
- `docs/implementation/mvp/agent-tasks/M10/M10-04-tenant-manager-user-create.md`

### 実装結果

- TENANT_MANAGER 用のユーザー作成画面と POST 処理を追加した
- TENANT_MANAGER のユーザー作成では `actor_type = 'TENANT'`、`company_id = currentUser.companyId()` で登録する
- TENANT_MANAGER 用フォームでは会社、actor_type、PLATFORM 権限セットを非表示にした
- `/users` に新規作成ボタンを追加した
- 自社未削除部署だけを取得する `findActiveDepartmentsByCompanyId` を追加した
- TENANT_MANAGER でも TENANT 権限セット選択肢を取得できるようにした
- TENANT_MANAGER が `PLATFORM_ADMIN`、他社部署、削除済み部署を指定した場合は Cognito 作成前に拒否する

### 確認結果

- `cd apps/web && .\mvnw.cmd test`
  - `Tests run: 164, Failures: 0, Errors: 0, Skipped: 0`
- Service テストで TENANT_MANAGER の自社ユーザー作成、現在ユーザー会社での登録、TENANT 権限セット割当、PLATFORM_ADMIN 拒否、他社/削除済み部署拒否、username/email 重複拒否を確認した
- Mapper 統合テストで自社未削除部署の取得と TENANT 権限セット選択肢の取得を確認した

### 残課題

- ユーザー編集と権限変更は M10-05 で扱う
- TENANT_MANAGER 数が 0 になる権限変更拒否は M10-05 で扱う
- ユーザー無効化・削除は M10 範囲外
- 実 Cognito の `AdminCreateUser` を伴う画面 E2E はユーザー確認対象
- Hosted UI 本格確認、PLATFORM / TENANT App Client 分離、Cognito Trigger、Pre Token Generation、初回ログイン時の WorkOps ユーザー自動作成は Phase 2 で扱う
