# P2-beta-01 CDK entrypoint / DependencyStack

## Summary

Phase 2-beta の先頭 task として、CDK entrypoint を `infra/cdk/bin/cdk.ts` 単一へ戻し、後続 task が依存する Top-level 基盤 `DependencyStack` を追加する。
この task では、CodeArtifact、ops notification topic、非 secret SSM parameters、`ConfigStack` / `SecretStack` 削除、spring profile SSM path 変更までを扱う。

## ユーザー要件

- 通常手順は `cdk deploy '*'` / `cdk diff '*'` とし、個別 Stack 名指定は通常手順から外す。
- 初回 bootstrap だけ `DependencyStack` と `PipelineStack` の先行 deploy を例外として認める。
- `DependencyStack` は CodeArtifact と ops notification topic と非 secret SSM parameters を所有する。
- `ConfigStack` / `SecretStack` は 2-beta で削除する。「撤去分類」と混同しない。
- 旧 `/workops/${stage}/spring/profile` は維持せず、`/workops/${stage}/dependencies/runtime/spring-profile` に移す。

## 案

- `infra/cdk/bin/cdk.ts` を単一 entrypoint とし、旧複数 entrypoint を削除する。
- `DependencyStack` を追加し、次を所有させる。
  - CodeArtifact domain
  - npm repository
  - Maven repository
  - ops notification SNS topic `workops-${stage}-ops-notifications`
  - EmailSubscription
  - 非 secret SSM parameters
- `ConfigStack` と `SecretStack` を CDK app から外し、関連ファイルを削除する。
- `AppRuntimeStack` の spring profile 参照先を新 SSM path に変更する。
- 初回 bootstrap 例外手順を README / 実行手順へ反映する。

## ADR

- `DependencyStack` は Top-level / Pipeline 基盤として、Pipeline Synth 前に必要な依存基盤を所有する。
- secret は `DependencyStack` に置かない。RDS master secret は `DataStack` 所有のままにする。
- DB 接続系 SSM parameters は RDS lifecycle に属するため `DataStack` 所有のままにする。
- CodeArtifact endpoint、authorization token、AWS account ID / domain-owner は SSM に保存しない。
- CodeArtifact package cleanup の独自実装はしない。domain / repository は `RemovalPolicy.DESTROY` に寄せる。

## Implementation

- `DependencyStack` の SSM parameters は次に固定する。

```text
/workops/${stage}/dependencies/runtime/spring-profile
/workops/${stage}/dependencies/codeartifact/domain-name
/workops/${stage}/dependencies/codeartifact/npm-repository-name
/workops/${stage}/dependencies/codeartifact/maven-repository-name
/workops/${stage}/dependencies/notifications/ops-topic-arn
```

- SSM parameters は `StringParameter`、明示 `parameterName`、`RemovalPolicy.DESTROY` とする。
- CodeArtifact は同一 domain 内で npm / Maven repository を分ける。
- npm repository は npmjs、Maven repository は Maven Central への external connection / upstream を持つ。
- CloudFormation管理外の手作業作成はしない。L2 constructで外部接続を表現できない場合はL1で表現する。
- `PipelineStack` は `DependencyStack` が公開した SSM parameters を参照する前提へ変更する。

## Verification

- `npm run build`
- `npm run test`
- `npm run lint`
- `npm run format:check`
- `WORKOPS_STAGE=dev` を明示して `cdk synth` を実行する。
- Synth templateで次を確認する。
  - `DependencyStack` が存在する。
  - CodeArtifact domain / npm repository / Maven repository が存在する。
  - ops notification topic と EmailSubscription が存在する。
  - 非 secret SSM parameters が存在する。
  - `ConfigStack` / `SecretStack` が CDK app から外れている。
  - `AppRuntimeStack` が `/workops/${stage}/dependencies/runtime/spring-profile` を参照している。
- 実AWS deployはこの task では必須にしない。`P2-beta-08` と `P2-beta-09` で確認する。

## Assumptions

- 実 account ID、SSO role ARN、credential値、CodeArtifact endpoint実値は文書に記録しない。
- `npm install` / `npm ci` が必要な場合は、依存操作の明示許可を得てから実行する。
