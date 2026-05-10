# M3-07 Cognito Authentication

## 目的

Cognito OAuth2 Login 後も local profile と同じく、Spring Security の `Authentication` にDB由来の `LoginUserContext` と `GrantedAuthority` を保持する。

## 対応範囲

- Cognitoログイン成功後に `OidcUser.sub` を取得する
- `users.cognito_sub` でDBユーザーと突合する
- DB由来の `LoginUserContext` を `Authentication.principal` に設定する
- `permission_sets.code` を `GrantedAuthority` に設定する
- 未登録Cognito sub はアプリ利用不可として扱う
- `/auth/claims` と `/auth/authorization/manager` の確認導線を維持する

## 対応ファイル

- `apps/server/src/main/java/com/example/workops/common/security/CognitoAuthenticationSuccessHandler.java`
- `apps/server/src/main/java/com/example/workops/common/security/SecurityConfig.java`
- `docs/implementation/mvp/agent-tasks/M3-07-cognito-authentication.md`

## 除外範囲

- 実Cognito sub のFlyway seed投入
- Cognitoユーザー登録時の `users` 自動登録
- Cognito Trigger
- Pre Token Generation
- Cognito group / scope を使った業務認可
- 会社境界の業務SQL実装
- 申請管理、資産管理、マスタ管理の個別Service実装

## 完了条件

- Cognitoログイン後、`OidcUser.sub` からDBユーザーを取得できる
- 取得したDBユーザーを `Authentication.principal` の `LoginUserContext` として保持できる
- DBの権限セットを `GrantedAuthority` として保持できる
- `@PreAuthorize("hasAuthority('TENANT_MANAGER')")` がCognitoログイン後もDB由来のauthorityで判定できる
- 未登録Cognito sub はアプリ利用不可として扱われる

## 確認方法

- `cd apps/server && .\mvnw.cmd test`
- local profileで既存の `/auth/claims` と `/auth/authorization/manager` が動作することを確認する
- 実Cognitoユーザーの `sub` に対応する `users` 行を手動SQLで投入する
- profileなし起動でCognitoログインする
- `/auth/claims` にDB由来の `LoginUserContext` と `GrantedAuthority` が表示されることを確認する
- `/auth/authorization/manager` が `TENANT_MANAGER` authority で `200` になることを確認する

## 実装時の記録

### 実装方針

- Cognito Hosted UI の認証自体は Spring Security OAuth2 Login に任せる。
- ログイン成功時に `CognitoAuthenticationSuccessHandler` で `OidcUser.sub` を取得する。
- `DbLoginUserContextFactory.fromCognitoSub(sub)` で `users.cognito_sub` と突合する。
- 取得した `LoginUserContext` は `WorkOpsAuthenticationFactory` で `Authentication` に変換する。
- 変換後の `Authentication` を `SecurityContext` とHTTPセッションへ保存する。
- Cognito claim の `email` / `username` は業務利用しない。
- `profile` scope と `aws.cognito.signin.user.admin` scope は使わない。
- 実Cognito sub はリポジトリ管理のseedには入れず、疎通確認時にユーザーが手動SQLで投入する。

### 実Cognitoユーザー投入SQL

```sql
START TRANSACTION;

UPDATE users
SET
    company_id = (
        SELECT c.id
        FROM companies c
        WHERE c.code = 'KTHM_PRECISION'
            AND c.is_deleted = FALSE
    ),
    department_id = (
        SELECT d.id
        FROM departments d
        INNER JOIN companies c
            ON c.id = d.company_id
        WHERE c.code = 'KTHM_PRECISION'
            AND d.code = 'ADMIN'
            AND c.is_deleted = FALSE
            AND d.is_deleted = FALSE
    ),
    username = 'iwanami',
    name = '岩波',
    email = 'sample-manager@example.local',
    actor_type = 'TENANT',
    is_deleted = FALSE,
    updated_by = NULL
WHERE cognito_sub = '00000000-0000-0000-0000-000000000099';

INSERT INTO users (
    company_id,
    department_id,
    cognito_sub,
    username,
    name,
    email,
    actor_type,
    is_deleted,
    created_by,
    updated_by
)
SELECT
    c.id,
    d.id,
    '00000000-0000-0000-0000-000000000099',
    'iwanami',
    '岩波',
    'sample-manager@example.local',
    'TENANT',
    FALSE,
    NULL,
    NULL
FROM companies c
INNER JOIN departments d
    ON d.company_id = c.id
    AND d.code = 'ADMIN'
    AND d.is_deleted = FALSE
WHERE c.code = 'KTHM_PRECISION'
    AND c.is_deleted = FALSE
    AND NOT EXISTS (
        SELECT 1
        FROM users u
        WHERE u.cognito_sub = '00000000-0000-0000-0000-000000000099'
    );

INSERT IGNORE INTO user_permission_sets (
    user_id,
    permission_set_id
)
SELECT
    u.id,
    ps.id
FROM users u
INNER JOIN permission_sets ps
    ON ps.code = 'TENANT_MANAGER'
    AND ps.is_deleted = FALSE
WHERE u.cognito_sub = '00000000-0000-0000-0000-000000000099';

COMMIT;
```

### 変更ファイル

- `apps/server/src/main/java/com/example/workops/common/security/CognitoAuthenticationSuccessHandler.java`
- `apps/server/src/main/java/com/example/workops/common/security/SecurityConfig.java`
- `docs/implementation/mvp/agent-tasks/M3-07-cognito-authentication.md`

### 確認結果

- `cd apps/server && .\mvnw.cmd test` が成功した。
- local profileで `/auth/claims` が `200` を返すことを確認した。
- local profileで既定ユーザー `kthm-manager` が `/auth/authorization/manager` にアクセスすると `200` を返すことを確認した。
- 起動確認後、Spring Bootプロセスを停止した。
- profileなしCognitoログイン確認は、実Cognitoユーザー投入SQLの実行後にブラウザで確認する。

### 残課題

- 実Cognitoユーザー投入SQLをユーザーがローカルDBへ実行する。
- profileなし起動でCognitoログイン後の画面確認を行う。
- Cognitoユーザー登録時の `users` 自動登録はPhase 2で扱う。
