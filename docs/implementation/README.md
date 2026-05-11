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

## UI フィードバック方針

- 保存・更新・削除など、画面遷移後にユーザーへ完了を知らせる通知は Bootstrap Toast で表示します。
- フォーム上部の alert は、入力エラーや画面内に残す必要がある警告に使います。
- MVP では SPA 化せず、SSR の flash message を Toast として表示します。

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
ただし、Spring Boot アプリ本体は公式 Spring Initializr 相当の最小構成で `apps/web` に生成します。

MVP の実装対象は `apps/web` の Spring Boot + Thymeleaf Web 業務アプリです。
`apps/api` と `apps/batch` は後続 Phase の配置先として予約しますが、MVP では実装を置きません。
Java package 名は配置変更では変更しません。

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
- Spring Security OAuth2 Login
- DevTools

MVP では、業務機能としてのログイン画面は作りません。
ただし、認証境界の手戻りを防ぐため、M3 で Cognito OAuth2 Login の最小接続を確認します。
M3 では、ユーザーが AWS マネジメントコンソールで作成したダミー Cognito User Pool、client secret なしの App Client、Hosted UI / managed login domain、テストユーザーを使います。
Callback URL は Spring Security OAuth2 Login のデフォルトに合わせて `http://localhost:8080/login/oauth2/code/cognito` を使います。

M3 では Cognito `sub` と `users.cognito_sub` の突合を確認し、DB由来の `LoginUserContext` を Spring Security の `Authentication.principal` として保持します。
local profile では `LocalAuthenticationFilter` がローカル用 `cognito_sub` から同じDB由来の `LoginUserContext` / `GrantedAuthority` を作ります。
Service 層は Cognito に直接依存せず、Spring Security の現在ユーザー、またはそれを読む `CurrentUserProvider` へ依存します。

Cognito 本格連携、PLATFORM / TENANT App Client 分離、Cognito Trigger、Pre Token Generation、CDK による Cognito 構築、AWS dev 環境デプロイは Phase 2 で扱います。

### M3 Cognito 最小接続の前提

M3 では、ユーザーが AWS マネジメントコンソールで次を用意します。

- ダミー Cognito User Pool
- client secret なしの App Client
- Hosted UI / managed login domain
- Cognito テストユーザー

Callback URL と logout URL は次を使います。

```text
Callback URL: http://localhost:8080/login/oauth2/code/cognito
Logout URL:   http://localhost:8080/
```

M3-02 以降、アプリは Cognito 接続値を環境変数で受け取ります。
実値はリポジトリに書きません。
ローカル開発ではルート `.env.local` に実値を書き、`.env.local` は git 管理しません。
キー名とダミー値だけを `.env.example` に置きます。

```text
WORKOPS_COGNITO_ISSUER_URI
WORKOPS_COGNITO_CLIENT_ID
WORKOPS_COGNITO_REDIRECT_URI
```

PowerShellでCognito接続確認用に起動する場合は、`apps/web` で `.\mvnw.cmd spring-boot:run` を実行します。
通常のローカル開発では `local` profile を付けるため、Cognitoへリダイレクトしません。
再度Cognito接続を確認する場合だけ、`local` profile を外して起動します。

M3 で業務利用する Cognito claim は、DB `users` と突合するための `sub` だけです。
`username`、`email`、`actorType`、`companyId`、`permissionSets` はDB由来の情報として扱います。
MVP / M3 の OAuth2 scope は `openid` と `email` だけを使います。
`profile` と `aws.cognito.signin.user.admin` はM3時点では不要です。

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

## 疎通確認結果の扱い

Cognito など外部連携の疎通確認で得られた値や手順は、以降の実装で再利用できる確認資産として記録します。
ただし、疎通確認で成功した値をそのまま業務DBやseedへ固定投入してはいけません。

今回の Cognito 疎通確認では、Spring Security OAuth2 Login から Cognito `sub` を取得できることを確認しました。
将来 `users.cognito_sub` と突合する実装を行う場合は、次を守ります。

- DB突合は別タスクとして扱い、ユーザー合意後に実装する
- 実Cognito `sub` をリポジトリ管理seedへ入れるか、ローカル専用データとして扱うかを先に確認する
- 突合キーは `sub` だけにする
- `username` / `email` / `actorType` / `companyId` / `permissionSets` はDB由来にする
- Cognito claim 由来の `username` / `email` を業務利用しない
- `users.status` / `userStatus` は作らず、ログイン可否と無効化はCognito側で制御する

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
