# MVP Implementation Phases

## 前提

MVP のスコープは、Notion のスコープ管理で `Phase = MVP` かつ `状態 = 確定` の項目を正とします。
このファイルは、MVP スコープをコーディングエージェントへ渡せる実装フェーズへ整理するものです。
このファイルでは、MVP スコープ自体を変更しません。

具体的な agent task 分割は、各 M フェーズ着手前にコーディングエージェントが提案します。
ユーザーが分割案を確認し、合意した後にだけ `agent-tasks/Mx/*.md` を作成します。
このファイルでは、M1-01 / M1-02 のような具体タスク粒度を固定しません。

## 画面操作方針

論理削除、復活など、対象データの表示状態や利用可否を切り替える操作は、画面上の確認モーダルを経由して POST します。
一覧画面や詳細画面から、確認なしの直接 POST ボタンとして実装しません。

## M1. プロジェクト土台

### 目的

Maven / Spring Boot / Thymeleaf / MyBatis / Docker Compose MySQL の最小構成を作る。

### 成果物

- Maven ベースの Spring Boot アプリ
- Spring Initializr 相当で生成した `apps/web`
- Maven Wrapper
- `.gitignore`
- ローカル起動できる最小構成
- Docker Compose MySQL の前提
- Thymeleaf 画面表示の前提
- MyBatis 利用の前提
- Phase 2 用の `infra/cdk` 置き場
- 後続 Phase 用の `apps/api` / `apps/batch` 置き場
- `.github/workflows` 置き場

### 除外範囲

- 業務テーブル
- Flyway 初期 DDL 本体
- seed
- 申請管理
- 資産管理
- 認証・認可
- Spring Security
- `apps/api` / `apps/batch` の実装
- AWS 連携
- CDK 実装
- GitHub Actions deploy

### 完了条件

ローカルでアプリが起動し、以降の MVP 業務機能実装に入れる土台ができている。

### agent task 分割方針

この M フェーズの具体的な agent task 分割は、M1 着手前にコーディングエージェントが提案する。
Notion 側では、この時点で M1-01 / M1-02 のような具体タスク粒度は固定しない。

## M2. DB / Flyway / seed

### 目的

MVP の業務データを扱う DB 土台と 2 社 seed を作る。

### 成果物

- Flyway 初期 DDL
- companies
- users
- departments
- permission_sets
- user_permission_sets
- common_master
- common_master_values
- generic_master
- generic_master_values
- requests
- assets
- 共通マスタ初期データ
- 汎用マスタ初期データ
- 2 社 seed

### 除外範囲

- Cognito 連携
- AWS RDS 接続
- Phase 2 用のユーザー作成画面
- PDF 関連テーブル
- メール関連テーブル
- バッチ関連テーブル

### 完了条件

Flyway でローカル DB を構築でき、2 社 seed で会社境界と権限確認の前提データを用意できている。

### agent task 分割方針

この M フェーズの具体的な agent task 分割は、M2 着手前にコーディングエージェントが提案する。
Notion 側では、この時点で M2-01 / M2-02 のような具体タスク粒度は固定しない。

## M3. 認証境界 / Cognito 接続確認 / local 代替 / 認可基盤

### 目的

Cognito OAuth2 Login の最小接続を先に確認し、Cognito `sub` とアプリ DB の `users.cognito_sub` を突合する認証境界を作る。
そのうえで、local profile でも同じ DB 由来の `LoginUserContext` / `GrantedAuthority` を `Authentication` に保持し、M4 以降の業務機能認可の土台を作る。

### 成果物

- Spring Security OAuth2 Login 最小設定
- Cognito Hosted UI / managed login へのリダイレクト確認
- localhost callback 確認
- Cognito テストユーザーによるログイン確認
- Cognito `sub` 取得確認
- Cognito `sub` と `users.cognito_sub` の突合確認
- CurrentUserProvider
- LoginUserContext
- DbLoginUserContextFactory
- LocalAuthenticationFilter
- CognitoAuthenticationSuccessHandler
- `Authentication.principal` としての `LoginUserContext`
- `permission_sets.code` 由来の `GrantedAuthority`
- `@PreAuthorize("hasAuthority('TENANT_MANAGER')")` によるWebページ単位の認可確認
- TENANT_VIEWER / TENANT_EDITOR / TENANT_MANAGER の local 確認ユーザー
- M4 / M5 のSQLで `company_id` 条件を必須にする会社境界方針

### 除外範囲

- client secret を前提にした設定
- Cognito Trigger
- Pre Token Generation
- PLATFORM / TENANT App Client 分離
- ユーザー管理画面
- 権限割当画面
- 本番用 Cognito 構成
- CDK による Cognito 構築
- AWS dev 環境デプロイ

### 完了条件

