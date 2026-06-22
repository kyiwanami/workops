# P2-alpha-2-05 Migration RunTask

## 目的

`MigrationStack` を追加し、Pipeline の Migration RunTask stage から Flyway 専用 migration image を ECS RunTask として実行し、exit code 0 の場合だけ成功にする。

## ユーザー要求

- P2-alpha-2 の task 分割は、`phases.md` の責務境界を最上位単位にする
- 既にコード、Notion、`phases.md` で分かることは質問しない
- task ファイル末尾に `実装時の記録`、`実装結果`、`確認結果`、`残課題` を置く

## 前提

- P2-alpha-2-01 が完了している
- P2-alpha-2-02 が完了している
- P2-alpha-2-03 が完了している
- P2-alpha-2-04 が完了している
- MigrationStack は ECS Service を作らない
- MigrationStack は主要課金 4 stack destroy 対象に含めない
- migration task は app private subnet で起動し、`app-sg` を使う
- RDS への接続は既存の `db-sg` ingress from `app-sg`:3306 を使う

## 案

`MigrationStack` を追加し、次を所有させる。

- Flyway 専用 image 実行用 ECS Task Definition
- migration container definition
- task execution role / task role
- CloudWatch Logs log group 参照
- RunTask buildspec が必要とする CloudFormation Outputs

MigrationStack は次を props で受け取る。

- ECS cluster
- app private subnets
- app security group
- migration repository
- migration log group
- stage

Migration RunTask stage は CodeBuild から `infra/cdk/scripts/run-migration-task.py` を呼び、script 内で AWS CLI を実行する。

- `aws ecs run-task`
- `run-task` の `failures` が空であることを確認
- taskArn を取得
- `aws ecs wait tasks-stopped`
- `aws ecs describe-tasks`
- `stopCode`、`stoppedReason`、`containers[].exitCode`、`containers[].reason` を確認
- migration container の `exitCode` が 0 の場合だけ成功

RunTask に必要な cluster、task definition、subnet、security group、container name は buildspec や script に固定値として書かない。
CloudFormation Outputs から必要値を取得して `awsvpcConfiguration` を組み立てる。

DB 接続情報は Web と同じ secret / parameter 経路を使う。
Migration 専用 DB user、secret、Security Group は作らない。

## 判断メモ

- RunTask 起動だけで成功扱いにしない。migration 成否は container exit code で判定するため。
- Lambda、Step Functions、custom resource による RunTask 結果判定は使わない。`phases.md` の除外範囲であるため。
- CloudFormation Outputs は RunTask 実行時の値取得に使うが、Stack 間参照の正本にはしない。

## 対応範囲

- `infra/cdk/lib/migration-stack.ts`
- PipelineStack の Deploy Data / Network / Migration stage
- PipelineStack の Migration RunTask stage
- Migration RunTask script
- migration ECS Task Definition
- CloudFormation Outputs
- `infra/cdk/test/`
- 本 task ファイルの実装結果・確認結果追記

## 除外範囲

- migration image 自体の作成
- DB SQL 移動
- AppRuntimeStack deploy
- AWS deploy
- Pipeline 実走
- migration 専用 DB user / secret / Security Group
- VPC Endpoint
- Step Functions
- Lambda
- custom resource

## 完了条件

- `MigrationStack` が定義されている
- MigrationStack が ECS Service を作っていない
- MigrationStack が migration Task Definition を所有している
- migration task が app private subnet と `app-sg` で起動する構成になっている
- Migration RunTask script が task 停止まで待機する
- migration container exit code 0 の場合だけ成功する
- RunTask failures、異常 stopCode、exitCode 欠落、exitCode 非 0 で build が失敗する
- RunTask に必要な値が CloudFormation Outputs から取得される

## 確認方法

```powershell
cd C:\git\workops\infra\cdk
npm run test
npm run lint
npm run format:check
$env:AWS_PROFILE = "amazon-connect"
$env:AWS_REGION = "ap-northeast-1"
$env:AWS_DEFAULT_REGION = $env:AWS_REGION
$env:WORKOPS_STAGE = "dev"
$env:WORKOPS_IMAGE_TAG = "test-sha"
$env:CDK_DEFAULT_ACCOUNT = aws sts get-caller-identity --profile $env:AWS_PROFILE --region $env:AWS_REGION --query Account --output text
$env:CDK_DEFAULT_REGION = $env:AWS_REGION
npm run cdk:pipeline -- synth --profile $env:AWS_PROFILE
```

