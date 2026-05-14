# M6-04 Service テスト / M6 確認

## 目的

M6 の申請種別マスタ管理、資産分類マスタ管理、業務画面へのマスタ値反映について、Service の操作可否と業務条件をテストで確認する。

## M6 共通方針

- M6 の管理対象は `REQUEST_TYPE` と `ASSET_CATEGORY` の2種だけとする
- `generic_master` 種別自体の追加・編集は MVP では扱わない
- 既存種別に対する会社別 `generic_master_values` の管理だけ行う
- マスタ値の無効化は論理削除だけで表現する
- `generic_master_values.is_deleted = TRUE` は論理削除済みとして扱う
- 物理削除は MVP では実装しない
- 通常一覧では、`is_deleted = FALSE` のマスタ値だけを表示する
- 「削除済みも表示」時は、`is_deleted = TRUE` の行も一覧に表示する
- 削除済み行に対する通常編集は許可せず、操作は復活だけにする
- 申請種別 / 資産分類の論理削除操作は、一覧画面上の確認モーダルを経由して POST する
- 申請種別 / 資産分類の復活操作は、一覧画面上の確認モーダルを経由して POST する
- 復活操作は、同じ `generic_master_values` 行の `is_deleted` を `FALSE` に戻す
- 復活操作の権限は `TENANT_MANAGER` のみとし、会社境界は `company_id` 条件で守る
- 復活後のマスタ値は、通常一覧、申請作成・編集フォーム、資産登録・編集フォームの選択肢に再表示する
- 参照先の状態表示は「（削除済み）」に統一する
- 新規作成・編集フォームの選択肢には、`is_deleted = TRUE` のマスタ値を表示しない
- `is_deleted = TRUE` のマスタ値が POST された場合は拒否する
- 削除済みマスタ値のコード再利用は禁止し、`generic_master_id + company_id + code` の一意性を維持する
- M6 の操作権限は `TENANT_MANAGER` のみとする
- 会社境界は Mapper SQL の `company_id` 条件で守る
- M6 のテストは Service テスト中心で進める
- Mapper / Testcontainers 強化は M8 に寄せる
- DB 変更と seed 追加は行わない

## 対応範囲

- `RequestTypeMasterService` の Service テストを作成する
- `AssetCategoryMasterService` の Service テストを作成する
- 申請種別登録の正常系を確認する
- 申請種別編集の正常系を確認する
- 申請種別論理削除の正常系を確認する
- 申請種別復活の正常系を確認する
- 資産分類登録の正常系を確認する
- 資産分類編集の正常系を確認する
- 資産分類論理削除の正常系を確認する
- 資産分類復活の正常系を確認する
- TENANT_VIEWER / TENANT_EDITOR が M6 の更新系操作を実行できないことを確認する
- TENANT_MANAGER が M6 の更新系操作を実行できることを確認する
- 他社マスタ値は Service から更新されないことを確認する
- `REQUEST_TYPE` 以外のマスタ値を申請種別Serviceで更新できないことを確認する
- `ASSET_CATEGORY` 以外のマスタ値を資産分類Serviceで更新できないことを確認する
- 論理削除済みマスタ値は通常編集・論理削除できず、復活できることを確認する
- 削除済みマスタ値のコードを再利用できないことを確認する
- 登録時のコード重複を確認する
- 通常一覧・選択肢取得で論理削除済みマスタ値を除外することを確認する
- 「削除済みも表示」条件付きの一覧で論理削除済みマスタ値を表示することを確認する
- 復活後のマスタ値が通常一覧・選択肢取得で表示対象に戻ることを確認する
- 既存データ表示用の取得では論理削除済みマスタ値を表示対象として扱えることを確認する
- `RequestCommandService` の申請種別検証で論理削除済み `REQUEST_TYPE` が拒否されることを確認する
- `AssetCommandService` の資産分類検証で論理削除済み `ASSET_CATEGORY` が拒否されることを確認する
- M6-01 から M6-03 の agent task Markdown に実装結果と確認結果が記録されていることを確認する

## 対応ファイル

- `apps/web/src/test/java/com/example/workops/master/service/RequestTypeMasterServiceTests.java`
- `apps/web/src/test/java/com/example/workops/master/service/AssetCategoryMasterServiceTests.java`
- `apps/web/src/test/java/com/example/workops/request/service/RequestMasterValueUsageServiceTests.java`
- `apps/web/src/test/java/com/example/workops/asset/service/AssetMasterValueUsageServiceTests.java`
- `docs/implementation/mvp/agent-tasks/M6/M6-04-master-service-tests.md`
- `docs/implementation/mvp/agent-tasks/M6/M6-01-request-type-master-management.md`
- `docs/implementation/mvp/agent-tasks/M6/M6-02-asset-category-master-management.md`
- `docs/implementation/mvp/agent-tasks/M6/M6-03-business-master-value-usage.md`

