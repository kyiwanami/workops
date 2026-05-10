# WorkOps DB Schema

## 位置づけ

このファイルは、WorkOps の DB 定義を人間が読みやすい形で整理する実装設計文書です。
実行上の正本は Flyway migration SQL です。

正本:

- `apps/web/src/main/resources/db/migration/V1__create_mvp_schema.sql`

Notion は DB 設計方針、ADR、テーブル群の役割を管理します。
リポジトリは Flyway migration SQL、カラム、型、NULL 可否、制約、seed SQL、確認 SQL を管理します。

## 共通方針

- MySQL 9.7.0 を前提にする
- テーブルは `ENGINE=InnoDB` を使う
- 文字コードは `utf8mb4` を使う
- 照合順序は `utf8mb4_0900_ai_ci` を使う
- 物理テーブル名に `mst_` / `trn_` / `wrk_` の接頭辞は付けない
- テーブル分類はこのファイルのテーブル一覧で管理する
- 主キーは `BIGINT AUTO_INCREMENT` を使う
- 日時は `DATETIME(6)` を使う
- 論理削除は `is_deleted BOOLEAN NOT NULL DEFAULT FALSE` で表現する
- MVP では `active`、`deleted_at`、`delete_token` は使わない
- MVP では削除済みコードの再利用を許可しない
- Cognito sub は論理的には UUID として扱い、MySQL では読みやすさを優先して `CHAR(36)` で保持する
- 履歴テーブルは作らず、申請と資産は現在状態を対象行に保持する
- 監査ログテーブルは作らず、業務操作・調査用ログはアプリケーションログへ出力する
- 有効期間管理は全体共通では入れず、業務上必要になった対象だけで検討する

## 作成・更新メタ情報

主要テーブルは、現在行の作成情報と最終更新情報として以下を持ちます。
これらは履歴管理ではありません。

| カラム | 型 | NULL | 既定値 | 説明 |
| --- | --- | --- | --- | --- |
| created_at | DATETIME(6) | NO | CURRENT_TIMESTAMP(6) | 作成日時 |
| created_by | BIGINT | YES |  | 作成ユーザーID |
| updated_at | DATETIME(6) | NO | CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6) | 更新日時 |
| updated_by | BIGINT | YES |  | 最終更新ユーザーID |

`created_by` / `updated_by` は M2 時点では外部キーを張りません。
`companies` や初期ユーザー作成時の循環参照を避け、M3 以降のアプリ実装でログインユーザーIDを設定します。

## テーブル一覧

| テーブル | 分類 | 役割 |
| --- | --- | --- |
| `companies` | マスタ | 会社境界を表す |
| `departments` | マスタ | 会社内の部署を表す |
| `users` | マスタ | 会社に所属するアプリ利用者情報の正本を表す |
| `permission_sets` | マスタ | 権限セットの入れ物を表す |
| `user_permission_sets` | 関連 / 割当 | ユーザーと権限セットの割当を表す |
| `common_master` | マスタ | 全社共通マスタの種別を表す |
| `common_master_values` | マスタ | 全社共通マスタの値を表す |
| `generic_master` | マスタ | 汎用マスタの種別を表す |
| `generic_master_values` | マスタ | 会社別汎用マスタの値を表す |
| `requests` | トランザクション | 申請の現在状態を表す |
| `assets` | 業務マスタ / 台帳 | 資産台帳として現在状態を表す |

## 作成しないテーブル

MVP では以下の履歴テーブル・監査ログテーブルを作成しません。

- `request_histories`
- `asset_status_histories`
- `audit_logs`

申請・資産は現在状態を対象行に保持し、状態変更時は対象行を更新します。
業務操作・調査用ログは RDB ではなくアプリケーションログとして出力します。
Phase 2 以降では CloudWatch Logs で確認する前提です。

## 主要な制約方針

- 会社別に一意であるべきコードは `(company_id, code)` で一意にする
- 全社共通で一意であるべきコードは `code` で一意にする
- 削除済み行も一意制約の対象に含め、MVP では削除済みコードの再利用を許可しない
- 会社境界は `company_id` で表現する
- 会社境界をまたぐ不正参照の防止は、MVP では Service 層の認可・会社境界チェックで扱う

## companies

会社を表します。

