# M5-02 資産登録 / 編集

## 目的

TENANT_EDITOR / TENANT_MANAGER が所属会社の資産を登録・編集できるようにする。

## M5 共通方針

- 資産作成・編集は TENANT_EDITOR / TENANT_MANAGER のみ許可する
- TENANT_VIEWER は資産作成・編集できない
- 作成・編集対象は現在ユーザーの `companyId` 内に限定する
- 会社境界は authority ではなく Mapper SQL の `company_id` 条件で守る
- 論理削除済み資産は編集できない
- 資産カテゴリは現在ユーザーの会社に属する `generic_master_values` / `ASSET_CATEGORY` から選択する
- 資産ステータスは `common_master_values` / `ASSET_STATUS` から選択する
- Form / DTO / Mapper 入力では手書き getter / setter を作らず、record を優先する
- 完了通知は Bootstrap Toast で表示する
- Mapper / Testcontainers 強化は M8 に寄せる

## 対応範囲

- 資産登録画面を作成する
- 資産編集画面を作成する
- `GET /assets/new` を追加する
- `POST /assets` を追加する
- `GET /assets/{id}/edit` を追加する
- `POST /assets/{id}` を追加する
- Controller の作成・編集系メソッドには `@PreAuthorize("hasAnyAuthority('TENANT_EDITOR','TENANT_MANAGER')")` を付ける
- Service の作成・編集系 public メソッドにも `@PreAuthorize("hasAnyAuthority('TENANT_EDITOR','TENANT_MANAGER')")` を付ける
- 入力項目は `code`、`name`、`assetCategoryValueId`、`departmentId`、`statusCode`、`note` とする
- `code` は必須、50文字以内、会社内で一意とする
- `name` は必須、100文字以内とする
- `assetCategoryValueId` は必須とし、現在ユーザーの会社に属する `ASSET_CATEGORY` 値だけ許可する
- `departmentId` は任意とし、指定された場合は現在ユーザーの会社に属する部署だけ許可する
- `statusCode` は必須とし、`ASSET_STATUS` の seed 済み値だけ許可する
- `note` は任意、1000文字以内とする
- 新規作成時は `company_id` に現在ユーザーの `companyId` を設定する
- 新規作成時は `created_by` と `updated_by` に現在ユーザーの `userId` を設定する
- 編集時は `updated_by` に現在ユーザーの `userId` を設定する
- 編集対象は `id + company_id + is_deleted = FALSE` で取得する
- 編集保存は `id + company_id + is_deleted = FALSE` を条件に更新する
- 作成・編集後は資産詳細へリダイレクトし、Toast で完了通知を表示する

## 対応ファイル

- `apps/web/src/main/java/com/example/workops/asset/web/AssetController.java`
- `apps/web/src/main/java/com/example/workops/asset/form/AssetForm.java`
- `apps/web/src/main/java/com/example/workops/asset/service/AssetCommandService.java`
- `apps/web/src/main/java/com/example/workops/asset/service/AssetQueryService.java`
- `apps/web/src/main/java/com/example/workops/asset/mapper/AssetMapper.java`
- `apps/web/src/main/java/com/example/workops/asset/model/AssetCategoryOption.java`
- `apps/web/src/main/java/com/example/workops/asset/model/AssetDepartmentOption.java`
- `apps/web/src/main/java/com/example/workops/asset/model/AssetStatusOption.java`
- `apps/web/src/main/resources/mapper/asset/AssetMapper.xml`
- `apps/web/src/main/resources/templates/asset/form.html`
- `apps/web/src/main/resources/templates/asset/detail.html`
- `docs/implementation/mvp/agent-tasks/M5/M5-02-asset-create-edit.md`

## 除外範囲

- 資産検索・絞り込み
- 資産ステータス変更専用操作
- 資産論理削除
- 申請との資産紐づけ
- 資産ステータス履歴テーブル
- 監査ログテーブル
- Mapper / Testcontainers 強化

## 完了条件

TENANT_EDITOR / TENANT_MANAGER が所属会社の資産を登録・編集できる。
TENANT_VIEWER、他社資産、論理削除済み資産、不正な資産カテゴリ・部署・ステータスは登録・編集できない。

