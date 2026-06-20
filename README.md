# WorkOps

WorkOps は、申請管理、資産管理、会社・部署管理、ユーザー管理を扱う Spring Boot + Thymeleaf の業務アプリケーション MVP です。
MVP では、ローカル MySQL 上で申請、資産、会社別マスタ、ユーザー、権限、業務操作ログを確認できます。

## MVP 技術スタック

- Java 25 LTS
- Maven Wrapper
- Spring Boot 4.0.6
- Spring Security
- Thymeleaf
- Bootstrap 5.3.8 CDN
- MyBatis Spring Boot Starter 4.0.1
- Flyway
- MySQL 8.4 LTS
- Docker Compose
- JUnit / Mockito / Testcontainers MySQL

## リポジトリ構成

```text
workops/
├─ apps/
│  ├─ web/        # MVP の Spring Boot + Thymeleaf Web アプリ
│  ├─ api/        # 後続 Phase 用の予約領域
│  └─ batch/      # 後続 Phase 用の予約領域
├─ docs/
│  └─ implementation/
├─ infra/
│  └─ cdk/        # Phase 2 AWS dev 基盤の CDK v2 プロジェクト
├─ compose.yaml
└─ README.md
```

MVP の実装対象は `apps/web` です。
`apps/api`、`apps/batch` は後続 Phase 用の置き場で、MVP では実装しません。
`infra/cdk` は Phase 2 で AWS dev 基盤を管理します。

## 前提ツール

- Java 25 LTS
- Docker Desktop
- PowerShell

Maven は `apps/web/mvnw.cmd` が取得するため、ローカルに Maven を直接インストールする必要はありません。

## ローカル DB 起動

PowerShell でリポジトリルートへ移動し、MySQL を起動します。

```powershell
cd C:\git\workops
docker compose up -d workops-mysql
docker compose ps
```

`workops-mysql` が `healthy` になれば MySQL 起動は完了です。

接続情報は次の通りです。

```text
種類: MySQL
ホスト: localhost
ポート: 3306
データベース: workops
ユーザー: workops
パスワード: workops
```

JDBC URL:

```text
jdbc:mysql://localhost:3306/workops?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Tokyo
```

## Flyway / seed

Spring Boot 起動時に Flyway が profile 別 locations の migration と seed を適用します。
通常のローカル再現では `local` profile を使い、`db/migration`、`db/seed/common`、`db/seed/local` を読み込みます。

| migration | 内容 |
| --- | --- |
| `V1__create_mvp_schema.sql` | MVP DB スキーマ |
| `V2__allow_platform_users.sql` | PLATFORM user 用の schema 前提 |
| `V3__insert_demo_companies_departments.sql` | デモ会社と部署 |
| `V4__insert_permission_sets.sql` | 権限セット |
| `V5__insert_business_masters.sql` | 共通マスタと会社別業務マスタ |
| `V6__insert_users.sql` | profile 別ユーザー seed |
| `V7__insert_asset_sample_seed.sql` | 資産サンプル |
| `V8__insert_request_sample_seed.sql` | 申請サンプル |

`local` profile の `V6__insert_users.sql` は固定 `cognito_sub` を持ちます。
AWS dev 用の `dev` profile は `db/seed/aws-dev/V6__insert_users.sql` を読み込み、`users.cognito_sub` は全件 `NULL` にします。
local と AWS dev の `V6` は同時に読み込まない locations なので、各 profile の適用順はどちらも `V1` から `V8` まで連続します。

local DB を空から作り直す場合は、MySQL ボリュームを削除してから起動します。

```powershell
cd C:\git\workops
docker compose down -v
docker compose up -d workops-mysql
docker compose ps
```

## Spring Boot 起動

通常の MVP 再現では `local` profile を使います。
`local` profile では Cognito にリダイレクトせず、DB seed の local 疑似ユーザーを現在ユーザーとして扱います。

```powershell
cd C:\git\workops\apps\web
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=local"
```

PowerShell では `-Dspring-boot.run.profiles=local` を引用符で囲んでください。

ブラウザでは次の URL を開きます。

```text
http://localhost:8080/
```

HTTP ステータスだけ確認する場合は、別の PowerShell で実行します。

```powershell
curl.exe -s -o NUL -w "%{http_code}" http://localhost:8080/
```

`200` が返れば起動確認は完了です。

## local 疑似ユーザー

local profile の既定ユーザーは、北浜精密機器株式会社の管理者です。

```text
WORKOPS_LOCAL_COGNITO_SUB=00000000-0000-0000-0000-000000000003
```

seed には次の疑似ユーザーがあります。

