# P2-3-02 Actuator health check 最小導入

## 目的

AWS dev の ALB / ECS health check 用に、Spring Boot Actuator の `/actuator/health` だけを最小導入する。
P2-3 では業務画面ログインや Cognito Hosted UI を確認せず、ECS task が ALB から正常判定されるための health endpoint を作る。

## ユーザー要求

- `spring-boot-starter-actuator` を最小導入する
- `/actuator/health` のみ公開する
- `/actuator/health` のみ認証除外する
- health 詳細は非表示にする
- 業務画面ログイン確認はしない
- metrics、Prometheus、詳細監視は P2-3 で扱わない

## 前提

- P2-3-01 が完了している
- `apps/web` の `!local` profile では Spring Security OAuth2 client 設定が有効になる
- P2-3 では Cognito User Pool、Hosted UI、App Client を作らない
- P2-3 では `/` などの業務画面アクセス成功を完了条件にしない

## 案

- `apps/web/pom.xml` に `spring-boot-starter-actuator` を追加する
- `application.yml` または `application-dev.yml` に Actuator health の最小設定を追加する
- 公開 endpoint は `health` のみにする
- health 詳細は `never` にする
- `SecurityConfig` の `!local` profile で `/actuator/health` を `permitAll` にする
- 他の request は従来どおり authenticated にする
- local profile でも `/actuator/health` にアクセスできるようにする

## ADR

- Actuator は ALB / ECS health check のためだけに最小導入する。P2-3 の目的は AWS dev HTTP 稼働確認であり、監視基盤全体の導入ではないため。
- `/actuator/health` は認証除外する。ALB health check は Cognito login を通れず、認証必須にすると target が unhealthy になるため。
- ALB の成功コードに `302` や `3xx` を追加しない。health check は機械的な正常判定であり、login redirect を正常扱いしないため。
- health 詳細は表示しない。DB URL や内部状態を利用者向け HTTP response に出さないため。
- P2-3 では業務画面ログインを確認しない。Cognito Hosted UI と users 突合は P2-5 の責務であるため。
- 既存データ移行と後方互換性は考慮しない。

## 対応範囲

- `spring-boot-starter-actuator` を追加する
- Actuator web exposure を `health` のみにする
- health details を `never` にする
- `/actuator/health` を Spring Security の認証対象外にする
- `/css/**`、`/favicon.ico` の既存 permit 設定は維持する
- `!local` profile の業務画面は authenticated のままにする
- Actuator health の応答をテストまたはローカル確認で確認する

## 除外範囲

- `/actuator/info`
- `/actuator/metrics`
- Prometheus
- Micrometer 全体設計
- OpenTelemetry
- CloudWatch Alarm
- Observability Dashboard
- readiness / liveness の詳細分割
- 独自 health controller
- ALB Cognito 認証
- Cognito Hosted UI
- users 突合
- DB 由来権限での画面操作

## 完了条件

- `spring-boot-starter-actuator` が追加されている
- `/actuator/health` のみ公開されている
- `/actuator/health` が `!local` profile で認証不要になっている
- `/actuator/health` の詳細が response に出ない
- `/actuator/health` が HTTP 200 を返す
- 通常業務画面の認証方針が P2-3 の範囲で変更されていない

## 確認方法

エージェント確認:

```powershell
cd C:\git\workops\apps\web
.\mvnw.cmd test
```

local profile のローカル起動後に確認する:

```powershell
Invoke-WebRequest -Uri http://localhost:8080/actuator/health -UseBasicParsing
```

期待する response:

- HTTP status が `200`
- body に `UP` が含まれる
- DB URL、password、secret value が含まれない

P2-3-04 の AWS dev 確認:

- ALB target group health check path が `/actuator/health`
- ALB target が healthy になる
- 業務画面ログイン成否は確認対象にしない

---

## 実装時の記録

### 実装方針

Spring Boot Actuator は ALB / ECS health check 用に限定して導入する。
公開する Actuator endpoint は `/actuator/health` のみとし、health details は表示しない。
`!local` profile では `/actuator/health` だけを認証不要にし、通常業務画面は従来どおり authenticated のままにする。

### 変更ファイル

- `apps/web/pom.xml`
- `apps/web/src/main/resources/application.yml`
- `apps/web/src/main/java/com/example/workops/common/security/SecurityConfig.java`
- `docs/implementation/phase2/agent-tasks/P2-3/P2-3-02-actuator-health.md`

### 実装結果

- `spring-boot-starter-actuator` を Spring Boot 管理バージョンで追加した。
- `management.endpoints.web.exposure.include` を `health` にした。
- `management.endpoint.health.show-details` を `never` にした。
- `!local` profile の Spring Security 設定で `/actuator/health` を `permitAll` にした。
- `local` profile の認証方針は変更していない。

### 確認結果

- `cd C:\git\workops\apps\web; .\mvnw.cmd test`
  - 結果: 成功
  - Tests run: 193, Failures: 0, Errors: 0, Skipped: 0
- `cd C:\git\workops\apps\web; docker build -t workops-web:p2-3-local .`
  - 結果: 成功
  - Docker build 内で `./mvnw -DskipTests package` が実行され、`workops-web:p2-3-local` image を作成できた。
- `cd C:\git\workops; docker compose up -d workops-mysql`
  - 結果: 成功
  - `workops-mysql` が healthy になったことを確認した。
- `cd C:\git\workops\apps\web; docker run ... workops-web:p2-3-local`
  - 結果: 成功
  - `SPRING_PROFILES_ACTIVE=local`、`WORKOPS_DB_URL=jdbc:mysql://host.docker.internal:3306/workops?...`、`WORKOPS_DB_USERNAME=workops`、`WORKOPS_DB_PASSWORD=workops` で起動した。
  - `http://localhost:8080/actuator/health` が HTTP 200 を返した。
  - response body は `{"groups":["liveness","readiness"],"status":"UP"}` で、DB URL、password、secret value、component details は含まれなかった。
  - ログで `Exposing 1 endpoint beneath base path '/actuator'` を確認した。
  - 確認後、`workops-p2-3-local` container は削除した。

### 残課題

- P2-3-03 で ALB target group の health check path を `/actuator/health` にする。
- P2-3-04 で AWS dev の ALB 経由 `/actuator/health` を確認する。
