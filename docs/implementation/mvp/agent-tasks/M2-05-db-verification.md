# M2-05 DB verification

## 目的

M2 で作成した Flyway DDL と local seed が、local MySQL 上で再現できることを確認する。

## 対応範囲

- `docker compose down -v` で local DB を初期化する
- `docker compose up -d workops-mysql` で MySQL を起動する
- Spring Boot local profile 起動で Flyway migration を実行する
- M2 対象テーブルの存在確認 SQL を記録する
- seed 件数確認 SQL を記録する
- 確認結果を agent task 文書に記録する

## 対応ファイル

- `docs/implementation/mvp/agent-tasks/M2-05-db-verification.md`
- `README.md`

## 除外範囲

- DDL 追加
- seed 追加
- Mapper
- Service
- Controller
- Thymeleaf 画面
- Testcontainers
- AWS RDS 接続

## 完了条件

README 手順で local DB を作り直し、Flyway で MVP テーブルと local seed を再現できる。

## 確認方法

- `docker compose down -v` を実行する
- `docker compose up -d workops-mysql` を実行する
- `docker compose ps` で `workops-mysql` が `healthy` であることを確認する
- `cd apps/server && .\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=local"` を実行する
- MySQL で M2 対象テーブルの存在を確認する
- MySQL で seed 件数を確認する

## 実装時の記録

実装後に、次をこのファイルへ追記する。

- 実装方針
- 変更ファイル
- 確認結果
- 残課題
