# M9-02 会社作成 / テナント初期化

## 目的

PLATFORM_ADMIN が会社を作成できるようにし、会社作成時に会社別 `generic_master_values` の初期値を投入する。

## M9 共通方針

- M9 の範囲は、会社一覧・詳細・作成・編集・論理削除、削除済み会社の表示、部署一覧・作成・編集・論理削除、削除済み部署の表示、会社作成時の会社別 `generic_master_values` 初期投入までとする
- 会社削除は物理削除せず、`companies.is_deleted = TRUE` の論理削除として扱う
- 削除済み会社コードは再利用しない
- 会社を論理削除しても、部署、ユーザー、申請、資産、会社別マスタ値などの配下データは物理削除しない
- 配下データが存在していても、警告表示したうえで会社の論理削除は可能とする
- 削除済み会社は通常の新規作成・選択導線には出さず、管理画面では削除済みであることを区別して表示する
- 初期 TENANT_MANAGER 作成、ユーザー作成、権限割当・変更、local Cognito Fake Bean、疑似 `cognito_sub` 発行は M10 で扱う
- 会社作成時の会社別 `generic_master_values` 初期投入は、会社作成に伴うテナント初期化処理として M9 に含める
- `generic_master` 種別管理やマスタ管理画面の拡張は M9 で扱わない
- 会社別初期値は既存の `generic_master` 種別に対する `generic_master_values` として投入する
- 削除済みコードの再利用は許可しない
- Git commit は実行しない

## 対応範囲

- PLATFORM_ADMIN 専用の会社作成画面を作成する
- `GET /admin/companies/new` を追加する
- `POST /admin/companies` を追加する
- Controller の会社作成系メソッドには `@PreAuthorize("hasAuthority('PLATFORM_ADMIN')")` を付ける
- Service の会社作成系 public メソッドにも `@PreAuthorize("hasAuthority('PLATFORM_ADMIN')")` を付ける
- 会社作成時は `companies.code`、`companies.name` を入力する
- 会社作成時は `companies.created_by`、`companies.updated_by` に現在ユーザーの `userId` を設定する
- 会社コード重複は画面入力エラーとしてフォームに戻す
- 会社作成と会社別 `generic_master_values` 初期投入は同一トランザクションで扱う
- 会社作成後、既存 `generic_master` の `REQUEST_TYPE` と `ASSET_CATEGORY` に対する会社別初期値を `generic_master_values` へ投入する
- 初期投入する `REQUEST_TYPE` と `ASSET_CATEGORY` のコード、名称、表示順を実装内で固定し、作成会社ごとに同じ初期値を投入する
- `generic_master` に `REQUEST_TYPE` または `ASSET_CATEGORY` が存在しない場合は 500 として扱い、会社作成を完了させない
- 初期 TENANT_MANAGER 作成は呼び出さない
- 会社作成後は、作成会社の部署一覧または会社作成完了を確認できる画面へリダイレクトする
- Bootstrap Toast で会社作成完了を表示する

## 対応ファイル

- `apps/web/src/main/java/com/example/workops/admin/company/web/CompanyAdminController.java`
- `apps/web/src/main/java/com/example/workops/admin/company/form/CompanyForm.java`
- `apps/web/src/main/java/com/example/workops/admin/company/service/CompanyAdminService.java`
- `apps/web/src/main/java/com/example/workops/admin/company/service/TenantInitializationService.java`
- `apps/web/src/main/java/com/example/workops/admin/company/mapper/CompanyAdminMapper.java`
- `apps/web/src/main/java/com/example/workops/admin/company/model/*.java`
- `apps/web/src/main/resources/mapper/admin/company/CompanyAdminMapper.xml`
- `apps/web/src/main/resources/templates/admin/company/company-form.html`
- `apps/web/src/main/resources/templates/index.html`
- `apps/web/src/test/java/com/example/workops/admin/company/**/*.java`
- `docs/implementation/mvp/agent-tasks/M9/M9-02-company-create-tenant-initialization.md`

## 除外範囲

- 会社一覧
- 会社詳細
- 会社編集
- 会社論理削除
- 削除済み会社の表示
- 初期 TENANT_MANAGER 作成
- ユーザー作成
- 権限割当・変更
- local Cognito Fake Bean
- `generic_master` 種別追加・編集
- 申請種別マスタ管理画面の拡張
- 資産分類マスタ管理画面の拡張
- Cognito 本物 API 呼び出し

## 完了条件

PLATFORM_ADMIN が local profile で会社を作成できる。
会社コード重複は拒否される。
会社作成時に、作成会社の `REQUEST_TYPE` と `ASSET_CATEGORY` の会社別初期 `generic_master_values` が投入される。
会社作成処理では、初期 TENANT_MANAGER 作成、ユーザー作成、権限割当、Cognito 関連処理を実行しない。

## 確認方法

