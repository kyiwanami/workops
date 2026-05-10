# M1-02 ローカル設定と Docker Compose MySQL 前提作成

## 目的

local profile と Docker Compose MySQL の前提を作る。

## 対応範囲

- ルートに Docker Compose 設定を作成する
- MySQL コンテナの起動設定を作成する
- Spring Boot の local profile 設定を作成する
- Spring Boot からローカル MySQL へ接続するための設定値を定義する
- ローカル起動に必要な環境変数または固定値を明示する
- Spring Initializr 相当で生成済みの MySQL Driver / Flyway dependency を前提にする

## 対応ファイル

- `compose.yaml`
- `apps/server/src/main/resources/application-local.yml`
- `apps/server/pom.xml`

## 除外範囲

- 業務テーブル
- Flyway 初期 DDL 本体
- seed
- Testcontainers
- Cognito 連携
- AWS RDS 接続

## 完了条件

Docker Compose で MySQL を起動でき、Spring Boot 側に local profile の接続設定がある。

## 確認方法

- Docker Compose で MySQL コンテナを起動する
- local profile の DB 接続設定が Spring Boot から参照できる状態になっていることを確認する
- このタスクでは Flyway migration ファイルを作成していないことを確認する

## 実装時の記録

実装後に、次をこのファイルへ追記する。

- 実装方針
- 変更ファイル
- 確認結果
- 残課題

## 実装方針

ローカル DB は Docker Compose の MySQL 9.7.0 コンテナとして定義した。
Spring Boot の `local` profile には、Docker Compose MySQL へ接続する datasource 設定を追加した。
Flyway は有効化したが、このタスクでは migration ファイルと seed は作成していない。

## 変更ファイル

- `compose.yaml`
- `apps/server/src/main/resources/application-local.yml`

## 確認結果

- `docker compose config` が成功した。
- `docker manifest inspect mysql:9.7.0` で MySQL 9.7.0 イメージの存在を確認した。
- `docker compose up -d workops-mysql` が成功した。
- `workops-mysql` は `healthy` になった。
- `apps/server` で `.\mvnw.cmd test` が成功した。
- `apps/server` で `.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=local" "-Dspring-boot.run.jvmArguments=-Dspring.main.web-application-type=none"` が成功した。
- `db/migration` と `db/seed` 配下のファイルは作成していない。

## 残課題

- M2 で Flyway 初期 DDL と seed を作成する。
