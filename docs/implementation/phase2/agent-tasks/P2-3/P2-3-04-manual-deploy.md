# P2-3-04 手動 deploy / HTTP health 確認

## 目的

P2-3 で追加した Docker image と実行確認セッション Stack を使い、AWS dev 上で `apps/web` を手動 deploy して HTTP ALB 経由の `/actuator/health` を確認する。
AWS 実操作はユーザー確認として扱い、エージェント確認と混同しない。

## ユーザー要求

- Maven test を実行する
- Docker build を実行する
- ECR login / push を実行する
- deploy 順序は `EgressStack`、`EdgeStack`、`AppRuntimeStack` とする
- HTTP ALB 経由で `/actuator/health` を確認する
- AWS 実操作はユーザー確認として記録する

## 前提

- P2-3-01、P2-3-02、P2-3-03 が完了している
- P2-2 の維持対象 Stack が必要分 deploy 済みである
- `DataStack` が deploy 済みで、RDS、RDS master secret、DB接続SSM Parameter が存在する
- `DataStack` 再deployとRDS接続再確認は P2-3 task に含めない
- AWS CLI の認証、ECR push、CDK deploy はユーザー確認として扱う
- GitHub Actions deploy は使わない

## 案

- `apps/web` の Maven test を実行する
- `apps/web` の Docker image を `p2-3-manual` tag で build する
- `RegistryStack` の ECR repository `workops-${stage}-web` へ login する
- image を ECR へ push する
- `EgressStack` を deploy する
- `EdgeStack` を deploy する
- `AppRuntimeStack` を deploy する
- ALB DNS name を確認する
- `http://<alb-dns-name>/actuator/health` を確認する

## ADR

- P2-3 の deploy は手動で行う。GitHub Actions OIDC deploy は P2-9 の責務であるため。
- AWS 実操作はユーザー確認として扱う。AWS 認証、課金対象リソース作成、ECR push、CDK deploy はユーザー資格情報と課金を伴うため。
- ECR image tag は `p2-3-manual` に固定する。P2-3 は手動 deploy であり、CI 由来 tag を使わないため。
- deploy 順序は `EgressStack`、`EdgeStack`、`AppRuntimeStack` とする。NAT、ALB、runtime の依存順に合わせるため。
- `/actuator/health` だけを HTTP ALB 経由の成功確認にする。業務画面ログイン、Cognito Hosted UI、users 突合は P2-3 の責務ではないため。
- 既存データ移行と後方互換性は考慮しない。

## 対応範囲

- 手動 deploy 手順を task に記録する
- Maven test 手順を記録する
- Docker build 手順を記録する
- ECR login / tag / push 手順を記録する
- CDK deploy 順序を記録する
- ALB DNS name の確認手順を記録する
- `/actuator/health` の HTTP 確認手順を記録する
- ECS service / task / target group health の確認手順を記録する

## 除外範囲

- GitHub Actions workflow
- CodeBuild
- CodePipeline
- main push 自動 deploy
- 本番 deploy
- HTTPS listener
- ACM certificate
- Route 53
- Cognito Hosted UI
- 業務画面ログイン確認
- users 突合
- seed 件数確認
- destroy 手順
- git commit

## 完了条件

- `apps/web` の Maven test が成功している
- Docker image を `p2-3-manual` tag で build できる
- ECR repository `workops-${stage}-web` に `p2-3-manual` tag を push できる
- `EgressStack` を deploy できる
- `EdgeStack` を deploy できる
- `AppRuntimeStack` を deploy できる
- ECS Service が task を起動できる
- Target Group が healthy になる
- HTTP ALB 経由で `/actuator/health` が HTTP 200 を返す

## 確認方法

エージェント確認:

```powershell
cd C:\git\workops\apps\web
.\mvnw.cmd test
docker build -t workops-web:p2-3-manual .
```

ユーザー確認:

```powershell
$env:WORKOPS_STAGE='dev'
$env:AWS_REGION='<aws-region>'
$env:AWS_ACCOUNT_ID='<aws-account-id>'

aws ecr get-login-password --region $env:AWS_REGION |
  docker login --username AWS --password-stdin "$env:AWS_ACCOUNT_ID.dkr.ecr.$env:AWS_REGION.amazonaws.com"

docker tag workops-web:p2-3-manual "$env:AWS_ACCOUNT_ID.dkr.ecr.$env:AWS_REGION.amazonaws.com/workops-dev-web:p2-3-manual"
docker push "$env:AWS_ACCOUNT_ID.dkr.ecr.$env:AWS_REGION.amazonaws.com/workops-dev-web:p2-3-manual"

cd C:\git\workops\infra\cdk
npm run cdk -- deploy EgressStack
npm run cdk -- deploy EdgeStack
npm run cdk -- deploy AppRuntimeStack
```

HTTP health 確認:

```powershell
Invoke-WebRequest -Uri "http://<alb-dns-name>/actuator/health" -UseBasicParsing
```

AWS Console 確認:

- ECR repository `workops-dev-web` に `p2-3-manual` tag がある
- CloudFormation Stack `workops-dev-egress` が作成済み
- CloudFormation Stack `workops-dev-edge` が作成済み
- CloudFormation Stack `workops-dev-app-runtime` が作成済み
- ECS Service が `desiredCount=1` である
- ECS task が `RUNNING` である
- Target Group target が healthy である
