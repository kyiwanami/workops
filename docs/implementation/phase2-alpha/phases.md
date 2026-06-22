# Phase 2α Implementation Phases

## 前提

Phase 2α のスコープ、設計方針、実装順序、完了条件は、Notion の「Phase 2α 実装計画」「要件定義」「基本設計」「スコープ管理」を正とします。
ADR 本文はこのファイルの作成時に参照しません。

このファイルは、Notion で合意済みの Phase 2α を、コーディングエージェントへ渡せるリポジトリ側の親計画へ整理するものです。
このファイルでは、Phase 2α のスコープや設計判断を変更しません。

リポジトリ側では、次を管理します。

- コード
- CDK
- Dockerfile
- Flyway migration と seed
- buildspec
- 品質ゲート設定
- compose 設定
- 実装指示 Markdown
- README
- テスト
- 実装結果
- 確認結果

具体的な agent task 分割は、P2-alpha-0 で作成した各 Phase 2α 小フェーズ用ディレクトリの中で、各小フェーズの作業として行います。
ユーザーが分割案を確認し、合意した後にだけ、その小フェーズ配下へ詳細な agent task Markdown を作成します。
このファイルでは、`P2-alpha-1-01` のような具体タスク名、詳細タスク数、対応ファイル一覧を固定しません。

PR は作業・レビューの単位であり、Phase 2α の完了条件ではありません。
Phase 2α-1、Phase 2α-2、Phase 2α-3 は、各フェーズに定義した検証、確認、運用条件を満たした時点で完了とします。
PR 作成、PR merge、commit 作成だけで完了扱いにしません。

## 案

- Phase 2α は、Phase 2 完了後に GitHub Actions OIDC 暫定 deploy を撤去し、CodePipeline V2 + CodeBuild + CDK Pipelines による AWS ネイティブ CI/CD へ移行する
- Phase 2α では、新規業務機能を追加しない
- Phase 2α の主対象は、CI/CD、実行基盤、migration 実行方式、品質ゲート、通知、Stack 構造の再構成とする
- Phase 2α-1 は、AWS リソースを作成・変更せず、Java と CDK TypeScript のコードベースを品質ゲート pass 状態に揃える
- Phase 2α-2 は、deploy を伴わず、PipelineStack、MigrationStack、RegistryStack、AppRuntimeStack、EdgeStack、Buildspec、品質ゲート、通知、GitHub Actions 撤去を含む新構成 CDK code を完成させる
- Phase 2α-3 は、CDK code 作業を行わず、初回 deploy、CodeConnection 認可、Pipeline 自走確認、動作確認 checklist、ベースライン記録、主要課金 4 stack destroy を行う
- Phase 2α 完了は、Phase 2α-3 の動作確認 checklist 全項目達成と、主要課金 4 stack destroy 済みを条件とする
- 数値 SLO は採用しない。Pipeline 実行時間などの値は記録のみ行い、合格判定には使わない

## ADR

ADR 本文は参照しません。
本計画では、Notion の Phase 2α 実装計画、要件定義、基本設計、スコープ管理で確定済みの次の判断を前提にします。

- GitHub Actions OIDC 暫定 deploy は Phase 2α で撤去する
- CI/CD は CodePipeline V2 + CodeBuild + CDK Pipelines に移行する
- CDK Pipelines は `selfMutation: true` とする
- Pipeline は単一 Pipeline とし、ManualApproval を含める
- GitHub は CodeConnections で CodePipeline に接続する
- GitHub Actions workflow は PR チェック用途にも残さない
- DeployStack は廃止し、同じ責務を PipelineStack に移す
- PipelineStack を新設する
- MigrationStack を新設する
- CodeDeploy は使わない
- ECS ネイティブ Blue/Green Deployment を採用する
- apps/web の ECS Task は ARM_64 + Graviton 上で稼働させる
- apps/web 起動時 Flyway 実行を廃止し、Flyway 専用 image + ECS RunTask による独立 migration stage へ移行する
- 品質ゲートは FAIL mode とし、WARNING 期間を設けない
- 品質ゲートの閾値は業界標準値を一括採用し、コードベース調査による up/down 調整を行わない
- Pipeline の承認待ちと全体失敗を CodeStar Notifications + SNS + メールで通知する
- 主要課金 4 stack は `EgressStack`、`DataStack`、`EdgeStack`、`AppRuntimeStack` とする
- `PipelineStack`、`MigrationStack`、`FoundationStack`、`ConfigStack`、`RegistryStack`、`LogsStack`、`SecretStack`、Artifact bucket、ECR repository、CodeConnection は維持対象とし、主要課金 4 stack destroy 対象に含めない

## Phase 2α 完了定義

Phase 2α は、Phase 2 で構築した AWS dev 環境の業務機能と基本トポロジーを維持したまま、次の状態になった時点で完了とします。

