# P2-7-02 AdminCreateUser AWS dev verification

## 目的

AWS dev 環境で、M9 / M10 で作成済みの管理導線を使い、WorkOps アプリから実 Cognito `AdminCreateUser` を呼び出し、取得した `sub` を `users.cognito_sub` として登録できることを確認する。

必須確認は、`platform-admin` の一時 bootstrap から始め、会社作成による初期 TENANT_MANAGER 作成、Cognito 招待メール受信、初回パスワード変更、TENANT ログインまでを通す。

## ユーザー要求

- 最初の操作主体は `platform-admin` とする
- `platform-admin` は P2-5 と同じ一時 SQL bootstrap で実 Cognito `sub` に紐付ける
- WorkOps 画面から最初の PLATFORM_ADMIN を作ることは P2-7 の前提にしない
- `/login/platform` から PLATFORM_ADMIN としてログインする
- 会社作成から初期 TENANT_MANAGER 作成を必須確認にする
- 初期 TENANT_MANAGER は WorkOps 管理導線から実 Cognito `AdminCreateUser` で作る
- Cognito 招待メールの実配送確認を P2-7 の完了条件に含める
- 招待メールが届かない場合、P2-7 は未完了とする
- Cognito コンソールや CLI で一時パスワードを代替取得しても、メール未着時は完了扱いにしない
- 一時パスワード変更完了まで P2-7 の完了条件に含める
- TENANT ユーザーは `/login/tenant` からログインする
- `registrationId=tenant` と `users.actor_type = TENANT` の整合を確認する
- 既存会社への TENANT ユーザー追加、PLATFORM ユーザー追加、権限変更は補助確認にする

## 前提

- P2-7-01 の実装が完了している
- `IdentityStack` は Cognito User Pool / Hosted UI domain / Platform App Client / Tenant App Client を維持している
- `IdentityStack` の招待メールテンプレートは日本語 HTML である
- `AppRuntimeStack` の ECS Task Role は `cognito-idp:AdminCreateUser` のみを持つ
- `DataStack` は短命 RDS を作成する
- RDS を作り直すたびに AWS dev seed の `users.cognito_sub` は NULL に戻る
- `db/seed/aws-dev/V6__insert_users.sql` は `platform-admin` を `cognito_sub = NULL` で投入する
- `platform-admin` は `actor_type = 'PLATFORM'` で、`PLATFORM_ADMIN` 権限セットを持つ
- P2-7 では既存データ移行と後方互換性を考慮しない

## 案

- ユーザー確認で `IdentityStack` を update する
- ユーザー確認で `DataStack`、`EgressStack`、`EdgeStack`、`AppRuntimeStack` を作成する
- Cognito App Client URL が新しい CloudFront default domain の `/login/oauth2/code/platform` と `/login/oauth2/code/tenant` に更新されていることを確認する
- AWS dev RDS の `platform-admin` に実 Cognito `sub` を一時 SQL で投入する
- `/login/platform` から `platform-admin` としてログインする
- `/auth/claims` で `username = platform-admin`、`actorType = PLATFORM`、`PLATFORM_ADMIN` を確認する
- `/admin/companies` から会社を作成する
- 会社作成フォームでは初期 TENANT_MANAGER の username、name、email を入力する
- 会社作成時に WorkOps アプリが Cognito `AdminCreateUser` を呼び出す
- Cognito response の `sub` が `users.cognito_sub` に登録されることを DB で確認する
- `user_permission_sets` に `TENANT_MANAGER` が登録されることを DB で確認する
- 初期 TENANT_MANAGER 宛に Cognito 招待メールが届くことを確認する
- 招待メール本文に WorkOps 向け日本語案内、ユーザー名、一時パスワードが表示されることを確認する
- 招待メールの一時パスワードで Hosted UI にログインする
- 初回パスワード変更を完了する
- CloudFront callback で WorkOps に戻る
- `/login/tenant` から TENANT として session が作成されることを確認する
- `/auth/claims` で初期 TENANT_MANAGER、`actorType = TENANT`、`TENANT_MANAGER` を確認する
- TENANT_MANAGER として自社のユーザー管理または部署管理画面に到達できることを確認する

## ADR

