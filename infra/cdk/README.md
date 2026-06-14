# WorkOps CDK

`infra/cdk` は WorkOps Phase 2 の AWS dev 基盤を管理する AWS CDK v2 TypeScript プロジェクトです。

## P2-1 で作るもの

- `FoundationStack`
  - VPC `workops-dev-vpc`
  - public / app / db subnet group
  - ALB / app / DB 用 Security Group
  - ECS Cluster `workops-dev-cluster`
- `RegistryStack`
  - ECR repository `workops-dev-web`
  - tagged image 最新2件保持、untagged image 1日後削除の lifecycle policy
- `LogsStack`
  - CloudWatch Logs log group `/workops/dev/web`
  - CloudWatch Logs log group `/workops/dev/migration`
- `ConfigStack`
  - P2-1 時点では空 Stack

## P2-2-01 で追加するもの

- `SecretStack`
  - P2-2-01 時点では将来の独立 secret 置き場として空 Stack
- `DataStack`
  - RDS for MySQL 8.4.9 `workops-dev-db`
  - DB Subnet Group `workops-dev-db-subnet-group`
  - RDS master secret `/workops/dev/db/master`
  - `app-sg` から `db-sg` への TCP 3306 inbound rule
- `ConfigStack`
  - `/workops/dev/spring/profile`
  - `/workops/dev/db/name`
  - `/workops/dev/db/port`
  - `/workops/dev/db/url`

## P2-2-01 で作らないもの

- Cognito
- ALB
- NAT Gateway
- ECS Service
- Fargate task
- Dockerfile
- Docker build / ECR push
- GitHub Actions workflow
- DB access host
- Flyway profile / seed 分割

## ローカル前提

PowerShell で次の環境変数を指定します。

```powershell
cd C:\git\workops\infra\cdk
$env:WORKOPS_STAGE = "dev"
$env:AWS_PROFILE = "your-profile"
$env:AWS_REGION = "ap-northeast-1"
```

- `WORKOPS_STAGE` は CloudFormation stackName、resource name、tag の `Environment` に使います。
- `AWS_PROFILE` と `AWS_REGION` は CDK CLI 実行環境の deploy 先指定です。
- CDK code は AWS account、profile、region を固定しません。
- CDK app は環境変数ファイルを読みません。
- `stage` はCDK contextではなく `WORKOPS_STAGE` から渡します。

## Install / Build / Test / Synth

依存関係を復元します。

```powershell
cd C:\git\workops\infra\cdk
npm ci
```

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

`synth` は AWS account 未指定でも実行できます。AWS環境を参照する lookup を使っていないため、テンプレート生成だけなら account は確定しません。

## Bootstrap

初回 deploy 前に対象 AWS account / region を bootstrap します。

```powershell
cd C:\git\workops\infra\cdk
$env:AWS_PROFILE = "your-profile"
$env:AWS_REGION = "ap-northeast-1"
npm run cdk -- bootstrap
```

`bootstrap` は CDK bootstrap stack を対象 account / region に作る操作です。WorkOps の `stage` resource name を生成しないため、`WORKOPS_STAGE` は使いません。

## Deploy

P2-1 の4 Stackを deploy します。

```powershell
cd C:\git\workops\infra\cdk
$env:WORKOPS_STAGE = "dev"
$env:AWS_PROFILE = "your-profile"
$env:AWS_REGION = "ap-northeast-1"
npm run cdk -- deploy --all
```

P2-2-01 後の deploy 対象 Stack は次の6つです。

- `workops-dev-foundation`
- `workops-dev-secret`
- `workops-dev-data`
- `workops-dev-config`
- `workops-dev-registry`
- `workops-dev-logs`

## AWS Console 確認項目

CloudFormation:

- `workops-dev-foundation`
- `workops-dev-secret`
- `workops-dev-data`
- `workops-dev-config`
- `workops-dev-registry`
- `workops-dev-logs`

VPC:

- CIDR が `10.0.0.0/16`
- Name tag が `workops-dev-vpc`
- public / app / db 用の subnet が合計6つある
- NAT Gateway がない

Security Group:

- `workops-dev-alb-sg`
- `workops-dev-app-sg`
- `workops-dev-db-sg`
- P2-2-01 後は `workops-dev-app-sg` から `workops-dev-db-sg` への TCP 3306 inbound rule がある

