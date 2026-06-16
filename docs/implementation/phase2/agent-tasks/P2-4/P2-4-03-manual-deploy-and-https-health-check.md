# P2-4-03 手動 deploy / HTTPS health 確認

## 目的

P2-4 で拡張した `EdgeStack` を AWS dev に手動 deploy し、CloudFront default domain の HTTPS URL から WorkOps へ到達できることを確認する。
AWS 実操作はユーザー確認として扱い、エージェント確認と混同しない。

## ユーザー要求

- `DataStack`、`EgressStack`、`EdgeStack`、`AppRuntimeStack` を再作成する
- CloudFront URL は `cloudFrontHttpsUrl` Output から取得する
- `https://{cloudFrontDomainName}/actuator/health` を確認する
- `https://{cloudFrontDomainName}/` を確認する
- `http://{cloudFrontDomainName}/actuator/health` が HTTPS へ redirect されることを確認する
- P2-5 には `cloudFrontHttpsUrl` をベース URL として渡す
- callback / sign-out の path は P2-5 で決める
- P2-4 runtime は P2-3 と同じく `SPRING_PROFILES_ACTIVE=local` のままにする

## 前提

- P2-4-01 と P2-4-02 が完了している
- `apps/web` の Docker image `p2-3-manual` が ECR repository `workops-${stage}-web` に存在する
- P2-3 cleanup により `DataStack`、`EgressStack`、`EdgeStack`、`AppRuntimeStack` は削除済みである
- P2-4 の AWS deploy 前に AWS account / region、既存 Stack 状態、CDK diff を確認する
- GitHub Actions deploy は使わない
- Cognito Hosted UI ログインは確認しない
- 業務操作は確認しない

## 案

- `apps/web` の Maven test を実行する
- `infra/cdk` の build / test / synth を実行する
- AWS account / region を確認する
- 維持対象 Stack の状態を確認する
- `DataStack`、`EgressStack`、`EdgeStack`、`AppRuntimeStack` の diff を確認する
- `DataStack` を deploy する
- `EgressStack` を deploy する
- `EdgeStack` を deploy し、Output を `p2-4-03-outputs.json` に保存する
- `AppRuntimeStack` を deploy する
- `cloudFrontDomainName` と `cloudFrontHttpsUrl` を `EdgeStack` Output から取得する
- CloudFront distribution の deploy 完了を待つ
- CloudFront 経由で `/actuator/health` の HTTPS 200 を確認する
- CloudFront 経由で `/` の HTTPS 200 を確認する
- CloudFront HTTP URL が HTTPS へ redirect されることを確認する
- `cloudFrontHttpsUrl` を P2-5 への引き継ぎ値として記録する

## ADR

- P2-4 の deploy は手動で行う。GitHub Actions OIDC deploy は P2-9 の責務であり、P2-4 では CloudFront HTTPS 入口を個別に確認するため。
- AWS 実操作はユーザー確認として扱う。CloudFront、ALB、NAT Gateway、ECS、RDS は課金対象リソースであり、AWS 認証情報もユーザー環境に依存するため。
- P2-4 runtime は `SPRING_PROFILES_ACTIVE=local` のままにする。P2-4 は HTTPS 入口確認であり、Cognito OAuth2 Client 設定不足をここで扱わないため。
- `DataStack` は P2-4 deploy 前に再作成する。P2-3 cleanup で削除済みであり、ECS task の DB 接続 secret / parameter がないと runtime を起動できないため。
- `cloudFrontHttpsUrl` は `EdgeStack` Output から取得する。P2-5 の入力となる HTTPS ベース URL を AWS Console 上の目視だけに依存させないため。
- `/actuator/health` と `/` を確認する。health で runtime 経路を確認し、`/` で Thymeleaf のトップ画面到達を確認するため。
- 業務操作は確認しない。P2-4 は HTTPS 入口の確認であり、業務画面操作や認証は P2-5 以降の責務であるため。
- 既存データ移行と後方互換性は考慮しない。

## 対応範囲

- Maven test 手順を記録する
- CDK build / test / synth 手順を記録する
- AWS account / region / stack state / CDK diff の事前確認手順を記録する
- `DataStack` 再deploy手順を記録する
- `EgressStack` deploy手順を記録する
- `EdgeStack` deploy手順を記録する
- `AppRuntimeStack` deploy手順を記録する
- `cloudFrontDomainName` と `cloudFrontHttpsUrl` の取得手順を記録する
- CloudFront distribution deploy 完了待ち手順を記録する
- HTTPS `/actuator/health` 確認手順を記録する
- HTTPS `/` 確認手順を記録する
- HTTP から HTTPS への redirect 確認手順を記録する
- P2-5 へ渡すベース URL の記録欄を用意する

