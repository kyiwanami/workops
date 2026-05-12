# M5-03 資産検索 / 絞り込み

## 目的

資産一覧で、会社内の未削除資産をキーワード・カテゴリ・部署・ステータスで検索できるようにする。

## M5 共通方針

- 検索対象は現在ユーザーの `companyId` 内に限定する
- 検索対象は `is_deleted = FALSE` の資産に限定する
- 会社境界は authority ではなく Mapper SQL の `company_id` 条件で守る
- M5 ではページングを入れない
- Mapper / Testcontainers 強化は M8 に寄せる
- 検索フォームは record とし、手書き getter / setter は作らない

## 対応範囲

- 資産一覧画面に検索条件を追加する
- `GET /assets` で検索条件をクエリパラメータとして受け取る
- 検索条件は `keyword`、`assetCategoryValueId`、`departmentId`、`statusCode` とする
- `keyword` は資産コードと資産名を部分一致検索する
- `assetCategoryValueId` は現在ユーザーの会社に属する `ASSET_CATEGORY` 値だけ受け付ける
- `departmentId` は現在ユーザーの会社に属する部署だけ受け付ける
- `statusCode` は `ASSET_STATUS` の seed 済み値だけ受け付ける
- 未指定条件は絞り込みに使わない
- 検索結果は `assets.company_id = #{companyId}` と `assets.is_deleted = FALSE` を必須条件にする
- 検索条件に使う選択肢は一覧画面へ表示する
- 検索実行後も入力済み条件を画面に保持する

## 対応ファイル

- `apps/web/src/main/java/com/example/workops/asset/web/AssetController.java`
- `apps/web/src/main/java/com/example/workops/asset/form/AssetSearchForm.java`
- `apps/web/src/main/java/com/example/workops/asset/service/AssetQueryService.java`
- `apps/web/src/main/java/com/example/workops/asset/mapper/AssetMapper.java`
- `apps/web/src/main/resources/mapper/asset/AssetMapper.xml`
- `apps/web/src/main/resources/templates/asset/list.html`
- `docs/implementation/mvp/agent-tasks/M5-03-asset-search-filter.md`

## 除外範囲

- ページング
- CSV 出力
- 資産登録・編集
- 資産ステータス変更
- 資産論理削除
- 申請との資産紐づけ
- Mapper / Testcontainers 強化

## 完了条件

資産一覧で、キーワード・資産カテゴリ・部署・ステータスを指定して、所属会社内の未削除資産だけを検索できる。

## 確認方法

- `cd apps/web && .\mvnw.cmd test`
- キーワードで資産コード・資産名を検索できることを確認する
- 資産カテゴリで絞り込めることを確認する
- 部署で絞り込めることを確認する
- ステータスで絞り込めることを確認する
- 複数条件を組み合わせて絞り込めることを確認する
- 他社資産と論理削除済み資産が検索結果に出ないことを確認する

## 実装時の記録

### 実装方針

- 資産コードと資産名を別々のクエリパラメータ `assetCode` / `assetName` として扱う。
- `AssetSearchForm` は record で作成し、手書き getter / setter は作らない。
- 検索条件はすべて Mapper SQL の WHERE 条件として扱い、未指定条件は絞り込みに使わない。
- 検索条件に対するマスタ妥当性の事前チェックは行わない。現在会社に存在しないカテゴリ・部署・ステータスが指定された場合も、`company_id` と検索 WHERE 条件により空結果にする。
- 会社境界と論理削除除外は `assets.company_id = #{companyId}` と `assets.is_deleted = FALSE` で守る。

### 変更ファイル

- `apps/web/src/main/java/com/example/workops/asset/form/AssetSearchForm.java`
- `apps/web/src/main/java/com/example/workops/asset/web/AssetController.java`
- `apps/web/src/main/java/com/example/workops/asset/service/AssetQueryService.java`
- `apps/web/src/main/java/com/example/workops/asset/mapper/AssetMapper.java`
- `apps/web/src/main/resources/mapper/asset/AssetMapper.xml`
- `apps/web/src/main/resources/templates/asset/list.html`
- `docs/implementation/mvp/agent-tasks/M5-03-asset-search-filter.md`

### 実装結果

- `GET /assets` で `assetCode` / `assetName` / `assetCategoryValueId` / `departmentId` / `statusCode` を受け取れるようにした。
- 資産一覧を、資産コード・資産名・資産カテゴリ・管理部署・ステータスでAND検索できるようにした。
- 検索フォームに資産カテゴリ、管理部署、ステータスの選択肢を表示するようにした。
- 検索後も入力済み条件を保持するようにした。
- クリアリンクで検索条件なしの `/assets` に戻れるようにした。

### 確認結果

- `cd apps/web && .\mvnw.cmd test` 成功。
- `cd apps/web && .\mvnw.cmd -DskipTests package` 成功。
- local profile の一時起動ポートで `/assets?assetCode=KTHM-NB` により資産コード部分一致検索できることを確認した。
- `/assets?assetName=ノートPC` により資産名部分一致検索できることを確認した。
- `/assets?assetCategoryValueId=1` により資産カテゴリで絞り込めることを確認した。
- `/assets?departmentId=2` により管理部署で絞り込めることを確認した。
- `/assets?statusCode=AVAILABLE` によりステータスで絞り込めることを確認した。
- `/assets?assetCode=KTHM&assetName=ノートPC&assetCategoryValueId=1&departmentId=4&statusCode=AVAILABLE` により複数条件をAND検索できることを確認した。
- 検索後もフォームに入力済み条件が残ることを確認した。
- 他社資産と論理削除済み資産が検索結果に出ないことを確認した。
- 他社カテゴリ、他社部署、不正ステータスをクエリパラメータ指定した場合、`400` ではなく `200` の空結果になることを確認した。

### 残課題

- 資産ステータス変更専用操作は M5-04 で扱う。
- 資産論理削除は M5-05 で扱う。
- 申請との資産紐づけは M5-06 で扱う。
- Service テストと M5 全体確認は M5-07 で扱う。
