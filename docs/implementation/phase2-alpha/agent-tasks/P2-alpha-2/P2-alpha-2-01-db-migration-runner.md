# P2-alpha-2-01 DB migration runner

## 目的

`apps/web` 起動時 Flyway を廃止し、root `db/` を SQL 正本とする Flyway CLI 専用 migration image と local one-shot migration 経路を作る。

## ユーザー要求

- P2-alpha-2 の task 分割は、`phases.md` の責務境界を最上位単位にする
- 既にコード、Notion、`phases.md` で分かることは質問しない
- 必要な場合は公式ドキュメントやベストプラクティスを確認する
- task ファイル末尾に `実装時の記録`、`実装結果`、`確認結果`、`残課題` を置く

## 前提

- P2-alpha-1 が完了している
- Phase 2α では Flyway 実行方式を CLI に統一する
- 現行 SQL は `apps/web/src/main/resources/db/` 配下にある
- 現行 `compose.yaml` は MySQL service のみである
- `apps/web/pom.xml` には Spring Boot Flyway 依存が残っている
- 既存データ移行と後方互換性は考慮しない

## 案

root に次の SQL 正本を作る。

```text
db/
  migration/
  seed/
    common/
    local/
    aws-dev/
```

既存の `apps/web/src/main/resources/db/migration`、`db/seed/common`、`db/seed/local`、`db/seed/aws-dev` を root `db/` へ移す。

Flyway CLI 専用 migration image を追加し、image 内には root `db/` の SQL を含める。
実行時は `WORKOPS_FLYWAY_LOCATIONS` で locations を切り替える。

```text
local:
  filesystem:/flyway/sql/migration
  filesystem:/flyway/sql/seed/common
  filesystem:/flyway/sql/seed/local

aws-dev:
  filesystem:/flyway/sql/migration
  filesystem:/flyway/sql/seed/common
  filesystem:/flyway/sql/seed/aws-dev
```

migration container は次の環境変数を受け取る。

- `WORKOPS_DB_URL`
- `WORKOPS_DB_USERNAME`
- `WORKOPS_DB_PASSWORD`
- `WORKOPS_FLYWAY_LOCATIONS`

`FLYWAY_USER`、`FLYWAY_PASSWORD` は使わず、entrypoint から `flyway -url`、`-user`、`-password`、`-locations` へ明示的に渡す。

local では `docker compose run --rm migration` を migration 実行経路にする。
通常の `docker compose up` で migration service を常駐起動しない。

`apps/web` から Spring Boot Flyway 実行依存を削除する。
既存の Flyway 関連テストは、apps/web 起動時 Flyway ではなく root `db/` SQL を Flyway CLI 相当で適用できることの確認へ寄せる。

## 判断メモ

- SQL 正本を root `db/` へ移す。migration image と web app の責務を分けるため。
- Flyway CLI 専用 image に閉じる。AWS dev と local で同じ migration 実行方式にするため。
- apps/web 起動時 Flyway は残さない。Pipeline 内の独立 migration stage で成否を判定するため。
- migration 専用 DB user、secret、Security Group は作らない。`phases.md` の Notion へ戻す判断に該当するため。

## 対応範囲

- root `db/`
- 既存 SQL の移動
- migration image 用 Dockerfile / entrypoint
- `compose.yaml` の migration one-shot service
- `apps/web/pom.xml` の Flyway 依存整理
- `application-*.yml` の Flyway 設定整理
- Flyway 関連テストの置き換え
- `.dockerignore` / `.gitignore` の必要最小限の調整
- 本 task ファイルの実装結果・確認結果追記

## 除外範囲

- MigrationStack
- Migration RunTask buildspec
- PipelineStack
- Build Images stage
- AWS deploy
- CodePipeline 実走
- migration 専用 DB user / secret / Security Group
- VPC Endpoint

## 完了条件

