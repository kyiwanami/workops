# Phase 2 Implementation Phases

## 前提

Phase 2 のスコープ、設計方針、実装順序、完了条件は、Notion の「Phase2実装計画」「基本設計」「スコープ管理」「ADR」を正とします。
このファイルは、Notion で合意済みの Phase 2 を、コーディングエージェントへ渡せるリポジトリ側の実装フェーズへ整理するものです。
このファイルでは、Phase 2 のスコープや設計判断を変更しません。

リポジトリ側では、次を管理します。

- コード
- CDK
- Dockerfile
- Spring Boot 設定
- Flyway migration と seed
- GitHub Actions workflow
- 実装指示 Markdown
- README
- テスト
- 実装結果
- 確認結果

具体的な agent task 分割は、各 P2 フェーズ着手前にコーディングエージェントが提案します。
ユーザーが分割案を確認し、合意した後にだけ詳細な agent task Markdown を作成します。
このファイルでは、P2-1-01 / P2-1-02 のような具体タスク名、詳細タスク数、対応ファイル一覧を固定しません。

## 案

- Phase 2 は P2-0 から P2-10 で MVP の `apps/web` を AWS dev 上に再現可能にする
- P2-3 は HTTP ALB による Web アプリ起動、RDS 接続、`/actuator/health`、起動時 Flyway 適用の確認に限定する
- P2-4 は HTTPS 入口 / dev ドメイン / ACM を扱い、Cognito callback URL と sign-out URL に使う HTTPS URL を確定する
- P2-9 の GitHub Actions OIDC deploy は暫定 deploy 手段として扱う
- P2-10 完了後、既存 Phase 3 の前に Phase 2α を置き、CI/CD を CodePipeline + CodeBuild へ AWS ネイティブ化する
- Phase 2 の migration は `apps/web` の Spring Boot 起動時 Flyway 実行を維持する
- Phase 2α で migration 用 ECS Task へ分離する
- MySQL はローカル、Testcontainers、RDS for MySQL のすべてで 8.4 LTS に固定する

## ADR

- ADR-035 に従い、MySQL は 8.4 LTS 固定とし、MySQL 8.0 と 9.x は採用しない
- Phase 2 の deploy は GitHub Actions OIDC を暫定採用し、Phase 2α で CodePipeline + CodeBuild へ移行する
- Phase 2 の AWS dev migration は `apps/web` 起動時に実行し、Phase 2α で migration 用 ECS Task へ分離する
- Cognito の redirect URI は localhost 例外を除き HTTPS が必要なため、Cognito Hosted UI ログイン前に P2-4 で HTTPS URL を確定する
- P2-3 は HTTPS 終端、ACM certificate、Route 53、独自ドメイン、DNS validation を扱わず、HTTP ALB 経由の稼働確認に集中する
- `DomainStack` は証明書と FQDN を維持するため Phase 2 期間中に維持し、ALB と listener は実行確認セッション用の `EdgeStack` で扱う
- ACM public certificate は AWS 統合サービスで使う非 exportable public certificate を前提とし、Route 53 public hosted zone、DNS query、ドメイン登録は ACM とは別の課金対象として扱う
- Phase 2α 完了後、GitHub Actions workflow は PR チェック用にも残さない
- CodeDeploy、Blue/Green deploy、Canary deploy、PR レビューゲートは Phase 2 / Phase 2α では採用しない

## Phase 2 完了定義

MVP で完成した `apps/web` が、AWS dev 環境上で次の構成を使って再現可能に稼働していることを Phase 2 の完了とします。

- RDS MySQL
- Cognito User Pool / Hosted UI
- PLATFORM / TENANT App Client
- ECS Fargate
- ALB
- HTTPS 入口 / dev ドメイン / ACM
- ECR
- Secrets Manager
- SSM Parameter Store Standard
- CloudWatch Logs
- AWS CDK
- GitHub Actions OIDC deploy

Phase 2 の中心は新規業務機能の追加ではなく、MVP の Web アプリを AWS dev 環境へ載せ、認証、認可、DB、実行基盤、ログ、設定管理、デプロイを含めて再現可能にすることです。

## Notion へ戻す判断

次の判断は、コーディングエージェントがリポジトリ側だけで確定しません。

- Phase 2 の完了定義を変更する
- P2-0 から P2-10 の順序または境界を変更する
- Phase 3 以降の機能を Phase 2 へ追加する
- Cognito と WorkOps アプリの責務を変更する
- Cognito Trigger または Pre Token Generation を採用する
- Cognito claim、group、scope を業務権限の正本にする
- `users` または `permission_sets` を業務利用者・業務権限の正本から外す
- 初回ログイン時に `users` を自動作成する
- ユーザー作成、権限割当、ユーザー無効化の対象範囲を広げる
- PLATFORM / TENANT の利用者境界を変更する
- RDS、ECS、ALB、NAT Gateway のライフサイクル方針を変更する
- dev ドメイン、ACM certificate、HTTPS 入口の責務を変更する
- Secrets Manager と SSM Parameter Store の責務を変更する
- Flyway migration の実行方式を変更する
- Phase 2 で作らない AWS リソースを追加する
- 基本設計または ADR と矛盾する実装判断が必要になる

該当する場合は、実装を進める前に判断内容、影響範囲、選択肢をユーザーへ提示し、Notion 側の更新要否を確認します。

## AWS 実環境確認の分担

コーディングエージェントは、リポジトリ内の実装、静的確認、ビルド、テスト、CDK synth、workflow 構文確認、実行手順と確認コマンドの作成を担当します。

AWS アカウントへの認証、課金対象リソースの作成、Cognito のメール受信、Hosted UI のブラウザ操作、GitHub Actions の実行承認など、ユーザーの資格情報または外部サービス操作が必要な確認はユーザー確認とします。
ユーザーが実行を明示的に依頼し、利用可能な認証状態が確認できる場合は、コーディングエージェントが操作を補助できます。

各フェーズの完了時には、次を分けて記録します。

- エージェント確認: ビルド、テスト、静的確認、生成物確認、コマンド結果
- ユーザー確認: AWS dev、Cognito Hosted UI、GitHub Actions、画面操作などの実環境結果
- 未確認事項: 実行できなかった理由と、ユーザーが実行する具体的な手順

