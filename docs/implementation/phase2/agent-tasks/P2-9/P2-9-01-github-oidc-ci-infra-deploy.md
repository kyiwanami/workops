# P2-9-01 GitHub OIDC / CI / infra deploy

## 目的

AWS dev 環境へ GitHub Actions から安全に接続するための OIDC deploy role を CDK 管理にし、main push / PR の CI と、非 runtime / 維持対象 Stack の自動 deploy を構成する。

P2-9 では GitHub Actions を暫定 deploy 手段として使う。Phase 2α では CodePipeline + CodeBuild へ移行し、この workflow は撤去する。

## ユーザー要求

- GitHub Actions OIDC を使う
- 長期 AWS credential を GitHub Secrets に置かない
- OIDC provider / deploy role / IAM policy は CDK の `DeployStack` で管理する
- `DeployStack` は初回だけローカル手動で deploy する
- GitHub Environment `dev` を使う
- OIDC trust policy は GitHub Environment `dev` を信頼対象にする
- branch 制御は workflow 側で `main` に限定する
- `ci.yml` は PR と main push で動かす
- docs のみの変更では CI / deploy を動かさない
- `infra-dev.yml` は `ci.yml` 成功後に起動する
- 非 runtime / 維持対象 Stack は自動 deploy する
- 課金 runtime Stack はこの task では deploy しない
- 外部 GitHub Actions は commit SHA 固定にする
- deploy job の `permissions` は job 単位で最小化する
- deploy workflow は同じ concurrency group で並列実行を防ぐ
- `cdk deploy` には P2-9 workflow では `--require-approval never` を付ける
- 実 account ID、role ARN、credential は git 管理文書に残さない

## 前提

- P2-8.5 が完了している
- `infra/cdk` は `WORKOPS_STAGE`、`AWS_REGION`、`CDK_DEFAULT_ACCOUNT`、`CDK_DEFAULT_REGION` などの環境変数で動く
- CDK app は `tryGetContext`、dotenv、`.env.local` を使わない
- `GITHUB_REPOSITORY` は `owner/repo` 形式で CDK に渡す
- `WORKOPS_STAGE` は P2-9 workflow では `dev` 固定にする
- GitHub Environment `dev` はリポジトリ側で作成する
- GitHub Environment `dev` の variables に `AWS_REGION` と `AWS_ROLE_ARN` を設定する
- `AWS_ROLE_ARN` は `DeployStack` の CloudFormation Output から取得する
- 実 `AWS_ROLE_ARN` は README や task 文書に記録しない
- 既存データ移行と後方互換性は考慮しない

## 案

### DeployStack

`infra/cdk` に `DeployStack` を追加する。

`DeployStack` は次を作成する。

| 種別 | 内容 |
| --- | --- |
| OIDC provider | GitHub Actions OIDC provider |
| deploy role | GitHub Actions から assume する WorkOps dev deploy role |
| trust policy | `repo:${GITHUB_REPOSITORY}:environment:dev` |
| IAM policy | P2-9 dev deploy 用の広め policy |
| Output | `githubActionsDeployRoleArn` |

`DeployStack` は `workops-${stage}-deploy` として作成する。`stage` は `WORKOPS_STAGE` から渡す。

`DeployStack` は GitHub Actions から更新しない。初回作成と更新は、ローカルの AWS profile で AWS account / region を確認してから手動実行する。

### IAM policy

P2-9 の deploy role は dev deploy 専用の広め policy とする。

許可対象は、P2-9 で CDK / CloudFormation が必要とする AWS サービスに限定する。

- CloudFormation
- CDK bootstrap asset 参照に必要な S3 / ECR
- IAM role / policy / PassRole
- VPC / EC2 network resources
- RDS
- ECS / ECR
- Elastic Load Balancing
- CloudFront
- Cognito
- CloudWatch Logs
- SSM Parameter Store
- Secrets Manager

完全な最小権限化は Phase 2α の CodePipeline + CodeBuild 移行時に再設計する。

### ci.yml

`.github/workflows/ci.yml` を追加する。

trigger は次に固定する。

```yaml
on:
  push:
    branches:
      - main
    paths:
      - apps/web/**
      - infra/cdk/**
      - .github/workflows/ci.yml
      - .github/workflows/infra-dev.yml
      - .github/workflows/app-deploy-dev.yml
  pull_request:
    branches:
      - main
    paths:
      - apps/web/**
      - infra/cdk/**
      - .github/workflows/ci.yml
      - .github/workflows/infra-dev.yml
      - .github/workflows/app-deploy-dev.yml
```

CI では次を実行する。

