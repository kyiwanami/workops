# セットアップと実行

ローカルではMySQLと疑似ユーザーで業務機能を確認できます。AWS devの構築と認証接続の手順も記載しています。

[README](../README.md) · [主要機能](key-features.md) · [利用例・デモ](example-or-demo.md) · [テスト](testing.md)

## 前提ツール

- Java 25 LTS
- Docker Desktop
- Apache Maven
- PowerShell

`apps/web`はMaven Wrapperを使います。`db/`のFlyway実行はMaven Wrapperを置かず、ローカルまたはCodeBuildの`mvn`を使います。

## ローカルDB起動

PowerShellでリポジトリルートへ移動し、MySQLを起動します。

```powershell
cd C:\git\workops
docker compose up -d workops-mysql
docker compose ps
```

`workops-mysql`が`healthy`になればMySQL起動は完了です。

接続情報は次の通りです。

```text
種類: MySQL
ホスト: localhost
ポート: 3306
データベース: workops
ユーザー: workops
パスワード: compose.yaml の MYSQL_PASSWORD を参照
```

JDBC URL

```text
jdbc:mysql://localhost:3306/workops?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Tokyo
```

## Flyway / seed

DBマイグレーションは`db/pom.xml`のFlyway Mavenプラグインで実行します。
通常のローカル再現では`local`プロファイルを使い、`db/migration`、`db/seed/common`、`db/seed/local`を読み込みます。
パイプラインのRunMigrationでは`dev`プロファイルを使い、`db/migration`、`db/seed/common`、`db/seed/dev`を読み込みます。

| migration | 内容 |
| --- | --- |
| `V1__create_mvp_schema.sql` | MVP DBスキーマ |
| `V2__allow_platform_users.sql` | PLATFORMユーザー用のスキーマ設定 |
| `V3__insert_demo_companies_departments.sql` | デモ会社と部署 |
| `V4__insert_permission_sets.sql` | 権限セット |
| `V5__insert_business_masters.sql` | 共通マスタと会社別業務マスタ |
| `V6__insert_users.sql` | プロファイル別のユーザーseed |
| `V7__insert_asset_sample_seed.sql` | 資産サンプル |
| `V8__insert_request_sample_seed.sql` | 申請サンプル |

`local`プロファイルの`V6__insert_users.sql`は固定`cognito_sub`を持ちます。
AWS dev用の`dev`プロファイルは`db/seed/dev/V6__insert_users.sql`を読み込み、`users.cognito_sub`は全件`NULL`にします。
localとAWS devの`V6`は同時に読み込まない設定のため、各プロファイルの適用順はどちらも`V1`から`V8`まで連続します。

local DBを空から作り直す場合は、MySQLボリュームを削除してから起動します。

```powershell
cd C:\git\workops
docker compose down -v
docker compose up -d workops-mysql
docker compose ps
```

local DBへmigrationとseedを適用します。

```powershell
cd C:\git\workops\db
$env:WORKOPS_DB_URL = "jdbc:mysql://localhost:3306/workops"
$env:WORKOPS_DB_USERNAME = "workops"
$env:WORKOPS_DB_PASSWORD = "<compose.yamlのMYSQL_PASSWORD>"
mvn -Plocal flyway:migrate "-Dflyway.url=$env:WORKOPS_DB_URL" "-Dflyway.user=$env:WORKOPS_DB_USERNAME" "-Dflyway.password=$env:WORKOPS_DB_PASSWORD"
```

適用状況だけ確認する場合は`flyway:info`を使います。

```powershell
mvn -Plocal flyway:info "-Dflyway.url=$env:WORKOPS_DB_URL" "-Dflyway.user=$env:WORKOPS_DB_USERNAME" "-Dflyway.password=$env:WORKOPS_DB_PASSWORD"
```

## Spring Boot起動

通常のローカル再現では`local`プロファイルを使います。
`local`プロファイルではCognitoにリダイレクトせず、DBのseedで用意したlocalの疑似ユーザーを現在ユーザーとして扱います。
起動前に`db/`のFlyway Maven手順でlocal DBへmigrationとseedを適用してください。

```powershell
cd C:\git\workops\apps\web
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=local"
```