| cognito_sub | 会社 | ユーザー | 権限 |
| --- | --- | --- | --- |
| `00000000-0000-0000-0000-000000000000` | なし | platform-admin | PLATFORM_ADMIN |
| `00000000-0000-0000-0000-000000000001` | 北浜精密機器株式会社 | kthm-viewer | TENANT_VIEWER |
| `00000000-0000-0000-0000-000000000002` | 北浜精密機器株式会社 | kthm-editor | TENANT_EDITOR |
| `00000000-0000-0000-0000-000000000003` | 北浜精密機器株式会社 | kthm-manager | TENANT_MANAGER |
| `00000000-0000-0000-0000-000000000004` | 青葉ケアサービス株式会社 | aoba-viewer | TENANT_VIEWER |
| `00000000-0000-0000-0000-000000000005` | 青葉ケアサービス株式会社 | aoba-editor | TENANT_EDITOR |
| `00000000-0000-0000-0000-000000000006` | 青葉ケアサービス株式会社 | aoba-manager | TENANT_MANAGER |

ユーザーを切り替える場合は、Spring Boot を停止し、同じ PowerShell セッションで環境変数を設定してから起動します。

```powershell
cd C:\git\workops\apps\web
$env:WORKOPS_LOCAL_COGNITO_SUB = "00000000-0000-0000-0000-000000000002"
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=local"
```

## MVP で確認できる業務操作

- 申請管理: 一覧、詳細、下書き作成、編集、提出、取下げ、承認、却下、差戻し
- 資産管理: 一覧、検索、詳細、登録、編集、ステータス変更、論理削除
- マスタ管理: 申請種別と資産分類の一覧、登録、編集、論理削除、復活、削除済み表示
- 会社管理: PLATFORM_ADMIN による会社一覧、詳細、登録、編集、論理削除、削除済み表示、会社別初期マスタ投入
- 部署管理: PLATFORM_ADMIN による会社指定管理、TENANT_MANAGER による自社管理
- ユーザー管理: PLATFORM_ADMIN による全ユーザー管理、TENANT_MANAGER による自社ユーザー管理、会社作成時の初期 TENANT_MANAGER 作成
- 権限: 閲覧者、編集者、管理者の操作可否
- 会社境界: `company_id` による他社データの参照・更新防止
- 業務操作ログ: 申請、資産、マスタ操作の成功・拒否ログ

詳細な確認シナリオは次を参照してください。

- `docs/implementation/mvp/verification-scenario.md`

## 主要 URL

| URL | 内容 |
| --- | --- |
| `/` | ホーム |
| `/requests` | 申請一覧 |
| `/assets` | 資産一覧 |
| `/masters/request-types` | 申請種別マスタ |
| `/masters/asset-categories` | 資産分類マスタ |
| `/admin/companies` | PLATFORM_ADMIN 向け会社管理 |
| `/admin/companies/new` | 会社登録 |
| `/admin/companies/{companyId}` | 会社詳細 |
| `/admin/companies/{companyId}/edit` | 会社編集 |
| `/admin/companies/1/departments` | PLATFORM_ADMIN 向け部署管理 |
| `/departments` | TENANT_MANAGER 向け部署管理 |
| `/admin/users` | PLATFORM_ADMIN 向けユーザー管理 |
| `/admin/users/new` | PLATFORM_ADMIN 向けユーザー作成 |
| `/admin/users/{userId}` | PLATFORM_ADMIN 向けユーザー詳細 |
| `/admin/users/{userId}/edit` | PLATFORM_ADMIN 向けユーザー編集 |
| `/users` | TENANT_MANAGER 向け自社ユーザー管理 |
| `/users/new` | TENANT_MANAGER 向け自社ユーザー作成 |
| `/users/{userId}` | TENANT_MANAGER 向け自社ユーザー詳細 |
| `/users/{userId}/edit` | TENANT_MANAGER 向け自社ユーザー編集 |
| `/auth/claims` | 認証情報確認 |
| `/auth/authorization/manager` | 管理者認可確認 |

## テスト

Docker Desktop が起動している状態で実行します。
MVP のテストには、通常の Service テストと Testcontainers MySQL を使う DB / Mapper 統合テストが含まれます。

```powershell
cd C:\git\workops\apps\web
.\mvnw.cmd test
```

## GitHub Actions OIDC deploy

Phase 2 P2-9 では、GitHub Actions から AWS dev へ接続するために OIDC を使います。
長期 AWS credential は GitHub Secrets に置きません。

