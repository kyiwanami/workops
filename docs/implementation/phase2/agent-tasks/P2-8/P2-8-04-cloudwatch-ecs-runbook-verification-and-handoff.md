# P2-8-04 CloudWatch / ECS runbook / verification / handoff

## 目的

P2-8 の実装結果を、AWS dev 上で CloudWatch Logs と ECS から調査できる手順として整理する。

CloudWatch Logs で見るロググループ、ECS アプリログ、`apps/web` 起動時 Flyway ログ、ECS task 停止理由、認証・認可・例外ログの確認手順を README / task に記録し、P2-9 の GitHub Actions OIDC deploy へ引き継ぐ。

## ユーザー要求

- CloudWatch Logs で見るロググループ、ECS アプリログ、Flyway 起動ログ、ECS task 停止理由の確認手順を整理する
- AWS dev で確認するシナリオを定義する
- シナリオは、未ログイン、認証切れ、認証失敗、`users` 未突合、actor_type 不整合、AccessDenied、アプリ例外、Flyway 失敗を含める
- エージェント確認とユーザー確認を分ける
- P2-9 へ、GitHub Actions deploy 時に確認すべきログ観点を引き継ぐ
- AWS 実操作は、ユーザーの明示指示と認証確認なしに実行しない
- AWS CLI、CDK、ECS、CloudWatch Logs など AWS に接続する操作では、実行前に AWS profile と region を明示する
- AWS 操作前に `AWS_ACCESS_KEY_ID`、`AWS_SECRET_ACCESS_KEY`、`AWS_SESSION_TOKEN` を削除してから profile を使う
- AWS 実操作の前には `aws sts get-caller-identity --region $env:AWS_REGION` で認証先を確認する
- AWS CLI コマンドには原則として `--region $env:AWS_REGION` を付ける

## 前提

- P2-8-01 が完了している
- P2-8-02 が完了している
- P2-8-03 が完了している
- P2-7 が完了している
- `LogsStack` は CloudWatch Logs log group を管理している
- `AppRuntimeStack` は ECS app log を CloudWatch Logs へ出力する
- `apps/web` 起動時 Flyway は標準ログとして CloudWatch Logs へ出力される
- P2-8 の AWS dev 確認には短命 Stack の再作成が必要である
- AWS dev RDS / MySQL への SQL 実行は、ユーザーが実施するか、ユーザーから明示的な実行指示が出た場合だけ行う
- AWS dev RDS への SQL 実行経路は RDS Console integrated CloudShell VPC に限定する
- 既存データ移行と後方互換性は考慮しない

## 案

- README または P2-8 task に CloudWatch Logs 調査 Runbook を追加する
- Runbook には、ロググループ名、log stream の見つけ方、ECS service / task から log stream へ辿る手順を記載する
- Runbook には、`com.example.workops.operation`、`com.example.workops.security`、`com.example.workops.application` の用途を記載する
- Runbook には、`eventType` と `reasonCode` を使った検索例を記載する
- Runbook には、`requestId` を使って同一 request のログを追う手順を記載する
- Runbook には、HTTP request 外の Flyway 起動ログは `requestId=-` になることを記載する
- Runbook には、ECS task 停止理由を ECS service event、stopped task、container exit code、CloudWatch Logs の順に確認する手順を記載する
- Runbook には、`apps/web` 起動時 Flyway 失敗時に見るログと ECS task 状態を記載する
- エージェント確認では Java test を実行する
- エージェント確認では CDK build / test を実行する
- エージェント確認では必要に応じて `LogsStack`、`IdentityStack`、`AppRuntimeStack` の synth を行う
- エージェント確認では README / task に実値の secret、token、password、実 Cognito `sub`、RDS の実更新値、public IP、EC2 instance id が含まれないことを確認する
- ユーザー確認では AWS dev で短命 Stack を作成する
- ユーザー確認では CloudFront default domain 経由で確認シナリオを実施する
- ユーザー確認では各シナリオの期待 `eventType` / `reasonCode` を CloudWatch Logs で確認する
- ユーザー確認後、短命 Stack cleanup の手順を実施する
- P2-9 へ、GitHub Actions deploy 後に CloudWatch Logs で確認する観点を引き継ぐ

## ADR

- Runbook と実環境シナリオを 1 task にまとめる。CloudWatch Logs / ECS の見方と、実際に発生させる失敗シナリオは同じ確認作業で使うため、分けると重複が増えるため。
- エージェント確認とユーザー確認を分ける。AWS dev、Cognito Hosted UI、CloudWatch Logs 実確認、RDS SQL は外部サービスとユーザー資格情報を伴うため、ローカル test や synth だけで完了扱いにしない。
- CloudWatch Logs retention は変更しない。P2-1 / LogsStack の 7 日方針を維持し、P2-8 では確認手順とアプリログの調査性に集中するため。
- CloudWatch Alarm と Dashboard は作らない。P2-8 の目的は調査可能性であり、監視通知や可視化は除外範囲であるため。
- Container Insights は作らない。Phase 2 の共通方針で Container Insights は採用しないため。
- Cognito 内部ログ取得は目的にしない。WorkOps アプリから見た Cognito 連携結果を `SecurityEventLogger` で確認するため。
- 既存データ移行と後方互換性は考慮しない。

## 対応範囲