- ローカル Spring Boot から Cognito Hosted UI / managed login へ遷移できる
- Cognito 上のテストユーザーでログインできる
- localhost callback でアプリへ戻れる
- Spring Security OAuth2 Login で OIDC / Cognito claim を取得できる
- Cognito `sub` とアプリ DB の `users.cognito_sub` を突合できる
- DB 由来の `LoginUserContext` を `Authentication.principal` として保持できる
- DB 由来の権限セットを `GrantedAuthority` として保持できる
- local profile でもDB由来の `LoginUserContext` / `GrantedAuthority` を保持できる
- `@PreAuthorize("hasAuthority('TENANT_MANAGER')")` で画面アクセス可否を確認できる

### agent task 分割方針

この M フェーズの具体的な agent task 分割は、M3 着手前にコーディングエージェントが提案する。
Notion 側では、この時点で M3-01 / M3-02 のような具体タスク粒度は固定しない。

## M4. 申請管理

### 目的

申請ワークフローを MVP で成立させる。

### 成果物

- 申請一覧
- 申請詳細
- 申請作成
- 申請編集
- 下書き保存
- 提出
- 承認
- 却下
- 差戻し
- 取下げ
- 却下・差戻し理由の記録

### 除外範囲

- PDF 出力
- メール通知
- 外部承認連携
- 複雑な承認ルート設定
- 申請履歴テーブル
- 監査ログテーブル

### 完了条件

DRAFT から SUBMITTED、SUBMITTED から APPROVED / REJECTED / DRAFT / WITHDRAWN の操作が画面から実行でき、申請の現在状態と理由項目が更新される。

### agent task 分割方針

この M フェーズの具体的な agent task 分割は、M4 着手前にコーディングエージェントが提案する。
Notion 側では、この時点で M4-01 / M4-02 のような具体タスク粒度は固定しない。

## M5. 資産カタログ

### 目的

資産管理の基本 CRUD と状態管理を成立させる。

### 成果物

- 資産一覧
- 資産詳細
- 資産登録
- 資産編集
- 資産論理削除
- 資産検索
- 資産絞り込み
- 資産ステータス変更

### 除外範囲

- 資産区分
- 資産種別区分
- 外部資産管理サービス連携
- バーコード管理
- QR コード管理
- 棚卸バッチ
- 資産ステータス履歴テーブル
- 監査ログテーブル

### 完了条件

資産を登録、編集、検索、ステータス変更、論理削除でき、資産の現在状態が更新される。

### agent task 分割方針

この M フェーズの具体的な agent task 分割は、M5 着手前にコーディングエージェントが提案する。
Notion 側では、この時点で M5-01 / M5-02 のような具体タスク粒度は固定しない。

## M6. マスタ管理

### 目的

会社ごとの申請種別・資産分類を管理できるようにする。

### 成果物

- 申請種別マスタ管理
- 資産分類マスタ管理
- 汎用マスタ値の論理削除
- 汎用マスタ値の復活
- 申請画面での申請種別選択肢
- 資産画面での資産分類選択肢

### 除外範囲

- 共通マスタ管理画面
- 権限セット管理画面
- ユーザー管理画面
- 部署管理画面
- 申請種別ごとのシステム処理分岐
- 申請承認による資産ステータス自動変更

### 完了条件

会社ごとの申請種別・資産分類を登録、編集、論理削除、復活でき、業務画面の選択肢として使える。

### agent task 分割方針

この M フェーズの具体的な agent task 分割は、M6 着手前にコーディングエージェントが提案する。
Notion 側では、この時点で M6-01 / M6-02 のような具体タスク粒度は固定しない。

## M7. 監査ログ

### 目的

業務操作をアプリケーションログとして出力し、後から追跡できる前提を作る。

### 成果物

- 申請操作のアプリケーションログ
- 資産操作のアプリケーションログ
- マスタ変更のアプリケーションログ
- ログ出力項目の整理

### 除外範囲

- 監査ログ閲覧画面
- 監査ログ検索画面
- 監査ログ CSV 出力
- 監査ログテーブル
- before_json
- after_json
- IP アドレス記録
- User-Agent 記録

### 完了条件

対象操作時にアプリケーションログへ必要な情報が出力される。

### agent task 分割方針

この M フェーズの具体的な agent task 分割は、M7 着手前にコーディングエージェントが提案する。
Notion 側では、この時点で M7-01 / M7-02 のような具体タスク粒度は固定しない。

## M8. テスト / README / 仕上げ

### 目的

MVP をポートフォリオとして再現・説明できる状態にする。

### 成果物

- Testcontainers MySQL
- Mapper テスト
- Service テスト
- 会社境界テスト
- README
- ローカル起動手順
- MVP 動作確認シナリオ
- 実装ドキュメント整理

### 除外範囲

- Phase 2 の AWS デプロイ手順
- Phase 3 の PDF 手順
- Phase 4 のバッチ手順

