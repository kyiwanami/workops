# P2-7-01 Cognito provider / invitation template / task role

## 目的

P2-7 の AWS dev 実確認に入る前に、P2-6 のログイン分離設定を実 Cognito で確実に使える形へ補強し、WorkOps アプリから Cognito `AdminCreateUser` を呼び出せる最小 IAM 権限を ECS Task Role に付与する。

あわせて、WorkOps 管理導線から作成されたユーザーへ送る Cognito 招待メールを日本語 HTML テンプレートにする。

## ユーザー要求

- P2-7 は WorkOps 管理導線から実 Cognito の `AdminCreateUser` を呼び出す
- Cognito 権限は `cognito-idp:AdminCreateUser` のみに限定する
- `AdminDeleteUser`、`AdminGetUser`、`AdminUpdateUserAttributes`、`AdminDisableUser` は P2-7 では付与しない
- Cognito 作成成功後に DB 登録が失敗しても、自動削除補償は実装しない
- 不整合発生時は運用で Cognito 側を確認・削除する
- 不整合検知用の追加 ERROR ログは P2-7 では実装しない
- `platform` / `tenant` OAuth2 registration は `provider: cognito` を明示する
- Cognito 招待メールテンプレートを P2-7 で追加する
- 招待メールテンプレートは `IdentityStack` の `userInvitation` で管理する
- 招待メールは日本語 HTML とする
- Cognito の送信元はデフォルトのままとし、SES カスタム FROM は扱わない
- AWS deploy / destroy は、この task 作成時点では実行しない
- git commit は今回だけユーザーが許可している

## 前提

- P2-6 により Cognito App Client は PLATFORM 用と TENANT 用に分離済みである
- `IdentityStack` は Cognito User Pool / Hosted UI domain / Platform App Client / Tenant App Client を維持対象として所有している
- `AppRuntimeStack` は ECS task に Cognito User Pool ID、Platform Client ID、Tenant Client ID、Platform redirect URI、Tenant redirect URI を環境変数で渡している
- `apps/web` は non-local profile で AWS SDK for Java 2.x の `CognitoIdentityProviderClient` を作成する
- `AwsCognitoUserProvisioner` は `AdminCreateUser` を呼び出し、Cognito response の `sub` を `users.cognito_sub` 登録に使う
- P2-7 では既存データ移行と後方互換性を考慮しない

## 案

- `application.yml` の `spring.security.oauth2.client.registration.platform` に `provider: cognito` を追加する
- `application.yml` の `spring.security.oauth2.client.registration.tenant` に `provider: cognito` を追加する
- `SecurityConfigTests` に、platform / tenant registration が provider `cognito` を参照する確認を追加する
- `IdentityStack` の `UserPool` 定義へ `userInvitation` を追加する
- 招待メール件名は `WorkOps アカウント作成のお知らせ` とする
- 招待メール本文は日本語 HTML とし、Cognito 必須 placeholder の `{username}` と `{####}` を必ず含める
- 招待メール本文には、ユーザー名、一時パスワード、初回ログイン時にパスワード変更が必要な旨を記載する
- SES custom email sender、SES verified identity、カスタム FROM は追加しない
- `AppRuntimeStack` の ECS Task Role に `cognito-idp:AdminCreateUser` の IAM policy を追加する
- IAM policy の resource は `IdentityStack` から渡される User Pool ID を使って User Pool ARN に限定する
- `AppRuntimeStackProps` には既存の `cognitoUserPoolId` を使い、不要な props は追加しない
- CDK assertion で、ECS Task Role policy に `cognito-idp:AdminCreateUser` のみが含まれることを確認する
- CDK assertion で、`AdminDeleteUser`、`AdminGetUser`、`AdminUpdateUserAttributes`、`AdminDisableUser` が含まれないことを確認する
- CDK assertion で、`IdentityStack` の invite message template が `{username}` と `{####}` を含むことを確認する

## ADR

- `provider: cognito` を明示する。P2-6 で registration を `platform` / `tenant` に分けたため、実 Cognito Hosted UI 確認前に provider 参照を明確にしておく必要がある。
- `AdminCreateUser` のみに限定する。P2-7 の実装対象は WorkOps 管理導線からの Cognito ユーザー作成であり、削除、停止、属性更新、作成後取得は範囲外である。
- 自動削除補償は入れない。DB 登録失敗は通常発生しない例外的な不整合として扱い、Cognito コンソールと DB 確認による運用対応にする。
- 招待メール実配送を確認する。P2-7 では Cognito 標準の一時パスワードフローまで実利用できることを完了条件にするため。
- 招待メールは Cognito デフォルト送信元を使う。SES カスタム FROM を入れると、SES identity 検証と送信設定が増え、P2-7 の目的から外れるため。
- 既存データ移行と後方互換性は考慮しない。

## 対応範囲

- OAuth2 registration の provider 明示
- Cognito 招待メール subject / HTML body の CDK 管理
- ECS Task Role への `cognito-idp:AdminCreateUser` 付与
- User Pool ARN に限定した IAM resource
- Java test 更新
- CDK assertion test 更新
- README または P2-7 task 側での確認手順整理

## 除外範囲

- `AdminDeleteUser`
- `AdminGetUser`
- `AdminUpdateUserAttributes`
- `AdminDisableUser`
- Cognito 作成成功後の DB 登録失敗に対する自動削除補償
- 不整合検知用の追加 ERROR ログ
- SES カスタム FROM
- SES verified identity
- Cognito Trigger
- Pre Token Generation
- Cognito group / scope による業務認可
- AWS deploy / destroy
- P2-7-01 での実メール受信確認

## 完了条件

- platform registration に `provider: cognito` が明示されている
- tenant registration に `provider: cognito` が明示されている
- `IdentityStack` が日本語 HTML の Cognito invite message template を定義している
- invite message template の subject が WorkOps 向けになっている
- invite message template の本文に `{username}` と `{####}` が含まれている
- ECS Task Role に `cognito-idp:AdminCreateUser` のみが付与されている
- `cognito-idp:AdminCreateUser` の resource が User Pool ARN に限定されている
- `AdminDeleteUser`、`AdminGetUser`、`AdminUpdateUserAttributes`、`AdminDisableUser` が CDK template に含まれていない
- Java test が成功している
- CDK build / test が成功している

## 確認方法

Java:

```powershell
cd C:\git\workops\apps\web
.\mvnw.cmd '-Dtest=SecurityConfigTests' test
```

CDK:

```powershell
cd C:\git\workops\infra\cdk
npm run build
npm test -- --runInBand
```

実 AWS 確認は P2-7-02 で扱う。P2-7-01 の task 作成または実装時点では、AWS の `cdk deploy` / `cdk destroy` を実行しない。
