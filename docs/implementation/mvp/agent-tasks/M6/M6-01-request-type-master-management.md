# M6-01 申請種別マスタ管理

## 目的

会社ごとの申請種別として、`REQUEST_TYPE` の `generic_master_values` を登録・編集・論理削除できるようにする。

## M6 共通方針

- M6 の管理対象は `REQUEST_TYPE` と `ASSET_CATEGORY` の2種だけとする
- このタスクでは `REQUEST_TYPE` だけを扱う
- `generic_master` 種別自体の追加・編集は MVP では扱わない
- 既存種別に対する会社別 `generic_master_values` の管理だけ行う
- マスタ値の無効化は論理削除だけで表現する
- `generic_master_values.is_deleted = TRUE` は論理削除済みとして扱う
- 物理削除は MVP では実装しない
- 申請種別の通常一覧では、`is_deleted = FALSE` のマスタ値だけを表示する
- 復活操作のため、管理画面には「削除済みも表示」条件を用意する
- 「削除済みも表示」時は、`is_deleted = TRUE` の行も一覧に表示する
- 削除済み行は状態として「削除済み」と分かるように表示する
- 削除済み行に対する通常編集は許可せず、操作は復活だけにする
- 復活操作は、同じ `generic_master_values` 行の `is_deleted` を `FALSE` に戻す
- 復活操作の権限は `TENANT_MANAGER` のみとし、会社境界は `company_id` 条件で守る
- 復活後の申請種別は、通常一覧と申請作成・編集フォームの選択肢に再表示する
- 参照先の状態表示は「（削除済み）」に統一する
- 新規作成・編集フォームの選択肢には、`is_deleted = TRUE` のマスタ値を表示しない
- `is_deleted = TRUE` のマスタ値が POST された場合は拒否する
- 削除済みマスタ値のコード再利用は禁止し、`generic_master_id + company_id + code` の一意性を維持する
- M6 の操作権限は `TENANT_MANAGER` のみとする
- 会社境界は Mapper SQL の `company_id` 条件で守る
- 汎用マスタ共通部品を先に大きく作り込まず、Service / Mapper の共通化は最小限にする
- DB 変更と seed 追加は行わない
- M2 seed 済みの `REQUEST_TYPE` を前提にする
- Mapper / Testcontainers 強化は M8 に寄せる

## 対応範囲

- 申請種別一覧画面を作成する
- 申請種別登録画面を作成する
- 申請種別編集画面を作成する
- 申請種別論理削除操作を作成する
- 申請種別復活操作を作成する
- `GET /masters/request-types` を追加する
- `GET /masters/request-types/new` を追加する
- `POST /masters/request-types` を追加する
- `GET /masters/request-types/{id}/edit` を追加する
- `POST /masters/request-types/{id}` を追加する
- `POST /masters/request-types/{id}/delete` を追加する
- `POST /masters/request-types/{id}/restore` を追加する
- Controller の申請種別管理系メソッドには `@PreAuthorize("hasAuthority('TENANT_MANAGER')")` を付ける
- Service の申請種別管理系 public メソッドにも `@PreAuthorize("hasAuthority('TENANT_MANAGER')")` を付ける
- 一覧は現在ユーザーの `companyId` と `REQUEST_TYPE` の `generic_master_id` で絞る
- 通常一覧には未削除の申請種別だけを表示する
- 「削除済みも表示」条件付きの一覧には、削除済み申請種別も表示する
- 削除済み申請種別は、一覧上の状態を「削除済み」として表示する
- 登録時は現在ユーザーの `companyId` を設定する
- 登録時は `REQUEST_TYPE` の `generic_master_id` を設定する
- 登録時は `code`、`name`、`sort_order` を入力する
- 編集時は `name`、`sort_order` を更新する
- 編集時に `code` は変更しない
- 論理削除時は `is_deleted = TRUE` と `updated_by` を更新する
- 復活時は同じ行の `is_deleted = FALSE` と `updated_by` を更新する
- 登録・編集・論理削除・復活では `updated_by` に現在ユーザーの `userId` を設定する
- 登録時は `created_by` に現在ユーザーの `userId` を設定する
- コード重複は画面入力エラーとしてフォームに戻す
- 他社の申請種別 ID、`REQUEST_TYPE` 以外のマスタ値 ID は操作できない
- 論理削除済み申請種別 ID は通常編集・論理削除できず、復活操作だけ可能とする
- トップ画面または既存ナビゲーションから申請種別一覧へ遷移できるようにする

## 対応ファイル