### 完了条件

README に従ってローカル起動でき、主要テストが通り、MVP の操作を再現できる。
MVP の詳細な動作確認シナリオは `verification-scenario.md` で確認できる。

### agent task 分割方針

この M フェーズの具体的な agent task 分割は、M8 着手前にコーディングエージェントが提案する。
Notion 側では、この時点で M8-01 / M8-02 のような具体タスク粒度は固定しない。

## M9. 会社・部署管理

### 目的

会社作成、部署管理の DB・画面・業務ルールをローカルで成立させる。
部署は users の所属、会社境界、部署コード一意性、論理削除方針に関わるため、MVP の管理導線として確定する。
会社作成時に初期 TENANT_MANAGER を必ず 1 人作る運用仕様は維持するが、実装順としては M10 のユーザー・権限管理で扱う。

### 成果物

- 会社作成
- 部署一覧
- 部署作成
- 部署編集
- 部署論理削除
- 削除済み部署の表示
- ユーザー作成時に利用する所属部署の任意選択肢

### 除外範囲

- Cognito 本物 API 呼び出し
- Cognito Hosted UI ログインの本格確認
- PLATFORM / TENANT App Client 分離
- ユーザー無効化
- 初期 TENANT_MANAGER 作成
- ユーザー作成
- 権限割当
- 部署階層
- 所属履歴
- 組織改編履歴
- 部署の有効期間管理
- 部署ごとの権限管理
- 部署単位の承認ルート
- 部署の物理削除
- 削除済み部署コードの再利用

### 完了条件

ローカル環境で、PLATFORM_ADMIN が会社を作成できる。
会社ごとの部署を一覧、作成、編集、論理削除でき、ユーザー作成時の所属部署として任意利用できる。
部署削除は物理削除せず、所属ユーザーがいる部署も警告表示したうえで論理削除できる。

### agent task 分割方針

この M フェーズの具体的な agent task 分割は、M9 着手前にコーディングエージェントが提案する。
ユーザーが分割案を確認し、合意した後にだけ `agent-tasks/M9/*.md` を作成する。
Notion 側およびこのファイルでは、この時点で M9-01 / M9-02 のような具体タスク粒度を固定しない。

## M10. ユーザー・権限管理

### 目的

ユーザー作成、権限割当・変更、local profile 用の疑似 `cognito_sub` 発行を含む DB・画面・業務ルールをローカルで成立させる。

### 成果物

- 会社作成時に必ず 1 人作る初期 TENANT_MANAGER 作成
- PLATFORM_ADMIN による PLATFORM ユーザー作成
- PLATFORM_ADMIN による全テナントの TENANT ユーザー作成
- TENANT_MANAGER による所属会社内ユーザー作成
- ユーザー一覧
- ユーザー詳細
- ユーザー作成時の権限セット割当
- ユーザー作成後の権限セット変更
- local profile 用の疑似 `cognito_sub` 発行コンポーネント
- 疑似 `cognito_sub` の発行と `users.cognito_sub` 登録

### 除外範囲

- Cognito 本物 API 呼び出し
- Cognito Hosted UI ログインの本格確認
- PLATFORM / TENANT App Client 分離
- Cognito Trigger
- Pre Token Generation
- 初回ログイン時の `users` 自動作成
- ユーザー無効化
- Cognito 側ユーザー属性の変更
- 権限セット定義そのものの CRUD

### 完了条件

ローカル環境で、PLATFORM_ADMIN が会社作成時の初期 TENANT_MANAGER、PLATFORM ユーザー、全テナントの TENANT ユーザーを作成できる。
TENANT_MANAGER が所属会社内のユーザーを作成できる。
ユーザー作成時および作成後に、許可された範囲で権限セットを割当・変更できる。
対象会社の TENANT_MANAGER 数が 0 になる権限変更は、更新後の count による業務バリデーションで不可にする。
`users.cognito_sub` は local profile 用の疑似値で登録する。

### agent task 分割方針

この M フェーズの具体的な agent task 分割は、M10 着手前にコーディングエージェントが提案する。
ユーザーが分割案を確認し、合意した後にだけ `agent-tasks/M10/*.md` を作成する。
Notion 側およびこのファイルでは、この時点で M10-01 / M10-02 のような具体タスク粒度を固定しない。

## MVP では扱わないもの

- Cognito 本格連携
- Cognito 本物 API によるユーザー作成
- Cognitoユーザー登録時の `users` 自動登録
- Cognito Trigger / Pre Token Generation
- AWS dev 環境
- ECS
- RDS
- CDK
- GitHub Actions deploy
- PLATFORM / TENANT App Client 分離後の管理画面確認
- PDF 出力
- メール通知
- バッチ
- 監査ログ閲覧画面
- SSO
- ポータルサイト
- AI チャットボット
- RAG
