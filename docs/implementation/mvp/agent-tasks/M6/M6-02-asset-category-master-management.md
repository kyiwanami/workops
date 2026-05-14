# M6-02 資産分類マスタ管理

## 目的

会社ごとの資産分類として、`ASSET_CATEGORY` の `generic_master_values` を登録・編集・論理削除できるようにする。

## M6 共通方針

- M6 の管理対象は `REQUEST_TYPE` と `ASSET_CATEGORY` の2種だけとする
- このタスクでは `ASSET_CATEGORY` だけを扱う
- `generic_master` 種別自体の追加・編集は MVP では扱わない
- 既存種別に対する会社別 `generic_master_values` の管理だけ行う
- マスタ値の無効化は論理削除だけで表現する
- `generic_master_values.is_deleted = TRUE` は論理削除済みとして扱う
- 物理削除は MVP では実装しない
- 資産分類の通常一覧では、`is_deleted = FALSE` のマスタ値だけを表示する
- 復活操作のため、管理画面には「削除済みも表示」条件を用意する
- 「削除済みも表示」時は、`is_deleted = TRUE` の行も一覧に表示する
- 削除済み行は状態として「削除済み」と分かるように表示する
- 削除済み行に対する通常編集は許可せず、操作は復活だけにする
- 資産分類の論理削除操作は、一覧画面上の確認モーダルを経由して POST する
- 資産分類の復活操作は、一覧画面上の確認モーダルを経由して POST する
- 復活操作は、同じ `generic_master_values` 行の `is_deleted` を `FALSE` に戻す
- 復活操作の権限は `TENANT_MANAGER` のみとし、会社境界は `company_id` 条件で守る
- 復活後の資産分類は、通常一覧と資産登録・編集フォームの選択肢に再表示する
- 参照先の状態表示は「（削除済み）」に統一する
- 新規作成・編集フォームの選択肢には、`is_deleted = TRUE` のマスタ値を表示しない
- `is_deleted = TRUE` のマスタ値が POST された場合は拒否する
- 削除済みマスタ値のコード再利用は禁止し、`generic_master_id + company_id + code` の一意性を維持する
- M6 の操作権限は `TENANT_MANAGER` のみとする
- 会社境界は Mapper SQL の `company_id` 条件で守る
- 申請種別と資産分類は、画面・agent task として分けて進める
- 汎用マスタ共通部品を先に大きく作り込まず、Service / Mapper の共通化は最小限にする
- DB 変更と seed 追加は行わない
- M2 seed 済みの `ASSET_CATEGORY` を前提にする
- Mapper / Testcontainers 強化は M8 に寄せる

## 対応範囲

- 資産分類一覧画面を作成する
- 資産分類登録画面を作成する
- 資産分類編集画面を作成する
- 資産分類論理削除操作を作成する
- 資産分類復活操作を作成する
- 資産分類論理削除操作は確認モーダル経由で実行する
- 資産分類復活操作は確認モーダル経由で実行する
- `GET /masters/asset-categories` を追加する
- `GET /masters/asset-categories/new` を追加する
- `POST /masters/asset-categories` を追加する
- `GET /masters/asset-categories/{id}/edit` を追加する
- `POST /masters/asset-categories/{id}` を追加する
- `POST /masters/asset-categories/{id}/delete` を追加する
- `POST /masters/asset-categories/{id}/restore` を追加する
- Controller の資産分類管理系メソッドには `@PreAuthorize("hasAuthority('TENANT_MANAGER')")` を付ける
- Service の資産分類管理系 public メソッドにも `@PreAuthorize("hasAuthority('TENANT_MANAGER')")` を付ける
- 一覧は現在ユーザーの `companyId` と `ASSET_CATEGORY` の `generic_master_id` で絞る
- 通常一覧には未削除の資産分類だけを表示する
- 「削除済みも表示」条件付きの一覧には、削除済み資産分類も表示する
- 削除済み資産分類は、一覧上の状態を「削除済み」として表示する
- 登録時は現在ユーザーの `companyId` を設定する
- 登録時は `ASSET_CATEGORY` の `generic_master_id` を設定する
- 登録時は `code`、`name`、`sort_order` を入力する
- 編集時は `name`、`sort_order` を更新する
- 編集時に `code` は変更しない
- 論理削除時は `is_deleted = TRUE` と `updated_by` を更新する
- 復活時は同じ行の `is_deleted = FALSE` と `updated_by` を更新する
- 登録・編集・論理削除・復活では `updated_by` に現在ユーザーの `userId` を設定する
- 登録時は `created_by` に現在ユーザーの `userId` を設定する
- コード重複は画面入力エラーとしてフォームに戻す
- 他社の資産分類 ID、`ASSET_CATEGORY` 以外のマスタ値 ID は操作できない
- 論理削除済み資産分類 ID は通常編集・論理削除できず、復活操作だけ可能とする
- トップ画面または既存ナビゲーションから資産分類一覧へ遷移できるようにする

