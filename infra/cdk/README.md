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
$env:WORKOPS_OPS_NOTIFICATION_EMAIL = "ops@example.com"
```

- `WORKOPS_STAGE` は CloudFormation stackName、resource name、tag の `Environment` に使います。
- `AWS_PROFILE` と `AWS_REGION` は AWS CLI / CDK CLI の接続先指定です。
- `CDK_DEFAULT_ACCOUNT` と `CDK_DEFAULT_REGION` は CDK stack の `env` と context lookup に使います。
- `WORKOPS_OPS_NOTIFICATION_EMAIL` は ops notification topic の EmailSubscription に使います。
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
$env:WORKOPS_OPS_NOTIFICATION_EMAIL = "ops@example.com"
npx cdk synth --quiet --profile $env:AWS_PROFILE
```

未キャッシュの CDK context lookup がある場合、`synth` でも AWS 認証情報が必要です。

## Stacks

CDK app は次の Stack を管理します。

- `workops-{stage}-foundation`
- `workops-{stage}-data`
- `workops-{stage}-dependency`
- `workops-{stage}-identity`
- `workops-{stage}-registry`
- `workops-{stage}-logs`
- `workops-{stage}-migration`
- `workops-{stage}-pipeline`

`stage` は `WORKOPS_STAGE` の値です。

`workops-{stage}-dependency` は CodeArtifact domain / npm repository / Maven repository、ops notification topic、非 secret SSM parameters を所有する Top-level / Pipeline 基盤 Stack です。

`workops-{stage}-identity` は Cognito User Pool、Hosted UI domain、App Client を所有する維持対象 Stack です。CloudFront / ALB / ECS / NAT Gateway の実行確認セッション Stack とは lifecycle を分けます。

`workops-{stage}-pipeline` は CodePipeline V2、CodeBuild、CodeConnection、Artifact bucket を所有し、通知先は `DependencyStack` の ops notification topic を参照します。GitHub Actions OIDC deploy 経路は使いません。

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

通常の diff / deploy は全 Stack 対象にします。

```powershell
cd C:\git\workops\infra\cdk
$env:WORKOPS_STAGE = "dev"
$env:AWS_PROFILE = "your-profile"
$env:AWS_REGION = "ap-northeast-1"
$env:GITHUB_REPOSITORY = "owner/repo"
$env:WORKOPS_OPS_NOTIFICATION_EMAIL = "ops@example.com"
$env:WORKOPS_IMAGE_TAG = "test-sha"
npx cdk diff --all --profile $env:AWS_PROFILE
npx cdk deploy --all --profile $env:AWS_PROFILE
```

初回 bootstrap 直後だけ、依存基盤と Pipeline を先行 deploy する例外を認めます。

```powershell
cd C:\git\workops\infra\cdk
$env:WORKOPS_STAGE = "dev"
$env:AWS_PROFILE = "your-profile"
$env:AWS_REGION = "ap-northeast-1"
$env:GITHUB_REPOSITORY = "owner/repo"
$env:WORKOPS_OPS_NOTIFICATION_EMAIL = "ops@example.com"
$env:WORKOPS_IMAGE_TAG = "test-sha"
npx cdk diff DependencyStack PipelineStack --profile $env:AWS_PROFILE
npx cdk deploy DependencyStack PipelineStack --profile $env:AWS_PROFILE
```

## Destroy

P2-alpha-3 以降、runtime resource の作成・更新・削除は Pipeline 管理に寄せます。
個別 runtime CDK entrypoint は使いません。

PipelineStack、DependencyStack、MigrationStack、FoundationStack、IdentityStack、RegistryStack、LogsStack、Artifact bucket、ECR repository、CodeConnection は維持対象です。

## Conventions

- resource name は `workops-{stage}-...` 形式にします。
- SSM Parameter Store Standard のパスは `/workops/{stage}/...` 形式にします。
- 秘匿値は SSM Parameter Store に保存しません。
- DB username と DB password は SSM Parameter Store に保存しません。
- DB endpoint から派生する SSM Parameter は、RDS を所有する Stack に置きます。
- DB に依存しない runtime config は `DependencyStack` の `/workops/{stage}/dependencies/...` に置きます。
- Cognito User Pool、Hosted UI domain、App Client は `IdentityStack` に置きます。
- `IdentityStack` は `EdgeStack` を参照しません。
- Stack 間参照は同一 CDK app 内の props 参照で渡し、cross-stack reference は `weak` に固定します。
- `weak` により、生成 template は `Fn::ImportValue` ではなく `Fn::GetStackOutput` を使います。
