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
$env:WORKOPS_STAGE = "dev"
$env:AWS_PROFILE = "your-profile"
$env:AWS_REGION = "ap-northeast-1"
```

- `WORKOPS_STAGE` は CloudFormation stackName、resource name、tag の `Environment` に使います。
- `AWS_PROFILE` と `AWS_REGION` は CDK CLI の deploy 先指定です。
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
$env:WORKOPS_STAGE = "dev"
npm run cdk -- synth
```

`synth` は AWS account 未指定でも実行できます。
AWS 環境を参照する lookup を使っていないため、template 生成だけなら account は確定しません。

## Stacks

CDK app は次の Stack を管理します。

- `workops-{stage}-foundation`
- `workops-{stage}-secret`
- `workops-{stage}-data`
- `workops-{stage}-config`
- `workops-{stage}-identity`
- `workops-{stage}-registry`
- `workops-{stage}-logs`

`stage` は `WORKOPS_STAGE` の値です。

`workops-{stage}-identity` は Cognito User Pool、Hosted UI domain、App Client を所有する維持対象 Stack です。CloudFront / ALB / ECS / NAT Gateway の実行確認セッション Stack とは lifecycle を分けます。

## Bootstrap

初回 deploy 前に対象 AWS account / region を bootstrap します。

```powershell
cd C:\git\workops\infra\cdk
$env:AWS_PROFILE = "your-profile"
$env:AWS_REGION = "ap-northeast-1"
npm run cdk -- bootstrap
```

`bootstrap` は CDK bootstrap stack を対象 account / region に作る操作です。
WorkOps の `stage` resource name を生成しないため、`WORKOPS_STAGE` は使いません。

## Deploy

全 Stack を deploy します。

```powershell
cd C:\git\workops\infra\cdk
$env:WORKOPS_STAGE = "dev"
$env:AWS_PROFILE = "your-profile"
$env:AWS_REGION = "ap-northeast-1"
npm run cdk -- deploy --all
```

個別 Stack を deploy する場合は、Stack 名を指定します。

```powershell
cd C:\git\workops\infra\cdk
$env:WORKOPS_STAGE = "dev"
$env:AWS_PROFILE = "your-profile"
$env:AWS_REGION = "ap-northeast-1"
npm run cdk -- deploy workops-dev-data
```

## Destroy

全 Stack を削除します。

```powershell
cd C:\git\workops\infra\cdk
$env:WORKOPS_STAGE = "dev"
$env:AWS_PROFILE = "your-profile"
$env:AWS_REGION = "ap-northeast-1"
npm run cdk -- destroy --all
```

個別 Stack を削除する場合は、Stack 名を指定します。

```powershell
cd C:\git\workops\infra\cdk
$env:WORKOPS_STAGE = "dev"
$env:AWS_PROFILE = "your-profile"
$env:AWS_REGION = "ap-northeast-1"
npm run cdk -- destroy workops-dev-data
```

実行確認セッション後に短命 Stack を削除する場合は、`workops-{stage}-app-runtime`、`workops-{stage}-edge`、`workops-{stage}-egress`、`workops-{stage}-data` を対象にします。`workops-{stage}-identity` は残します。

## Conventions

- resource name は `workops-{stage}-...` 形式にします。
- SSM Parameter Store Standard のパスは `/workops/{stage}/...` 形式にします。
- 秘匿値は SSM Parameter Store に保存しません。
- DB username と DB password は SSM Parameter Store に保存しません。
- DB endpoint から派生する SSM Parameter は、RDS を所有する Stack に置きます。
- DB に依存しない runtime config は `ConfigStack` に置きます。
- Cognito User Pool、Hosted UI domain、App Client は `IdentityStack` に置きます。
- `IdentityStack` は `EdgeStack` を参照しません。
- CloudFormation Output に password、secret value、public IP、EC2 instance id を出しません。
- Stack 間参照は同一 CDK app 内の props 参照で渡し、cross-stack reference は `weak` に固定します。
- `weak` により、生成 template は `Fn::ImportValue` ではなく `Fn::GetStackOutput` を使います。
