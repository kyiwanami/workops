# P2-beta-07 CDK stage source branch / props boundary

## Summary

確認taskの前に、CDKのstage決定方法とStack間props境界を整理する。
stageはGit branchから決め、CodePipelineのSource actionは対象branchだけをfilterする。
あわせてCDK全体で、naming、SSM path、tagのためだけに渡しているpropsを削減できるか棚卸しする。

## ユーザー要件

- `WORKOPS_STAGE` は人間が通常指定する入力にしない。
- stageはGit branchから決める。
- `infra/cdk/lib/pipeline-stack.ts` のSource actionで対象branchをfilterする。
- CDKコード全体でprops渡しを減らせるか検討する。
- props削減ではSSM化までは行わない。
- contextやhelperで済む値、そもそも渡さなくてよい値を優先して削る。
- VPC、subnet、security group、listener、target group、repository、log group、Cognito IDなど、実リソース依存を表すprops / construct参照は維持する。

## 案

- Git branchのraw名はCodePipeline Source actionのfilterに使う。
- AWS resource name、SSM path、stackName、tagに使うstage keyはGit branchから決定的に生成する。
- stage keyの文字種を理由にPipeline対象外へ落とす制約は、このtaskでは作らない。
- CDK app rootで `workops:stage` だけをcontextに設定する。
- Stack内でnaming / SSM path / tagだけに使うstageは、propsではなくcontext helperから読む。
- `PipelineStack` の `CodeStarConnectionsSourceAction.branch` は `'main'` 固定をやめ、source branchを指定する。
- Pipeline内のSynth stepには、初回deploy時に確定したstage keyとsource branchを環境変数として内部伝播してよい。

## ADR

- CodePipelineの対象branch制御は `CodeStarConnectionsSourceAction.branch` で行う。
- source branchとstage keyを分ける。source branchはGitのbranch名、stage keyはAWS resource nameへ使う識別子とする。
- stage keyはsource branchから生成し、手動対応表や任意指定にはしない。
- source branchの `/` などAWS resource nameに使えない文字は、stage key生成helperで `-` に置換する。
- stage key生成helperは小文字化、連続hyphenの圧縮、先頭末尾hyphenの除去を行う。
- stage keyが空になる場合は `branch` を使う。
- stage keyが数字で始まる場合は `b-` を付ける。
- stage key生成helperはCDK app内だけの責務とし、SSM Parameterにはしない。
- `WORKOPS_STAGE` はPipeline内のself-mutation / synthへの伝播値としては残してよいが、人間が通常入力するstage正本にはしない。
- CDK内部の実リソース依存はprops / construct参照を維持し、props削減のためにSSM化しない。

## Implementation

- `infra/cdk/lib/environment.ts` にGit branch / stage key用helperを追加する。
  - source branch取得は `git branch --show-current` を使う。
  - `WORKOPS_SOURCE_BRANCH` がある場合は、その値をsource branchとして使う。
  - detached HEADなどでsource branchを取得できない場合だけ、`WORKOPS_SOURCE_BRANCH` 未設定エラーでfail fastする。
  - stage keyはsource branchから生成する。
- `infra/cdk/bin/cdk.ts` は `WORKOPS_STAGE` 必須読み取りをやめる。
  - source branchとstage keyを取得する。
  - `app.node.setContext('workops:stage', stage)` を設定する。
  - stackNameとtagの `Environment` にはstage keyを使う。
- `infra/cdk/lib/pipeline-stack.ts` はsource branchをpropsで受け取る。
  - `CodeStarConnectionsSourceAction.branch` にはsource branchを渡す。
  - Synth step envには `WORKOPS_SOURCE_BRANCH` と `WORKOPS_STAGE` を渡す。
- Stack propsの棚卸しを行う。
  - `stage` propsは原則削除する。
  - Stack内部で必要なstage keyはcontext helperから取得する。
  - `env` propsは `StackProps` 側に含まれるため、独自props interfaceから削除できるものは削除する。
  - resource dependencyを表すpropsは削除しない。
- README / task文書の通常手順を更新する。
  - 通常手順では `WORKOPS_STAGE=dev` を設定しない。
  - branchを切り替えることで対象stageが決まることを明記する。
  - Pipeline対象branchは `PipelineStack` Source actionのbranch filterで固定されることを明記する。

## Verification

- `npm run build`
- `npm test -- --runInBand`
- `npx cdk synth`
- Templateで次を確認する。
  - Pipeline Source action の branch がsource branchになっている。
  - stackName / SSM path / resource name がstage keyで生成されている。
  - Synth step envに `WORKOPS_SOURCE_BRANCH` と `WORKOPS_STAGE` がある。
- `rg` で次を確認する。
  - `branch: 'main'` が `PipelineStack` に残っていない。
  - `WORKOPS_STAGE=dev` が通常手順として残っていない。
  - `stage: string` propsが、naming / SSM path / tagだけのStack props interfaceに残っていない。
- 実AWS deployはこのtaskでは必須にしない。`P2-beta-08` と `P2-beta-09` で確認する。

## Assumptions

- source branch名はCodePipelineのfilter値としてrawのまま扱う。
- stage keyはsource branchから機械的に生成されるため、手動対応表は使わない。
- 既存データ移行と後方互換性は考慮しない。
