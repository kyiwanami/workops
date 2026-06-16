# P2-4-04 検証 / cleanup / 記録

## 目的

P2-4 の AWS dev 実行確認結果を整理し、CloudFront default domain の HTTPS 入口、HTTP to HTTPS redirect、P2-5 へのベース URL、実行確認セッション終了後の Stack 削除を記録する。
確認結果は task Markdown に記録し、エージェント確認とユーザー確認を分ける。

## ユーザー要求

- P2-4 確認後は `AppRuntimeStack`、`EdgeStack`、`EgressStack`、`DataStack` の順で削除する
- CloudFront distribution 削除は時間がかかる前提を明記する
- 削除後、ALB、CloudFront distribution、NAT Gateway、RDS、ALB / app Security Group ingress が残っていないことを確認する
- 維持対象は `FoundationStack`、`RegistryStack`、`LogsStack` を基本にする
- P2-5 には `cloudFrontHttpsUrl` をベース URL として引き継ぐ
- タイムゾーン対応は P2-8 に移す

## 前提

- P2-4-03 で AWS dev への手動 deploy が完了している
- CloudFront 経由で `/actuator/health` が HTTPS 200 を返している
- CloudFront 経由で `/` が HTTPS 200 を返している
- CloudFront の HTTP request が HTTPS へ redirect されている
- `cloudFrontHttpsUrl` が P2-5 のベース URL として記録されている
- P2-4-03 で `DataStack` を再deployしている

## 案

- P2-4 のエージェント確認結果を記録する
- P2-4 のユーザー確認結果を記録する
- `cloudFrontDomainName` を記録する
- `cloudFrontHttpsUrl` を P2-5 ベース URL として記録する
- CloudWatch Logs `/workops/dev/web` の Spring Boot 起動ログを確認する
- CloudFront distribution の状態を確認する
- ALB target group health を確認する
- ECS Service / task 状態を確認する
- 実行確認セッション終了後に `AppRuntimeStack` を destroy する
- `AppRuntimeStack` 削除後に `EdgeStack` を destroy する
- `EdgeStack` 削除後に `EgressStack` を destroy する
- `EgressStack` 削除後に `DataStack` を destroy する
- CloudFront distribution 削除は完了まで待つ
- 削除後に一時リソースが残っていないことを確認する

## ADR

- destroy 順序は `AppRuntimeStack`、`EdgeStack`、`EgressStack`、`DataStack` とする。ECS service、CloudFront / ALB、NAT Gateway、RDS の依存関係に合わせるため。
- `EdgeStack` は確認後に削除する。CloudFront distribution は実行確認セッション用であり、P2-4 完了後に常設しない方針であるため。
- `DataStack` も P2-4 cleanup で削除する。P2-4 では P2-3 cleanup 後に再作成した RDS を使うため、確認完了後に RDS 課金を残さない。
- CloudFront distribution 削除は時間がかかる前提にする。CloudFront は distribution disable / delete に待ち時間が発生する AWS リソースであるため。
- P2-5 の callback / sign-out path は記録しない。P2-4 の責務は `cloudFrontHttpsUrl` をベース URL として確定することに限定するため。
- タイムゾーン対応は P2-8 で扱う。ログ確認と時刻表示の文脈で判断するため。
- 既存データ移行と後方互換性は考慮しない。

## 対応範囲

- P2-4 の確認結果を task Markdown に記録する
- CloudFront HTTPS health 確認結果を記録する
- CloudFront HTTPS `/` 確認結果を記録する
- CloudFront HTTP to HTTPS redirect 確認結果を記録する
- `cloudFrontDomainName` を記録する
- `cloudFrontHttpsUrl` を P2-5 ベース URL として記録する
- CloudWatch Logs 確認結果を記録する
- ECS Service / task / target group health の確認結果を記録する
- destroy 手順を記録する
- 削除後確認手順を記録する
- P2-5 への引き継ぎ事項を記録する
- P2-8 へ送るタイムゾーン事項を記録する

## 除外範囲

- CloudFront distribution の常設
- `AppRuntimeStack` を `desiredCount=0` で残す運用
- RDS の常設
- Cognito Hosted UI
- users 突合
- callback / sign-out path 確定
- 業務操作確認
- GitHub Actions workflow
- CodeBuild
- CodePipeline
- CloudWatch Alarm
- Observability Dashboard
- タイムゾーン実装
- git commit