## 除外範囲

- ECR push
- Docker build
- GitHub Actions workflow
- CodeBuild
- CodePipeline
- Cognito Hosted UI
- users 突合
- callback / sign-out path 確定
- 業務操作確認
- 権限確認
- CloudFront custom domain
- Route 53
- ACM 独自ドメイン証明書
- ALB HTTPS listener
- destroy 手順
- git commit

## 完了条件

- `apps/web` の Maven test が成功している
- `infra/cdk` の build / test / synth が成功している
- AWS account / region を確認している
- CDK diff を確認している
- `DataStack` を deploy できる
- `EgressStack` を deploy できる
- `EdgeStack` を deploy できる
- `AppRuntimeStack` を deploy できる
- `EdgeStack` Output から `cloudFrontDomainName` を取得できる
- `EdgeStack` Output から `cloudFrontHttpsUrl` を取得できる
- CloudFront distribution が deploy 済みになる
- `https://{cloudFrontDomainName}/actuator/health` が HTTP 200 を返す
- `https://{cloudFrontDomainName}/` が HTTP 200 を返す
- `http://{cloudFrontDomainName}/actuator/health` が HTTPS へ redirect される
- `cloudFrontHttpsUrl` が P2-5 のベース URL として記録されている

## 確認方法

エージェント確認:

```powershell
cd C:\git\workops\apps\web
.\mvnw.cmd test

cd C:\git\workops\infra\cdk
npm run build
npm test -- --runInBand
$env:WORKOPS_STAGE='dev'
npm run cdk -- synth
```

ユーザー確認:

```powershell
$env:WORKOPS_STAGE='dev'

cd C:\git\workops\infra\cdk
aws sts get-caller-identity
aws configure get region

aws cloudformation describe-stacks --stack-name workops-dev-foundation
aws cloudformation describe-stacks --stack-name workops-dev-registry
aws cloudformation describe-stacks --stack-name workops-dev-logs
# workops-dev-data は削除済み前提のため、ValidationError の場合は想定どおり。
aws cloudformation describe-stacks --stack-name workops-dev-data

npm run cdk -- diff DataStack
npm run cdk -- diff EgressStack
npm run cdk -- diff EdgeStack
npm run cdk -- diff AppRuntimeStack

npm run cdk -- deploy DataStack
npm run cdk -- deploy EgressStack
npm run cdk -- deploy EdgeStack --outputs-file .\p2-4-03-outputs.json
npm run cdk -- deploy AppRuntimeStack
```

CloudFront URL 取得:

```powershell
$outputs = Get-Content -LiteralPath .\p2-4-03-outputs.json -Encoding UTF8 | ConvertFrom-Json
$cloudFrontDomainName = $outputs.EdgeStack.cloudFrontDomainName
$cloudFrontHttpsUrl = $outputs.EdgeStack.cloudFrontHttpsUrl
$cloudFrontDomainName
$cloudFrontHttpsUrl
```

CloudFront distribution の deploy 完了確認:

```powershell
aws cloudfront list-distributions `
  --query "DistributionList.Items[?DomainName=='$cloudFrontDomainName'].[Id,DomainName,Status,Enabled]" `
  --output table
```

HTTPS health 確認:

```powershell
Invoke-WebRequest -Uri "$cloudFrontHttpsUrl/actuator/health" -UseBasicParsing
```

トップ画面確認:

```powershell
Invoke-WebRequest -Uri "$cloudFrontHttpsUrl/" -UseBasicParsing
```

HTTP から HTTPS への redirect 確認:

```powershell
curl.exe -I "http://$cloudFrontDomainName/actuator/health"
```

AWS Console 確認:

- CloudFormation Stack `workops-dev-data` が作成済み
- CloudFormation Stack `workops-dev-egress` が作成済み
- CloudFormation Stack `workops-dev-edge` が作成済み
- CloudFormation Stack `workops-dev-app-runtime` が作成済み
- CloudFront distribution が Deployed である
- ALB target group target が healthy である
- ECS Service が `desiredCount=1` である
- ECS task が `RUNNING` である
- CloudWatch Logs `/workops/dev/web` に Spring Boot 起動ログが出ている

P2-5 引き継ぎ:

```text
P2-5 base URL: <cloudFrontHttpsUrl の値>
```
