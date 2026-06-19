# P2-8-03 application exception logging

## 目的

AWS dev 上で発生したアプリ例外を CloudWatch Logs から調査できるようにする。

認証・認可イベントとは logger を分け、HTTP request の `requestId`、例外種別、HTTP status、path を最小限の `key=value` 形式で出力する。

## ユーザー要求

- P2-8 は、AWS dev 上でアプリ例外を CloudWatch Logs から調査できる状態にする
- 認証・認可ログとは別 logger でアプリ例外ログを出す
- `eventType=APPLICATION_EXCEPTION`、`exceptionType`、HTTP status、path、`requestId` を出す
- フォーム入力値、業務本文、token、password、secret、DB 接続文字列は出さない
- P2-8 では監査ログテーブル、ログ検索画面、Alarm、Dashboard、OpenTelemetry を作らない

## 前提

- P2-8-01 が完了している
- HTTP request のログ行には `requestId` が付与される
- P2-8-02 の `SecurityEventLogger` は認証・認可イベント専用である
- `apps/web` は Spring MVC / Thymeleaf の画面アプリである
- 業務操作ログは既存 `OperationLogger` が担う
- 既存データ移行と後方互換性は考慮しない

## 案

- `ApplicationExceptionLogger` または同等の logger コンポーネントを追加する
- logger name は `com.example.workops.application` に固定する
- `ApplicationExceptionLogRecord` を追加する
- `ApplicationExceptionLogRecord` は `eventType`、`status`、`path`、`exceptionType` を持つ
- `eventType` は `APPLICATION_EXCEPTION` に固定する
- 値なしは `-` として出力する
- `@ControllerAdvice` を追加し、想定外例外を捕捉する
- `@ControllerAdvice` は例外ログを ERROR で出力する
- `@ControllerAdvice` は既存の画面エラー表示方針を壊さない
- `ResponseStatusException` など明示 status を持つ例外は status をログへ出す
- 明示 status を持たない例外は status `500` としてログへ出す
- path は `HttpServletRequest#getRequestURI()` から取得する
- query string はログへ出さない
- request body、form parameter、reason comment、業務本文はログへ出さない
- exception message はログ本文の `key=value` には出さない
- stack trace は通常の ERROR ログとして出力する
- stack trace に token、password、secret、DB 接続文字列を意図的に含める実装はしない
- `ApplicationExceptionLoggerTests` を追加し、出力形式と禁止値が出ないことを確認する
- `@ControllerAdvice` のテストを追加し、想定外例外で `eventType=APPLICATION_EXCEPTION` が出ることを確認する

## ADR

- アプリ例外 logger は security logger と分ける。例外は認証・認可イベントではなく、P2-8 の調査対象として種類が異なるため。
- `@ControllerAdvice` を採用する。Spring MVC の画面 request で発生した想定外例外を一箇所で捕捉し、CloudWatch Logs に調査用ログを残すため。
- query string と request body はログに出さない。検索条件、フォーム入力、コメント、業務本文、token などが含まれる可能性があるため。
- exception message は `key=value` 項目にしない。例外 message には外部ライブラリ由来の接続文字列や入力値が混ざる可能性があるため。
- stack trace は ERROR ログとして残す。例外調査には stack trace が必要であり、P2-8 の目的は CloudWatch Logs から原因入口を切り分けられる状態にすることだから。
- 監査ログテーブルは作らない。WorkOps の監査・履歴方針では、P2-8 のログは RDB ではなくアプリケーションログとして扱うため。
- 既存データ移行と後方互換性は考慮しない。

## 対応範囲

- アプリ例外用 logger の追加
- アプリ例外用 log record の追加
- `@ControllerAdvice` による想定外例外ログ
- HTTP status / path / exceptionType の出力
- query string、request body、form parameter を出さない実装
- アプリ例外ログの自動テスト
- 禁止値がログに出ないことの自動テスト

## 除外範囲

- 認証・認可イベントログ
- `SecurityEventLogger` への例外ログ混在
- 業務操作ログの変更
- 監査ログテーブル
- 履歴テーブル
- ログ検索画面
- JSON structured logging
- OpenTelemetry
- 分散トレーシング
- CloudWatch Alarm
- Observability Dashboard
- request body logging
- query string logging
- フォーム入力値 logging
- AWS deploy / destroy

## 完了条件

- アプリ例外が `com.example.workops.application` logger へ ERROR で出力される
- アプリ例外ログに `eventType=APPLICATION_EXCEPTION` が出力される
- アプリ例外ログに `status` が出力される
- アプリ例外ログに query string を含まない `path` が出力される
- アプリ例外ログに `exceptionType` が出力される
- ログ行には P2-8-01 の `requestId` が付与される
- request body、form parameter、業務本文、token、password、secret、DB 接続文字列が `key=value` 項目として出力されない
- Java test が成功している

## 確認方法

Java:

```powershell
cd C:\git\workops\apps\web
.\mvnw.cmd test
```

限定実行:

```powershell
cd C:\git\workops\apps\web
.\mvnw.cmd '-Dtest=ApplicationExceptionLoggerTests,*Exception*ControllerAdvice*Tests' test
```

禁止値の実装残存確認:

```powershell
Select-String -Path 'C:\git\workops\apps\web\src\main\java\com\example\workops\**\*.java' `
  -Pattern 'getQueryString', 'getParameter', 'getParameterMap', 'getInputStream', 'getReader', 'password=', 'token=', 'secret=' `
  -Encoding UTF8
```

上記でアプリ例外ログ用の実装が query string、request body、form parameter、credential 値をログ出力していないことを確認する。

## 引き継ぎ

- P2-8-04 では CloudWatch Logs 上で `logger=com.example.workops.application` と `eventType=APPLICATION_EXCEPTION` を検索する手順を記載する
- P2-9 では GitHub Actions deploy 後の smoke check でアプリ例外ログの確認先を Runbook から参照できるようにする