## 完了条件

- `cloudFrontDomainName` が記録されている
- `cloudFrontHttpsUrl` が P2-5 ベース URL として記録されている
- `https://{cloudFrontDomainName}/actuator/health` が HTTP 200 を返したことが記録されている
- `https://{cloudFrontDomainName}/` が HTTP 200 を返したことが記録されている
- `http://{cloudFrontDomainName}/actuator/health` が HTTPS へ redirect されたことが記録されている
- `/workops/dev/web` に Spring Boot 起動ログが出ている
- `AppRuntimeStack` を destroy できる
- `EdgeStack` を destroy できる
- `EgressStack` を destroy できる
- `DataStack` を destroy できる
- CloudFront distribution が削除済みである
- ALB が削除済みである
- NAT Gateway が削除済みである
- RDS instance `workops-dev-db` が削除済みである
- `workops-dev-alb-sg` と `workops-dev-app-sg` の ingress が空である
- 維持対象の `FoundationStack`、`RegistryStack`、`LogsStack` が残っている

## 確認方法

エージェント確認:

```powershell
cd C:\git\workops\infra\cdk
npm run build
npm test -- --runInBand
$env:WORKOPS_STAGE='dev'
npm run cdk -- synth
```

ユーザー確認:

CloudFront / ECS / ALB 状態:

```powershell
cd C:\git\workops\infra\cdk
$outputs = Get-Content -LiteralPath .\p2-4-03-outputs.json -Encoding UTF8 | ConvertFrom-Json
$cloudFrontDomainName = $outputs.EdgeStack.cloudFrontDomainName
$cloudFrontHttpsUrl = $outputs.EdgeStack.cloudFrontHttpsUrl

aws cloudfront list-distributions `
  --query "DistributionList.Items[?DomainName=='$cloudFrontDomainName'].[Id,DomainName,Status,Enabled]" `
  --output table

aws ecs describe-services --cluster workops-dev-cluster --services workops-dev-web
```

CloudWatch Logs:

- Log group `/workops/dev/web`
- stream prefix `web`
- Spring Boot 起動ログ
- `/actuator/health` の応答に関する異常がないこと
- CloudFront 経由アクセス時のアプリ例外がないこと

destroy:

```powershell
cd C:\git\workops\infra\cdk
$env:WORKOPS_STAGE='dev'
npm run cdk -- destroy AppRuntimeStack
npm run cdk -- destroy EdgeStack
npm run cdk -- destroy EgressStack
npm run cdk -- destroy DataStack
```

CloudFront distribution の削除は時間がかかる。
`EdgeStack` destroy が完了するまで、別 terminal で distribution status を確認する。

削除後に確認する:

```powershell
aws cloudformation describe-stacks --stack-name workops-dev-app-runtime
aws cloudformation describe-stacks --stack-name workops-dev-edge
aws cloudformation describe-stacks --stack-name workops-dev-egress
aws cloudformation describe-stacks --stack-name workops-dev-data

aws elbv2 describe-load-balancers --names workops-dev-web-alb
aws rds describe-db-instances --db-instance-identifier workops-dev-db
aws ecs describe-services --cluster workops-dev-cluster --services workops-dev-web
```

Security Group ingress 確認:

```powershell
aws ec2 describe-security-groups `
  --filters Name=group-name,Values=workops-dev-alb-sg,workops-dev-app-sg `
  --query "SecurityGroups[].{GroupName:GroupName,Ingress:IpPermissions}" `
  --output table
```

維持対象 Stack 確認:

```powershell
aws cloudformation describe-stacks --stack-name workops-dev-foundation
aws cloudformation describe-stacks --stack-name workops-dev-registry
aws cloudformation describe-stacks --stack-name workops-dev-logs
```

P2-5 引き継ぎ:

```text
P2-5 base URL: <cloudFrontHttpsUrl の値>
callback path: P2-5 で確定
sign-out path: P2-5 で確定
```

P2-8 引き継ぎ:

```text
P2-8 で TZ=Asia/Tokyo または JAVA_TOOL_OPTIONS=-Duser.timezone=Asia/Tokyo の採否を決める。
```
