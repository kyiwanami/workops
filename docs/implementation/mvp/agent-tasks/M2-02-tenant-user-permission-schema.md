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

- `apps/server/src/main/resources/db/migration/V1__create_mvp_schema.sql`
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

実装後に、次をこのファイルへ追記する。

- 実装方針
- 変更ファイル
- 確認結果
- 残課題