Phase 2 の完了判定では、エージェント確認とユーザー確認を混同しません。
agent task またはフェーズの完了条件にユーザー確認が含まれる場合、エージェント確認だけでは完了扱いにしません。
ユーザー確認が未実施の場合は、報告文で `エージェント確認完了、ユーザー確認未実施のためフェーズ未完了` と明記します。
実環境の期待結果がある task では、CDK synth、生成テンプレート確認、workflow 構文確認、単体テストの成功を、実環境確認の代替にしません。
後続のREADME整理フェーズや横断確認フェーズは、前段 task の初回ユーザー確認を代替しません。

## Phase 2 共通実装方針

- AWS リソースは `infra/cdk` の AWS CDK v2 で管理する
- CDK の Stack クラス名に `dev` / `stg` / `prod` を固定しない
- CloudFormation の物理スタック名は `stage` props から組み立てる
- `FoundationStack`、`IdentityStack`、`ConfigStack`、`SecretStack`、`RegistryStack`、`LogsStack`、`DataStack`、`DomainStack` は Phase 2 期間中維持する
- `EgressStack`、`EdgeStack`、`AppRuntimeStack` は実行確認セッション開始時に作成し、確認後に削除する
- `AppRuntimeStack` を `desiredCount=0` で残さない
- `DomainStack` は dev 用 FQDN、ACM public certificate、DNS validation、Cognito callback URL と sign-out URL に使う HTTPS URL の正本を扱う
- `EdgeStack` は ALB、Target Group、HTTP listener、HTTPS listener、HTTP から HTTPS への redirect、Route 53 Alias record を扱う
- VPC Endpoint と Container Insights は作成しない
- ALB Cognito 認証を使わず、Spring Security OAuth2 Login と Cognito Hosted UI を使う
- アプリ利用者情報の正本は `users` とする
- 業務権限の正本は `permission_sets` とする
- Cognito から業務利用する突合キーは `sub` とする
- 会社境界は Mapper SQL の `company_id` 条件で守る
- Phase 2 の AWS dev Flyway migration は、`apps/web` の Spring Boot 起動時実行を維持する
- Phase 2α で migration 用 ECS Task へ分離し、Web アプリ起動時の Flyway 自動実行を止める
- ローカルでは Spring Boot 起動時の Flyway 実行を維持する
- 秘匿値は Secrets Manager、非機密設定値は SSM Parameter Store Standard で管理する
- DB endpoint に依存する SSM Parameter は RDS と同じ Stack で管理する
- DB 非依存の runtime config は `ConfigStack` で管理する
- GitHub Actions は `workflow_dispatch` による暫定の手動 deploy から開始する
- GitHub Actions 固有の処理を増やしすぎず、deploy 実体は CDK、リポジトリ内スクリプト、npm、Maven、Docker、AWS CLI の標準コマンドへ寄せる
- P2-10 完了後、Phase 2α で CodePipeline + CodeBuild へ移行し、GitHub Actions workflow を撤去する
- CodeDeploy、Blue/Green deploy、Canary deploy、PR レビューゲートは採用しない
- 旧サービス単位の Stack 名は、ADR 内の却下案を除き、実装、README、agent task 名、基本設計寄りの説明に出さない
- Git commit は実行しない

## P2-0. Phase 2 設計・リポジトリ準備

### 目的

Notion で確定した Phase 2 の範囲、順序、完了条件、責務境界を、リポジトリ側で実装へ渡せる状態にします。

### 前提フェーズ

- MVP の M1 から M10 が完了している
- `apps/web` がローカルで起動、テスト、業務操作できる
- Notion の Phase2実装計画、基本設計、スコープ管理、ADR が確定している

### 成果物

- `docs/implementation/phase2/phases.md`
- Phase 2 の実装順序
- Phase 2 のリポジトリ側責務
- AWS 実環境確認の分担
- Notion へ戻す判断条件
- agent task 作成ルール

### 実装方針

- Notion の内容をコード詳細へ展開するための入口文書として管理する
- Notion の文章をそのまま複製せず、実装順序、依存関係、成果物、確認方法をリポジトリ向けに整理する
- 詳細 agent task は対象フェーズの着手直前に作成する
- P2-1 以降の実装を P2-0 に含めない

### 除外範囲

- CDK 実装
- Dockerfile
- Spring Security 実装
- `application-dev.yml` の具体設定
- AWS リソース作成
- GitHub Actions workflow
- 詳細 agent task Markdown
- `agent-tasks` ディレクトリ

### 完了条件

P2-0 から P2-10 の目的、前提、成果物、実装方針、除外範囲、完了条件、確認方針、agent task 分割方針が定義されている。
Notion とリポジトリの責務境界、および AWS 実環境確認の分担が明記されている。

### 確認方針

エージェントは、Notion の Phase2実装計画、基本設計、スコープ管理、ADR と本ファイルを照合します。
ユーザーは、フェーズ境界と実装順序が Notion の合意内容どおりであることを確認します。

### agent task 分割方針

P2-0 のための詳細 agent task Markdownは作成しません。
本ファイルの作成と確認を P2-0 の成果物とします。

## P2-1. AWS dev 基盤

### 目的

後続の RDS、ECS、HTTPS 入口、Cognito を載せられる AWS dev 環境の最小土台を作ります。

### 前提フェーズ

- P2-0 が完了している
- AWS アカウント、dev 用 region、CDK bootstrap 対象がユーザーにより確定している
- `infra/cdk` を Phase 2 のインフラ実装場所として使用できる

### 成果物

- AWS CDK v2 の初期構成
- stage props による環境差分管理
- `FoundationStack`
- `ConfigStack`
- `RegistryStack`
- `LogsStack`
- VPC
- public / private subnet
- Route Table
- Internet Gateway
- Security Group
- ECS Cluster
- ECR private repository
- ECR lifecycle policy
- CloudWatch Logs log group
- SSM Parameter Store Standard の非機密設定領域
- AWS dev 基盤の構築、確認、削除手順

### 実装方針

- Stack クラス名は環境非依存にする
- 物理スタック名は `workops-${stage}-foundation` の形式で stage props から生成する
- `FoundationStack` に NAT Gateway と ALB を含めない
- `RegistryStack` の ECR は最新 2 イメージだけを保持する
- `LogsStack` の retention は 7 日に固定する
- `ConfigStack` には DB 非依存の非機密設定だけを置く
- ECS Cluster は後続の Web アプリで利用し、Phase 2α 以降の migration task でも共用する

