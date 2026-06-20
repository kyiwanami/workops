# P2-9-02 app deploy / runtime verification

## 目的

GitHub Actions の手動 workflow から、AWS dev の課金 runtime / DB Stack を明示的に deploy し、Docker image の ECR push、ECS 反映、CloudFront HTTPS health check まで再現可能にする。

P2-9 の app deploy は暫定 deploy 手段である。Phase 2α では CodePipeline + CodeBuild へ移行し、この workflow は撤去する。

## ユーザー要求

- `app-deploy-dev.yml` は手動実行にする
- 課金 runtime Stack は自動 deploy しない
- 手動実行時に課金 runtime deploy の確認 input を付ける
- GitHub Environment `dev` を使う
- OIDC trust policy は GitHub Environment `dev` を信頼対象にする
- branch 制御は workflow 側で `main` に限定する
- CI 成功後に手動 deploy する運用にする
- deploy workflow では Java test / CDK build / CDK test / CDK synth を再実行しない
- Docker build は deploy workflow 内で実行する
- Docker image tag は `github.sha` のみを使う
- `dev` tag と `latest` tag は使わない
- ECR push を行う
- `AppRuntimeStack` の image tag は `WORKOPS_WEB_IMAGE_TAG` 環境変数で CDK に渡す
- deploy 順序は `DataStack → EgressStack → EdgeStack → AppRuntimeStack` にする
- deploy 前に `cdk diff` を残す
- `cdk deploy` には P2-9 workflow では `--require-approval never` を付ける
- deploy 後に ECS service stable を確認する
- deploy 後に CloudFront HTTPS `/actuator/health` を bounded retry で確認する
- Flyway 成功確認は CloudWatch Logs の文字列検索ではなく、ECS stable と health check で扱う
- docs のみ変更では手動 deploy しないことを README に書く
- 実 account ID、role ARN、credential、CloudFront domain の実値は git 管理文書に残さない

## 前提

- P2-9-01 が完了している
- `DeployStack` が deploy 済みである
- GitHub Environment `dev` が存在する
- GitHub Environment `dev` の variables に `AWS_REGION` と `AWS_ROLE_ARN` が設定されている
- `ci.yml` が対象 commit で成功している
- `app-deploy-dev.yml` は `main` branch から実行する
- `infra-dev.yml` により非 runtime / 維持対象 Stack が deploy されている
- `RegistryStack` に ECR repository `workops-${stage}-web` が存在する
- `AppRuntimeStack` は `WORKOPS_WEB_IMAGE_TAG` 環境変数から ECR image tag を参照するように変更する
- `WORKOPS_STAGE` は P2-9 workflow では `dev` 固定にする
- 既存データ移行と後方互換性は考慮しない

## 案

### app-deploy-dev.yml

`.github/workflows/app-deploy-dev.yml` を追加する。

trigger は `workflow_dispatch` のみにする。

```yaml
on:
  workflow_dispatch:
    inputs:
      confirm_runtime_deploy:
        description: Deploy paid AWS dev runtime stacks
        required: true
        type: boolean
```

deploy job は次の条件をすべて満たす場合だけ実行する。

- `github.ref == 'refs/heads/main'`
- `inputs.confirm_runtime_deploy == true`

deploy job には `environment: dev` を付ける。

workflow 共通の `env` は次に固定する。

```yaml
env:
  WORKOPS_STAGE: dev
  WORKOPS_WEB_IMAGE_TAG: ${{ github.sha }}
```

deploy workflow では CI 検証を再実行しない。`ci.yml` 成功後に手動実行する運用を README に明記する。

ただし、ECR に push する image artifact を作るため、Docker build は deploy workflow 内で実行する。

### Docker build / ECR push

`apps/web` で Docker image を build し、ECR repository `workops-dev-web` に `${{ github.sha }}` tag で push する。

`dev` tag と `latest` tag は作らない。

ECR login は `aws-actions/amazon-ecr-login@d539f0932e70871a027e9d5a9d8fc38589180a64` を使う。action は commit SHA 固定にする。

### CDK deploy

`app-deploy-dev.yml` は `infra/cdk` で `npm ci` を実行する。これは検証ではなく、`cdk diff` / `cdk deploy` を実行するための依存復元である。

deploy 前に次の Stack の `cdk diff` を実行する。

```text
DataStack
EgressStack
EdgeStack
AppRuntimeStack
```

