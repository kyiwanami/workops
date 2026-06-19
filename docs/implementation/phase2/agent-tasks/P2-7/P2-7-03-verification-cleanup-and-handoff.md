# P2-7-03 verification / cleanup / handoff

## 目的

P2-7 のローカル検証、AWS dev 実確認結果、短命 Stack cleanup、P2-8 への引き継ぎを整理する。

P2-7 では WorkOps 管理導線からの `AdminCreateUser`、`users.cognito_sub` 登録、招待メール実配送、初回パスワード変更、TENANT ログインが成立したことを確認し、確認後は課金対象の短命 Stack を削除する。

## ユーザー要求

- 有料リソースは確認後に削除する
- `IdentityStack` は維持する
- `DataStack`、`EgressStack`、`EdgeStack`、`AppRuntimeStack` は短命 Stack として扱う
- Cognito 招待メールが届かない場合、P2-7 は未完了とする
- Cognito コンソールや CLI で一時パスワードを代替取得しても、メール未着時は完了扱いにしない
- Cognito 作成成功後に DB 登録が失敗した場合、自動削除補償はしない
- 不整合は Cognito コンソールと DB を確認して運用で削除する
- RDS Console integrated CloudShell VPC environment は DB 確認後に削除する
- AWS dev の destroy は対象 Stack を明示してユーザー確認後に行う
- P2-8 では CloudWatch Logs / 認証・認可イベントログ確認へ進む

## 前提

- P2-7-01 の実装が完了している
- P2-7-02 の AWS dev 実確認が完了している
- AWS dev 実確認では、`DataStack`、`EgressStack`、`EdgeStack`、`AppRuntimeStack` の作成、CloudFront health check、Cognito App Client URL 更新、`platform-admin` bootstrap、platform login、会社作成、初期 TENANT_MANAGER 招待メール受信、初回パスワード変更、tenant login を確認済みである
- `IdentityStack` は Cognito User Pool / Hosted UI domain / Platform App Client / Tenant App Client を維持対象として所有している
- `DataStack` は RDS を含むため確認後に削除する
- `EgressStack` は NAT Gateway を含むため確認後に削除する
- `EdgeStack` は CloudFront distribution / VPC origin / internal ALB を含むため確認後に削除する
- `AppRuntimeStack` は ECS Service / Task を含むため確認後に削除する
- P2-7 では既存データ移行と後方互換性を考慮しない

## 案

- エージェント確認では Java test を実行する
- エージェント確認では CDK build / test を実行する
- 必要に応じて CDK synth を行い、`IdentityStack`、`EdgeStack`、`AppRuntimeStack` template を確認する
- エージェント確認では旧 `/login/oauth2/code/cognito`、旧 `WORKOPS_COGNITO_CLIENT_ID`、旧 `WORKOPS_COGNITO_REDIRECT_URI` が実装対象ソースに残っていないことを確認する
- エージェント確認では `AdminDeleteUser`、`AdminGetUser`、`AdminUpdateUserAttributes`、`AdminDisableUser` が CDK template に含まれていないことを確認する
- P2-7-02 の AWS dev 実確認結果を確認し、招待メール受信、初回パスワード変更、tenant login が完了済みであることを記録する
- RDS Console integrated CloudShell VPC environment が削除済みであることをユーザー確認で記録する
- ユーザーから destroy 対象 Stack の明示指示を得てから、`AppRuntimeStack`、`EdgeStack`、`EgressStack`、`DataStack` の順で削除する
- `IdentityStack` は削除しない
- cleanup 後、`IdentityStack` と Cognito User Pool / Hosted UI domain / App Client が残っていることを確認する
- cleanup 後、`AppRuntimeStack`、`EdgeStack`、`EgressStack`、`DataStack` が存在しないことを確認する
- P2-8 へ、CloudWatch Logs で見るべき認証・認可イベントと不整合調査観点を引き継ぐ

## ADR

- cleanup 対象に `IdentityStack` を含めない。Cognito User Pool / Hosted UI domain / App Client は Phase2 期間中の維持対象であり、短命 runtime stack と lifecycle を分けるため。
- メール未着時は未完了にする。P2-7 の完了条件に Cognito 招待メールの実配送確認を含めたため。
- 自動削除補償は実装しない。P2-7 の目的は `AdminCreateUser` と DB 登録の接続確認であり、DB 登録失敗後の Cognito 削除フローは範囲外である。
- CloudShell VPC environment の削除を cleanup 条件に含める。DB bootstrap と確認のためだけに使う一時 environment であり、確認後に残す運用対象ではないため。
- destroy は runtime 側から依存元へ向かう順序で実行する。ECS Service、CloudFront / ALB、NAT Gateway、RDS の順で削除し、参照関係による削除失敗を避けるため。
- P2-8 へログ確認を引き継ぐ。P2-7 は実Cognito接続と初回ログイン確認を中心にし、ログ分類と認証・認可イベントの体系化は P2-8 で扱うため。
- 既存データ移行と後方互換性は考慮しない。

## 対応範囲

- Java test 実行結果の整理
- CDK build / test 実行結果の整理
- CDK synth 確認手順
- AWS dev 実確認結果の記録
- メール未着時の未完了記録
- RDS Console integrated CloudShell VPC environment 削除確認
- 短命 Stack cleanup 手順
- `IdentityStack` 維持確認
- P2-8 への引き継ぎ

## 除外範囲