- GitHub Actions workflow が撤去されている
- DeployStack が撤去されている
- CodePipeline V2 + CodeBuild + CDK Pipelines による単一 Pipeline が構成されている
- `main` push を契機に Pipeline が起動できる
- ManualApproval 後に RegistryStack が deploy され、ECR repository 4 本が作成・更新される
- Web image と migration image が並列 build され、commit SHA tag で ECR に push される
- Flyway 専用 migration image が ECS RunTask として実行され、exit code 0 で migration 成功を判定できる
- AppRuntimeStack が ECS ネイティブ Blue/Green Deployment で切り替えできる
- apps/web の ECS Task が ARM_64 で起動している
- Pipeline の承認待ちと全体失敗が SNS メール通知される
- Phase 2α-3 の動作確認 checklist が全項目達成されている
- ベースライン値が記録されている
- 確認完了後、主要課金 4 stack が destroy 済みである

## Notion へ戻す判断

次の判断は、コーディングエージェントがリポジトリ側だけで確定しません。

- Phase 2α の完了定義を変更する
- Phase 2α-1、Phase 2α-2、Phase 2α-3 の境界を変更する
- 新規業務機能を Phase 2α に追加する
- apps/api または apps/batch の stage を Phase 2α に追加する
- preview、staging、prod、cross-account 構成を Phase 2α に追加する
- CodePipeline V2 + CodeBuild + CDK Pipelines 以外を CI/CD の正本にする
- GitHub Actions workflow を残す
- CodeDeploy を採用する
- ECS ネイティブ Blue/Green Deployment を採用しない
- apps/web の ECS Task を ARM_64 + Graviton へ移行しない
- Flyway CLI 以外を migration 実行方式の正本にする
- apps/web 起動時 Flyway 実行を残す
- migration 専用 DB user、migration 専用 DB secret、migration 専用 Security Group を追加する
- VPC Endpoint を Phase 2α に追加する
- 品質ゲート対象ツールまたは閾値を変更する
- 品質ゲートを WARNING mode にする
- suppression、ignore、JaCoCo 除外を広く使う方針に変える
- Pipeline stage 順序を変更する
- 主要課金 4 stack の destroy 戦略を変更する
- 実 account ID、role ARN、credential、public IP、実 Cognito `sub`、DB 実データ値を git 管理文書に記録する必要が出る

該当する場合は、実装を進める前に判断内容、影響範囲、選択肢をユーザーへ提示し、Notion 側の更新要否を確認します。

## AWS 実環境確認の分担

コーディングエージェントは、リポジトリ内の実装、静的確認、ビルド、テスト、CDK synth、生成 template 確認、buildspec 構文確認、実行手順と確認コマンドの作成を担当します。

AWS アカウントへの認証、課金対象リソースの作成、CodeConnection の GitHub OAuth 認可、SNS メール購読確認、ManualApproval 操作、CloudFront domain 経由の最終画面確認、Cognito Hosted UI のブラウザ操作など、ユーザーの資格情報または外部サービス操作が必要な確認はユーザー確認とします。
ユーザーが実行を明示的に依頼し、利用可能な認証状態が確認できる場合は、コーディングエージェントが操作を補助できます。

各フェーズの完了時には、次を分けて記録します。

- エージェント確認: ビルド、テスト、静的確認、生成物確認、コマンド結果
- ユーザー確認: AWS dev、CodePipeline、CodeConnection、SNS、ManualApproval、Cognito、画面操作などの実環境結果
- 未確認事項: 実行できなかった理由と、ユーザーが実行する具体的な手順

エージェント確認とユーザー確認を混同しません。
ユーザー確認が完了条件に含まれる場合、エージェント確認だけではフェーズ完了扱いにしません。

## Phase 2α 共通実装方針

