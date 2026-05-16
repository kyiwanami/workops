# M9-01 PLATFORM_ADMIN local 前提

## 目的

M9 の会社作成と部署管理を実装する前提として、local profile で PLATFORM_ADMIN 疑似ユーザーを扱えるようにする。

## M9 共通方針

- M9 の範囲は、会社作成、部署管理、会社作成時の会社別 `generic_master_values` 初期投入までとする
- 初期 TENANT_MANAGER 作成、ユーザー作成、権限割当・変更、local Cognito Fake Bean、疑似 `cognito_sub` 発行は M10 で扱う
- ユーザー情報は既存の `users` テーブルを使い、別のユーザーテーブルは作らない
- `users.actor_type = 'PLATFORM'` のユーザーは `company_id = NULL` とする
- `users.actor_type = 'TENANT'` のユーザーは `company_id` 必須とする
- `PLATFORM` / `TENANT` は `users.actor_type` で区別する
- Cognito 本物 API 呼び出し、Cognito Hosted UI ログインの本格確認、PLATFORM / TENANT App Client 分離は M9 で扱わない
- Git commit は実行しない

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

実装後に、次をこのファイルへ追記する。

- 実装方針
- 変更ファイル
- 実装結果
- 確認結果
- 残課題
