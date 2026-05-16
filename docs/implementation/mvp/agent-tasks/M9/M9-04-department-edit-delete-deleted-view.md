# M9-04 部署編集 / 論理削除 / 削除済み表示

## 目的

会社ごとの部署を編集・論理削除できるようにし、削除済み部署を明示的に表示できるようにする。

## M9 共通方針

- M9 の範囲は、会社作成、部署管理、会社作成時の会社別 `generic_master_values` 初期投入までとする
- 初期 TENANT_MANAGER 作成、ユーザー作成、権限割当・変更、local Cognito Fake Bean、疑似 `cognito_sub` 発行は M10 で扱う
- 部署は既存 `departments` テーブルで管理する
- 部署削除は物理削除せず、`departments.is_deleted = TRUE` の論理削除として扱う
- 所属ユーザーがいる部署も、警告表示したうえで論理削除できる
- 削除済み部署コードの再利用は許可しない
- 既存ユーザーが削除済み部署に所属している場合は、部署名の末尾に「（削除済み）」を付けて表示する
- Git commit は実行しない

## 対応範囲

- 部署編集画面を作成する
- 部署論理削除操作を作成する
- 削除済み部署の表示切替を作成する
- PLATFORM_ADMIN 用の会社指定付き部署編集・論理削除・削除済み表示を作成する
- TENANT_MANAGER 用の自社部署編集・論理削除・削除済み表示を作成する
- `GET /admin/companies/{companyId}/departments?showDeleted=true` を追加する
- `GET /admin/companies/{companyId}/departments/{departmentId}/edit` を追加する
- `POST /admin/companies/{companyId}/departments/{departmentId}` を追加する
- `POST /admin/companies/{companyId}/departments/{departmentId}/delete` を追加する
- `GET /departments?showDeleted=true` を追加する
- `GET /departments/{departmentId}/edit` を追加する
- `POST /departments/{departmentId}` を追加する
- `POST /departments/{departmentId}/delete` を追加する
- PLATFORM_ADMIN 向け Controller / Service メソッドには `@PreAuthorize("hasAuthority('PLATFORM_ADMIN')")` を付ける
- TENANT_MANAGER 向け Controller / Service メソッドには `@PreAuthorize("hasAuthority('TENANT_MANAGER')")` を付ける
- 部署編集では `name` を更新し、`code` と `company_id` は変更しない
- 部署編集時は `updated_by` に現在ユーザーの `userId` を設定する
- 部署論理削除時は `is_deleted = TRUE` と `updated_by` を更新する
- 部署論理削除は確認モーダル経由で POST する
- 所属ユーザーがいる部署の削除確認モーダルでは、所属ユーザーがいることを警告表示する
- 所属ユーザー数は削除確認表示に使い、削除可否の拒否条件にはしない
- 通常一覧には未削除部署だけを表示する
- `showDeleted = true` の一覧には削除済み部署も表示する
- 削除済み部署は一覧上の状態を「削除済み」として表示する
- 削除済み部署は通常編集・論理削除の対象外とする
- 削除済み部署の復活操作は作らない
- ユーザー表示で部署名を表示する既存箇所がある場合は、削除済み部署名を「部署名（削除済み）」として表示する

## 対応ファイル

- `apps/web/src/main/java/com/example/workops/admin/department/web/DepartmentAdminController.java`
- `apps/web/src/main/java/com/example/workops/admin/department/form/DepartmentForm.java`
- `apps/web/src/main/java/com/example/workops/admin/department/form/DepartmentSearchForm.java`
- `apps/web/src/main/java/com/example/workops/admin/department/service/DepartmentAdminService.java`
- `apps/web/src/main/java/com/example/workops/admin/department/mapper/DepartmentAdminMapper.java`
- `apps/web/src/main/java/com/example/workops/admin/department/model/*.java`
- `apps/web/src/main/resources/mapper/admin/department/DepartmentAdminMapper.xml`
- `apps/web/src/main/resources/templates/admin/department/department-list.html`
- `apps/web/src/main/resources/templates/admin/department/department-form.html`
- `apps/web/src/test/java/com/example/workops/admin/department/**/*.java`
- `docs/implementation/mvp/agent-tasks/M9/M9-04-department-edit-delete-deleted-view.md`

## 除外範囲

- 部署復活
- 部署物理削除
- 削除済み部署コード再利用
- 所属ユーザーの部署自動解除
- 所属履歴
- 組織改編履歴
- 部署の有効期間管理
- 部署ごとの権限管理
- 部署単位の承認ルート
- ユーザー作成画面
- ユーザー編集画面

## 完了条件

PLATFORM_ADMIN が任意の会社の部署を編集・論理削除でき、削除済み部署を表示できる。
TENANT_MANAGER が自社部署を編集・論理削除でき、削除済み部署を表示できる。
所属ユーザーがいる部署は警告表示したうえで論理削除できる。
削除済み部署は通常一覧から除外され、削除済み表示時だけ一覧に表示される。
削除済み部署コードは再利用できない。

