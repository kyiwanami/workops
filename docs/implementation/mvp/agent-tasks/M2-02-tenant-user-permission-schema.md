# M2-02 tenant / user / permission schema

## 目的

会社境界と権限確認の前提になる tenant / user / permission 系テーブルを作る。

## 対応範囲

- `companies` テーブルを作成する
- `departments` テーブルを作成する
- `users` テーブルを作成する
- `permission_sets` テーブルを作成する
- `user_permission_sets` テーブルを作成する
- 会社境界を表す `company_id` を必要なテーブルに持たせる
- M3 の local 疑似認証が参照できる DB 構造にする

## 対応ファイル

- `apps/web/src/main/resources/db/migration/V1__create_mvp_schema.sql`
- `docs/implementation/mvp/agent-tasks/M2-02-tenant-user-permission-schema.md`

## 除外範囲

- seed SQL
- Cognito 連携
- Spring Security
- ユーザー管理画面
- 権限割当画面
- Mapper
- Service
- Controller

## 完了条件

2社構成、部署、ユーザー、権限セット、ユーザー権限割当を表現できる DDL が `V1__create_mvp_schema.sql` に定義されている。

## 確認方法

- Flyway migration を実行する
- MySQL 上に対象5テーブルが存在することを確認する
- 対象5テーブルの外部キーと一意制約が作成されていることを確認する

## 実装時の記録

### 実装方針

- `companies` / `departments` / `users` / `permission_sets` / `user_permission_sets` を作成する
- Notion 側で了承された論理削除方針に従い、`is_deleted BOOLEAN NOT NULL DEFAULT FALSE` のみを採用する
- `active`、`deleted_at`、`delete_token` は採用しない
- MVP では削除済みコードの再利用を許可しないため、一意制約は削除済み行も含めて維持する
- 権限セットはテーブルだけ作成し、`TENANT_VIEWER` などの seed は M2-04 で投入する
- テーブル定義の読み取り用文書を `docs/implementation/db/schema.md` に作成する

### 変更ファイル

- `apps/web/src/main/resources/db/migration/V1__create_mvp_schema.sql`
- `docs/implementation/db/schema.md`
- `docs/implementation/mvp/agent-tasks/M2-01-flyway-migration-structure.md`
- `docs/implementation/mvp/agent-tasks/M2-02-tenant-user-permission-schema.md`

### 確認結果

- MySQL 上に `companies` / `departments` / `users` / `permission_sets` / `user_permission_sets` が存在することを確認した
- `companies` に主キーと `uq_companies_code` が存在することを確認した
- `departments` に主キー、`uq_departments_company_code`、`fk_departments_company` が存在することを確認した
- `users` に主キー、`uq_users_cognito_sub`、`uq_users_company_username`、`uq_users_company_email`、`fk_users_company`、`fk_users_department` が存在することを確認した
- `permission_sets` に主キーと `uq_permission_sets_code` が存在することを確認した
- `user_permission_sets` に複合主キー、`fk_user_permission_sets_user`、`fk_user_permission_sets_permission_set` が存在することを確認した

### 残課題

- なし
