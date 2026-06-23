# P2-alpha-3-03 Completion record and cleanup

## 目的

P2-alpha-3 の確認結果、ベースライン、失敗記録、ADR 化候補、主要課金 4 stack destroy を集約し、Phase 2α の完了判定を記録する。

この task は、P2-alpha-3 全体の完了記録と cleanup を兼ねる。

## ユーザー要求

- Cleanup は独立 task にしない
- ベースラインは stage 単位の開始・終了時刻・所要時間だけ記録する
- ベースライン値は合否判定に使わない
- 失敗時は P2-alpha-3 の 03 に集約する
- 失敗しても自動的に P2-alpha-2 へ戻さない
- 失敗事象は、原因、対策、ADR 化候補、ユーザーとの壁打ち結果として記録する
- ADR 本文は作らず、ADR 化候補として残す
- 実 account ID、role ARN、credential、public IP、実 Cognito `sub`、DB 実データ値、CloudFront domain は git 管理文書に残さない

## 前提

- P2-alpha-3-01 が完了している
- P2-alpha-3-02 が完了している
- Pipeline が 1 周完走している
- 主要課金 4 stack の destroy を実行してよい状態である

## 案

P2-alpha-3 の完了記録は、本ファイルに集約する。

確認結果は、Pipeline、Migration、App、Alarm / Log、Cleanup に分けて記録する。
ベースラインは Pipeline stage 単位で記録し、合否判定には使わない。

失敗が発生した場合は、本ファイルに集約し、原因、対策、ADR 化候補、ユーザーとの壁打ち結果を残す。
ADR 本文を作る必要が出た場合は、Notion 側の更新要否をユーザーに確認する。

Cleanup では `EgressStack`、`DataStack`、`EdgeStack`、`AppRuntimeStack` のみを destroy 対象にする。
維持対象 Stack とリソースは destroy しない。

## 判断メモ

- Cleanup は独立 task にしない。P2-alpha-3 を3本に収め、完了記録と課金停止確認を一体で扱うため。
- 失敗記録は各 task に分散させない。最終的な判断と対策を一箇所で追えるようにするため。
- ADR 本文は作らない。ADR は Notion 側の責務であり、P2-alpha-3 では ADR 化候補として扱うため。

## 対応範囲

- P2-alpha-3 checklist 集約
- ベースライン記録
- 失敗事象の集約
- ADR 化候補の記録
- 主要課金 4 stack destroy 手順
- destroy 後の Stack 状態確認
- 維持対象 Stack / resource が残っていることの確認
- Phase 2α 完了判定

## 除外範囲

- CDK code 変更
- 追加品質ゲート tuning
- 数値 SLO 合格判定
- production-grade SLA 設定
- preview / staging / prod 環境
- cross-account
- ADR 本文作成
- Notion 更新

## 完了条件

- P2-alpha-3 の動作確認 checklist が全項目達成されている
- ベースライン記録が保存されている
- 失敗事象がある場合、原因、対策、ADR 化候補、ユーザーとの壁打ち結果が記録されている
- `EgressStack` が destroy 済みである
- `DataStack` が destroy 済みである
- `EdgeStack` が destroy 済みである
- `AppRuntimeStack` が destroy 済みである
- `PipelineStack`、`MigrationStack`、`FoundationStack`、`ConfigStack`、`RegistryStack`、`LogsStack`、`SecretStack`、Artifact bucket、ECR repository、CodeConnection が維持されている
- 実 account ID、role ARN、credential、public IP、実 Cognito `sub`、DB 実データ値、CloudFront domain を記録していない

## Checklist 集約

### Pipeline

| 項目 | 状態 | 根拠 |
| --- | --- | --- |
| Source が CodeConnections で `main` を取得して起動した | 未確認 | P2-alpha-3-02 |
| Synth stage で `cdk synth` が pass した | 未確認 | P2-alpha-3-02 |
| Build & Test stage で全品質ゲートが pass した | 未確認 | P2-alpha-3-02 |
| ManualApproval 通知メールが届いた | 未確認 | P2-alpha-3-01 |
| Deploy Registry stage で ECR repository 4 本が存在した | 未確認 | P2-alpha-3-02 |
| Web image と migration image が commit SHA tag で push された | 未確認 | P2-alpha-3-02 |
| Deploy Data / Network / Migration stage が完走した | 未確認 | P2-alpha-3-02 |
| Migration RunTask stage が完走した | 未確認 | P2-alpha-3-02 |
| Deploy AppRuntime stage が完走した | 未確認 | P2-alpha-3-02 |
| Pipeline 全体がエラーなく 1 周完走した | 未確認 | P2-alpha-3-02 |

### Migration

| 項目 | 状態 | 根拠 |
| --- | --- | --- |
| ECS RunTask が exit code 0 で終了した | 未確認 | P2-alpha-3-02 |
| CloudWatch Logs 上で Flyway schema 適用完了を確認した | 未確認 | P2-alpha-3-02 |
| Flyway 適用後、アプリケーションから DB 接続が成功した | 未確認 | P2-alpha-3-02 |