- AWS リソースは `infra/cdk` の AWS CDK v2 で管理する
- CDK の Stack クラス名に `dev` / `stg` / `prod` を固定しない
- CloudFormation の物理スタック名は `stage` props から組み立てる
- Stack 間参照は、同一 CDK app 内の props 参照を基本にする
- CloudFormation Export や SSM Parameter を Stack 間参照の正本にしない
- CDK 管理リソースの共通タグは `Project=WorkOps`、`Environment={stage}`、`ManagedBy=CDK` とする
- タグに機密情報、個人情報、account ID、credential、実 Cognito `sub`、DB 値を入れない
- RunTask 実行時に追加の user tag や `startedBy` 運用は導入しない
- Pipeline 実行との対応は、CodeBuild logs、ECS taskArn、CloudWatch Logs stream、commit SHA image tag で追跡する
- CodeBuild image は `aws/codebuild/amazonlinux-aarch64-standard:3.0` を使う
- CodeBuild compute は ARM_64 / Graviton 系の Medium とする
- Docker build と Testcontainers のため CodeBuild は `privileged: true` とする
- CodeBuild VPC mode は使わない
- VPC Endpoint は作らない
- ECR image tag は commit SHA tag のみを使う
- `latest`、`dev`、phase 名 tag、manual tag は使わない
- Web image と migration image は同じ commit SHA tag を使う
- Phase 2 の `WORKOPS_WEB_IMAGE_TAG` 単独運用は廃止する
- Pipeline では `webImageTag` と `migrationImageTag` を明示的に扱い、どちらも同じ commit SHA を渡す
- ローカル synth / test 用の image tag は `test-sha` のような明示値を使う
- 本 image repository は immutable tag、commit SHA tag、最新 10 個保持とする
- cache repository は mutable tag、`buildcache` tag 上書き、最新 5 個保持とする
- Docker buildx registry cache は `--cache-from` と `--cache-to mode=max` を常に使う
- 初回 cache miss 用の事前存在確認、分岐、特別処理は作らない
- buildx builder は CodeBuild の各 Build Images project で `docker-container` driver を作成して使う
- ECR Enhanced Scanning は ECR 側設定として扱い、Pipeline gate にはしない
- Build Images stage の合否判定は Trivy image scan と docker push 結果で行う
- Trivy は image scan のみに限定する
- Trivy は HIGH / CRITICAL で fail する
- Trivy DB cache 専用の永続 cache は作らない
- CycloneDX SBOM は生成成功を gate とし、生成物は git 管理しない
- OWASP Dependency-Check は CVSS 7 以上で fail する
- NVD API key は Phase 2α-1 の必須条件にしない
- Dependency-Check suppression file は初期作成しない
- suppression、ignore、JaCoCo 除外、cdk-nag suppression、SpotBugs suppression は最終手段とする
- 抑制が必要な場合は、最小範囲、理由、根拠、再検討条件を記録する
- wildcard 的な一括 suppression、一括 ignore、一括除外は禁止する
- 品質ゲート未達は、原則として依存更新、テスト追加、実装修正で解消する
- OWASP Dependency-Check の脆弱性は依存更新で解消できるものを suppression しない
- OWASP Dependency-Check suppression は、HTML report などで false positive、到達不能、修正版なしを確認した場合だけ追加する
- OWASP Dependency-Check suppression を追加する場合は `apps/web/config/dependency-check-suppressions.xml` に置き、CVE、CPE、GAV などの最小範囲と notes を記録する
- Trivy の HIGH / CRITICAL は、まずベース image 更新または同等要件を満たす公式系 image への切替で解消する
- `.trivyignore` は修正版なし、到達不能、誤検知を説明できる場合だけ使う
- SpotBugs / FindSecBugs は、まず実装修正で解消する
- `@SuppressFBWarnings` や exclude filter は誤検知、フレームワーク都合、テストコード特有の制約など、実装修正が不自然になる場合だけ最小範囲で使う
- セキュリティ系警告は suppression 前に必ず実装修正で解消できないか確認する
- cdk-nag suppression は、dev ポートフォリオ用途として意図を説明でき、代替実装が過剰設計、過剰コスト、または今回のスコープ外になる場合だけ許可する
- cdk-nag suppression は rule ID 単位の最小範囲に限定し、理由と将来再検討条件を残す
- JaCoCo は line coverage 80%、branch coverage 70% とする
- JaCoCo 除外は最終手段とし、除外で閾値達成を狙わない
- Spotless は Java のみを対象にし、google-java-format を使う
- ESLint / Prettier は `infra/cdk` の TypeScript を対象にする
- `infra/cdk` の生成物、`node_modules`、`cdk.out`、`cdk.out.*`、coverage、dist は ESLint / Prettier 対象から除外する
- GitHub Actions workflow は Phase 2α-2 で撤去する
- `.github/workflows` 配下の `.gitkeep` も残さない

## Flyway / DB migration 方針

Phase 2α では、Flyway 実行方式を環境を問わず CLI に統一します。

- AWS dev では、ECS RunTask が migration image 内の Flyway CLI を実行する
- local では、同じ migration image を `docker compose run --rm migration` で実行し、Docker Compose MySQL に対して Flyway CLI を実行する
- apps/web 起動時 Flyway は廃止する
- Spring Boot Flyway AutoConfiguration を migration 実行経路として使わない
- Maven Flyway plugin を migration 実行経路として使わない
- apps/web から Spring Boot Flyway 実行依存を削除する
- Flyway CLI と MySQL driver は migration image 側に閉じ込める

SQL 正本は root `db/` に移します。

```text
db/
  migration/
  seed/
    common/
    local/
    aws-dev/
```

migration image には、次をすべて含めます。

- `db/migration`
- `db/seed/common`
- `db/seed/local`
- `db/seed/aws-dev`

