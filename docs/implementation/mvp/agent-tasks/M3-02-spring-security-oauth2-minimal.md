# M3-02 Spring Security OAuth2 Login minimal setup

## 目的

Spring Security OAuth2 Login を最小構成で導入し、Cognito Hosted UI / managed login に遷移できる土台を作る。

## 対応範囲

- `.env.local` によるCognito接続値のローカル管理を追加する
- Spring Security OAuth2 Client の依存関係を追加する
- OAuth2 Login の最小 Security 設定を作成する
- `local` profile では認証を通さず、`local` profile なしでは Cognito 設定を読み込めるようにする
- client secret なし App Client を前提に設定する
- callback URL は `http://localhost:8080/login/oauth2/code/cognito` に固定する

## 対応ファイル

- `.env.example`
- `.gitignore`
- `apps/server/pom.xml`
- `apps/server/src/main/resources/application.yml`
- `apps/server/src/main/java/com/example/workops/common/security/`
- `README.md`
- `docs/implementation/README.md`
- `docs/implementation/mvp/agent-tasks/M3-02-spring-security-oauth2-minimal.md`

## 除外範囲

- Cognito Trigger
- Pre Token Generation
- PLATFORM / TENANT App Client 分離
- DB の users との突合実装
- Service 層認可
- ユーザー管理画面

## 完了条件

`.env.local` がgit管理対象外のまま、Spring Boot `local` profile が従来通り起動し、`local` profile なしでは未認証アクセス時に Cognito Hosted UI / managed login へ遷移できる。

## 確認方法

- `cd apps/server && .\mvnw.cmd test` が成功する
- `cd apps/server && .\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=local"` が起動する
- `.env.local` 作成後に `cd apps/server && .\mvnw.cmd spring-boot:run` が起動する
- ブラウザで保護対象URLへアクセスし、Cognito 側へリダイレクトされることを確認する

## 実装時の記録

### 実装方針

- Cognito接続値はルート `.env.local` で管理し、gitには載せない。
- `.env.example` にはキー名とダミー値だけを置く。
- Spring Boot は `application.yml` から `../../.env.local` を任意読み込みする。
- Spring Security は `local` profile なしのときだけ認証必須にする。
- `local` profile では従来通り全リクエストを許可する。
- OAuth2 scope はM3で確認するclaimに必要な `openid` と `email` だけにする。
- `profile` と `aws.cognito.signin.user.admin` はM3時点では不要なため使わない。
- `application-local.yml` は設定を持たないため削除し、`local` profile は起動引数だけで切り替える。

### 変更ファイル

- `.env.example`
- `.gitignore`
- `apps/server/pom.xml`
- `apps/server/src/main/resources/application.yml`
- `apps/server/src/main/resources/application-local.yml`
- `apps/server/src/main/java/com/example/workops/common/security/SecurityConfig.java`
- `README.md`
- `docs/implementation/README.md`
- `docs/implementation/mvp/agent-tasks/M3-02-spring-security-oauth2-minimal.md`

### 確認結果

- `cd apps/server && .\mvnw.cmd test` が成功した。
- `local` profile の起動で `http://localhost:8080/` が `200` を返した。
- `local` profile の起動ではCognito issuerへの接続が発生しないことを確認した。
- `local` profile なしの起動で `http://localhost:8080/` が `/oauth2/authorization/cognito` へ `302` を返した。
- `local` profile なしの起動で、Cognito Hosted UIへ渡すscopeが `openid email` だけであることを確認した。
- Cognito callback URL は `http://localhost:8080/login/oauth2/code/cognito` で、末尾 `/` なしであることを確認した。
- Cognito側に `email` scope が無い状態では `invalid_scope` になり、Spring Securityのデフォルトログイン画面で `Invalid credentials` と表示されることを確認した。
- M3時点では `profile` scope が不要なため、アプリ側の要求scopeから `profile` を削除した。
- M3時点では `aws.cognito.signin.user.admin` scope も不要なため、アプリ側では要求しない。
- `scope=openid%20email` でCognito Hosted UIログインに成功した。
- `cognito` profile は不要な複雑化と判断し、削除した。
- `.vscode/launch.json`、`.vscode/tasks.json`、`scripts/run-server-cognito.ps1` は不要な起動導線として削除した。
- `local` profileなしのCognito確認でもローカルMySQLへ接続するため、DB/Flywayのデフォルト設定を `application.yml` に移した。
- `application-local.yml` は設定を持たなくなったため削除した。

### 残課題

- `.env.local` の実値入力はユーザーが行う。
- ログイン後のclaim確認、`LoginUserContext`、DBユーザー突合、Service層認可はM3-03以降で扱う。
