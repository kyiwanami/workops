# M8-03 README / docs MVP 仕上げ

## 目的

README、ローカル起動手順、MVP 動作確認シナリオ、実装ドキュメントを整理し、WorkOps MVP をポートフォリオとして再現・説明できる状態にする。

## M8 共通方針

- M8 は新機能追加ではなく、MVP をポートフォリオとして再現・説明できる状態に仕上げるフェーズとする
- README と実装ドキュメントは、MVP をローカルで再現・説明できる範囲に限定する
- Phase 2 以降は、今後の Phase で扱う旨を短く触れる程度に留める
- UI の最終的な目視確認・操作感確認はユーザーが行う
- コーディングエージェントは README 手順の機械的整合確認までを行う
- `apps/web` を正として進める

## 対応範囲

- README のローカル起動手順を整理する
- Docker Compose MySQL の起動手順を整理する
- Flyway と seed の適用前提を整理する
- `mvnw test` の実行手順を整理する
- Spring Boot の local profile 起動手順を整理する
- local 疑似ユーザーの使い方を整理する
- MVP で確認できる主要業務操作を整理する
- 申請、資産、マスタ、業務操作ログの概要を整理する
- MVP 動作確認シナリオを整理する
- 実装ドキュメントの M8 完了時点の整合を確認する
- M8 の確認結果を agent task Markdown に記録する

## README / docs に含めるもの

- ローカル起動手順
- Docker Compose MySQL
- Flyway
- seed
- `mvnw test`
- local 疑似ユーザー
- MVP で確認できる主要業務操作
- 申請、資産、マスタ、業務操作ログの概要

## README / docs に含めないもの

- Phase 2 の AWS デプロイ手順
- Cognito 本格連携手順
- ECS / RDS / CDK / GitHub Actions deploy 手順
- PDF 出力手順
- メール・バッチ手順
- CloudWatch Logs 連携手順

## 対応ファイル

- `README.md`
- `docs/implementation/README.md`
- `docs/implementation/mvp/phases.md`
- `docs/implementation/mvp/**/*.md`
- `docs/implementation/db/schema.md`
- `docs/implementation/mvp/agent-tasks/M8/M8-03-readme-docs-mvp-finish.md`

## 除外範囲

- 新機能追加
- DB 変更
- Flyway DDL 変更
- seed 仕様変更
- Phase 2 の AWS デプロイ手順
- Cognito 本格連携手順
- ECS / RDS / CDK / GitHub Actions deploy 手順
- PDF 出力手順
- メール・バッチ手順
- CloudWatch Logs 連携手順
- UI の最終品質判断
- 見た目の主観的調整
- ユーザー承認なしの画面仕様変更

## 完了条件

README に従って WorkOps MVP をローカルで再現できる手順が整理されている。
申請、資産、マスタ、業務操作ログについて、MVP で確認できる主要操作と確認観点が説明できる。
実装ドキュメントが M8 完了時点の MVP 範囲と矛盾しない状態になっている。

## 確認方法

- `cd apps/web && .\mvnw.cmd test`
- README のコマンドが現在のリポジトリ構成と一致していることを確認する
- Docker Compose MySQL、Flyway、seed、local profile、local 疑似ユーザーの説明が現行実装と一致していることを確認する
- MVP 動作確認シナリオが申請、資産、マスタ、業務操作ログを含んでいることを確認する
- Phase 2 以降の詳細手順が README / docs に混入していないことを確認する

## 実装時の記録

### 実装方針

- M8-03 はドキュメント整理のみとし、コード、DB、Flyway migration、seed、画面仕様は変更しない。
- ルート README は MVP のローカル再現入口として、起動、テスト、local 疑似ユーザー、主要URL、主要業務操作を整理する。
- 詳細な動作確認シナリオは `docs/implementation/mvp/verification-scenario.md` に分離する。
- Phase 2 以降の AWS / Cognito 本格連携 / ECS / RDS / CDK / deploy / PDF / メール / バッチ / CloudWatch Logs 詳細手順は含めない。

### 変更ファイル

- `README.md`
- `docs/implementation/README.md`
- `docs/implementation/mvp/phases.md`
- `docs/implementation/mvp/verification-scenario.md`
- `docs/implementation/mvp/agent-tasks/M8/M8-03-readme-docs-mvp-finish.md`

### 実装結果

- `README.md` を M8 時点の入口文書として更新した。
  - Docker Compose MySQL、Flyway V1 から V4、seed、DB再構築、Spring Boot local profile 起動、HTTP確認、停止手順を現行構成に合わせた。
  - `.\mvnw.cmd test` が Docker Desktop 起動前提で、Service テストと Testcontainers MySQL 統合テストを実行することを明記した。
  - `WORKOPS_LOCAL_COGNITO_SUB` による local 疑似ユーザー6件の切替方法を記載した。
  - 申請、資産、マスタ、業務操作ログの MVP 確認概要と主要URLを整理した。
  - Cognito 最小接続は M3 の確認資産として残し、通常の MVP 再現は `local` profile を使う説明に寄せた。
- `docs/implementation/mvp/verification-scenario.md` を追加した。
  - 申請、資産、申請種別マスタ、資産分類マスタ、業務操作ログの確認シナリオを整理した。
  - UI の最終目視判断はユーザー実施であることを明記した。
- `docs/implementation/README.md` に M8 完了時点の MVP 再現ドキュメント導線を追加した。
- `docs/implementation/mvp/phases.md` の M8 完了条件に `verification-scenario.md` の導線を追加した。

### 確認結果

- 実行コマンド: `cd apps/web && .\mvnw.cmd test`
- 結果: 成功
- テスト結果: `Tests run: 97, Failures: 0, Errors: 0, Skipped: 0`
- README に記載した主要コマンドと現行 `compose.yaml`、`application.yml`、Flyway migration、seed、Controller URL、local 認証実装の整合を確認した。
- Phase 2 以降の詳細手順を README / docs に追加していないことを確認した。

### 残課題

- UI の最終目視確認・操作感確認はユーザー実施とする。
