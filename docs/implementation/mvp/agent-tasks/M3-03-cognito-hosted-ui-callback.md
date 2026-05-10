# M3-03 Cognito Hosted UI / localhost callback

## 目的

Cognito テストユーザーでログインし、localhost callback で Spring Boot アプリへ戻れることを確認する。

## 対応範囲

- Cognito Hosted UI / managed login への遷移を確認する
- Cognito テストユーザーでログインする
- `http://localhost:8080/login/oauth2/code/cognito` callback を確認する
- Spring Security が認証済み状態を保持できることを確認する
- 取得できる OIDC / Cognito claim を画面またはログで確認する

## 対応ファイル

- `apps/web/src/main/java/com/example/workops/common/security/`
- `apps/web/src/main/java/com/example/workops/common/web/`
- `apps/web/src/main/resources/templates/`
- `docs/implementation/mvp/agent-tasks/M3-03-cognito-hosted-ui-callback.md`

## 除外範囲

- DB users との突合
- LoginUserContext の確定実装
- local 代替ユーザー
- Service 層認可
- 本番用 Cognito 構成
- AWS dev 環境デプロイ

## 完了条件

Cognito テストユーザーでログイン後、Spring Boot アプリへ戻り、OIDC / Cognito claim を確認できる。

## 確認方法

- ブラウザで Cognito Hosted UI / managed login へ遷移する
- Cognito テストユーザーでログインする
- localhost callback でアプリへ戻る
- `sub` の取得可否を記録する

## 実装時の記録

### 実装方針

- AWS SDK は使わず、Spring Security OAuth2 Login が保持する `OidcUser` からclaimを読む。
- `/auth/claims` をM3確認用URLとして追加する。
- M3で業務利用するclaimは、DB `users` と突合するための `sub` に限定する。
- OAuth2 scope は `openid` と `email` だけを使う。
- `profile` と `aws.cognito.signin.user.admin` はMVP / M3では使わない。
- DB users との突合、`LoginUserContext`、local代替ユーザーはM3-04以降で扱う。

### 変更ファイル

- `apps/web/src/main/java/com/example/workops/common/web/AuthClaimsController.java`
- `apps/web/src/main/resources/templates/auth/claims.html`
- `apps/web/src/main/resources/templates/index.html`
- `docs/implementation/mvp/agent-tasks/M3-03-cognito-hosted-ui-callback.md`

### 確認結果

- `cd apps/web && .\mvnw.cmd test` が成功した。
- `local` profile の起動で `http://localhost:8080/` が `200` を返した。
- `local` profile の起動で `http://localhost:8080/auth/claims` が `200` を返した。
- `local` profile なしの起動で `http://localhost:8080/auth/claims` が `/oauth2/authorization/cognito` へ `302` を返した。
- Cognito側の追加変更は不要。既存の callback URL と scope 設定を使う。
- ブラウザでCognitoテストユーザーとしてログイン後、`/auth/claims` で `sub` が取得できることを確認した。

### 残課題

- DB users との突合、`LoginUserContext`、local代替ユーザーはM3-04以降で扱う。