deploy は次の順序で明示的に実行する。

```text
DataStack
EgressStack
EdgeStack
AppRuntimeStack
```

各 deploy には `--require-approval never` を付ける。

`AppRuntimeStack` は `WORKOPS_WEB_IMAGE_TAG` の値を使って ECR image tag を参照する。環境変数が空の場合は warning を出し、`AppRuntimeStack` を CDK app に定義せず、`AppRuntimeStack` を指定した CDK synth / deploy を失敗させる。

### ECS stable / CloudFront health

`AppRuntimeStack` deploy 後、AWS CLI で ECS service stable を待つ。

```powershell
aws ecs wait services-stable `
  --cluster workops-dev-cluster `
  --services workops-dev-web `
  --region $env:AWS_REGION
```

その後、CloudFormation Output から `cloudFrontHttpsUrl` を取得し、`$cloudFrontHttpsUrl/actuator/health` が HTTP 200 を返すことを確認する。

CloudFront / ALB / ECS 反映直後の揺れを考慮し、health check は bounded retry にする。最大回数と間隔は workflow 内で固定し、無限 retry はしない。

推奨値:

```text
interval: 30 seconds
max attempts: 10
```

Flyway は WorkOps Spring Boot 起動時に実行される。起動時 Flyway が失敗した場合、ECS task は安定せず、CloudFront health も成功しない。そのため P2-9 workflow では CloudWatch Logs の Flyway 文言検索は行わない。

## ADR

- `app-deploy-dev.yml` は `workflow_dispatch` を採用する。RDS、NAT Gateway、ECS、ALB、CloudFront など課金 runtime を main push で自動作成しないため。
- 課金 runtime deploy の確認 input を採用する。手動 workflow の押し間違いで課金 runtime を起動することを避けるため。
- GitHub Environment `dev` を採用する。AWS dev deploy の環境境界を GitHub 側にも作り、将来 environment protection を追加しやすくするため。
- branch 制御は workflow 側で `main` に限定する。OIDC trust policy は GitHub Environment `dev` を subject にするため、branch は workflow の条件で制御する。
- deploy workflow では Java test / CDK build / CDK test / CDK synth を再実行しない。`ci.yml` の責務と deploy workflow の責務を分け、重複検証を避けるため。
- Docker build は deploy workflow 内で実行する。ECR に push する image artifact は deploy workflow の成果物であり、CI の Docker build は検証用で別物だからである。
- Docker image tag は `github.sha` のみを採用する。ECS に反映した commit を追跡しやすくし、`dev` や `latest` の可変 tag を避けるため。
- `WORKOPS_WEB_IMAGE_TAG` 環境変数で `AppRuntimeStack` へ image tag を渡す。既存 CDK app が環境変数で実行時値を受け取る方針に合うため。
- deploy 順序は `DataStack → EgressStack → EdgeStack → AppRuntimeStack` に固定する。DB secret / SSM、NAT、CloudFront / ALB、ECS runtime の依存順に合わせるため。
- `--require-approval never` を P2-9 workflow では採用する。ユーザーがこの option の使用を明示したため。
- ECS stable と CloudFront `/actuator/health` で起動時 Flyway 失敗なしを確認する。Flyway ログ文言検索はログフォーマットやバージョン差分に workflow が依存するため採用しない。
- CloudFront health check は bounded retry にする。CloudFront / ALB / ECS の反映直後に一時失敗する可能性があるため。
- `app-deploy-dev.yml` は path filter を持たせない。手動 deploy は人が明示的に実行する復旧・再 deploy の入口でもあり、changed files 判定で止めると運用上の邪魔になるため。
- docs のみ変更では手動 deploy しない。これは workflow の path filter ではなく README の運用ルールとして明記する。
- 実 account ID、role ARN、CloudFront domain、credential、実 Cognito `sub` は git 管理文書に記録しない。

## 対応範囲

- `app-deploy-dev.yml`
- Docker build
- ECR login / push
- `WORKOPS_WEB_IMAGE_TAG`
- `AppRuntimeStack` の ECR image tag 参照変更
- runtime Stack の `cdk diff`
- `DataStack` deploy
- `EgressStack` deploy
- `EdgeStack` deploy
- `AppRuntimeStack` deploy
- ECS service stable wait
- CloudFront HTTPS health retry
- README の app deploy 手順
- README の CI 成功後 deploy 運用
- README の課金 runtime 注意
- CDK tests

## 除外範囲

- `DeployStack`
- GitHub OIDC provider / deploy role
- `ci.yml`
- `infra-dev.yml`
- main push による runtime 自動 deploy
- CodePipeline
- CodeBuild
- Blue/Green deploy
- Canary deploy
- 複雑な承認フロー
- 本番 deploy
- 独立 Flyway CLI
- Maven plugin による migration 実行
- migration 用 ECS Task
- CloudWatch Logs の Flyway 文言検索
- PR からの deploy

## 完了条件

- `app-deploy-dev.yml` が `workflow_dispatch` のみで起動する
- `app-deploy-dev.yml` に `confirm_runtime_deploy` input がある
- `app-deploy-dev.yml` が `main` branch 以外では deploy しない
- deploy job に `environment: dev` がある
- deploy job が GitHub OIDC で AWS role を assume する
- deploy job が `WORKOPS_STAGE=dev` を使う
- deploy job が `WORKOPS_WEB_IMAGE_TAG=${{ github.sha }}` を使う
- deploy job が Java test / CDK build / CDK test / CDK synth を再実行しない
- deploy job が `apps/web` の Docker image を build する
- deploy job が ECR repository `workops-dev-web` に `${{ github.sha }}` tag を push する
- `dev` tag と `latest` tag を push しない
- `AppRuntimeStack` が `WORKOPS_WEB_IMAGE_TAG` の image tag を参照する
- `WORKOPS_WEB_IMAGE_TAG` が空の場合、`AppRuntimeStack` 指定の CDK 実行が失敗する
- `DataStack`、`EgressStack`、`EdgeStack`、`AppRuntimeStack` の `cdk diff` が deploy 前に実行される
- `DataStack → EgressStack → EdgeStack → AppRuntimeStack` の順に deploy される
- 各 `cdk deploy` に `--require-approval never` が付く
- `AppRuntimeStack` deploy 後に ECS service stable wait を実行する
- CloudFront HTTPS `/actuator/health` を bounded retry で確認する
- CloudFront health check が最後まで失敗した場合、workflow が失敗する
- README に CI 成功後に app deploy を手動実行することが書かれている
- README に docs のみ変更では app deploy しないことが書かれている
- README に実値を記録しない注意が書かれている

## 確認方法

エージェント確認:

```powershell
cd C:\git\workops\infra\cdk
npm run build
npm test -- --runInBand
$env:WORKOPS_STAGE = "dev"
$env:GITHUB_REPOSITORY = "owner/repo"
$env:WORKOPS_WEB_IMAGE_TAG = "test-sha"
npm run cdk -- synth AppRuntimeStack
```

Docker build 確認:

```powershell
cd C:\git\workops\apps\web
docker build -t workops-web:test-sha .
```

GitHub Actions YAML 確認:

- `app-deploy-dev.yml` が `workflow_dispatch` のみを持つ
- `confirm_runtime_deploy` input がある
- deploy job が `github.ref == 'refs/heads/main'` と input を確認する
- deploy job に `environment: dev` がある
- deploy job に `id-token: write` がある
- deploy job が `WORKOPS_WEB_IMAGE_TAG` に `${{ github.sha }}` を設定する
- deploy job が `dev` / `latest` tag を push しない
- deploy job が Java test / CDK build / CDK test / CDK synth を実行しない
- deploy job が `cdk diff` を実行する
- deploy job が Stack を明示順に deploy する
- deploy job が ECS stable wait を実行する
- deploy job が CloudFront health retry を実行する
- action が commit SHA 固定になっている

ユーザー確認:

- GitHub Actions の `ci.yml` が対象 commit で成功している
- GitHub Actions の `app-deploy-dev.yml` を main branch から実行できる
- `confirm_runtime_deploy` を true にした場合だけ deploy job が進む
- ECR repository `workops-dev-web` に commit SHA tag の image がある
- CloudFormation Stack `workops-dev-data` が作成または更新されている
- CloudFormation Stack `workops-dev-egress` が作成または更新されている
- CloudFormation Stack `workops-dev-edge` が作成または更新されている
- CloudFormation Stack `workops-dev-app-runtime` が作成または更新されている
- ECS Service `workops-dev-web` が stable である
- CloudFront HTTPS `/actuator/health` が HTTP 200 を返す

AWS dev 実確認はユーザー確認として扱い、エージェント確認だけでは P2-9 完了扱いにしない。
