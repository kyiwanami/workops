# M9-03 部署一覧 / 作成

## 目的

会社ごとの部署を一覧表示し、PLATFORM_ADMIN と TENANT_MANAGER が許可された会社範囲で部署を作成できるようにする。

## M9 共通方針

- M9 の範囲は、会社一覧・詳細・作成・編集・論理削除、削除済み会社の表示、部署一覧・作成・編集・論理削除、削除済み部署の表示、会社作成時の会社別 `generic_master_values` 初期投入までとする
- 会社削除は物理削除せず、`companies.is_deleted = TRUE` の論理削除として扱う
- 削除済み会社コードは再利用しない
- 会社を論理削除しても、部署、ユーザー、申請、資産、会社別マスタ値などの配下データは物理削除しない
- 配下データが存在していても、警告表示したうえで会社の論理削除は可能とする
- 削除済み会社は通常の新規作成・選択導線には出さず、管理画面では削除済みであることを区別して表示する
- 初期 TENANT_MANAGER 作成、ユーザー作成、権限割当・変更、local Cognito Fake Bean、疑似 `cognito_sub` 発行は M10 で扱う
- 部署は既存 `departments` テーブルで管理する
- 部署は会社ごとに管理する
- 部署コードは会社内で一意とする
- 削除済み部署コードの再利用は許可しない
- 部署階層、所属履歴、組織改編履歴、有効期間管理、部署ごとの権限管理は M9 で扱わない
- Git commit は実行しない

## 対応範囲

- 部署一覧画面を作成する
- 部署登録画面を作成する
- PLATFORM_ADMIN 用の会社指定付き部署一覧を作成する
- PLATFORM_ADMIN 用の会社指定付き部署作成を作成する
- TENANT_MANAGER 用の自社部署一覧を作成する
- TENANT_MANAGER 用の自社部署作成を作成する
- `GET /admin/companies/{companyId}/departments` を追加する
- `GET /admin/companies/{companyId}/departments/new` を追加する
- `POST /admin/companies/{companyId}/departments` を追加する
- `GET /departments` を追加する
- `GET /departments/new` を追加する
- `POST /departments` を追加する
- PLATFORM_ADMIN 向け Controller / Service メソッドには `@PreAuthorize("hasAuthority('PLATFORM_ADMIN')")` を付ける
- TENANT_MANAGER 向け Controller / Service メソッドには `@PreAuthorize("hasAuthority('TENANT_MANAGER')")` を付ける
- PLATFORM_ADMIN は任意の会社 ID を対象に部署一覧・作成を実行できる
- TENANT_MANAGER は現在ユーザーの `companyId` だけを対象に部署一覧・作成を実行できる
- 部署一覧では通常、`is_deleted = FALSE` の部署だけを表示する
- 部署作成時は `company_id`、`code`、`name` を設定する
- 部署作成時は `created_by`、`updated_by` に現在ユーザーの `userId` を設定する
- 同一会社内の部署コード重複は、削除済み行を含めて画面入力エラーとしてフォームに戻す
- 他社部署が TENANT_MANAGER の一覧・作成対象に混ざらないようにする
- トップ画面または既存ナビゲーションから部署管理画面へ遷移できるようにする

## 対応ファイル

- `apps/web/src/main/java/com/example/workops/admin/department/web/DepartmentAdminController.java`
- `apps/web/src/main/java/com/example/workops/admin/department/form/DepartmentForm.java`
- `apps/web/src/main/java/com/example/workops/admin/department/service/DepartmentAdminService.java`
- `apps/web/src/main/java/com/example/workops/admin/department/mapper/DepartmentAdminMapper.java`
- `apps/web/src/main/java/com/example/workops/admin/department/model/*.java`
- `apps/web/src/main/resources/mapper/admin/department/DepartmentAdminMapper.xml`
- `apps/web/src/main/resources/templates/admin/department/department-list.html`
- `apps/web/src/main/resources/templates/admin/department/department-form.html`
- `apps/web/src/main/resources/templates/index.html`
- `apps/web/src/test/java/com/example/workops/admin/department/**/*.java`
- `docs/implementation/mvp/agent-tasks/M9/M9-03-department-list-create.md`

## 除外範囲

- 部署編集
- 部署論理削除
- 削除済み部署表示
- 所属ユーザーあり部署の削除警告
- 部署物理削除
- 削除済み部署コード再利用
- 所属ユーザーの部署自動解除
- ユーザー作成画面
- ユーザー作成時の所属部署選択肢
- 部署階層
- 部署単位の承認ルート

## 完了条件