### 除外範囲

- NAT Gateway
- ALB
- ECS Service
- Fargate task 起動
- RDS
- Secrets Manager
- Cognito
- Flyway migration 実行
- GitHub Actions deploy
- VPC Endpoint
- Container Insights
- API 用 ECS Service

### 完了条件

`FoundationStack`、`ConfigStack`、`RegistryStack`、`LogsStack` を CDK で synth できる。
ユーザーの AWS dev 環境で各 Stack を deploy できる。
NAT Gateway、ALB、ECS Service、RDS、Secrets Manager が P2-1 の Stack に含まれていない。

### 確認方針

エージェントは、CDK のビルド、静的確認、テスト、`cdk synth`、生成テンプレートのリソース確認を行います。
ユーザーは、AWS dev への deploy、CloudFormation Stack、VPC、ECS Cluster、ECR、CloudWatch Logs、SSM Parameter の作成結果を確認します。

### agent task 分割方針

P2-1 着手前に、CDK 初期化、基盤 Stack、ECR / Logs / Config、検証記録の境界で agent task 分割案を提示します。
ユーザー合意前に詳細 task Markdown を作成しません。

## P2-2. RDS・Secrets・Flyway migration

### 目的

AWS dev 環境に WorkOps の業務 DB 正本を作り、P2-3 の `apps/web` 起動時 Flyway 実行で構築できる状態にします。

### 前提フェーズ

- P2-1 が完了している
- `FoundationStack` の VPC、private subnet、Security Group、ECS Cluster を参照できる
- MVP の Flyway migration と seed がローカルで正常に適用できる

### 成果物

- `SecretStack`
- `DataStack`
- RDS for MySQL 8.4 LTS
- DB Subnet Group
- DB Security Group
- DB 接続情報用 Secrets Manager secret
- DB endpoint 由来の SSM Parameter
- DB 非依存の非機密設定用 SSM Parameter
- RDS Console integrated CloudShell VPC による private RDS 接続確認手順
- AWS dev 用 Spring profile 設定
- AWS dev 用 Flyway migration 実行方式
- 共通マスタ seed
- AWS dev 動作確認用 seed
- RDS 接続、migration、seed の確認手順

### 実装方針

- MySQL はローカル、Testcontainers、RDS for MySQL のすべてで 8.4 LTS に固定する
- MySQL 8.0 と 9.x は採用しない
- RDS は Single-AZ、最小クラス、20GB、backup retention 1日、deletion protection なしにする
- RDS は private subnet に配置し、Phase 2 期間中維持する
- DB username と DB password は Secrets Manager で管理する
- RDS master secret と DB endpoint 由来の SSM Parameter は `DataStack` で管理し、RDS と同じ lifecycle にする
- `/workops/{stage}/db/name`、`/workops/{stage}/db/port`、`/workops/{stage}/db/url` は `DataStack` が所有する
- `/workops/{stage}/spring/profile` は `ConfigStack` が所有する
- `ConfigStack` は `DataStack` を参照しない
- Spring profile、AWS region、Cognito 設定値、ALB URL などの非機密値は SSM Parameter Store Standard で管理する
- Cognito issuer URI を設定値の正本として保存せず、region と User Pool ID から構成する
- Cognito App Client は client secret なしを前提とする
- private RDS への人手の接続確認は RDS Console integrated CloudShell VPC で行い、EC2 踏み台、SSM port forwarding、Session Manager Plugin は使わない
- RDS Console CloudShell 接続確認用 Security Group は専用用途とし、他リソースへ流用しない
- Phase 2 の AWS dev では `apps/web` 起動時に Flyway を自動実行する
- Phase 2α までは migration 用 ECS Task 定義を作らない
- Phase 2α で migration 用 ECS Task へ分離し、Web アプリ起動時の Flyway 自動実行を止める
- AWS dev seed は再現可能な動作確認データとして Flyway で管理する
- Flyway seed は `db/seed/common`、`db/seed/local`、`db/seed/aws-dev` に分け、profile 別 locations で local と AWS dev の seed を切り替える
- local と AWS dev の `V6__insert_users.sql` は同一 version とし、同じ profile で同時に読み込まない
- 実 Cognito の `sub` は seed に固定しない

### 除外範囲

- 本番 DB 構成
- Multi-AZ
- Blue/Green migration
- MySQL 8.0
- MySQL 9.x
- 複雑なロールバック運用
- Cognito ユーザー作成
- Cognito `sub` と `users.cognito_sub` の実環境突合
- SSM Parameter Store への秘匿値保存
- Phase 3 以降のテーブル

### 完了条件

`SecretStack` と `DataStack` を CDK で synth できる。
ユーザーの AWS dev 環境で RDS と DB 接続 secret を作成できる。
RDS Console integrated CloudShell VPC から private RDS へ接続し、`SELECT 1;` を確認できる。
AWS dev 用 Spring profile、DB 接続設定、Flyway locations、AWS dev seed の適用方針を実装できる。
P2-3 で `apps/web` 起動時に既存 migration と AWS dev seed を適用できる状態になっている。

### 確認方針

エージェントは、CDK synth、Spring Boot 設定の検証、Flyway migration のローカル適用、secret と parameter の参照構造を確認します。
ユーザーは、RDS の構成値、Secrets Manager と SSM Parameter Store の格納先、RDS Console integrated CloudShell VPC による private RDS 接続、P2-3 で実行する `apps/web` 起動時 migration の確認手順を確認します。

### agent task 分割方針

P2-2 着手前に、Secret / Data Stack、AWS dev 設定、Spring Boot Flyway 設定、AWS dev seed、検証記録の境界で agent task 分割案を提示します。
DDL や seed SQL の具体的なファイル分割は agent task 作成時に既存 migration を確認して決定します。

## P2-3. `apps/web` ECS Fargate 稼働

### 目的

MVP で完成した `apps/web` を AWS dev 上で起動します。

P2-3 では、実行確認セッション用に `EgressStack`、`EdgeStack`、`AppRuntimeStack` を作成し、HTTP ALB 経由で `apps/web` の起動、Spring Boot Actuator `/actuator/health`、RDS 接続、起動時 Flyway 適用を確認します。