実行時の locations だけを環境ごとに切り替えます。

```text
local:
  filesystem:/flyway/sql/migration
  filesystem:/flyway/sql/seed/common
  filesystem:/flyway/sql/seed/local

aws-dev:
  filesystem:/flyway/sql/migration
  filesystem:/flyway/sql/seed/common
  filesystem:/flyway/sql/seed/aws-dev
```

migration container に注入する DB 接続環境変数は、Web と揃えます。

- `WORKOPS_DB_URL`
- `WORKOPS_DB_USERNAME`
- `WORKOPS_DB_PASSWORD`
- `WORKOPS_FLYWAY_LOCATIONS`

Flyway CLI は entrypoint 内で `WORKOPS_DB_*` を `flyway -url`、`-user`、`-password` に渡して実行します。
`FLYWAY_USER`、`FLYWAY_PASSWORD` は使いません。

Migration RunTask のネットワーク経路は Web ECS Task と同じにします。

- app private subnet で起動する
- security group は `app-sg` を使う
- RDS への接続は既存の `db-sg` ingress from `app-sg`:3306 を使う
- Migration 専用 Security Group は作らない
- DB Security Group の追加 ingress は作らない
- CloudShell 経路、別 subnet、NAT 経由の DB 接続は使わない
- ECR image pull、Secrets Manager、SSM Parameter Store、CloudWatch Logs への通信は、Phase 2 と同じく `EgressStack` の NAT Gateway 経由とする

Migration RunTask stage は、RunTask 起動だけで完了扱いにしません。
CodeBuild buildspec 内の bash + AWS CLI で次を実行します。

- `aws ecs run-task` で migration task を起動する
- `run-task` の `failures` が空であることを確認する
- taskArn を取得する
- `aws ecs wait tasks-stopped` で停止まで待機する
- `aws ecs describe-tasks` で `stopCode`、`stoppedReason`、`containers[].exitCode`、`containers[].reason` を確認する
- migration container の `exitCode` が 0 の場合だけ成功とする
- `exitCode` が 0 以外、`exitCode` が取れない、`run-task` failures がある、stopCode が異常系の場合は CodeBuild step を失敗させる

## Pipeline stage 方針

Phase 2α の Pipeline stage は次の順序に固定します。

1. Source
2. Synth
3. Build & Test
4. ManualApproval
5. Deploy Registry
6. Build Images
7. Deploy Data / Network / Migration
8. Migration RunTask
9. Deploy AppRuntime

### Source

CodeConnections で GitHub の `main` branch を取得します。
file path filter は使いません。
変更がない Stack は CloudFormation no-op に任せます。

### Synth

CDK Pipelines の synth を実行します。
`cdk synth` と cdk-nag を実行します。

### Build & Test

Java と CDK TypeScript の品質ゲートを FAIL mode で実行します。
2α-1 で整えたローカル用 command / npm script / Maven goal と同じものを CodeBuild でも使います。

### ManualApproval

Build Images の前に ManualApproval を置きます。
承認待ちは CodeStar Notifications + SNS + メールで通知します。
承認期限は default 7 days とします。

### Deploy Registry

ManualApproval 後に RegistryStack を deploy します。
Build Images の前提として、次の 4 repository を作成・更新します。

- `workops-${stage}-web`
- `workops-${stage}-migration`
- `workops-${stage}-web-cache`
- `workops-${stage}-migration-cache`

Build Images stage 前に ECR repository の個別存在確認 step は置きません。
RegistryStack deploy が失敗すれば Deploy Registry stage で停止し、Build Images stage で push できなければ Build Images stage が失敗します。

### Build Images

Web image と migration image を別々の CodeBuild project に分け、並列実行します。
1 つの buildspec にまとめません。

- BuildWebImage
- BuildMigrationImage

両方とも同じ commit SHA tag で push します。
Web image tag と migration image tag は Pipeline 内で同じ commit SHA として扱います。
既存の `WORKOPS_WEB_IMAGE_TAG` 単独運用は廃止します。

```text
workops-${stage}-web:${commitSha}
workops-${stage}-migration:${commitSha}
```

cache tag は `buildcache` に固定します。

```text
workops-${stage}-web-cache:buildcache
workops-${stage}-migration-cache:buildcache
```

### Deploy Data / Network / Migration

DataStack、EgressStack、EdgeStack、MigrationStack を deploy します。
MigrationStack は維持対象ですが、migration RunTask 実行に必要な Task Definition をこの stage で確定します。

### Migration RunTask

MigrationStack の Task Definition を ECS RunTask として起動し、Flyway CLI を実行します。
RunTask の終了まで待機し、exit code 0 の場合だけ次へ進みます。

### Deploy AppRuntime

