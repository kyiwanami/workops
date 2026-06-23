# P2-alpha-3-01 Initial Pipeline deploy

## 目的

P2-alpha-3 の最初の作業として、`PipelineStack` の初回 deploy、CodeConnection OAuth 認可、`main` push による Pipeline 起動、ManualApproval までを協働で確認する。

この task では CDK code 変更を行わない。

## ユーザー要求

- P2-alpha-3 は CDK code 作業を伴わない
- `.ps1` は作らず、Markdown 上の PowerShell 手順として残す
- P2-alpha-3 は分割しすぎず、初回 deploy、実走確認、完了記録の3本に分ける
- AWS 操作では profile、region、`WORKOPS_STAGE` を明示する
- AWS credential 環境変数を削除してから profile を使う
- 実 account ID、role ARN、credential、public IP、実 Cognito `sub`、DB 実データ値、CloudFront domain は git 管理文書に残さない
- Codex は `git commit` を実行しない

## 前提

- P2-alpha-2 が完了している
- 新構成の `cdk synth` が pass している
- `PipelineStack` が定義済みである
- CodeConnection OAuth 認可をユーザーが実施できる
- P2-alpha-3 手順 Markdown の追加 commit / push はユーザーが実施する

## 案

初回 deploy は、AWS 認証先、既存 Stack 状態、`cdk diff` を確認したうえで行う。

`PipelineStack` deploy 後、CodeConnection OAuth 認可をユーザーが実施する。
その後、P2-alpha-3 手順 Markdown 追加 commit を `main` に push し、Pipeline が起動することを確認する。

ManualApproval の通知到達と承認は、ユーザー操作を含む協働確認として扱う。

## 判断メモ

- 事前確認だけの独立 task は作らない。既存の確認は P2-alpha-2 までで実施済みであり、P2-alpha-3 では初回 deploy 手順の一部として扱うため。
- Pipeline 自走確認用の差分は、P2-alpha-3 手順 Markdown 追加 commit とする。空 commit ではなく、運用文書の追加を起点にするため。
- `cdk diff` と `cdk deploy` は同じ task に含める。実 deploy の直前確認として扱うため。

## 対応範囲

- AWS profile / region / stage の明示
- AWS credential 環境変数削除
- caller identity 確認
- 既存 Stack 状態確認
- `PipelineStack` の `cdk diff`
- `PipelineStack` 初回 deploy
- CodeConnection OAuth 認可のユーザー操作確認
- P2-alpha-3 手順 Markdown 追加 commit / push のユーザー操作確認
- Pipeline 起動確認
- ManualApproval 通知到達と承認確認

## 除外範囲

- CDK code 変更
- Pipeline stage 詳細確認
- ECR image tag 確認
- Migration RunTask 結果確認
- AppRuntime / CloudFront / Alarm / Logs 確認
- ベースライン記録
- 主要課金 4 stack destroy
- 失敗事象の最終集約

## 完了条件

- AWS profile、region、`WORKOPS_STAGE` が明示されている
- AWS credential 環境変数を削除した状態で caller identity を確認している
- 既存 Stack 状態を確認している
- `PipelineStack` の `cdk diff` を確認している
- `PipelineStack` 初回 deploy が完了している
- CodeConnection OAuth 認可が完了している
- `main` push を契機に Pipeline が起動している
- ManualApproval 通知が到達し、ユーザーが承認している
- 実 account ID、role ARN、credential、public IP、実 Cognito `sub`、DB 実データ値、CloudFront domain を記録していない

## 確認方法

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

既存 Stack 状態確認:

```powershell
aws cloudformation describe-stacks `
  --profile $env:AWS_PROFILE `
  --region $env:AWS_REGION `
  --query "Stacks[?contains(StackName, 'workops')].[StackName,StackStatus]" `
  --output table
```

`PipelineStack` diff / deploy:

```powershell
cd C:\git\workops\infra\cdk
npm run cdk:pipeline -- diff PipelineStack --profile $env:AWS_PROFILE
npm run cdk:pipeline -- deploy PipelineStack --profile $env:AWS_PROFILE
```

