# M9-01 PLATFORM_ADMIN local 前提

## 目的

M9 の会社作成と部署管理を実装する前提として、local profile で PLATFORM_ADMIN 疑似ユーザーを扱えるようにする。

## M9 共通方針

- M9 の範囲は、会社一覧・詳細・作成・編集・論理削除、削除済み会社の表示、部署一覧・作成・編集・論理削除、削除済み部署の表示、会社作成時の会社別 `generic_master_values` 初期投入までとする
- 会社削除は物理削除せず、`companies.is_deleted = TRUE` の論理削除として扱う
- 削除済み会社コードは再利用しない
- 会社を論理削除しても、部署、ユーザー、申請、資産、会社別マスタ値などの配下データは物理削除しない
- 配下データが存在していても、警告表示したうえで会社の論理削除は可能とする
- 削除済み会社は通常の新規作成・選択導線には出さず、管理画面では削除済みであることを区別して表示する
- 初期 TENANT_MANAGER 作成、ユーザー作成、権限割当・変更、local Cognito Fake Bean、疑似 `cognito_sub` 発行は M10 で扱う
- ユーザー情報は既存の `users` テーブルを使い、別のユーザーテーブルは作らない
- `users.actor_type = 'PLATFORM'` のユーザーは `company_id = NULL` とする
- `users.actor_type = 'TENANT'` のユーザーは `company_id` 必須とする
- `PLATFORM` / `TENANT` は `users.actor_type` で区別する
- Cognito 本物 API 呼び出し、Cognito Hosted UI ログインの本格確認、PLATFORM / TENANT App Client 分離は M9 で扱わない

## 対応範囲

- `users.company_id` が `PLATFORM` ユーザーでは `NULL`、`TENANT` ユーザーでは必須になるように DB 定義を調整する
- `permission_sets` に `PLATFORM_ADMIN` seed を追加する
- local profile 用の PLATFORM_ADMIN 疑似ユーザー seed を追加する
- PLATFORM_ADMIN 疑似ユーザーには `PLATFORM_ADMIN` 権限セットを割り当てる
- local profile で `WORKOPS_LOCAL_COGNITO_SUB` を使い、PLATFORM_ADMIN 疑似ユーザーを選択できるようにする
- DB 由来の `LoginUserContext` と `GrantedAuthority` が PLATFORM_ADMIN 疑似ユーザーでも作成されるようにする
- TENANT ユーザーでは `company_id` が必須であることを Service または Mapper 境界で崩さない
- `LoginUserContext` が PLATFORM ユーザーの `companyId = null` を扱えることを確認する
- `@PreAuthorize("hasAuthority('PLATFORM_ADMIN')")` を使う M9 以降の管理操作に入れる前提を作る
- DB 定義書は、M9 実装時に変更した DB 正本と一致するように更新する

## 対応ファイル

- `apps/web/src/main/resources/db/migration/*.sql`
- `apps/web/src/main/java/com/example/workops/common/security/*.java`
- `apps/web/src/main/java/com/example/workops/common/security/mapper/*.java`
- `apps/web/src/main/resources/mapper/common/*.xml`
- `apps/web/src/test/java/com/example/workops/common/security/**/*.java`
- `docs/implementation/db/schema.md`
- `docs/implementation/mvp/agent-tasks/M9/M9-01-platform-admin-local-prerequisite.md`

## 除外範囲

- 会社作成画面
- 部署管理画面
- 初期 TENANT_MANAGER 作成
- ユーザー作成
- 権限割当・変更画面
- local Cognito Fake Bean
- 疑似 `cognito_sub` 発行コンポーネント
- Cognito 本物 API 呼び出し
- Cognito Hosted UI ログインの本格確認
- PLATFORM / TENANT App Client 分離
- 別ユーザーテーブルの作成

## 完了条件

local profile で PLATFORM_ADMIN 疑似ユーザーを現在ユーザーとして扱える。
PLATFORM_ADMIN 疑似ユーザーは `users.actor_type = 'PLATFORM'`、`users.company_id = NULL`、`permission_sets.code = 'PLATFORM_ADMIN'` として表現される。
既存の TENANT_VIEWER / TENANT_EDITOR / TENANT_MANAGER 疑似ユーザーは従来どおり `company_id` を持ち、既存の申請、資産、マスタ画面を利用できる。

