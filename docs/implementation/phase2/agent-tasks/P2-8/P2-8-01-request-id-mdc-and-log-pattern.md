# P2-8-01 requestId MDC / log pattern

## 目的

P2-8 の認証・認可・例外・業務操作ログを CloudWatch Logs 上で同一 HTTP request 単位に追えるようにする。

`apps/web` の HTTP request ごとに `requestId` を生成し、MDC と response header に設定する。Spring Boot の既定ログ形式は維持し、`logging.pattern.level` だけで `requestId` をログ行へ追加する。

## ユーザー要求

- P2-8 は、AWS dev 上で業務操作・認証・認可・例外・Flyway / ECS 停止理由を CloudWatch Logs から調査できる状態にする
- `requestId` は `OncePerRequestFilter` で生成して MDC へ入れる
- MDC への設定は `MDC.putCloseable("requestId", requestId)` を使う
- response header `X-Request-Id` に同じ値を返す
- `logging.pattern.level` に `requestId=%X{requestId:--}` を追加する
- `logging.pattern.console` は全面上書きしない
- HTTP request 外の Flyway 起動ログは `requestId=-` として扱う
- Plan Mode 中にファイル作成はしない。Default mode で本 task Markdown を作成する

## 前提

- P2-7 が完了している
- `apps/web` は Spring Boot / Spring MVC の Servlet 構成である
- 既存の `OperationLogger` は `MDC.get("requestId")` を参照している
- P2-8 では OpenTelemetry、分散トレーシング、Observability Dashboard、CloudWatch Alarm を作らない
- P2-8 では JSON ログ基盤へ全面移行しない
- 既存データ移行と後方互換性は考慮しない

## 案

- `apps/web` に `RequestIdMdcFilter` を追加する
- `RequestIdMdcFilter` は `OncePerRequestFilter` を継承する
- `RequestIdMdcFilter` は Spring Bean として登録する
- `RequestIdMdcFilter` は Security filter chain の内外に依存せず、通常の HTTP request 全体に `requestId` を付与する
- `requestId` は `UUID.randomUUID().toString()` で生成する
- MDC key は `requestId` に固定する
- response header 名は `X-Request-Id` に固定する
- `MDC.putCloseable("requestId", requestId)` を try-with-resources で使う
- try-with-resources の範囲内で `filterChain.doFilter(request, response)` を呼び出す
- filter 完了後、MDC の `requestId` は `MDCCloseable.close()` により削除される
- `application.yml` に `logging.pattern.level` を追加する
- `logging.pattern.level` の値は `requestId=%X{requestId:--} %5p` とする
- `logging.pattern.console` は設定しない
- 既存 `OperationLogger` の `requestId=` 出力は P2-8-01 では削除しない
- `OperationLogger` の `requestId` 重複整理は、P2-8-02 以降でログ形式全体を確認してから扱う
- HTTP request 外で出る Spring Boot 起動ログ、Flyway ログ、ECS task 停止理由では `requestId=-` になることを正常とする
- `RequestIdMdcFilterTests` を追加し、`X-Request-Id` header と MDC cleanup を確認する
- `application.yml` の `logging.pattern.level` は Java test または設定読み取りテストで確認する

## ADR

- `OncePerRequestFilter` を採用する。Spring Framework 公式が request dispatch ごとの単一実行を目的にした Filter 基底クラスとして提供しており、HTTP request 単位の MDC 設定に合うため。
- `MDC.putCloseable` を採用する。SLF4J 公式が close 時に key を削除する用途として提供しており、Logback が推奨する put / remove の対応漏れを避けやすいため。
- `X-Request-Id` を response header に返す。ブラウザ操作時のエラー画面、CloudWatch Logs、ユーザー報告を同じ request ID で照合できるようにするため。
- `logging.pattern.level` だけを変更する。Spring Boot 公式が MDC を既定ログ形式へ足す方法として `logging.pattern.level` を示しており、console pattern の全面上書きより既定形式を壊しにくいため。
- MDC 未設定時の pattern は `%X{requestId:--}` とする。`%X{requestId:-}` では未設定時の表示が空になり、HTTP request 外ログを `requestId=-` として扱う完了条件を満たさないため。
- JSON structured logging は採用しない。P2-8 は既存の `key=value` ログ方針を CloudWatch Logs で調査可能にするフェーズであり、JSON ログ基盤への全面移行は除外範囲であるため。
- HTTP request 外のログに `requestId=-` を許容する。Flyway 起動ログと ECS task 停止理由は HTTP request に紐づかないため。
- 既存データ移行と後方互換性は考慮しない。

## 対応範囲

- `RequestIdMdcFilter` の追加
- `X-Request-Id` response header の追加
- `requestId` MDC 設定と cleanup
- `logging.pattern.level` の追加
- requestId 付与の自動テスト
- MDC cleanup の自動テスト
- HTTP request 外ログの扱いを task / README に記載するための引き継ぎ

## 除外範囲

- `logging.pattern.console` の全面上書き
- JSON structured logging
- Logback XML の追加
- OpenTelemetry
- 分散トレーシング
- CloudWatch Alarm
- Observability Dashboard
- requestId を AWS X-Ray trace ID と連携する実装
- requestId を外部 request header から受け入れる実装
- 非同期処理への MDC 伝播
- `OperationLogger` の出力形式整理
- AWS deploy / destroy

## 完了条件

- HTTP request ごとに UUID 形式の `requestId` が生成される
- response header `X-Request-Id` に生成した `requestId` が設定される
- request 処理中のログへ `requestId` が出力される
- request 処理後に MDC の `requestId` が残らない
- `logging.pattern.level` に `requestId=%X{requestId:--}` が含まれている
- `logging.pattern.console` が追加されていない
- HTTP request 外ログは `requestId=-` として扱う方針が明記されている
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
.\mvnw.cmd '-Dtest=RequestIdMdcFilterTests' test
```

設定確認:

```powershell
Select-String -Path 'C:\git\workops\apps\web\src\main\resources\application.yml' `
  -Pattern 'logging:', 'pattern:', 'level:', 'requestId=%X\{requestId:--\}' `
  -Encoding UTF8
```

禁止設定確認:

```powershell
Select-String -Path 'C:\git\workops\apps\web\src\main\resources\application.yml' `
  -Pattern 'pattern:\s*console|logging\.pattern\.console' `
  -Encoding UTF8
```

上記の禁止設定確認で `logging.pattern.console` が出ないことを確認する。

## 引き継ぎ

- P2-8-02 以降のセキュリティログ、例外ログ、既存業務操作ログは、P2-8-01 で追加した `requestId` を前提に CloudWatch Logs 上で追跡する
- P2-8-04 では、HTTP request 外の Flyway 起動ログが `requestId=-` になることを Runbook に明記する
