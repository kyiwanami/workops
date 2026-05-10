# M2-03 master / business schema

## 目的

MVP のマスタ管理、申請管理、資産管理で使う業務テーブルを DDL として作る。

## 対応範囲

- 既存5テーブルに `created_by` / `updated_by` を追加する
- `common_master` テーブルを作成する
- `common_master_values` テーブルを作成する
- `generic_master` テーブルを作成する
- `generic_master_values` テーブルを作成する
- `requests` テーブルを作成する
- `assets` テーブルを作成する
- 会社別データは `company_id` で会社境界を表現する
- テーブル分類と役割を `docs/implementation/db/schema.md` に記録する

## 対応ファイル

- `apps/web/src/main/resources/db/migration/V1__create_mvp_schema.sql`
- `docs/implementation/db/schema.md`
- `docs/implementation/mvp/phases.md`
- `docs/implementation/mvp/agent-tasks/M2-03-master-business-schema.md`

## 除外範囲

- seed SQL
- 申請サンプルデータ
- 資産サンプルデータ
- 申請履歴テーブル
- 資産ステータス履歴テーブル
- 監査ログテーブル
- 申請履歴サンプルデータ
- 資産ステータス履歴サンプルデータ
- 監査ログサンプルデータ
- Mapper
- Service
- Controller
- Thymeleaf 画面

## 完了条件

M4 以降の申請管理、M5 以降の資産管理、M6 以降のマスタ管理で使うテーブルが `V1__create_mvp_schema.sql` に定義されている。
`request_histories`、`asset_status_histories`、`audit_logs` が作成対象から外れている。

## 確認方法

- Flyway migration を実行する
- MySQL 上に M2-03 対象6テーブルが存在することを確認する
- MySQL 上に `request_histories` / `asset_status_histories` / `audit_logs` が存在しないことを確認する
- 会社境界が必要なテーブルに `company_id` が存在することを確認する
- 主要テーブルに `created_by` / `updated_by` が存在することを確認する

## 実装時の記録

### 実装方針

- Notion 側で合意された DB 設計方針変更に従い、物理テーブル名に `mst_` / `trn_` / `wrk_` 接頭辞は付けない
- テーブル分類は `docs/implementation/db/schema.md` のテーブル一覧に記録する
- 履歴テーブルと監査ログテーブルは作成しない
- 業務操作・調査用ログは RDB ではなくアプリケーションログへ出力する
- 有効期間管理は全体共通では入れない
- `created_by` / `updated_by` は M2 時点では外部キーを張らない
- `generic_master` は会社別ではなく汎用マスタ種別を表す
- `generic_master_values` に `company_id` を持たせ、会社別の値を表す
- `requests` は現在状態を持つトランザクションとして定義し、物理削除しないため `is_deleted` を持たせない
- `assets` は業務マスタ / 台帳として定義し、論理削除用の `is_deleted` を持たせる
- MVP で不要な資産詳細項目、利用者、設置場所、メーカー、シリアル番号、申請種別値、理由、完了日時は M2-03 の物理カラムに含めない

### 変更ファイル

- `apps/web/src/main/resources/db/migration/V1__create_mvp_schema.sql`
- `docs/implementation/db/schema.md`
- `docs/implementation/mvp/phases.md`
- `docs/implementation/mvp/agent-tasks/M2-03-master-business-schema.md`

### 確認結果

- `docker compose down -v` で local DB を初期化した
- `docker compose up -d workops-mysql` で MySQL を起動した
- `docker compose ps` で `workops-mysql` が `healthy` であることを確認した
- `cd apps/web && .\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=local"` で Flyway migration が成功することを確認した
- `flyway_schema_history` に `V1__create_mvp_schema.sql` が success として登録されたことを確認した
- MySQL 上に `common_master` / `common_master_values` / `generic_master` / `generic_master_values` / `requests` / `assets` が存在することを確認した
- `generic_master` に `company_id` が存在しないことを確認した
- `generic_master_values` に `company_id` が存在することを確認した
- MySQL 上に `request_histories` / `asset_status_histories` / `audit_logs` が存在しないことを確認した
- `assets` に `assigned_user_id` / `location_value_id` / `manufacturer_value_id` / `serial_number` が存在しないことを確認した
- `requests` に `request_type_value_id` / `reason` / `completed_at` が存在しないことを確認した
- 主要テーブルに `created_by` / `updated_by` が存在することを確認した
- 対象テーブルの主キー、一意制約、外部キーが存在することを確認した
- `cd apps/web && .\mvnw.cmd test` が成功した

### 残課題

- なし
