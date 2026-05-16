# M9-03 部署一覧 / 作成

## 目的

会社ごとの部署を一覧表示し、PLATFORM_ADMIN と TENANT_MANAGER が許可された会社範囲で部署を作成できるようにする。

## M9 共通方針

- M9 の範囲は、会社作成、部署管理、会社作成時の会社別 `generic_master_values` 初期投入までとする
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

実装後に、次をこのファイルへ追記する。

- 実装方針
- 変更ファイル
- 実装結果
- 確認結果
- 残課題
