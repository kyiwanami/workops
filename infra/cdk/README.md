# WorkOps CDK

`infra/cdk` は WorkOps の AWS dev 基盤を管理する AWS CDK v2 TypeScript プロジェクトです。

## Prerequisites

- Node.js
- npm
- AWS CLI
- AWS CDK CLI
- AWS 認証 profile

依存関係の復元は、ユーザー確認済みのタイミングでだけ実行します。

```powershell
cd C:\git\workops\infra\cdk
npm ci
```

## Environment

PowerShell で次の環境変数を指定します。

```powershell
cd C:\git\workops\infra\cdk
Remove-Item Env:AWS_ACCESS_KEY_ID -ErrorAction SilentlyContinue
Remove-Item Env:AWS_SECRET_ACCESS_KEY -ErrorAction SilentlyContinue
Remove-Item Env:AWS_SESSION_TOKEN -ErrorAction SilentlyContinue

$env:WORKOPS_STAGE = "dev"
$env:AWS_PROFILE = "your-profile"
$env:AWS_SDK_LOAD_CONFIG = "1"
$env:AWS_REGION = "ap-northeast-1"
$env:AWS_DEFAULT_REGION = "ap-northeast-1"
$env:CDK_DEFAULT_ACCOUNT = (aws sts get-caller-identity --region $env:AWS_REGION --query Account --output text)
$env:CDK_DEFAULT_REGION = "ap-northeast-1"
```

- `WORKOPS_STAGE` は CloudFormation stackName、resource name、tag の `Environment` に使います。
- `AWS_PROFILE` と `AWS_REGION` は AWS CLI / CDK CLI の接続先指定です。
- `CDK_DEFAULT_ACCOUNT` と `CDK_DEFAULT_REGION` は CDK stack の `env` と context lookup に使います。
- CDK app は AWS account、profile、region を固定しません。
- CDK app は環境変数ファイルを読みません。
- `stage` は CDK context ではなく `WORKOPS_STAGE` から渡します。

## Commands

build と test を実行します。

```powershell
cd C:\git\workops\infra\cdk
npm run build
npm test
```

CloudFormation template を生成します。

```powershell
cd C:\git\workops\infra\cdk
Remove-Item Env:AWS_ACCESS_KEY_ID -ErrorAction SilentlyContinue
Remove-Item Env:AWS_SECRET_ACCESS_KEY -ErrorAction SilentlyContinue
Remove-Item Env:AWS_SESSION_TOKEN -ErrorAction SilentlyContinue

$env:WORKOPS_STAGE = "dev"
$env:AWS_PROFILE = "your-profile"
$env:AWS_SDK_LOAD_CONFIG = "1"
$env:AWS_REGION = "ap-northeast-1"
$env:AWS_DEFAULT_REGION = "ap-northeast-1"
$env:CDK_DEFAULT_ACCOUNT = (aws sts get-caller-identity --region $env:AWS_REGION --query Account --output text)
$env:CDK_DEFAULT_REGION = "ap-northeast-1"
$env:WORKOPS_IMAGE_TAG = "test-sha"
$env:GITHUB_REPOSITORY = "owner/repo"
$env:WORKOPS_PIPELINE_NOTIFICATION_EMAIL = "pipeline@example.com"
npm run cdk:infra -- synth --quiet --profile $env:AWS_PROFILE
npm run cdk:runtime -- synth --quiet --profile $env:AWS_PROFILE
npm run cdk:pipeline -- synth --quiet --profile $env:AWS_PROFILE
```

`EdgeStack` は CloudFront origin-facing managed prefix list を `PrefixList.fromLookup` で参照します。
未キャッシュの lookup がある場合、`synth` でも AWS 認証情報が必要です。

## Stacks

CDK app は次の Stack を管理します。

- `workops-{stage}-foundation`
- `workops-{stage}-data`
- `workops-{stage}-config`
- `workops-{stage}-identity`
- `workops-{stage}-registry`
- `workops-{stage}-logs`
- `workops-{stage}-migration`
- `workops-{stage}-pipeline`

`stage` は `WORKOPS_STAGE` の値です。

`workops-{stage}-identity` は Cognito User Pool、Hosted UI domain、App Client を所有する維持対象 Stack です。CloudFront / ALB / ECS / NAT Gateway の実行確認セッション Stack とは lifecycle を分けます。

`workops-{stage}-pipeline` は CodePipeline V2、CodeBuild、CodeConnection、SNS 通知、Artifact bucket を所有します。GitHub Actions OIDC deploy 経路は使いません。

## Bootstrap

初回 deploy 前に対象 AWS account / region を bootstrap します。

```powershell
cd C:\git\workops\infra\cdk
$env:AWS_PROFILE = "your-profile"
$env:AWS_REGION = "ap-northeast-1"
npx cdk bootstrap
```

`bootstrap` は CDK bootstrap stack を対象 account / region に作る操作です。
WorkOps の `stage` resource name を生成しないため、`WORKOPS_STAGE` は使いません。

## Deploy

PipelineStack を初回 deploy します。

```powershell
cd C:\git\workops\infra\cdk
$env:WORKOPS_STAGE = "dev"
$env:AWS_PROFILE = "your-profile"
$env:AWS_REGION = "ap-northeast-1"
$env:GITHUB_REPOSITORY = "owner/repo"
$env:WORKOPS_PIPELINE_NOTIFICATION_EMAIL = "pipeline@example.com"
$env:WORKOPS_IMAGE_TAG = "test-sha"
npm run cdk:pipeline -- diff PipelineStack
npm run cdk:pipeline -- deploy PipelineStack
```

Pipeline 実走前に手元で runtime Stack を確認する場合は、synth と diff までに留めます。

```powershell
cd C:\git\workops\infra\cdk
$env:WORKOPS_STAGE = "dev"
$env:AWS_PROFILE = "your-profile"
$env:AWS_REGION = "ap-northeast-1"
$env:WORKOPS_IMAGE_TAG = "test-sha"
npm run cdk:runtime -- diff AppRuntimeStack
```

## Destroy

P2-alpha-3 の確認完了後、主要課金 4 stack を削除します。

```powershell
cd C:\git\workops\infra\cdk
$env:WORKOPS_STAGE = "dev"
$env:AWS_PROFILE = "your-profile"
$env:AWS_REGION = "ap-northeast-1"
npm run cdk:runtime -- destroy AppRuntimeStack EdgeStack EgressStack DataStack
```

PipelineStack、MigrationStack、FoundationStack、ConfigStack、IdentityStack、RegistryStack、LogsStack、Artifact bucket、ECR repository、CodeConnection は維持対象です。

## Conventions

- resource name は `workops-{stage}-...` 形式にします。
- SSM Parameter Store Standard のパスは `/workops/{stage}/...` 形式にします。
- 秘匿値は SSM Parameter Store に保存しません。
- DB username と DB password は SSM Parameter Store に保存しません。
- DB endpoint から派生する SSM Parameter は、RDS を所有する Stack に置きます。
- DB に依存しない runtime config は `ConfigStack` に置きます。
- Cognito User Pool、Hosted UI domain、App Client は `IdentityStack` に置きます。
- `IdentityStack` は `EdgeStack` を参照しません。
- Stack 間参照は同一 CDK app 内の props 参照で渡し、cross-stack reference は `weak` に固定します。
- `weak` により、生成 template は `Fn::ImportValue` ではなく `Fn::GetStackOutput` を使います。
