# M10-01 Cognito SDK / local Fake Provisioner

## 目的

M10 のユーザー作成処理で利用する Cognito ユーザー作成境界を作る。
`local` profile では Fake が疑似 `cognito_sub` を発行し、`local` なしでは AWS SDK for Java 2.x で Cognito `AdminCreateUser` を呼び出す。

## M10 共通方針

- M10 の範囲は、初期 TENANT_MANAGER 作成、ユーザー作成、権限割当・変更、local Cognito Fake Bean、疑似 `cognito_sub` 発行・登録に限定する
- M10 で使う Cognito 本物 API は `AdminCreateUser` に限定する
- Cognito Hosted UI 本格確認、PLATFORM / TENANT App Client 分離、Cognito Trigger、Pre Token Generation は Phase 2 で扱う
- M9 では会社・部署管理を扱い、M10 で会社作成フローへ初期 TENANT_MANAGER 作成を後付け連携する
- 最終仕様として、会社作成時に初期 TENANT_MANAGER を必ず 1 人作る
- ユーザー作成後に編集できる範囲は、表示名、email、所属部署、権限セットだけとする
- `actor_type` と `company_id` は作成後変更不可とする
- TENANT_MANAGER 数が 0 になる権限変更は、認可ではなく、更新後 count による業務バリデーションで拒否する
- 削除済み部署に所属するユーザーは、ユーザー一覧・詳細で `部署名（削除済み）` と表示する
- Git commit は実行しない

## 対応範囲

- AWS SDK for Java 2.x の BOM と Cognito Identity Provider dependency を追加する
- Cognito 接続先は `AWS_REGION` と `WORKOPS_COGNITO_USER_POOL_ID` を使う
- Cognito issuer URI は `AWS_REGION` と `WORKOPS_COGNITO_USER_POOL_ID` から `application.yml` で構成する
- AWS SDK の region は Spring 設定として読んだ `AWS_REGION` を明示的に使う
- AWS SDK の認証情報は Default Credentials Provider Chain に任せる
- `.env.example` には smoke test 用メールアドレスを載せない
- `CognitoUserProvisioner` interface を作成する
- `CognitoUserProvisionRequest` record を作成する
- `ProvisionedCognitoUser` record を作成する
- `local` profile 用の `LocalCognitoUserProvisioner` を作成する
- `local` なし用の `AwsCognitoUserProvisioner` を作成する
- `local` なし用の `CognitoIdentityProviderClient` Bean を作成する
- `AwsCognitoUserProvisioner` は `AdminCreateUser` を呼び出す
- `AdminCreateUser` は `DesiredDeliveryMediums.EMAIL` を指定し、`MessageAction` は指定しない
- Cognito へ `email` と `email_verified=true` を user attribute として渡す
- Cognito response の user attribute から `sub` を取り出し、`ProvisionedCognitoUser.cognitoSub` として返す
- `UsernameExistsException` は `400 BAD_REQUEST` の `ResponseStatusException` に変換する
- その他の `CognitoIdentityProviderException` と `sub` 欠落は `500 INTERNAL_SERVER_ERROR` の `ResponseStatusException` に変換する
- 既存の `LocalAuthenticationFilter`、`DbLoginUserContextFactory`、`CognitoAuthenticationSuccessHandler` は変更しない

## 対応ファイル

- `apps/web/pom.xml`
- `.env.example`
- `apps/web/src/main/resources/application.yml`
- `apps/web/src/main/java/com/example/workops/admin/user/service/*.java`
- `apps/web/src/test/java/com/example/workops/admin/user/service/*.java`
- `docs/implementation/mvp/agent-tasks/M10/M10-01-local-cognito-fake-sub-issuer.md`

## 除外範囲

- ユーザー作成画面
- 権限割当・変更画面
- `users` への登録処理
- Cognito Hosted UI 本格確認
- PLATFORM / TENANT App Client 分離
- Cognito Trigger
- Pre Token Generation
- 初回ログイン時の `users` 自動作成
- Cognito username 変更
- Cognito 側ユーザー属性変更

## 完了条件

`local` profile で疑似 `cognito_sub` を発行できる。
`local` なしでは Cognito SDK の `AdminCreateUser` を呼び出す実装を利用できる。
通常テストでは AWS へ接続しない。
実 Cognito の E2E 確認はユーザーが行う。
M10-03 以降のユーザー作成処理が `CognitoUserProvisioner` interface へ依存できる。

## 確認方法

- `cd apps/web && .\mvnw.cmd test`
- local Fake 実装が UUID 形式の 36 文字 `cognitoSub` を返すことを確認する
- local Fake 実装が複数回呼び出しで異なる `cognitoSub` を返すことを確認する
- AWS SDK 実装が `AdminCreateUserRequest` に `userPoolId`、`username`、`email`、`email_verified=true`、`DesiredDeliveryMediums.EMAIL` を設定することを確認する
- AWS SDK 実装が `MessageAction` を指定しないことを確認する
- AWS SDK 実装が response の `sub` を戻り値へ変換することを確認する
- AWS SDK 実装が Cognito 例外を `ResponseStatusException` へ変換することを確認する
- 実 Cognito の招待メール送信、Hosted UI ログイン、画面操作を含む E2E 確認はユーザーが行う

