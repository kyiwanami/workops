# P2-8-02 security event logging

## 目的

AWS dev 上で、認証失敗、認可拒否、WorkOps ユーザー未突合、actor_type 不整合、権限セット不正、WorkOps ユーザー確定後の認証成功を CloudWatch Logs から調査できるようにする。

業務操作ログとは logger を分けるが、既存 `OperationLogger` と同じ `key=value` 形式に寄せる。

## ユーザー要求

- `OperationLogger` は使い回さず、形式だけ揃えた `SecurityEventLogger` / `SecurityEventLogRecord` を作る
- 認証失敗は Spring Security 公式の `AuthenticationFailureEvent` 系を第一候補にする
- 認可拒否は Spring Security 公式の `AuthorizationDeniedEvent` を第一候補にする
- WorkOps 固有の `users` 未突合、actor_type 不整合、権限セット不正、WorkOps ユーザー確定後の認証成功は既存の DB 突合境界で出す
- AOP では拾わない
- `sub`、`subHash`、email、username、token、secret は通常ログに出さない
- 必須キーは `requestId`、`eventType`、`reasonCode`
- DB 突合後は `userId` も出す
- ログ形式は既存業務操作ログと同じ `key=value` にする
- P2-8 では CloudWatch Logs での調査性を作るが、監視通知や Dashboard は作らない

## 前提

- P2-8-01 が完了している
- ログ行には `logging.pattern.level` により `requestId` が付与される
- P2-7 が完了し、PLATFORM / TENANT App Client 分離と実 Cognito 接続が成立している
- `CognitoAuthenticationSuccessHandler` は Cognito ログイン後に `users.cognito_sub` で DB 突合している
- `DbLoginUserContextFactory` は DB 由来の `LoginUserContext` と `GrantedAuthority` 相当の権限セットを作る
- `OperationLogger` は業務操作ログ専用として残す
- P2-8 では Cognito 内部ログ取得を目的にしない
- 既存データ移行と後方互換性は考慮しない

## 案

- `com.example.workops.common.logging` または `com.example.workops.common.security` 配下に `SecurityEventLogger` を追加する
- `SecurityEventLogger` の logger name は `com.example.workops.security` に固定する
- `SecurityEventLogRecord` を追加し、認証・認可イベントのログ項目を保持する
- `SecurityEventLogRecord` は `eventType`、`result`、`reasonCode`、`userId`、`companyId`、`actorType`、`authorities`、`exceptionType` を持つ
- `SecurityEventLogger` は値なしを `-` として出力する
- `SecurityEventLogger` は `INFO` と `WARN` を使い分ける
- WorkOps ユーザー確定後の認証成功は `INFO` で出す
- 認証失敗、認可拒否、`users` 未突合、actor_type 不整合、権限セット不正は `WARN` で出す
- `SecurityEventLogger` の出力は `key=value` 形式にする
- ログ本文には `sub`、`subHash`、email、username、token、secret を含めない
- `DefaultAuthenticationEventPublisher` を Bean 登録する
- `AbstractAuthenticationFailureEvent` を `@EventListener` で受け、`eventType=AUTHENTICATION_FAILED` として出力する
- `AuthorizationEventPublisher` として `SpringAuthorizationEventPublisher` を Bean 登録する
- `AuthorizationDeniedEvent` を `@EventListener` で受け、`eventType=AUTHORIZATION_DENIED` として出力する
- `CognitoAuthenticationSuccessHandler` で WorkOps ユーザー確定後に `eventType=AUTHENTICATION_SUCCEEDED` を 1 件だけ出す
- `CognitoAuthenticationSuccessHandler` で `users` 未突合時に `eventType=AUTHENTICATION_REJECTED reasonCode=USER_NOT_LINKED` を出す
- `CognitoAuthenticationSuccessHandler` で actor_type 不整合時に `eventType=AUTHENTICATION_REJECTED reasonCode=ACTOR_TYPE_MISMATCH` を出す
- `DbLoginUserContextFactory` の権限セット不正は `SecurityEventLogger` で `eventType=AUTHENTICATION_REJECTED reasonCode=INVALID_PERMISSION_SET` として出す
- 権限セット未割当は `reasonCode=PERMISSION_SET_NOT_ASSIGNED` として出す
- actor_type 自体が不正な場合は `reasonCode=INVALID_ACTOR_TYPE` として出す
- `SecurityEventLoggerTests` を追加し、出力形式、空値、禁止値が出ないことを確認する
- `CognitoAuthenticationSuccessHandlerTests` を更新し、成功、未突合、actor_type 不整合で期待する security event が出ることを確認する
- 認証失敗 event listener と認可拒否 event listener の単体テストを追加する

