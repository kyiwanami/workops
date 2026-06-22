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

Migration RunTask stage は CodeBuild buildspec 内の bash + AWS CLI で次を実行する。

- `aws ecs run-task`
- `run-task` の `failures` が空であることを確認
- taskArn を取得
- `aws ecs wait tasks-stopped`
- `aws ecs describe-tasks`
- `stopCode`、`stoppedReason`、`containers[].exitCode`、`containers[].reason` を確認
- migration container の `exitCode` が 0 の場合だけ成功

RunTask に必要な cluster、task definition、subnet、security group、container name は buildspec に固定値として書かない。
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
- Migration RunTask buildspec
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
- Migration RunTask buildspec が task 停止まで待機する
- migration container exit code 0 の場合だけ成功する
- RunTask failures、異常 stopCode、exitCode 欠落、exitCode 非 0 で build が失敗する
- RunTask に必要な値が CloudFormation Outputs から取得される

## 確認方法

```powershell
cd C:\git\workops\infra\cdk
npm run test
npm run lint
npm run format:check
$env:WORKOPS_STAGE = "dev"
$env:CDK_DEFAULT_ACCOUNT = "000000000000"
$env:CDK_DEFAULT_REGION = "ap-northeast-1"
npm run cdk:pipeline -- synth
```

## 実装時の記録

### 実装結果

- 未実装。

### 確認結果

- 未確認。

### 残課題

- 未整理。
