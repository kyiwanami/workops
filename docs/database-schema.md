# WorkOps DB Schema

## DB定義

テーブル、カラム、制約を記載しています。DBの作成・変更には以下のFlywayマイグレーションSQLを使います。

- [DBスキーマ](../db/migration/V1__create_mvp_schema.sql)
- [PLATFORMユーザーの会社所属制約](../db/migration/V2__allow_platform_users.sql)

## 共通方針

- MySQL 8.4 LTS を前提にする
- テーブルは `ENGINE=InnoDB` を使う
- 文字コードは `utf8mb4` を使う
- 照合順序は `utf8mb4_0900_ai_ci` を使う
- 物理テーブル名に `mst_` / `trn_` / `wrk_` の接頭辞は付けない
- テーブル分類はこのファイルのテーブル一覧で管理する
- 主キーは `BIGINT AUTO_INCREMENT` を使う
- 日時は `DATETIME(6)` を使う
- 論理削除は `is_deleted BOOLEAN NOT NULL DEFAULT FALSE` で表現する
- `active`、`deleted_at`、`delete_token` は使わない
- 削除済みコードの再利用を許可しない
- CognitoのsubはUUIDとして扱い、MySQLでは読みやすさを優先して`CHAR(36)`で保持する
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

`created_by` / `updated_by` は外部キーを張りません。
`companies` や初期ユーザー作成時の循環参照を避け、アプリ実装でログインユーザーIDを設定します。

## テーブル一覧

| テーブル | 分類 | 役割 |
| --- | --- | --- |
| `companies` | マスタ | 会社境界を表す |
| `departments` | マスタ | 会社内の部署を表す |
| `users` | マスタ | アプリ利用者と所属会社を管理する |
| `permission_sets` | マスタ | 権限セットの入れ物を表す |
| `user_permission_sets` | 関連 / 割当 | ユーザーと権限セットの割当を表す |
| `common_master` | マスタ | 全社共通マスタの種別を表す |
| `common_master_values` | マスタ | 全社共通マスタの値を表す |
| `generic_master` | マスタ | 汎用マスタの種別を表す |
| `generic_master_values` | マスタ | 会社別汎用マスタの値を表す |
| `requests` | トランザクション | 申請の現在状態を表す |
| `assets` | 業務マスタ / 台帳 | 資産台帳として現在状態を表す |

## 作成しないテーブル

以下の履歴テーブル・監査ログテーブルは作成しません。

- `request_histories`
- `asset_status_histories`
- `audit_logs`

申請・資産は現在状態を対象行に保持し、状態変更時は対象行を更新します。
業務操作・調査用ログはアプリケーションログとして出力します。RDBへの保存は行いません。
AWS環境のアプリケーションログはCloudWatch Logsで確認します。

## 主要な制約方針

- 会社別に一意であるべきコードは `(company_id, code)` で一意にする
- 全社共通で一意であるべきコードは `code` で一意にする
- 削除済み行も一意制約の対象に含め、削除済みコードの再利用を許可しない
- 会社境界は `company_id` で表現する
- PLATFORM ユーザーは会社に所属しないため `users.company_id` を `NULL` とする
- TENANT ユーザーは必ず会社に所属するため `users.company_id` を必須とする
- 会社境界をまたぐ不正参照は、Service層の認可・会社境界チェックと、会社IDを条件に含めたSQLで防ぐ

## companies

会社を表します。
会社削除は物理削除せず、`is_deleted = TRUE` の論理削除として扱います。
削除済み会社コードは再利用しません。
会社を論理削除しても、部署、ユーザー、申請、資産、会社別マスタ値などの配下データは物理削除しません。
配下データが存在していても、管理画面で警告表示したうえで会社の論理削除は可能とします。
削除済み会社は通常の新規作成・選択導線には表示せず、管理画面では削除済みであることを区別して表示します。

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

制約

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

制約

- `PRIMARY KEY (id)`
- `UNIQUE KEY uq_departments_company_code (company_id, code)`
- `CONSTRAINT fk_departments_company FOREIGN KEY (company_id) REFERENCES companies (id)`

## users

アプリ利用者の情報を管理します。
PLATFORMユーザーはWorkOps運営側のユーザーとして会社に所属せず、TENANTユーザーは会社に所属します。
ログイン可否と無効化はCognito側で制御し、`users.status`は持ちません。
Cognitoログイン時は`cognito_sub`で`users`と照合します。

| カラム | 型 | NULL | 既定値 | 説明 |
| --- | --- | --- | --- | --- |
| id | BIGINT | NO | AUTO_INCREMENT | 主キー |
| company_id | BIGINT | YES |  | 所属会社ID。PLATFORM は NULL、TENANT は必須 |
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

制約

- `PRIMARY KEY (id)`
- `UNIQUE KEY uq_users_cognito_sub (cognito_sub)`
- `UNIQUE KEY uq_users_company_username (company_id, username)`
- `UNIQUE KEY uq_users_company_email (company_id, email)`
- `CONSTRAINT fk_users_company FOREIGN KEY (company_id) REFERENCES companies (id)`
- `CONSTRAINT fk_users_department FOREIGN KEY (department_id) REFERENCES departments (id)`
- `CONSTRAINT ck_users_actor_company CHECK ((actor_type = 'PLATFORM' AND company_id IS NULL) OR (actor_type = 'TENANT' AND company_id IS NOT NULL))`

## permission_sets

権限セットの入れ物を表します。
`TENANT_VIEWER`、`TENANT_EDITOR`、`TENANT_MANAGER`、`PLATFORM_ADMIN`のseedは[権限セットのseed SQL](../db/seed/common/V4__insert_permission_sets.sql)で投入します。

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

制約

- `PRIMARY KEY (id)`
- `UNIQUE KEY uq_permission_sets_code (code)`

## user_permission_sets

ユーザーと権限セットの割当を表します。

| カラム | 型 | NULL | 既定値 | 説明 |
| --- | --- | --- | --- | --- |
| user_id | BIGINT | NO |  | ユーザーID |
| permission_set_id | BIGINT | NO |  | 権限セットID |
| created_at | DATETIME(6) | NO | CURRENT_TIMESTAMP(6) | 作成日時 |

制約

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

制約

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

制約

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

制約

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

制約

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

制約

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
| request_type_value_id | BIGINT | NO |  | 申請種別値ID |
| status_code | VARCHAR(50) | NO |  | 申請ステータスコード |
| title | VARCHAR(100) | NO |  | 件名 |
| content | VARCHAR(2000) | YES |  | 申請内容 |
| review_comment | VARCHAR(1000) | YES |  | 承認・却下・差戻しコメント |
| submitted_at | DATETIME(6) | YES |  | 提出日時 |
| created_at | DATETIME(6) | NO | CURRENT_TIMESTAMP(6) | 作成日時 |
| created_by | BIGINT | YES |  | 作成ユーザーID |
| updated_at | DATETIME(6) | NO | CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6) | 更新日時 |
| updated_by | BIGINT | YES |  | 最終更新ユーザーID |

制約

- `PRIMARY KEY (id)`
- `CONSTRAINT fk_requests_company FOREIGN KEY (company_id) REFERENCES companies (id)`
- `CONSTRAINT fk_requests_requester_user FOREIGN KEY (requester_user_id) REFERENCES users (id)`
- `CONSTRAINT fk_requests_asset FOREIGN KEY (asset_id) REFERENCES assets (id)`
- `CONSTRAINT fk_requests_request_type_value FOREIGN KEY (request_type_value_id) REFERENCES generic_master_values (id)`
