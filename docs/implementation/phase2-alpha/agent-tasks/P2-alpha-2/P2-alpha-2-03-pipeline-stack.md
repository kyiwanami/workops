# P2-alpha-2-03 PipelineStack

## 目的

`PipelineStack` と `bin/cdk-pipeline.ts` を追加し、CodePipeline V2 + CodeBuild + CDK Pipelines による Phase 2α の単一 Pipeline 骨格を作る。

## ユーザー要求

- P2-alpha-2 の task 分割は、`phases.md` の責務境界を最上位単位にする
- 既にコード、Notion、`phases.md` で分かることは質問しない
- 必要な場合は公式ドキュメントやベストプラクティスを確認する
- task ファイル末尾に `実装時の記録`、`実装結果`、`確認結果`、`残課題` を置く

## 前提

- P2-alpha-2-01 が完了している
- P2-alpha-2-02 が完了している
- Phase 2α の Pipeline stage 順序は `phases.md` で固定済みである
- Build & Test は Pipeline stage の一部として PipelineStack 側で扱う
- CDK Pipelines は `selfMutation: true` とする
- CodeConnections は GitHub `main` branch を source とする
- file path filter は使わない

## 案

`PipelineStack` を追加し、次を所有させる。

- CodePipeline V2
- CDK Pipelines
- CodeBuild project
- Artifact bucket
- CodeConnections source
- ManualApproval
- CodeStar Notifications
- SNS Topic

`bin/cdk-pipeline.ts` を追加し、PipelineStack と props 参照に必要な Stack を同一 CDK app 内で instantiate する。

`infra/cdk/package.json` に `cdk:pipeline` script を追加する。

Pipeline stage 順序は次にする。

1. Source
2. Synth
3. Build & Test
4. ManualApproval
5. Deploy Registry
6. Build Images
7. Deploy Data / Network / Migration
8. Migration RunTask
9. Deploy AppRuntime

この task では Source / Synth / Build & Test / ManualApproval / Deploy Registry の骨格を作る。
Build Images、Migration RunTask、AppRuntime Blue/Green の詳細は後続 task で実装する。

Artifact bucket は AWS managed key、`crossAccountKeys: false`、30日 expire、非 current version 1日 expire にする。

CodeConnections IAM Policy は `FullRepositoryId` と `BranchName` の condition keys で特定 repository / branch に限定する。

通知は CodeStar Notifications → SNS → メールとし、通知契機は承認待ちと Pipeline 全体失敗だけにする。

## 参考

- AWS CDK Pipelines は CodePipeline V2 を `pipelineType` / feature flag で扱える。実装時は CDK v2 の現在の construct API に合わせて確認する。
- CodePipeline V2 は trigger configuration などの追加パラメータを持つ。file path filter は `phases.md` の方針どおり使わない。

## 判断メモ

- Build & Test は PipelineStack task に含める。`phases.md` の task 境界では単独責務ではなく Pipeline stage の一部であるため。
- ManualApproval は Build Images の前に置く。image build と deploy 系 AWS 更新の前にユーザー承認を挟むため。
- PR review gate は設けない。品質チェックは CodeBuild 内の lint / test / synth で扱うため。
- GitHub Actions は source trigger としても使わない。Phase 2α の正本 CI/CD を AWS 側に寄せるため。

## 対応範囲

- `infra/cdk/lib/pipeline-stack.ts`
- `infra/cdk/bin/cdk-pipeline.ts`
- `infra/cdk/package.json`
- Pipeline artifact bucket
- CodeConnections source
- Build & Test CodeBuild project / step
- ManualApproval
- Deploy Registry stage
- CodeStar Notifications / SNS
- `infra/cdk/test/`
- 本 task ファイルの実装結果・確認結果追記

## 除外範囲

- Build Images project の詳細
- MigrationStack
- Migration RunTask buildspec
- AppRuntimeStack Blue/Green 詳細
- DeployStack 撤去
- GitHub Actions workflow 撤去
- `cdk deploy`
- CodeConnection OAuth 認可
- Pipeline 実走

## 完了条件

