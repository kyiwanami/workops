# P2-alpha-3-02 Pipeline runtime verification

## 目的

P2-alpha-3-01 で起動・承認した Pipeline について、Pipeline 完走、Registry、Build Images、Migration RunTask、AppRuntime、CloudFront、Alarm、Logs を協働で確認する。

CLI で確認できる項目は CLI を正とし、メール、ブラウザ、Console 操作が必要なものはユーザー操作を含む協働確認として扱う。

## ユーザー要求

- 2α-3 の確認は CLI で確認できる項目を CLI 正とする
- Console / メール / ブラウザしか確認できないものはユーザー確認として扱う
- CloudFront default domain は HTTP 200 とトップページ到達の両方を確認する
- CloudFront domain の実値は git 管理文書に残さない
- Migration RunTask は exit code 0 を正とする
- CloudWatch Logs は Flyway locations と schema 適用完了の確認までとし、SQL全文やDB実値を記録しない
- Alarm は bake 後に対象 Alarm が `ALARM` でないことを CLI で確認し、時系列詳細は追わない

## 前提

- P2-alpha-3-01 が完了している
- Pipeline が `main` push を契機に起動している
- ManualApproval が承認済みである
- P2-alpha-2 の実装結果に基づく Stack 名、Pipeline 名、Output key、Alarm 名、container 名を確認できる

## 案

Pipeline 完走を確認したうえで、各 stage の成果を CLI で確認する。

確認対象は、Stack 状態、ECR repository と image tag、Migration RunTask exit code、Target Group health、ECS Task の CPU architecture、CloudFront HTTP 200、Alarm 状態、CloudWatch Logs の致命的 ERROR なしとする。

ブラウザ上のトップページ到達、ManualApproval、メール受信などはユーザー操作を含む協働確認として記録する。

## 判断メモ

- Pipeline stage 成功だけで完了扱いにしない。実際に作成・更新された AWS リソースの状態を CLI で確認するため。
- CloudWatch Logs は証跡確認に留める。SQL全文、endpoint、DB実データ値を記録しないため。
- Alarm は `ALARM` でないことを確認する。P2-alpha-3 では本番監視設計や時系列分析を行わないため。

## 対応範囲

- Pipeline 完走確認
- `PipelineStack` / `RegistryStack` / `DataStack` / `EgressStack` / `EdgeStack` / `MigrationStack` / `AppRuntimeStack` の状態確認
- ECR repository 4 本の存在確認
- Web image / migration image の commit SHA tag push 確認
- Migration RunTask の exit code 0 確認
- CloudWatch Logs 上の Flyway schema 適用完了確認
- AppRuntimeStack deploy 完了確認
- ALB Target Group healthy 確認
- ECS Task `cpuArchitecture: ARM_64` 確認
- CloudFront default domain 経由の HTTP 200 確認
- ブラウザ上のトップページ到達確認
- bake 後の Alarm 非 `ALARM` 確認
- CloudWatch Logs の致命的 ERROR なし確認

## 除外範囲

- CDK code 変更
- PipelineStack 初回 deploy
- CodeConnection OAuth 認可
- ManualApproval 操作そのもの
- ベースライン記録の最終集約
- 主要課金 4 stack destroy
- 失敗事象の最終集約

## 完了条件

Pipeline:

- Source が CodeConnections で `main` を取得して起動している
- Synth stage で `cdk synth` が pass している
- Build & Test stage で全品質ゲートが pass している
- Deploy Registry stage で RegistryStack が deploy され、ECR repository 4 本が存在する
- Build Images stage で Web image と migration image が並列 build され、ECR に commit SHA tag で push されている
- Deploy Data / Network / Migration stage が完走している
- Migration RunTask stage が完走している
- Deploy AppRuntime stage が完走している
- Pipeline 全体がエラーなく 1 周完走している

Migration:

- ECS RunTask が exit code 0 で終了している
- CloudWatch Logs 上で Flyway の schema 適用完了が確認できる
- Flyway 適用後、アプリケーションから DB 接続が成功している

