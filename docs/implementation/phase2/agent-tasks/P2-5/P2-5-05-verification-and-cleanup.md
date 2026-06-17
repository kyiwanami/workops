# P2-5-05 verification / cleanup

## 目的

P2-5 のエージェント確認、AWS dev ユーザー確認、短命 Stack cleanup、P2-6 への引き継ぎを整理する。

## ユーザー要求

- P2-5 の実 AWS 確認は `platform-admin` の成功ログイン、`users.cognito_sub` 突合、DB 由来権限での画面利用に限定する
- 突合失敗時の HTTP 403 確認は P2-5 task に入れない
- P2-5 実行確認後は `AppRuntimeStack`、`EdgeStack`、`EgressStack`、`DataStack` を削除する
- `IdentityStack` は削除しない
- RDS は短命運用とし、次回作成時は `platform-admin` の `users.cognito_sub` を再度 SQL で投入する
- Custom Resource Delete 時は Cognito App Client を変更しない
- 次回 `EdgeStack` 作成時に新しい CloudFront default domain で App Client URL が自動更新されることを確認手順に入れる

## 前提

- P2-5-01 で `IdentityStack` が作成されている
- P2-5-02 で `EdgeStack` に Custom Resource が追加されている
- P2-5-03 で `AppRuntimeStack` が dev profile / Cognito 設定で起動する
- P2-5-04 で `platform-admin` の `users.cognito_sub` が実 Cognito `sub` と紐付いている
- CloudFront / ALB / ECS / NAT Gateway は短命 Stack のまま扱う
- Cognito User Pool / Hosted UI domain / App Client は維持対象にする

## 案

- エージェント確認では CDK build / test / synth を行う
- エージェント確認では Spring Boot test を行う
- エージェント確認では task Markdown と README の整合を確認する
- ユーザー確認では Hosted UI ログインを確認する
- ユーザー確認では CloudFront callback を確認する
- ユーザー確認では `platform-admin` として業務画面へ入れることを確認する
- ユーザー確認では DB 由来権限で画面利用できることを確認する
- P2-5 確認後、短命 Stack を `AppRuntimeStack`、`EdgeStack`、`EgressStack`、`DataStack` の順で削除する
- `IdentityStack` は維持する
- 次回 P2-5 実行確認時は `DataStack` を再作成し、`platform-admin` の `users.cognito_sub` を再投入する
- cleanup 後、Cognito User Pool / App Client / Hosted UI domain が残っていることを確認する
- 次回実行確認時は `EgressStack`、`EdgeStack`、`AppRuntimeStack` を再作成し、Custom Resource が App Client URL を更新する

## ADR

- P2-5 の AWS 実環境確認は成功ログインに限定する。P2-5 の AWS 固有論点は Hosted UI、CloudFront callback、Cognito `sub`、RDS 上の `users.cognito_sub`、ECS dev profile 起動にあるため。
- `IdentityStack` は cleanup 対象にしない。Cognito 本体は維持対象であり、短命 Edge / Runtime / Egress の lifecycle と分けるため。
- `DataStack` は P2-5 確認後に削除する。RDS は短命運用であり、作り直すたびに `platform-admin` の `users.cognito_sub` を実 Cognito `sub` で再投入する前提にするため。
- 次回 Edge 作成時の App Client URL 更新を確認する。CloudFront default domain は `EdgeStack` 作成ごとに変わるため、Custom Resource の再更新が P2-5 方針の中心であるため。
- 既存データ移行と後方互換性は考慮しない。

## 対応範囲

- P2-5 エージェント確認手順の記載
- P2-5 ユーザー確認手順の記載
- Hosted UI 成功ログイン確認の記載
- `platform-admin` 画面利用確認の記載
- 短命 Stack cleanup 手順の記載
- `IdentityStack` 維持確認の記載
- 次回 `EdgeStack` 再作成時の App Client URL 更新確認の記載
- 次回 `DataStack` 再作成時の `users.cognito_sub` 再投入確認の記載
- P2-6 への引き継ぎ記載

## 除外範囲

- 突合失敗時の HTTP 403 実 AWS 確認
- TENANT ユーザー Hosted UI ログイン確認
- PLATFORM / TENANT App Client 分離
- `AdminCreateUser`
- GitHub Actions deploy
- CodePipeline
- CodeBuild
- CloudWatch Alarm
- Observability Dashboard
- git commit

## 完了条件

- CDK build / test / synth が成功している
- Spring Boot test が成功している
- `IdentityStack` が deploy できる
- `EdgeStack` deploy 時に Cognito App Client URL が CloudFront default domain へ更新されている
- `AppRuntimeStack` が dev profile で起動している
- Hosted UI で Cognito テストユーザーがログインできる
- CloudFront callback で `apps/web` に戻れる
- `platform-admin` として業務画面へ入れる
- DB 由来権限で画面利用できる
- `AppRuntimeStack`、`EdgeStack`、`EgressStack`、`DataStack` の cleanup 手順が記録されている
- cleanup 後も `IdentityStack` が残る

## 確認方法

エージェント確認:

```powershell
cd C:\git\workops\infra\cdk
npm run build
npm test -- --runInBand
$env:WORKOPS_STAGE='dev'
npm run cdk -- synth IdentityStack
npm run cdk -- synth EdgeStack
npm run cdk -- synth AppRuntimeStack
```

Spring Boot 確認:

```powershell
cd C:\git\workops\apps\web
.\mvnw.cmd test
```

ユーザー確認:

```powershell
cd C:\git\workops\infra\cdk
$env:WORKOPS_STAGE='dev'
npm run cdk -- deploy IdentityStack
npm run cdk -- deploy EgressStack
npm run cdk -- deploy EdgeStack
npm run cdk -- deploy AppRuntimeStack
```

Cognito App Client URL 確認:

- Cognito App Client の callback URL が CloudFront default domain の `/login/oauth2/code/cognito` になっている
- Cognito App Client の logout URL が CloudFront default domain の `/` になっている
- `DefaultRedirectURI` が callback URL と一致している

ログイン確認:

- Cognito テストユーザーで Hosted UI へログインする
- CloudFront callback で `apps/web` に戻る
- `platform-admin` として業務画面へ入る
- DB 由来の PLATFORM_ADMIN 権限で画面利用できる

実行確認セッション終了時の cleanup:

```powershell
cd C:\git\workops\infra\cdk
$env:WORKOPS_STAGE='dev'
npm run cdk -- destroy AppRuntimeStack
npm run cdk -- destroy EdgeStack
npm run cdk -- destroy EgressStack
npm run cdk -- destroy DataStack
```

維持対象確認:

```powershell
aws cloudformation describe-stacks --stack-name workops-dev-identity
aws cognito-idp list-user-pools --max-results 10
```

次回実行確認時:

```text
DataStack、EgressStack、EdgeStack、AppRuntimeStack を再作成する。
platform-admin の users.cognito_sub は、実 Cognito sub を使って再投入する。
EdgeStack deploy 時に Custom Resource が新しい CloudFront default domain を Cognito App Client に反映する。
```

## P2-6 引き継ぎ

- P2-6 では PLATFORM / TENANT App Client 分離を扱う
- P2-6 では単一 App Client で成立した P2-5 の Hosted UI / `sub` 突合を前提にする
- P2-6 では actor_type とログイン導線の整合チェックを扱う
- P2-7 では WorkOps 管理導線から `AdminCreateUser` と `users.cognito_sub` 登録を扱う