PLATFORM_ADMIN が任意の会社の部署一覧を表示し、その会社に部署を作成できる。
TENANT_MANAGER が自社部署一覧を表示し、自社に部署を作成できる。
TENANT_MANAGER は他社部署を一覧表示・作成できない。
同一会社内の部署コード重複は削除済み行を含めて拒否される。

## 確認方法

- `cd apps/web && .\mvnw.cmd test`
- local profile の PLATFORM_ADMIN で会社 ID を指定した部署一覧を表示できることを確認する
- local profile の PLATFORM_ADMIN で会社 ID を指定して部署を作成できることを確認する
- local profile の TENANT_MANAGER で自社部署一覧を表示できることを確認する
- local profile の TENANT_MANAGER で自社部署を作成できることを確認する
- TENANT_MANAGER で他社会社 ID を指定する部署管理 URL へアクセスできないことを確認する
- 同一会社内の部署コード重複がフォーム入力エラーになることを確認する
- 別会社では同じ部署コードを作成できることを確認する

## 実装時の記録

### 実装方針

- 部署管理は `admin/department` 配下に Controller / Form / Service / Mapper / Model / Thymeleaf template を置いた
- 会社存在確認と会社表示項目取得は部署管理 Service / Mapper に閉じ、汎用会社参照 Service は作らない
- 部署一覧画面の会社表示は `DepartmentListPage` の `companyId`、`companyCode`、`companyName` で扱う
- PLATFORM_ADMIN は path の `companyId` を対象会社にする
- TENANT_MANAGER は現在ユーザーの `companyId` を対象会社にする
- 部署コード重複は Service で検出し、Controller で `BindingResult.rejectValue("code", ...)` に変換する
- M9-02 の会社作成後リダイレクトは、作成会社の部署一覧へ変更した

### 変更ファイル

- `apps/web/src/main/java/com/example/workops/admin/company/web/CompanyAdminController.java`
- `apps/web/src/main/java/com/example/workops/admin/department/form/DepartmentForm.java`
- `apps/web/src/main/java/com/example/workops/admin/department/mapper/DepartmentAdminMapper.java`
- `apps/web/src/main/java/com/example/workops/admin/department/model/DepartmentListItem.java`
- `apps/web/src/main/java/com/example/workops/admin/department/model/DepartmentListPage.java`
- `apps/web/src/main/java/com/example/workops/admin/department/service/DepartmentAdminService.java`
- `apps/web/src/main/java/com/example/workops/admin/department/web/DepartmentAdminController.java`
- `apps/web/src/main/resources/mapper/admin/department/DepartmentAdminMapper.xml`
- `apps/web/src/main/resources/templates/admin/department/department-form.html`
- `apps/web/src/main/resources/templates/admin/department/department-list.html`
- `apps/web/src/main/resources/templates/index.html`
- `apps/web/src/test/java/com/example/workops/admin/department/service/DepartmentAdminServiceTests.java`
- `apps/web/src/test/java/com/example/workops/integration/DepartmentAdminMapperIntegrationTests.java`
- `docs/implementation/mvp/agent-tasks/M9/M9-03-department-list-create.md`

### 実装結果

- PLATFORM_ADMIN 用の部署一覧・作成ルートを追加した
- TENANT_MANAGER 用の部署一覧・作成ルートを追加した
- 会社未存在または論理削除済みは `404 NOT_FOUND` として扱う
- 部署一覧は `is_deleted = FALSE` の部署だけを表示する
- 部署作成時に `created_by` / `updated_by` へ現在ユーザー ID を設定する
- 同一会社内の部署コード重複は、削除済み行を含めて拒否する
- 会社作成後は `/admin/companies/{createdCompanyId}/departments` へ redirect する
- DB schema migration は追加していない

### 確認結果

- `cd apps/web && .\mvnw.cmd test`
  - 120 tests
  - Failures: 0
  - Errors: 0
  - Skipped: 0
- PLATFORM_ADMIN local profile
  - `GET /admin/companies/1/departments`: 200
  - `GET /admin/companies/1/departments/new`: 200
  - `POST /admin/companies/1/departments`: 作成後一覧へ遷移し、Toast と作成部署コードを表示
- TENANT_MANAGER local profile
  - `GET /departments`: 200
  - `GET /departments/new`: 200
  - `POST /departments`: 作成後一覧へ遷移し、Toast と作成部署コードを表示
  - `GET /admin/companies/2/departments`: 403

### 残課題

- 部署編集、論理削除、削除済み表示は M9-04 で扱う
- PLATFORM_ADMIN 向け会社一覧・会社選択導線は M9-03 では作らない
- Notion 側の M9 スコープ更新により、会社一覧・詳細・編集・論理削除・削除済み表示は M9 内の追加 task で扱う