- `apps/web/src/main/java/com/example/workops/master/web/RequestTypeMasterController.java`
- `apps/web/src/main/java/com/example/workops/master/form/RequestTypeMasterForm.java`
- `apps/web/src/main/java/com/example/workops/master/service/RequestTypeMasterService.java`
- `apps/web/src/main/java/com/example/workops/master/mapper/RequestTypeMasterMapper.java`
- `apps/web/src/main/java/com/example/workops/master/model/RequestTypeMasterListItem.java`
- `apps/web/src/main/java/com/example/workops/master/model/RequestTypeMasterDetail.java`
- `apps/web/src/main/resources/mapper/master/RequestTypeMasterMapper.xml`
- `apps/web/src/main/resources/templates/master/request-type-list.html`
- `apps/web/src/main/resources/templates/master/request-type-form.html`
- `apps/web/src/main/resources/templates/index.html`
- `docs/implementation/mvp/agent-tasks/M6/M6-01-request-type-master-management.md`

## 除外範囲

- 資産分類マスタ管理
- `generic_master` 種別自体の追加・編集
- 共通マスタ管理画面
- 権限セット管理画面
- ユーザー管理画面
- 部署管理画面
- 申請種別ごとのシステム処理分岐
- DB 変更
- seed 追加
- Mapper / Testcontainers 強化
- M6 Service テスト

## 完了条件

TENANT_MANAGER が所属会社の `REQUEST_TYPE` 値を登録・編集・論理削除・復活できる。
TENANT_VIEWER / TENANT_EDITOR、他社マスタ値、`REQUEST_TYPE` 以外のマスタ値は操作できない。
論理削除済みマスタ値は通常編集・論理削除できず、復活操作だけ可能。
削除済みマスタ値のコードを再利用できない。

## 確認方法

- `cd apps/web && .\mvnw.cmd test`
- local profile の TENANT_MANAGER で申請種別一覧を表示できることを確認する
- local profile の TENANT_MANAGER で申請種別を登録できることを確認する
- local profile の TENANT_MANAGER で申請種別を編集できることを確認する
- local profile の TENANT_MANAGER で申請種別を論理削除できることを確認する
- local profile の TENANT_MANAGER で「削除済みも表示」条件を使い、削除済み申請種別を一覧表示できることを確認する
- local profile の TENANT_MANAGER で削除済み申請種別を復活できることを確認する
- TENANT_VIEWER / TENANT_EDITOR が申請種別管理画面へアクセスできないことを確認する
- 他社申請種別 ID を操作できないことを確認する
- `REQUEST_TYPE` 以外のマスタ値 ID を操作できないことを確認する
- 論理削除済み申請種別 ID を通常編集・論理削除できないことを確認する
- 論理削除済み申請種別 ID は復活操作だけできることを確認する
- 削除済み申請種別コードを再利用できないことを確認する

## 実装時の記録

実装後に、次をこのファイルへ追記する。

- 実装方針
- 変更ファイル
- 実装結果
- 確認結果
- 残課題

### 実装方針

- `REQUEST_TYPE` 専用の Controller / Service / Mapper / Form / View を追加し、汎用マスタ共通部品の大きな抽象化は行わない
- 一覧の「削除済みも表示」は `RequestTypeMasterSearchForm.showDeleted` の画面条件として扱う
- 通常一覧では未削除のみを表示し、`showDeleted` が true の場合だけ削除済み行も表示する
- 削除操作は既存資産詳細画面と同じく確認モーダルを経由して POST する
- 復活操作も確認モーダルを経由して POST する
- 削除済み行は通常編集・論理削除の対象外とし、復活操作だけを表示する
- 登録時のコード重複判定は削除済み行も含めて行う

### 変更ファイル

- `apps/web/src/main/java/com/example/workops/master/web/RequestTypeMasterController.java`
- `apps/web/src/main/java/com/example/workops/master/form/RequestTypeMasterForm.java`
- `apps/web/src/main/java/com/example/workops/master/form/RequestTypeMasterSearchForm.java`
- `apps/web/src/main/java/com/example/workops/master/service/RequestTypeMasterService.java`
- `apps/web/src/main/java/com/example/workops/master/mapper/RequestTypeMasterMapper.java`
- `apps/web/src/main/java/com/example/workops/master/model/RequestTypeMasterListItem.java`
- `apps/web/src/main/java/com/example/workops/master/model/RequestTypeMasterDetail.java`
- `apps/web/src/main/resources/mapper/master/RequestTypeMasterMapper.xml`
- `apps/web/src/main/resources/templates/master/request-type-list.html`
- `apps/web/src/main/resources/templates/master/request-type-form.html`
- `apps/web/src/main/resources/templates/index.html`

### 実装結果

- TENANT_MANAGER 専用の申請種別一覧、登録、編集、論理削除、復活ルートを追加した
- Mapper SQL は `company_id` と `REQUEST_TYPE` で会社境界と対象種別を絞るようにした
- 削除済み行は一覧で「削除済み」と表示し、復活操作のみを表示するようにした

### 確認結果

- `cd apps/web && .\mvnw.cmd test` 成功
- 既存 Service テスト 48 件が成功
- local profile で Spring Boot を起動し、`/`、`/masters/request-types`、`/masters/request-types?showDeleted=true`、`/masters/request-types/new` が HTTP 200 を返すことを確認

### 残課題

- M6-04 で Service テストを追加する
- local profile の TENANT_MANAGER によるブラウザ画面操作確認を行う
