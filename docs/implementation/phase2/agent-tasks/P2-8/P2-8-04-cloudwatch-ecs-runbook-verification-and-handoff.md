# P2-8-04 Application log E2E verification and handoff

## 目的

P2-8 で実装した requestId、認証・認可ログ、アプリ例外ログを、ブラウザ E2E と CloudWatch Logs で調査できる状態にする。

この task はアプリケーションログの E2E を対象にする。ECS task 停止理由、container exit code、service event、Flyway 失敗時の停止調査は対象外とする。

## ユーザー要求

- ブラウザ機能を使って E2E を実施する
- まずログ E2E テストケースを洗い出す
- ローカルで実施できるケースと、AWS dev デプロイ先でしか実施できないケースを分ける
- アプリケーションログの確認を対象にし、ECS task 停止理由はスコープ外にする
- CloudWatch Logs で見るロググループ、アプリログ、認証・認可・例外ログの確認手順を整理する
- エージェント確認とユーザー確認を分ける
- P2-9 へ、GitHub Actions deploy 時に確認すべきアプリログ観点を引き継ぐ
- AWS 実操作は、ユーザーの明示指示と認証確認なしに実行しない
- AWS CLI、CDK、CloudWatch Logs など AWS に接続する操作では、実行前に AWS profile と region を明示する
- AWS 操作前に `AWS_ACCESS_KEY_ID`、`AWS_SECRET_ACCESS_KEY`、`AWS_SESSION_TOKEN` を削除してから profile を使う
- AWS 実操作の前には `aws sts get-caller-identity --region $env:AWS_REGION` で認証先を確認する
- AWS CLI コマンドには原則として `--region $env:AWS_REGION` を付ける

## 前提

- P2-8-01 が完了している
- P2-8-02 が完了している
- P2-8-03 が完了している
- P2-7 が完了している
- local profile は Cognito Hosted UI へ接続せず、`LocalAuthenticationFilter` で DB seed の疑似ユーザーを現在ユーザーにする
- local profile の既定ユーザーは `TENANT_MANAGER` である
- local profile で AccessDenied を発生させる場合は、Spring Boot を `WORKOPS_LOCAL_COGNITO_SUB=00000000-0000-0000-0000-000000000001` の閲覧者で起動する
- AWS dev profile は Cognito Hosted UI と実 DB 突合を使う
- AWS dev のアプリログ確認先は CloudWatch Logs `/workops/dev/web` とする
- AWS dev の CloudFront domain、Cognito user、実 Cognito `sub`、credential、RDS の実更新値は文書へ記録しない
- 既存データ移行と後方互換性は考慮しない

## 案

ログ E2E テストケースは次で固定する。

| No | ケース | ローカル E2E | AWS dev E2E | 期待ログ |
| --- | --- | --- | --- | --- |
| 1 | 通常ページ表示で requestId を確認する | 実施する | 実施する | レスポンスヘッダー `X-Request-Id` が返る。通常ページ表示だけでは event log を必須にしない |
| 2 | HTTP 外ログを確認する | 自動テストで確認済みとして扱う | CloudWatch で確認する | 起動時ログは `requestId=-` として扱う |
| 3 | AccessDenied を発生させる | 閲覧者で `/auth/authorization/manager` にアクセスして実施する | `TENANT_MANAGER` 以外のユーザーで実施する | `eventType=AUTHORIZATION_DENIED reasonCode=AUTHORIZATION_DENIED` |
| 4 | アプリ例外を発生させる | `/assets/999999999` にアクセスして実施する | 同じ操作で実施する | `eventType=APPLICATION_EXCEPTION exceptionType=ResponseStatusException status=404 path=/assets/999999999` |
| 5 | WorkOps ユーザー確定後の認証成功 | local profile では対象外 | Cognito login 後に実施する | `eventType=AUTHENTICATION_SUCCEEDED result=SUCCESS reasonCode=-` |
| 6 | `users` 未突合 | local profile では対象外 | Cognito user は存在するが `users` に紐づかない状態で実施する | `eventType=AUTHENTICATION_REJECTED reasonCode=USER_NOT_LINKED userId=-` |
| 7 | actor_type 不整合 | local profile では対象外 | login 導線と DB user の actor_type を不一致にして実施する | `eventType=AUTHENTICATION_REJECTED reasonCode=ACTOR_TYPE_MISMATCH` |
| 8 | Spring Security 認証失敗 | local profile では対象外 | app 側が OAuth2 failure を受ける経路で実施する | `eventType=AUTHENTICATION_FAILED reasonCode=AUTHENTICATION_FAILED` |
| 9 | 未ログインアクセス | local profile では対象外 | cookie なしで保護ページへアクセスする | Hosted UI へ遷移する。WorkOps app に到達しない場合、app security event は必須にしない |
| 10 | 認証切れ | local profile では対象外 | session / cookie を削除して保護ページへアクセスする | 再 login 導線へ遷移する。WorkOps app に到達しない場合、app security event は必須にしない |
| 11 | 業務操作ログ | 更新系 UI 操作を伴うためローカル DB で実施する | AWS dev の検証データで実施する | `com.example.workops.operation` に `operationType`、`result`、`reasonCode` が出る |