- root `db/` が SQL 正本になっている
- `apps/web/src/main/resources/db/` に SQL 正本が残っていない
- migration image が Flyway CLI 専用になっている
- `docker compose run --rm migration` で local MySQL へ migration を実行できる構成になっている
- apps/web 起動時 Flyway が廃止されている
- Flyway 実行に必要な DB 接続環境変数が Web と揃っている
- 既存の Flyway 関連テストが新しい SQL 正本に追従している

## 確認方法

```powershell
cd C:\git\workops
docker compose -p workops-p2alpha-migration-test build migration
docker compose -p workops-p2alpha-migration-test up -d workops-mysql
docker compose -p workops-p2alpha-migration-test run --rm migration
docker compose -p workops-p2alpha-migration-test down -v

cd C:\git\workops\apps\web
.\mvnw.cmd test
.\mvnw.cmd spotless:check
.\mvnw.cmd compile spotbugs:check
.\mvnw.cmd verify
```

既存 local DB volume を壊さないため、migration 動作確認は検証用 compose project で行う。

通常の local migration は次で実行する。

```powershell
cd C:\git\workops
docker compose run --rm migration
```

## 実装時の記録

### 実装結果

- SQL 正本を root `db/` へ移動した。
  - `db/migration/`
  - `db/seed/common/`
  - `db/seed/local/`
  - `db/seed/aws-dev/`
- `infra/docker/migration/` に Flyway CLI 専用 image を追加した。
  - base image は `flyway/flyway:12.9.0-alpine`
  - MySQL Connector/J は `9.7.0`
  - root `db/` を `/flyway/sql/` に含める
  - entrypoint で `WORKOPS_DB_URL` / `WORKOPS_DB_USERNAME` / `WORKOPS_DB_PASSWORD` / `WORKOPS_FLYWAY_LOCATIONS` を必須にして `flyway migrate` を実行する
- `compose.yaml` に one-shot `migration` service を追加した。
  - `profiles: ["migration"]`
  - local locations は `migration`、`seed/common`、`seed/local`
  - `workops-mysql` の healthcheck 完了後に実行する
- `compose.yaml` の `workops-mysql` から固定 `container_name` を外した。
  - `-p workops-p2alpha-migration-test` の検証用 project が既存 local container と衝突しないようにするため。
- `apps/web` から Spring Boot Flyway 実行経路を削除した。
  - `spring-boot-starter-flyway`
  - `flyway-mysql`
  - `spring-boot-starter-flyway-test`
  - `application*.yml` の `spring.flyway.*`
- Mapper 統合テストは、Testcontainers MySQL 起動後に root `db/` の SQL を Spring JDBC script utility で適用する形へ変更した。
- `SeedMigrationIntegrationTests` は `flyway_schema_history` 前提をやめ、root SQL の適用順と seed 結果を検証する形へ変更した。
- migration build context 用に root `.dockerignore` を追加した。

### 確認結果

- `cd C:\git\workops\apps\web; .\mvnw.cmd test`
  - 成功。Tests run: 387, Failures: 0, Errors: 0, Skipped: 0
- `cd C:\git\workops; docker compose -p workops-p2alpha-migration-test build migration`
  - 成功。`flyway/flyway:12.9.0-alpine` tag は取得可能だった。
- `docker compose -p workops-p2alpha-migration-test up -d workops-mysql`
  - 成功。
- `docker compose -p workops-p2alpha-migration-test run --rm migration`
  - 成功。Flyway CLI 12.9.0 が 8 migrations を v8 まで適用した。
- `docker compose -p workops-p2alpha-migration-test down -v`
  - 成功。検証用 volume を削除した。
- `cd C:\git\workops\apps\web; .\mvnw.cmd spotless:check`
  - 成功。
- `cd C:\git\workops\apps\web; .\mvnw.cmd compile spotbugs:check`
  - 成功。SpotBugs BugInstance size 0 / Error size 0。
- `cd C:\git\workops\apps\web; .\mvnw.cmd verify`
  - 成功。Tests run: 387, Failures: 0, Errors: 0, Skipped: 0。JaCoCo coverage check も成功。

### 残課題

- AWS dev の ECS RunTask、MigrationStack、PipelineStack 連携は本 task の除外範囲として後続 task で扱う。