- `platform-admin` の一時 SQL bootstrap は継続する。WorkOps 画面から最初の PLATFORM_ADMIN を作るには、すでにログイン済みの PLATFORM_ADMIN が必要であり、ここは P2-5 の bootstrap 手順を引き継ぐ。
- 必須確認は会社作成による初期 TENANT_MANAGER 作成に絞る。P2-7 の中心は、PLATFORM 管理導線から TENANT 管理者を作成し、TENANT が実ログインできることである。
- 招待メール未着時は未完了にする。P2-7 では Cognito 標準招待メールの実配送まで確認するため、代替ログインで完了扱いにしない。
- 補助確認は完了条件に含めない。既存会社への TENANT ユーザー追加、PLATFORM ユーザー追加、権限変更は M10 実装済み導線の追加確認であり、P2-7 の最短完了経路にはしない。
- 既存データ移行と後方互換性は考慮しない。

## 対応範囲

- `platform-admin` bootstrap 手順
- PLATFORM login 確認
- 会社作成による初期 TENANT_MANAGER 作成確認
- Cognito `AdminCreateUser` 呼び出し確認
- `users.cognito_sub` 登録確認
- `user_permission_sets` 登録確認
- Cognito 招待メール実受信確認
- 初回パスワード変更確認
- TENANT login 確認
- DB 由来権限での画面到達確認
- メール未着時の未完了扱い
- 補助確認項目の明記

## 除外範囲

- WorkOps 画面から最初の PLATFORM_ADMIN を作成する導線
- Cognito 作成成功後の DB 登録失敗に対する自動削除補償
- Cognito ユーザー削除
- Cognito ユーザー停止
- Cognito ユーザー属性変更
- Cognito Trigger
- Pre Token Generation
- SES カスタム FROM
- メール未着時の代替ログインによる完了扱い
- P2-7-02 task 作成時点での AWS deploy / destroy

## 完了条件

- `IdentityStack` update が完了している
- `DataStack`、`EgressStack`、`EdgeStack`、`AppRuntimeStack` が AWS dev に作成されている
- Cognito App Client URL が CloudFront default domain の platform / tenant callback URL に更新されている
- `platform-admin` の `users.cognito_sub` を一時 SQL で実 Cognito `sub` に紐付けられる
- `/login/platform` から `platform-admin` としてログインできる
- `platform-admin` が DB 由来 `PLATFORM_ADMIN` 権限で `/admin/companies` に到達できる
- 会社作成画面から初期 TENANT_MANAGER を作成できる
- 初期 TENANT_MANAGER が Cognito User Pool に作成される
- 初期 TENANT_MANAGER の Cognito `sub` が `users.cognito_sub` に登録される
- 初期 TENANT_MANAGER に `TENANT_MANAGER` 権限セットが登録される
- 初期 TENANT_MANAGER 宛の Cognito 招待メールを実受信できる
- 招待メールの一時パスワードで Hosted UI にログインできる
- 初回パスワード変更を完了できる
- CloudFront callback で WorkOps に戻れる
- `/login/tenant` 導線で `actorType = TENANT` の session が作成される
- 初期 TENANT_MANAGER が DB 由来 `TENANT_MANAGER` 権限で自社管理画面に到達できる
- 招待メールが届かない場合は P2-7 未完了として記録される

## 確認方法


```powershell
cd C:\git\workops\infra\cdk
Remove-Item Env:AWS_ACCESS_KEY_ID -ErrorAction SilentlyContinue
Remove-Item Env:AWS_SECRET_ACCESS_KEY -ErrorAction SilentlyContinue
Remove-Item Env:AWS_SESSION_TOKEN -ErrorAction SilentlyContinue
$env:WORKOPS_STAGE='dev'
$env:AWS_PROFILE='amazon-connect'
$env:AWS_SDK_LOAD_CONFIG='1'
$env:AWS_REGION='ap-northeast-1'
$env:AWS_DEFAULT_REGION='ap-northeast-1'
$env:CDK_DEFAULT_ACCOUNT=(aws sts get-caller-identity --region $env:AWS_REGION --query Account --output text)
$env:CDK_DEFAULT_REGION='ap-northeast-1'
npm run cdk -- deploy IdentityStack
npm run cdk -- deploy DataStack
npm run cdk -- deploy EgressStack
npm run cdk -- deploy EdgeStack
npm run cdk -- deploy AppRuntimeStack
```

`platform-admin` bootstrap は P2-5-04 の一時 SQL 手順を使う。

接続後に DB が未選択の状態になり得るため、SQL 実行前に対象 database を選択する。

```sql
USE workops;
```

更新前確認:

```sql
SELECT
    id,
    username,
    actor_type,
    cognito_sub,
    is_deleted
FROM users
WHERE username = 'platform-admin'
  AND actor_type = 'PLATFORM'
  AND is_deleted = FALSE;
```

`platform-admin` の `cognito_sub` 更新:

```sql
UPDATE users
SET cognito_sub = :cognito_sub
WHERE username = 'platform-admin'
  AND actor_type = 'PLATFORM'
  AND is_deleted = FALSE;
```

更新後確認:

```sql
SELECT
    COUNT(*) AS linked_platform_admin_count
FROM users
WHERE username = 'platform-admin'
  AND actor_type = 'PLATFORM'
  AND cognito_sub = :cognito_sub
  AND is_deleted = FALSE;
```

初期 TENANT_MANAGER 登録確認:

```sql
SELECT
    u.id,
    u.username,
    u.email,
    u.actor_type,
    u.company_id,
    u.cognito_sub,
    ps.code
FROM users u
JOIN user_permission_sets ups ON ups.user_id = u.id
JOIN permission_sets ps ON ps.id = ups.permission_set_id
WHERE u.username = :tenant_manager_username
  AND u.actor_type = 'TENANT'
  AND u.is_deleted = FALSE;
```

## 実施結果

2026-06-19 の AWS dev 検証で、P2-7-02 の必須経路を確認した。

- `DataStack`、`EgressStack`、`EdgeStack`、`AppRuntimeStack` の作成が完了した。
- CloudFront 経由の `/actuator/health` が 200 / `UP` を返すことを確認した。
- Cognito App Client の platform / tenant callback URL が CloudFront default domain に更新されていることを確認した。
- RDS Console integrated CloudShell VPC から `platform-admin` の `users.cognito_sub` bootstrap を実施した。
- `/login/platform` から platform login し、DB 由来 `PLATFORM_ADMIN` 権限で claims を確認した。
- 会社作成画面から初期 TENANT_MANAGER を作成した。
- 初期 TENANT_MANAGER の Cognito 招待メールを実受信した。
- 招待メールの一時パスワードで Hosted UI にログインし、初回パスワード変更を完了した。
- `/login/tenant` 導線で TENANT session が作成され、DB 由来 `TENANT_MANAGER` 権限が付与されていることを確認した。

実 Cognito `sub`、実メールアドレス、credential 値は記録しない。

## 実施中トラブル報告

2026-06-19 の AWS dev 検証中に、次のトラブルが発生した。

### EdgeStack の LogGroup 名衝突

- 事象: `EdgeStack` deploy が `AWS::Logs::LogGroup` `/workops/dev/custom-resources/cognito-client-url-updater-provider` の既存リソース衝突で停止した。
- 原因: 同名 LogGroup が CloudFormation 管理外に残っていた。確認時点の stored bytes は 0 で、EdgeStack が同名 LogGroup を新規作成できなかった。
- 対応: 孤立していた同名 LogGroup を削除し、`EdgeStack` を再 deploy した。
- 分類: 暫定対応。既存孤立リソースの運用 cleanup であり、CDK 側の恒久修正は行っていない。

### AppRuntimeStack の ECS task 起動失敗

- 事象: `AppRuntimeStack` deploy が timeout 後に rollback へ入り、ECS service は `tasks failed to start` になった。
- 原因: ECR の `p2-3-manual` image が古く、Spring Boot 起動時に旧 OAuth2 registration `cognito` を読んでいた。CloudWatch Logs で `Client id of registration 'cognito' must not be empty.` を確認した。
- 対応: P2-7-01 後の `apps/web` で Docker image を build し、ECR `workops-dev-web:p2-3-manual` へ push した後、`AppRuntimeStack` を再 deploy した。
- 分類: 暫定対応。手動 deploy 用 tag を上書きした運用対応であり、恒久対応は app deploy pipeline で current code の image build / push / deploy 順序を固定すること。

### DB bootstrap 経路の誤提案

- 事象: `platform-admin` の `cognito_sub` bootstrap に進む前に、一回限りの Fargate task で SQL を実行する案を提示した。
- 原因: `docs/implementation/README.md` の AWS dev RDS / MySQL 操作ルールと、この task の「RDS Console integrated CloudShell VPC から行う」前提を確認せず、private RDS への到達性だけで代替経路を提案した。
- 対応: ユーザー指摘で停止した。Fargate task による SQL 実行、task definition 登録、DB bootstrap は実行していない。
- 分類: 作業ミス。暫定対応ではなく、ルール違反の誤提案として扱う。DB bootstrap は RDS Console integrated CloudShell VPC のみで実施する。

補助確認:

- PLATFORM_ADMIN による既存会社への TENANT ユーザー追加
- PLATFORM_ADMIN による PLATFORM ユーザー追加
- TENANT_MANAGER による自社 TENANT ユーザー追加
- 権限変更
- TENANT_MANAGER 0 人防止バリデーション
