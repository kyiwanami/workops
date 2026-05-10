# MVP Implementation Phases

## 前提

MVP のスコープは、Notion のスコープ管理で `Phase = MVP` かつ `状態 = 確定` の項目を正とします。
このファイルは、MVP スコープをコーディングエージェントへ渡せる実装フェーズへ整理するものです。
このファイルでは、MVP スコープ自体を変更しません。

具体的な agent task 分割は、各 M フェーズ着手前にコーディングエージェントが提案します。
ユーザーが分割案を確認し、合意した後にだけ `agent-tasks/*.md` を作成します。
このファイルでは、M1-01 / M1-02 のような具体タスク粒度を固定しません。

## M1. プロジェクト土台

### 目的

Maven / Spring Boot / Thymeleaf / MyBatis / Docker Compose MySQL の最小構成を作る。

### 成果物

- Maven ベースの Spring Boot アプリ
- Spring Initializr 相当で生成した `apps/server`
- Maven Wrapper
- `.gitignore`
- ローカル起動できる最小構成
- Docker Compose MySQL の前提
- Thymeleaf 画面表示の前提
- MyBatis 利用の前提
- Phase 2 用の `infra/cdk` 置き場
- `.github/workflows` 置き場

### 除外範囲

- 業務テーブル
- Flyway 初期 DDL 本体
- seed
- 申請管理
- 資産管理
- 認証・認可
- Spring Security
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
- request_histories
- assets
- asset_status_histories
- audit_logs
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

## M3. local 疑似認証 / 簡易認可

### 目的

Cognito なしで会社境界と権限セットを実装に組み込む。

### 成果物

- CurrentUserProvider
- LoginUserContext
- LocalCurrentUserProvider
- TENANT_VIEWER / TENANT_EDITOR / TENANT_MANAGER の疑似ユーザー
- company_id 境界条件
- Service 層認可チェック

### 除外範囲

- Cognito Hosted UI
- Spring Security OAuth2 Login
- Cognito Trigger
- Pre Token Generation
- ユーザー管理画面
- 権限割当画面

### 完了条件

疑似ユーザーを切り替え、会社境界と権限セットに応じて操作可否が変わる。

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
- 申請履歴

### 除外範囲

- PDF 出力
- メール通知
- 外部承認連携
- 複雑な承認ルート設定

### 完了条件

DRAFT から SUBMITTED、SUBMITTED から APPROVED / REJECTED / DRAFT / WITHDRAWN の操作が画面から実行でき、履歴が残る。

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
- 資産ステータス履歴

### 除外範囲

- 資産区分
- 資産種別区分
- 外部資産管理サービス連携
- バーコード管理
- QR コード管理
- 棚卸バッチ

### 完了条件

資産を登録、編集、検索、ステータス変更、論理削除でき、履歴が残る。

### agent task 分割方針

この M フェーズの具体的な agent task 分割は、M5 着手前にコーディングエージェントが提案する。
Notion 側では、この時点で M5-01 / M5-02 のような具体タスク粒度は固定しない。

## M6. マスタ管理

### 目的

会社ごとの申請種別・資産分類を管理できるようにする。

### 成果物

- 申請種別マスタ管理
- 資産分類マスタ管理
- 汎用マスタ値の停止扱い
- 申請画面での申請種別選択肢
- 資産画面での資産分類選択肢

### 除外範囲

- 共通マスタ管理画面
- 権限セット管理画面
- ユーザー管理画面
- 部署管理画面

### 完了条件

会社ごとの申請種別・資産分類を登録、編集、停止でき、業務画面の選択肢として使える。

### agent task 分割方針

この M フェーズの具体的な agent task 分割は、M6 着手前にコーディングエージェントが提案する。
Notion 側では、この時点で M6-01 / M6-02 のような具体タスク粒度は固定しない。

## M7. 監査ログ

### 目的

業務操作を後から追跡できるようにする。

### 成果物

- 申請操作の監査ログ
- 資産操作の監査ログ
- マスタ変更の監査ログ
- Service 層での同一トランザクション記録

### 除外範囲

- 監査ログ閲覧画面
- 監査ログ検索画面
- 監査ログ CSV 出力
- before_json
- after_json
- IP アドレス記録
- User-Agent 記録

### 完了条件

対象操作時に audit_logs へ記録され、管理コンソールで確認できる。

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

### agent task 分割方針

この M フェーズの具体的な agent task 分割は、M8 着手前にコーディングエージェントが提案する。
Notion 側では、この時点で M8-01 / M8-02 のような具体タスク粒度は固定しない。

## MVP では扱わないもの

- Cognito ログイン
- AWS dev 環境
- ECS
- RDS
- CDK
- GitHub Actions deploy
- ユーザー管理
- 権限割当
- PLATFORM 管理画面
- PDF 出力
- メール通知
- バッチ
- 監査ログ閲覧画面
- SSO
- ポータルサイト
- AI チャットボット
- RAG
