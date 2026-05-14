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
- 認可拒否を WARN の業務操作ログとして出力する
- 会社境界不一致を WARN の業務操作ログとして出力する
- 対象外マスタ種別への操作を WARN の業務操作ログとして出力する
- 論理削除済み行への通常編集を WARN の業務操作ログとして出力する
- active 行への復活を WARN の業務操作ログとして出力する
- コード重複を WARN の業務操作ログとして出力する
- マスタ名称、氏名、メールアドレス、自由入力本文はログに出さない

## operation / targetType

- 申請種別登録: `operation=REQUEST_TYPE_CREATE`, `targetType=REQUEST_TYPE`
- 申請種別編集: `operation=REQUEST_TYPE_UPDATE`, `targetType=REQUEST_TYPE`
- 申請種別論理削除: `operation=REQUEST_TYPE_DELETE`, `targetType=REQUEST_TYPE`
- 申請種別復活: `operation=REQUEST_TYPE_RESTORE`, `targetType=REQUEST_TYPE`
- 資産分類登録: `operation=ASSET_CATEGORY_CREATE`, `targetType=ASSET_CATEGORY`
- 資産分類編集: `operation=ASSET_CATEGORY_UPDATE`, `targetType=ASSET_CATEGORY`
- 資産分類論理削除: `operation=ASSET_CATEGORY_DELETE`, `targetType=ASSET_CATEGORY`
- 資産分類復活: `operation=ASSET_CATEGORY_RESTORE`, `targetType=ASSET_CATEGORY`

## reasonCode

- 成功: `-`
- 認可拒否: `ACCESS_DENIED`
- 会社境界不一致: `COMPANY_MISMATCH`
- 対象外マスタ種別: `MASTER_TYPE_MISMATCH`
- 論理削除済み行への通常編集: `MASTER_VALUE_DELETED`
- active 行への復活: `MASTER_VALUE_ACTIVE`
- コード重複: `DUPLICATE_MASTER_VALUE_CODE`
- 入力値不正: `VALIDATION_ERROR`

## 対応ファイル

- `apps/web/src/main/java/com/example/workops/master/service/RequestTypeMasterService.java`
- `apps/web/src/main/java/com/example/workops/master/service/AssetCategoryMasterService.java`
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
- Mapper / Testcontainers 強化

## 完了条件

申請種別・資産分類の対象操作について、成功時に INFO、想定内拒否時に WARN の業務操作ログが `OperationLogger` 相当の共通部品から出力される。
マスタ名称、氏名、メールアドレス、自由入力本文がログに含まれない。

## 確認方法

- `cd apps/web && .\mvnw.cmd test`
- 申請種別の登録、編集、論理削除、復活の成功時に `OperationLogger` が呼ばれることを確認する
- 資産分類の登録、編集、論理削除、復活の成功時に `OperationLogger` が呼ばれることを確認する
- 認可拒否、会社境界不一致、対象外マスタ種別、削除済み行への通常編集、active 行への復活、コード重複で `OperationLogger` が WARN 用に呼ばれることを確認する
- マスタ名称がログ項目として渡されないことを確認する

## 実装時の記録

実装後に、次をこのファイルへ追記する。

- 実装方針
- 変更ファイル
- 実装結果
- 確認結果
- 残課題
