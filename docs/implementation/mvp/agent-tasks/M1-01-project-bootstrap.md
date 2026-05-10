# M1-01 Maven / Spring Boot プロジェクト初期化

## 目的

`apps/web` に公式 Spring Initializr 相当の Maven ベース Spring Boot アプリを作り、最小起動できる状態にする。

## 対応範囲

- `apps/web` 配下に Spring Initializr 相当の Maven プロジェクトを作成する
- Spring Boot 4 系のアプリケーションエントリポイントを作成する
- Java 25 LTS を前提にする
- Maven Wrapper を含める
- 共通設定用の `application.yml` を作成する
- `.gitignore` を作成する
- `common` / `request` / `asset` / `master` / `audit` のパッケージ置き場を作成する
- `infra/cdk` は Phase 2 の置き場として README だけを作成する
- `.github/workflows` は Phase 2 以降の置き場として作成する
- Maven build が実行できる状態にする

Spring Initializr 相当の生成では、次の依存関係を含める。

- Spring Web
- Thymeleaf
- Validation
- MyBatis Framework
- Flyway Migration
- MySQL Driver
- Spring Boot Test
- Testcontainers
- Testcontainers MySQL
- DevTools

次の構成は流用しない。

- 完成済み CRUD サンプル
- JPA Quickstart
- Spring Security ログインサンプル
- 会社や個人のテンプレート

## 対応ファイル

- `.gitignore`
- `apps/web/pom.xml`
- `apps/web/mvnw`
- `apps/web/mvnw.cmd`
- `apps/web/.mvn/`
- `apps/web/src/main/java/com/example/workops/WorkOpsApplication.java`
- `apps/web/src/main/java/com/example/workops/common/`
- `apps/web/src/main/java/com/example/workops/request/`
- `apps/web/src/main/java/com/example/workops/asset/`
- `apps/web/src/main/java/com/example/workops/master/`
- `apps/web/src/main/java/com/example/workops/audit/`
- `apps/web/src/main/resources/application.yml`
- `apps/web/src/main/resources/application-dev.yml`
- `apps/web/src/main/resources/application-prod.yml`
- `infra/cdk/README.md`
- `.github/workflows/`

## 除外範囲

- DB 接続
- Docker Compose MySQL
- Thymeleaf 画面
- MyBatis 設定
- Flyway DDL
- seed
- 業務機能
- 認証・認可
- Spring Security
- AWS 連携
- CDK 実装
- GitHub Actions workflow 実装

## 完了条件

`apps/web` で Maven build が通り、Spring Boot アプリの起動前提ができている。
リポジトリには、MVP のアプリ実装と Phase 2 以降の置き場を追加できる最小構成ができている。

## 確認方法

- `apps/web` で Maven Wrapper による build を実行する
- Spring Boot アプリケーションがコンパイル対象として認識されることを確認する
- 完成済み CRUD サンプル、JPA Quickstart、Spring Security ログインサンプル由来の不要コードがないことを確認する

## 実装時の記録

実装後に、次をこのファイルへ追記する。

- 実装方針
- 変更ファイル
- 確認結果
- 残課題

## 実装方針

Spring Initializr 公式 API から Spring Boot 4.0.6 の Maven プロジェクトを生成し、`apps/web` に配置した。
Java はユーザー方針に従い Java 25 LTS を前提にした。
生成後、WorkOps の単一リポジトリ構成に合わせてアプリケーション名、設定ファイル、パッケージ置き場、Phase 2 用ディレクトリを整えた。

## 変更ファイル

- `.gitignore`
- `.github/workflows/.gitkeep`
- `apps/web/pom.xml`
- `apps/web/mvnw`
- `apps/web/mvnw.cmd`
- `apps/web/.mvn/wrapper/maven-wrapper.properties`
- `apps/web/src/main/java/com/example/workops/WorkOpsApplication.java`
- `apps/web/src/main/java/com/example/workops/common/.gitkeep`
- `apps/web/src/main/java/com/example/workops/request/.gitkeep`
- `apps/web/src/main/java/com/example/workops/asset/.gitkeep`
- `apps/web/src/main/java/com/example/workops/master/.gitkeep`
- `apps/web/src/main/java/com/example/workops/audit/.gitkeep`
- `apps/web/src/main/resources/application.yml`
- `apps/web/src/main/resources/application-dev.yml`
- `apps/web/src/main/resources/application-prod.yml`
- `apps/web/src/test/java/com/example/workops/WorkOpsApplicationTests.java`
- `infra/cdk/README.md`
- `docs/implementation/README.md`
- `docs/implementation/mvp/agent-tasks/M1-01-project-bootstrap.md`

## 確認結果

- Spring Boot は `4.0.6` を設定した。
- Java は `25` を設定した。
- Maven Wrapper は Maven `3.9.15` を取得する設定になっている。
- `.\mvnw.cmd test` は JDK 25 で成功した。
- Spring Security dependency は含まれていない。
- Spring Data JPA dependency は含まれていない。
- 完成済み CRUD サンプル由来の Controller / Entity / Repository は存在しない。
- 主要依存は、Spring Boot 管理または Spring Initializr 生成結果に従っている。
- 実効バージョンは Thymeleaf `3.1.5.RELEASE`、MyBatis Spring Boot Starter `4.0.1`、MySQL Connector/J `9.7.0`、Flyway `11.14.1`、JUnit Jupiter `6.0.3`、Mockito `5.20.0`、Testcontainers `2.0.5`。

## 残課題

- M1-01 の範囲ではなし。