AppRuntimeStack を deploy し、ECS ネイティブ Blue/Green Deployment で切り替えます。
CodeDeploy は使いません。
apps/web の ECS Task は ARM_64 で起動します。

## P2-alpha-0. Phase 2α 設計・リポジトリ準備

### 目的

Notion で確定した Phase 2α の範囲、順序、完了条件、責務境界を、リポジトリ側で実装へ渡せる状態にします。
P2-alpha-0 では、親計画である `phases.md` と、後続小フェーズごとの agent task 置き場だけを用意します。
詳細な agent task Markdown の作成は P2-alpha-0 では行わず、P2-alpha-1、P2-alpha-2、P2-alpha-3 の各フェーズ内で行います。

### 前提フェーズ

- Phase 2 が完了している
- Notion の Phase 2α 実装計画、要件定義、基本設計、スコープ管理が確定している
- Notion の Pipeline stage 順序に Deploy Registry が追加済みである

### ユーザー要件

- P2-alpha-0 の完了条件は、`docs/implementation/phase2-alpha/agent-tasks/P2-alpha-1/`、`docs/implementation/phase2-alpha/agent-tasks/P2-alpha-2/`、`docs/implementation/phase2-alpha/agent-tasks/P2-alpha-3/` のフォルダ分けができていることを含む
- 各フェーズ内の詳細なファイル分けは、そのフェーズの作業として行う
- P2-alpha-0 では詳細な agent task Markdown を作らない

### 成果物

- `docs/implementation/phase2-alpha/phases.md`
- Phase 2α の実装順序
- Phase 2α のリポジトリ側責務
- AWS 実環境確認の分担
- Notion へ戻す判断条件
- agent task 作成ルール
- `docs/implementation/phase2-alpha/agent-tasks/P2-alpha-1/`
- `docs/implementation/phase2-alpha/agent-tasks/P2-alpha-2/`
- `docs/implementation/phase2-alpha/agent-tasks/P2-alpha-3/`
- 上記ディレクトリを git 管理対象に残すための `.gitkeep`

### 実装方針

- Notion の内容をコード詳細へ展開するための入口文書として管理する
- Notion の文章をそのまま複製せず、実装順序、依存関係、成果物、確認方法をリポジトリ向けに整理する
- P2-alpha-0 では、後続フェーズの作業場所として `agent-tasks/P2-alpha-1/`、`agent-tasks/P2-alpha-2/`、`agent-tasks/P2-alpha-3/` を作成する
- 詳細 agent task Markdown は対象フェーズの中で、着手時点の Notion とコードベースを確認してから作成する
- `.gitkeep` はディレクトリを git 管理対象に残すためだけに置き、詳細 task Markdown とは扱わない
- Phase 2α-1 以降の実装を P2-alpha-0 に含めない

### 除外範囲

- CDK 実装
- Dockerfile
- Flyway SQL 移動
- buildspec
- 品質ゲート設定
- AWS リソース作成
- 詳細 agent task Markdown
- P2-alpha-1、P2-alpha-2、P2-alpha-3 の詳細なファイル分け

### 完了条件

- Phase 2α の目的、前提、成果物、実装方針、除外範囲、完了条件、確認方針、agent task 分割方針が定義されている
- Notion とリポジトリの責務境界、および AWS 実環境確認の分担が明記されている
- 次のフェーズ別ディレクトリが作成されている
- `docs/implementation/phase2-alpha/agent-tasks/P2-alpha-1/`
- `docs/implementation/phase2-alpha/agent-tasks/P2-alpha-2/`
- `docs/implementation/phase2-alpha/agent-tasks/P2-alpha-3/`
- 上記ディレクトリは `.gitkeep` により git 管理対象として残せる
- P2-alpha-1、P2-alpha-2、P2-alpha-3 の詳細な agent task Markdown は作成されていない

### 確認方針

エージェントは、Notion の Phase 2α 実装計画、要件定義、基本設計、スコープ管理と本ファイルを照合します。
エージェントは、`agent-tasks/P2-alpha-1/`、`agent-tasks/P2-alpha-2/`、`agent-tasks/P2-alpha-3/` が存在することを確認します。
ユーザーは、フェーズ境界、実装順序、agent task ディレクトリ分けが合意内容どおりであることを確認します。

### agent task 分割方針

P2-alpha-0 のための詳細 agent task Markdown は作成しません。
P2-alpha-1、P2-alpha-2、P2-alpha-3 の詳細 task は、それぞれのフェーズ内で作成します。
本ファイルの作成と確認、および Phase 2α-1、Phase 2α-2、Phase 2α-3 用の agent task ディレクトリ作成を P2-alpha-0 の成果物とします。

## P2-alpha-1. コードベース品質ゲート対応

### 目的

AWS リソースの作成・変更を行わず、Java と CDK TypeScript のコードベースを品質ゲート pass 状態に揃えます。
Phase 2α-2 で buildspec に品質ゲートを組み込んだ際、初回から FAIL mode で起動できる状態を用意します。

