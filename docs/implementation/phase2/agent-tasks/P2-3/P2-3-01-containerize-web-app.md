# P2-3-01 apps/web コンテナ化

## 目的

MVP で完成した `apps/web` を、AWS dev の ECS Fargate で起動できる Docker image として build できる状態にする。
P2-3 では GitHub Actions や CodeBuild を使わず、手動 build / push の前提となる Dockerfile とローカル確認手順を作る。

## ユーザー要求

- Phase2-3 の実装に入る前に、`docs/implementation/phase2/agent-tasks/P2-3/` 配下へ task Markdown を作成する
- タスク数は後で調整可能だが、初期案は5本構成とする
- `DataStack` 再deploy、RDS再確認、CloudFront、HTTPS 入口、ACM、Route 53、Cognito Hosted UI、GitHub Actions deploy は P2-3 task 範囲に含めない
- Dockerfile は multi-stage build とする
- Docker build 内で `mvnw -DskipTests package` を実行する
- runtime は Java 25 とする
- ECR image tag は `p2-3-manual` とする
- Docker build だけでなく、local profile でのローカルコンテナ起動確認まで含める

## 前提

- P2-2 が完了している
- `apps/web` の Maven test がローカルで成功する
- ローカル Docker が利用できる
- ローカル起動確認では AWS dev の RDS、Secrets Manager、SSM Parameter Store を使わない
- ローカル起動確認では既存の local profile とローカルDB接続設定を使う

## 案

- `apps/web/Dockerfile` を追加する
- `apps/web/.dockerignore` を追加する
- Dockerfile は build stage と runtime stage に分ける
- build stage では Maven Wrapper を使い、`./mvnw -DskipTests package` を実行する
- runtime stage では Java 25 runtime image を使う
- runtime container は Spring Boot jar を `java -jar` で起動する
- container port は `8080` とする
- image tag は P2-3 手動 deploy 用に `p2-3-manual` を使う
- ローカル確認用 tag は `workops-web:p2-3-local` を使う

## ADR

- Dockerfile はローカルの `target/` に依存しない。Docker build 単体で jar 作成まで完結させ、P2-9 の GitHub Actions や Phase 2α の CodeBuild へ移しやすくするため。
- Docker build 内では test を実行しない。test は P2-3-04 の手動 deploy 前確認として `.\mvnw.cmd test` で明示的に実行し、build と test の失敗原因を分けるため。
- runtime は Java 25 に固定する。`apps/web/pom.xml` の `java.version` と合わせるため。
- ECR image tag は `p2-3-manual` に固定する。P2-3 は CI 前の手動 deploy であり、commit SHA や GitHub run id を正本にしないため。
- `latest` を正本にしない。ECS task definition が参照する image を P2-3 の確認対象として説明しやすくするため。
- 既存データ移行と後方互換性は考慮しない。

## 対応範囲

- `apps/web/Dockerfile` を作成する
- `apps/web/.dockerignore` を作成する
- `.dockerignore` では `target/`、ログ、IDEファイル、Git管理外の一時成果物を除外する
- Dockerfile は Maven Wrapper を使う
- Dockerfile は Java 25 runtime image を使う
- Dockerfile は container port `8080` を明示する
- Docker image を `workops-web:p2-3-local` として build できるようにする
- local profile で container を起動できるようにする
- `http://localhost:8080/actuator/health` の応答確認は P2-3-02 完了後に行う

## 除外範囲

- ECR push
- ECS Task Definition
- ECS Service
- ALB
- NAT Gateway
- GitHub Actions workflow
- CodeBuild
- CodePipeline
- Docker image の脆弱性スキャン設定
- 本番用 image tag ルール
- Spring Boot Actuator の追加
- `/actuator/health` の認証除外
- AWS dev RDS 接続確認
- Cognito Hosted UI

## 完了条件

- `apps/web/Dockerfile` が存在する
- `apps/web/.dockerignore` が存在する
- Docker build がローカル `target/` に依存していない
- Docker build 内で Maven Wrapper による package が実行される
- runtime image が Java 25 である
- container port が `8080` である
- `workops-web:p2-3-local` として image build できる
- local profile で container を起動できる
- P2-3 の手動 deploy で使う tag が `p2-3-manual` として記録されている

## 確認方法

エージェント確認:

```powershell
cd C:\git\workops\apps\web
.\mvnw.cmd test
docker build -t workops-web:p2-3-local .
docker run --rm -p 8080:8080 `
  -e SPRING_PROFILES_ACTIVE=local `
  -e WORKOPS_DB_URL="jdbc:mysql://host.docker.internal:3306/workops?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Tokyo" `
  -e WORKOPS_DB_USERNAME=workops `
  -e WORKOPS_DB_PASSWORD=workops `
  workops-web:p2-3-local