App:

- Blue/Green 切替が完了している
- ALB Target Group が healthy を示している
- CloudFront default domain 経由で top page への HTTP 200 を取得できる
- ブラウザでトップページ到達を確認している
- ECS Task が `cpuArchitecture: ARM_64` で起動している

Alarm / Log:

- bake 期間後、連動 Alarm が `ALARM` ではない
- CloudWatch Logs に致命的 ERROR がない

## 確認方法

Pipeline 状態:

```powershell
aws codepipeline get-pipeline-state `
  --name "<pipeline-name>" `
  --profile $env:AWS_PROFILE `
  --region $env:AWS_REGION

aws codepipeline list-pipeline-executions `
  --pipeline-name "<pipeline-name>" `
  --profile $env:AWS_PROFILE `
  --region $env:AWS_REGION
```

Stack 状態:

```powershell
aws cloudformation describe-stacks `
  --profile $env:AWS_PROFILE `
  --region $env:AWS_REGION `
  --query "Stacks[?contains(StackName, 'workops')].[StackName,StackStatus]" `
  --output table
```

ECR repository / image tag:

```powershell
aws ecr describe-repositories `
  --profile $env:AWS_PROFILE `
  --region $env:AWS_REGION `
  --query "repositories[?contains(repositoryName, 'workops')].[repositoryName,imageTagMutability]" `
  --output table

aws ecr describe-images `
  --repository-name "<web-repository-name>" `
  --profile $env:AWS_PROFILE `
  --region $env:AWS_REGION `
  --query "imageDetails[].imageTags" `
  --output table

aws ecr describe-images `
  --repository-name "<migration-repository-name>" `
  --profile $env:AWS_PROFILE `
  --region $env:AWS_REGION `
  --query "imageDetails[].imageTags" `
  --output table
```

Migration RunTask:

```powershell
aws ecs describe-tasks `
  --cluster "<cluster-name-or-arn>" `
  --tasks "<task-arn>" `
  --profile $env:AWS_PROFILE `
  --region $env:AWS_REGION `
  --query "tasks[].{lastStatus:lastStatus,stopCode:stopCode,stoppedReason:stoppedReason,containers:containers[].{name:name,exitCode:exitCode,reason:reason}}"
```

Flyway logs:

```powershell
aws logs filter-log-events `
  --log-group-name "<migration-log-group-name>" `
  --filter-pattern "Flyway" `
  --profile $env:AWS_PROFILE `
  --region $env:AWS_REGION
```

Target Group health:

```powershell
aws elbv2 describe-target-health `
  --target-group-arn "<target-group-arn>" `
  --profile $env:AWS_PROFILE `
  --region $env:AWS_REGION
```

ECS Task architecture:

```powershell
aws ecs describe-task-definition `
  --task-definition "<task-definition-arn>" `
  --profile $env:AWS_PROFILE `
  --region $env:AWS_REGION `
  --query "taskDefinition.runtimePlatform"
```

CloudFront HTTP 200:

```powershell
$response = Invoke-WebRequest -Uri "https://<cloudfront-default-domain>/" -UseBasicParsing
$response.StatusCode
```

CloudFront default domain の実値は記録しない。

Alarm:

```powershell
aws cloudwatch describe-alarms `
  --alarm-names "<alarm-name-1>" "<alarm-name-2>" `
  --profile $env:AWS_PROFILE `
  --region $env:AWS_REGION `
  --query "MetricAlarms[].{AlarmName:AlarmName,StateValue:StateValue}" `
  --output table
```

Application logs:

```powershell
aws logs filter-log-events `
  --log-group-name "<web-log-group-name>" `
  --filter-pattern "ERROR" `
  --profile $env:AWS_PROFILE `
  --region $env:AWS_REGION
```

## 実装時の記録

### 実施結果

- 未実施。

### 確認結果

- 未実施。

### 残課題

- 未実施。
