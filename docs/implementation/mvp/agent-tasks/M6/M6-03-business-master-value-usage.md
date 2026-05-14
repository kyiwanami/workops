# M6-03 業務画面へのマスタ値反映

## 目的

申請画面と資産画面で、論理削除済みマスタ値を新規選択肢から除外し、既存データの参照表示は「（削除済み）」付きで維持する。

## M6 共通方針

- M6 の管理対象は `REQUEST_TYPE` と `ASSET_CATEGORY` の2種だけとする
- このタスクでは、申請画面の `REQUEST_TYPE` 利用と資産画面の `ASSET_CATEGORY` 利用を扱う
- `generic_master` 種別自体の追加・編集は MVP では扱わない
- `generic_master_values.is_deleted = TRUE` は論理削除済みとして扱う
- 復活後のマスタ値は、申請作成・編集フォームと資産登録・編集フォームの選択肢に再表示する
- 既存申請・既存資産が論理削除済みマスタ値を参照している場合も、親データは表示し続ける
- 論理削除済みマスタ値を表示する場合は、参照先名称の末尾に「（削除済み）」を付ける
- 既存申請・既存資産の「名称（削除済み）」表示は復活導線ではなく、復活はマスタ管理画面から行う
- 参照先の状態表示は「（削除済み）」に統一する
- 新規作成・編集フォームの選択肢には、`is_deleted = TRUE` のマスタ値を表示しない
- `is_deleted = TRUE` のマスタ値が POST された場合は拒否する
- M6 の操作権限は `TENANT_MANAGER` のみだが、申請・資産の既存作成編集権限は M4 / M5 の方針を維持する
- 会社境界は Mapper SQL の `company_id` 条件で守る
- DB 変更と seed 追加は行わない
- Mapper / Testcontainers 強化は M8 に寄せる

## 対応範囲

- 申請一覧で、論理削除済み申請種別を「名称（削除済み）」として表示する
- 申請詳細で、論理削除済み申請種別を「名称（削除済み）」として表示する
- 申請作成フォームで、論理削除済み申請種別を選択肢に表示しない
- 申請編集フォームで、論理削除済み申請種別を選択肢に表示しない
- 復活済み申請種別を申請作成・編集フォームの選択肢に再表示する
- 申請作成時に論理削除済み `REQUEST_TYPE` 値が POST された場合は拒否する
- 申請編集時に論理削除済み `REQUEST_TYPE` 値が POST された場合は拒否する
- 資産一覧で、論理削除済み資産分類を「名称（削除済み）」として表示する
- 資産詳細で、論理削除済み資産分類を「名称（削除済み）」として表示する
- 資産作成フォームで、論理削除済み資産分類を選択肢に表示しない
- 資産編集フォームで、論理削除済み資産分類を選択肢に表示しない
- 復活済み資産分類を資産登録・編集フォームの選択肢に再表示する
- 資産検索フォームで、論理削除済み資産分類を選択肢に表示しない
- 資産作成時に論理削除済み `ASSET_CATEGORY` 値が POST された場合は拒否する
- 資産編集時に論理削除済み `ASSET_CATEGORY` 値が POST された場合は拒否する
- 既存申請・既存資産が論理削除済みマスタ値を参照している場合、一覧・詳細から親データを消さない
- 他社マスタ値 ID が POST された場合は既存方針どおり拒否する
- 申請種別ごとのシステム処理分岐は追加しない
- 申請承認による資産ステータス自動変更は追加しない

## 対応ファイル

- `apps/web/src/main/java/com/example/workops/request/service/RequestQueryService.java`
- `apps/web/src/main/java/com/example/workops/request/service/RequestCommandService.java`
- `apps/web/src/main/java/com/example/workops/request/mapper/RequestMapper.java`
- `apps/web/src/main/java/com/example/workops/request/model/RequestListItem.java`
- `apps/web/src/main/java/com/example/workops/request/model/RequestDetail.java`
- `apps/web/src/main/resources/mapper/request/RequestMapper.xml`
- `apps/web/src/main/resources/templates/request/list.html`
- `apps/web/src/main/resources/templates/request/detail.html`
- `apps/web/src/main/resources/templates/request/form.html`
- `apps/web/src/main/java/com/example/workops/asset/service/AssetQueryService.java`
- `apps/web/src/main/java/com/example/workops/asset/service/AssetCommandService.java`
- `apps/web/src/main/java/com/example/workops/asset/mapper/AssetMapper.java`
- `apps/web/src/main/java/com/example/workops/asset/model/AssetListItem.java`
- `apps/web/src/main/java/com/example/workops/asset/model/AssetDetail.java`
- `apps/web/src/main/resources/mapper/asset/AssetMapper.xml`
- `apps/web/src/main/resources/templates/asset/list.html`
- `apps/web/src/main/resources/templates/asset/detail.html`
- `apps/web/src/main/resources/templates/asset/form.html`
- `docs/implementation/mvp/agent-tasks/M6/M6-03-business-master-value-usage.md`

## 除外範囲