### 前提フェーズ

- P2-alpha-0 が完了している
- Phase 2 のコードベースが現行の AWS dev 実行確認済み状態である

### 成果物

- `apps/web` の Maven 品質ゲート設定
- `infra/cdk` の ESLint / Prettier 設定
- cdk-nag 適用
- CDK assertion test 拡充
- 品質ゲート違反修正
- 必要なテスト追加
- ローカルで実行できる固定 command / npm script

### 実装方針

- AWS リソースは作成・変更しない
- PipelineStack、MigrationStack、Stack 構造変更は行わない
- ARM_64、Blue/Green、RunTask migration への先行対応は行わない
- Java 側は `apps/web` の Maven project を対象にする
- リポジトリ root に親 pom や multi-module 構成を作らない
- Maven 品質ゲートは `apps/web` で実行する
- CDK TypeScript 側は `infra/cdk` を対象にする
- `infra/cdk/package.json` に `lint`、`lint:fix`、`format:check`、`format:write` を追加する
- `infra/cdk/package.json` に `cdk:pipeline` script を追加し、`bin/cdk-pipeline.ts` を明示して実行できるようにする
- `lint` は `eslint .`、`lint:fix` は `eslint . --fix` とする
- `format:check` は `prettier --check .`、`format:write` は `prettier --write .` とする
- `cdk:pipeline` は `cdk --app "npx ts-node --prefer-ts-exts bin/cdk-pipeline.ts"` とする
- Spotless / Prettier の自動整形差分と意味のある実装修正は、可能な限り commit を分ける
- 本番コード変更は許可するが、業務仕様変更を目的にしない
- 本番コードを変更した場合は、対象機能のローカル動作確認手順または結果を実装記録へ残す
- JaCoCo 未達はテスト追加を基本に解消する
- 閾値を下げない
- 品質ゲート詳細は、公式ドキュメントと一般的な実務上のベストプラクティスに従う

### 除外範囲

- PipelineStack
- MigrationStack
- RegistryStack 4 repo 化
- GitHub Actions workflow 撤去
- buildspec 作成
- Docker buildx cache 設計
- Flyway SQL 正本移動
- apps/web 起動時 Flyway 廃止
- AWS deploy
- CodePipeline 実走

### 完了条件

ローカルで次がすべて exit 0 となること。

- `apps/web` で `./mvnw spotless:check`
- `apps/web` で `./mvnw spotbugs:check`
- `apps/web` で `./mvnw org.owasp:dependency-check-maven:check`
- `apps/web` で `./mvnw jacoco:check`
- `apps/web` で `./mvnw verify`
- `apps/web` で `./mvnw cyclonedx:makeAggregateBom`
- Web image の Trivy image scan
- `infra/cdk` で `npm run lint`
- `infra/cdk` で `npm run format:check`
- `infra/cdk` で `npm run build`
- `infra/cdk` で `npm test -- --runInBand`
- 現行 stack 構成の CDK synth
- cdk-nag 全 rule pass

SBOM 生成物、Dependency-Check report、JaCoCo report などの生成物は、必要な確認には使いますが、生成物自体は git 管理しません。

### 確認方針

エージェントは、ローカル command の成功、品質ゲート設定、違反修正、テスト追加、生成物除外を確認します。
ユーザー確認は原則不要です。
ただし、本番コード変更により画面操作や業務挙動確認が必要になった場合は、ローカルでの代表操作確認を実施し、結果を記録します。

### agent task 分割方針

P2-alpha-1 着手前に、Java 品質ゲート、CDK TypeScript 品質ゲート、横断検証の単位で詳細 agent task 分割案を作成します。
分割案は、その時点のコードベース、依存状態、品質ゲート違反状況を確認してから決めます。

## P2-alpha-2. CDK Stack 構造変更・Pipeline 構築

### 目的

deploy を伴わず、Phase 2α の新構成 CDK code を完成させます。
PipelineStack / MigrationStack を新設し、DeployStack を廃止し、RegistryStack、AppRuntimeStack、EdgeStack、CodeBuild、buildspec、品質ゲート、通知、GitHub Actions 撤去を Phase 2α 確定仕様に揃えます。

### 前提フェーズ

- P2-alpha-1 が完了している
- 品質ゲートがローカルで pass している
- Phase 2α の Pipeline stage 順序が Notion 側で確定している

### 成果物