P2-3 では HTTPS 終端、ACM certificate、Route 53、独自ドメイン、Cognito Hosted UI ログインは扱いません。
HTTPS 入口は P2-4 で扱います。

### 前提フェーズ

- P2-2 が完了している
- RDS、DB 接続 secret、AWS dev 用 Spring profile 設定を参照できる
- P2-2 で RDS Console integrated CloudShell VPC による private RDS 接続確認が完了している
- ECR、CloudWatch Logs、ECS Cluster を利用できる

### 成果物

- `apps/web` 用 Dockerfile
- コンテナ build 手順
- ECR push 手順
- `EgressStack`
- `EdgeStack`
- `AppRuntimeStack`
- NAT Gateway
- ALB
- HTTP listener
- Target Group
- ECS Task Definition
- ECS Service
- Fargate task
- HTTP ALB ルーティング
- Secrets Manager と SSM Parameter Store のタスク設定
- dev profile 起動設定
- Spring Boot 起動時 Flyway 実行設定
- Spring Boot Actuator `/actuator/health`
- `apps/web` 起動時 Flyway による migration / seed 適用
- P2-2 から引き継いだ `flyway_schema_history` と主要 seed テーブル確認
- CloudWatch Logs への標準ログ出力
- 手動 deploy 手順
- 実行確認セッション終了後の削除手順

### 実装方針

- ALB は public subnet、ECS Fargate と RDS は private subnet に配置する
- `EgressStack`、`EdgeStack`、`AppRuntimeStack` は実行確認セッション単位で作成する
- 作成順序は `EgressStack`、`EdgeStack`、`AppRuntimeStack` とする
- `apps/web` の起動時に Flyway migration と AWS dev seed を適用する
- 起動時 migration が失敗した場合、ECS task は正常稼働扱いにしない
- HTTP ALB / ECS の health check は `/actuator/health` を使う
- `/actuator/health` は Spring Security の認証対象外にする
- ALB Cognito 認証は使わない
- 確認後は `AppRuntimeStack`、`EdgeStack`、`EgressStack` の順で削除する
- `AppRuntimeStack` を `desiredCount=0` で残さない

### 除外範囲

- HTTPS 終端
- ACM certificate
- Route 53
- 独自ドメイン
- DNS validation
- Cognito Hosted UI 本格ログイン
- Cognito `sub` と `users` の実環境突合
- PLATFORM / TENANT App Client 分離
- GitHub Actions deploy
- VPC Endpoint
- Container Insights
- Actuator / Micrometer / Observability 全体の導入
- API 用 ECS Service

### 完了条件

コンテナを build して ECR へ push できる。
実行確認セッションで `EgressStack`、`EdgeStack`、`AppRuntimeStack` を作成できる。
HTTP ALB 経由で Spring Boot Actuator の `/actuator/health` による health check に成功し、RDS に接続した状態で MVP アプリが AWS dev 上で起動する。
`apps/web` 起動時に Flyway migration と AWS dev seed を適用できる。
`flyway_schema_history` と主要 seed テーブルを確認できる。
確認後に `AppRuntimeStack`、`EdgeStack`、`EgressStack` を削除できる。
HTTPS 終端、ACM certificate、Route 53、独自ドメイン、DNS validation は P2-4 で扱う。

### 確認方針

エージェントは、Maven build、テスト、Docker build、コンテナ起動、CDK synth、health endpoint の認証除外設定を確認します。
ユーザーは、ECR push、各 Stack の deploy、ALB health check、ECS task 状態、RDS 接続、CloudWatch Logs、確認後の Stack 削除を確認します。

### agent task 分割方針

P2-3 着手前に、コンテナ化、実行セッション Stack、Actuator と dev 設定、手動 deploy、削除確認の境界で agent task 分割案を提示します。
Cognito 本格連携を P2-3 の task に含めません。

## P2-4. HTTPS 入口・dev ドメイン・ACM

### 目的

P2-5 の Cognito Hosted UI ログイン確認に入る前に、AWS dev 環境へ HTTPS でアクセスできる入口を作ります。

P2-3 では HTTP ALB で `apps/web` の起動、health check、RDS 接続を確認します。
P2-4 では、Cognito callback URL と sign-out URL として使える HTTPS URL を確定します。

### 前提フェーズ

- P2-3 が完了している
- HTTP ALB 経由で `apps/web` に到達できる
- HTTPS 確認に使う独自ドメインまたは管理可能なサブドメインをユーザーが決定している
- Route 53 public hosted zone を作成する場合は、課金対象操作としてユーザー確認が完了している

### 成果物

- `DomainStack`
- dev 用 FQDN
- ACM public certificate
- DNS validation
- ALB HTTPS listener
- HTTP listener から HTTPS への redirect
- Route 53 Alias record または管理可能な DNS から ALB への名前解決
- Cognito callback URL と sign-out URL に使う HTTPS URL の確定
- HTTPS 経由の `/actuator/health` 確認
- HTTPS 入口の README / task 記載

### 実装方針

- ALB デフォルト DNS 名には ACM public certificate を発行できないため、HTTPS 確認には独自ドメインまたは管理可能なサブドメインを使う
- ACM public certificate は、ALB など AWS 統合サービスで使う非 exportable public certificate を前提とする
- ACM certificate 自体は追加料金なしで使える前提とする
- Route 53 public hosted zone、DNS query、ドメイン登録には別途費用が発生し得る
- 証明書は DNS validation を使い、自動更新できる状態にする
- ALB は HTTPS 終端を担当し、ALB から ECS task への通信は HTTP とする
- ALB Cognito 認証は使わない
- Cognito 認証は P2-5 で Spring Security OAuth2 Login + Cognito Hosted UI として扱う
- `DomainStack` は Phase 2 期間中維持する
- `EdgeStack` は実行確認セッション用の ALB、listener、Route 53 Alias record を扱い、確認後に削除する
- P2-4 の確認時は、必要に応じて `EgressStack`、`EdgeStack`、`AppRuntimeStack` を再作成する

### 費用方針

ACM public certificate を ALB など AWS 統合サービスで使う場合、証明書自体は追加料金なしとします。
ただし、Route 53 public hosted zone、DNS query、ドメイン登録は ACM とは別の課金対象として扱います。

