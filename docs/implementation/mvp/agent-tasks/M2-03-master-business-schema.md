# M2-03 master / business schema

## 目的

MVP のマスタ管理、申請管理、資産管理、監査ログで使う業務テーブルを DDL として作る。

## 対応範囲

- `common_master` テーブルを作成する
- `common_master_values` テーブルを作成する
- `generic_master` テーブルを作成する
- `generic_master_values` テーブルを作成する
- `requests` テーブルを作成する
- `request_histories` テーブルを作成する
- `assets` テーブルを作成する
- `asset_status_histories` テーブルを作成する
- `audit_logs` テーブルを作成する
- 会社別データは `company_id` で会社境界を表現する

## 対応ファイル

- `apps/server/src/main/resources/db/migration/V1__create_mvp_schema.sql`
- `docs/implementation/mvp/agent-tasks/M2-03-master-business-schema.md`

## 除外範囲

- seed SQL
- 申請サンプルデータ
- 資産サンプルデータ
- 申請履歴サンプルデータ
- 資産ステータス履歴サンプルデータ
- 監査ログサンプルデータ
- Mapper
- Service
- Controller
- Thymeleaf 画面

## 完了条件

M4 以降の申請管理、M5 以降の資産管理、M6 以降のマスタ管理、M7 以降の監査ログで使うテーブルが `V1__create_mvp_schema.sql` に定義されている。

## 確認方法

- Flyway migration を実行する
- MySQL 上に対象9テーブルが存在することを確認する
- 会社境界が必要なテーブルに `company_id` が存在することを確認する

## 実装時の記録

実装後に、次をこのファイルへ追記する。

- 実装方針
- 変更ファイル
- 確認結果
- 残課題
