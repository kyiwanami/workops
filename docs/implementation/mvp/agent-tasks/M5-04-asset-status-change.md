# M5-04 資産ステータス変更

## 目的

TENANT_EDITOR / TENANT_MANAGER が所属会社の未削除資産の現在ステータスを変更できるようにする。

## M5 共通方針

- 資産ステータス変更は TENANT_EDITOR / TENANT_MANAGER のみ許可する
- TENANT_VIEWER は資産ステータス変更できない
- 変更対象は現在ユーザーの `companyId` 内に限定する
- 論理削除済み資産はステータス変更できない
- 会社境界は authority ではなく Mapper SQL の `company_id` 条件で守る
- ステータスは `common_master_values` / `ASSET_STATUS` の seed 済み値だけ許可する
- M5 では厳密な状態遷移表を作らず、選択された `ASSET_STATUS` へ更新できる
- 申請承認による資産ステータス自動変更は行わない
- 資産ステータス履歴テーブルは作らない
- 完了通知は Bootstrap Toast で表示する

## 対応範囲

- 資産詳細画面からステータス変更画面へ遷移できるようにする
- `GET /assets/{id}/status` を追加する
- `POST /assets/{id}/status` を追加する
- Controller のステータス変更系メソッドには `@PreAuthorize("hasAnyAuthority('TENANT_EDITOR','TENANT_MANAGER')")` を付ける
- Service のステータス変更系 public メソッドにも `@PreAuthorize("hasAnyAuthority('TENANT_EDITOR','TENANT_MANAGER')")` を付ける
- 入力項目は `statusCode` のみとする
- `statusCode` は必須とし、`ASSET_STATUS` の seed 済み値だけ許可する
- 変更対象は `id + company_id + is_deleted = FALSE` で取得する
- 更新時は `status_code` と `updated_by` を更新する
- 更新後は資産詳細へリダイレクトし、Toast で完了通知を表示する

## 対応ファイル

- `apps/web/src/main/java/com/example/workops/asset/web/AssetController.java`
- `apps/web/src/main/java/com/example/workops/asset/form/AssetStatusForm.java`
- `apps/web/src/main/java/com/example/workops/asset/service/AssetCommandService.java`
- `apps/web/src/main/java/com/example/workops/asset/service/AssetQueryService.java`
- `apps/web/src/main/java/com/example/workops/asset/mapper/AssetMapper.java`
- `apps/web/src/main/resources/mapper/asset/AssetMapper.xml`
- `apps/web/src/main/resources/templates/asset/detail.html`
- `apps/web/src/main/resources/templates/asset/status-form.html`
- `docs/implementation/mvp/agent-tasks/M5-04-asset-status-change.md`

## 除外範囲

- 資産ステータス履歴テーブル
- 厳密なステータス遷移表
- 申請承認による資産ステータス自動変更
- 資産登録・編集
- 資産論理削除
- 申請との資産紐づけ
- Mapper / Testcontainers 強化

## 完了条件

TENANT_EDITOR / TENANT_MANAGER が所属会社の未削除資産のステータスを `ASSET_STATUS` の範囲で変更できる。
TENANT_VIEWER、他社資産、論理削除済み資産、不正ステータスは変更できない。

## 確認方法

- `cd apps/web && .\mvnw.cmd test`
- local profile の TENANT_EDITOR で資産ステータスを変更できることを確認する
- local profile の TENANT_MANAGER で資産ステータスを変更できることを確認する
- TENANT_VIEWER が資産ステータス変更できないことを確認する
- 他社資産を変更できないことを確認する
- 論理削除済み資産を変更できないことを確認する
- 不正ステータスを指定できないことを確認する
- ステータス変更後に資産詳細で現在ステータスを確認できることを確認する

## 実装時の記録

### 実装方針

- 資産編集フォームとは分けて、詳細画面起点のステータス変更専用フォームを追加した。
- ステータス変更の業務条件は `AssetCommandService` に集約し、Controller と Service の両方に `@PreAuthorize` を付けた。
- 変更対象は既存の詳細取得と同じ `id + company_id + is_deleted = FALSE` で取得し、他社資産・論理削除済み資産は `404` とした。
- ステータスは保存値のため、`ASSET_STATUS` に存在する値だけ許可する。検索条件と違い、不正ステータスは空結果ではなく保存前に `400` とする。
- 厳密な状態遷移表、履歴テーブル、申請承認による資産ステータス自動変更は実装しない。

### 変更ファイル

- `apps/web/src/main/java/com/example/workops/asset/form/AssetStatusForm.java`
- `apps/web/src/main/java/com/example/workops/asset/web/AssetController.java`
- `apps/web/src/main/java/com/example/workops/asset/service/AssetCommandService.java`
- `apps/web/src/main/java/com/example/workops/asset/service/AssetQueryService.java`
- `apps/web/src/main/java/com/example/workops/asset/mapper/AssetMapper.java`
- `apps/web/src/main/resources/mapper/asset/AssetMapper.xml`
- `apps/web/src/main/resources/templates/asset/detail.html`
- `apps/web/src/main/resources/templates/asset/status-form.html`
- `docs/implementation/mvp/agent-tasks/M5-04-asset-status-change.md`

### 実装結果

- `GET /assets/{id}/status` でステータス変更フォームを表示できるようにした。
- `POST /assets/{id}/status` で未削除資産の `status_code` と `updated_by` を更新できるようにした。
- `TENANT_EDITOR` / `TENANT_MANAGER` だけがステータス変更できるようにした。
- 資産詳細画面に、権限がある場合だけ「ステータス変更」リンクを表示するようにした。
- ステータス変更後は資産詳細へリダイレクトし、Bootstrap Toast で完了通知を表示するようにした。

### 確認結果

- `cd apps/web && .\mvnw.cmd test` 成功。
- `cd apps/web && .\mvnw.cmd -DskipTests package` 成功。
- local profile の `TENANT_MANAGER` で資産ステータスを変更できることを確認した。
- local profile の `TENANT_EDITOR` を別ポートで一時起動し、資産ステータスを変更できることを確認した。
- ステータス変更後に資産詳細へ戻り、現在ステータスと Toast が表示されることを確認した。
- local profile の `TENANT_VIEWER` を別ポートで一時起動し、`GET /assets/1/status` と `POST /assets/1/status` が `403` になることを確認した。
- 他社資産の `/assets/10/status` が `404` になることを確認した。
- 論理削除済み資産の `/assets/9/status` が `404` になることを確認した。
- 空の `statusCode` はフォームエラーとして同じフォームを再表示することを確認した。
- 不正な `statusCode` は `400` になることを確認した。
- 申請系のコード・Mapper・テンプレートは変更していない。
- HTTP確認で変更したローカルDBの資産ステータスは、確認後に seed 相当の状態へ戻した。

### 残課題

- 資産論理削除は M5-05 で扱う。
- 申請との資産紐づけは M5-06 で扱う。
- Service テストと M5 全体確認は M5-07 で扱う。