| カラム | 型 | NULL | 既定値 | 説明 |
| --- | --- | --- | --- | --- |
| id | BIGINT | NO | AUTO_INCREMENT | 主キー |
| code | VARCHAR(50) | NO |  | 会社コード |
| name | VARCHAR(100) | NO |  | 会社名 |
| is_deleted | BOOLEAN | NO | FALSE | 論理削除フラグ |
| created_at | DATETIME(6) | NO | CURRENT_TIMESTAMP(6) | 作成日時 |
| created_by | BIGINT | YES |  | 作成ユーザーID |
| updated_at | DATETIME(6) | NO | CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6) | 更新日時 |
| updated_by | BIGINT | YES |  | 最終更新ユーザーID |

制約:

- `PRIMARY KEY (id)`
- `UNIQUE KEY uq_companies_code (code)`

## departments

会社内の部署を表します。

| カラム | 型 | NULL | 既定値 | 説明 |
| --- | --- | --- | --- | --- |
| id | BIGINT | NO | AUTO_INCREMENT | 主キー |
| company_id | BIGINT | NO |  | 所属会社ID |
| code | VARCHAR(50) | NO |  | 部署コード |
| name | VARCHAR(100) | NO |  | 部署名 |
| is_deleted | BOOLEAN | NO | FALSE | 論理削除フラグ |
| created_at | DATETIME(6) | NO | CURRENT_TIMESTAMP(6) | 作成日時 |
| created_by | BIGINT | YES |  | 作成ユーザーID |
| updated_at | DATETIME(6) | NO | CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6) | 更新日時 |
| updated_by | BIGINT | YES |  | 最終更新ユーザーID |

制約:

- `PRIMARY KEY (id)`
- `UNIQUE KEY uq_departments_company_code (company_id, code)`
- `CONSTRAINT fk_departments_company FOREIGN KEY (company_id) REFERENCES companies (id)`

## users

会社に所属するアプリ利用者情報の正本を表します。
ログイン可否と無効化は Cognito 側で制御し、`users.status` は持ちません。
Cognito ログイン時は `cognito_sub` で `users` と突合します。

| カラム | 型 | NULL | 既定値 | 説明 |
| --- | --- | --- | --- | --- |
| id | BIGINT | NO | AUTO_INCREMENT | 主キー |
| company_id | BIGINT | NO |  | 所属会社ID |
| department_id | BIGINT | YES |  | 所属部署ID |
| cognito_sub | CHAR(36) | YES |  | Cognito sub |
| username | VARCHAR(100) | NO |  | アプリ利用者名 |
| name | VARCHAR(100) | NO |  | ユーザー名 |
| email | VARCHAR(255) | NO |  | メールアドレス |
| actor_type | VARCHAR(50) | NO |  | 利用者種別 |
| is_deleted | BOOLEAN | NO | FALSE | 論理削除フラグ |
| created_at | DATETIME(6) | NO | CURRENT_TIMESTAMP(6) | 作成日時 |
| created_by | BIGINT | YES |  | 作成ユーザーID |
| updated_at | DATETIME(6) | NO | CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6) | 更新日時 |
| updated_by | BIGINT | YES |  | 最終更新ユーザーID |

制約:

- `PRIMARY KEY (id)`
- `UNIQUE KEY uq_users_cognito_sub (cognito_sub)`
- `UNIQUE KEY uq_users_company_username (company_id, username)`
- `UNIQUE KEY uq_users_company_email (company_id, email)`
- `CONSTRAINT fk_users_company FOREIGN KEY (company_id) REFERENCES companies (id)`
- `CONSTRAINT fk_users_department FOREIGN KEY (department_id) REFERENCES departments (id)`

## permission_sets

権限セットの入れ物を表します。
`TENANT_VIEWER`、`TENANT_EDITOR`、`TENANT_MANAGER` の seed は M2-04 で投入します。

| カラム | 型 | NULL | 既定値 | 説明 |
| --- | --- | --- | --- | --- |
| id | BIGINT | NO | AUTO_INCREMENT | 主キー |
| code | VARCHAR(50) | NO |  | 権限セットコード |
| name | VARCHAR(100) | NO |  | 権限セット名 |
| description | VARCHAR(255) | YES |  | 説明 |
| is_deleted | BOOLEAN | NO | FALSE | 論理削除フラグ |
| created_at | DATETIME(6) | NO | CURRENT_TIMESTAMP(6) | 作成日時 |
| created_by | BIGINT | YES |  | 作成ユーザーID |
| updated_at | DATETIME(6) | NO | CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6) | 更新日時 |
| updated_by | BIGINT | YES |  | 最終更新ユーザーID |