ローカル E2E は、コーディングエージェントが in-app browser で実施する。Spring Boot は `local` profile で起動し、確認対象ログはローカルの Spring Boot コンソール出力とする。

AWS dev E2E は、ユーザー確認として扱う。コーディングエージェントは AWS profile、region、認証先確認、CloudWatch Logs クエリ手順を提示できるが、ユーザーの明示指示なしに AWS 実操作を完了扱いにしない。

CloudWatch Logs では次を固定で確認する。

| 種別 | 値 |
| --- | --- |
| log group | `/workops/dev/web` |
| requestId | `requestId=<uuid>` |
| operation logger | `com.example.workops.operation` |
| security logger | `com.example.workops.security` |
| application logger | `com.example.workops.application` |
| security 必須キー | `requestId`, `eventType`, `reasonCode` |
| application exception 必須キー | `requestId`, `eventType`, `exceptionType`, `status`, `path` |

## ADR

- P2-8-04 はアプリケーションログ E2E に限定する。P2-8-01 から P2-8-03 の成果物は HTTP request、認証・認可、アプリ例外のログであり、ECS task 停止理由はアプリケーションログの E2E ではないため。
- ECS service event、stopped task、container exit code、task stopped reason はこの task に書かない。必要になった場合は運用 Runbook または別 task で扱う。
- Flyway 失敗シナリオはこの task の E2E ケースに含めない。ブラウザ操作で発生させるアプリログ E2Eではなく、起動失敗・deploy 検証の領域であるため。
- local profile では Cognito Hosted UI を通らないため、`USER_NOT_LINKED`、`ACTOR_TYPE_MISMATCH`、`AUTHENTICATION_FAILED`、未ログイン、認証切れは AWS dev 専用ケースにする。
- AccessDenied とアプリ例外はローカルでもブラウザで発生させる。Spring MVC、Spring Security method security、P2-8 ログ実装を実画面経由で検証できるため。
- CloudWatch Logs 確認は AWS dev 専用とする。ローカル E2E は同じログ本文が Spring Boot コンソールに出ることを確認し、CloudWatch への配送確認は AWS dev 側で扱う。
- Cognito Hosted UI 内のパスワード誤りは、WorkOps app まで到達しない場合があるため、WorkOps app security event 必須ケースにしない。app 側が OAuth2 failure を受けた場合のみ `AUTHENTICATION_FAILED` を確認する。
- 実 account ID、SSO role ARN、SSO user、credential、実 Cognito `sub`、RDS の実更新値は文書に残さない。
- `/favicon.ico` の `APPLICATION_EXCEPTION` 混入は `src/main/resources/static/favicon.ico` を追加して解消する。Spring Boot の静的リソース配信に任せるのが最小で標準的であり、Controller で 204 を返す案や `ApplicationExceptionHandler` で `NoResourceFoundException` を握りつぶす案は採用しない。
- 403 画面の `Request ID` 欄が空になる問題は `ErrorAttributes` 拡張で解消する。Spring Boot の `/error` 経路全体へ `requestId` を渡すためであり、AccessDenied 専用の `AccessDeniedHandler` で画面を特別扱いする案は採用しない。
- `ApplicationExceptionHandler` で `AccessDeniedException` を処理する案は採用しない。AccessDenied はアプリ例外ではなく Spring Security の認可拒否であり、ログは `AuthorizationDeniedEvent` listener、HTTP 403 表示は Spring Security / Spring Boot error handling 側で扱う。

