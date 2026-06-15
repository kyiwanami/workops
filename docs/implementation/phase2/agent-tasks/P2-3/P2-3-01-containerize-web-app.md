# P2-3-01 apps/web コンテナ化

## 目的

MVP で完成した `apps/web` を、AWS dev の ECS Fargate で起動できる Docker image として build できる状態にする。
P2-3 では GitHub Actions や CodeBuild を使わず、手動 build / push の前提となる Dockerfile とローカル確認手順を作る。

## ユーザー要求

- Phase2-3 の実装に入る前に、`docs/implementation/phase2/agent-tasks/P2-3/` 配下へ task Markdown を作成する
- タスク数は後で調整可能だが、初期案は5本構成とする
- `DataStack` 再deploy、RDS再確認、HTTPS、ACM、Route 53、Cognito Hosted UI、GitHub Actions deploy は P2-3 task 範囲に含めない
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
- git commit

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