## 確認方法

- `cd apps/web && .\mvnw.cmd test`
- local profile の PLATFORM_ADMIN で会社 ID を指定して部署を編集できることを確認する
- local profile の PLATFORM_ADMIN で会社 ID を指定して部署を論理削除できることを確認する
- local profile の PLATFORM_ADMIN で削除済み部署を一覧表示できることを確認する
- local profile の TENANT_MANAGER で自社部署を編集できることを確認する
- local profile の TENANT_MANAGER で自社部署を論理削除できることを確認する
- local profile の TENANT_MANAGER で削除済み自社部署を一覧表示できることを確認する
- 所属ユーザーがいる部署の削除確認モーダルに警告が表示されることを確認する
- 所属ユーザーがいる部署も論理削除できることを確認する
- 削除済み部署コードを同じ会社内で再利用できないことを確認する
- TENANT_MANAGER が他社部署を編集・論理削除できないことを確認する

## 実装時の記録

### 実装方針

- 削除済み表示切替は既存マスタ管理と同じ `showDeleted` request parameter と `DepartmentSearchForm(Boolean showDeleted)` で扱った
- 会社存在確認と会社表示項目取得は、M9-03 と同じく `DepartmentAdminService` / `DepartmentAdminMapper` に閉じた
- 部署編集では `name` だけを更新し、`code` と `company_id` は更新対象にしない
- 部署削除は `departments.is_deleted = TRUE` と `updated_by` 更新による論理削除で実装した
- 所属ユーザー数は一覧行に持たせ、削除確認モーダルで警告表示に使った

### 変更ファイル

- `apps/web/src/main/java/com/example/workops/admin/department/form/DepartmentForm.java`
- `apps/web/src/main/java/com/example/workops/admin/department/form/DepartmentSearchForm.java`
- `apps/web/src/main/java/com/example/workops/admin/department/mapper/DepartmentAdminMapper.java`
- `apps/web/src/main/java/com/example/workops/admin/department/model/DepartmentListItem.java`
- `apps/web/src/main/java/com/example/workops/admin/department/model/DepartmentListPage.java`
- `apps/web/src/main/java/com/example/workops/admin/department/service/DepartmentAdminService.java`
- `apps/web/src/main/java/com/example/workops/admin/department/web/DepartmentAdminController.java`
- `apps/web/src/main/resources/mapper/admin/department/DepartmentAdminMapper.xml`
- `apps/web/src/main/resources/templates/admin/department/department-form.html`
- `apps/web/src/main/resources/templates/admin/department/department-list.html`
- `apps/web/src/test/java/com/example/workops/admin/department/service/DepartmentAdminServiceTests.java`
- `apps/web/src/test/java/com/example/workops/integration/DepartmentAdminMapperIntegrationTests.java`
- `docs/implementation/mvp/agent-tasks/M9/M9-04-department-edit-delete-deleted-view.md`

### 実装結果

- PLATFORM_ADMIN 用の部署編集・論理削除・削除済み表示を追加した
- TENANT_MANAGER 用の自社部署編集・論理削除・削除済み表示を追加した
- 通常一覧では未削除部署だけを表示し、`showDeleted=true` では削除済み部署も表示する
- 削除済み部署は一覧上で「削除済み」状態を表示し、編集・削除操作を出さない
- 所属中の未削除ユーザー数を一覧と削除確認モーダルに表示する
- 所属ユーザーがいる部署も論理削除できる
- DB schema migration は追加していない
- ユーザー一覧・ユーザー詳細画面は現時点で存在しないため、ユーザー表示側の部署名補正は行っていない

### 確認結果

- `cd apps/web && .\mvnw.cmd test`
  - 130 tests
  - Failures: 0
  - Errors: 0
  - Skipped: 0
- PLATFORM_ADMIN local profile
  - 部署登録: 200 / 一覧表示
  - 部署編集: 200 / 更新後一覧表示
  - 部署論理削除: 通常一覧から非表示
  - `showDeleted=true`: 削除済み部署と削除済み badge を表示
  - 所属ユーザーあり部署の削除確認モーダル警告を表示
- TENANT_MANAGER local profile
  - 自社部署登録: 200 / 一覧表示
  - 自社部署編集: 200 / 更新後一覧表示
  - 自社部署論理削除: 通常一覧から非表示
  - `showDeleted=true`: 削除済み部署と削除済み badge を表示
  - PLATFORM_ADMIN 用編集 URL は 403

### 残課題

- 部署復活、物理削除、所属ユーザーの部署自動解除は M9-04 では扱わない
- ユーザー管理画面が追加された段階で、削除済み部署名を「部署名（削除済み）」として表示する対応を行う