## E2E 中の検出事項

E2E 中に障害、違和感、仕様未確定点を検出した場合は、場当たり的に修正しない。まずこの節へ記録し、クリティカルな場合は E2E を停止してユーザー判断を仰ぐ。クリティカルでない場合は記録したうえで E2E を継続し、後で対処方針を決める。

| ID | 検出事項 | 発生条件 | 影響 | クリティカル判定 | 対応状態 |
| --- | --- | --- | --- | --- | --- |
| E2E-001 | ブラウザが `/favicon.ico` を自動取得し、未配置のため `NoResourceFoundException` が `APPLICATION_EXCEPTION` として出力された | local profile で通常ページをブラウザ表示した直後 | 通常ページ表示だけでアプリ例外ログが混入し、ログ E2E の判定が汚れる | 非クリティカル。アプリ本体のリクエスト処理は継続できる | 記録のみ。favicon 追加などの修正は、公式・既存方針確認後に判断する |
| E2E-002 | AccessDenied の 403 画面では `Request ID` 欄が空だったが、レスポンスヘッダー `X-Request-Id` と `AUTHORIZATION_DENIED` ログには requestId が出ていた | local profile を `TENANT_VIEWER` で起動し、`/auth/authorization/manager` をブラウザ表示 | ログ相関自体は可能だが、画面上の Request ID 表示は 403 経路で欠落する | 非クリティカル。P2-8 のログ確認はヘッダーとログで継続できる | 記録のみ。`/error`、`ErrorAttributes`、`AccessDeniedHandler` の扱いはベストプラクティス確認後に判断する |
| E2E-003 | AccessDenied は `ApplicationExceptionHandler` ではなく Spring Security / Spring Boot の `/error` 経路で 403 応答になった | `@PreAuthorize` で拒否されたとき | 認可拒否の画面表示とアプリ例外画面を同一視すると、責務境界を誤る | 非クリティカル。ログは `AuthorizationDeniedEvent` listener で出ている | 記録のみ。認証・認可は Spring Security 側で扱い、アプリ例外ログとは分ける |
| E2E-004 | in-app browser のページ内 `fetch` ではヘッダー取得ができなかった | ブラウザ評価内で `fetch('/')` を実行したとき | ブラウザだけで `X-Request-Id` ヘッダーを直接検証できない | 非クリティカル。画面確認はブラウザ、ヘッダー確認は `curl.exe` で補助できる | 記録のみ。E2E結果ではブラウザ確認とヘッダー補助確認を分けて記録する |

## E2E NG 判定

| ID | 判定 | 理由 | 後続対応 |
| --- | --- | --- | --- |
| E2E-001 | NG | 通常ページ表示だけで `eventType=APPLICATION_EXCEPTION status=500 path=/favicon.ico` が出るため、CloudWatch Logs のアプリ例外調査を汚す | favicon を追加するか、`/favicon.ico` を明示的に 204 / 404 静的扱いにするかを、既存 UI 方針確認後に決める |
| E2E-002 | NG 候補 | `X-Request-Id` と security log では追跡できるが、エラー画面に `Request ID` 欄があるのに空になる | `ErrorAttributes` 拡張または HTML 用 `AccessDeniedHandler` のどちらで扱うかを決める |
| E2E-003 | NG ではない | AccessDenied が `ApplicationExceptionHandler` ではなく Spring Security / `/error` 経路になるのは、責務境界として妥当 | 認証・認可ログは `AuthorizationDeniedEvent` listener、HTTP 403 表示は Spring Security / Spring Boot error handling 側で扱う |
| E2E-004 | アプリ NG ではない | in-app browser の制約であり、アプリ挙動ではない | E2E 手順では画面確認とヘッダー確認を分ける |

