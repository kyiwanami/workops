# P2-beta-08 Permanent baseline deploy check

## Summary

`P2-beta-01` から `P2-beta-07` までで整えた Top-level / 常設基盤の一部を、Pipeline再構成前に実AWSで中間確認する。
この task は実AWS操作を含むため、コーディングエージェントは手順と事前確認を作り、実行はユーザーの明示依頼時だけ行う。

## ユーザー要件

- 実AWS操作はAWS profile / regionを明示する。
- AWS credential環境変数を削除してprofileを使う。
- `aws sts get-caller-identity --region $env:AWS_REGION` で認証先を確認する。
- 実 account ID、SSO role ARN、credential値は記録しない。
- 中間確認では課金対象/撤去分類/休止対象を除外する。

## 案

中間確認対象は次に限定する。

```text
DependencyStack
FoundationStack
LogsStack
IdentityStack
RegistryStack
DataPauseStack
WebAclStack
```

`WebDeliveryStack` は常設分類だが、この中間確認から除外する。
理由は `WebIngressStack` が作る Web ingress origin contract を deploy時に解決するため。

## ADR

- このtaskだけStack名指定deployを中間確認例外として使う。
- 通常運用は `cdk deploy --all` / `cdk diff --all` のまま。
- `--exclusively` は使わない。
- CodeArtifactの実 npm/Maven依存取得はこのtaskで確認せず、最終Pipeline確認で確認する。
- DataPauseの実 `RDS-EVENT-0154` 再現はこのtaskで確認しない。
- WAF log は作成する。stage 別の WAF log retention / destination 方針は後続 TODO とし、このtaskでは扱わない。

## Implementation

- 実行前に次を文書化する。
  - AWS profile
  - AWS region
  - credential環境変数削除手順
  - `sts get-caller-identity` 確認手順
  - `cdk diff` 確認手順
- 実行コマンド例は対象Stack名指定とする。

```powershell
npm run cdk -- deploy DependencyStack FoundationStack LogsStack IdentityStack RegistryStack DataPauseStack WebAclStack
```

## Verification

- `DependencyStack`
  - CodeArtifact domain / repositories
  - ops notification topic / EmailSubscription
  - 非 secret SSM parameters
- `FoundationStack`
  - VPC / SG / ECS cluster / S3 Gateway Endpoint
- `LogsStack`
  - 既存LogGroup
  - DataPause用LogGroup
  - 認証・認可イベント metric filter
- `IdentityStack`
  - Cognito User Pool / App Clients / Domain / Managed Login branding
- `RegistryStack`
  - Web image repository
  - Web cache repository
  - migration image repositoryがないこと
- `DataPauseStack`
  - EventBridge Rule 2個
  - Lambda 2個
  - Lambda Errors alarm
  - ops notification topicへのAlarm action
- `WebAclStack`
  - CloudFront scope の WAF Web ACL
  - AWS Managed Rules Common Rule Set の Count mode
  - WAF logging が作成されること
  - `us-east-1` にdeployすること

## Assumptions

- このtaskは中間確認であり、Pipeline完走確認ではない。
- 実行しない場合は、実行できなかった理由とユーザーが実行する手順を記録する。
- `us-east-1` のCDK bootstrapが未実施の場合は、実deploy前の前提条件として記録する。