## 対応ファイル

- `apps/web/src/main/java/com/example/workops/master/web/AssetCategoryMasterController.java`
- `apps/web/src/main/java/com/example/workops/master/form/AssetCategoryMasterForm.java`
- `apps/web/src/main/java/com/example/workops/master/service/AssetCategoryMasterService.java`
- `apps/web/src/main/java/com/example/workops/master/mapper/AssetCategoryMasterMapper.java`
- `apps/web/src/main/java/com/example/workops/master/model/AssetCategoryMasterListItem.java`
- `apps/web/src/main/java/com/example/workops/master/model/AssetCategoryMasterDetail.java`
- `apps/web/src/main/resources/mapper/master/AssetCategoryMasterMapper.xml`
- `apps/web/src/main/resources/templates/master/asset-category-list.html`
- `apps/web/src/main/resources/templates/master/asset-category-form.html`
- `apps/web/src/main/resources/templates/index.html`
- `docs/implementation/mvp/agent-tasks/M6/M6-02-asset-category-master-management.md`

## 除外範囲

- 申請種別マスタ管理
- `generic_master` 種別自体の追加・編集
- 共通マスタ管理画面
- 権限セット管理画面
- ユーザー管理画面
- 部署管理画面
- 資産分類ごとのシステム処理分岐
- DB 変更
- seed 追加
- Mapper / Testcontainers 強化
- M6 Service テスト

## 完了条件

TENANT_MANAGER が所属会社の `ASSET_CATEGORY` 値を登録・編集・論理削除・復活できる。
TENANT_VIEWER / TENANT_EDITOR、他社マスタ値、`ASSET_CATEGORY` 以外のマスタ値は操作できない。
論理削除済みマスタ値は通常編集・論理削除できず、復活操作だけ可能。
削除済みマスタ値のコードを再利用できない。

## 確認方法

- `cd apps/web && .\mvnw.cmd test`
- local profile の TENANT_MANAGER で資産分類一覧を表示できることを確認する
- local profile の TENANT_MANAGER で資産分類を登録できることを確認する
- local profile の TENANT_MANAGER で資産分類を編集できることを確認する
- local profile の TENANT_MANAGER で資産分類を論理削除できることを確認する
- 資産分類の論理削除が確認モーダル経由で実行されることを確認する
- local profile の TENANT_MANAGER で「削除済みも表示」条件を使い、削除済み資産分類を一覧表示できることを確認する
- local profile の TENANT_MANAGER で削除済み資産分類を復活できることを確認する
- 資産分類の復活が確認モーダル経由で実行されることを確認する
- TENANT_VIEWER / TENANT_EDITOR が資産分類管理画面へアクセスできないことを確認する
- 他社資産分類 ID を操作できないことを確認する
- `ASSET_CATEGORY` 以外のマスタ値 ID を操作できないことを確認する
- 論理削除済み資産分類 ID を通常編集・論理削除できないことを確認する
- 論理削除済み資産分類 ID は復活操作だけできることを確認する
- 削除済み資産分類コードを再利用できないことを確認する

## 実装時の記録

実装後に、次をこのファイルへ追記する。

- 実装方針
- 変更ファイル
- 実装結果
- 確認結果
- 残課題
