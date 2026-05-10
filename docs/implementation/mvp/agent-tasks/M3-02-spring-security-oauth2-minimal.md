# M3-02 Spring Security OAuth2 Login minimal setup

## 目的

Spring Security OAuth2 Login を最小構成で導入し、Cognito Hosted UI / managed login に遷移できる土台を作る。

## 対応範囲

- Spring Security OAuth2 Client の依存関係を追加する
- OAuth2 Login の最小 Security 設定を作成する
- local profile で Cognito 設定を読み込めるようにする
- client secret なし App Client を前提に設定する
- callback URL は `http://localhost:8080/login/oauth2/code/cognito` に固定する

## 対応ファイル

- `apps/server/pom.xml`
- `apps/server/src/main/resources/application-local.yml`
- `apps/server/src/main/java/com/example/workops/common/security/`
- `docs/implementation/mvp/agent-tasks/M3-02-spring-security-oauth2-minimal.md`

## 除外範囲

- Cognito Trigger
- Pre Token Generation
- PLATFORM / TENANT App Client 分離
- DB の users との突合実装
- Service 層認可
- ユーザー管理画面

## 完了条件

Spring Boot local profile で起動し、未認証アクセス時に Cognito Hosted UI / managed login へ遷移できる。

## 確認方法

- `cd apps/server && .\mvnw.cmd test` が成功する
- `cd apps/server && .\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=local"` が起動する
- ブラウザで保護対象URLへアクセスし、Cognito 側へリダイレクトされることを確認する

## 実装時の記録

実装後に、次をこのファイルへ追記する。

- 実装方針
- 変更ファイル
- 確認結果
- 残課題
