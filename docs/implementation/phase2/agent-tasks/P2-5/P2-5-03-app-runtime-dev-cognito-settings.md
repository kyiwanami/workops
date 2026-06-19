# P2-5-03 AppRuntimeStack dev profile / Cognito settings

## 目的

`AppRuntimeStack` の ECS runtime を P2-4 までの `local` profile から、stage 由来の Spring profile へ切り替え、AWS dev 上で Cognito OAuth2 Login を使えるようにする。

## ユーザー要求

- P2-4 までの `local` profile 起動は意図的な暫定であり、P2-5では入れない
- P2-5 runtime は stage 由来の Spring profile で起動する
- Phase2 dev では結果として `dev` profile で起動する
- `SPRING_PROFILES_ACTIVE` は `/workops/${stage}/spring/profile` から受け取る
- Cognito 接続値は ECS 環境変数として渡す
- `WORKOPS_COGNITO_REDIRECT_URI` は `EdgeStack.cloudFrontHttpsUrl` から組み立てる
- `AppRuntimeStack` から `EdgeStack` への依存は許容する
- `AppRuntimeStack` から `IdentityStack` への依存は許容する

## 前提

- `ConfigStack` が `/workops/${stage}/spring/profile` を所有している
- Phase2 dev では `/workops/dev/spring/profile` の値が `dev` である
- `IdentityStack` が User Pool ID と App Client ID を output する
- `EdgeStack` が `cloudFrontHttpsUrl` を output する
- `AppRuntimeStack` は短命 Stack である
- P2-5 では Cognito Hosted UI ログインを実行する

## 案

- `AppRuntimeStack` は `SPRING_PROFILES_ACTIVE` を SSM Parameter `/workops/${stage}/spring/profile` から ECS secret として渡す
- `AppRuntimeStack` は `AWS_REGION` を ECS environment として渡す
- `AppRuntimeStack` は `WORKOPS_COGNITO_USER_POOL_ID` を `IdentityStack` から受け取って ECS environment として渡す
- `AppRuntimeStack` は `WORKOPS_COGNITO_CLIENT_ID` を `IdentityStack` から受け取って ECS environment として渡す
- `AppRuntimeStack` は `WORKOPS_COGNITO_REDIRECT_URI` を `EdgeStack.cloudFrontHttpsUrl + "/login/oauth2/code/cognito"` として ECS environment に渡す
- `AppRuntimeStack` は DB 接続値の受け渡しを既存の `WORKOPS_DB_URL`、`WORKOPS_DB_USERNAME`、`WORKOPS_DB_PASSWORD` のまま維持する
- `AppRuntimeStack` は `IdentityStack` と `EdgeStack` に依存する
- P2-5 では `AppRuntimeStack` の image tag 方針は P2-3 の手動 deploy 方針を維持する

## ADR

- `SPRING_PROFILES_ACTIVE` は `local` 固定をやめる。P2-5 は Cognito Hosted UI ログインを確認するフェーズであり、`local` profile では OAuth2 Login に入らないため。
- Spring profile は stage 由来の SSM Parameter から渡す。CDK stage と Spring profile を分けつつ、既存 `ConfigStack` の責務を使うため。
- `WORKOPS_COGNITO_REDIRECT_URI` は `EdgeStack` から組み立てる。Cognito App Client に登録する callback URL と Spring Security が使う redirect URI を一致させるため。
- `AppRuntimeStack` から `EdgeStack` への依存を許容する。`AppRuntimeStack` は短命 Stack であり、既に target group 参照で `EdgeStack` に依存しているため。
- 既存データ移行と後方互換性は考慮しない。

## 対応範囲

- `AppRuntimeStack` の `SPRING_PROFILES_ACTIVE` 受け渡し変更
- `AppRuntimeStack` の Cognito 環境変数追加
- `AppRuntimeStackProps` への User Pool ID / App Client ID / CloudFront HTTPS URL 入力追加
- `bin/cdk.ts` の Stack 接続更新
- CDK assertion test の更新
- P2-5 runtime 起動手順の記録

## 除外範囲

- `application-local.yml` の変更
- local profile の削除
- Cognito Trigger
- Pre Token Generation
- PLATFORM / TENANT App Client 分離
- `AdminCreateUser`
- GitHub Actions deploy
- CodePipeline
- CodeBuild
- migration 専用 ECS task

## 完了条件

- `AppRuntimeStack` の ECS container が `SPRING_PROFILES_ACTIVE=local` 固定ではない
- `SPRING_PROFILES_ACTIVE` が `/workops/${stage}/spring/profile` から渡される
- Phase2 dev runtime が `dev` profile で起動する
- ECS container に `AWS_REGION` が渡される
- ECS container に `WORKOPS_COGNITO_USER_POOL_ID` が渡される
- ECS container に `WORKOPS_COGNITO_CLIENT_ID` が渡される
- ECS container に `WORKOPS_COGNITO_REDIRECT_URI` が渡される
- `WORKOPS_COGNITO_REDIRECT_URI` が CloudFront default domain の callback path と一致する
- DB 接続値の受け渡しは既存環境変数のまま維持されている

## 確認方法

エージェント確認:

```powershell
cd C:\git\workops\infra\cdk
npm run build
npm test -- --runInBand
$env:WORKOPS_STAGE='dev'
npm run cdk -- synth AppRuntimeStack
```

CDK assertions で確認する代表条件:

- ECS Task Definition に `SPRING_PROFILES_ACTIVE` がある
- `SPRING_PROFILES_ACTIVE` の値が `local` 固定ではない
- ECS Task Definition に `AWS_REGION` がある
- ECS Task Definition に `WORKOPS_COGNITO_USER_POOL_ID` がある
- ECS Task Definition に `WORKOPS_COGNITO_CLIENT_ID` がある
- ECS Task Definition に `WORKOPS_COGNITO_REDIRECT_URI` がある
- `WORKOPS_DB_URL`、`WORKOPS_DB_USERNAME`、`WORKOPS_DB_PASSWORD` が維持されている

ユーザー確認:

- AWS dev の ECS task が `dev` profile で起動する
- Spring Security OAuth2 Login が有効になる
- Cognito Hosted UI へ redirect される
- callback URL mismatch が発生しない
