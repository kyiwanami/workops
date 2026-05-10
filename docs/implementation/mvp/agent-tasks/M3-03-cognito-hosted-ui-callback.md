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

- `apps/server/src/main/java/com/example/workops/common/security/`
- `apps/server/src/main/java/com/example/workops/common/web/`
- `apps/server/src/main/resources/templates/`
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
- `sub`、username 相当、`email`、`email_verified` の取得可否を記録する

## 実装時の記録

実装後に、次をこのファイルへ追記する。

- 実装方針
- 変更ファイル
- 確認結果
- 残課題