## 確認方法

- `cd apps/web && .\mvnw.cmd test`
- local profile の TENANT_EDITOR で資産を登録できることを確認する
- local profile の TENANT_MANAGER で資産を登録できることを確認する
- 所属会社の未削除資産を編集できることを確認する
- TENANT_VIEWER が資産登録・編集できないことを確認する
- 他社資産を編集できないことを確認する
- 論理削除済み資産を編集できないことを確認する
- 他社カテゴリ、他社部署、不正ステータスを指定できないことを確認する

## 実装時の記録

### 実装方針

- M4 のフォーム実装方針に合わせ、`AssetForm` と選択肢DTOは record で作成し、手書き getter / setter は作らない。
- 資産作成・編集の業務条件は `AssetCommandService` に集約し、Controller と Service の両方に `@PreAuthorize` を付けた。
- 会社境界と論理削除除外は Mapper SQL の `company_id` / `is_deleted = FALSE` 条件で守る。
- 資産コード重複は通常の画面入力エラーとしてフォームへ戻し、field error で表示する。
- 不正な資産カテゴリ・管理部署・資産ステータスは `ResponseStatusException(HttpStatus.BAD_REQUEST, ...)` とした。

### 変更ファイル

- `apps/web/src/main/java/com/example/workops/asset/web/AssetController.java`
- `apps/web/src/main/java/com/example/workops/asset/form/AssetForm.java`
- `apps/web/src/main/java/com/example/workops/asset/service/AssetCommandService.java`
- `apps/web/src/main/java/com/example/workops/asset/service/AssetQueryService.java`
- `apps/web/src/main/java/com/example/workops/asset/mapper/AssetMapper.java`
- `apps/web/src/main/java/com/example/workops/asset/model/AssetCategoryOption.java`
- `apps/web/src/main/java/com/example/workops/asset/model/AssetDepartmentOption.java`
- `apps/web/src/main/java/com/example/workops/asset/model/AssetStatusOption.java`
- `apps/web/src/main/resources/mapper/asset/AssetMapper.xml`
- `apps/web/src/main/resources/templates/asset/form.html`
- `apps/web/src/main/resources/templates/asset/list.html`
- `apps/web/src/main/resources/templates/asset/detail.html`
- `docs/implementation/mvp/agent-tasks/M5/M5-02-asset-create-edit.md`

### 実装結果

- `TENANT_EDITOR` / `TENANT_MANAGER` が資産を新規登録できるようにした。
- 所属会社内の未削除資産を編集できるようにした。
- 一覧画面に、作成権限がある場合だけ「新規作成」リンクを表示するようにした。
- 詳細画面に、編集権限がある場合だけ「編集」リンクを表示するようにした。
- 作成・更新後は資産詳細へリダイレクトし、Bootstrap Toast で完了通知を表示するようにした。

### 確認結果

- `cd apps/web && .\mvnw.cmd test` 成功。
- `cd apps/web && .\mvnw.cmd -DskipTests package` 成功。
- local profile の `TENANT_MANAGER` で資産を新規登録できることを確認した。
- local profile の `TENANT_MANAGER` で所属会社の未削除資産を編集できることを確認した。
- local profile の `TENANT_EDITOR` を別ポートで一時起動し、資産を新規登録できることを確認した。
- local profile の `TENANT_VIEWER` を別ポートで一時起動し、`/assets/new` と `/assets/1/edit` が `403` になることを確認した。
- 他社資産の `/assets/10/edit` が `404` になることを確認した。
- 論理削除済み資産の `/assets/9/edit` が `404` になることを確認した。
- 他社カテゴリ、他社部署、不正ステータスをPOSTした場合に `400` になることを確認した。
- 会社内で重複する資産コードをPOSTした場合に、フォームへ戻り field error が表示されることを確認した。

### 残課題

- 資産検索・絞り込みは M5-03 で扱う。
- 資産ステータス変更専用操作は M5-04 で扱う。
- 資産論理削除は M5-05 で扱う。
- 申請との資産紐づけは M5-06 で扱う。
- Service テストと M5 全体確認は M5-07 で扱う。
