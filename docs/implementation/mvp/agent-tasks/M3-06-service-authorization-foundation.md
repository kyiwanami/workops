# M3-06 Service authorization foundation

## 目的

Service 層で会社境界と権限セットに基づく簡易認可を行う土台を作る。

## 対応範囲

- Service 層から `CurrentUserProvider` を参照できる構成にする
- 会社境界チェックの共通方針を作る
- `TENANT_VIEWER` / `TENANT_EDITOR` / `TENANT_MANAGER` の権限判定方針を作る
- 他社データ参照・更新を拒否する基盤を作る
- 権限不足時の例外方針を作る
- 操作ログをアプリケーションログとして出す方針を作る

## 対応ファイル

- `apps/server/src/main/java/com/example/workops/common/security/`
- `apps/server/src/main/java/com/example/workops/common/exception/`
- `docs/implementation/mvp/agent-tasks/M3-06-service-authorization-foundation.md`

## 除外範囲

- 申請管理の個別認可実装
- 資産管理の個別認可実装
- マスタ管理の個別認可実装
- 監査ログテーブル
- 履歴テーブル
- PLATFORM 管理者の本格認可

## 完了条件

Service 層で使う会社境界・権限判定の土台が作られ、M4以降の業務Serviceから利用できる。

## 確認方法

- `TENANT_VIEWER` / `TENANT_EDITOR` / `TENANT_MANAGER` の判定ができることを確認する
- 利用者の `company_id` と対象データの `company_id` を比較できることを確認する
- 権限不足・会社不一致時に利用を拒否できることを確認する

## 実装時の記録

実装後に、次をこのファイルへ追記する。

- 実装方針
- 変更ファイル
- 確認結果
- 残課題