- `cd apps/web && .\mvnw.cmd test`
- local profile の PLATFORM_ADMIN で `GET /admin/companies/new` が HTTP 200 を返すことを確認する
- local profile の PLATFORM_ADMIN で会社を作成できることを確認する
- 作成会社が `companies` に登録されることを確認する
- 作成会社の `REQUEST_TYPE` 初期値が `generic_master_values` に登録されることを確認する
- 作成会社の `ASSET_CATEGORY` 初期値が `generic_master_values` に登録されることを確認する
- 既存会社コードと同じコードで会社作成するとフォーム入力エラーになることを確認する
- TENANT_MANAGER が会社作成画面へアクセスできないことを確認する
- Service テストで、会社作成とテナント初期化が同一トランザクション内の処理として呼ばれることを確認する

## 実装時の記録

### 実装方針

- DB schema migration は追加せず、既存の `companies` と `generic_master_values` を使用した
- PLATFORM_ADMIN 専用の会社作成 Controller / Service / Mapper / Thymeleaf template を `admin/company` 配下に新設した
- 会社作成と会社別 `generic_master_values` 初期投入は `CompanyAdminService#create` の同一トランザクションで扱う
- `TenantInitializationService#initializeTenant` は `Propagation.MANDATORY` とし、会社作成トランザクション内からだけ呼び出す
- 会社作成後は M9-03 で追加した作成会社の部署一覧 `/admin/companies/{createdCompanyId}/departments` にリダイレクトし、Bootstrap Toast で完了を表示する

### 変更ファイル

- `apps/web/src/main/java/com/example/workops/admin/company/form/CompanyForm.java`
- `apps/web/src/main/java/com/example/workops/admin/company/mapper/CompanyAdminMapper.java`
- `apps/web/src/main/java/com/example/workops/admin/company/model/TenantInitialMasterValue.java`
- `apps/web/src/main/java/com/example/workops/admin/company/service/CompanyAdminService.java`
- `apps/web/src/main/java/com/example/workops/admin/company/service/TenantInitializationService.java`
- `apps/web/src/main/java/com/example/workops/admin/company/web/CompanyAdminController.java`
- `apps/web/src/main/resources/mapper/admin/company/CompanyAdminMapper.xml`
- `apps/web/src/main/resources/templates/admin/company/company-form.html`
- `apps/web/src/main/resources/templates/index.html`
- `apps/web/src/test/java/com/example/workops/admin/company/service/CompanyAdminServiceTests.java`
- `apps/web/src/test/java/com/example/workops/integration/CompanyAdminMapperIntegrationTests.java`
- `docs/implementation/mvp/agent-tasks/M9/M9-02-company-create-tenant-initialization.md`

### 実装結果

- `GET /admin/companies/new` を追加した
- `POST /admin/companies` を追加した
- Controller と Service の会社作成メソッドに `@PreAuthorize("hasAuthority('PLATFORM_ADMIN')")` を付けた
- 会社作成時に `companies.code`、`companies.name`、`created_by`、`updated_by` を登録する
- 会社コード重複はフォーム入力エラーとして `company-form` に戻す
- 作成会社へ `ASSET_CATEGORY` 初期値6件と `REQUEST_TYPE` 初期値3件を投入する
- `generic_master` の `ASSET_CATEGORY` または `REQUEST_TYPE` が存在しない場合は `500 INTERNAL_SERVER_ERROR` とし、会社作成を完了させない
- 初期 TENANT_MANAGER 作成、ユーザー作成、権限割当、Cognito 関連処理は呼び出していない

### 確認結果

- `cd apps/web && .\mvnw.cmd test`
  - `Tests run: 107, Failures: 0, Errors: 0, Skipped: 0`
- local profile の PLATFORM_ADMIN で `GET /admin/companies/new` が HTTP 200 を返すことを確認した
- local profile の PLATFORM_ADMIN で会社作成後に `/admin/companies/{createdCompanyId}/departments` へリダイレクトし、作成会社の部署一覧を表示することを確認した
- リダイレクト後の画面に Bootstrap Toast の `会社を登録しました。` が表示されることを確認した
- 作成会社が `companies` に登録されることを確認した
- 作成会社の `ASSET_CATEGORY` 初期値が6件登録されることを確認した
- 作成会社の `REQUEST_TYPE` 初期値が3件登録されることを確認した
- local profile の TENANT_MANAGER で `GET /admin/companies/new` が HTTP 403 を返すことを確認した

### 残課題

- 会社一覧、会社詳細、会社編集、会社論理削除、削除済み会社の表示は M9-02 では未実装
- Notion 側の M9 スコープ更新により、会社一覧、会社詳細、会社編集、会社論理削除、削除済み会社の表示は M9 内の追加 task で扱う
- 会社作成後の部署一覧遷移は M9-03 で対応済み
- 初期 TENANT_MANAGER 作成、ユーザー作成、権限割当・変更、local Cognito Fake Bean、疑似 `cognito_sub` 発行は M10 で扱う