初回だけ、ローカルの AWS profile から `cdk:deploy-app` entrypoint で `DeployStack` を手動 deploy し、GitHub Actions 用 role を作成します。

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
npm run cdk:deploy-app -- diff DeployStack
npm run cdk:deploy-app -- deploy DeployStack
```

`DeployStack` deploy 後、CloudFormation Output の `githubActionsDeployRoleArn` を GitHub Environment `dev` の variable `AWS_ROLE_ARN` に設定します。
同じ GitHub Environment `dev` に `AWS_REGION` も設定します。

P2-9-01 の GitHub Actions は次の役割です。

| workflow | trigger | 内容 |
| --- | --- | --- |
| `ci.yml` | PR / main push | Java test、Docker build、CDK build / test / synth |
| `infra-dev.yml` | `ci.yml` 成功後 | 非 runtime / 維持対象 Stack の AWS dev deploy |
| `app-deploy-dev.yml` | 手動実行 | Docker image push と課金 runtime Stack の AWS dev deploy |

`infra-dev.yml` は GitHub Environment `dev` を使い、`FoundationStack`、`SecretStack`、`ConfigStack`、`IdentityStack`、`RegistryStack`、`LogsStack` だけを deploy します。
`DeployStack` は GitHub Actions から更新しません。
`DataStack`、`EgressStack`、`EdgeStack`、`AppRuntimeStack` の課金 runtime deploy は `app-deploy-dev.yml` の責務です。

`app-deploy-dev.yml` は GitHub Actions の手動 workflow です。
対象 commit の `ci.yml` が成功していることを確認してから、main branch で `confirm_runtime_deploy` を true にして実行します。
この workflow は RDS、NAT Gateway、ECS、ALB、CloudFront を作成または更新する課金 runtime 操作です。
docs のみの変更では `app-deploy-dev.yml` を実行しません。

`app-deploy-dev.yml` は `apps/web` の Docker image を build し、ECR repository `workops-dev-web` に commit SHA tag だけを push します。
`dev` tag と `latest` tag は使いません。
その後、`cdk:runtime` entrypoint で `DataStack`、`EgressStack`、`EdgeStack`、`AppRuntimeStack` の順に `cdk diff` と `cdk deploy --require-approval never` を実行し、ECS service stable と CloudFront HTTPS `/actuator/health` を確認します。

Workflow の `uses: owner/action@<sha>` は、外部 action を公開 Git commit SHA で固定している指定です。
この値は secret ではなく、GitHub 上の公開 commit ID です。

実 account ID、role ARN、SSO role ARN、SSO user、credential、public IP、実 Cognito `sub` は git 管理文書へ記録しません。

## Cognito Hosted UI 接続確認

Phase 2 では Cognito Hosted UI、PLATFORM / TENANT App Client 分離、WorkOps 画面からのログアウト導線を確認します。
M10 ではユーザー作成用に AWS SDK for Java 2.x の Cognito `AdminCreateUser` 呼び出し境界を追加しています。
通常の MVP ローカル再現では `local` profile を使うため、Cognito 接続値は不要です。

Cognito 接続を再確認する場合だけ、ルート `.env.local` に接続値を用意し、`local` profile を付けずに起動します。
`.env.local` は git 管理しません。

```powershell
cd C:\git\workops
Copy-Item .env.example .env.local
notepad .env.local

cd C:\git\workops\apps\web
.\mvnw.cmd spring-boot:run
```

`.env.local` に書く主なキーは次です。

```text
AWS_REGION
WORKOPS_COGNITO_USER_POOL_ID
WORKOPS_COGNITO_PLATFORM_CLIENT_ID
WORKOPS_COGNITO_TENANT_CLIENT_ID
WORKOPS_COGNITO_HOSTED_UI_DOMAIN_BASE_URL
WORKOPS_COGNITO_LOGOUT_URI
WORKOPS_COGNITO_PLATFORM_REDIRECT_URI
WORKOPS_COGNITO_TENANT_REDIRECT_URI
```

Cognito issuer URI は `AWS_REGION` と `WORKOPS_COGNITO_USER_POOL_ID` からアプリ側で構成します。
PLATFORM / TENANT の redirect URI は、それぞれ `/login/oauth2/code/platform` と `/login/oauth2/code/tenant` を使います。
logout URI は Cognito App Client の allowed sign-out URL と一致させ、AWS dev では CloudFront HTTPS URL の `/login` を使います。

Cognito で実際に招待メールを送るユーザー作成 E2E は、実行者が AWS 認証情報と実メールアドレスを用意して手動確認します。
Cognito Hosted UI のブラウザ操作、CloudFront 経由の login / logout、実 Cognito `sub` と `users` の突合確認はユーザー確認として扱います。
実 account ID、credential、実 Cognito `sub`、public IP は README や git 管理文書へ記録しません。
Cognito Trigger、Pre Token Generation、SSO、テナント別ログイン画面、本格ブランドデザインは Phase 2 では扱いません。

## 停止

Spring Boot は起動中の PowerShell で `Ctrl + C` を押して停止します。

MySQL を停止する場合はリポジトリルートで実行します。

```powershell
cd C:\git\workops
docker compose stop workops-mysql
```

MySQL のデータボリュームも削除する場合は次を実行します。

```powershell
docker compose down -v
```

## 実装ドキュメント

- `docs/implementation/README.md`
- `docs/implementation/mvp/phases.md`
- `docs/implementation/mvp/verification-scenario.md`
- `docs/implementation/mvp/agent-tasks/`
