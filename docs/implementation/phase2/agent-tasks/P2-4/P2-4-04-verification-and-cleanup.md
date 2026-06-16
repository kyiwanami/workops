# P2-4-04 検証 / cleanup 延期 / 記録

## 目的

P2-4 の AWS dev 実行確認結果を整理し、CloudFront default domain の HTTPS 入口、HTTP to HTTPS redirect、P2-5 へのベース URL、P2-5 へ進むための Stack 維持を記録する。
確認結果は task Markdown に記録し、エージェント確認とユーザー確認を分ける。

## ユーザー要求

- P2-4 確認後は `AppRuntimeStack`、`EdgeStack`、`EgressStack`、`DataStack` の順で削除する
- P2-5 に進むため、今回は `AppRuntimeStack`、`EdgeStack`、`EgressStack`、`DataStack` を削除しない
- CloudFront distribution 削除は時間がかかる前提を明記する
- 削除後、ALB、CloudFront distribution、NAT Gateway、RDS、ALB / app Security Group ingress が残っていないことを確認する
- 維持対象は `FoundationStack`、`RegistryStack`、`LogsStack` を基本にする
- P2-5 には `EdgeStack.cloudFrontHttpsUrl` を CDK props / cross-stack reference として引き継ぐ
- タイムゾーン対応は P2-8 に移す

## 前提

- P2-4-03 で AWS dev への手動 deploy が完了している
- CloudFront 経由で `/actuator/health` が HTTPS 200 を返している
- CloudFront 経由で `/` が HTTPS 200 を返している
- CloudFront の HTTP request が HTTPS へ redirect されている
- `EdgeStack.cloudFrontHttpsUrl` が P2-5 のベース URL の CDK 参照元として記録されている
- P2-4-03 で `DataStack` を再deployしている
- P2-5 は同じ AWS dev 実行確認セッション上で続行する

## 案

- P2-4 のエージェント確認結果を記録する
- P2-4 のユーザー確認結果を記録する
- `cloudFrontDomainName` を記録する
- `EdgeStack.cloudFrontHttpsUrl` を P2-5 ベース URL の CDK 参照元として記録する
- CloudWatch Logs `/workops/dev/web` の Spring Boot 起動ログを確認する
- CloudFront distribution の状態を確認する
- ALB target group health を確認する
- ECS Service / task 状態を確認する
- P2-5 へ進むため `AppRuntimeStack`、`EdgeStack`、`EgressStack`、`DataStack` の cleanup を延期する
- cleanup 延期により課金対象リソースが残ることを記録する
- P2-5 開始時の前提として、CloudFront、ALB、NAT Gateway、RDS、ECS service が残っていることを確認する
- 実行確認セッション終了時の cleanup 順序は `AppRuntimeStack`、`EdgeStack`、`EgressStack`、`DataStack` のまま維持する

## ADR

- destroy 順序は `AppRuntimeStack`、`EdgeStack`、`EgressStack`、`DataStack` とする。ECS service、CloudFront / ALB、NAT Gateway、RDS の依存関係に合わせるため。
- P2-5 へ進む今回は cleanup を延期する。P2-5 の Cognito Hosted UI 確認では、P2-4 で確定した CloudFront HTTPS 入口、ECS runtime、RDS、NAT egress が必要なため。
- `EdgeStack` は P2-5 の確認が終わるまで維持する。P2-5 の callback / sign-out URL は `EdgeStack.cloudFrontHttpsUrl` を CDK 参照元として組み立てるため。
- `DataStack` は P2-5 の確認が終わるまで維持する。P2-5 では Cognito `sub` と DB の `users` 突合を確認するため。
- `EgressStack` と `AppRuntimeStack` は P2-5 の確認が終わるまで維持する。Hosted UI ログイン後の Web runtime と外向き通信を保つため。
- CloudFront distribution 削除は時間がかかる前提にする。CloudFront は distribution disable / delete に待ち時間が発生する AWS リソースであるため。
- P2-5 の callback / sign-out path は記録しない。P2-4 の責務は `EdgeStack.cloudFrontHttpsUrl` を P2-5 の CDK 参照元として確定することに限定するため。
- タイムゾーン対応は P2-8 で扱う。ログ確認と時刻表示の文脈で判断するため。
- 既存データ移行と後方互換性は考慮しない。

## 対応範囲

