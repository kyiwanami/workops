# P2-alpha-2-06 AppRuntime Blue/Green

## 目的

AppRuntimeStack を ARM_64 + ECS ネイティブ Blue/Green Deployment 構成へ変更し、Target Group と Listener rule を AppRuntimeStack 側に集約する。

## ユーザー要求

- P2-alpha-2 の task 分割は、`phases.md` の責務境界を最上位単位にする
- 既にコード、Notion、`phases.md` で分かることは質問しない
- 必要な場合は公式ドキュメントやベストプラクティスを確認する
- task ファイル末尾に `実装時の記録`、`実装結果`、`確認結果`、`残課題` を置く

## 前提

- P2-alpha-2-01 が完了している
- P2-alpha-2-02 が完了している
- P2-alpha-2-03 が完了している
- P2-alpha-2-04 が完了している
- P2-alpha-2-05 が完了している
- 現行 AppRuntimeStack は X86_64 である
- 現行 EdgeStack は Target Group と Listener default target を持っている
- Phase 2α では CodeDeploy を使わない
- Phase 2α では all-at-once + bake time 3分 + Alarm 2本連動とする

## 案

EdgeStack は次を所有する。

- internal ALB
- Listener 本体
- CloudFront distribution
- Cognito client URL updater
- CloudFront origin-facing managed prefix list ID の deploy 時解決 custom resource

AppRuntimeStack は次を所有する。

- Target Group
- Listener rule
- ECS Fargate Service
- ECS Task Definition
- ECS ネイティブ Blue/Green Deployment 設定

EdgeStack から AppRuntimeStack へ Listener を props 参照で渡す。
cross-stack 参照は AppRuntimeStack → EdgeStack の一方向にする。

AppRuntimeStack の Task Definition は ARM_64 にする。
Web image は `webRepository` と commit SHA tag を使う。

ECS Service の deployment controller は ECS にする。
CodeDeploy application、deployment group、appspec.yml は作らない。

Blue/Green は all-at-once、bake time 3分、Alarm 2本連動にする。
Alarm は P2-alpha-3 で実走確認するが、P2-alpha-2 では CDK code と synth を完了条件にする。

## 参考

- Amazon ECS の blue/green deployment では、traffic shift 後に blue / green 両 revision が同時稼働する bake time がある。
- ECS deployment failure detection では CloudWatch alarm を rollback trigger として扱える。
- 公式ドキュメント確認日: 2026-06-23
  - Amazon ECS blue/green deployments
  - Amazon ECS deployment alarms
  - AWS::ECS::Service DeploymentAlarms

## 判断メモ

- Target Group と Listener rule を AppRuntimeStack へ集約する。runtime service と target registration のライフサイクルを揃えるため。
- Listener 本体は EdgeStack に残す。CloudFront / ALB / Listener の入口責務を EdgeStack に残すため。
- CodeDeploy は採用しない。Phase 2α は ECS ネイティブ Blue/Green を採用するため。
- desiredCount=0 で AppRuntimeStack を残す運用は採用しない。主要課金 4 stack destroy の方針に合わせるため。
- CloudFront origin-facing managed prefix list ID は `PrefixList.fromLookup` / `cdk.context.json` に固定しない。CDK context lookup は AWS CDK の一般的な運用として有効だが、context key に account ID / region / lookup 条件が入り、lookup 結果も環境依存値になるため、本 project の「実 account ID と環境依存 lookup 結果を git 管理しない」方針と合わない。
- CloudFront origin-facing managed prefix list は AWS managed shared value であり、project-owned resource の props 参照では生成できないため、`EdgeStack` deploy 時の custom resource lookup を採用する。
- custom resource は EC2 `DescribeManagedPrefixLists` を AWS owner と prefix list name で絞り込む read-only lookup に限定する。VPC / subnet / security group など通常の project-owned resource 参照へ拡大しない。
- `AwsCustomResource` は deploy 時の最新 SDK 取得を避けるため `installLatestAwsSdk: false` を明示する。

## 対応範囲

- `infra/cdk/lib/edge-stack.ts`
- `infra/cdk/lib/app-runtime-stack.ts`
- `infra/cdk/bin/cdk-runtime.ts`
- PipelineStack の Deploy AppRuntime stage
- Alarm 定義
- ARM_64 Task Definition
- Target Group / Listener rule 再配置
- `infra/cdk/test/`
- 本 task ファイルの実装結果・確認結果追記

## 除外範囲

- CodeDeploy application / deployment group / appspec.yml
- AWS deploy
- Pipeline 実走
- CloudFront domain 経由の HTTP 200 確認
- Alarm 実発火確認
- 数値 SLO 合格判定
- ALB HTTPS listener
- 独自ドメイン / ACM / Route53

## 完了条件