PowerShellでは`-Dspring-boot.run.profiles=local`を引用符で囲んでください。

ブラウザでは次のURLを開きます。

```text
http://localhost:8080/
```

HTTPステータスだけ確認する場合は、別のPowerShellで実行します。

```powershell
curl.exe -s -o NUL -w "%{http_code}" http://localhost:8080/
```

`200`が返れば起動確認は完了です。

## local疑似ユーザー

localプロファイルの既定ユーザーは、北浜精密機器株式会社の管理者です。

```text
WORKOPS_LOCAL_COGNITO_SUB=00000000-0000-0000-0000-000000000003
```

seedには次の疑似ユーザーがあります。

| cognito_sub | 会社 | ユーザー | 権限 |
| --- | --- | --- | --- |
| `00000000-0000-0000-0000-000000000000` | なし | platform-admin | PLATFORM_ADMIN |
| `00000000-0000-0000-0000-000000000001` | 北浜精密機器株式会社 | kthm-viewer | TENANT_VIEWER |
| `00000000-0000-0000-0000-000000000002` | 北浜精密機器株式会社 | kthm-editor | TENANT_EDITOR |
| `00000000-0000-0000-0000-000000000003` | 北浜精密機器株式会社 | kthm-manager | TENANT_MANAGER |
| `00000000-0000-0000-0000-000000000004` | 青葉ケアサービス株式会社 | aoba-viewer | TENANT_VIEWER |
| `00000000-0000-0000-0000-000000000005` | 青葉ケアサービス株式会社 | aoba-editor | TENANT_EDITOR |
| `00000000-0000-0000-0000-000000000006` | 青葉ケアサービス株式会社 | aoba-manager | TENANT_MANAGER |

ユーザーを切り替える場合は、Spring Bootを停止し、同じPowerShellセッションで環境変数を設定してから起動します。

```powershell
cd C:\git\workops\apps\web
$env:WORKOPS_LOCAL_COGNITO_SUB = "00000000-0000-0000-0000-000000000002"
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=local"
```

業務機能は[主要機能](key-features.md)、操作シナリオは[利用例・デモ](example-or-demo.md)、自動テストの方法と結果は[テスト](testing.md)を参照してください。

## AWSセットアップと実行

AWS devへのCI/CDにはCodePipeline V2、CodeBuild、CDK Pipelinesを使用します。
GitHub Actionsのworkflowは使用しません。

AWS devのデプロイ単位は次の通りです。

| 区分 | 実行入口 | 対象 |
| --- | --- | --- |
| 通常のCDKデプロイ | ローカル`npx cdk deploy '*'` | `DependencyStack`, `PipelineStack`, Pipeline管理対象Stack |
| 初回の先行デプロイ例外 | ローカル`npx cdk deploy DependencyStack PipelineStack` | `DependencyStack`, `PipelineStack`, CodeConnection, アーティファクトバケット |
| パイプライン実行 | CodePipeline | Source, Synth, BuildAndTest, DeployFoundation, BuildWebImage, ManualApproval, DeployRuntimeInfrastructure, RunMigration, DeployAppRuntime |
| 実行環境の確認 | パイプラインの実行基盤デプロイ工程 | `DataStack`, `EgressStack`, `WebIngressStack`, `WebDeliveryStack`, `WebAclStack`, `AppRuntimeStack` |

AWS実環境のdeploy、Cognito Hosted UIのブラウザ操作、CloudFront経由の画面操作、CloudWatch Logs確認はユーザー確認として扱います。
実account ID、role ARN、SSO role ARN、SSO user、credential、CloudFront domain、public IP、実Cognito `sub`はREADMEやgit管理文書へ記録しません。

### AWSの前提ツール

- AWS CLI
- AWS profile
- Docker Desktop
- Node.js 24
- GitHub CodeConnection認可

AWS CLIを使う前に、PowerShellでAWS profileとregionを明示し、認証情報の環境変数を削除してから認証先を確認します。