- `PipelineStack`
- `MigrationStack`
- `bin/cdk-pipeline.ts`
- `RegistryStack` 4 repo 構成
- Web image build 用 CodeBuild project
- migration image build 用 CodeBuild project
- Build & Test 用 CodeBuild project
- Migration RunTask 用 buildspec
- Web image / migration image Docker build 設計
- Flyway CLI 専用 migration image
- root `db/` 配下の migration / seed 正本
- local migration 用 compose service
- ECS ネイティブ Blue/Green 対応 AppRuntimeStack
- ARM_64 対応 ECS Task Definition
- CodeStar Notifications + SNS 通知
- GitHub Actions workflow 撤去
- DeployStack 撤去
- CDK assertion test 更新

### 実装方針

- `bin/cdk-deploy.ts` と `DeployStack` を削除する
- `bin/cdk-pipeline.ts` を追加し、PipelineStack を生成する
- `bin/cdk-pipeline.ts` は props 参照のため、FoundationStack、RegistryStack、LogsStack、ConfigStack、IdentityStack など必要な参照元 Stack を同一 CDK app 内で instantiate する
- `infra/cdk/package.json` に `cdk:pipeline` script を追加する
- `cdk-infra.ts` は維持系基盤 Stack 用として残す
- `cdk-runtime.ts` は実行確認セッション用 Stack の定義に合わせて再構成する
- PipelineStack は CodePipeline V2、CodeBuild、CDK Pipelines、Artifact bucket、CodeConnections、CodeStar Notifications、SNS Topic を所有する
- Artifact bucket は AWS managed key、`crossAccountKeys: false`、30日 expire、非 current version 1日 expire とする
- CodeConnections IAM Policy は特定 repository / branch に限定する
- MigrationStack は Flyway 専用 image 実行用 ECS Task Definition を所有する
- MigrationStack は ECS Service を作らない
- Migration RunTask に必要な cluster、task definition、app private subnet、app security group、container name は buildspec に固定値として書かない
- Migration RunTask stage は、CloudFormation Outputs から必要値を取得して `aws ecs run-task` の `awsvpcConfiguration` を組み立てる
- MigrationStack は主要課金 4 stack destroy 対象に含めない
- RegistryStack は web、migration、web-cache、migration-cache の 4 repository を作る
- 既存の `registryStack.repository` 単一公開は廃止し、`webRepository`、`migrationRepository`、`webCacheRepository`、`migrationCacheRepository` のように責務を明示する
- AppRuntimeStack は `webRepository` を受け取る
- MigrationStack は `migrationRepository` を受け取る
- cache repository は Pipeline / CodeBuild から参照し、AppRuntimeStack / MigrationStack からは参照しない
- GitHub Actions workflow は `.github/workflows/ci.yml`、`infra-dev.yml`、`app-deploy-dev.yml`、`.gitkeep` を含めて撤去する
- Phase 2α 後は GitHub Actions workflow を PR チェック用にも残さない
- `apps/web` から Spring Boot Flyway 実行依存を削除する
- 既存の Flyway 関連テストは、apps/web 起動時 Flyway の確認ではなく、root `db/` SQL を Flyway CLI 相当で適用できることの確認へ置き換える
- local migration は `docker compose run --rm migration` 専用の one-shot service とする
- 通常の `docker compose up` で migration service を常駐起動しない
- AppRuntimeStack は Target Group と Listener rule を所有する
- EdgeStack は Listener 本体を持ち、AppRuntimeStack へ props 参照で渡す
- ECS Service の deployment controller は ECS とする
- CodeDeploy application、deployment group、appspec.yml は作らない
- Blue/Green は all-at-once、bake time 3分、Alarm 2本連動とする

### 除外範囲

- `cdk deploy`
- AWS 実リソース作成
- CodeConnection OAuth 認可
- Pipeline 実走
- preview / staging / prod 環境
- cross-account
- apps/api / apps/batch stage
- CodeBuild VPC mode
- VPC Endpoint
- db/app user
- db/migration user
- migration 専用 Security Group
- Step Functions
- Lambda による RunTask 結果判定
- custom resource による RunTask 結果判定
- 数値 SLO 合格判定
- GitHub Actions workflow の残置

### 完了条件

- 新構成で `cdk synth` 全 stack が exit 0 となる
- cdk-nag が全 pass する
- CDK assertion test が全 pass する
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

### 確認方針

エージェントは、CDK synth、cdk-nag、CDK assertion test、buildspec 静的確認、Dockerfile 構成確認、compose 設定確認を行います。
AWS deploy、Pipeline 実走、CodeConnection 認可、SNS メール受信、ManualApproval は P2-alpha-2 では実施しません。

### agent task 分割方針

P2-alpha-2 着手前に、現行 CDK entrypoint、Stack 関係、Dockerfile、Flyway SQL、workflow、テストを確認し、詳細 agent task 分割案を作成します。
分割は、DB migration runner、RegistryStack、PipelineStack、Build Images、Migration RunTask、AppRuntime Blue/Green、GitHub Actions 撤去、検証の責務境界で行います。
分割案は、その時点の Notion とコードベースを確認してから決めます。

