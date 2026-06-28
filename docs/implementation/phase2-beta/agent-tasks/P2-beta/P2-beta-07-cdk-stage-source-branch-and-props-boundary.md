# P2-beta-07 CDK stage source branch / props boundary

## Summary

確認taskの前に、CDKの通常入力を `WORKOPS_STAGE` から `WORKOPS_SOURCE_BRANCH` へ移す。
`WORKOPS_SOURCE_BRANCH` は entrypoint 境界の入力名に閉じ込め、CDK app 内ではすぐ `stage` として扱う。
`PipelineStackProps.stage` は維持し、`sourceBranch` props は追加しない。
あわせて、naming / SSM path / tag / stackName / CDK env 横流しだけに使う props を削減する。

## ユーザー要件

- `WORKOPS_STAGE` は人間が通常指定する入力にしない。
- `WORKOPS_SOURCE_BRANCH` は entrypoint で `stage` に畳み込み、Stack props として引き回さない。
- `PipelineStackProps.stage` は削除しない。
- 親計画にない branch 自動取得や stage key 正規化は実装しない。
- Pipeline Synth step env に `WORKOPS_STAGE` は渡さない。
- 実リソース依存 props は維持する。
- props 削減目的の SSM 化は行わない。
- stage だけに限定せず、`stackName` と独自 `env` props も削減対象にする。

## 案

- `infra/cdk/bin/cdk.ts` で `WORKOPS_SOURCE_BRANCH` を必須読み取りする。
- 読み取った値を `stage` に代入し、以降の CDK app 内では `stage` だけを使う。
- `PipelineStack` には従来通り `stage` を渡す。
- `PipelineStack` の Source action `branch` は `props.stage` を使う。
- Synth step env には `WORKOPS_SOURCE_BRANCH: props.stage` だけを渡す。
- DataPause Lambda など runtime が読む `WORKOPS_STAGE` は、Lambda 環境変数としては維持する。
- CDK app root に `workops:stage` context を設定する。
- 各 Stack は context から stage を読み、自身の `stackName` と stage 由来の物理名を生成する。
- Pipeline 内部の deploy stage props から `stage` / 独自 `env` を削除し、実入力だけを残す。

## ADR

- `WORKOPS_SOURCE_BRANCH` は外部入力名であり、CDK 内部モデル名にはしない。
- `stage` は source branch 由来の実行 stage として扱う。
- Pipeline Source action branch filter は `stage` をそのまま使う。
- branch 名の変換、fallback、自動取得は採用しない。
- `WORKOPS_STAGE` は通常の CDK 実行入力として廃止するが、既存 runtime env 名として必要な箇所だけ残す。
- CDK内部の実リソース依存はprops / construct参照を維持し、props削減のためにSSM化しない。
- `stackName` は呼び出し側から渡さず、Stack 自身が `workops-${stage}-...` 形式で生成する。
- `env` は `StackProps` / `StageProps` の標準入力として扱い、custom props interface へ独自再定義しない。

## Implementation

- `infra/cdk/bin/cdk.ts` は `WORKOPS_STAGE` 必須読み取りをやめる。
  - `WORKOPS_SOURCE_BRANCH` を読み、即 `stage` として扱う。
  - `app.node.setContext('workops:stage', stage)` を設定する。
  - stackNameとtagの `Environment` には `stage` を使う。
- `infra/cdk/lib/pipeline-stack.ts` は `PipelineStackProps.stage` を維持する。
  - `CodeStarConnectionsSourceAction.branch` には `props.stage` を渡す。
  - Synth step envには `WORKOPS_SOURCE_BRANCH` だけを渡す。
- `infra/cdk/lib/environment.ts` に stage context helper と stackName helper を追加する。
- `PipelineStackProps` 以外の Stack props interface から `stage` を削除する。
- 呼び出し側から `stackName: workops-${stage}-...` を渡さない。
- Pipeline 内部 DeployStage props から `stage` / 独自 `env` を削除する。
- 実リソース依存 props と entrypoint 由来の `githubRepository` / `notificationEmail` / `webImageTag` は維持する。
- README / task文書の通常手順を更新する。
  - 通常手順では `WORKOPS_STAGE=dev` を設定しない。
  - 通常手順では `WORKOPS_SOURCE_BRANCH=dev` を設定する。
  - Pipeline対象branchは `PipelineStack` Source actionのbranch filterで固定されることを明記する。

## Verification

- `npm run build`
- `npm test`
- 必要時のみ `WORKOPS_SOURCE_BRANCH=dev` を明示して `npx cdk synth --quiet --profile $env:AWS_PROFILE`
- Templateで次を確認する。
  - Pipeline Source action の branch が `props.stage` 由来になっている。
  - Synth step envに `WORKOPS_SOURCE_BRANCH` がある。
  - Pipeline Synth step envに `WORKOPS_STAGE` がない。
  - DataPause Lambda の runtime env に `WORKOPS_STAGE` が残る。
- `rg` で次を確認する。
  - `branch: 'main'` が `PipelineStack` に残っていない。
  - `WORKOPS_STAGE=dev` が通常手順として残っていない。
  - `PipelineStackProps` 以外の Stack props interface に `stage: string` が残っていない。
  - `bin/cdk.ts` と `pipeline-stack.ts` の Stack 呼び出し側に `stackName: \`workops-` 横流しが残っていない。
  - custom props interface に独自 `env: Environment` が残っていない。
- 実AWS deployはこのtaskでは必須にしない。`P2-beta-08` と `P2-beta-09` で確認する。

## Assumptions

- `WORKOPS_SOURCE_BRANCH=dev` のように、branch 名は既存の stage 命名としてそのまま使える値にする。
- 既存データ移行と後方互換性は考慮しない。
