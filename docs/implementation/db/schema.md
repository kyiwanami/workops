# WorkOps DB Schema

## 位置づけ

このファイルは、WorkOps の DB 定義を人間が読みやすい形で整理する実装設計文書です。
実行上の正本は Flyway migration SQL です。

正本:

- `apps/server/src/main/resources/db/migration/V1__create_mvp_schema.sql`

Notion は DB 設計方針、ADR、テーブル群の役割を管理します。
リポジトリは Flyway migration SQL、カラム、型、NULL 可否、制約、seed SQL、確認 SQL を管理します。

## 共通方針

- MySQL 9.7.0 を前提にする
- テーブルは `ENGINE=InnoDB` を使う
- 文字コードは `utf8mb4` を使う
- 照合順序は `utf8mb4_0900_ai_ci` を使う
- 主キーは `BIGINT AUTO_INCREMENT` を使う
- 日時は `DATETIME(6)` を使う
- 論理削除は `is_deleted BOOLEAN NOT NULL DEFAULT FALSE` で表現する
- MVP では `active`、`deleted_at`、`delete_token` は使わない
- MVP では削除済みコードの再利用を許可しない

## M2-01 / M2-02

M2-01 / M2-02 では、会社境界、ユーザー、権限セットの前提になる5テーブルを作成します。
seed は M2-04 で作成します。

### companies

会社を表します。

| カラム | 型 | NULL | 既定値 | 説明 |
| --- | --- | --- | --- | --- |
| id | BIGINT | NO | AUTO_INCREMENT | 主キー |
| code | VARCHAR(50) | NO |  | 会社コード |
| name | VARCHAR(100) | NO |  | 会社名 |
| is_deleted | BOOLEAN | NO | FALSE | 論理削除フラグ |
| created_at | DATETIME(6) | NO | CURRENT_TIMESTAMP(6) | 作成日時 |
| updated_at | DATETIME(6) | NO | CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6) | 更新日時 |

制約:

- `PRIMARY KEY (id)`
- `UNIQUE KEY uq_companies_code (code)`

### departments

会社内の部署を表します。

| カラム | 型 | NULL | 既定値 | 説明 |
| --- | --- | --- | --- | --- |
| id | BIGINT | NO | AUTO_INCREMENT | 主キー |
| company_id | BIGINT | NO |  | 所属会社ID |
| code | VARCHAR(50) | NO |  | 部署コード |
| name | VARCHAR(100) | NO |  | 部署名 |
| is_deleted | BOOLEAN | NO | FALSE | 論理削除フラグ |
| created_at | DATETIME(6) | NO | CURRENT_TIMESTAMP(6) | 作成日時 |
| updated_at | DATETIME(6) | NO | CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6) | 更新日時 |

制約:

- `PRIMARY KEY (id)`
- `UNIQUE KEY uq_departments_company_code (company_id, code)`
- `CONSTRAINT fk_departments_company FOREIGN KEY (company_id) REFERENCES companies (id)`

### users

会社に所属するユーザーを表します。

| カラム | 型 | NULL | 既定値 | 説明 |
| --- | --- | --- | --- | --- |
| id | BIGINT | NO | AUTO_INCREMENT | 主キー |
| company_id | BIGINT | NO |  | 所属会社ID |
| department_id | BIGINT | YES |  | 所属部署ID |
| login_id | VARCHAR(100) | NO |  | ローカル疑似認証で使うログインID |
| name | VARCHAR(100) | NO |  | ユーザー名 |
| email | VARCHAR(255) | NO |  | メールアドレス |
| is_deleted | BOOLEAN | NO | FALSE | 論理削除フラグ |
| created_at | DATETIME(6) | NO | CURRENT_TIMESTAMP(6) | 作成日時 |
| updated_at | DATETIME(6) | NO | CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6) | 更新日時 |

制約:

- `PRIMARY KEY (id)`
- `UNIQUE KEY uq_users_company_login_id (company_id, login_id)`
- `UNIQUE KEY uq_users_company_email (company_id, email)`
- `CONSTRAINT fk_users_company FOREIGN KEY (company_id) REFERENCES companies (id)`
- `CONSTRAINT fk_users_department FOREIGN KEY (department_id) REFERENCES departments (id)`

### permission_sets

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
| updated_at | DATETIME(6) | NO | CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6) | 更新日時 |

制約:

- `PRIMARY KEY (id)`
- `UNIQUE KEY uq_permission_sets_code (code)`

### user_permission_sets

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