| 対象 | コマンド |
| --- | --- |
| Java | `apps/web/mvnw test` |
| Docker | `docker build -t workops-web:ci .` in `apps/web` |
| CDK dependency | `npm ci` in `infra/cdk` |
| CDK build | `npm run build` |
| CDK test | `npm test -- --runInBand` |
| CDK synth | `WORKOPS_STAGE=dev npm run cdk -- synth` |

CI では AWS OIDC を使わない。ECR push と CDK deploy もしない。

Java は `actions/setup-java` で Java 25 を使う。Node.js は `actions/setup-node` で Node 24 を使う。Maven cache と npm cache を使う。

### infra-dev.yml

`.github/workflows/infra-dev.yml` を追加する。

`infra-dev.yml` は `ci.yml` の成功後に `workflow_run` で起動する。

deploy job は次の条件をすべて満たす場合だけ実行する。

- `github.event.workflow_run.conclusion == 'success'`
- `github.event.workflow_run.event == 'push'`
- `github.event.workflow_run.head_branch == 'main'`
- changed files に `infra/cdk/**` または `.github/workflows/infra-dev.yml` が含まれる

changed files 判定には `dorny/paths-filter` を使い、action は commit SHA 固定にする。

deploy job には `environment: dev` を付ける。OIDC trust policy は GitHub Environment `dev` を前提にする。

deploy 対象は次に固定する。

```text
FoundationStack
SecretStack
ConfigStack
IdentityStack
RegistryStack
LogsStack
```

`DeployStack` は含めない。

`infra-dev.yml` では CI の検証を再実行しない。実行するのは次だけとする。

- checkout
- `npm ci`
- AWS OIDC 接続
- AWS caller identity 確認
- `cdk diff`
- `cdk deploy --require-approval never`

### GitHub Actions 共通

deploy workflow は同じ concurrency group を使う。

```yaml
concurrency:
  group: workops-dev-deploy
  cancel-in-progress: false
```

AWS OIDC を使う job だけ、次の permissions を付ける。

```yaml
permissions:
  contents: read
  id-token: write
```

CI job は `contents: read` のみにする。

外部 action はすべて commit SHA 固定にする。実装時は各 action の公式 repository / release tag から対応 commit を確認し、workflow に SHA で記録する。

## ADR

- GitHub Actions OIDC を採用する。AWS access key / secret access key の長期 credential を GitHub Secrets に置かず、短期 credential で deploy するため。
- OIDC provider / deploy role は `DeployStack` で CDK 管理する。IAM trust policy と deploy role をリポジトリ側の infrastructure 正本として扱うため。
- `DeployStack` は GitHub Actions から更新しない。workflow が自分自身の deploy role を変更して失敗した場合の復旧を避けるため。
- GitHub Environment `dev` を採用する。P2-9 は AWS dev deploy であり、将来 GitHub 側の environment protection を追加しやすくするため。
- OIDC trust policy は `repo:${GITHUB_REPOSITORY}:environment:dev` を採用する。GitHub Environment `dev` の deploy job だけが role を assume できるようにするため。
- branch 制御は workflow 側で行う。GitHub OIDC の subject を `environment:dev` にすると、AWS trust policy だけで branch と environment の両方を単純に固定しにくいため。
- `ci.yml` は PR と main push の両方で実行する。PR で壊れを検出し、main push の成功を `infra-dev.yml` の起点にするため。
- `infra-dev.yml` は main push 直接起動ではなく `workflow_run` を採用する。CI 検証を重複実行せず、CI 成功後に非 runtime Stack を deploy するため。
- `infra-dev.yml` は `infra/cdk/**` または `infra-dev.yml` 変更時だけ deploy する。`apps/web` だけの変更で維持対象 Stack を deploy しないため。
- 非 runtime / 維持対象 Stack は自動 deploy する。RDS / NAT / ECS / ALB / CloudFront のような課金 runtime を含まず、main に入った infrastructure 変更を AWS dev に反映しやすくするため。
- `DeployStack` は `infra-dev.yml` の deploy 対象に含めない。GitHub Actions が自分の OIDC role を更新する自己更新構造を避けるため。
- action は commit SHA 固定にする。外部 action の tag 差し替えリスクを避けるため。
- `--require-approval never` を P2-9 workflow では採用する。ユーザーがこの option の使用を明示したため。
- deploy role は P2-9 dev deploy 専用の広め policy とする。CDK deploy が複数 AWS サービスを CloudFormation 経由で操作するため、P2-9 で完全な最小権限化を詰め切らない。最小権限化は Phase 2α で CodePipeline + CodeBuild へ移行するときに再設計する。
- 実 account ID、role ARN、credential、public IP、実 Cognito `sub` は git 管理文書に記録しない。