## AccessDenied / error handling 方針メモ

`/auth/authorization/manager` は `@PreAuthorize("hasAuthority('TENANT_MANAGER')")` で保護されている。閲覧者ユーザーでアクセスすると、Spring Security method security が認可拒否を発生させ、`AuthorizationDeniedEvent` と `AccessDeniedException` が発生する。

`AccessDeniedException` はアプリ例外ではなく、Spring Security の認可拒否である。HTTP 応答は Spring Security の exception handling が 403 として扱い、Spring Boot 側の `/error` / `BasicErrorController` がエラー表示を組み立てる。そのため、`ApplicationExceptionHandler` で AccessDenied を拾ってアプリ例外として扱う方針は採用しない。

`requestId` は Spring Boot 標準の error attributes ではない。P2-8-01 で独自に `X-Request-Id` と MDC へ入れている値なので、`/error` 経由の HTML に表示する場合は `ErrorAttributes` 拡張または HTML 用 `AccessDeniedHandler` で明示的に渡す。

採用方針:

- `/favicon.ico` は `src/main/resources/static/favicon.ico` を追加して Spring Boot の静的リソース配信で返す
- 認可拒否ログは `AuthorizationDeniedEvent` listener で `AUTHORIZATION_DENIED` を出す
- 認可拒否を `ApplicationExceptionHandler` でアプリ例外ログにしない
- HTTP 403 表示は Spring Security / Spring Boot error handling 側で扱う
- エラー画面へ requestId を表示するため、`ErrorAttributes` 拡張で Spring Boot `/error` の model に `requestId` を追加する
- HTML 用 `AccessDeniedHandler` は採用しない。403 だけを特別扱いせず、`/error` 経路全体で同じ requestId 表示にするため
- `/error` や Thymeleaf forward が二次的に認可拒否される場合は、`DispatcherType.ERROR` / `DispatcherType.FORWARD` を permit する

参考:

- Spring Security Method Security: `AuthorizationDeniedEvent` と `AccessDeniedException`
- Spring Security Architecture: `ExceptionTranslationFilter` による `AccessDeniedException` / `AuthenticationException` の HTTP 応答変換
- Spring Boot `BasicErrorController`: `${error.path:/error}` の標準 error controller
- Spring Security Authorization: `ERROR` / `FORWARD` dispatch も authorization 対象になる

## ローカル E2E 実施結果

2026-06-19 に local profile で in-app browser E2E を実施した。検出事項はすべて非クリティカルであり、E2E は継続可能と判断した。

| ケース | 実施場所 | 結果 | ログ確認 | 備考 |
| --- | --- | --- | --- | --- |
| requestId | local browser + `curl.exe` | pass | pass | ブラウザで `http://localhost:8080/` が表示された。`curl.exe` で `X-Request-Id` が返ることを確認した。通常ページ表示だけでは event log を必須にしない |
| アプリ例外 | local browser + local log | pass | pass | ブラウザで `/assets/999999999` が 404 エラー画面になり、画面の Request ID と同じ値で `eventType=APPLICATION_EXCEPTION status=404 path=/assets/999999999 exceptionType=ResponseStatusException` が出た |
| AccessDenied | local browser + local log | pass | pass | `WORKOPS_LOCAL_COGNITO_SUB=00000000-0000-0000-0000-000000000001` で起動し、`/auth/authorization/manager` が 403 エラー画面になった。`eventType=AUTHORIZATION_DENIED reasonCode=AUTHORIZATION_DENIED exceptionType=AccessDeniedException` が出た |

## NG 対応後のローカル E2E 実施結果

2026-06-19 に NG 対応後の local profile E2E を実施した。

