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

### 実装方針

- M2-01 単体では migration が薄くなるため、M2-02 とまとめて実装する
- `V1__create_mvp_schema.sql` に M2-02 対象の tenant / user / permission 系5テーブルを定義する
- MySQL 9.7.0 向けに `InnoDB`、`utf8mb4`、`utf8mb4_0900_ai_ci` を明示する
- 実行上の正本は Flyway migration SQL とし、人間が読むDB定義書を `docs/implementation/db/schema.md` に作成する
- seed、業務テーブル、Mapper、Service、Controller は作成しない

### 変更ファイル

- `apps/server/src/main/resources/db/migration/V1__create_mvp_schema.sql`
- `docs/implementation/db/schema.md`
- `docs/implementation/mvp/agent-tasks/M2-01-flyway-migration-structure.md`
- `docs/implementation/mvp/agent-tasks/M2-02-tenant-user-permission-schema.md`

### 確認結果

- `docker compose down -v` で local DB を初期化した
- `docker compose up -d workops-mysql` で MySQL を起動した
- `docker compose ps` で `workops-mysql` が `healthy` であることを確認した
- `cd apps/server && .\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=local"` で起動した
- `http://localhost:8080/` が HTTP 200 を返した
- `flyway_schema_history` に `V1__create_mvp_schema.sql` が success として登録されたことを確認した
- `cd apps/server && .\mvnw.cmd test` が成功した
- 起動確認用の Java プロセスが残っていないことを確認した

### 残課題

- なし
