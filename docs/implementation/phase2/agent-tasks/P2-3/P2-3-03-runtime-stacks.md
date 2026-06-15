# P2-3-03 実行確認セッション Stack

## 目的

AWS dev 上で `apps/web` を HTTP ALB 経由で起動確認するため、実行確認セッション用の `EgressStack`、`EdgeStack`、`AppRuntimeStack` を追加する。
3 Stack は常時 synth 対象にし、deploy / destroy で実行確認セッションを開始・終了する。

## ユーザー要求

- `EgressStack` / `EdgeStack` / `AppRuntimeStack` を作る
- 3 Stack は常時 synth 対象にする
- ECS Service は `desiredCount=1` とする
- Fargate は `cpu=512` / `memory=1024MiB` とする
- internet-facing ALB、HTTP 80 のみとする
- health check は `/actuator/health`、成功コード `200` とする
- CloudWatch Logs は `/workops/dev/web` を使う
- `/workops/dev/migration` は P2-3 では使わない

## 前提

- P2-2 の維持対象 Stack が deploy 済みである
- `DataStack` が deploy 済みで、RDS、RDS master secret、DB接続SSM Parameter が存在する
- `DataStack` 再deployとRDS接続再確認は P2-3 task に含めない
- `RegistryStack` の ECR repository `workops-${stage}-web` が存在する
- `LogsStack` の log group `/workops/${stage}/web` が存在する
- P2-3-01 と P2-3-02 が完了している

## 案

- `EgressStack` を追加する
- `EdgeStack` を追加する
- `AppRuntimeStack` を追加する
- `bin/cdk.ts` から3 Stackを常時作成する
- Stack class 名に `dev` / `stg` / `prod` を入れない
- CloudFormation stackName は `workops-${stage}-egress`、`workops-${stage}-edge`、`workops-${stage}-app-runtime` とする
- 作成順序は `EgressStack`、`EdgeStack`、`AppRuntimeStack` とする
- 削除順序は `AppRuntimeStack`、`EdgeStack`、`EgressStack` とする

## ADR

- 実行確認用 Stack は常時 synth 対象にする。deploy しなければ AWS リソースは作られず、CDK assertion test と synth で常に検証できるため。
- `DataStack` 再deployは P2-3 に含めない。RDS、DB secret、DB接続SSM Parameter は P2-2 の責務であるため。
- `EgressStack` は実行確認セッション用の NAT Gateway を持つ。`FoundationStack` は P2-1 方針どおり NAT Gateway を持たないため。
- `EdgeStack` は internet-facing ALB と HTTP listener だけを持つ。HTTPS、ACM、Route 53、redirect は P2-4 の責務であるため。
- `AppRuntimeStack` は ECS Service を `desiredCount=1` に固定する。P2-3 は可用性や autoscaling ではなく単一 task の起動確認が目的であるため。
- `AppRuntimeStack` は確認後に削除する。`desiredCount=0` で残す運用は採用しない。
- `/workops/${stage}/migration` log group は P2-3 では使わない。P2-3 の Flyway は `apps/web` 起動時に実行され、ログは web app log として `/workops/${stage}/web` に出るため。
- 既存データ移行と後方互換性は考慮しない。

## 対応範囲