## ADR

- Spring Security 公式イベントを優先する。認証失敗と認可拒否は Spring Security の責務境界で発生するため、AOP や業務 Service 層で拾うより公式 event を使う方が境界が明確になるため。
- `OperationLogger` は使い回さない。`OperationLogger` は申請、資産、マスタなどの業務操作用であり、`operation`、`targetType`、`targetId`、`reasonCommentPresent` を前提にしているため、認証・認可イベントを無理に詰めると意味が崩れる。
- `SecurityEventLogger` を新設する。logger name と record model は分け、出力形式だけ既存の `key=value` に揃えることで、CloudWatch Logs で種類別に絞り込めるようにするため。
- AOP は採用しない。未ログイン、認証切れ、OAuth2 ログイン失敗、Spring Security Filter / Method Security の拒否は、Controller / Service AOP だけでは正しく覆えないため。
- 認証成功ログは WorkOps ユーザー確定後の 1 件に限定する。Cognito 認証成功だけでは業務利用者として確定しておらず、成功ログを増やすと CloudWatch Logs の量と調査ノイズが増えるため。
- `sub`、`subHash`、email、username、token、secret は通常ログに出さない。CloudWatch Logs へ永続的な利用者識別子や認証関連値を増やさず、`requestId`、`eventType`、`reasonCode`、突合後の `userId` で調査するため。
- 既存データ移行と後方互換性は考慮しない。

## 対応範囲

- `SecurityEventLogger` の追加
- `SecurityEventLogRecord` の追加
- `DefaultAuthenticationEventPublisher` Bean の追加
- 認証失敗 event listener の追加
- `SpringAuthorizationEventPublisher` Bean の追加
- 認可拒否 event listener の追加
- `CognitoAuthenticationSuccessHandler` の security event 出力
- `DbLoginUserContextFactory` の不正コンテキスト security event 出力
- security event の自動テスト
- 禁止値がログに出ないことの自動テスト

## 除外範囲

- `OperationLogger` の使い回し
- AOP による認証・認可ログ収集
- Spring Security DEBUG / TRACE ログへの依存
- Cognito 内部ログ取得
- Cognito Trigger
- Pre Token Generation
- Cognito group / scope を業務権限の正本にする変更
- 初回ログイン時の `users` 自動作成
- `users` 未突合時の自動復旧
- `sub` の通常ログ出力
- `subHash` の通常ログ出力
- email / username の通常ログ出力
- token / secret / password の通常ログ出力
- CloudWatch Alarm
- Observability Dashboard
- OpenTelemetry
- 分散トレーシング
- AWS deploy / destroy

## 完了条件

- `SecurityEventLogger` が `com.example.workops.security` logger へ出力する
- security event が `key=value` 形式で出力される
- security event の必須キーとして `eventType` と `reasonCode` が出力される
- ログ行には P2-8-01 の `requestId` が付与される
- DB 突合後の security event には `userId` が出力される
- WorkOps ユーザー確定後の認証成功が 1 件だけ INFO で出力される
- 認証失敗が WARN で出力される
- 認可拒否が WARN で出力される
- `users` 未突合が `reasonCode=USER_NOT_LINKED` で出力される
- actor_type 不整合が `reasonCode=ACTOR_TYPE_MISMATCH` で出力される
- 権限セット未割当が `reasonCode=PERMISSION_SET_NOT_ASSIGNED` で出力される
- 権限セット不正が `reasonCode=INVALID_PERMISSION_SET` で出力される
- actor_type 不正が `reasonCode=INVALID_ACTOR_TYPE` で出力される
- security event に `sub`、`subHash`、email、username、token、secret が出力されない
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
.\mvnw.cmd '-Dtest=SecurityEventLoggerTests,CognitoAuthenticationSuccessHandlerTests,*SecurityEvent*Tests' test
```

禁止値の実装残存確認:

```powershell
Select-String -Path 'C:\git\workops\apps\web\src\main\java\com\example\workops\**\*.java' `
  -Pattern 'cognitoSub=\{\}', 'subHash', 'email=', 'username=', 'token=', 'secret=' `
  -Encoding UTF8
```

上記で security event logger の通常ログ出力に禁止値が含まれていないことを確認する。

## 引き継ぎ

- P2-8-04 では CloudWatch Logs 上で `logger=com.example.workops.security`、`eventType`、`reasonCode` を検索する手順を記載する
- P2-9 では GitHub Actions deploy 後の Hosted UI / ECS 確認時に、security event が CloudWatch Logs へ出ていることを確認観点に含める