```powershell
Remove-Item Env:AWS_ACCESS_KEY_ID -ErrorAction SilentlyContinue
Remove-Item Env:AWS_SECRET_ACCESS_KEY -ErrorAction SilentlyContinue
Remove-Item Env:AWS_SESSION_TOKEN -ErrorAction SilentlyContinue

$env:WORKOPS_SOURCE_BRANCH = "dev"
$env:AWS_PROFILE = "<aws-profile>"
$env:AWS_SDK_LOAD_CONFIG = "1"
$env:AWS_REGION = "<aws-region>"
$env:AWS_DEFAULT_REGION = $env:AWS_REGION
$env:CDK_DEFAULT_ACCOUNT = (aws sts get-caller-identity --region $env:AWS_REGION --query Account --output text)
$env:CDK_DEFAULT_REGION = $env:AWS_REGION

aws sts get-caller-identity --region $env:AWS_REGION
```

### CDK deploy

CloudFront用WAFとCDK Pipelinesのcross-region supportを使うため、AWS devの事前準備では`ap-northeast-1`と`us-east-1`の両方をCDK bootstrap済みにします。
`us-east-1` bootstrapはWorkOps appのsynthを不要にするため、`cdk.json`のないdirectoryからCDK CLIを直接呼びます。

```powershell
cd C:\tmp
& 'C:\git\workops\infra\cdk\node_modules\.bin\cdk.cmd' bootstrap "aws://<aws-account-id>/us-east-1" --profile $env:AWS_PROFILE
```

通常はローカルのAWS profileから単一のCDKエントリーポイントで全Stackをdiff / deployします。
CodeConnectionのGitHub OAuth認可とManualApprovalはユーザー操作です。
`cdk deploy`は課金対象や外部連携を作成するため、実行前にAWS account、region、既存stack状態、`cdk diff`を確認します。

```powershell
cd C:\git\workops\infra\cdk
$env:GITHUB_REPOSITORY = "<owner>/<repo>"
$env:WORKOPS_OPS_NOTIFICATION_EMAIL = "<notification-email>"
$env:WORKOPS_IMAGE_TAG = "test-sha"

npx cdk diff '*' --profile $env:AWS_PROFILE
npx cdk deploy '*' --profile $env:AWS_PROFILE
```

TTYがない実行環境で、セキュリティに関わる更新の承認が必要になった場合だけ、ユーザーが`--require-approval never`の使用を明示してから`npx cdk deploy '*' --profile $env:AWS_PROFILE --require-approval never`を実行します。

初回bootstrap直後だけ`DependencyStack`と`PipelineStack`の先行deployを例外として認めます。

```powershell
cd C:\git\workops\infra\cdk
$env:GITHUB_REPOSITORY = "<owner>/<repo>"
$env:WORKOPS_OPS_NOTIFICATION_EMAIL = "<notification-email>"
$env:WORKOPS_IMAGE_TAG = "test-sha"

npx cdk diff DependencyStack PipelineStack --profile $env:AWS_PROFILE
npx cdk deploy DependencyStack PipelineStack --profile $env:AWS_PROFILE
```

CloudFormation Outputの実値、CodeConnection ARN、実account IDはgit管理文書へ記録しません。

### CodePipeline

CodePipelineは次の順序で実行します。

| stage | 内容 |
| --- | --- |
| Source | CodeConnectionsで`WORKOPS_SOURCE_BRANCH`と同じGit branchを取得 |
| Synth | CDK Pipelines synth |
| BuildAndTest | Java / CDK TypeScript品質ゲート |
| DeployFoundation | `FoundationStack`、`LogsStack`、`IdentityStack`、`RegistryStack`、`DataPauseStack`をdeploy |
| BuildWebImage | WebイメージをコミットSHAのタグでbuild / scan / push |
| ManualApproval | 課金が始まる手前の手動承認 |
| DeployRuntimeInfrastructure | `DataStack`、`EgressStack`、`WebAclStack`、`WebIngressStack`、`MigrationRunnerStack`、`WebDeliveryStack`をdeploy |
| RunMigration | MigrationRunnerStackのCodeBuildプロジェクトで`cd db; mvn -Pdev flyway:migrate`を実行 |
| DeployAppRuntime | ECSのネイティブBlue/GreenでWeb実行環境をdeploy |

RunMigrationはRDSの起動 / 停止を行いません。RDSが停止中の場合、MigrationRunnerStackのCodeBuildはDB接続で失敗するため、AWS dev確認時はRDSインスタンスの状態を先に確認します。

