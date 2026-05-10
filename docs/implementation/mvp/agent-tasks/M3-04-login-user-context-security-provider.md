# M3-04 LoginUserContext / SecurityCurrentUserProvider

## 最終状態補足

このファイルはM3-04実施時点のタスク名と記録を残す。
その後のM3-06 / M3-07で、現在ユーザー管理は Spring Security の `Authentication` へ寄せた。
最終状態では `SecurityCurrentUserProvider` を使わず、DB由来の `LoginUserContext` を `Authentication.principal` として扱う。

## 目的

Spring Security OAuth2 Login から Cognito `sub` を取得し、WorkOps 側の認証境界へ渡せることを確認する。

## 対応範囲

- `LoginUserContext` を作成する
- `CurrentUserProvider` を作成する
- `SecurityCurrentUserProvider` を作成する
- Spring Security の認証情報から Cognito `sub` を取得する
- `/auth/claims` に `sub` と `LoginUserContext.providerSubject` を表示する
- Cognito 疎通確認で得た `sub` を、将来の DB 突合実装に使える確認資産として記録する

## 対応ファイル

- `apps/web/src/main/java/com/example/workops/common/security/`
- `apps/web/src/main/java/com/example/workops/common/web/AuthClaimsController.java`
- `apps/web/src/main/resources/templates/auth/claims.html`
- `docs/implementation/mvp/agent-tasks/M3-04-login-user-context-security-provider.md`

## 除外範囲

- 実 Cognito `sub` と `users` の紐づけ
- `users.cognito_sub` を使ったDB突合
- `users` / `user_permission_sets` / `permission_sets` からの `LoginUserContext` 作成
- 疎通確認済みCognitoユーザーの `sub` をseedへ固定投入すること
- local 代替ユーザー
- Service 層認可チェック
- Cognitoユーザー登録時の `users` 自動登録
- Cognito Trigger
- Pre Token Generation
- PLATFORM / TENANT App Client 分離
- ユーザー管理画面
- AWS SDK 利用

## 完了条件

Cognitoログイン後、Spring Security の `OidcUser` から `sub` を取得でき、`LoginUserContext.providerSubject` として表示できる。

## 確認方法

- `cd apps/web && .\mvnw.cmd test`
- profile なし起動で未ログイン時に `/auth/claims` が Cognito OAuth2 Login へ `302` する
- ブラウザでCognitoログイン後、`/auth/claims` に Raw Claim `sub` と `LoginUserContext.providerSubject` が表示される
- `email_verified`、`userStatus`、`users.status` がコードと画面に存在しないことを確認する

## 実装時の記録

### 実装方針

- M3-04時点で業務利用する Cognito claim は `sub` だけにする。
- `LoginUserContext` は M3-04 時点では `providerSubject` のみにする。
- Cognito claim 由来の `username` / `email` は業務利用しない。
- Cognito疎通確認で取得できた `sub` は、将来のDB突合実装で `users.cognito_sub` と照合するための確認資産として扱う。
- 実Cognito `sub` をseedへ固定投入しない。
- `users` とのDB突合は、別タスクとしてユーザー合意後に実装する。

### 将来DB突合を実装するときの進め方

1. 先にDB定義タスクとして `users.cognito_sub` の要否、NULL可否、一意制約、seedへの入れ方を合意する。
2. 実Cognito `sub` をリポジトリ管理seedに入れるか、ローカル専用データとして扱うかをユーザーに確認する。
3. DB突合タスクでは、Spring Security から取得した `sub` だけを突合キーにする。
4. `username` / `email` / `actorType` / `companyId` / `permissionSets` はDBから取得する。
5. `email` / `username` をCognito claimから業務利用しない。
6. `users.status` / `userStatus` は作らず、ログイン可否と無効化はCognito側で制御する。
7. Service層認可はM3-06で扱い、DB突合タスクへ混ぜない。

### 変更ファイル

- `apps/web/src/main/java/com/example/workops/common/security/LoginUserContext.java`
- `apps/web/src/main/java/com/example/workops/common/security/CurrentUserProvider.java`
- `apps/web/src/main/java/com/example/workops/common/security/SecurityCurrentUserProvider.java`
- `apps/web/src/main/java/com/example/workops/common/web/AuthClaimsController.java`
- `apps/web/src/main/resources/templates/auth/claims.html`
- `docs/implementation/mvp/agent-tasks/M3-04-login-user-context-security-provider.md`

### 確認結果

- Cognitoログイン後に `sub` を取得できた。
- `sub` を `LoginUserContext.providerSubject` として表示できた。
- DB突合を実装するときは、今回確認できた `sub` を突合キーとして使う方針が確認できた。

### 残課題

- DB由来の `LoginUserContext` 作成は、別タスクとしてユーザー合意後に実装する。
- local profile でDB由来の `LoginUserContext` を返す実装は、DB突合方針が確定した後に扱う。
- Service層認可はM3-06で扱う。
- Cognitoユーザー登録時の `users` 自動登録はPhase 2で扱う。
