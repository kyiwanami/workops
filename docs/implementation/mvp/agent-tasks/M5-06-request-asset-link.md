# M5-06 申請との資産紐づけ

## 目的

M5で資産カタログが成立した後、M4の申請作成・編集・参照に `asset_id` の資産紐づけを接続する。

## M5 共通方針

- M4では `asset_id` は任意・未選択中心だったため、M5-06で申請フォームへ資産選択を追加する
- 申請に紐づけられる資産は、現在ユーザーの `companyId` 内かつ `is_deleted = FALSE` の資産だけとする
- `asset_id` は任意であり、未選択を許可する
- 他社資産 ID や論理削除済み資産 ID を POST された場合は拒否する
- 会社境界は authority ではなく Mapper SQL の `company_id` 条件で守る
- 申請承認による資産ステータス自動変更は行わない
- 資産ステータス変更はM5-04の手動操作として扱う
- 完了通知は Bootstrap Toast で表示する

## 対応範囲

- 申請作成フォームに資産選択を追加する
- 申請編集フォームに資産選択を追加する
- 申請詳細で資産IDだけでなく資産コード・資産名を表示する
- 申請一覧で資産コード・資産名を表示する
- `RequestForm` に `assetId` を追加する
- 申請作成時に、未選択なら `asset_id = NULL` として登録する
- 申請編集時に、未選択なら `asset_id = NULL` として更新する
- 選択肢は現在ユーザーの会社に属する未削除資産だけにする
- 申請作成・編集Serviceで、指定された `assetId` が現在ユーザーの会社に属する未削除資産か検証する
- 申請一覧・詳細のMapper SQLで `requests.asset_id = assets.id` かつ `requests.company_id = assets.company_id` かつ `assets.is_deleted = FALSE` を満たす資産だけ JOIN する
- 資産未選択や紐づき資産が論理削除済みの場合でも申請自体は表示し、資産表示は未選択扱いにする
- M5-06では申請の提出・承認・却下・差戻し・取下げロジックは変更しない

## 対応ファイル

- `apps/web/src/main/java/com/example/workops/request/form/RequestForm.java`
- `apps/web/src/main/java/com/example/workops/request/service/RequestCommandService.java`
- `apps/web/src/main/java/com/example/workops/request/service/RequestQueryService.java`
- `apps/web/src/main/java/com/example/workops/request/mapper/RequestMapper.java`
- `apps/web/src/main/java/com/example/workops/request/model/RequestListItem.java`
- `apps/web/src/main/java/com/example/workops/request/model/RequestDetail.java`
- `apps/web/src/main/java/com/example/workops/request/model/RequestAssetOption.java`
- `apps/web/src/main/resources/mapper/request/RequestMapper.xml`
- `apps/web/src/main/resources/templates/request/form.html`
- `apps/web/src/main/resources/templates/request/list.html`
- `apps/web/src/main/resources/templates/request/detail.html`
- `docs/implementation/mvp/agent-tasks/M5-06-request-asset-link.md`

## 除外範囲

- 申請承認による資産ステータス自動変更
- 資産ステータス履歴テーブル
- 申請ワークフロー状態遷移の変更
- 資産CRUDの変更
- 申請種別マスタ管理
- Mapper / Testcontainers 強化

## 完了条件

申請作成・編集で、同一会社かつ未削除の資産を任意で紐づけできる。
他社資産や論理削除済み資産は申請に紐づけできない。
申請一覧・詳細で紐づけ資産のコード・名前を確認できる。
申請承認による資産ステータス自動変更は行われない。

## 確認方法

- `cd apps/web && .\mvnw.cmd test`
- 申請作成時に同一会社の未削除資産を選択できることを確認する
- 申請作成時に資産未選択でも保存できることを確認する
- 申請編集時に資産を変更・未選択化できることを確認する
- 他社資産 ID を POST しても紐づけできないことを確認する
- 論理削除済み資産 ID を POST しても紐づけできないことを確認する
- 申請一覧・詳細で資産コード・資産名を表示できることを確認する
- 承認操作で資産ステータスが自動変更されないことを確認する

## 実装時の記録

実装後に、次をこのファイルへ追記する。

- 実装方針
- 変更ファイル
- 実装結果
- 確認結果
- 残課題
