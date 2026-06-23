# P2-alpha-2-08 Verification and record

## 目的

P2-alpha-2 の新構成 CDK code、buildspec、migration image、local migration、GitHub Actions 撤去を横断確認し、P2-alpha-2 の完了判定をエージェント確認として記録する。

## ユーザー要求

- P2-alpha-2 の task 分割は、`phases.md` の責務境界を最上位単位にする
- 既にコード、Notion、`phases.md` で分かることは質問しない
- task ファイル末尾に `実装時の記録`、`実装結果`、`確認結果`、`残課題` を置く
- エージェント確認とユーザー確認を混同しない

## 前提

- P2-alpha-2-01 が完了している
- P2-alpha-2-02 が完了している
- P2-alpha-2-03 が完了している
- P2-alpha-2-04 が完了している
- P2-alpha-2-05 が完了している
- P2-alpha-2-06 が完了している
- P2-alpha-2-07 が完了している
- P2-alpha-2 では AWS deploy、Pipeline 実走、CodeConnection OAuth 認可、SNS メール受信、ManualApproval は実施しない

## 案

P2-alpha-2 の最後に次を横断確認する。

- Java test / 品質ゲートが壊れていないこと
- CDK lint / format が通ること
- 新構成 `cdk synth` 全 stack が exit 0 になること
- PipelineStack が synth できること
- MigrationStack が synth できること
- DeployStack が存在しないこと
- GitHub Actions workflow が存在しないこと
- RegistryStack が 4 repository 構成になっていること
- buildspec が Pipeline stage 方針に沿っていること
- migration image が Flyway CLI 専用であること
- apps/web 起動時 Flyway が廃止されていること
- local migration が migration image / Flyway CLI 経路で実行できること
- AppRuntimeStack が ARM_64 と ECS ネイティブ Blue/Green の構成になっていること

各 task ファイルへ実装結果、確認結果、残課題を追記する。
本 task には P2-alpha-2 全体としての最終確認結果を記録する。

## 判断メモ

- P2-alpha-2 の完了条件は deploy 成功ではなく、新構成 `cdk synth` 全 stack 成功と静的確認である。
- AWS 実環境確認は P2-alpha-3 に分離する。ユーザー資格情報や外部サービス操作が必要なため。
- 実 account ID、role ARN、credential、public IP、実 Cognito `sub`、DB 実データ値は git 管理文書に記録しない。

## 対応範囲

- P2-alpha-2 全 task の確認結果整理
- Java test / 品質ゲートの再確認
- CDK test / lint / format / synth
- local migration 確認
- Docker image build の静的確認
- GitHub Actions workflow 撤去確認
- 生成物が git 管理対象に入っていないことの確認
- 各 task ファイルの記録更新
- 本 task ファイルの実装結果・確認結果追記

## 除外範囲

- `cdk deploy`
- AWS 実リソース作成
- CodeConnection OAuth 認可
- Pipeline 実走
- SNS メール受信
- ManualApproval
- CloudFront default domain 経由の画面確認
- Cognito Hosted UI のブラウザ操作
- 主要課金 4 stack destroy

## 完了条件

- 新構成で `cdk synth` 全 stack が exit 0 となる
- `PipelineStack` が定義されている
- `MigrationStack` が定義されている
- `DeployStack` が存在しない
- GitHub Actions workflow が存在しない
- RegistryStack が 4 repository 構成になっている
- buildspec が Pipeline stage 方針に沿っている
- migration image が Flyway CLI 専用になっている
- apps/web 起動時 Flyway が廃止されている
- local migration が migration image / Flyway CLI 経路で実行できる構成になっている
- AppRuntimeStack が ARM_64 と ECS ネイティブ Blue/Green の構成になっている
- P2-alpha-2 の各 task ファイルに実装結果、確認結果、残課題が記録されている

## 確認方法

Java:

```powershell
cd C:\git\workops\apps\web
.\mvnw.cmd spotless:check
.\mvnw.cmd compile spotbugs:check
.\mvnw.cmd verify
```

local migration:

```powershell
cd C:\git\workops
docker compose -p workops-p2alpha-2-final-check build migration
docker compose -p workops-p2alpha-2-final-check up -d workops-mysql
docker compose -p workops-p2alpha-2-final-check run --rm migration
docker compose -p workops-p2alpha-2-final-check down -v
```

CDK:

```powershell
cd C:\git\workops\infra\cdk
npm run build
npm run test
npm run lint
npm run format:check
$env:WORKOPS_STAGE = "dev"
$env:AWS_PROFILE = "amazon-connect"
$env:AWS_REGION = "ap-northeast-1"
$env:AWS_DEFAULT_REGION = $env:AWS_REGION
$env:CDK_DEFAULT_ACCOUNT = (aws sts get-caller-identity --profile $env:AWS_PROFILE --region $env:AWS_REGION --query Account --output text)
$env:CDK_DEFAULT_REGION = "ap-northeast-1"
$env:WORKOPS_IMAGE_TAG = "test-sha"
$env:GITHUB_REPOSITORY = "kyiwanami/workops"
$env:WORKOPS_PIPELINE_NOTIFICATION_EMAIL = "rockwave0711@gmail.com"
npm run cdk:infra -- synth --quiet --profile $env:AWS_PROFILE
npm run cdk:runtime -- synth --quiet --profile $env:AWS_PROFILE
npm run cdk:pipeline -- synth --quiet --profile $env:AWS_PROFILE
```

