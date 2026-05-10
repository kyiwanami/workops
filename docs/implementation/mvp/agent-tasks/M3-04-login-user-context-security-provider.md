# M3-04 LoginUserContext / SecurityCurrentUserProvider

## 目的

Cognito claim を WorkOps 内部で扱う最小の現在ユーザー表現へ変換する。

## 対応範囲

- `LoginUserContext` を作成する
- `CurrentUserProvider` を作成する
- `SecurityCurrentUserProvider` を作成する
- Spring Security の認証情報から OIDC / Cognito claim を読む
- `/auth/claims` に `LoginUserContext` の内容を表示する

## 対応ファイル

- `apps/server/src/main/java/com/example/workops/common/security/`
- `apps/server/src/main/java/com/example/workops/common/web/AuthClaimsController.java`
- `apps/server/src/main/resources/templates/auth/claims.html`
- `docs/implementation/mvp/agent-tasks/M3-04-login-user-context-security-provider.md`

## 除外範囲

- DB `users` との突合
- `company_id` の解決
- `permission_sets` / `user_permission_sets` の解決
- local 代替ユーザー
- Service 層認可チェック
- Cognito Trigger
- Pre Token Generation
- PLATFORM / TENANT App Client 分離
- ユーザー管理画面
- AWS SDK 利用

## 完了条件

Cognito ログイン済みユーザーから `LoginUserContext` を取得でき、`providerSubject`、`username`、`email` を確認できる。

## 確認方法

- `cd apps/server && .\mvnw.cmd test`
- `local` profile 起動で `/auth/claims` が `200` を返す
- profile なし起動で未ログイン時に `/auth/claims` が Cognito OAuth2 Login へ `302` する
- ブラウザでCognitoログイン後、`/auth/claims` に `LoginUserContext` の `providerSubject`、`username`、`email` が表示される
- `email_verified` が `LoginUserContext` と画面表示に含まれていないことを確認する

## 実装時の記録

### 実装方針

- `emailVerified` はM3時点で不要なため `LoginUserContext` に含めない。
- `LoginUserContext` は `providerSubject`、`username`、`email` のみにする。
- `username` は `cognito:username` を正とする。
- Controller / Service 層は `CurrentUserProvider` 経由で現在ユーザーを取得する。
- `SecurityCurrentUserProvider` は `SecurityContextHolder` の `Authentication` から `OidcUser` を取り出して `LoginUserContext` に変換する。
- 未認証、または `OidcUser` でない場合は現在ユーザーなしとして扱う。
- DB users / permission_sets との突合は後続タスクで扱う。

### 変更ファイル

- `apps/server/src/main/java/com/example/workops/common/security/LoginUserContext.java`
- `apps/server/src/main/java/com/example/workops/common/security/CurrentUserProvider.java`
- `apps/server/src/main/java/com/example/workops/common/security/SecurityCurrentUserProvider.java`
- `apps/server/src/main/java/com/example/workops/common/web/AuthClaimsController.java`
- `apps/server/src/main/resources/templates/auth/claims.html`
- `docs/implementation/mvp/agent-tasks/M3-04-login-user-context-security-provider.md`

### 確認結果

- `cd apps/server && .\mvnw.cmd test` が成功した。
- `local` profile 起動で `/auth/claims` が `200` を返した。
- profile なし起動で未ログイン時に `/auth/claims` が `/oauth2/authorization/cognito` へ `302` を返した。
- profile なし起動のまま、ブラウザ確認用にアプリを起動した状態で残した。

### 残課題

- ブラウザでCognitoログイン後、`/auth/claims` に `LoginUserContext` の `providerSubject`、`username`、`email` が表示されることを確認する。
- ブラウザでCognitoログイン後、`email_verified` が画面に表示されていないことを確認する。
- DB users との突合は後続タスクで扱う。
- company_id と permission_sets の解決は後続タスクで扱う。
- local 代替ユーザーはM3-05で扱う。
