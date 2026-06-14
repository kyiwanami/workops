# P2-2-03 Flyway Profile / Seed Split

## 目的

AWS dev の RDS for MySQL 8.4 LTS に対して、`apps/web` 起動時 Flyway で共通 schema と AWS dev seed を適用できるように Spring profile と Flyway locations を整理する。

## ユーザー要求

- P2-2 の実装用 task Markdown を作成する
- 今回はアプリ実装そのものは行わない
- 現行 Phase 2 では `apps/web` の Spring Boot 起動時 Flyway 実行を維持する
- migration 用 ECS Task 分離は Phase 2α で扱う
- 環境変数名は既存のアプリ構成に従う
- 先に提案した `WORKOPS_DB_HOST` / `WORKOPS_DB_PORT` / `WORKOPS_DB_NAME` は使わない
- AWS dev JDBC URL は `useSSL=true` とする
- local と AWS dev の seed は分けるが、認証に依存しない seed は使い回す
- 初期 task Markdown には `実装時の記録` テンプレ欄を入れない

## P2-2 共通方針

- MySQL はローカル、Testcontainers、RDS for MySQL のすべてで 8.4 LTS に固定する
- MySQL 8.0 と 9.x は採用しない
- Phase 2 の AWS dev Flyway migration は `apps/web` の Spring Boot 起動時実行を維持する
- Phase 2α で migration 用 ECS Task へ分離し、AWS dev の Web アプリ起動時 Flyway 自動実行を止める
- ローカルでは Spring Boot 起動時の Flyway 実行を維持する
- Git commit は実行しない

## 案

- 既存の migration / seed SQL を確認し、schema と seed の責務を分ける
- 全環境共通 schema は `db/migration` に置く
- 認証非依存 seed は `db/seed/common` に置く
- local 認証依存 seed は `db/seed/local` に置く
- AWS dev 認証依存 seed は `db/seed/aws-dev` に置く
- `application-local.yml` と `application-dev.yml` で `spring.flyway.locations` を明示する
- 既存環境変数 `WORKOPS_DB_URL`、`WORKOPS_DB_USERNAME`、`WORKOPS_DB_PASSWORD` を使う
- AWS dev seed では実 Cognito `sub` を固定しない
- local と AWS dev の user seed は、排他 locations 上の同じ `V6__insert_users.sql` として管理する
- P2-5 以降で実 Cognito `sub` と `users.cognito_sub` を突合する

## ADR

- Phase 2 では Web アプリ起動時 Flyway を維持する。現行Webアプリ構成を優先し、migration用ECS Task分離はCI/CD AWSネイティブ化のPhase 2αに合わせるため
- `WORKOPS_DB_URL`、`WORKOPS_DB_USERNAME`、`WORKOPS_DB_PASSWORD` を使う。既存アプリ構成に合わせ、DB接続設定の入口を増やさないため
- seed は `common`、`local`、`aws-dev` に分ける。認証方式に依存しないデータは共通化し、local疑似ユーザーとAWS devのCognito前提データを混ぜないため
- Flyway の実行順は locations の列挙順ではなく version 順で制御する。locations は migration 探索場所であり、versioned migration は version 順に適用されるため
- Flyway version番号は active locations 内で重複させない。`db/seed/local/V6__insert_users.sql` と `db/seed/aws-dev/V6__insert_users.sql` は同じ意味の環境別 seed であり、同一 profile で同時に読まないため許容する
- Repeatable migration は採用しない。checksum変更時の再実行と冪等SQLが必要になり、今回の固定ID seed分割には過剰であるため
- AWS dev seed で実 Cognito `sub` を固定しない。実Cognitoユーザー作成と突合はP2-5以降の責務であるため
- P2-2では CA bundle、trust store、VERIFY_IDENTITY 相当の厳密なサーバー証明書検証までは入れない。AWS dev の最小接続確認では `useSSL=true` までに留めるため

## 対応範囲