## 確認方法

- `cd apps/web && .\mvnw.cmd test`
- local profile の PLATFORM_ADMIN 疑似ユーザーで `/auth/claims` が HTTP 200 を返すことを確認する
- `/auth/claims` で `actorType = PLATFORM`、`companyId = null`、`authorities` に `PLATFORM_ADMIN` が含まれることを確認する
- local profile の TENANT_MANAGER 疑似ユーザーで `/auth/claims` が HTTP 200 を返すことを確認する
- TENANT_MANAGER 疑似ユーザーで既存の `/requests`、`/assets`、`/masters/request-types`、`/masters/asset-categories` が HTTP 200 を返すことを確認する
- Testcontainers MySQL 上で PLATFORM_ADMIN seed と TENANT 既存 seed が成立することを確認する

## 実装時の記録

### 実装方針

- 既存の `users` テーブルを正本として使い、PLATFORM / TENANT の区別は `users.actor_type` に集約した
- PLATFORM ユーザーは `users.company_id = NULL`、TENANT ユーザーは `users.company_id` 必須とする DB 制約を追加した
- local profile の現在ユーザー解決は既存の `LocalAuthenticationFilter` と `WORKOPS_LOCAL_COGNITO_SUB` を使い、Fake Bean や疑似 `cognito_sub` 発行は追加しなかった
- `PLATFORM_ADMIN` 権限セットと local PLATFORM_ADMIN 疑似ユーザーを Flyway seed として追加した
- `/auth/claims` は PLATFORM ユーザーの `companyId = null` を画面上で確認できるようにした

### 変更ファイル

- `apps/web/src/main/resources/db/migration/V5__platform_admin_local_prerequisite.sql`
- `apps/web/src/main/resources/templates/auth/claims.html`
- `apps/web/src/test/java/com/example/workops/integration/DatabaseConstraintIntegrationTests.java`
- `apps/web/src/test/java/com/example/workops/integration/SeedMigrationIntegrationTests.java`
- `docs/implementation/db/schema.md`
- `docs/implementation/mvp/agent-tasks/M9/M9-01-platform-admin-local-prerequisite.md`

### 実装結果

- `users.company_id` を nullable に変更し、`ck_users_actor_company` で次を強制した
  - `actor_type = 'PLATFORM'` の場合は `company_id IS NULL`
  - `actor_type = 'TENANT'` の場合は `company_id IS NOT NULL`
- `permission_sets` に `PLATFORM_ADMIN` を追加した
- `users` に local PLATFORM_ADMIN 疑似ユーザーを追加した
  - `id = 7`
  - `cognito_sub = '00000000-0000-0000-0000-000000000000'`
  - `username = 'platform-admin'`
  - `actor_type = 'PLATFORM'`
  - `company_id = NULL`
- `user_permission_sets` で PLATFORM_ADMIN 疑似ユーザーへ `PLATFORM_ADMIN` を割り当てた
- Testcontainers MySQL 上で PLATFORM_ADMIN seed と actor/company 制約を検証した

### 確認結果

- `cd apps/web && .\mvnw.cmd test`
  - `Tests run: 100, Failures: 0, Errors: 0, Skipped: 0`
- `WORKOPS_LOCAL_COGNITO_SUB=00000000-0000-0000-0000-000000000000` で local 起動し、`/auth/claims` が HTTP 200 を返すことを確認した
- PLATFORM_ADMIN の `/auth/claims` で `actorType=PLATFORM`、`companyId=null`、`PLATFORM_ADMIN` が表示されることを確認した
- `WORKOPS_LOCAL_COGNITO_SUB=00000000-0000-0000-0000-000000000003` で local 起動し、TENANT_MANAGER の `/auth/claims` が HTTP 200 を返すことを確認した
- TENANT_MANAGER で次の既存画面が HTTP 200 を返すことを確認した
  - `/requests`
  - `/assets`
  - `/masters/request-types`
  - `/masters/asset-categories`

### 残課題

- 初期 TENANT_MANAGER 作成、ユーザー作成、権限割当・変更、local Cognito Fake Bean、疑似 `cognito_sub` 発行は M10 で扱う
- Cognito 本物 API 呼び出し、Cognito Hosted UI ログインの本格確認、PLATFORM / TENANT App Client 分離は M9 では扱わない
