# M5-07 Service テスト / M5確認

## 目的

M5 の資産カタログについて、Service の作成・編集・検索条件・ステータス変更・論理削除・申請紐づけをテストで確認する。

## M5 共通方針

- M5 では資産カタログの基本 CRUD と状態管理を成立させる
- M5 では Service テストまで入れる
- Mapper / Testcontainers 強化は M8 に寄せる
- 操作可否は会社境界、権限、論理削除状態で制御する
- 会社境界は authority ではなく Mapper SQL の `company_id` 条件で守る
- Form / DTO / Mapper 入力では手書き getter / setter を作らない
- 後続フェーズの実装運用ルールは `docs/implementation/README.md` を確認してから実装する

## 対応範囲

- `AssetCommandService` の Service テストを作成する
- `AssetQueryService` の検索条件組み立てを Service テストで確認する
- 資産作成の正常系を確認する
- 資産編集の正常系を確認する
- 資産ステータス変更の正常系を確認する
- 資産論理削除の正常系を確認する
- TENANT_VIEWER が更新系操作を実行できないことを確認する
- TENANT_EDITOR が資産作成・編集・ステータス変更できることを確認する
- TENANT_EDITOR が資産論理削除できないことを確認する
- TENANT_MANAGER が資産作成・編集・ステータス変更・論理削除できることを確認する
- 他社資産は Service から更新されないことを確認する
- 論理削除済み資産は編集・ステータス変更・論理削除できないことを確認する
- 不正な資産カテゴリ・部署・ステータスが拒否されることを確認する
- 申請作成・編集で同一会社の未削除資産を紐づけできることを確認する
- 申請作成・編集で他社資産・論理削除済み資産を紐づけできないことを確認する
- M5-01 から M5-06 の agent task Markdown に実装結果と確認結果が記録されていることを確認する

## 対応ファイル

- `apps/web/src/test/java/com/example/workops/asset/service/AssetCommandServiceTests.java`
- `apps/web/src/test/java/com/example/workops/asset/service/AssetQueryServiceTests.java`
- `apps/web/src/test/java/com/example/workops/request/service/RequestAssetLinkServiceTests.java`
- `apps/web/src/main/java/com/example/workops/asset/service/AssetCommandService.java`
- `apps/web/src/main/java/com/example/workops/asset/service/AssetQueryService.java`
- `apps/web/src/main/java/com/example/workops/request/service/RequestCommandService.java`
- `docs/implementation/mvp/agent-tasks/M5-07-asset-service-tests.md`
- `docs/implementation/mvp/agent-tasks/M5-01-asset-list-detail.md`
- `docs/implementation/mvp/agent-tasks/M5-02-asset-create-edit.md`
- `docs/implementation/mvp/agent-tasks/M5-03-asset-search-filter.md`
- `docs/implementation/mvp/agent-tasks/M5-04-asset-status-change.md`
- `docs/implementation/mvp/agent-tasks/M5-05-asset-logical-delete.md`
- `docs/implementation/mvp/agent-tasks/M5-06-request-asset-link.md`

## 除外範囲

- Mapper / Testcontainers 強化
- Flyway DDL 変更
- seed 変更
- README 更新
- `docs/implementation/mvp/phases.md` の modified 表示対応
- 資産ステータス履歴テーブル
- 監査ログテーブル
- 申請承認による資産ステータス自動変更

## 完了条件

M5 の資産作成・編集・検索条件・ステータス変更・論理削除・申請紐づけの操作可否が Service テストで確認されている。
M5-01 から M5-06 の実装結果と確認結果が各 agent task Markdown に記録されている。
Mapper / Testcontainers 強化、DB変更、README変更、`phases.md` の modified 表示対応は M5 作業に含まれていない。

## 確認方法

- `cd apps/web && .\mvnw.cmd test`
- Service テストで資産作成・編集・ステータス変更・論理削除の正常系を確認する
- Service テストで権限不足、会社不一致、論理削除済み、不正選択肢の拒否を確認する
- Service テストで申請と資産の紐づけ正常系・拒否系を確認する
- M5-01 から M5-06 の agent task Markdown に実装結果と確認結果が記録されていることを確認する

## 実装時の記録

実装後に、次をこのファイルへ追記する。

- 実装方針
- 変更ファイル
- 実装結果
- 確認結果
- 残課題