- 既存 `src/main/resources/db` 配下の migration / seed SQL を確認する
- schema DDL を全環境共通の `db/migration` として維持する
- 認証非依存 seed を `db/seed/common` に分ける
- local 認証依存 seed を `db/seed/local` に分ける
- AWS dev 認証依存 seed を `db/seed/aws-dev` に分ける
- `application-local.yml` の `spring.flyway.locations` を local 用に明示する
- `application-dev.yml` を追加または更新する
- `application-dev.yml` で `WORKOPS_DB_URL`、`WORKOPS_DB_USERNAME`、`WORKOPS_DB_PASSWORD` を参照する
- `application-dev.yml` で AWS dev 用 `spring.flyway.locations` を明示する
- AWS dev JDBC URL は SSM Parameter の `/workops/${stage}/db/url` と整合させる
- AWS dev JDBC URL は `useSSL=true&serverTimezone=Asia/Tokyo` を含める
- local JDBC URL は既存の local 接続方針を維持する
- AWS dev seed で `users` を作成する
- AWS dev seed の `users.cognito_sub` は NULL にする
- AWS dev seed に PLATFORM_ADMIN 相当の確認ユーザーを 1 件入れる
- AWS dev seed に TENANT_MANAGER 相当の確認ユーザーをサンプル会社ごとに必要最小限入れる
- AWS dev seed に TENANT_MEMBER 相当の確認ユーザーを必要最小限入れる
- 既存 V2 から V5 の seed 内容は、実装時に内容を見て `common` / `local` / `aws-dev` へ分解する
- Flyway version番号は local profile と dev profile の active locations それぞれで `V1` から `V8` まで連続させる
- `db/seed/local/V6__insert_users.sql` と `db/seed/aws-dev/V6__insert_users.sql` は同時に読み込まない
- Testcontainers MySQL image が `mysql:8.4` であることを確認する
- Docker Compose MySQL image が `mysql:8.4` であることを確認する
- local と Testcontainers で既存テストが通る構成を維持する

## 対応ファイル

- `apps/web/src/main/resources/application-local.yml`
- `apps/web/src/main/resources/application-dev.yml`
- `apps/web/src/main/resources/db/migration/*.sql`
- `apps/web/src/main/resources/db/seed/common/*.sql`
- `apps/web/src/main/resources/db/seed/local/*.sql`
- `apps/web/src/main/resources/db/seed/aws-dev/*.sql`
- `apps/web/src/test/java/com/example/workops/integration/MapperIntegrationTestBase.java`
- `compose.yaml`
- `docs/implementation/phase2/agent-tasks/P2-2/P2-2-03-flyway-profile-seed-split.md`

## 除外範囲

- migration 用 ECS Task
- migration 専用 image
- migration 専用 Security Group
- AWS dev の `spring.flyway.enabled=false`
- SQL 所有の一本化判断
- Cognito user 作成
- 実 Cognito `sub` の seed 固定
- Cognito `sub` と `users.cognito_sub` の実環境突合
- 初回ログイン時の `users` 自動作成
- P2-5 以降の Hosted UI ログイン
- IAM DB authentication
- Kerberos authentication
- CA bundle
- trust store
- VERIFY_IDENTITY 相当の厳密なサーバー証明書検証
- MySQL 8.0
- MySQL 9.x
- 既存データ移行
- 後方互換性対応
- git commit

## 完了条件

- local、Testcontainers、RDS for MySQL の前提が MySQL 8.4 LTS で揃っている
- `application-local.yml` が local 用 Flyway locations を明示している
- `application-dev.yml` が AWS dev 用 DB 接続設定と Flyway locations を明示している
- `application-dev.yml` が `WORKOPS_DB_URL`、`WORKOPS_DB_USERNAME`、`WORKOPS_DB_PASSWORD` を使っている
- `WORKOPS_DB_HOST`、`WORKOPS_DB_PORT`、`WORKOPS_DB_NAME` を新設していない
- `db/seed/common`、`db/seed/local`、`db/seed/aws-dev` の責務が分かれている
- local profile と dev profile の active locations がそれぞれ `V1` から `V8` まで連続している
- `db/seed/local` と `db/seed/aws-dev` が同一 profile で同時に読み込まれていない
- AWS dev seed の `users.cognito_sub` が実 Cognito `sub` 固定値を持っていない
- local の既存テストが新しい locations 構成で成立する
- P2-3 で `apps/web` 起動時に RDS へ migration / seed を適用できる構成になっている

## 確認方法

エージェント確認:

- `Select-String` で `mysql:9`、`mysql:8.0` が現行構成に残っていないことを確認する
- `Select-String` で `WORKOPS_DB_HOST`、`WORKOPS_DB_PORT`、`WORKOPS_DB_NAME` を新設していないことを確認する
- local profile と dev profile の active locations がそれぞれ `V1` から `V8` まで連続していることを確認する
- `db/seed/aws-dev` に固定 UUID の `cognito_sub` がないことを確認する
- `./mvnw -pl apps/web test` またはリポジトリ既存のテストコマンドを実行する
- `git diff --check`

ユーザー確認:

- local profile で `apps/web` を起動し、local seed が適用されることを確認する
- P2-3 で AWS dev profile の `apps/web` をECS起動し、RDS上の `flyway_schema_history` を確認する
- P2-3 で AWS dev seed の主要ユーザー、会社、権限データを SELECT して確認する
