# M5-01 資産一覧 / 詳細 / 参照基盤

## 目的

資産カタログの参照基盤として、会社境界と論理削除を守った資産一覧と資産詳細を作る。

## M5 共通方針

- M5 では資産カタログの基本 CRUD と状態管理を成立させる
- 資産一覧・詳細は `company_id` で会社内資産に絞る
- 論理削除済み資産は `is_deleted = FALSE` 条件で除外する
- 会社境界は authority ではなく Mapper SQL の `company_id` 条件で守る
- 資産カテゴリは M2 seed 済みの会社別 `generic_master_values` / `ASSET_CATEGORY` を参照する
- 資産ステータスは M2 seed 済みの `common_master_values` / `ASSET_STATUS` を参照する
- M5 では資産ステータス履歴テーブル、監査ログテーブルは作らない
- Mapper / Testcontainers 強化は M8 に寄せる
- 後続フェーズの実装運用ルールは `docs/implementation/README.md` を確認してから実装する

## 対応範囲

- 資産一覧画面を作成する
- 資産詳細画面を作成する
- `GET /assets` を追加する
- `GET /assets/{id}` を追加する
- 一覧は現在ユーザーの `companyId` で会社内資産に絞る
- 詳細は `id` と現在ユーザーの `companyId` で取得する
- 一覧・詳細とも `assets.is_deleted = FALSE` を必須条件にする
- Mapper SQL の一覧取得では `assets.company_id = #{companyId}` を必須条件にする
- Mapper SQL の詳細取得では `assets.id = #{id} AND assets.company_id = #{companyId}` を必須条件にする
- TENANT_VIEWER / TENANT_EDITOR / TENANT_MANAGER は所属会社内の資産を閲覧できる
- 一覧と詳細では、資産コード、資産名、資産カテゴリ、管理部署、ステータス、備考、作成日時、更新日時を表示する
- 資産カテゴリ表示名は `generic_master_values` を JOIN して表示する
- 管理部署名は `departments` を JOIN して表示する
- 資産ステータス表示名は `common_master` / `common_master_values` を JOIN して表示する
- トップ画面から資産一覧へ遷移できるようにする
- M5-01 では登録・編集・検索条件・ステータス変更・論理削除ボタンは置かない

## 対応ファイル

- `apps/web/src/main/java/com/example/workops/asset/web/AssetController.java`
- `apps/web/src/main/java/com/example/workops/asset/service/AssetQueryService.java`
- `apps/web/src/main/java/com/example/workops/asset/mapper/AssetMapper.java`
- `apps/web/src/main/java/com/example/workops/asset/model/AssetListItem.java`
- `apps/web/src/main/java/com/example/workops/asset/model/AssetDetail.java`
- `apps/web/src/main/resources/mapper/asset/AssetMapper.xml`
- `apps/web/src/main/resources/templates/asset/list.html`
- `apps/web/src/main/resources/templates/asset/detail.html`
- `apps/web/src/main/resources/templates/index.html`
- `docs/implementation/mvp/agent-tasks/M5-01-asset-list-detail.md`

## 除外範囲

- 資産登録
- 資産編集
- 資産検索・絞り込み
- 資産ステータス変更
- 資産論理削除
- 申請との資産紐づけ
- 資産ステータス履歴テーブル
- 監査ログテーブル
- Mapper / Testcontainers 強化

## 完了条件

local profile でログイン中の DB 由来ユーザーの `companyId` に紐づく未削除資産だけを一覧・詳細で表示できる。
他社の資産 ID または論理削除済み資産 ID を URL に指定しても詳細を取得できない。

## 確認方法

- `cd apps/web && .\mvnw.cmd test`
- local profile で Spring Boot を起動する
- `/assets` で所属会社内の未削除資産一覧を表示できることを確認する
- `/assets/{id}` で所属会社内の未削除資産詳細を表示できることを確認する
- 他社の資産 ID を指定しても詳細が表示されないことを確認する
- 論理削除済み資産が一覧・詳細に表示されないことを確認する

## 実装時の記録

実装後に、次をこのファイルへ追記する。

- 実装方針
- 変更ファイル
- 実装結果
- 確認結果
- 残課題
