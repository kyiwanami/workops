# WorkOps

WorkOps は、申請管理と資産管理を扱う Spring Boot + Thymeleaf の業務アプリケーション MVP です。
MVP では、ローカル MySQL 上で申請、資産、会社別マスタ、権限、業務操作ログを確認できます。

## MVP 技術スタック

- Java 25 LTS
- Maven Wrapper
- Spring Boot 4.0.6
- Spring Security
- Thymeleaf
- Bootstrap 5.3.8 CDN
- MyBatis Spring Boot Starter 4.0.1
- Flyway
- MySQL 9.7.0
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
│  └─ cdk/        # 後続 Phase 用の予約領域
├─ compose.yaml
└─ README.md
```

MVP の実装対象は `apps/web` です。
`apps/api`、`apps/batch`、`infra/cdk` は後続 Phase 用の置き場で、MVP では実装しません。

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

Spring Boot 起動時に Flyway が `apps/web/src/main/resources/db/migration` の migration を適用します。

| migration | 内容 |
| --- | --- |
| `V1__create_mvp_schema.sql` | MVP DB スキーマ |
| `V2__insert_local_seed.sql` | 2社、ユーザー、権限、共通/汎用マスタ |
| `V3__insert_request_sample_seed.sql` | 申請サンプル |
| `V4__insert_asset_sample_seed.sql` | 資産サンプル |
| `V5__platform_admin_local_prerequisite.sql` | local PLATFORM_ADMIN 前提 |

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
- 会社管理: 現行実装では PLATFORM_ADMIN による会社登録と会社別初期マスタ投入を確認できます。M9 追加スコープとして会社一覧、詳細、編集、論理削除、削除済み表示を扱います。
- 部署管理: PLATFORM_ADMIN による会社指定管理、TENANT_MANAGER による自社管理
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
| `/admin/companies/new` | 会社登録 |
| `/admin/companies/1/departments` | PLATFORM_ADMIN 向け部署管理 |
| `/departments` | TENANT_MANAGER 向け部署管理 |
| `/auth/claims` | 認証情報確認 |
| `/auth/authorization/manager` | 管理者認可確認 |

## テスト

Docker Desktop が起動している状態で実行します。
M9 時点のテストには、通常の Service テストと Testcontainers MySQL を使う DB / Mapper 統合テストが含まれます。

```powershell
cd C:\git\workops\apps\web
.\mvnw.cmd test
```

## Cognito 最小接続確認

M3 では Cognito OAuth2 Login の最小接続を確認しています。
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

`.env.local` に書くキーは次の4つです。

```text
AWS_REGION
WORKOPS_COGNITO_USER_POOL_ID
WORKOPS_COGNITO_CLIENT_ID
WORKOPS_COGNITO_REDIRECT_URI
```

Cognito issuer URI は `AWS_REGION` と `WORKOPS_COGNITO_USER_POOL_ID` からアプリ側で構成します。

Cognito 本格連携、PLATFORM / TENANT App Client 分離、Cognito Trigger、Pre Token Generation、CDK による Cognito 構築、AWS dev 環境デプロイは後続 Phase で扱います。

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
