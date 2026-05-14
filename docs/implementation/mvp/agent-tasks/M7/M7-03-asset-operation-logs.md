# M7-03 資産操作ログ

## 目的

資産の主要更新系業務操作について、Service 層の業務処理の文脈で、成功または想定内拒否を業務操作ログとして出力する。

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

- 資産登録
- 資産編集
- 資産ステータス変更
- 資産論理削除

## 対象外

- 資産一覧表示
- 資産詳細表示
- 資産検索
- 資産フォーム選択肢取得

## 対応範囲

- `AssetCommandService` の資産登録成功時に INFO の業務操作ログを出力する
- `AssetCommandService` の資産編集成功時に INFO の業務操作ログを出力する
- `AssetCommandService` の資産ステータス変更成功時に INFO の業務操作ログを出力する
- `AssetCommandService` の資産論理削除成功時に INFO の業務操作ログを出力する
- `@PreAuthorize` による認可拒否は Service 本体に入らないため、業務操作ログとしては出力しない
- 会社境界不一致を WARN の業務操作ログとして出力する
- 論理削除済み資産への更新操作は現状の SQL 条件上、会社境界不一致と同じ `COMPANY_MISMATCH` として出力する
- 不正な資産分類を WARN の業務操作ログとして出力する
- 不正な部署を WARN の業務操作ログとして出力する
- 不正な資産ステータスを WARN の業務操作ログとして出力する
- 会社内の資産コード重複を WARN の業務操作ログとして出力する
- 資産名、備考、氏名、メールアドレス、自由入力本文はログに出さない

## operation / targetType

- 資産登録: `operation=ASSET_CREATE`, `targetType=ASSET`
- 資産編集: `operation=ASSET_UPDATE`, `targetType=ASSET`
- 資産ステータス変更: `operation=ASSET_STATUS_CHANGE`, `targetType=ASSET`
- 資産論理削除: `operation=ASSET_DELETE`, `targetType=ASSET`

## reasonCode

- 成功: `-`
- 会社境界不一致: `COMPANY_MISMATCH`
- 資産分類不正: `INVALID_ASSET_CATEGORY`
- 部署不正: `INVALID_DEPARTMENT`
- 資産ステータス不正: `INVALID_ASSET_STATUS`
- 資産コード重複: `DUPLICATE_ASSET_CODE`

## 対応ファイル

- `apps/web/src/main/java/com/example/workops/asset/service/AssetCommandService.java`
- `apps/web/src/test/java/com/example/workops/asset/service/AssetCommandServiceTests.java`
- `docs/implementation/mvp/agent-tasks/M7/M7-03-asset-operation-logs.md`

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
- 資産名のログ出力
- 備考のログ出力
- Mapper / Testcontainers 強化

## 完了条件

資産の対象操作について、成功時に INFO、想定内拒否時に WARN の業務操作ログが `OperationLogger` 相当の共通部品から出力される。
資産名、備考、氏名、メールアドレス、自由入力本文がログに含まれない。

## 確認方法

- `cd apps/web && .\mvnw.cmd test`
- 資産登録、編集、ステータス変更、論理削除の成功時に `OperationLogger` が呼ばれることを確認する
- 資産の会社境界不一致、不正分類、不正部署、不正ステータス、コード重複で `OperationLogger` が WARN 用に呼ばれることを確認する
- `@PreAuthorize` で拒否される更新系操作では `OperationLogger` が呼ばれないことを確認する
- 資産名、備考がログ項目として渡されないことを確認する

## 実装時の記録

実装後に、次をこのファイルへ追記する。

- 実装方針
- 変更ファイル
- 実装結果
- 確認結果
- 残課題

## 実装時の記録 2026-05-14

### 実装方針

- `OperationLogger` を `AssetCommandService` に constructor injection で追加した。
- 対象操作は資産登録、編集、ステータス変更、論理削除に限定した。
- 成功時は更新処理完了後に `logSuccess` を呼び出す。
- Service 内で判定できる想定内拒否は、既存例外を throw する直前に `logRejected` を呼び出す。
- read 系の `findAssetForEdit`、`findAssetForStatusChange`、`isDuplicateCodeForCreate`、`isDuplicateCodeForUpdate` では業務操作ログを出さない。
- `@PreAuthorize` による拒否は Service 本体に入らないため、業務操作ログ対象外とした。
- 論理削除済み資産は現状の Mapper 条件では他社資産と区別せず、`COMPANY_MISMATCH` に寄せた。

### 変更ファイル

- `apps/web/src/main/java/com/example/workops/asset/service/AssetCommandService.java`
- `apps/web/src/test/java/com/example/workops/asset/service/AssetCommandServiceTests.java`
- `docs/implementation/mvp/agent-tasks/M7/M7-03-asset-operation-logs.md`

### 実装結果

- 成功ログ:
  - `ASSET_CREATE / ASSET`
  - `ASSET_UPDATE / ASSET`
  - `ASSET_STATUS_CHANGE / ASSET`
  - `ASSET_DELETE / ASSET`
- 拒否ログ:
  - `COMPANY_MISMATCH`
  - `INVALID_ASSET_CATEGORY`
  - `INVALID_DEPARTMENT`
  - `INVALID_ASSET_STATUS`
  - `DUPLICATE_ASSET_CODE`
- 資産操作の `reasonCommentPresent` は常に `false` とした。
- 拒否ログの `exceptionType` は `ResponseStatusException` とした。
- 資産コード、資産名、備考、部署名、カテゴリ名は `OperationLogRecord` の専用フィールドとして持たせていない。

### 確認結果

- `cd apps/web && .\mvnw.cmd test` を実行し、78 tests / failures 0 / errors 0 を確認した。
- `git diff --check` を実行し、空白エラーがないことを確認した。
- `AssetCommandServiceTests` で、成功ログ、想定内拒否ログ、`@PreAuthorize` 拒否時にログが呼ばれないことを確認した。

### 残課題

- 削除済み資産と他社資産の区別は M7-03 では行わない。Mapper 追加や DB 確認が必要な場合は M8 以降で扱う。
- Mapper / Testcontainers 強化は既存方針どおり M8 に寄せる。