- P2-7-03 task 作成時点での AWS deploy
- P2-7-03 task 作成時点での AWS destroy
- `IdentityStack` destroy
- Cognito User Pool destroy
- Cognito ユーザー削除自動化
- CloudWatch Alarm
- Observability Dashboard
- 監査ログテーブル
- ログ検索画面
- GitHub Actions deploy
- CodePipeline
- CodeBuild

## 完了条件

- Java test が成功している
- CDK build が成功している
- CDK test が成功している
- `IdentityStack` template に日本語 HTML 招待メールテンプレートが含まれている
- `AppRuntimeStack` template に `cognito-idp:AdminCreateUser` のみが含まれている
- 旧 `/login/oauth2/code/cognito` が実装対象ソースから除去されている
- 旧 `WORKOPS_COGNITO_CLIENT_ID` が実装対象ソースから除去されている
- 旧 `WORKOPS_COGNITO_REDIRECT_URI` が実装対象ソースから除去されている
- AWS dev 実確認で初期 TENANT_MANAGER の招待メールを実受信できている
- AWS dev 実確認で初期 TENANT_MANAGER の初回パスワード変更が完了している
- AWS dev 実確認で初期 TENANT_MANAGER が TENANT として WorkOps にログインできている
- メール未着時は P2-7 未完了として記録されている
- RDS Console integrated CloudShell VPC environment が削除されている
- 実確認後、`AppRuntimeStack`、`EdgeStack`、`EgressStack`、`DataStack` が削除されている
- `IdentityStack` が維持されている
- P2-8 への引き継ぎが記録されている

## 確認方法

Java:

```powershell
cd C:\git\workops\apps\web
.\mvnw.cmd test
```

CDK:

```powershell
cd C:\git\workops\infra\cdk
npm run build
npm test -- --runInBand
```

CDK synth:

```powershell
cd C:\git\workops\infra\cdk
Remove-Item Env:AWS_ACCESS_KEY_ID -ErrorAction SilentlyContinue
Remove-Item Env:AWS_SECRET_ACCESS_KEY -ErrorAction SilentlyContinue
Remove-Item Env:AWS_SESSION_TOKEN -ErrorAction SilentlyContinue
$env:WORKOPS_STAGE='dev'
$env:AWS_PROFILE='amazon-connect'
$env:AWS_SDK_LOAD_CONFIG='1'
$env:AWS_REGION='ap-northeast-1'
$env:AWS_DEFAULT_REGION='ap-northeast-1'
$env:CDK_DEFAULT_ACCOUNT=(aws sts get-caller-identity --region $env:AWS_REGION --query Account --output text)
$env:CDK_DEFAULT_REGION='ap-northeast-1'
npm run cdk -- synth IdentityStack
npm run cdk -- synth AppRuntimeStack
```

旧設定残存確認:

```powershell
Select-String -Path 'apps\web\src\main\**\*','infra\cdk\lib\*.ts','infra\cdk\bin\cdk.ts' `
  -Pattern '/login/oauth2/code/cognito','WORKOPS_COGNITO_CLIENT_ID','WORKOPS_COGNITO_REDIRECT_URI' `
  -Encoding UTF8
```


```powershell
cd C:\git\workops\infra\cdk
Remove-Item Env:AWS_ACCESS_KEY_ID -ErrorAction SilentlyContinue
Remove-Item Env:AWS_SECRET_ACCESS_KEY -ErrorAction SilentlyContinue
Remove-Item Env:AWS_SESSION_TOKEN -ErrorAction SilentlyContinue
$env:WORKOPS_STAGE='dev'
$env:AWS_PROFILE='amazon-connect'
$env:AWS_SDK_LOAD_CONFIG='1'
$env:AWS_REGION='ap-northeast-1'
$env:AWS_DEFAULT_REGION='ap-northeast-1'
$env:CDK_DEFAULT_ACCOUNT=(aws sts get-caller-identity --region $env:AWS_REGION --query Account --output text)
$env:CDK_DEFAULT_REGION='ap-northeast-1'
npm run cdk -- destroy AppRuntimeStack
npm run cdk -- destroy EdgeStack
npm run cdk -- destroy EgressStack
npm run cdk -- destroy DataStack
```

維持対象確認:

```powershell
aws cloudformation describe-stacks --stack-name workops-dev-identity --region $env:AWS_REGION
aws cognito-idp list-user-pools --max-results 10 --region $env:AWS_REGION
```

cleanup 後に確認すること:

- `workops-dev-identity` が存在する
- Cognito User Pool が存在する
- Cognito Hosted UI domain が存在する
- Platform App Client が存在する
- Tenant App Client が存在する
- `workops-dev-app-runtime` が削除されている
- `workops-dev-edge` が削除されている
- `workops-dev-egress` が削除されている
- `workops-dev-data` が削除されている

## P2-8 引き継ぎ

- P2-8 では CloudWatch Logs で Cognito `AdminCreateUser` 成功・失敗時のアプリログを確認する
- P2-8 では Hosted UI ログイン成功後の `users.cognito_sub` 突合失敗 WARN を確認する
- P2-8 では PLATFORM / TENANT login route と `actor_type` 不一致 WARN を確認する
- P2-8 では AccessDenied、未ログイン、認証切れのログ確認を扱う
- P2-8 では Cognito 内部ログ取得ではなく、WorkOps アプリ側で調査可能なログを中心にする
