# M7-04 マスタ変更ログ

## 目的

申請種別・資産分類の主要更新系業務操作について、Service 層の業務処理の文脈で、成功または想定内拒否を業務操作ログとして出力する。

## M7 共通方針

- M7 で扱うログは、システムログ基盤ではなく業務操作ログとする
- 出力基盤は Spring Boot 標準の SLF4J + Logback を使う
- 業務操作ログは Service 層の業務処理の文脈で明示的に出力する
- AOP、独自アノテーション、汎用監査ログ基盤は MVP では採用しない
- ログ形式は JSON ではなく key=value 形式の構造化風メッセージにする
- 成功した主要業務操作は INFO で出力する
- 認可拒否、業務ルール違反などの想定内拒否は WARN で出力する
- 想定外例外は M7 で個別に抱え込まず、既存の例外処理・アプリケーションログに任せる
- ログ項目は ID 中心にする
- 個人情報・業務本文はログに出さない
- M7 では DB 変更を行わない
- Mapper / Testcontainers 強化は M8 に寄せる
- `apps/web` を正として進める

## 対象操作

- 申請種別登録
- 申請種別編集
- 申請種別論理削除
- 申請種別復活
- 資産分類登録
- 資産分類編集
- 資産分類論理削除
- 資産分類復活

## 対象外

- マスタ一覧表示
- マスタ詳細表示
- 削除済み表示の切替
- 申請・資産フォームの選択肢取得

## 対応範囲

- `RequestTypeMasterService` の申請種別登録成功時に INFO の業務操作ログを出力する
- `RequestTypeMasterService` の申請種別編集成功時に INFO の業務操作ログを出力する
- `RequestTypeMasterService` の申請種別論理削除成功時に INFO の業務操作ログを出力する
- `RequestTypeMasterService` の申請種別復活成功時に INFO の業務操作ログを出力する
- `AssetCategoryMasterService` の資産分類登録成功時に INFO の業務操作ログを出力する
- `AssetCategoryMasterService` の資産分類編集成功時に INFO の業務操作ログを出力する
- `AssetCategoryMasterService` の資産分類論理削除成功時に INFO の業務操作ログを出力する
- `AssetCategoryMasterService` の資産分類復活成功時に INFO の業務操作ログを出力する
- `@PreAuthorize` による認可拒否は Service 本体に入らないため、業務操作ログとしては出力しない
- 編集・論理削除の対象が取れない場合を WARN の業務操作ログとして出力する
- 復活の対象が取れない場合を WARN の業務操作ログとして出力する
- コード重複を WARN の業務操作ログとして出力する
- マスタ名称、氏名、メールアドレス、自由入力本文はログに出さない

## operation / targetType

- 申請種別登録: `operation=REQUEST_TYPE_CREATE`, `targetType=GENERIC_MASTER_VALUE`
- 申請種別編集: `operation=REQUEST_TYPE_UPDATE`, `targetType=GENERIC_MASTER_VALUE`
- 申請種別論理削除: `operation=REQUEST_TYPE_DELETE`, `targetType=GENERIC_MASTER_VALUE`
- 申請種別復活: `operation=REQUEST_TYPE_RESTORE`, `targetType=GENERIC_MASTER_VALUE`
- 資産分類登録: `operation=ASSET_CATEGORY_CREATE`, `targetType=GENERIC_MASTER_VALUE`
- 資産分類編集: `operation=ASSET_CATEGORY_UPDATE`, `targetType=GENERIC_MASTER_VALUE`
- 資産分類論理削除: `operation=ASSET_CATEGORY_DELETE`, `targetType=GENERIC_MASTER_VALUE`
- 資産分類復活: `operation=ASSET_CATEGORY_RESTORE`, `targetType=GENERIC_MASTER_VALUE`

## reasonCode

- 成功: `-`
- 編集・論理削除の対象なしまたは禁止: `MASTER_VALUE_NOT_FOUND_OR_FORBIDDEN`
- 復活対象なしまたは削除済みでない: `MASTER_VALUE_NOT_FOUND_OR_NOT_DELETED`
- コード重複: `DUPLICATE_MASTER_VALUE_CODE`

## 対応ファイル

- `apps/web/src/main/java/com/example/workops/master/service/RequestTypeMasterService.java`
- `apps/web/src/main/java/com/example/workops/master/service/AssetCategoryMasterService.java`
- `apps/web/src/main/java/com/example/workops/master/mapper/RequestTypeMasterMapper.java`
- `apps/web/src/main/java/com/example/workops/master/mapper/AssetCategoryMasterMapper.java`
- `apps/web/src/main/resources/mapper/master/RequestTypeMasterMapper.xml`
- `apps/web/src/main/resources/mapper/master/AssetCategoryMasterMapper.xml`
- `apps/web/src/test/java/com/example/workops/master/service/RequestTypeMasterServiceTests.java`
- `apps/web/src/test/java/com/example/workops/master/service/AssetCategoryMasterServiceTests.java`
- `docs/implementation/mvp/agent-tasks/M7/M7-04-master-operation-logs.md`

## 除外範囲

