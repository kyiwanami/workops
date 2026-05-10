# M1-05 README 最小起動手順作成

## 目的

M1 時点のローカル起動手順を README に残す。

## 対応範囲

- ルート README を作成する
- M1 時点の前提ツールを明示する
- Spring Boot アプリは公式 Spring Initializr 相当の最小構成から始めたことを明示する
- 完成済み CRUD サンプル、JPA Quickstart、Spring Security ログインサンプルを流用していないことを明示する
- Docker Compose MySQL の起動手順を記載する
- Spring Boot の起動手順を記載する
- 確認 URL を記載する
- M1 時点で未実装の範囲を明示する

## 対応ファイル

- `README.md`

## 除外範囲

- MVP 全体の操作説明
- 申請管理手順
- 資産管理手順
- マスタ管理手順
- AWS デプロイ手順
- Phase 2 以降の手順
- Cognito ログイン手順
- Spring Security OAuth2 Login 手順

## 完了条件

README に従って M1 時点のアプリ起動確認ができる。

## 確認方法

- README の手順に従って Docker Compose MySQL を起動する
- README の手順に従って Spring Boot アプリを起動する
- README に記載した確認 URL で画面を表示する

## 実装時の記録

実装後に、次をこのファイルへ追記する。

- 実装方針
- 変更ファイル
- 確認結果
- 残課題