CloudFront / VPC Origin / Cognito URL updaterは`WebDeliveryStack`に分離します。更新に時間がかかり、変更頻度が低い部分を分けるため、ALB側の`WebIngressStack`と分けています。

スタック間の値の受け渡しにはSSM Parameter Storeを使います。
実環境の確認に直接使う値として、`cloudFrontHttpsUrl`と`rdsConsoleCloudShellSecurityGroupId`だけを残します。
RDSエンドポイント、secret ARN、ALB DNS name、listener ARN、CloudFront domain nameは、SSMの連携仕様、Secrets Manager、AWS CLI、または命名規約から確認します。Outputsは参照元にしません。

ローカルでCDK synthだけ確認する場合は次を使います。

```powershell
cd C:\git\workops\infra\cdk
$env:WORKOPS_SOURCE_BRANCH = "dev"
$env:GITHUB_REPOSITORY = "<owner>/<repo>"
$env:WORKOPS_OPS_NOTIFICATION_EMAIL = "<notification-email>"
$env:WORKOPS_IMAGE_TAG = "test-sha"
npx cdk synth --quiet --profile $env:AWS_PROFILE
```

パイプライン実行、ECRへのpush、RunMigrationのCodeBuild実行、CloudFront経由の画面操作の結果は[テスト](testing.md)に記載しています。

実account ID、role ARN、SSO role ARN、SSO user、credential、CloudFront domain、public IP、実Cognito `sub`はgit管理文書へ記録しません。

### ALB再作成時のCloudFront接続更新

CloudFrontを保持してALBを撤去・再作成すると、VPC Originが参照するALB ARNも変わります。既存のDistributionに関連付けたままVPC Originを更新すると、AWSが409で拒否します。配信停止中に、次の順で接続を更新します。

1. `GetDistributionConfig`で設定とETagを取得し、復元用の設定を保持する。
2. `UpdateDistribution`で対象Originの`VpcOriginConfig`を一時的な`CustomOriginConfig`へ置き換え、Distributionが`Deployed`になるまで待つ。ALBの公開設定やSecurity Groupは変更しない。
3. `WebDeliveryStack`をCDKでdeployし、VPC OriginのALB参照を更新する。
4. DistributionのOriginをVPC Originへ再接続し、`Deployed`、VPC OriginのALB参照、CloudFront経由の画面表示を確認する。CDKで再接続されない場合は、保持した設定のOriginを新ALBへ合わせ、最新ETagを使って`UpdateDistribution`で復元する。

