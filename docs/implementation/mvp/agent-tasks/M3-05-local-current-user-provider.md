# M3-05 LocalCurrentUserProvider

## 目的

local profile でCognito claimを疑似再現せず、ローカル確認用に選択したDBユーザーから `LoginUserContext` を作る。

## 対応範囲

- `LocalCurrentUserProvider` を作成する
- local profile でのみ有効にする
- local user key で `users` を取得する
- `users` / `user_permission_sets` / `permission_sets` からDB由来情報を取得する
- TENANT_VIEWER / TENANT_EDITOR / TENANT_MANAGER の確認ユーザーを切り替えられるようにする
- 北浜精密機器株式会社 / 青葉ケアサービス株式会社のユーザーを切り替えられるようにする

## 対応ファイル

- `apps/server/src/main/java/com/example/workops/common/security/`
- `apps/server/src/main/java/com/example/workops/master/mapper/`
- `apps/server/src/main/resources/mapper/master/UserAccountMapper.xml`
- `docs/implementation/mvp/agent-tasks/M3-05-local-current-user-provider.md`

## 除外範囲

- Cognito claim の疑似再現
- Cognito Hosted UI 接続
- Service 層認可チェック
- ユーザー管理画面
- 権限割当画面

## 完了条件

local profile で、選択したseedユーザーからDB由来の `LoginUserContext` を取得できる。

## 確認方法

- local profile 起動時に `TENANT_VIEWER` / `TENANT_EDITOR` / `TENANT_MANAGER` 相当のユーザーを切り替える
- `company_id = 1` / `company_id = 2` のユーザーを切り替える
- `LoginUserContext` がCognito接続時と同じ構造で返ることを確認する

## 実装時の記録

### 実装方針

- local profile では `WORKOPS_LOCAL_COGNITO_SUB` で指定した固定UUID形式の `cognito_sub` を使い、DBの `users` と突合する。
- `WORKOPS_LOCAL_COGNITO_SUB` 未指定時は `00000000-0000-0000-0000-000000000003` を使う。
- `users.cognito_sub` は論理的にはUUIDとして扱い、MySQLの物理型は `CHAR(36)` とする。
- `users` は `cognito_sub` の一意制約で1件取得し、権限セットは `user_id` を使う別SQLで取得する。
- 権限セットは文字列結合で潰さず、`code` / `name` を持つDTOとして `LoginUserContext` に渡す。
- 画面表示でも権限セットを文字列結合せず、`LoginUserContext.permissionSets()` の各要素をそのまま表示する。
- MyBatisの取得行オブジェクトは setter 前提のクラスにせず、record と constructor mapping を使う。
- Lombok はM3-05では不要なため使わない。
- local seed の `cognito_sub` はローカルDB突合確認用であり、本物のCognito subではない。
- 実Cognito subをseedへ入れる場合は、別タスクでユーザー確認後に行う。
- `users.status` / `userStatus` は作らない。

### 変更ファイル

- `apps/server/src/main/resources/db/migration/V1__create_mvp_schema.sql`
- `apps/server/src/main/resources/db/migration/V2__insert_local_seed.sql`
- `apps/server/src/main/java/com/example/workops/common/security/`
- `apps/server/src/main/java/com/example/workops/master/mapper/`
- `apps/server/src/main/resources/mapper/master/UserAccountMapper.xml`
- `apps/server/src/main/resources/templates/auth/claims.html`
- `docs/implementation/db/schema.md`

### 確認結果

- `cd apps/server && .\mvnw.cmd test` が成功した。
- `docker compose down -v` から `docker compose up -d workops-mysql` でDBを再構築した。
- `flyway_schema_history` で V1 / V2 の成功を確認した。
- `users.cognito_sub` が `CHAR(36)` で作成されていることを確認した。
- local profile 起動で `/auth/claims` が `200` を返すことを確認した。
- `WORKOPS_LOCAL_COGNITO_SUB` 未指定時、`00000000-0000-0000-0000-000000000003` に紐づく `kthm-manager` / `TENANT_MANAGER` が表示されることを確認した。
- `WORKOPS_LOCAL_COGNITO_SUB=00000000-0000-0000-0000-000000000001` で `kthm-viewer` / `TENANT_VIEWER` が表示されることを確認した。
- `WORKOPS_LOCAL_COGNITO_SUB=00000000-0000-0000-0000-000000000002` で `kthm-editor` / `TENANT_EDITOR` が表示されることを確認した。
- profileなし起動で `/auth/claims` が `302` を返し、`/oauth2/authorization/cognito` へリダイレクトされることを確認した。
- 起動確認用Javaプロセスが残っていないことを確認した。

### 残課題

- 実Cognito sub と `users` の紐づけ確認は、ユーザー合意後の別タスクで扱う。
- Service層認可チェックは M3-06 で扱う。