## 実装時の記録

- 2026-06-23: `MigrationStack` を追加し、Flyway 専用 migration image を ECS Fargate Task Definition として定義した。
- 2026-06-23: `MigrationStack` は ECS Service を作らず、RunTask に必要な CloudFormation Outputs を出す構成にした。
- 2026-06-23: Pipeline に `DeployDataNetworkMigration` stage と `MigrationRunTask` wave を追加した。
- 2026-06-23: `RunMigration` step は `envFromCfnOutputs` で cluster / task definition / subnet / security group / container name を受け取り、buildspec に物理値を固定しない構成にした。
- 2026-06-23: RunTask 判定処理を buildspec 内 heredoc ではなく `infra/cdk/scripts/run-migration-task.py` に分離した。
- 2026-06-23: AWS credential 環境変数を削除し、profile `amazon-connect`、region `ap-northeast-1`、`WORKOPS_STAGE=dev`、`WORKOPS_IMAGE_TAG=test-sha` を指定し、AWS account は手元の AWS 認証で確認済み。実 account 値は記録しない。
- 2026-06-23: P2-alpha-2 の範囲どおり、AWS deploy / Pipeline 実走 / 実 ECS RunTask / 実 DB migration は実施していない。

### 実装結果

- `infra/cdk/lib/migration-stack.ts`
  - `MigrationStack` を追加した。
  - migration Task Definition は Fargate、`awsvpc`、`ARM64`、`LINUX`、CPU `512`、memory `1024`、family `workops-${stage}-migration` とした。
  - migration container name は `migration`、image tag は `migrationImageTag` で受け取る構成にした。
  - `WORKOPS_FLYWAY_LOCATIONS` は aws-dev 用 locations を固定値として注入した。
  - DB 接続は Web と同じ SSM `/workops/${stage}/db/url` と Secrets Manager `/workops/${stage}/db/master` を使う構成にした。
  - task execution role / task role は `workops-${stage}-migration-execution` / `workops-${stage}-migration-task` とした。
- `infra/cdk/lib/pipeline-stack.ts`
  - Synth step に `WORKOPS_IMAGE_TAG` として source commit id を渡す構成にした。
  - `DeployDataNetworkMigration` stage を追加し、DataStack / EgressStack / EdgeStack / MigrationStack を deploy 対象にした。
  - `RegistryStack` は同 stage で再作成せず、migration repository は repository name import にした。
  - `MigrationRunTask` wave と `RunMigration` CodeBuildStep を追加した。
  - `RunMigration` は `infra/cdk/scripts/run-migration-task.py` を呼ぶだけにし、buildspec に長い判定処理を埋め込まない構成にした。
  - `RunMigration` の IAM は `ecs:RunTask`、`ecs:DescribeTasks`、`iam:PassRole` に限定した。
- `infra/cdk/scripts/run-migration-task.py`
  - `aws ecs run-task`、`aws ecs wait tasks-stopped`、`aws ecs describe-tasks` を実行し、run-task failures、describe failures、異常 stopCode、container 不在、exitCode 欠落、exitCode 非 0 を失敗にする構成にした。
- `infra/cdk/bin/cdk-pipeline.ts`
  - local synth / Pipeline synth の両方で `WORKOPS_IMAGE_TAG` を必須 env として読み、代替 tag は作らない構成にした。
- `infra/cdk/test/workops-app.test.ts`
  - MigrationStack 単体検証と Pipeline の Migration RunTask stage 検証を追加した。

### 確認結果

- `cd C:\git\workops\infra\cdk; npm run build`
  - 成功。
- `cd C:\git\workops\infra\cdk; npm run test`
  - 成功。2 test suites / 23 tests passed。
- `cd C:\git\workops\infra\cdk; npm run lint`
  - 成功。
- `cd C:\git\workops\infra\cdk; npm run format:check`
  - 成功。
- AWS credential 環境変数を削除し、profile `amazon-connect`、region `ap-northeast-1`、`WORKOPS_STAGE=dev`、`WORKOPS_IMAGE_TAG=test-sha`、dummy email で `npm run cdk:pipeline -- synth --quiet --profile amazon-connect`
  - 成功。AWS account は手元の AWS 認証で確認済み。実 account 値は記録しない。

### 残課題

- 実 AWS deploy、Pipeline 実走、実 ECS RunTask、実 DB migration は P2-alpha-2 の範囲外。
- AppRuntimeStack へ web image tag を渡して deploy する処理は P2-alpha-2-06 で扱う。