- `infra/cdk/lib/egress-stack.ts` を追加する
- `infra/cdk/lib/edge-stack.ts` を追加する
- `infra/cdk/lib/app-runtime-stack.ts` を追加する
- `infra/cdk/bin/cdk.ts` へ3 Stackを追加する
- `EgressStack` は NAT Gateway と NAT Gateway 用 EIP を作成する
- `EgressStack` は app subnet route table に `0.0.0.0/0` route を追加する
- `EdgeStack` は public subnet に internet-facing Application Load Balancer を作成する
- `EdgeStack` は HTTP listener port `80` を作成する
- `EdgeStack` は target group を作成し、health check を `/actuator/health` にする
- health check は success code `200`、interval 30秒、timeout 5秒、healthy threshold 2、unhealthy threshold 3 とする
- `AppRuntimeStack` は Fargate Task Definition を作成する
- `AppRuntimeStack` は Fargate Service を作成する
- task cpu は `512`、memory は `1024` とする
- container port は `8080` とする
- desiredCount は `1` とする
- autoscaling は作らない
- assignPublicIp は false とする
- app private subnet に配置する
- deployment circuit breaker は rollback enabled とする
- container image は `workops-${stage}-web:p2-3-manual` を参照する
- container log driver は awslogs とし、log group は `/workops/${stage}/web`、stream prefix は `web` とする
- container environment は `SPRING_PROFILES_ACTIVE=dev` を設定する
- `WORKOPS_DB_URL` は `/workops/${stage}/db/url` から渡す
- `WORKOPS_DB_USERNAME` と `WORKOPS_DB_PASSWORD` は `/workops/${stage}/db/master` の `username` / `password` から渡す
- Task Role は必要な Secrets Manager / SSM Parameter 読み取り権限を持つ
- Execution Role は ECR pull と CloudWatch Logs 出力に必要な権限を持つ
- `alb-sg` は TCP 80 inbound を許可する
- `app-sg` は `alb-sg` から TCP 8080 inbound を許可する
- 既存の `db-sg` への `app-sg` TCP 3306 inbound は P2-2 の定義を使う
- CDK assertion test を追加する

## 除外範囲

- `FoundationStack` の NAT Gateway 常設化
- `DataStack` 再deploy
- RDS 再確認
- Dockerfile
- Actuator 実装
- ECR push
- HTTPS listener
- HTTP to HTTPS redirect
- ACM certificate
- Route 53
- 独自ドメイン
- ALB Cognito 認証
- Cognito Hosted UI
- PLATFORM / TENANT App Client
- GitHub Actions workflow
- migration 用 ECS Task
- autoscaling
- VPC Endpoint
- Container Insights
- CloudWatch Alarm
- git commit

## 完了条件

- `EgressStack`、`EdgeStack`、`AppRuntimeStack` が存在する
- 3 Stack が `bin/cdk.ts` から常時 synth 対象になっている
- `EgressStack` が NAT Gateway を作成する
- `EdgeStack` が internet-facing ALB と HTTP listener port 80 を作成する
- target group health check が `/actuator/health`、success code `200` になっている
- `AppRuntimeStack` が Fargate Service を `desiredCount=1` で作成する
- task cpu が `512`、memory が `1024` になっている
- container port が `8080` になっている
- image tag が `p2-3-manual` になっている
- `/workops/${stage}/web` にログ出力する
- `/workops/${stage}/migration` を P2-3 runtime で使っていない
- 必要な Secrets Manager / SSM Parameter 読み取り権限が Task Role にある
- CDK assertions が代表条件を検証している

## 確認方法

エージェント確認:

```powershell
cd C:\git\workops\infra\cdk
npm run build
npm test
$env:WORKOPS_STAGE='dev'
npm run cdk -- synth
```

CDK assertions で確認する代表条件:

- `EgressStack` に NAT Gateway がある
- `EdgeStack` に internet-facing ALB がある
- `EdgeStack` に HTTP listener port 80 がある
- Target Group health check path が `/actuator/health`
- Target Group matcher が `200`
- ECS Task Definition が cpu `512` / memory `1024`
- ECS Service desired count が `1`
- ECS Service が public IP を割り当てない
- container image tag が `p2-3-manual`
- log group が `/workops/dev/web`
- `/workops/dev/migration` が AppRuntimeStack の log driver に出ない
- IAM policy に Secrets Manager / SSM Parameter 読み取りが含まれる

ユーザー確認:

- P2-3-04 で3 Stackを deploy する
- P2-3-05 で3 Stackを destroy する