```

別 terminal で確認する:

```powershell
Invoke-WebRequest -Uri http://localhost:8080/ -UseBasicParsing
```

P2-3-02 完了後は次も確認する:

```powershell
Invoke-WebRequest -Uri http://localhost:8080/actuator/health -UseBasicParsing
```

ユーザー確認:

- ローカル Docker 起動確認の結果を見る
- P2-3-04 で `p2-3-manual` tag の ECR push を確認する

---

## 実装時の記録

### 実装方針

`apps/web` の Docker build だけで Spring Boot jar を作成できるようにし、runtime image には Java 25 runtime と jar だけを配置する。
Docker build 内では Maven Wrapper で `./mvnw -DskipTests package` を実行し、ローカル `target/` には依存しない。
P2-3 手動 deploy 用 image tag は `workops-web:p2-3-manual`、ローカル確認用 image tag は `workops-web:p2-3-local` とする。

### 変更ファイル

- `apps/web/Dockerfile`
- `apps/web/.dockerignore`
- `docs/implementation/phase2/agent-tasks/P2-3/P2-3-01-containerize-web-app.md`

### 実装結果

- `apps/web/Dockerfile` を multi-stage build として追加した。
- build stage は `public.ecr.aws/amazoncorretto/amazoncorretto:25-al2023` を使い、Maven Wrapper で jar を作成する。build stage には Maven Wrapper が展開処理で使う `unzip`、`tar`、`gzip` を追加する。
- runtime stage は `public.ecr.aws/amazonlinux/amazonlinux:2023-minimal` に `java-25-amazon-corretto-headless` を入れ、`/app/workops-web.jar` を `java -jar` で起動する。
- container port は `8080` として明示した。
- `.dockerignore` で `target/`、ログ、IDE設定、OSファイル、ローカル環境ファイル、一時成果物を除外した。
- `apps/web/pom.xml` で `jackson-bom.version` を脆弱性修正版へ固定し、Pipeline 同等の Trivy gate を通す。

### Pipeline base image 方針

- P2-3 当時の `eclipse-temurin` 採用は Java 25 runtime 要件を満たすための手動 build 前提の判断だった。
- Pipeline では Docker Hub 匿名 pull に依存せず、AWS 側 registry と Amazon Corretto を Java 25 の base image 方針として採用する。
- CodeArtifact は Maven / npm dependency 管理に使い、Docker base image は container registry 管理として扱う。
- Amazon Linux 2023 minimal の runtime stage は `microdnf`、Amazon Corretto AL2023 の build stage は `dnf` を使う。local Docker build で package manager と Maven Wrapper 必須コマンドを確認し、Dockerfile に反映した。
- Pipeline の Maven integration test も Docker Hub 匿名 pull に依存しない。Testcontainers MySQL は ECR Public mirror を `mysql` compatible substitute として使い、CodeBuild の使い捨て build host では Ryuk を無効化する。

### 確認結果

- `cd C:\git\workops\apps\web; .\mvnw.cmd -DskipTests package`
  - 結果: 成功
  - `target/workops-web-0.0.1-SNAPSHOT.jar` が作成され、Spring Boot repackage まで完了した。
- `cd C:\git\workops\apps\web; .\mvnw.cmd test`
  - 結果: 失敗
  - 原因: Testcontainers 初期化時に Windows path `?C:\Users\rockw\AppData\Local\sqlite3` が `InvalidPathException` になり、`MapperIntegrationTestBase` の初期化に失敗した。
  - 単体テスト群は失敗前に通過しており、失敗は統合テストの Docker/Testcontainers 初期化で発生した。
- `cd C:\git\workops\apps\web; docker build -t workops-web:p2-3-local .`
  - 初回結果: 未完了
  - 初回原因: Docker Desktop Linux engine の named pipe `dockerDesktopLinuxEngine` に接続できず、Docker daemon が利用できない状態だった。
  - 再実行結果: 成功
  - Docker build 内で `./mvnw -DskipTests package` が実行され、`workops-web:p2-3-local` image を作成できた。
- `cd C:\git\workops; docker compose up -d workops-mysql`
  - 結果: 成功
  - `workops-mysql` が healthy になったことを確認した。
- `cd C:\git\workops\apps\web; docker run ... workops-web:p2-3-local`
  - 結果: 成功
  - `SPRING_PROFILES_ACTIVE=local`、`WORKOPS_DB_URL=jdbc:mysql://host.docker.internal:3306/workops?...`、`WORKOPS_DB_USERNAME=workops`、`WORKOPS_DB_PASSWORD=workops` で起動した。
  - `http://localhost:8080/` が HTTP 200 を返した。
  - ログで Java 25、Tomcat 8080、local profile、Flyway `Current version of schema workops: 8` を確認した。
  - 確認後、`workops-p2-3-local` container は削除した。
- 2026-06-29 P2-beta-09 再確認:
  - `linux/arm64` image は build 成功、Pipeline 同等 Trivy scan で OS / jar とも 0 件。
  - `linux/arm64` image を amd64 host で run すると `exec format error` になるため、ローカル実起動確認は同じ Dockerfile から `linux/amd64` image を別 tag で build して実施した。
  - `linux/amd64` image は build 成功、Trivy scan で OS / jar とも 0 件。
  - `workops-mysql` を使ったローカル実起動で `/actuator/health` が HTTP 200 を返すことを確認した。

### 残課題

- P2-3-02 完了後に `/actuator/health` のコンテナ内応答確認を行う。
- P2-3-04 で `workops-web:p2-3-manual` を ECR へ push する。