Pipeline 起動確認:

```powershell
aws codepipeline list-pipelines `
  --profile $env:AWS_PROFILE `
  --region $env:AWS_REGION `
  --output table

aws codepipeline get-pipeline-state `
  --name "<pipeline-name>" `
  --profile $env:AWS_PROFILE `
  --region $env:AWS_REGION
```

ManualApproval 確認:

- 承認待ち通知メールが届くことを確認する
- ユーザーが AWS Console で ManualApproval を承認する
- 承認後に Pipeline が次 stage へ進むことを確認する

## 実装時の記録

### 実施結果

- AWS profile `amazon-connect`、region `ap-northeast-1`、`WORKOPS_STAGE=dev` で実施した。
- AWS credential 環境変数を削除し、`aws sts get-caller-identity` で認証先を確認した。実 account ID は記録しない。
- 既存 Stack 状態を確認した。
- `npm run cdk:pipeline -- synth --quiet --profile amazon-connect` は成功した。
- `npm run cdk:pipeline -- diff PipelineStack --profile amazon-connect` は成功した。
- 初回 `PipelineStack` deploy は `AWS::CodeStarNotifications::NotificationRule` 作成で失敗し、`workops-dev-pipeline` は `ROLLBACK_COMPLETE` になった。
- 失敗時に残った空の `workops-dev-pipeline-artifacts` bucket と `ROLLBACK_COMPLETE` stack を削除した。
- 初回失敗で AWS 側に作成された `AWSServiceRoleForCodeStarNotifications` を削除した。
- CDK で `AWS::IAM::ServiceLinkedRole` を明示定義し、`AWS::CodeStarNotifications::NotificationRule` がその作成後に実行されるようにした。
- Artifact bucket の `removalPolicy` を `DESTROY` に変更し、`PipelineStack` destroy 時に bucket が retain されないようにした。
- CodePipeline Source action が権限で失敗したため、Pipeline role に入れていた `codeconnections:FullRepositoryId` / `codeconnections:BranchName` 不一致時の明示 Deny を削除した。
- 修正後の `npm run build`、`npm run lint`、`npm run test -- --runInBand` は成功した。
- 修正後の `PipelineStack` deploy は成功した。
- `PipelineStack` 更新 deploy により、artifact bucket の `DeletionPolicy` / `UpdateReplacePolicy` が `Delete` になったことを確認した。
- `PipelineStack` 更新 deploy により、Pipeline role policy から `UseConnection` の明示 Deny が削除されたことを確認した。
- `workops-dev-github` CodeConnection 認可後に Pipeline execution を手動開始した。
- `GitHubSource` は成功した。
- `Synth` は `npm ci` で失敗したため、CodeBuild と同じ npm 10.8.2 で `package-lock.json` を同期した。
- lock 同期後の `npm ci --dry-run`、`npm run build`、`npm run lint`、`npm run test -- --runInBand` は成功した。

### 確認結果

- `workops-dev-pipeline` は `CREATE_COMPLETE`。
- CodeConnection は、CLI で `workops-dev-github` と別名 `GitHub` がどちらも `AVAILABLE` と返った。Pipeline が参照する接続は `workops-dev-github`。
- `workops-dev-pipeline-notifications` の email subscription は確認済み ARN が返っている。
- CodePipeline は Source stage / `GitHubSource` で failed。原因は Pipeline role の `UseConnection` 明示 Deny。
- `UseConnection` 明示 Deny 削除後も、既存の Pipeline execution は failed のまま。修正 commit / push 後に再実行して確認する。
- 修正 commit / push 後も新規 Pipeline execution は発生していない。Pipeline が参照する `workops-dev-github` の認可が未完了のため、`GitHub` ではなく `workops-dev-github` を認可する必要がある。
- `workops-dev-github` 認可後の手動 execution では Source が成功し、Synth が `npm ci` で失敗した。lock 同期後に再実行して確認する。

### 残課題

- Pipeline 起動確認。
- ManualApproval 通知到達と承認確認。
