# M2-01 Flyway migration structure

## 目的

MVP の DB schema を Flyway で再現できる migration 構成を作る。

## 対応範囲

- `apps/server/src/main/resources/db/migration/` を作成する
- MVP 初期 DDL 用の `V1__create_mvp_schema.sql` を作成する
- MySQL 9.7.0 向けの DDL として作成する
- M2 で作る全テーブルの DDL を1つの migration にまとめる
- Flyway が local profile 起動時に migration を実行できることを確認する

## 対応ファイル

- `apps/server/src/main/resources/db/migration/V1__create_mvp_schema.sql`
- `docs/implementation/mvp/agent-tasks/M2-01-flyway-migration-structure.md`

## 除外範囲

- seed SQL
- Mapper
- Service
- Controller
- Thymeleaf 画面
- AWS RDS 接続
- 既存 DB の移行

## 完了条件

空の local MySQL に対して Spring Boot local profile を起動すると、Flyway が `V1__create_mvp_schema.sql` を適用できる。

## 確認方法

- `docker compose down -v` で local DB を初期化する
- `docker compose up -d workops-mysql` で MySQL を起動する
- `cd apps/server && .\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=local"` で起動する
- Flyway migration が成功することを確認する

## 実装時の記録

実装後に、次をこのファイルへ追記する。

- 実装方針
- 変更ファイル
- 確認結果
- 残課題
