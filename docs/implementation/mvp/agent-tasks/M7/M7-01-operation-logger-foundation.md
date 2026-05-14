# M7-01 業務操作ログ基盤

## 目的

MVP の主要更新系業務操作を追跡するため、Spring Boot 標準の SLF4J + Logback を使い、key=value 形式の業務操作ログを出力する薄い共通部品を作る。

## M7 共通方針

- M7 で扱うログは、システムログ基盤ではなく業務操作ログとする
- 目的は、主要な業務操作について、誰が、どの会社で、どの対象に対して、どの操作を行い、成功または拒否されたかを追えるようにすることとする
- 出力基盤は Spring Boot 標準の SLF4J + Logback を使う
- 追加のロギングライブラリ、JSON ログ基盤、Logstash Encoder、Log4j2 への切替は MVP では行わない
- 業務操作ログは Service 層の業務処理の文脈で明示的に出力する
- AOP、独自アノテーション、汎用監査ログ基盤のような大きな抽象化は MVP では採用しない
- ログ項目の組み立ては `OperationLogger` のような薄い共通部品で key=value 形式を揃える
- ログ形式は JSON ではなく key=value 形式の構造化風メッセージにする
- 参照系は M7 では対象外とし、主要な更新系業務操作だけを対象にする
- 成功した主要業務操作は INFO で出力する
- 認可拒否、業務ルール違反などの想定内拒否は WARN で出力する
- 想定外例外は M7 で個別に抱え込まず、既存の例外処理・アプリケーションログに任せる
- ログ項目は ID 中心にする
- 個人情報・業務本文はログに出さない
- `title`、`content`、`review_comment` 本文、資産名、メールアドレス、氏名、自由入力本文、`before_json`、`after_json` はログに出さない
- `review_comment` などは本文ではなく、有無だけを `reasonCommentPresent` として出力する
- M7 では DB 変更を行わない
- Mapper / Testcontainers 強化は M8 に寄せる
- `apps/web` を正として進める

## 対応範囲

- `OperationLogger` 相当の共通コンポーネントを追加する
- INFO 用の成功ログ出力メソッドを用意する
- WARN 用の想定内拒否ログ出力メソッドを用意する
- key=value 形式の出力順序を固定する
- `requestId` を MDC から取得する
- MDC に `requestId` が存在しない場合は `requestId=-` として出力する
- `userId`、`companyId`、`actorType`、`authorities` を `LoginUserContext` 由来で出力できるようにする
- `operation`、`targetType`、`targetId`、`result`、`reasonCode`、`reasonCommentPresent`、`exceptionType` を出力できるようにする
- 値が存在しない任意項目は `-` で出力する
- boolean 項目は `true` / `false` で出力する
- `authorities` は複数権限をカンマ区切りで出力する
- ログ値に空白が含まれないよう、列挙値、ID、boolean、例外クラス名だけを出力対象にする
- 出力対象の文字列項目に空白が含まれる場合は、業務本文を出さず固定コードへ寄せる
- `OperationLogger` の出力形式確認を追加する

## ログ基本項目

- `requestId`
- `userId`
- `companyId`
- `actorType`
- `authorities`
- `operation`
- `targetType`
- `targetId`
- `result`
- `reasonCode`
- `reasonCommentPresent`
- `exceptionType`

## 対応ファイル

- `apps/web/src/main/java/com/example/workops/common/logging/OperationLogger.java`
- `apps/web/src/main/java/com/example/workops/common/logging/OperationLogRecord.java`
- `apps/web/src/test/java/com/example/workops/common/logging/OperationLoggerTests.java`
- `docs/implementation/mvp/agent-tasks/M7/M7-01-operation-logger-foundation.md`

## 除外範囲

- 監査ログテーブル
- 履歴テーブル
- ログ閲覧画面
- ログ検索
- JSON ログ基盤
- Logstash Encoder
- Log4j2 への切替
- Spring Boot Actuator / Micrometer / Observation を使った可観測性設計
- OpenTelemetry / 分散トレーシング
- CloudWatch Logs 連携実装
- AOP による全メソッド横断ログ
- 独自アノテーション
- 汎用監査ログ基盤
- `before_json`
- `after_json`
- DB 変更
- Mapper / Testcontainers 強化

## 完了条件

`OperationLogger` 相当の薄い共通部品で、M7 共通方針に沿った key=value 形式の業務操作ログを INFO / WARN で出力できる。
ログ基本項目の出力順序、未設定値の表現、コメント本文を出さない表現が固定されている。

## 確認方法

- `cd apps/web && .\mvnw.cmd test`
- `OperationLogger` の出力形式確認で、key=value 形式になっていることを確認する
- `requestId` が MDC にない場合に `requestId=-` になることを確認する
- `authorities` がカンマ区切りで出力されることを確認する
- 任意項目が未設定の場合に `-` で出力されることを確認する
- `reasonCommentPresent` が本文ではなく `true` / `false` で出力されることを確認する
- JSON 形式、個人情報、業務本文、`before_json`、`after_json` を出力しないことを確認する

## 実装時の記録

### 実装方針

- Spring Boot 標準の SLF4J + Logback だけを使い、追加依存は入れない
- Service 層から直接呼び出す前提の薄い `OperationLogger` と、ID 中心の `OperationLogRecord` を追加する
- ロガー名は `com.example.workops.operation` とし、通常のクラスロガーとは分ける
- `requestId` は MDC の `requestId` から取得し、未設定時は `-` として出力する
- 出力順序は `requestId userId companyId actorType authorities operation targetType targetId result reasonCode reasonCommentPresent exceptionType` で固定する
- コメント本文や業務本文を持てないよう、`OperationLogRecord` のフィールドは ID、コード、boolean、例外種別だけにする
- M7-01 では Service への組み込みは行わず、共通部品と出力形式確認までに限定する

### 変更ファイル

- `apps/web/src/main/java/com/example/workops/common/logging/OperationLogger.java`
- `apps/web/src/main/java/com/example/workops/common/logging/OperationLogRecord.java`
- `apps/web/src/test/java/com/example/workops/common/logging/OperationLoggerTests.java`
- `docs/implementation/mvp/agent-tasks/M7/M7-01-operation-logger-foundation.md`

### 実装結果

- `OperationLogger` を追加し、`logSuccess` で INFO、`logRejected` で WARN の業務操作ログを出力できるようにした
- `OperationLogRecord` を追加し、`LoginUserContext`、操作コード、対象種別、対象ID、理由コード、理由コメント有無、例外種別だけを保持するようにした
- `targetId`、`reasonCode`、`exceptionType`、`requestId` の未設定値は `-` として出力するようにした
- `authorities` は `LoginUserContext.permissionSets().code()` を現在順のカンマ区切りで出力し、空の場合は `-` とした
- `OperationLoggerTests` を追加し、key=value 形式、INFO / WARN、MDC 未設定、複数権限、null / 空文字の `-` 表現、業務本文フィールドを持たないことを確認した

### 確認結果

- `cd apps/web && .\mvnw.cmd test` 成功
- 全 76 件が成功
- `OperationLoggerTests` は 4 件成功
- `git diff --check` で空白エラーなし

### 残課題

- 申請 Service への組み込みは M7-02 で扱う
- 資産 Service への組み込みは M7-03 で扱う
- マスタ Service への組み込みは M7-04 で扱う
- `requestId` を設定する Filter は M7-01 では追加していない