## 実装時の記録

### 実装方針

- ユーザー作成処理から Cognito ユーザー作成を呼び出す境界として `CognitoUserProvisioner` を追加した
- `local` profile は `LocalCognitoUserProvisioner` で疑似 `cognito_sub` を発行する
- `local` なしでは `AwsCognitoUserProvisioner` が AWS SDK for Java 2.x の `CognitoIdentityProviderClient` で `AdminCreateUser` を呼び出す
- Cognito SDK の認証情報は AWS SDK の Default Credentials Provider Chain に任せる
- 招待メール送信先は request の `email` から渡し、`.env.example` に smoke test 用メールアドレスは載せない
- 実 Cognito の E2E 確認はユーザーが行うため、自動テストから外した
- M10-01 では DB 登録、ユーザー作成画面、会社作成フロー連携は実装していない
- 既存のログイン導線、local 現在ユーザー解決、Cognito ログイン成功後の DB 突合は変更していない

### 変更ファイル

- `.env.example`
- `apps/web/pom.xml`
- `apps/web/src/main/resources/application.yml`
- `apps/web/src/main/java/com/example/workops/admin/user/service/CognitoUserProvisioner.java`
- `apps/web/src/main/java/com/example/workops/admin/user/service/CognitoUserProvisionRequest.java`
- `apps/web/src/main/java/com/example/workops/admin/user/service/ProvisionedCognitoUser.java`
- `apps/web/src/main/java/com/example/workops/admin/user/service/LocalCognitoUserProvisioner.java`
- `apps/web/src/main/java/com/example/workops/admin/user/service/AwsCognitoUserProvisioner.java`
- `apps/web/src/main/java/com/example/workops/admin/user/service/CognitoIdentityProviderClientConfig.java`
- `apps/web/src/test/java/com/example/workops/admin/user/service/LocalCognitoUserProvisionerTests.java`
- `apps/web/src/test/java/com/example/workops/admin/user/service/AwsCognitoUserProvisionerTests.java`
- `docs/implementation/mvp/agent-tasks/M10/M10-01-local-cognito-fake-sub-issuer.md`

### 実装結果

- AWS SDK for Java 2.x の BOM `2.44.3` と `cognitoidentityprovider` dependency を追加した
- `.env.example` に `AWS_REGION` と `WORKOPS_COGNITO_USER_POOL_ID` を追加した
- `.env.example` から `WORKOPS_COGNITO_ISSUER_URI` と `WORKOPS_COGNITO_REGION` を削除した
- `application.yml` の issuer URI は `AWS_REGION` と `WORKOPS_COGNITO_USER_POOL_ID` から構成するようにした
- `application.yml` から `workops.cognito.region` と `workops.cognito.user-pool-id` を削除した
- `CognitoIdentityProviderClient` は Spring 設定として読んだ `AWS_REGION` を明示的に使うようにした
- `CognitoIdentityProviderClient` の認証情報は明示せず、AWS SDK の Default Credentials Provider Chain に任せるようにした
- `CognitoUserProvisioner#provision` で Cognito 作成境界を固定した
- `LocalCognitoUserProvisioner` は `UUID.randomUUID().toString()` で疑似 `cognito_sub` を返す
- `AwsCognitoUserProvisioner` は `AdminCreateUser` を呼び、Cognito から返る `sub` を戻り値にする
- `AwsCognitoUserProvisioner` は招待メールを送るため `DesiredDeliveryMediums.EMAIL` を指定し、`MessageAction` は指定していない
- 通常テストでは AWS に接続しない
- 実 Cognito の招待メール送信、Hosted UI ログイン、画面操作を含む E2E 確認はユーザーが行う

### 確認結果

- `cd apps/web && .\mvnw.cmd test`
  - `Tests run: 136, Failures: 0, Errors: 0, Skipped: 0`
- 通常テストでは AWS へ接続していない
- 実 Cognito の招待メール送信、Hosted UI ログイン、画面操作を含む E2E 確認はユーザー確認対象として自動テストから外した
- Mockito の dynamic agent warning は既存と同種の警告として残る
- Flyway の MySQL 9.7 検証済みバージョン警告は既存と同種の警告として残る

### 残課題

- `users` への登録処理は M10-03 / M10-04 で対応済み
- 会社作成フローへの初期 TENANT_MANAGER 作成連携は M10-03 で対応済み
- ユーザー作成画面、ユーザー編集、権限割当・変更は M10-03 / M10-04 / M10-05 で対応済み
- Cognito Hosted UI 本格確認、PLATFORM / TENANT App Client 分離、Cognito Trigger、Pre Token Generation は Phase 2 で扱う