- 参照系ログ
- Controller 横断ログ
- AOP
- 独自アノテーション
- 監査ログテーブル
- 履歴テーブル
- DB 変更
- `before_json`
- `after_json`
- マスタ名称のログ出力
- 拒否理由を細分化するためだけの追加 Mapper 参照
- Testcontainers 強化

## 完了条件

申請種別・資産分類の対象操作について、成功時に INFO、想定内拒否時に WARN の業務操作ログが `OperationLogger` 相当の共通部品から出力される。
マスタ名称、氏名、メールアドレス、自由入力本文がログに含まれない。

## 確認方法

- `cd apps/web && .\mvnw.cmd test`
- 申請種別の登録、編集、論理削除、復活の成功時に `OperationLogger` が呼ばれることを確認する
- 資産分類の登録、編集、論理削除、復活の成功時に `OperationLogger` が呼ばれることを確認する
- コード重複、編集・論理削除の対象なし、復活の対象なしで `OperationLogger` が WARN 用に呼ばれることを確認する
- `@PreAuthorize` で拒否される操作では `OperationLogger` が呼ばれないことを確認する
- マスタ名称がログ項目として渡されないことを確認する

## 実装時の記録

実装後に、次をこのファイルへ追記する。

- 実装方針
- 変更ファイル
- 実装結果
- 確認結果
- 残課題

## 実装時の記録 2026-05-15

### 実装方針

- `OperationLogger` を `RequestTypeMasterService` と `AssetCategoryMasterService` に constructor injection で追加した。
- 対象操作は申請種別・資産分類の登録、編集、論理削除、復活に限定した。
- 成功時は更新処理完了後に `logSuccess` を呼び出す。
- 登録成功時は `INSERT` 後に `findLastInsertId()` で採番済み `generic_master_values.id` を取得し、`targetId` に設定する。
- Service 内で判定できる想定内拒否は、既存例外を throw する直前に `logRejected` を呼び出す。
- `findList`、`findForEdit`、`isDuplicateCodeForCreate` は参照系として業務操作ログを出さない。
- `@PreAuthorize` による拒否は Service 本体に入らないため、業務操作ログ対象外とした。
- 拒否理由を細分化するためだけの追加照会は行わず、対象が取れない場合は統合 reasonCode に寄せた。

### 変更ファイル

- `apps/web/src/main/java/com/example/workops/master/service/RequestTypeMasterService.java`
- `apps/web/src/main/java/com/example/workops/master/service/AssetCategoryMasterService.java`
- `apps/web/src/main/java/com/example/workops/master/mapper/RequestTypeMasterMapper.java`
- `apps/web/src/main/java/com/example/workops/master/mapper/AssetCategoryMasterMapper.java`
- `apps/web/src/main/resources/mapper/master/RequestTypeMasterMapper.xml`
- `apps/web/src/main/resources/mapper/master/AssetCategoryMasterMapper.xml`
- `apps/web/src/test/java/com/example/workops/master/service/RequestTypeMasterServiceTests.java`
- `apps/web/src/test/java/com/example/workops/master/service/AssetCategoryMasterServiceTests.java`
- `docs/implementation/mvp/agent-tasks/M7/M7-04-master-operation-logs.md`

### 実装結果

- 成功ログ:
  - `REQUEST_TYPE_CREATE / GENERIC_MASTER_VALUE`
  - `REQUEST_TYPE_UPDATE / GENERIC_MASTER_VALUE`
  - `REQUEST_TYPE_DELETE / GENERIC_MASTER_VALUE`
  - `REQUEST_TYPE_RESTORE / GENERIC_MASTER_VALUE`
  - `ASSET_CATEGORY_CREATE / GENERIC_MASTER_VALUE`
  - `ASSET_CATEGORY_UPDATE / GENERIC_MASTER_VALUE`
  - `ASSET_CATEGORY_DELETE / GENERIC_MASTER_VALUE`
  - `ASSET_CATEGORY_RESTORE / GENERIC_MASTER_VALUE`
- 拒否ログ:
  - `DUPLICATE_MASTER_VALUE_CODE`
  - `MASTER_VALUE_NOT_FOUND_OR_FORBIDDEN`
  - `MASTER_VALUE_NOT_FOUND_OR_NOT_DELETED`
- マスタ操作の `reasonCommentPresent` は常に `false` とした。
- 拒否ログの `exceptionType` は `ResponseStatusException` とした。
- マスタ名称、氏名、メールアドレス、自由入力本文は `OperationLogRecord` の専用フィールドとして持たせていない。

### 確認結果

- `cd apps/web && .\mvnw.cmd test` を実行し、78 tests / failures 0 / errors 0 を確認した。
- `git diff --check` を実行し、空白エラーがないことを確認した。
- `RequestTypeMasterServiceTests` と `AssetCategoryMasterServiceTests` で、成功ログ、想定内拒否ログ、`@PreAuthorize` 拒否時にログが呼ばれないことを確認した。

### 残課題

- 拒否理由の細分化は M7-04 では行わない。必要な場合は M8 以降で Mapper / Testcontainers 強化と合わせて扱う。