制約:

- `PRIMARY KEY (id)`
- `UNIQUE KEY uq_permission_sets_code (code)`

## user_permission_sets

ユーザーと権限セットの割当を表します。

| カラム | 型 | NULL | 既定値 | 説明 |
| --- | --- | --- | --- | --- |
| user_id | BIGINT | NO |  | ユーザーID |
| permission_set_id | BIGINT | NO |  | 権限セットID |
| created_at | DATETIME(6) | NO | CURRENT_TIMESTAMP(6) | 作成日時 |

制約:

- `PRIMARY KEY (user_id, permission_set_id)`
- `CONSTRAINT fk_user_permission_sets_user FOREIGN KEY (user_id) REFERENCES users (id)`
- `CONSTRAINT fk_user_permission_sets_permission_set FOREIGN KEY (permission_set_id) REFERENCES permission_sets (id)`

## common_master

全社共通マスタの種別を表します。

| カラム | 型 | NULL | 既定値 | 説明 |
| --- | --- | --- | --- | --- |
| id | BIGINT | NO | AUTO_INCREMENT | 主キー |
| code | VARCHAR(50) | NO |  | 共通マスタ種別コード |
| name | VARCHAR(100) | NO |  | 共通マスタ種別名 |
| description | VARCHAR(255) | YES |  | 説明 |
| is_deleted | BOOLEAN | NO | FALSE | 論理削除フラグ |
| created_at | DATETIME(6) | NO | CURRENT_TIMESTAMP(6) | 作成日時 |
| created_by | BIGINT | YES |  | 作成ユーザーID |
| updated_at | DATETIME(6) | NO | CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6) | 更新日時 |
| updated_by | BIGINT | YES |  | 最終更新ユーザーID |

制約:

- `PRIMARY KEY (id)`
- `UNIQUE KEY uq_common_master_code (code)`

## common_master_values

全社共通マスタの値を表します。

| カラム | 型 | NULL | 既定値 | 説明 |
| --- | --- | --- | --- | --- |
| id | BIGINT | NO | AUTO_INCREMENT | 主キー |
| common_master_id | BIGINT | NO |  | 共通マスタ種別ID |
| code | VARCHAR(50) | NO |  | 共通マスタ値コード |
| name | VARCHAR(100) | NO |  | 共通マスタ値名 |
| sort_order | INT | NO | 0 | 表示順 |
| is_deleted | BOOLEAN | NO | FALSE | 論理削除フラグ |
| created_at | DATETIME(6) | NO | CURRENT_TIMESTAMP(6) | 作成日時 |
| created_by | BIGINT | YES |  | 作成ユーザーID |
| updated_at | DATETIME(6) | NO | CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6) | 更新日時 |
| updated_by | BIGINT | YES |  | 最終更新ユーザーID |

制約:

- `PRIMARY KEY (id)`
- `UNIQUE KEY uq_common_master_values_master_code (common_master_id, code)`
- `CONSTRAINT fk_common_master_values_master FOREIGN KEY (common_master_id) REFERENCES common_master (id)`

## generic_master

汎用マスタの種別を表します。
種別自体は会社別に分けず、会社ごとの差は `generic_master_values` で表現します。

| カラム | 型 | NULL | 既定値 | 説明 |
| --- | --- | --- | --- | --- |
| id | BIGINT | NO | AUTO_INCREMENT | 主キー |
| code | VARCHAR(50) | NO |  | 汎用マスタ種別コード |
| name | VARCHAR(100) | NO |  | 汎用マスタ種別名 |
| description | VARCHAR(255) | YES |  | 説明 |
| is_deleted | BOOLEAN | NO | FALSE | 論理削除フラグ |
| created_at | DATETIME(6) | NO | CURRENT_TIMESTAMP(6) | 作成日時 |
| created_by | BIGINT | YES |  | 作成ユーザーID |
| updated_at | DATETIME(6) | NO | CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6) | 更新日時 |
| updated_by | BIGINT | YES |  | 最終更新ユーザーID |

制約:

- `PRIMARY KEY (id)`
- `UNIQUE KEY uq_generic_master_code (code)`

## generic_master_values

会社別汎用マスタの値を表します。