## 除外範囲

- Mapper / Testcontainers 強化
- Flyway DDL 変更
- seed 追加
- README 更新
- `docs/implementation/mvp/phases.md` の方針変更
- M6 以外の業務仕様変更
- 申請種別ごとのシステム処理分岐
- 申請承認による資産ステータス自動変更

## 完了条件

M6 の申請種別マスタ管理、資産分類マスタ管理、業務画面へのマスタ値反映の操作可否が Service テストで確認されている。
会社境界、削除済み除外、削除済み表示、復活、コード一意性、他社データ拒否、論理削除済みマスタ値の POST 拒否が確認されている。
M6-01 から M6-03 の実装結果と確認結果が各 agent task Markdown に記録されている。

## 確認方法

- `cd apps/web && .\mvnw.cmd test`
- Service テストで申請種別登録・編集・論理削除の正常系を確認する
- Service テストで申請種別復活の正常系を確認する
- Service テストで資産分類登録・編集・論理削除の正常系を確認する
- Service テストで資産分類復活の正常系を確認する
- Service テストで TENANT_VIEWER / TENANT_EDITOR の M6 更新系操作拒否を確認する
- Service テストで他社マスタ値、対象外マスタ種別、論理削除済みマスタ値の通常編集・論理削除拒否を確認する
- Service テストで論理削除済みマスタ値の復活を確認する
- Service テストで削除済みマスタ値のコード再利用禁止を確認する
- Service テストで申請・資産フォーム向け選択肢が論理削除済みマスタ値を除外することを確認する
- Service テストで申請・資産保存時に論理削除済みマスタ値が拒否されることを確認する
- M6-01 から M6-03 の agent task Markdown に実装結果と確認結果が記録されていることを確認する

## 実装時の記録

実装後に、次をこのファイルへ追記する。

- 実装方針
- 変更ファイル
- 実装結果
- 確認結果
- 残課題

### 実装方針

- 既存 Service テストと同じ `@SpringJUnitConfig`、mock Mapper、`SecurityContextHolder` の構成で追加する
- Mapper SQL 自体の検証は M8 に寄せ、M6-04 では Service が正しい Mapper メソッドと権限条件を使うことを確認する
- 申請種別マスタ管理と資産分類マスタ管理は、それぞれ専用 Service テストとして分ける
- 業務画面の削除済みマスタ値 POST 拒否は、既存の `RequestCommandServiceTests` / `AssetCommandServiceTests` に追加する

### 変更ファイル

- `apps/web/src/test/java/com/example/workops/master/service/RequestTypeMasterServiceTests.java`
- `apps/web/src/test/java/com/example/workops/master/service/AssetCategoryMasterServiceTests.java`
- `apps/web/src/test/java/com/example/workops/request/service/RequestCommandServiceTests.java`
- `apps/web/src/test/java/com/example/workops/asset/service/AssetCommandServiceTests.java`
- `docs/implementation/mvp/agent-tasks/M6/M6-04-master-service-tests.md`

### 実装結果

- `RequestTypeMasterServiceTests` を追加し、一覧、登録、編集、論理削除、復活、コード重複拒否、マスタ種別未設定時の 500、VIEWER / EDITOR 拒否を確認した
- `AssetCategoryMasterServiceTests` を追加し、資産分類でも同じ観点を確認した
- active 行だけ編集・論理削除できることを、`findActiveByIdAndCompanyId` が empty の場合は 404 になり更新系 Mapper が呼ばれない形で確認した
- deleted 行だけ復活できることを、`findDeletedByIdAndCompanyId` が empty の場合は 404 になり復活 Mapper が呼ばれない形で確認した
- 申請作成・編集で削除済みまたは他社の `REQUEST_TYPE` が POST された場合に 400 になることを確認した
- 資産編集で削除済みまたは他社の `ASSET_CATEGORY` が POST された場合に 400 になることを確認した

### 確認結果

- `cd apps/web && .\mvnw.cmd test` 成功
- Service テストを含む全 72 件が成功
- `git diff --check` で空白エラーなし

### 残課題

- Mapper / Testcontainers 強化は M8 で扱う