| 対象 | 対応 | 結果 |
| --- | --- | --- |
| E2E-001 | `src/main/resources/static/favicon.ico` を追加 | pass。`/favicon.ico` は HTTP 200 `Content-Type: image/x-icon` で返り、favicon 由来の `APPLICATION_EXCEPTION` は出なかった |
| E2E-002 | `ErrorAttributes` 拡張で `/error` model に `requestId` を追加 | pass。`/auth/authorization/manager` の 403 画面に Request ID が表示され、同じ requestId で `AUTHORIZATION_DENIED` ログが出た |
| アプリ例外 | 既存の `ApplicationExceptionHandler` を維持 | pass。`/assets/999999999` は引き続き `APPLICATION_EXCEPTION status=404 path=/assets/999999999 exceptionType=ResponseStatusException` を出した |

## ローカル E2E 手順

ローカル DB を起動する。

```powershell
cd C:\git\workops
docker compose up -d workops-mysql
docker compose ps
```

通常ページとアプリ例外は既定の `TENANT_MANAGER` で起動して確認する。

```powershell
cd C:\git\workops\apps\web
Remove-Item Env:WORKOPS_LOCAL_COGNITO_SUB -ErrorAction SilentlyContinue
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=local"
```

ブラウザで次を確認する。

| ケース | URL | ブラウザ期待結果 | ログ期待結果 |
| --- | --- | --- | --- |
| requestId | `http://localhost:8080/` | HTTP 200。`X-Request-Id` が返る | 通常ページ表示だけでは event log を必須にしない |
| アプリ例外 | `http://localhost:8080/assets/999999999` | エラー画面。HTTP 404 | `eventType=APPLICATION_EXCEPTION exceptionType=ResponseStatusException status=404 path=/assets/999999999` |

AccessDenied は閲覧者で Spring Boot を起動し直して確認する。

```powershell
cd C:\git\workops\apps\web
$env:WORKOPS_LOCAL_COGNITO_SUB='00000000-0000-0000-0000-000000000001'
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=local"
```

ブラウザで次を確認する。

| ケース | URL | ブラウザ期待結果 | ログ期待結果 |
| --- | --- | --- | --- |
| AccessDenied | `http://localhost:8080/auth/authorization/manager` | エラー画面。HTTP 403 | `eventType=AUTHORIZATION_DENIED reasonCode=AUTHORIZATION_DENIED` |

業務操作ログはローカル DB の更新を伴う。実施する操作は、ローカル DB を使った資産または申請の登録・更新・拒否ケースに固定し、実施後に `com.example.workops.operation` の `result` と `reasonCode` を確認する。

## AWS dev E2E 手順

AWS dev 実確認前に、必ず profile と region を明示し、credential 環境変数を削除してから認証先を確認する。

```powershell
Remove-Item Env:AWS_ACCESS_KEY_ID -ErrorAction SilentlyContinue
Remove-Item Env:AWS_SECRET_ACCESS_KEY -ErrorAction SilentlyContinue
Remove-Item Env:AWS_SESSION_TOKEN -ErrorAction SilentlyContinue
$env:AWS_PROFILE='<user-approved-profile>'
$env:AWS_SDK_LOAD_CONFIG='1'
$env:AWS_REGION='ap-northeast-1'
$env:AWS_DEFAULT_REGION='ap-northeast-1'
aws sts get-caller-identity --region $env:AWS_REGION
```

実 account ID、SSO role ARN、SSO user は文書へ転記しない。確認結果を記録する場合は「AWS account は手元の AWS 認証で確認済み」とだけ書く。

ロググループを確認する。

```powershell
aws logs describe-log-groups `
  --region $env:AWS_REGION `
  --log-group-name-prefix '/workops/dev'
```

`/workops/dev/web` の log stream を新しい順に確認する。

```powershell
aws logs describe-log-streams `
  --region $env:AWS_REGION `
  --log-group-name '/workops/dev/web' `
  --order-by LastEventTime `
  --descending `
  --max-items 20