Route 53 public hosted zone を作る場合は、課金対象操作のため、ユーザー確認を前提にします。
既存ドメインまたは既存 hosted zone を使える場合は、それを優先します。

### 除外範囲

- CloudFront
- WAF
- ALB Cognito 認証
- 本番ドメイン設計
- 複雑な DNS ルーティング
- Blue/Green deploy
- Route 53 health check
- Cognito Hosted UI ログイン本体
- users 突合

### 完了条件

AWS dev 環境で、独自ドメインまたは管理可能なサブドメインの HTTPS URL から ALB へ到達できる。
ALB で HTTPS 終端し、ECS 上の `apps/web` へ HTTP で転送できる。
HTTP アクセスは HTTPS へ redirect される。
HTTPS 経由で `/actuator/health` を確認できる。
P2-5 で Cognito Hosted UI の callback URL と sign-out URL として使う HTTPS URL が確定している。

### 確認方針

エージェントは、DomainStack、ACM certificate、DNS validation、HTTPS listener、HTTP から HTTPS への redirect、Route 53 Alias record、CDK synth、README の確認手順を確認します。
ユーザーは、ドメインまたは hosted zone の利用、証明書検証、HTTPS 到達、HTTP redirect、HTTPS 経由の `/actuator/health`、確認後の実行確認セッション Stack 削除を確認します。

### agent task 分割方針

P2-4 着手前に、DomainStack、証明書と DNS validation、HTTPS listener と redirect、Alias record、HTTPS health check、README と確認記録の境界で agent task 分割案を提示します。
Cognito Hosted UI ログイン本体を P2-4 の task に含めません。

## P2-5. Cognito Hosted UI ログイン・`users` 突合

### 目的

P2-4 で確定した HTTPS URL を使い、AWS dev 上で Cognito Hosted UI によるログインを成立させ、Cognito `sub` で `users.cognito_sub` を突合し、DB 由来の利用者情報と権限で業務画面を利用できるようにします。

### 前提フェーズ

- P2-4 が完了している
- P2-4 で Cognito callback URL と sign-out URL に使う HTTPS URL が確定している
- MVP の Cognito OAuth2 Login 最小接続、`LoginUserContext`、`GrantedAuthority` の実装が存在する
- AWS dev の `users` に実 Cognito テストユーザーと突合できるデータを準備できる

### 成果物

- `IdentityStack`
- Cognito User Pool
- Hosted UI / managed login domain
- client secret なしの App Client
- P2-4 で確定した HTTPS callback URL と sign-out URL の設定
- AWS dev 用 Spring Security OAuth2 Login
- Cognito `sub` 取得
- `users.cognito_sub` 突合
- DB 由来の `LoginUserContext`
- `permission_sets.code` 由来の `GrantedAuthority`
- 突合失敗時の業務利用拒否
- 突合失敗時の WARN ログ

### 実装方針

- Cognito は認証主体の発行元とする
- `users` をアプリ利用者情報の正本とする
- `permission_sets` を業務権限の正本とする
- Cognito から業務利用する突合キーは `sub` に限定する
- username、email、actor type、company ID、permission sets は DB から取得する
- DB 由来の `LoginUserContext` を `Authentication.principal` として保持する
- DB 由来の権限セットを `GrantedAuthority` として保持する
- Cognito ログイン成功後に `users` が見つからない場合は業務画面へ入れない
- 突合失敗時に仮ユーザーまたは `users` 行を自動作成しない

### 除外範囲

- PLATFORM / TENANT App Client 分離
- Cognito Trigger
- Pre Token Generation
- Cognito group / scope による業務認可
- 初回ログイン時の `users` 自動作成
- users 突合失敗時の自動復旧
- ユーザー作成画面の追加
- 権限割当画面の追加

### 完了条件

Cognito Hosted UI でログインできる。
ログイン後に Cognito `sub` と `users.cognito_sub` を突合できる。
DB 由来の利用者情報と権限セットで MVP の業務画面を利用できる。
該当する `users` が存在しない場合は業務利用を拒否し、WARN ログを確認できる。

### 確認方針

エージェントは、Spring Security 設定、`sub` 突合処理、principal と authority の構築、突合成功・失敗の自動テスト、CDK synth を確認します。
ユーザーは、Hosted UI のブラウザログイン、callback、業務画面遷移、DB 由来権限、突合失敗時の拒否と CloudWatch Logs を確認します。

### agent task 分割方針

P2-5 着手前に、Identity Stack、AWS dev OAuth2 設定、`users` 突合、認証成功・失敗処理、実環境確認の境界で agent task 分割案を提示します。
App Client 分離は P2-6 へ残します。

## P2-6. PLATFORM・TENANT App Client 分離

### 目的

WorkOps 運営側と企業テナント側のログイン導線を分け、ログイン経路と `users.actor_type` の整合を検証します。

### 前提フェーズ

- P2-5 が完了している
- 単一 App Client で Cognito ログインと `users` 突合が成立している
- `users.actor_type` に PLATFORM / TENANT が保存されている

### 成果物

- PLATFORM 向け App Client
- TENANT 向け App Client
- PLATFORM 向けログイン入口
- TENANT 向けログイン入口
- App Client ごとの callback URL と logout URL
- ログイン導線の識別
- `users.actor_type` 整合チェック
- 導線不整合時の業務利用拒否
- 導線不整合時の WARN ログ

### 実装方針

- 同一 User Pool 内で PLATFORM / TENANT の App Client を分ける
- App Client は client secret なしで作成する
- 利用者区分の正本は `users.actor_type` とする
- PLATFORM 導線では PLATFORM ユーザーだけを許可する
- TENANT 導線では TENANT ユーザーだけを許可する
- App Client またはログイン入口から期待 actor type を識別する
- `sub` で `users` を突合した後、期待 actor type と `users.actor_type` を比較する
- 不整合時は業務利用を拒否し、WARN ログを出力する

### 除外範囲

- 企業ごとの User Pool
- 企業ごとの App Client
- PLATFORM 管理画面の新規業務機能
- Cognito Trigger
- Pre Token Generation
- Cognito group による actor type 判定
- ユーザー作成・権限割当の新規実装

### 完了条件