- AppRuntimeStack の ECS Task が ARM_64 になっている
- ECS Service が ECS ネイティブ Blue/Green Deployment 構成になっている
- CodeDeploy 関連リソースが作られていない
- Target Group と Listener rule が AppRuntimeStack 側にある
- EdgeStack は Listener 本体を所有している
- AppRuntimeStack が Listener を props 参照で受け取っている
- bake time 3分と Alarm 2本連動が CDK code に反映されている
- 新構成で synth できる

## 確認方法

```powershell
cd C:\git\workops\infra\cdk
npm run build
npm run test
npm run lint
npm run format:check
Remove-Item Env:AWS_ACCESS_KEY_ID -ErrorAction SilentlyContinue
Remove-Item Env:AWS_SECRET_ACCESS_KEY -ErrorAction SilentlyContinue
Remove-Item Env:AWS_SESSION_TOKEN -ErrorAction SilentlyContinue
$env:AWS_PROFILE = "amazon-connect"
$env:AWS_REGION = "ap-northeast-1"
$env:AWS_DEFAULT_REGION = $env:AWS_REGION
$account = aws sts get-caller-identity --profile $env:AWS_PROFILE --region $env:AWS_REGION --query Account --output text
$env:CDK_DEFAULT_ACCOUNT = $account
$env:CDK_DEFAULT_REGION = $env:AWS_REGION
$env:WORKOPS_STAGE = "dev"
$env:WORKOPS_IMAGE_TAG = "test-sha"
npm run cdk:runtime -- synth --quiet --profile $env:AWS_PROFILE
$env:GITHUB_REPOSITORY = "owner/repo"
$env:WORKOPS_PIPELINE_NOTIFICATION_EMAIL = "pipeline@example.com"
npm run cdk:pipeline -- synth --quiet --profile $env:AWS_PROFILE
```

## 実装時の記録

- 2026-06-23: ECS native blue/green と deployment alarms の公式ドキュメントを確認した。
- 2026-06-23: Latency alarm は数値 SLO が P2-alpha-2 で未定義のため採用せず、ALB target 5xx と unhealthy host を deployment rollback 用 alarm とした。
- 2026-06-23: P2-alpha-2 では AWS deploy / Pipeline 実走 / ECR push / ECS RunTask は行わず、CDK synth までを確認対象にした。

### 実装結果

- `EdgeStack` は internal ALB、listener、CloudFront、Cognito client URL updater の所有に絞り、listener default action は fixed 404 にした。
- `EdgeStack` の CloudFront origin-facing managed prefix list ID 解決は `PrefixList.fromLookup` から `AwsCustomResource` に変更した。`cdk.context.json` に account ID と lookup 結果を固定しないため。
- `AwsCustomResource` は EC2 `describeManagedPrefixLists` の read-only lookup に限定し、`installLatestAwsSdk: false` を明示した。
- custom resource の結果は ALB security group ingress の `SourcePrefixListId` にだけ使う。
- `AppRuntimeStack` は blue / green target group、listener rule、ARM64 Fargate task definition、ECS native blue/green service、deployment alarms を所有する構成にした。
- Web ECS service は `DeploymentStrategy.BLUE_GREEN`、bake time 3分、rollback enabled deployment alarms を使う。
- Web ECS service は task 起動失敗を早く検出するため、ECS deployment circuit breaker も rollback enabled で維持する。
- Web image tag は `WORKOPS_IMAGE_TAG` から渡し、`WORKOPS_WEB_IMAGE_TAG` は復活させない。
- `PipelineStack` に `DeployAppRuntime` stage を追加し、`MigrationRunTask` 後に AppRuntimeStack を deploy する配線にした。
- CodeDeploy application、deployment group、appspec.yml、Lambda / Step Functions / custom resource による deploy hook は追加していない。CloudFront origin-facing managed prefix list ID 解決 custom resource は deploy hook ではなく、ALB ingress の source prefix list ID を解決する read-only lookup である。

### 確認結果

- `npm run build` 成功。
- `npm run test -- --runInBand` 成功。
- `npm run lint` 成功。
- `npm run format:check` 成功。
- AWS profile `amazon-connect` / region `ap-northeast-1` で `aws sts get-caller-identity` により認証先を確認済み。
- `WORKOPS_STAGE=dev`、`WORKOPS_IMAGE_TAG=test-sha` で `npm run cdk:runtime -- synth --quiet --profile amazon-connect` 成功。
- `WORKOPS_STAGE=dev`、`WORKOPS_IMAGE_TAG=test-sha`、`GITHUB_REPOSITORY=kyiwanami/workops`、`WORKOPS_PIPELINE_NOTIFICATION_EMAIL` 設定で `npm run cdk:pipeline -- synth --quiet --profile amazon-connect` 成功。
- AWS deploy、Pipeline 実走、ECR push、ECS RunTask は実施していない。

### 残課題

- AWS deploy、Pipeline 実走、ECR push、ECS RunTask、CloudFront domain 経由の HTTP 200、Alarm 実発火確認は P2-alpha-2 の除外範囲として未実施。
- Blue/Green の実 traffic shift と rollback 挙動は P2-alpha-3 以降の AWS dev 実走確認で扱う。