| カラム | 型 | NULL | 既定値 | 説明 |
| --- | --- | --- | --- | --- |
| id | BIGINT | NO | AUTO_INCREMENT | 主キー |
| generic_master_id | BIGINT | NO |  | 汎用マスタ種別ID |
| company_id | BIGINT | NO |  | 会社ID |
| code | VARCHAR(50) | NO |  | 汎用マスタ値コード |
| name | VARCHAR(100) | NO |  | 汎用マスタ値名 |
| sort_order | INT | NO | 0 | 表示順 |
| is_deleted | BOOLEAN | NO | FALSE | 論理削除フラグ |
| created_at | DATETIME(6) | NO | CURRENT_TIMESTAMP(6) | 作成日時 |
| created_by | BIGINT | YES |  | 作成ユーザーID |
| updated_at | DATETIME(6) | NO | CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6) | 更新日時 |
| updated_by | BIGINT | YES |  | 最終更新ユーザーID |

制約:

- `PRIMARY KEY (id)`
- `UNIQUE KEY uq_generic_master_values_master_company_code (generic_master_id, company_id, code)`
- `CONSTRAINT fk_generic_master_values_master FOREIGN KEY (generic_master_id) REFERENCES generic_master (id)`
- `CONSTRAINT fk_generic_master_values_company FOREIGN KEY (company_id) REFERENCES companies (id)`

## assets

資産台帳として現在状態を表します。

| カラム | 型 | NULL | 既定値 | 説明 |
| --- | --- | --- | --- | --- |
| id | BIGINT | NO | AUTO_INCREMENT | 主キー |
| company_id | BIGINT | NO |  | 会社ID |
| asset_category_value_id | BIGINT | NO |  | 資産カテゴリ値ID |
| department_id | BIGINT | YES |  | 管理部署ID |
| code | VARCHAR(50) | NO |  | 資産コード |
| name | VARCHAR(100) | NO |  | 資産名 |
| status_code | VARCHAR(50) | NO |  | 資産ステータスコード |
| note | VARCHAR(1000) | YES |  | 備考 |
| is_deleted | BOOLEAN | NO | FALSE | 論理削除フラグ |
| created_at | DATETIME(6) | NO | CURRENT_TIMESTAMP(6) | 作成日時 |
| created_by | BIGINT | YES |  | 作成ユーザーID |
| updated_at | DATETIME(6) | NO | CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6) | 更新日時 |
| updated_by | BIGINT | YES |  | 最終更新ユーザーID |

制約:

- `PRIMARY KEY (id)`
- `UNIQUE KEY uq_assets_company_code (company_id, code)`
- `CONSTRAINT fk_assets_company FOREIGN KEY (company_id) REFERENCES companies (id)`
- `CONSTRAINT fk_assets_category_value FOREIGN KEY (asset_category_value_id) REFERENCES generic_master_values (id)`
- `CONSTRAINT fk_assets_department FOREIGN KEY (department_id) REFERENCES departments (id)`

## requests

申請の現在状態を表します。
申請は物理削除しないため、`is_deleted` を持ちません。

| カラム | 型 | NULL | 既定値 | 説明 |
| --- | --- | --- | --- | --- |
| id | BIGINT | NO | AUTO_INCREMENT | 主キー |
| company_id | BIGINT | NO |  | 会社ID |
| requester_user_id | BIGINT | NO |  | 申請者ユーザーID |
| asset_id | BIGINT | YES |  | 関連資産ID |
| process_type_code | VARCHAR(50) | NO |  | 処理種別コード |
| status_code | VARCHAR(50) | NO |  | 申請ステータスコード |
| title | VARCHAR(100) | NO |  | 件名 |
| content | VARCHAR(2000) | YES |  | 申請内容 |
| review_comment | VARCHAR(1000) | YES |  | 承認・却下・差戻しコメント |
| submitted_at | DATETIME(6) | YES |  | 提出日時 |
| created_at | DATETIME(6) | NO | CURRENT_TIMESTAMP(6) | 作成日時 |
| created_by | BIGINT | YES |  | 作成ユーザーID |
| updated_at | DATETIME(6) | NO | CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6) | 更新日時 |
| updated_by | BIGINT | YES |  | 最終更新ユーザーID |

制約:

- `PRIMARY KEY (id)`
- `CONSTRAINT fk_requests_company FOREIGN KEY (company_id) REFERENCES companies (id)`
- `CONSTRAINT fk_requests_requester_user FOREIGN KEY (requester_user_id) REFERENCES users (id)`
- `CONSTRAINT fk_requests_asset FOREIGN KEY (asset_id) REFERENCES assets (id)`