## 対応範囲

- `DeployStack` 追加
- GitHub OIDC provider / deploy role / trust policy / deploy policy
- `githubActionsDeployRoleArn` Output
- `ci.yml`
- `infra-dev.yml`
- GitHub Environment `dev` の設定手順
- GitHub Environment variables `AWS_REGION` / `AWS_ROLE_ARN` の設定手順
- commit SHA 固定 action の更新手順
- README の P2-9 CI / infra deploy 手順
- CDK tests

## 除外範囲

- `app-deploy-dev.yml`
- Docker image の ECR push
- `DataStack`
- `EgressStack`
- `EdgeStack`
- `AppRuntimeStack`
- ECS service 更新
- CloudFront health check
- CodePipeline
- CodeBuild
- 本番 deploy
- main push による runtime 自動 deploy
- GitHub Actions からの `DeployStack` 更新
- 完全な IAM 最小権限化

## 完了条件

- `DeployStack` が CDK app に追加されている
- `DeployStack` が GitHub OIDC provider を作成する
- `DeployStack` が GitHub Environment `dev` 用 trust policy の deploy role を作成する
- `DeployStack` が `githubActionsDeployRoleArn` Output を持つ
- `ci.yml` が PR と main push で `apps/web/**`、`infra/cdk/**`、P2-9 workflow 変更時だけ起動する
- `ci.yml` が Java test、Docker build、CDK build / test / synth を実行する
- `ci.yml` が AWS OIDC、ECR push、CDK deploy を実行しない
- `infra-dev.yml` が `ci.yml` 成功後に起動する
- `infra-dev.yml` が main push 成功時だけ deploy する
- `infra-dev.yml` が `infra/cdk/**` または `.github/workflows/infra-dev.yml` 変更時だけ deploy する
- `infra-dev.yml` が `FoundationStack`、`SecretStack`、`ConfigStack`、`IdentityStack`、`RegistryStack`、`LogsStack` を deploy する
- `infra-dev.yml` が `DeployStack` を deploy しない
- deploy job が GitHub Environment `dev` を使う
- deploy job が job 単位で `id-token: write` を持つ
- workflow action が commit SHA 固定になっている
- README に初回 `DeployStack` 手動 deploy と GitHub Environment `dev` 設定手順が書かれている

## 確認方法

エージェント確認:

```powershell
cd C:\git\workops\infra\cdk
npm run build
npm test -- --runInBand
$env:WORKOPS_STAGE = "dev"
$env:GITHUB_REPOSITORY = "owner/repo"
npm run cdk -- synth DeployStack
npm run cdk -- synth
```

GitHub Actions YAML 確認:

- `ci.yml` に PR と main push の trigger がある
- `ci.yml` の paths に P2-9 workflow 3種が含まれる
- `infra-dev.yml` が `workflow_run` を使う
- `infra-dev.yml` が `workflow_run.conclusion`、`workflow_run.event`、`workflow_run.head_branch` を確認する
- `infra-dev.yml` が changed files を見て `infra/cdk/**` と `infra-dev.yml` のみ deploy 対象にする
- deploy job に `environment: dev` がある
- AWS OIDC job だけに `id-token: write` がある
- action が commit SHA 固定になっている

ユーザー確認:

```powershell
cd C:\git\workops\infra\cdk
Remove-Item Env:AWS_ACCESS_KEY_ID -ErrorAction SilentlyContinue
Remove-Item Env:AWS_SECRET_ACCESS_KEY -ErrorAction SilentlyContinue
Remove-Item Env:AWS_SESSION_TOKEN -ErrorAction SilentlyContinue

$env:WORKOPS_STAGE = "dev"
$env:AWS_PROFILE = "<aws-profile>"
$env:AWS_SDK_LOAD_CONFIG = "1"
$env:AWS_REGION = "<aws-region>"
$env:AWS_DEFAULT_REGION = $env:AWS_REGION
$env:CDK_DEFAULT_ACCOUNT = (aws sts get-caller-identity --region $env:AWS_REGION --query Account --output text)
$env:CDK_DEFAULT_REGION = $env:AWS_REGION
$env:GITHUB_REPOSITORY = "<owner>/<repo>"

aws sts get-caller-identity --region $env:AWS_REGION
npm run cdk -- diff DeployStack
npm run cdk -- deploy DeployStack
```

`DeployStack` deploy 後、CloudFormation Output から role ARN を取得し、GitHub Environment `dev` の variable `AWS_ROLE_ARN` に設定する。`AWS_REGION` も GitHub Environment `dev` の variable に設定する。

実 account ID、role ARN、SSO role ARN、SSO user、credential は README や task 文書に記録しない。