RDS:

- DB identifier が `workops-dev-db`
- Engine が MySQL 8.4.9
- Instance class が `db.t4g.micro`
- Storage が 20 GiB / gp2
- Single-AZ
- Public accessibility が false
- Backup retention が1日
- Deletion protection が false

Secrets Manager:

- `/workops/dev/db/master`
- secret JSON は `username` / `password` を持つ
- username は `workops_admin`

Systems Manager Parameter Store:

- `/workops/dev/spring/profile`
- `/workops/dev/db/name`
- `/workops/dev/db/port`
- `/workops/dev/db/url`

ECS:

- Cluster 名が `workops-dev-cluster`

ECR:

- repository 名が `workops-dev-web`
- lifecycle policy が設定されている
- stack削除時に repository 内の image を空にして削除できる

CloudWatch Logs:

- `/workops/dev/web`
- `/workops/dev/migration`
- retention が7日

CloudFormation Outputs:

- FoundationStack の VPC / subnet / Security Group / ECS Cluster 情報
- DataStack の RDS identifier / endpoint / port / DB name / subnet group / master secret ARN
- RegistryStack の repository 情報
- LogsStack の log group 名

## Destroy

P2-1単体確認後、または Phase 2 全撤去時にStackを削除します。

```powershell
cd C:\git\workops\infra\cdk
$env:WORKOPS_STAGE = "dev"
$env:AWS_PROFILE = "your-profile"
$env:AWS_REGION = "ap-northeast-1"
npm run cdk -- destroy --all
```

P2-2へ進む場合は P2-1 Stack を削除しません。P2-2以降のRDS、Cognito、ECS実行基盤がP2-1 Stackを前提にします。

ECR repository は `RemovalPolicy.DESTROY` と `emptyOnDelete: true` を明示しています。imageが残っていても、`destroy --all` では image を空にしてから repository を削除します。

## 設計意図

- `WORKOPS_STAGE` はCDK contextではなく環境変数で渡します。
- ローカルとGitHub Actionsは同じ `WORKOPS_STAGE` 名で stage を渡します。
- GitHub Actions の OIDC と `aws-region` 指定は P2-8 で扱います。
- `ConfigStack` はP2-2-01から非機密SSM Parameterを管理します。
- `SecretStack` はP2-2-01では空 Stack とし、app用secretやmigration用secretを作りません。
- RDS master secret はRDS lifecycleに合わせるため `DataStack` に置きます。
- Stack間参照は同一CDK app内のprops参照で渡し、cross-stack reference は `weak` に固定します。
- `weak` により、生成テンプレートは `Fn::ImportValue` ではなく `Fn::GetStackOutput` を使います。
- LogsStack は `AppRuntimeStack` 削除後も Phase 2 期間中の調査ログを残すため独立 Stack にします。
- ECR image は再生成可能な成果物として扱い、非空 repository を理由に `destroy --all` を失敗させません。
- SSM Parameter Store Standard のパス規約は `/workops/{stage}/...` とし、秘匿値は入れません。

## CDK デフォルトに任せるもの

- subnet CIDR の具体割り当て
- subnet の Name tag
- DNS support
- DNS hostnames
- Internet Gateway
- public route table
- subnet route table association

## 明示設定するもの

- VPC CIDR は `10.0.0.0/16`
- VPC は2AZ構成
- subnet group は `public`、`app`、`db`
- `public` subnet は `PUBLIC`
- `app` と `db` subnet は P2-1 時点で `PRIVATE_ISOLATED`
- NAT Gateway は `0`
- Security Group は `alb-sg`、`app-sg`、`db-sg`
- P2-2-01では `app-sg` から `db-sg` への TCP 3306 inbound rule を追加する
- migration task は `app-sg` を使い、`migration-sg` は作らない
- CloudWatch Logs retention は7日
- CloudWatch Logs は `RemovalPolicy.DESTROY`
- ECR repository は `RemovalPolicy.DESTROY` と `emptyOnDelete: true`
- `AWS_REGION`、`retentionDays`、ECR repository 名は SSM に入れない
- DB username、DB password は SSM に入れない