一時的なCustom Originは内部ALBへの到達経路を持たないため、配信の復旧はVPC Originへの再接続後に確認します。AWSの[既存VPC Originの更新手順](https://docs.aws.amazon.com/AmazonCloudFront/latest/DeveloperGuide/private-content-vpc-origins.html)に沿った操作です。

## Cognito Hosted UI接続確認

Cognito Hosted UI、PLATFORM / TENANT App Client分離、WorkOps画面からのログアウト導線を確認します。
ユーザー作成にはAWS SDK for Java 2.xのCognito `AdminCreateUser`呼び出し境界を追加しています。
通常のローカル再現では`local`プロファイルを使うため、Cognito接続値は不要です。

Cognito接続を再確認する場合だけ、ルート`.env.local`に接続値を用意し、`local`プロファイルを付けずに起動します。
`.env.local`はgit管理しません。

```powershell
cd C:\git\workops
Copy-Item .env.example .env.local
notepad .env.local

cd C:\git\workops\apps\web
.\mvnw.cmd spring-boot:run
```

`.env.local`に書く主なキーは次です。

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

Cognito issuer URIは`AWS_REGION`と`WORKOPS_COGNITO_USER_POOL_ID`からアプリ側で構成します。
PLATFORM / TENANTのredirect URIは、それぞれ`/login/oauth2/code/platform`と`/login/oauth2/code/tenant`を使います。
logout URIはCognito App Clientのallowed sign-out URLと一致させ、AWS devではCloudFront HTTPS URLの`/login`を使います。

### AWS dev初回接続導線

AWS devのRDSをdeployした直後は、seed済みの`users.cognito_sub`が`NULL`です。
そのため、最初にPlatform管理者として使うCognitoユーザーの`sub`を、AWS dev RDSの`platform-admin`に紐付けます。
このSQLはRDS Console integrated CloudShell VPCから実行し、接続後に必ず`USE workops;`でdatabaseを選択します。
実Cognito `sub`はREADMEやgit管理文書へ記録しません。

RDS Consoleで対象DBの接続方法からCloudShell VPC環境を起動し、`workops-{stage}-rds-console-cloudshell-sg`が適用されていることを確認します。ConsoleのAWSアカウント・リージョンは、上記で確認した接続先に合わせます。
CloudShellで次を実行し、TCP 3306への到達性を確認します。

```bash
timeout 5 bash -c 'cat < /dev/null > /dev/tcp/<rds-endpoint>/3306' \
  && echo tcp-ok \
  || echo tcp-ng
```

`tcp-ok`になったら、RDS Consoleが提示する接続コマンドを使います。CloudShellのMariaDB版`mysql`クライアントでは、CA証明書とサーバー証明書の検証を指定します。

```bash
mysql -h <rds-endpoint> -P 3306 \
  --ssl-ca /certs/global-bundle.pem --ssl-verify-server-cert \
  -u workops_admin -p workops
```

CA証明書のパスはConsoleの接続コマンドに合わせます。MySQL版クライアントでは`--ssl-verify-server-cert`の代わりに`--ssl-mode=VERIFY_IDENTITY`を使います。[AWSのTLS接続手順](https://docs.aws.amazon.com/AmazonRDS/latest/UserGuide/USER_ConnectToInstanceSSL.CLI.html)を参照してください。
パスワードはSecrets Managerの対象DB用secretから本人が確認し、接続コマンドの入力プロンプトに入力します。

接続後に対象databaseと接続を確認し、次の紐付けSQLへ進みます。

```sql
USE workops;
SELECT 1;
```

```sql
USE workops;

UPDATE users
SET cognito_sub = '<platform-admin-cognito-sub>'
WHERE username = 'platform-admin'
  AND email = 'platform-admin@example.local'
  AND actor_type = 'PLATFORM'
  AND cognito_sub IS NULL;

SELECT ROW_COUNT() AS updated_rows;
```

`updated_rows`が`1`であることを確認した後、CloudFrontの`/login/platform`からPlatform用のログインを実行します。
Platformログイン後、画面から会社を作成し、その会社に所属するTENANTユーザーを作成します。
TENANTユーザーはアプリのユーザー作成処理でCognitoユーザー作成と`users.cognito_sub`登録を行うため、作成後にCloudFrontの`/login/tenant`から同じTENANTユーザーでログインして確認します。

確認順序は次の通りです。

1. AWS dev RDSの`platform-admin`にPlatform管理者Cognitoユーザーの`sub`を登録する
2. `/login/platform`でPlatform用のログインを実行する
3. Platform画面で会社を作成する
4. 作成した会社に所属するTENANTユーザーを作成する
5. `/login/tenant`で作成したTENANTユーザーとしてログインする
6. トップページの導線と権限外URLの403を確認する

Cognitoで実際に招待メールを送るユーザー作成E2Eは、実行者がAWS認証情報と実メールアドレスを用意して手動確認します。
Cognito Hosted UIのブラウザ操作、CloudFront経由のログイン・ログアウト、実Cognito `sub`と`users`の突合確認はユーザー確認として扱います。
実account ID、credential、実Cognito `sub`、public IPはREADMEやgit管理文書へ記録しません。
Cognito Trigger、Pre Token Generation、SSO、テナント別ログイン画面、本格ブランドデザインは対象に含めません。

DBの作業後は`exit;`でMySQL接続を終了し、確認用のCloudShell VPC環境を削除します。環境のENIが残ると、関連するSecurity Groupを削除できません。

## 停止

Spring Bootは起動中のPowerShellで`Ctrl + C`を押して停止します。

MySQLを停止する場合はリポジトリルートで実行します。

```powershell
cd C:\git\workops
docker compose stop workops-mysql
```

MySQLのデータボリュームも削除する場合は次を実行します。

```powershell
docker compose down -v
```
