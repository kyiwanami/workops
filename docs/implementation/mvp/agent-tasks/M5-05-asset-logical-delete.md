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

### 実装方針

- 資産削除は物理削除ではなく、`assets.is_deleted = TRUE` と `updated_by` の更新で実装した。
- 削除対象は既存の詳細取得と同じ `id + company_id + is_deleted = FALSE` で事前取得し、他社資産・論理削除済み資産は `404` とした。
- 削除実行は Controller と Service の両方で `TENANT_MANAGER` のみに制限した。
- `canDeleteAsset` は認可ガードではなく、詳細画面で削除ボタンを表示するための UI 状態判定として扱う。
- 削除は詳細画面の Bootstrap Modal で「この資産を削除してよろしいですか？」と確認し、確認後にPOSTする。
- 削除理由、削除履歴、復元操作は M5-05 では実装しない。

### 変更ファイル

- `apps/web/src/main/java/com/example/workops/asset/web/AssetController.java`
- `apps/web/src/main/java/com/example/workops/asset/service/AssetCommandService.java`
- `apps/web/src/main/java/com/example/workops/asset/service/AssetQueryService.java`
- `apps/web/src/main/java/com/example/workops/asset/mapper/AssetMapper.java`
- `apps/web/src/main/resources/mapper/asset/AssetMapper.xml`
- `apps/web/src/main/resources/templates/asset/detail.html`
- `apps/web/src/main/resources/templates/asset/list.html`
- `docs/implementation/mvp/agent-tasks/M5-05-asset-logical-delete.md`

### 実装結果

- `POST /assets/{id}/delete` で所属会社内の未削除資産を論理削除できるようにした。
- `AssetCommandService#deleteAsset` を追加し、現在ユーザーの会社ID・ユーザーIDを使って削除対象を限定するようにした。
- `AssetMapper#logicalDeleteAssetByIdAndCompanyId` を追加し、Mapper SQL で `id + company_id + is_deleted = FALSE` を必須条件にした。
- 資産詳細画面に、`TENANT_MANAGER` の場合だけ削除確認モーダルを開く「削除」ボタンを表示するようにした。
- 削除確認モーダル内の「削除する」POSTフォームから論理削除するようにした。
- 削除後は資産一覧へリダイレクトし、Bootstrap Toast で完了通知を表示するようにした。

### 確認結果

- `cd apps/web && .\mvnw.cmd test` 成功。
- local profile の `TENANT_MANAGER` で確認用資産を作成し、詳細画面から論理削除できることを確認した。
- 削除後に資産一覧へ戻り、Toast 表示が出ることを確認した。
- 削除済み資産が一覧に表示されないことを確認した。
- 削除済み資産を資産コード検索しても空結果になることを確認した。
- 削除済み資産の `/assets/{id}`、`/assets/{id}/edit`、`/assets/{id}/status` が `404` になることを確認した。
- local profile の `TENANT_VIEWER` / `TENANT_EDITOR` を別ポートで一時起動し、削除ボタンが表示されず、削除POSTも `403` になることを確認した。
- 他社資産IDの削除POSTが `404` になることを確認した。
- 既存の論理削除済み資産IDの削除POSTが `404` になることを確認した。
- ブラウザスナップショットで、管理者の詳細画面に「削除」ボタンが既存の操作領域へ表示されることを確認した。
- 削除ボタンが直接POSTではなく、削除確認モーダルを開くことを実装後に反映した。

### 残課題

- 申請との資産紐づけは M5-06 で扱う。
- 申請紐づけ候補から論理削除済み資産を除外する確認は、候補取得SQLを実装する M5-06 で行う。
- Service テストと M5 全体確認は M5-07 で扱う。