- 申請種別マスタ管理画面
- 資産分類マスタ管理画面
- `generic_master` 種別自体の追加・編集
- DB 変更
- seed 追加
- 申請ワークフロー状態遷移の変更
- 資産ステータス変更ロジックの変更
- 申請承認による資産ステータス自動変更
- Mapper / Testcontainers 強化
- M6 Service テスト

## 完了条件

申請・資産の新規作成・編集フォームで、論理削除済み `REQUEST_TYPE` / `ASSET_CATEGORY` が選択肢に出ない。
論理削除済み `REQUEST_TYPE` / `ASSET_CATEGORY` が POST された場合は保存できない。
既存申請・既存資産が論理削除済みマスタ値を参照していても親データは一覧・詳細に表示され、参照先名称に「（削除済み）」が付く。

## 確認方法

- `cd apps/web && .\mvnw.cmd test`
- 論理削除済み申請種別が申請作成・編集フォームの選択肢に出ないことを確認する
- 復活済み申請種別が申請作成・編集フォームの選択肢に出ることを確認する
- 論理削除済み申請種別 ID を申請作成・編集で POST すると拒否されることを確認する
- 論理削除済み申請種別を参照する申請が一覧・詳細に表示されることを確認する
- 論理削除済み申請種別を参照する申請で、申請種別名に「（削除済み）」が付くことを確認する
- 論理削除済み資産分類が資産作成・編集・検索フォームの選択肢に出ないことを確認する
- 復活済み資産分類が資産作成・編集フォームの選択肢に出ることを確認する
- 論理削除済み資産分類 ID を資産作成・編集で POST すると拒否されることを確認する
- 論理削除済み資産分類を参照する資産が一覧・詳細に表示されることを確認する
- 論理削除済み資産分類を参照する資産で、資産分類名に「（削除済み）」が付くことを確認する

## 実装時の記録

実装後に、次をこのファイルへ追記する。

- 実装方針
- 変更ファイル
- 実装結果
- 確認結果
- 残課題

### 実装方針

- 業務画面の一覧・詳細取得では、参照先 `generic_master_values` の論理削除済み行も取得対象にする
- 新規作成・編集・検索フォームの選択肢取得では、既存どおり `is_deleted = FALSE` だけを取得対象にする
- POST 拒否は既存の selectable 判定を維持し、削除済み値を不正なマスタ値として扱う
- 画面表示用モデルには参照先マスタ値の `is_deleted` を表す `requestTypeValueIsDeleted` / `assetCategoryValueIsDeleted` を追加する
- 削除済み参照は一覧・詳細画面だけで `名称（削除済み）` と表示する

### 変更ファイル

- `apps/web/src/main/java/com/example/workops/request/model/RequestListItem.java`
- `apps/web/src/main/java/com/example/workops/request/model/RequestDetail.java`
- `apps/web/src/main/resources/mapper/request/RequestMapper.xml`
- `apps/web/src/main/resources/templates/request/list.html`
- `apps/web/src/main/resources/templates/request/detail.html`
- `apps/web/src/main/java/com/example/workops/asset/model/AssetListItem.java`
- `apps/web/src/main/java/com/example/workops/asset/model/AssetDetail.java`
- `apps/web/src/main/resources/mapper/asset/AssetMapper.xml`
- `apps/web/src/main/resources/templates/asset/list.html`
- `apps/web/src/main/resources/templates/asset/detail.html`
- `apps/web/src/test/java/com/example/workops/request/service/RequestQueryServiceTests.java`
- `apps/web/src/test/java/com/example/workops/request/service/RequestCommandServiceTests.java`
- `apps/web/src/test/java/com/example/workops/request/service/RequestAssetLinkServiceTests.java`
- `apps/web/src/test/java/com/example/workops/asset/service/AssetQueryServiceTests.java`
- `apps/web/src/test/java/com/example/workops/asset/service/AssetCommandServiceTests.java`
- `docs/implementation/mvp/agent-tasks/M6/M6-03-business-master-value-usage.md`

### 実装結果

- 申請一覧・詳細は、削除済み `REQUEST_TYPE` を参照していても親申請を表示し続けるようにした
- 申請一覧・詳細では、削除済み `REQUEST_TYPE` 名の末尾に「（削除済み）」を付けるようにした
- 申請作成・編集フォームの選択肢取得と POST 拒否条件は `is_deleted = FALSE` のまま維持した
- 資産一覧・詳細は、削除済み `ASSET_CATEGORY` を参照していても親資産を表示し続けるようにした
- 資産一覧・詳細では、削除済み `ASSET_CATEGORY` 名の末尾に「（削除済み）」を付けるようにした
- 資産登録・編集・検索フォームの選択肢取得と POST 拒否条件は `is_deleted = FALSE` のまま維持した

### 確認結果

- `cd apps/web && .\mvnw.cmd test` 成功
- 既存 Service テスト 48 件が成功
- local profile 起動中の `/requests`、`/requests/new`、`/assets`、`/assets/new` が HTTP 200 を返すことを確認

### 残課題

- M6-04 で Service テストを追加する
- 削除済みマスタ値を参照する実データでのブラウザ画面確認を行う