- CloudWatch Logs 調査 Runbook
- ECS service / task 停止理由の確認手順
- `apps/web` 起動時 Flyway ログ確認手順
- 業務操作ログの CloudWatch Logs 確認手順
- 認証・認可イベントログの CloudWatch Logs 確認手順
- アプリ例外ログの CloudWatch Logs 確認手順
- AWS dev 確認シナリオ
- エージェント確認とユーザー確認の分離
- P2-9 への引き継ぎ

## 除外範囲

- CloudWatch Alarm
- Observability Dashboard
- Container Insights
- OpenTelemetry
- 分散トレーシング
- Cognito 内部ログ取得
- 監査ログテーブル
- 履歴テーブル
- ログ検索画面
- JSON structured logging への全面移行
- GitHub Actions workflow 実装
- CodePipeline
- CodeBuild
- AWS deploy / destroy の自動実行
- RDS Console integrated CloudShell VPC 以外の SQL 実行経路

## 完了条件

- CloudWatch Logs の対象ロググループ確認手順が記載されている
- ECS app log stream の見つけ方が記載されている
- `apps/web` 起動時 Flyway ログの確認手順が記載されている
- ECS service event、stopped task、container exit code、CloudWatch Logs の確認順序が記載されている
- `requestId` による同一 request の追跡手順が記載されている
- `eventType` / `reasonCode` による security event 検索手順が記載されている
- `com.example.workops.operation` の業務操作ログ確認手順が記載されている
- `com.example.workops.security` の認証・認可ログ確認手順が記載されている
- `com.example.workops.application` の例外ログ確認手順が記載されている
- 未ログインの確認シナリオが記載されている
- 認証切れの確認シナリオが記載されている
- 認証失敗の確認シナリオが記載されている
- `users` 未突合の確認シナリオが記載されている
- actor_type 不整合の確認シナリオが記載されている
- AccessDenied の確認シナリオが記載されている
- アプリ例外の確認シナリオが記載されている
- Flyway 失敗の確認シナリオが記載されている
- エージェント確認とユーザー確認が分けて記載されている
- P2-9 へのログ確認観点が記載されている
- Java test が成功している
- CDK build / test が成功している
- README / task に secret、token、password、実 Cognito `sub`、RDS の実更新値、public IP、EC2 instance id が含まれていない

## 確認方法

エージェント確認 Java:

```powershell
cd C:\git\workops\apps\web
.\mvnw.cmd test
```

エージェント確認 CDK:

```powershell
cd C:\git\workops\infra\cdk
npm run build
npm test -- --runInBand
```

必要に応じた CDK synth:

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
npm run cdk -- synth LogsStack
npm run cdk -- synth IdentityStack
npm run cdk -- synth AppRuntimeStack
```

AWS dev ユーザー確認前の認証先確認:

```powershell
Remove-Item Env:AWS_ACCESS_KEY_ID -ErrorAction SilentlyContinue
Remove-Item Env:AWS_SECRET_ACCESS_KEY -ErrorAction SilentlyContinue
Remove-Item Env:AWS_SESSION_TOKEN -ErrorAction SilentlyContinue
$env:AWS_PROFILE='amazon-connect'
$env:AWS_SDK_LOAD_CONFIG='1'
$env:AWS_REGION='ap-northeast-1'
$env:AWS_DEFAULT_REGION='ap-northeast-1'
aws sts get-caller-identity --region $env:AWS_REGION
```

CloudWatch Logs 確認例:

```powershell
aws logs describe-log-groups --region $env:AWS_REGION --log-group-name-prefix '/workops/dev'
```

ECS task 確認例:

```powershell
aws ecs list-tasks --region $env:AWS_REGION --cluster workops-dev-cluster
aws ecs describe-tasks --region $env:AWS_REGION --cluster workops-dev-cluster --tasks <task-arn>
```

CloudWatch Logs Insights では、次の観点を確認する。

```text
eventType=AUTHENTICATION_FAILED
eventType=AUTHENTICATION_REJECTED reasonCode=USER_NOT_LINKED
eventType=AUTHENTICATION_REJECTED reasonCode=ACTOR_TYPE_MISMATCH
eventType=AUTHORIZATION_DENIED
eventType=APPLICATION_EXCEPTION
```

実値を記録しない確認:

```powershell
Select-String -Path 'C:\git\workops\docs\implementation\phase2\agent-tasks\P2-8\*.md' `
  -Pattern 'password=', 'token=', 'secret=', 'cognito_sub = ', 'sub=', 'public ip', 'i-[0-9a-f]' `
  -Encoding UTF8
```

上記で手順説明以外の実値が含まれていないことを確認する。

## 引き継ぎ

- P2-9 の GitHub Actions OIDC deploy では、deploy 後に CloudWatch Logs で `eventType=APPLICATION_EXCEPTION` が増えていないことを確認する
- P2-9 の GitHub Actions OIDC deploy では、Hosted UI login / app deploy 後の smoke check で `com.example.workops.security` の確認先を Runbook から参照する
- P2-9 の migration 確認では、`apps/web` 起動時 Flyway 成功・失敗を CloudWatch Logs と ECS task 状態で確認する
- P2-9 でも AWS dev 実確認はユーザー確認として扱い、エージェント確認だけで完了扱いにしない