```

CloudWatch Logs Insights の基本形は次に揃える。

```text
fields @timestamp, @logStream, @message
| filter @message like /eventType=APPLICATION_EXCEPTION/
| sort @timestamp desc
| limit 50
```

確認用クエリ例:

```text
fields @timestamp, @logStream, @message
| filter @message like /requestId=/
| sort @timestamp desc
| limit 50
```

```text
fields @timestamp, @logStream, @message
| filter @message like /com.example.workops.security/
| filter @message like /eventType=AUTHENTICATION_REJECTED/
| sort @timestamp desc
| limit 50
```

```text
fields @timestamp, @logStream, @message
| filter @message like /reasonCode=USER_NOT_LINKED/
| sort @timestamp desc
| limit 50
```

```text
fields @timestamp, @logStream, @message
| filter @message like /reasonCode=ACTOR_TYPE_MISMATCH/
| sort @timestamp desc
| limit 50
```

```text
fields @timestamp, @logStream, @message
| filter @message like /eventType=AUTHORIZATION_DENIED/
| sort @timestamp desc
| limit 50
```

```text
fields @timestamp, @logStream, @message
| filter @message like /eventType=APPLICATION_EXCEPTION/
| sort @timestamp desc
| limit 50
```

AWS dev のブラウザ E2E は次で固定する。

| ケース | 操作 | CloudWatch Logs 期待結果 |
| --- | --- | --- |
| requestId | CloudFront HTTPS URL の通常ページを表示する | `X-Request-Id` と同じ `requestId` のログが出る |
| 認証成功 | Cognito Hosted UI から正常 login する | `eventType=AUTHENTICATION_SUCCEEDED result=SUCCESS reasonCode=-` |
| `users` 未突合 | Cognito user は存在するが `users` に紐づかない状態で login する | `eventType=AUTHENTICATION_REJECTED reasonCode=USER_NOT_LINKED userId=-` |
| actor_type 不整合 | login 導線と DB user の actor_type を不一致にして login する | `eventType=AUTHENTICATION_REJECTED reasonCode=ACTOR_TYPE_MISMATCH` |
| AccessDenied | `TENANT_MANAGER` 以外のユーザーで `/auth/authorization/manager` へアクセスする | `eventType=AUTHORIZATION_DENIED reasonCode=AUTHORIZATION_DENIED` |
| アプリ例外 | `/assets/999999999` へアクセスする | `eventType=APPLICATION_EXCEPTION status=404 path=/assets/999999999` |
| 未ログイン | cookie なしで保護ページへアクセスする | Hosted UI へ遷移する。WorkOps app に到達しない場合、security event は必須にしない |
| 認証切れ | session / cookie を削除して保護ページへアクセスする | 再 login 導線へ遷移する。WorkOps app に到達しない場合、security event は必須にしない |

## エージェント確認

コーディングエージェントは次を実施する。

- P2-8-04 task file の作成・更新
- ローカル DB 起動確認
- `apps/web` の `local` profile 起動
- in-app browser によるローカル E2E
- ローカル Spring Boot コンソール出力で `requestId`、`eventType`、`reasonCode`、`APPLICATION_EXCEPTION` を確認
- Java test
- CDK build / test
- task file に secret、token、credential、実 Cognito `sub`、RDS の実更新値が含まれていないことの確認

Java:

```powershell
cd C:\git\workops\apps\web
.\mvnw.cmd test
```

CDK:

```powershell
cd C:\git\workops\infra\cdk
npm run build
npm test -- --runInBand
```

`npm run build` で `tsc` が見つからない場合、依存復旧を試みず、まず `node_modules` が Junction か確認する。`node_modules` が Junction でない、または存在しない場合は停止してユーザーに報告する。

実値を記録しない確認:

```powershell
Select-String -Path 'C:\git\workops\docs\implementation\phase2\agent-tasks\P2-8\*.md' `
  -Pattern 'password=', 'token=', 'secret=', 'cognito_sub = ', 'public ip', 'i-[0-9a-f]' `
  -Encoding UTF8
