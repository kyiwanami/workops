# WorkOps Implementation Docs

## 目的

このディレクトリは、WorkOps の実装指示、実装結果、確認結果をリポジトリ側で管理するためのものです。

Notion は、要件定義、基本設計、スコープ管理、ADR、Phase 別実装計画を管理します。
リポジトリは、コード、実装指示 Markdown、実装結果、README、テスト、実装詳細を管理します。

## 管理範囲

このディレクトリでは、各 Phase の実装フェーズ、合意済み agent task、実装時に決まった具体事項、確認結果を管理します。

MVP 固有の M1 から M8 の詳細は `mvp/phases.md` で管理します。
Phase 2 以降は、各 Phase の着手前に対応する `phases.md` を作成します。

## Notion とリポジトリの役割

Notion 側では、次を管理します。

- 要件定義
- 基本設計
- スコープ管理
- ADR
- Phase 別実装計画
- 実装順序
- 完了条件

リポジトリ側では、次を管理します。

- コード
- 実装指示 Markdown
- 実装結果
- README
- ローカル起動手順
- テスト
- Flyway DDL
- seed SQL
- MyBatis SQL
- Controller / Service / Mapper の実装詳細
- Form / DTO / Model の具体フィールド
- Thymeleaf テンプレート
- Bootstrap のコンポーネント / クラス詳細

## agent task 作成ルール

Notion 側では、Phase 内の大きな実装フェーズまでを定義します。
具体的な agent task 分割は、各 M フェーズまたは各 Phase 内フェーズの着手前にコーディングエージェントが提案します。

コーディングエージェントは、agent task 分割案として次を提示します。

- agent task 名
- 実装順序
- 対応範囲
- 対応ファイル
- 除外範囲
- 完了条件
- 確認方法

ユーザーが分割案を確認し、合意した後にだけ `agent-tasks/*.md` を作成します。
合意前に `agent-tasks/*.md` を作成しません。

## Spring Boot アプリ生成ルール

Git リポジトリはゼロから作成します。
ただし、Spring Boot アプリ本体は公式 Spring Initializr 相当の最小構成で `apps/server` に生成します。

## バージョン選定ルール

WorkOps では、LTS が明確に提供されている技術は現時点の LTS を使います。
LTS という扱いがない技術は、現時点の最新安定版を使います。
Spring Boot によって依存バージョンが管理されるライブラリは、個別に固定せず Spring Boot 管理バージョンを優先します。

MVP 開始時点の方針は次の通りです。

- Java は Java 25 LTS を使う
- MySQL はローカルでは MySQL 9.7 LTS を使う
- Maven は Maven Wrapper が取得する最新安定版を使う
- Spring Boot は最新安定版を使う
- Thymeleaf / Flyway / JUnit / Mockito / Testcontainers は Spring Boot 管理バージョンを使う
- Spring Boot 管理外の依存は、採用時点の最新安定版を明示する
- Bootstrap は導入時点の最新安定版を使う
- AWS CDK v2 は Phase 2 着手時点の最新安定版を使う

採用する生成方針は次の通りです。

- ビルドツールは Maven を使う
- Java 25 LTS を使う
- Spring Boot 4 系を使う
- Maven Wrapper を含める
- ランダムな完成済み CRUD サンプルを流用しない
- JPA Quickstart を流用しない
- Spring Security ログインサンプルを流用しない
- 会社や個人のテンプレートを流用しない
- WorkOps の MVP 方針に合わせて、生成後の構成を整える

MVP で使う Spring Boot アプリの依存関係は次を基本とします。

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

MVP では Cognito ログイン画面を作らないため、Spring Security は最初から導入しません。
認証済み利用者コンテキストは、M3 で `CurrentUserProvider` と local 疑似認証として作ります。
Spring Security OAuth2 Login と Cognito 連携は Phase 2 で扱います。

## Notion へ戻す判断

次の判断はコーディングエージェント側で確定しません。

- スコープ変更
- Phase 変更
- 認可方針変更
- DB 境界変更
- マスタ方針変更
- 監査ログ方針変更
- 技術スタック変更
- 画面範囲の追加または削除
- 要件定義、基本設計、スコープ管理、ADR と矛盾する実装判断

## 実装時の記録

agent task を実装した後は、対象の agent task Markdown に次を記録します。

- 実装方針
- 変更ファイル
- 実装結果
- 確認結果
- 残課題

実装で判明した具体事項はリポジトリ側へ記録します。
Notion 側の要件、スコープ、ADR を変更する必要がある場合は、リポジトリ側で確定せず、ユーザー確認を挟みます。

## MVP の PR 単位

MVP は一括で実装せず、次の PR 単位で薄く積みます。

- PR-001 repository bootstrap
- PR-002 local database foundation
- PR-003 local auth and authorization foundation
- PR-004 request module vertical slice
- PR-005 request workflow
- PR-006 asset module vertical slice
- PR-007 tests hardening
- PR-008 cleanup

各 PR の具体的な agent task は、対象フェーズの着手前にコーディングエージェントが提案し、ユーザー合意後に作成します。
