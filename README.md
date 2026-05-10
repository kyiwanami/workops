# WorkOps

WorkOps は、申請管理と資産管理を扱う業務アプリケーションのMVPです。
M1時点では、Maven / Spring Boot / Thymeleaf / MyBatis / Docker Compose MySQL の最小構成だけを用意しています。

## M1 時点の技術スタック

- Java 25 LTS
- Maven Wrapper
- Spring Boot 4.0.6
- Thymeleaf
- Bootstrap 5.3.8 CDN
- MyBatis Spring Boot Starter 4.0.1
- Flyway
- MySQL 9.7.0
- Docker Compose

## 生成方針

Spring Bootアプリ本体は、公式 Spring Initializr 相当の最小構成から `apps/server` に生成しています。

次の完成済みサンプルは流用していません。

- CRUDサンプル
- JPA Quickstart
- Spring Securityログインサンプル
- 会社や個人のテンプレート

## リポジトリ構成

```text
workops/
├─ apps/
│  └─ server/
│     ├─ pom.xml
│     ├─ mvnw
│     ├─ mvnw.cmd
│     └─ src/
├─ docs/
│  └─ implementation/
├─ infra/
│  └─ cdk/
├─ compose.yaml
└─ README.md
```

## 前提ツール

- Java 25 LTS
- Docker Desktop
- PowerShell

Mavenは `apps/server/mvnw.cmd` が取得するため、ローカルにMavenを直接インストールする必要はありません。

## MySQL 起動

PowerShellでリポジトリルートへ移動します。

```powershell
cd C:\git\workops
docker compose up -d workops-mysql
docker compose ps
```

`workops-mysql` が `healthy` になればMySQL起動は完了です。

## MySQL 接続情報

A5M2などのDBクライアントから接続する場合は、次を使います。

```text
種類: MySQL
ホスト: localhost
ポート: 3306
データベース: workops
ユーザー: workops
パスワード: workops
```

JDBC URL形式:

```text
jdbc:mysql://localhost:3306/workops?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Tokyo
```

## DB 再構築確認

local DBを空から作り直し、FlywayでDDLとseedを再適用する場合は次を実行します。

```powershell
cd C:\git\workops
docker compose down -v
docker compose up -d workops-mysql
docker compose ps

cd C:\git\workops\apps\server
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=local"
```

Spring Bootが起動すれば、`V1__create_mvp_schema.sql` と `V2__insert_local_seed.sql` が適用されています。

## テスト

```powershell
cd C:\git\workops\apps\server
.\mvnw.cmd test
```

## Spring Boot 起動

```powershell
cd C:\git\workops\apps\server
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=local"
```

PowerShellでは `-Dspring-boot.run.profiles=local` を引用符で囲んでください。

## Cognito 接続値のローカル管理

Cognito 接続値はルート `.env.local` に書きます。
`.env.local` は git 管理しません。
キー名とダミー値は `.env.example` を参照してください。

```powershell
cd C:\git\workops
Copy-Item .env.example .env.local
notepad .env.local
```

`.env.local` に書くキーは次の3つです。

```text
WORKOPS_COGNITO_ISSUER_URI
WORKOPS_COGNITO_CLIENT_ID
WORKOPS_COGNITO_REDIRECT_URI
```

Codex はローカルの `.env.local` を参照できますが、`.env.local` は git に載せません。

## Spring Boot Cognito 起動

`.env.local` 作成後、Cognito接続を確認する場合は `local` profile を付けずに起動します。

```powershell
cd C:\git\workops\apps\server
.\mvnw.cmd spring-boot:run
```

通常のローカル開発では `local` profile を付けるため、Cognitoへリダイレクトしません。
再度Cognito接続を確認したい場合だけ、`local` profile を外して起動します。

MVP / M3 の OAuth2 scope は `openid` と `email` だけを使います。
`profile` と `aws.cognito.signin.user.admin` はM3時点では不要です。

## HTTP 確認

別のPowerShellで確認します。

```powershell
curl.exe -i http://localhost:8080/
```

ステータスコードだけ確認する場合は次を使います。

```powershell
curl.exe -s -o NUL -w "%{http_code}" http://localhost:8080/
```

`200` が返ればM1時点の起動確認は完了です。
ブラウザでは次のURLを開きます。

```text
http://localhost:8080/
```

## 停止

Spring Bootは起動中のPowerShellで `Ctrl + C` を押して停止します。

MySQLを停止する場合はリポジトリルートで次を実行します。

```powershell
cd C:\git\workops
docker compose stop workops-mysql
```

MySQLのデータボリュームも削除する場合は次を実行します。

```powershell
docker compose down -v
```

## M2 時点で未実装の範囲

- 申請管理
- 資産管理
- マスタ管理
- 業務機能向け認証・認可
- Cognito claim とDBユーザーの突合
- AWS連携
- CDK実装
- GitHub Actions deploy

## 実装ドキュメント

- `docs/implementation/README.md`
- `docs/implementation/mvp/phases.md`
- `docs/implementation/mvp/agent-tasks/`