## P2-alpha-3. 初回 deploy・実走確認

### 目的

CDK code の作業を伴わず、初回 deploy、CodeConnection 認可、Pipeline 自走、動作確認 checklist、ベースライン記録、主要課金 4 stack destroy を行います。
このフェーズの完了を Phase 2α 全体の完了とします。

### 前提フェーズ

- P2-alpha-2 が完了している
- 新構成の CDK synth、cdk-nag、CDK assertion test が pass している
- AWS profile、region、WORKOPS_STAGE がユーザーにより確認可能である
- CodeConnection の GitHub OAuth 認可をユーザーが実施できる

### 成果物

- 初回 PipelineStack deploy
- CodeConnection 認可
- Pipeline 自走結果
- ManualApproval 実施結果
- Deploy Registry stage 確認
- Build Images stage 確認
- Migration RunTask stage 確認
- Deploy AppRuntime stage 確認
- CloudFront default domain 経由の top page / health 確認
- ARM_64 ECS Task 起動確認
- Alarm / Log 確認
- ベースライン記録
- 主要課金 4 stack destroy 記録

### 実施方針

- CDK code 変更は行わない
- 必要な AWS 操作前に AWS profile と region を明示する
- AWS 操作前に `AWS_ACCESS_KEY_ID`、`AWS_SECRET_ACCESS_KEY`、`AWS_SESSION_TOKEN` を削除してから profile を使う
- AWS 実操作前に `aws sts get-caller-identity --region $env:AWS_REGION` で認証先を確認する
- 実 account ID、role ARN、credential、public IP、実 Cognito `sub`、DB 実データ値は git 管理文書に記録しない
- PipelineStack 初回 deploy はユーザーの明示指示がある場合だけ実施する
- CodeConnection OAuth 認可はユーザー操作とする
- ManualApproval はユーザー操作とする
- 確認完了後、主要課金 4 stack を個別 destroy して休眠状態に戻す

### 動作確認 checklist

Pipeline:

- Source が CodeConnections で `main` を取得して起動した
- Synth stage で `cdk synth` と cdk-nag が pass した
- Build & Test stage で全品質ゲートが pass した
- ManualApproval 通知メールが届いた
- 手動承認後、Deploy Registry stage で RegistryStack が deploy され、ECR repository 4 本が存在することを確認した
- Build Images stage で Web image と migration image が並列 build され、ECR に commit SHA tag で push された
- Deploy Data / Network / Migration stage が完走した
- Migration RunTask stage が完走した
- Deploy AppRuntime stage が完走した
- Pipeline 全体がエラーなく 1 周完走した

Migration:

- ECS RunTask が exit code 0 で終了した
- CloudWatch Logs 上で Flyway の schema 適用が記録されている
- Flyway 適用後、アプリケーションから DB 接続が成功している

App:

- Blue/Green 切替が完了した
- ALB Target Group が healthy を示している
- CloudFront default domain 経由で top page への HTTP 200 を取得できる
- ECS Task が `cpuArchitecture: ARM_64` で起動している

Alarm / Log:

- bake 期間 3 分の間、連動させた Alarm が発火していない
- CloudWatch Logs に致命的 ERROR がない

Cleanup:

- EgressStack が destroy 済みである
- DataStack が destroy 済みである
- EdgeStack が destroy 済みである
- AppRuntimeStack が destroy 済みである
- PipelineStack、MigrationStack、FoundationStack、ConfigStack、RegistryStack、LogsStack、SecretStack、Artifact bucket、ECR repository、CodeConnection は維持されている

### ベースライン記録項目

以下は合格判定には使わず、記録のみ行います。

- Pipeline 全体の run 時間
- Build & Test stage の時間
- Build Images stage の時間
- Migration RunTask の時間
- Blue/Green 切替完了までの時間

### 除外範囲

- CDK code 変更
- 数値 SLO 合格判定
- production-grade SLA 設定
- Pipeline 追加自動化
- 追加品質ゲートの tuning
- preview / staging / prod 環境
- cross-account

### 完了条件

- 動作確認 checklist が全項目達成されている
- ベースライン記録が保存されている
- 主要課金 4 stack が destroy 済みである
- ユーザー確認が必要な項目について、ユーザー確認結果が記録されている

### 確認方針

エージェントは、AWS CLI の確認コマンド作成、実行補助、結果整理を担当します。
ユーザーは、AWS認証、CodeConnection OAuth 認可、ManualApproval、メール受信、必要なブラウザ操作を担当します。

### agent task 分割方針

P2-alpha-3 着手前に、初回 deploy 手順、Pipeline 自走確認、チェックリスト消化、cleanup 記録の単位で詳細 agent task 分割案を作成します。
P2-alpha-3 の詳細 task は、P2-alpha-2 完了時点の実装結果に合わせて作成します。

