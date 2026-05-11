# M5-05 資産論理削除

## 目的

TENANT_MANAGER が所属会社の未削除資産を論理削除できるようにする。

## M5 共通方針

- 資産削除は物理削除ではなく `is_deleted = TRUE` とする
- 資産論理削除は TENANT_MANAGER のみ許可する
- TENANT_VIEWER / TENANT_EDITOR は資産論理削除できない
- 削除対象は現在ユーザーの `companyId` 内に限定する
- 会社境界は authority ではなく Mapper SQL の `company_id` 条件で守る
- 論理削除済み資産は一覧・検索・詳細・編集・ステータス変更・申請紐づけ候補に表示しない
- 削除済みコードの再利用は MVP では許可しない
- 完了通知は Bootstrap Toast で表示する

## 対応範囲

- 資産詳細画面に削除操作を追加する
- `POST /assets/{id}/delete` を追加する
- Controller の削除メソッドには `@PreAuthorize("hasAuthority('TENANT_MANAGER')")` を付ける
- Service の削除 public メソッドにも `@PreAuthorize("hasAuthority('TENANT_MANAGER')")` を付ける
- 削除対象は `id + company_id + is_deleted = FALSE` で取得する
- 論理削除時は `is_deleted = TRUE` と `updated_by` を更新する
- 削除後は資産一覧へリダイレクトし、Toast で完了通知を表示する

## 対応ファイル

- `apps/web/src/main/java/com/example/workops/asset/web/AssetController.java`
- `apps/web/src/main/java/com/example/workops/asset/service/AssetCommandService.java`
- `apps/web/src/main/java/com/example/workops/asset/mapper/AssetMapper.java`
- `apps/web/src/main/resources/mapper/asset/AssetMapper.xml`
- `apps/web/src/main/resources/templates/asset/detail.html`
- `docs/implementation/mvp/agent-tasks/M5-05-asset-logical-delete.md`

## 除外範囲

- 物理削除
- 削除済みコードの再利用
- 削除理由入力
- 資産ステータス履歴テーブル
- 監査ログテーブル
- 申請との資産紐づけ
- Mapper / Testcontainers 強化

## 完了条件

TENANT_MANAGER が所属会社の未削除資産を論理削除できる。
論理削除済み資産は一覧・検索・詳細・編集・ステータス変更・申請紐づけ候補に表示されない。
TENANT_VIEWER / TENANT_EDITOR、他社資産、論理削除済み資産は削除できない。

## 確認方法

- `cd apps/web && .\mvnw.cmd test`
- local profile の TENANT_MANAGER で資産を論理削除できることを確認する
- TENANT_VIEWER / TENANT_EDITOR が資産を論理削除できないことを確認する
- 他社資産を論理削除できないことを確認する
- 論理削除済み資産が一覧・検索・詳細に表示されないことを確認する
- 論理削除済み資産が編集・ステータス変更・申請紐づけ候補に出ないことを確認する

## 実装時の記録

実装後に、次をこのファイルへ追記する。

- 実装方針
- 変更ファイル
- 実装結果
- 確認結果
- 残課題