PLATFORM 向けと TENANT 向けでログイン入口を分離できる。
各入口からログインしたユーザーの `users.actor_type` が期待値と一致する場合だけ業務利用できる。
PLATFORM / TENANT の導線不整合時は業務利用を拒否し、WARN ログを確認できる。

### 確認方針

エージェントは、App Client 定義、ログイン入口、期待 actor type の受け渡し、整合チェック、成功・不整合の自動テスト、CDK synth を確認します。
ユーザーは、PLATFORM / TENANT の各 Hosted UI 導線、正しい組み合わせのログイン、誤った組み合わせの拒否、CloudWatch Logs を確認します。

### agent task 分割方針

P2-6 着手前に、Identity Stack の App Client 分離、アプリ側ログイン入口、actor type 整合チェック、実環境確認の境界で agent task 分割案を提示します。
管理導線の実 Cognito 接続は P2-7 へ残します。

## P2-7. ユーザー管理導線の AWS dev 接続

### 目的

M9 / M10 で実装済みの会社、部署、ユーザー、権限管理導線と Cognito `AdminCreateUser` 境界を AWS dev の実 Cognito へ接続します。

### 前提フェーズ

- P2-6 が完了している
- PLATFORM / TENANT のログイン導線が分離されている
- M9 / M10 の管理導線がローカルで動作する
- local profile の Fake Bean と AWS dev profile の実 Cognito Bean の境界が存在する

### 成果物

- AWS dev profile の実 Cognito `AdminCreateUser` 接続
- ECS Task Role の Cognito 操作権限
- PLATFORM_ADMIN による PLATFORM ユーザー作成
- PLATFORM_ADMIN による全テナントの TENANT ユーザー作成
- PLATFORM_ADMIN による会社と初期 TENANT_MANAGER 作成
- TENANT_MANAGER による自社 TENANT ユーザー作成
- 権限セット割当・変更
- `AdminCreateUser` で取得した `sub` の `users.cognito_sub` 登録
- 作成ユーザーの Hosted UI ログイン確認
- DB 由来権限による業務利用確認

### 実装方針

- ユーザー作成は WorkOps の管理導線を起点にする
- WorkOps アプリから Cognito `AdminCreateUser` を呼び出す
- Cognito 作成成功後に取得した `sub` を `users.cognito_sub` に登録する
- `users` と `user_permission_sets` は同じ業務処理の中で登録する
- Cognito 標準の一時パスワードと初回変更フローを使う
- local profile は Fake Bean を維持し、AWS API を呼び出さない
- AWS dev profile は AWS SDK for Java 2.x と Default Credentials Provider Chain を使う
- PLATFORM_ADMIN と TENANT_MANAGER の操作範囲は M10 の業務ルールを維持する
- TENANT_MANAGER 数が 0 になる権限変更は、更新後 count による業務バリデーションで拒否する
- actor type と company ID は作成後変更しない

### 除外範囲

- 管理導線の全面的な作り直し
- 認証キー方式
- Cognito Trigger
- Pre Token Generation
- 初回ログイン時の `users` 自動作成
- ユーザー無効化
- Cognito 側 username 変更
- Cognito 側 email 変更
- 複雑な招待メール
- 企業 SSO
- 組織階層管理
- 詳細な監査ログ画面

### 完了条件

WorkOps の管理導線から実 Cognito の `AdminCreateUser` を呼び出せる。
取得した `sub` を `users.cognito_sub` に登録できる。
作成ユーザーが一時パスワードを変更し、対応する Hosted UI からログインできる。
ログイン後に DB 由来の権限セットで許可された業務画面を利用できる。
PLATFORM_ADMIN と TENANT_MANAGER の管理範囲が M10 の仕様どおりに制限される。

### 確認方針

エージェントは、profile ごとの Bean 切替、AWS SDK 呼び出し境界、IAM 権限、DB 登録トランザクション、権限範囲、失敗時処理の自動テストを確認します。
通常の自動テストは AWS へ接続しません。
ユーザーは、実 Cognito へのユーザー作成、招待受信、初回パスワード変更、Hosted UI ログイン、`users.cognito_sub`、業務権限を確認します。

### agent task 分割方針

P2-7 着手前に、IAM と AWS SDK 接続、PLATFORM 管理導線、TENANT 管理導線、実 Cognito 作成・ログイン確認、検証記録の境界で agent task 分割案を提示します。
M9 / M10 の既存業務機能を重複実装しません。

## P2-8. CloudWatch Logs・認証認可イベントログ

### 目的

AWS dev 上で、業務操作、認証、認可、利用者突合、例外を調査できるログを CloudWatch Logs から確認できるようにします。

### 前提フェーズ

- P2-7 が完了している
- ECS アプリログと `apps/web` 起動時 Flyway ログが CloudWatch Logs へ出力される
- MVP の業務操作ログが実装済みである

### 成果物

- ECS アプリログの確認手順
- `apps/web` 起動時 Flyway ログの確認手順
- ECS task 停止理由の確認手順
- 認証失敗ログ
- 未ログインログ
- 認証切れログ
- AccessDenied ログ
- `users` 突合失敗ログ
- actor type 不整合ログ
- 権限不足ログ
- 例外ログ
- Spring Security handler
- 調査時に見るログを整理した README

### 実装方針

- MVP の業務操作ログを CloudWatch Logs で確認可能にする
- 認証失敗、未ログイン、認証切れ、AccessDenied は Spring Security handler で明示的に出力する
- `users` 突合失敗と actor type 不整合は、認証後の DB 突合処理で WARN ログを出力する
- Cognito 自体の内部ログ取得を目的にしない
- WorkOps アプリから見た Cognito 連携結果をログへ残す
- ログへパスワード、token、secret、業務本文を出力しない
- CloudWatch Logs retention は 7 日を維持する

### 除外範囲

- 監査ログテーブル
- 履歴テーブル
- ログ検索画面
- Observability Dashboard
- CloudWatch Alarm
- Container Insights
- OpenTelemetry
- 分散トレーシング
- JSON ログ基盤への全面移行

### 完了条件

AWS dev 上で、MVP の主要業務操作、認証成功・失敗、認可拒否、`users` 突合失敗、actor type 不整合、アプリ例外、`apps/web` 起動時 Flyway 失敗を CloudWatch Logs から調査できる。
README に、事象ごとの確認先と確認手順が記載されている。

