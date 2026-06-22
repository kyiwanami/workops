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
docker compose up -d workops-mysql
docker compose run --rm migration
```

CDK:

```powershell
cd C:\git\workops\infra\cdk
npm run test
npm run lint
npm run format:check
$env:WORKOPS_STAGE = "dev"
$env:CDK_DEFAULT_ACCOUNT = "000000000000"
$env:CDK_DEFAULT_REGION = "ap-northeast-1"
npm run cdk:infra -- synth
npm run cdk:runtime -- synth
npm run cdk:pipeline -- synth
```

生成物確認:

```powershell
cd C:\git\workops
git status --short
```

## 実装時の記録

### 実装結果

- 未実装。

### 確認結果

- 未確認。

### 残課題

- 未整理。