生成物確認:

```powershell
cd C:\git\workops
git status --short
```

## 実装時の記録

- 2026-06-23 に P2-alpha-2-01 から P2-alpha-2-07 の task ファイル末尾を確認し、`実装結果`、`確認結果`、`残課題` が記録済みで、未完了 placeholder が残っていないことを確認した。
- AWS 接続を伴う synth は `AWS_ACCESS_KEY_ID`、`AWS_SECRET_ACCESS_KEY`、`AWS_SESSION_TOKEN` を削除し、`AWS_PROFILE=amazon-connect`、`AWS_REGION=ap-northeast-1` を明示したうえで実施した。
- `aws sts get-caller-identity --profile amazon-connect --region ap-northeast-1` で AWS account は手元の AWS 認証で確認済み。実 account ID は git 管理文書に記録しない。
- `WORKOPS_STAGE=dev`、`WORKOPS_IMAGE_TAG=test-sha`、`GITHUB_REPOSITORY=kyiwanami/workops`、`WORKOPS_PIPELINE_NOTIFICATION_EMAIL=rockwave0711@gmail.com` を設定して synth した。
- AWS deploy、Pipeline 実走、ECR push、ECS RunTask 実行、CodeConnection OAuth 認可、SNS メール受信、ManualApproval、実 Cognito ログイン確認は P2-alpha-2 では実施していない。

### 実装結果

- P2-alpha-2 最終確認 task として、Java / CDK / local migration / AWS profile 前提 synth / 静的確認の結果を本ファイルに集約した。
- `P2-alpha-2-01` から `P2-alpha-2-07` の実装記録が埋まっていることを確認した。
- `DeployStack` / `cdk-deploy.ts` / `cdk:deploy-app` は存在しないことを確認した。
- `.github/workflows` 配下に workflow file が存在しないことを確認した。
- root `db/` の `migration`、`seed/common`、`seed/local`、`seed/aws-dev` が存在することを確認した。
- `infra/docker/migration/Dockerfile`、`infra/docker/migration/entrypoint.sh`、`infra/cdk/lib/migration-stack.ts` が存在することを確認した。
- `apps/web` の Spring Boot Flyway 依存と `spring.flyway.*` 設定、および旧 `apps/web/src/main/resources/db` は残っていないことを確認した。

### 確認結果

- `cd C:\git\workops\apps\web; .\mvnw.cmd spotless:check` 成功。
- `cd C:\git\workops\apps\web; .\mvnw.cmd compile spotbugs:check` 成功。
- `cd C:\git\workops\apps\web; .\mvnw.cmd verify` 成功。`Tests run: 387, Failures: 0, Errors: 0, Skipped: 0`、Jacoco coverage check 成功。
- `cd C:\git\workops\infra\cdk; npm run build` 成功。
- `cd C:\git\workops\infra\cdk; npm run test -- --runInBand` 成功。`Test Suites: 2 passed, Tests: 22 passed`。
- `cd C:\git\workops\infra\cdk; npm run lint` 成功。
- `cd C:\git\workops\infra\cdk; npm run format:check` 成功。
- `docker compose -p workops-p2alpha-2-final-check build migration` 成功。
- `docker compose -p workops-p2alpha-2-final-check up -d workops-mysql` 成功。
- `docker compose -p workops-p2alpha-2-final-check run --rm migration` 成功。Flyway OSS Edition 12.9.0 で 8 migrations が適用され、schema は v8 になった。
- `docker compose -p workops-p2alpha-2-final-check down -v` を実行し、確認用 MySQL volume を削除した。
- `cd C:\git\workops\infra\cdk; npm run cdk:infra -- synth --quiet --profile amazon-connect` 成功。
- `cd C:\git\workops\infra\cdk; npm run cdk:runtime -- synth --quiet --profile amazon-connect` 成功。
- `cd C:\git\workops\infra\cdk; npm run cdk:pipeline -- synth --quiet --profile amazon-connect` 成功。
- 静的確認として、GitHub Actions workflow file 0 件、旧 deploy entrypoint なし、Spring Boot Flyway 経路なし、旧 app resource db なし、root DB / migration image / MigrationStack の必要 path あり、CDK 実装ソースに `WORKOPS_WEB_IMAGE_TAG` / `:latest` / `:dev` なしを確認した。

### 残課題

- P2-alpha-2 の範囲ではなし。
- AWS 実 deploy、Pipeline 実走、ECR push、ECS RunTask 実行、CodeConnection OAuth 認可、SNS メール受信、ManualApproval、実 Cognito ログイン確認は P2-alpha-3 以降の実環境確認で扱う。
