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

## 判断メモ

- Target Group と Listener rule を AppRuntimeStack へ集約する。runtime service と target registration のライフサイクルを揃えるため。
- Listener 本体は EdgeStack に残す。CloudFront / ALB / Listener の入口責務を EdgeStack に残すため。
- CodeDeploy は採用しない。Phase 2α は ECS ネイティブ Blue/Green を採用するため。
- desiredCount=0 で AppRuntimeStack を残す運用は採用しない。主要課金 4 stack destroy の方針に合わせるため。

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
npm run test
npm run lint
npm run format:check
$env:WORKOPS_STAGE = "dev"
$env:CDK_DEFAULT_ACCOUNT = "000000000000"
$env:CDK_DEFAULT_REGION = "ap-northeast-1"
$env:WORKOPS_WEB_IMAGE_TAG = "test-sha"
npm run cdk:runtime -- synth
npm run cdk:pipeline -- synth
```

## 実装時の記録

### 実装結果

- 未実装。

### 確認結果

- 未確認。

### 残課題

- 未整理。