### App

| 項目 | 状態 | 根拠 |
| --- | --- | --- |
| Blue/Green 切替が完了した | 未確認 | P2-alpha-3-02 |
| ALB Target Group が healthy を示した | 未確認 | P2-alpha-3-02 |
| CloudFront default domain 経由で HTTP 200 を取得した | 未確認 | P2-alpha-3-02 |
| ブラウザでトップページ到達を確認した | 未確認 | P2-alpha-3-02 |
| ECS Task が `cpuArchitecture: ARM_64` で起動した | 未確認 | P2-alpha-3-02 |

### Alarm / Log

| 項目 | 状態 | 根拠 |
| --- | --- | --- |
| bake 後、連動 Alarm が `ALARM` ではなかった | 未確認 | P2-alpha-3-02 |
| CloudWatch Logs に致命的 ERROR がなかった | 未確認 | P2-alpha-3-02 |

### Cleanup

| 項目 | 状態 | 根拠 |
| --- | --- | --- |
| `EgressStack` が destroy 済みである | 未確認 | 本 task |
| `DataStack` が destroy 済みである | 未確認 | 本 task |
| `EdgeStack` が destroy 済みである | 未確認 | 本 task |
| `AppRuntimeStack` が destroy 済みである | 未確認 | 本 task |
| 維持対象 Stack / resource が残っている | 未確認 | 本 task |

## ベースライン記録

以下は合否判定に使わない。

| 項目 | 開始 | 終了 | 所要時間 | 備考 |
| --- | --- | --- | --- | --- |
| Pipeline 全体 | 未記録 | 未記録 | 未記録 | 合否判定に使わない |
| Build & Test stage | 未記録 | 未記録 | 未記録 | 合否判定に使わない |
| Build Images stage | 未記録 | 未記録 | 未記録 | 並列 stage 全体 |
| Migration RunTask | 未記録 | 未記録 | 未記録 | 合否判定に使わない |
| Blue/Green 切替 | 未記録 | 未記録 | 未記録 | 合否判定に使わない |

## 失敗記録

失敗が発生した場合は、次の表に集約する。

| 発生箇所 | 事象 | 原因 | 対策 | ADR 化候補 | 壁打ち結果 |
| --- | --- | --- | --- | --- | --- |
| なし | なし | なし | なし | なし | なし |

## Cleanup 確認方法

AWS 操作前:

```powershell
Remove-Item Env:AWS_ACCESS_KEY_ID -ErrorAction SilentlyContinue
Remove-Item Env:AWS_SECRET_ACCESS_KEY -ErrorAction SilentlyContinue
Remove-Item Env:AWS_SESSION_TOKEN -ErrorAction SilentlyContinue

$env:WORKOPS_STAGE = "<stage>"
$env:AWS_PROFILE = "<aws-profile>"
$env:AWS_SDK_LOAD_CONFIG = "1"
$env:AWS_REGION = "<aws-region>"
$env:AWS_DEFAULT_REGION = $env:AWS_REGION
$env:CDK_DEFAULT_ACCOUNT = (aws sts get-caller-identity --profile $env:AWS_PROFILE --region $env:AWS_REGION --query Account --output text)
$env:CDK_DEFAULT_REGION = $env:AWS_REGION

aws sts get-caller-identity --profile $env:AWS_PROFILE --region $env:AWS_REGION
```

主要課金 4 stack destroy:

```powershell
cd C:\git\workops\infra\cdk
npm run cdk:runtime -- destroy AppRuntimeStack --profile $env:AWS_PROFILE
npm run cdk:runtime -- destroy EdgeStack --profile $env:AWS_PROFILE
npm run cdk:runtime -- destroy DataStack --profile $env:AWS_PROFILE
npm run cdk:runtime -- destroy EgressStack --profile $env:AWS_PROFILE
```

destroy 後の状態確認:

```powershell
aws cloudformation describe-stacks `
  --profile $env:AWS_PROFILE `
  --region $env:AWS_REGION `
  --query "Stacks[?contains(StackName, 'workops')].[StackName,StackStatus]" `
  --output table
```

維持対象確認:

```powershell
aws cloudformation describe-stacks `
  --profile $env:AWS_PROFILE `
  --region $env:AWS_REGION `
  --query "Stacks[?contains(StackName, 'workops') && (contains(StackName, 'pipeline') || contains(StackName, 'migration') || contains(StackName, 'foundation') || contains(StackName, 'config') || contains(StackName, 'registry') || contains(StackName, 'logs') || contains(StackName, 'secret'))].[StackName,StackStatus]" `
  --output table
```

## 実装時の記録

### 実施結果

- 未実施。

### 確認結果

- 未実施。

### 残課題

- 未実施。