- P2-4 の確認結果を task Markdown に記録する
- CloudFront HTTPS health 確認結果を記録する
- CloudFront HTTPS `/` 確認結果を記録する
- CloudFront HTTP to HTTPS redirect 確認結果を記録する
- `cloudFrontDomainName` を記録する
- `EdgeStack.cloudFrontHttpsUrl` を P2-5 ベース URL の CDK 参照元として記録する
- CloudWatch Logs 確認結果を記録する
- ECS Service / task / target group health の確認結果を記録する
- cleanup 延期理由を記録する
- P2-5 開始時に維持する Stack と課金対象リソースを記録する
- 実行確認セッション終了時の destroy 手順を記録する
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
- P2-4-04 時点での AWS Stack destroy
- git commit

## 完了条件

- `cloudFrontDomainName` が記録されている
- `EdgeStack.cloudFrontHttpsUrl` が P2-5 ベース URL の CDK 参照元として記録されている
- `https://{cloudFrontDomainName}/actuator/health` が HTTP 200 を返したことが記録されている
- `https://{cloudFrontDomainName}/` が HTTP 200 を返したことが記録されている
- `http://{cloudFrontDomainName}/actuator/health` が HTTPS へ redirect されたことが記録されている
- `/workops/dev/web` に Spring Boot 起動ログが出ている
- P2-5 へ進むため cleanup を延期したことが記録されている
- `AppRuntimeStack` が維持されている
- `EdgeStack` が維持されている
- `EgressStack` が維持されている
- `DataStack` が維持されている
- CloudFront distribution が `Deployed` である
- ECS Service が `desiredCount=1` / `runningCount=1` である
- ALB target group target が `healthy` である
- RDS instance `workops-dev-db` が存在する
- 維持対象の `FoundationStack`、`RegistryStack`、`LogsStack` が残っている

## 実施結果

2026-06-16 時点で、P2-4 の HTTPS 入口確認は完了している。
P2-5 へ同じ AWS dev 実行確認セッションを継続するため、`AppRuntimeStack`、`EdgeStack`、`EgressStack`、`DataStack` の cleanup は延期する。

エージェント確認:

- `infra/cdk` の build / test / synth は P2-4-03 で成功済み
- `EdgeStack.cloudFrontHttpsUrl` が P2-5 のベース URL の CDK 参照元であることを確認済み
- `p2-4-03-outputs.json` のような環境固有 output file を正本として使わない方針へ修正済み

AWS dev 確認:

- AWS account は `REMOVED_AWS_ACCOUNT_ID`
- AWS region は `ap-northeast-1`
- `workops-dev-data` は `CREATE_COMPLETE`
- `workops-dev-egress` は `CREATE_COMPLETE`
- `workops-dev-edge` は `CREATE_COMPLETE`
- `workops-dev-app-runtime` は `CREATE_COMPLETE`
- CloudFront distribution は `Deployed`
- ECS Service `workops-dev-web` は `desiredCount=1` / `runningCount=1`
- HTTPS `/actuator/health` は HTTP 200
- HTTPS `/` は HTTP 200
- HTTP `/actuator/health` は HTTPS へ 301 redirect

P2-5 引き継ぎ:

- P2-5 base URL source: `EdgeStack.cloudFrontHttpsUrl`
- callback path: P2-5 で確定
- sign-out path: P2-5 で確定
- `AppRuntimeStack`、`EdgeStack`、`EgressStack`、`DataStack` は P2-5 のため維持する

延期した cleanup:

```powershell
cd C:\git\workops\infra\cdk
$env:WORKOPS_STAGE='dev'
npm run cdk -- destroy AppRuntimeStack --force
npm run cdk -- destroy EdgeStack --force
npm run cdk -- destroy EgressStack --force
npm run cdk -- destroy DataStack --force
```

注意:

- CloudFront distribution の削除には時間がかかる
- P2-5 へ進む間、CloudFront、ALB、NAT Gateway、ECS Fargate、RDS の課金対象リソースが残る

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
$edgeStack = aws cloudformation describe-stacks --stack-name workops-dev-edge | ConvertFrom-Json
$cloudFrontDomainName = ($edgeStack.Stacks[0].Outputs | Where-Object OutputKey -eq 'cloudFrontDomainName').OutputValue
$cloudFrontHttpsUrl = ($edgeStack.Stacks[0].Outputs | Where-Object OutputKey -eq 'cloudFrontHttpsUrl').OutputValue

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

実行確認セッション終了時の destroy:

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
P2-5 base URL source: EdgeStack.cloudFrontHttpsUrl
callback path: P2-5 で確定
sign-out path: P2-5 で確定
```

P2-8 引き継ぎ:

```text
P2-8 で TZ=Asia/Tokyo または JAVA_TOOL_OPTIONS=-Duser.timezone=Asia/Tokyo の採否を決める。
```