### 確認方針

エージェントは、各 handler とログ出力処理の自動テスト、機密情報を出力しないこと、README のコマンドとログ項目を確認します。
ユーザーは、AWS dev で各失敗ケースを発生させ、CloudWatch Logs と ECS task 停止理由から調査できることを確認します。

### agent task 分割方針

P2-8 着手前に、Spring Security イベントログ、Cognito / DB 突合ログ、CloudWatch 確認手順、失敗シナリオ確認の境界で agent task 分割案を提示します。
監視、Alarm、Dashboard を task に含めません。

## P2-9. GitHub Actions OIDC deploy

### 目的

長期アクセスキーを使わず、GitHub Actions の手動実行から AWS dev へテスト、build、ECS 反映、`apps/web` 起動時 migration を再現可能にします。
P2-9 の GitHub Actions OIDC deploy は Phase 2 の暫定 deploy 手段であり、Phase 2α 後の最終形ではありません。

### 前提フェーズ

- P2-8 が完了している
- 手動手順で CDK deploy、ECR push、ECS deploy、`apps/web` 起動時 migration が成功している
- GitHub repository と AWS account の OIDC 信頼設定を作成できる

### 成果物

- GitHub Actions OIDC provider または既存 provider 参照
- GitHub Actions 用 IAM Role
- repository / branch 条件を含む trust policy
- `infra-dev` workflow
- `app-deploy-dev` workflow
- `workflow_dispatch`
- path filter
- Maven test
- Docker build
- ECR push
- CDK deploy
- `apps/web` 起動時 migration の成否確認
- ECS deploy
- 初回構築、初回 deploy、通常 deploy、失敗時確認の README

### 実装方針

- AWS 認証は GitHub Actions OIDC を使う
- 長期アクセスキーを GitHub Secrets に保存しない
- `infra-dev` と `app-deploy-dev` を分離する
- 初回構築は `infra-dev` の後に `app-deploy-dev` を実行する
- アプリ変更時は `app-deploy-dev` を実行する
- deploy トリガーは `workflow_dispatch` に限定する
- docs だけの変更では deploy しない
- `apps/web`、`infra/cdk`、Flyway migration の変更を deploy 対象とする
- app deploy はテスト、Docker build、ECR push、CDK deploy、ECS deploy、`apps/web` 起動時 migration 確認の順で実行する
- `apps/web` 起動時 migration が失敗した場合は deploy 失敗として扱う
- migration の差分なしの場合は Flyway の正常終了を許可する
- infra deploy では、P2-4 で追加した `DomainStack` と、HTTPS 対応済みの `EdgeStack` 構成も再現対象に含める
- GitHub Actions 固有の分岐や独自処理を増やしすぎない
- deploy 実体は CDK、リポジトリ内スクリプト、npm、Maven、Docker、AWS CLI の標準コマンドへ寄せる
- Phase 2α で CodePipeline + CodeBuild へ移行できる形に保つ

### 除外範囲

- main push による自動 deploy
- 本番 deploy
- 長期 AWS アクセスキー
- 複雑な承認フロー
- GitHub Actions の恒久利用
- PR レビューゲート
- CodeDeploy
- Blue/Green deploy
- Canary deploy
- マルチアカウント構成
- Phase 3 の API deploy

### 完了条件

GitHub Actions の `workflow_dispatch` から AWS dev へ接続できる。
`infra-dev` で対象インフラを deploy できる。
`app-deploy-dev` でテスト、Docker build、ECR push、ECS 反映、`apps/web` 起動時 migration 確認を順番どおり実行できる。
`apps/web` 起動時 migration が失敗した場合に deploy 失敗として扱える。

### 確認方針

エージェントは、workflow 構文、action の入力、OIDC trust 条件、IAM 最小権限、path filter、`apps/web` 起動時 migration 成否判定、秘密情報の非埋め込みを確認します。
ユーザーは、GitHub の `workflow_dispatch`、OIDC 認証、各 job のログ、`apps/web` 起動時 migration 成功時の deploy、migration 失敗時の扱いを確認します。

### agent task 分割方針

P2-9 着手前に、OIDC / IAM、`infra-dev`、`app-deploy-dev`、`apps/web` 起動時 migration 確認、README と失敗確認の境界で agent task 分割案を提示します。
自動 deploy や本番 deploy を task に含めません。

## P2-10. 総合確認・README 整理

### 目的

Phase 2 の完了条件を横断確認し、第三者が構築、実行、削除、再デプロイの流れを再現できる状態にします。

### 前提フェーズ

- P2-1 から P2-9 が完了している
- 各フェーズのエージェント確認とユーザー確認が記録されている
- 未解消事項が Phase 2 完了条件へ影響するか判断できる

### 成果物

- Phase 2 総合確認結果
- AWS dev 初回構築手順
- 実行確認セッション開始手順
- `apps/web` 起動時 migration 手順
- アプリ deploy 手順
- PLATFORM / TENANT ログイン確認手順
- ユーザー作成・権限確認手順
- CloudWatch Logs 調査手順
- 実行確認セッション終了時の削除手順
- GitHub Actions 再 deploy 手順
- Phase 2α への引き継ぎ事項
- トラブル時確認手順
- Phase 2 の構成と設計判断を説明する README

### 実装方針

- README を実装済みのコマンド、設定名、Stack 名、workflow 名に合わせる
- 初回構築、通常 deploy、実行確認、削除、トラブル対応を分けて記載する
- secret、token、password、実アカウント ID をリポジトリへ記載しない
- P2-1 から P2-9 の重複手順を整理し、README を再現の入口にする
- Phase 2 で扱わない機能を実装済みとして記載しない
- 未確認事項は確認済みとして記載しない
- P2-9 の GitHub Actions OIDC deploy が暫定手段であることを README に明記する
- Phase 2α で CodePipeline + CodeBuild へ移行し、GitHub Actions workflow を撤去する前提を README に明記する

### 除外範囲

- Phase 3 の外部提供 API
- `apps/api`
- API 用 ECS Service
- Phase 4 の PDF
- Phase 5 のメール、バッチ、Lambda
- 本番運用設計
- SSO
- 高度な監視・可観測性
- 新規業務機能
- Phase 2α の CodePipeline / CodeBuild 実装

### 完了条件

