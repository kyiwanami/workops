# M2-05 DB verification

## 目的

M2 で作成した Flyway DDL と local seed が、local MySQL 上で再現できることを確認する。

## 対応範囲

- `docker compose down -v` で local DB を初期化する
- `docker compose up -d workops-mysql` で MySQL を起動する
- Spring Boot local profile 起動で Flyway migration を実行する
- M2 対象テーブルの存在確認 SQL を記録する
- seed 件数確認 SQL を記録する
- 確認結果を agent task 文書に記録する

## 対応ファイル

- `docs/implementation/mvp/agent-tasks/M2-05-db-verification.md`
- `README.md`

## 除外範囲

- DDL 追加
- seed 追加
- Mapper
- Service
- Controller
- Thymeleaf 画面
- Testcontainers
- AWS RDS 接続

## 完了条件

README 手順で local DB を作り直し、Flyway で MVP テーブルと local seed を再現できる。

## 確認方法

- `docker compose down -v` を実行する
- `docker compose up -d workops-mysql` を実行する
- `docker compose ps` で `workops-mysql` が `healthy` であることを確認する
- `cd apps/web && .\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=local"` を実行する
- MySQL で M2 対象テーブルの存在を確認する
- MySQL で seed 件数を確認する

## 実装時の記録

### 実装方針

- README 手順で local DB を空から作り直し、Flyway の `V1` / `V2` が再現できることを確認する
- M2 対象テーブルの存在、作成しないテーブルの不存在、local seed 件数を SQL で確認する
- `generic_master` は会社別ではなく、`generic_master_values.company_id` で会社別値を表すことを確認する
- DDL、seed、Mapper、Service、Controller、画面は追加しない

### 変更ファイル

- `README.md`
- `docs/implementation/mvp/agent-tasks/M2-05-db-verification.md`

### 確認SQL

Flyway migration 履歴:

```sql
SELECT installed_rank, version, description, success
FROM flyway_schema_history
ORDER BY installed_rank;
```

M2 対象テーブル:

```sql
SELECT table_name
FROM information_schema.tables
WHERE table_schema = 'workops'
ORDER BY table_name;
```

作成しないテーブル:

```sql
SELECT table_name
FROM information_schema.tables
WHERE table_schema = 'workops'
  AND table_name IN ('request_histories', 'asset_status_histories', 'audit_logs')
ORDER BY table_name;
```

seed 件数:

```sql
SELECT 'companies' AS table_name, COUNT(*) AS row_count FROM companies
UNION ALL SELECT 'departments', COUNT(*) FROM departments
UNION ALL SELECT 'users', COUNT(*) FROM users
UNION ALL SELECT 'permission_sets', COUNT(*) FROM permission_sets
UNION ALL SELECT 'user_permission_sets', COUNT(*) FROM user_permission_sets
UNION ALL SELECT 'common_master', COUNT(*) FROM common_master
UNION ALL SELECT 'common_master_values', COUNT(*) FROM common_master_values
UNION ALL SELECT 'generic_master', COUNT(*) FROM generic_master
UNION ALL SELECT 'generic_master_values', COUNT(*) FROM generic_master_values
UNION ALL SELECT 'requests', COUNT(*) FROM requests
UNION ALL SELECT 'assets', COUNT(*) FROM assets;
```

generic master の会社スコープ:

```sql
SELECT table_name, column_name
FROM information_schema.columns
WHERE table_schema = 'workops'
  AND table_name IN ('generic_master', 'generic_master_values')
ORDER BY table_name, ordinal_position;

SELECT gm.code AS master_code, c.code AS company_code, gmv.code AS value_code
FROM generic_master gm
JOIN generic_master_values gmv ON gmv.generic_master_id = gm.id
JOIN companies c ON c.id = gmv.company_id
ORDER BY c.id, gmv.sort_order;
```

### 確認結果

- `docker compose down -v` で local DB を初期化した
- `docker compose up -d workops-mysql` で MySQL を起動した
- `docker compose ps` で `workops-mysql` が `healthy` であることを確認した
- `cd apps/web && .\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=local"` で `V1` / `V2` の Flyway migration が成功することを確認した
- `flyway_schema_history` に `V1__create_mvp_schema.sql` と `V2__insert_local_seed.sql` が success として登録されたことを確認した
- M2 対象テーブルが存在することを確認した
- `request_histories` / `asset_status_histories` / `audit_logs` が存在しないことを確認した
- `generic_master` に `company_id` が存在しないことを確認した
- `generic_master_values` に `company_id` が存在することを確認した
- `generic_master` が1件、`generic_master_values` が9件であることを確認した
- `requests` と `assets` が0件であることを確認した
- `cd apps/web && .\mvnw.cmd test` が成功した
- 起動確認用 Java プロセスが残っていないことを確認した

seed 件数:

| テーブル | 件数 |
| --- | ---: |
| companies | 2 |
| departments | 5 |
| users | 6 |
| permission_sets | 3 |
| user_permission_sets | 6 |
| common_master | 3 |
| common_master_values | 12 |
| generic_master | 1 |
| generic_master_values | 9 |
| requests | 0 |
| assets | 0 |

### 残課題

- なし