- `PipelineStack` が定義されている
- `bin/cdk-pipeline.ts` が追加されている
- `npm run cdk:pipeline -- synth` で PipelineStack を synth できる構成になっている
- Pipeline が CodePipeline V2 として定義されている
- CDK Pipelines の `selfMutation` が有効である
- Source が CodeConnections + main branch で定義されている
- Build & Test が FAIL mode 品質ゲートを実行する構成になっている
- ManualApproval が Build Images の前に定義されている
- Deploy Registry stage が Build Images の前に定義されている
- 通知契機が承認待ちと Pipeline 全体失敗に限定されている

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

- `CodePipelineSource.connection` は action role を直接受け取らないため、core `CodePipeline` と CDK Pipelines の両方で `usePipelineRoleForActions: true` を指定し、Pipeline role に `codeconnections:FullRepositoryId` / `codeconnections:BranchName` 不一致時の明示 Deny を追加した。
- CodeBuild ARM image は `LinuxArmBuildImage.AMAZON_LINUX_2023_STANDARD_3_0` を使用し、生成 template 上で `aws/codebuild/amazonlinux-aarch64-standard:3.0` になることを確認した。
- `WORKOPS_PIPELINE_NOTIFICATION_EMAIL` は環境変数で受け取り、git 管理文書には実メールアドレスを記録しない。確認では `pipeline@example.com` を使用した。

### 実装結果

- `infra/cdk/lib/pipeline-stack.ts` を追加し、CodePipeline V2、CDK Pipelines、CodeConnections、artifact bucket、SNS email topic、ManualApproval、Deploy Registry stage を定義した。
- artifact bucket は S3 managed encryption、`crossAccountKeys: false`、versioning enabled、current object 30日 expire、noncurrent version 1日 expire、Retain にした。
- Source は GitHub `main` branch の CodeConnections source とし、path filter は使っていない。
- Synth / Build & Test / ManualApproval / DeployRegistry の順序を CDK Pipelines の stage と pre steps で定義した。
- Build & Test は `./mvnw spotless:check`、`./mvnw compile spotbugs:check`、`./mvnw verify`、CDK `build` / `test` / `lint` / `format:check` を実行する。
- CodeStar Notifications は承認待ちと Pipeline 全体失敗だけを SNS topic へ通知し、detail type は `BASIC` にした。
- `infra/cdk/bin/cdk-pipeline.ts` と `cdk:pipeline` script を追加し、`WORKOPS_STAGE`、`GITHUB_REPOSITORY`、`WORKOPS_PIPELINE_NOTIFICATION_EMAIL` を必須入力にした。
- CDK test に PipelineStack、entrypoint、CodeConnections IAM 条件、ARM CodeBuild image、通知契機、Build & Test command の検証を追加した。

### 確認結果

- `cd C:\git\workops\infra\cdk; npm run build` 成功。
- `cd C:\git\workops\infra\cdk; npm run test` 成功。2 suites / 22 tests passed。
- `cd C:\git\workops\infra\cdk; npm run lint` 成功。
- `cd C:\git\workops\infra\cdk; npm run format:check` 成功。
- AWS credential 環境変数を削除し、`WORKOPS_STAGE=dev`、`GITHUB_REPOSITORY=kyiwanami/workops`、`WORKOPS_PIPELINE_NOTIFICATION_EMAIL=pipeline@example.com`、`CDK_DEFAULT_ACCOUNT=000000000000`、`CDK_DEFAULT_REGION=ap-northeast-1` で `npm run cdk:pipeline -- synth --quiet` 成功。
- 生成 template で `PipelineType: V2`、`codeconnections:FullRepositoryId=kyiwanami/workops`、`codeconnections:BranchName=main`、`aws/codebuild/amazonlinux-aarch64-standard:3.0`、通知 `DetailType: BASIC` を確認した。

### 残課題

- CodeConnection OAuth 認可、PipelineStack deploy、Pipeline 実走はこの task の除外範囲のため未実施。
- Build Images、MigrationStack、Migration RunTask、AppRuntime Blue/Green、DeployStack / GitHub Actions 撤去は後続 task で扱う。