```

上記で手順説明以外の実値が含まれていないことを確認する。

## ユーザー確認

ユーザー確認では次を実施する。

- AWS dev の短命 Stack を作成する
- 現在の `apps/web` から Docker image を build し、ECR へ push する
- `AppRuntimeStack` deploy 前に ECR の対象 image tag / digest / pushedAt を確認する
- CloudFront HTTPS URL 経由で AWS dev ブラウザ E2E を実施する
- CloudWatch Logs `/workops/dev/web` で logger、`requestId`、`eventType`、`reasonCode` を確認する
- 必要な場合だけ RDS Console integrated CloudShell VPC で DB 状態を確認する
- 確認後、短命 Stack を cleanup する

AWS dev 実確認は、エージェント確認だけで完了扱いにしない。

## 対応範囲

- アプリケーションログ E2E テストケースの洗い出し
- ローカルで実施できる E2E と AWS dev でしか実施できない E2E の分離
- in-app browser によるローカル E2E 手順
- CloudWatch Logs `/workops/dev/web` のアプリログ確認手順
- 業務操作ログの確認手順
- 認証・認可ログの確認手順
- アプリ例外ログの確認手順
- エージェント確認とユーザー確認の分離
- P2-9 へのアプリログ確認観点の引き継ぎ

## 除外範囲

- ECS task 停止理由
- ECS service event 調査
- stopped task 調査
- container exit code 調査
- Flyway 失敗時の停止調査
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

- ログ E2E テストケースが一覧化されている
- ローカルで実施できるケースと AWS dev でしか実施できないケースが分かれている
- ECS task 停止理由、service event、stopped task、container exit code が対象外として明記されている
- Flyway 失敗時の停止調査が対象外として明記されている
- ローカル E2E の URL、期待画面、期待ログが記載されている
- AWS dev E2E の操作と CloudWatch Logs 期待結果が記載されている
- `requestId` による同一 request の追跡手順が記載されている
- `com.example.workops.operation` の業務操作ログ確認手順が記載されている
- `com.example.workops.security` の認証・認可ログ確認手順が記載されている
- `com.example.workops.application` の例外ログ確認手順が記載されている
- エージェント確認とユーザー確認が分けて記載されている
- P2-9 へのアプリログ確認観点が記載されている
- ローカルで実施可能なブラウザ E2E が実施されている
- Java test が成功している
- CDK build / test が成功している
- task file に secret、token、credential、実 Cognito `sub`、RDS の実更新値、public IP、EC2 instance id が含まれていない

## 確認方法

エージェント確認を実施する。

文書の差分確認:

```powershell
git diff -- docs/implementation/phase2/agent-tasks/P2-8/P2-8-04-cloudwatch-ecs-runbook-verification-and-handoff.md
```

ローカル E2E 実施結果は、次の形式で最終報告に含める。

| ケース | 実施場所 | 結果 | ログ確認 |
| --- | --- | --- | --- |
| requestId | local browser | pass / fail | pass / fail |
| アプリ例外 | local browser | pass / fail | pass / fail |
| AccessDenied | local browser | pass / fail | pass / fail |

AWS dev E2E はユーザー確認として残し、実施した場合は CloudWatch Logs の実値を伏せて結果だけ記録する。

## 引き継ぎ

- P2-9 の GitHub Actions OIDC deploy では、deploy 後に `/workops/dev/web` で `eventType=APPLICATION_EXCEPTION` が増えていないことを確認する
- P2-9 の deploy smoke check では、CloudFront HTTPS URL の通常ページで `X-Request-Id` が返り、CloudWatch Logs に同じ `requestId` が出ることを確認する
- P2-9 の Hosted UI login smoke check では、`eventType=AUTHENTICATION_SUCCEEDED result=SUCCESS reasonCode=-` を確認する
- P2-9 の権限 smoke check では、`eventType=AUTHORIZATION_DENIED reasonCode=AUTHORIZATION_DENIED` を確認する
- P2-9 でも AWS dev 実確認はユーザー確認として扱い、エージェント確認だけで完了扱いにしない