次の項目を実装結果と確認結果で説明できる。

- AWS dev 環境を CDK で構築できる
- `apps/web` 起動時に RDS へ Flyway migration と AWS dev seed を適用できる
- `apps/web` が ECS Fargate で動作する
- HTTP ALB 経由の `/actuator/health` が成功する
- HTTPS 入口が作成され、HTTPS 経由で `/actuator/health` を確認できる
- HTTP アクセスが HTTPS へ redirect される
- Cognito Hosted UI でログインできる
- Cognito `sub` と `users.cognito_sub` を突合できる
- DB 由来の権限セットで認可できる
- PLATFORM / TENANT App Client が分離されている
- PLATFORM_ADMIN が会社と初期 TENANT_MANAGER を作成できる
- TENANT_MANAGER が自社ユーザーと権限を管理できる
- CloudWatch Logs から業務操作、認証、認可、例外を調査できる
- GitHub Actions の `workflow_dispatch` から `apps/web` 起動時 migration と deploy を再現できる
- README に従って構築、確認、削除、再 deploy ができる
- Phase 2α で AWS ネイティブ CI/CD へ移行する対象と、GitHub Actions workflow の撤去方針を説明できる

### 確認方針

エージェントは、全自動テスト、Maven build、CDK build / synth、workflow 静的確認、ドキュメントと実装の整合、`git diff --check` を確認します。
ユーザーは、README に従った AWS dev の構築、ログイン、主要業務操作、ユーザー管理、ログ調査、GitHub Actions deploy、実行セッション Stack の削除を確認します。

### agent task 分割方針

P2-10 着手前に、機械的総合確認、AWS 実環境シナリオ、README 整理、Phase 2 完了記録の境界で agent task 分割案を提示します。
P2-10 では新規業務機能を追加しません。

## Phase 2α. CI/CD AWS ネイティブ化

### 目的

P2-9 の暫定 GitHub Actions OIDC deploy を、AWS 側で完結する CodePipeline + CodeBuild 構成へ移行します。
あわせて、Phase 2 で `apps/web` 起動時に実行していた Flyway migration を、migration 用 ECS Task として分離します。
既存 Phase 3 から Phase 5 の番号は変更せず、Phase 3 の外部提供 API より前に CI/CD を AWS ネイティブ化します。

### 前提フェーズ

- P2-10 が完了している
- GitHub Actions OIDC deploy で AWS dev への build、deploy、確認が再現できる
- Phase 2 の Stack、ECR、ECS、RDS、Logs、Domain の構成が README から再現できる

### 成果物

- CodePipeline
- CodeConnections による GitHub source 接続
- 品質チェック、Docker build、ECR push を行う CodeBuild project
- migration 用 ECS Task 定義
- migration 用 ECS Task の実行、完了待機、exitCode 確認を行う CodeBuild project
- deploy を行う CodeBuild project
- GitHub push を起点に AWS 側 pipeline が起動する構成
- GitHub Actions workflow の撤去
- CodePipeline / CodeBuild の README

### 実装方針

- GitHub を CodeConnections で CodePipeline へ直結する
- GitHub Actions を CodePipeline 起動トリガーにしない
- GitHub push を CodePipeline の source event として扱う
- CodeBuild で lint、test、CDK synth、CDK assertions、Docker build を実行する
- CodeBuild から ECR push を実行する
- migration 用 ECS Task を単発実行し、完了待機と exitCode 確認を行う
- migration 用 ECS Task が失敗した場合は AWS dev への deploy を進めない
- migration 用 ECS Task へ分離した後、AWS dev の `apps/web` 起動時 Flyway 自動実行を止める
- deploy 側の CodeBuild で AWS dev への反映と確認を行う
- ECS Service は CodeDeploy deployment controller にしない
- Blue/Green deploy と Canary deploy は作らない
- rolling deploy を前提にし、CDK deploy で管理できる構成にする
- Phase 2α 完了時に GitHub Actions workflow を完全撤去する

### 除外範囲

- Phase 3 の外部提供 API
- `apps/api`
- API 用 ECS Service
- CodeDeploy
- Blue/Green deploy
- Canary deploy
- 複雑な承認フロー
- GitHub Actions の恒久利用
- PR レビューゲート

### 完了条件

GitHub push から CodePipeline が起動する。
CodeBuild で品質チェック、Docker build、ECR push、AWS dev への deploy を実行できる。
migration 用 ECS Task の実行、完了待機、exitCode 確認を実行できる。
AWS dev の `apps/web` 起動時 Flyway 自動実行が止まっている。
GitHub Actions workflow が撤去され、CI/CD の説明が AWS 側に寄っている。
CodeDeploy、Blue/Green deploy、Canary deploy、PR レビューゲートを使っていないことを説明できる。

### 確認方針

エージェントは、pipeline 定義、CodeBuild buildspec、IAM 権限、CDK synth、workflow 撤去差分、README を確認します。
ユーザーは、CodeConnections 認可、GitHub push による pipeline 起動、CodeBuild ログ、AWS dev 反映結果を確認します。

### agent task 分割方針

Phase 2α 着手前に、CodeConnections / CodePipeline、CodeBuild 品質チェック、CodeBuild deploy、GitHub Actions 撤去、README と確認記録の境界で agent task 分割案を提示します。

## Phase 2 では扱わないもの

- `apps/api`
- API 用 ECS Service
- 外部提供 API
- PDF 出力
- PDF 生成履歴
- PDF 再ダウンロード
- S3 への PDF 保存
- メール通知
- メール送信バッチ
- ECS タスク型業務バッチ
- CSV 取込 Lambda
- ジョブ管理
- 外部連携同期
- SQS
- Cognito Trigger
- Pre Token Generation
- 初回ログイン時の `users` 自動作成
- `users` 突合失敗時の自動復旧
- Cognito / `users` 再同期・復旧導線
- ユーザー無効化
- Cognito 側ユーザー属性変更
- 本番運用設計
- SSO
- ポータルサイト
- AI チャットボット
- RAG
- VPC Endpoint
- Container Insights
- CodeDeploy
- Blue/Green deploy
- Canary deploy
- GitHub Actions の恒久利用
- PR レビューゲート
- CloudWatch Alarm
- Observability Dashboard
- OpenTelemetry
- 分散トレーシング
