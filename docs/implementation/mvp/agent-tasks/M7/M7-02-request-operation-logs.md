# M7-02 申請操作ログ

## 目的

申請の主要更新系業務操作について、Service 層の業務処理の文脈で、成功または想定内拒否を業務操作ログとして出力する。

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
- `review_comment` 本文は出さず、有無だけを `reasonCommentPresent` として出力する
- M7 では DB 変更を行わない
- Mapper / Testcontainers 強化は M8 に寄せる
- `apps/web` を正として進める

## 対象操作

- 申請作成
- 申請編集
- 申請提出
- 申請承認
- 申請却下
- 申請差戻し
- 申請取下げ

## 対象外

- 申請一覧表示
- 申請詳細表示
- 申請検索
- 申請フォーム選択肢取得

## 対応範囲

- `RequestCommandService` の申請作成成功時に INFO の業務操作ログを出力する
- `RequestCommandService` の申請編集成功時に INFO の業務操作ログを出力する
- `RequestCommandService` の申請提出成功時に INFO の業務操作ログを出力する
- `RequestCommandService` の申請承認成功時に INFO の業務操作ログを出力する
- `RequestCommandService` の申請却下成功時に INFO の業務操作ログを出力する
- `RequestCommandService` の申請差戻し成功時に INFO の業務操作ログを出力する
- `RequestCommandService` の申請取下げ成功時に INFO の業務操作ログを出力する
- 認可拒否を WARN の業務操作ログとして出力する
- 本人条件不一致を WARN の業務操作ログとして出力する
- 会社境界不一致を WARN の業務操作ログとして出力する
- 状態不一致を WARN の業務操作ログとして出力する
- 削除済みまたは他社の申請種別が POST された場合を WARN の業務操作ログとして出力する
- 削除済みまたは他社の関連資産が POST された場合を WARN の業務操作ログとして出力する
- 却下・差戻しコメント本文は出さず、`reasonCommentPresent` のみ出力する
- 申請タイトル、申請内容、氏名、メールアドレス、自由入力本文はログに出さない

## operation / targetType

- 申請作成: `operation=REQUEST_CREATE`, `targetType=REQUEST`
- 申請編集: `operation=REQUEST_UPDATE`, `targetType=REQUEST`
- 申請提出: `operation=REQUEST_SUBMIT`, `targetType=REQUEST`
- 申請承認: `operation=REQUEST_APPROVE`, `targetType=REQUEST`
- 申請却下: `operation=REQUEST_REJECT`, `targetType=REQUEST`
- 申請差戻し: `operation=REQUEST_REMAND`, `targetType=REQUEST`
- 申請取下げ: `operation=REQUEST_WITHDRAW`, `targetType=REQUEST`

## reasonCode

- 成功: `-`
- 認可拒否: `ACCESS_DENIED`
- 本人条件不一致: `REQUESTER_MISMATCH`
- 会社境界不一致: `COMPANY_MISMATCH`
- 状態不一致: `STATUS_MISMATCH`
- 申請種別不正: `INVALID_REQUEST_TYPE`
- 関連資産不正: `INVALID_ASSET`
- 入力値不正: `VALIDATION_ERROR`

## 対応ファイル

- `apps/web/src/main/java/com/example/workops/request/service/RequestCommandService.java`
- `apps/web/src/test/java/com/example/workops/request/service/RequestCommandServiceTests.java`
- `docs/implementation/mvp/agent-tasks/M7/M7-02-request-operation-logs.md`

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
- 申請タイトルのログ出力
- 申請内容のログ出力
- 却下・差戻しコメント本文のログ出力
- Mapper / Testcontainers 強化

## 完了条件

申請の対象操作について、成功時に INFO、想定内拒否時に WARN の業務操作ログが `OperationLogger` 相当の共通部品から出力される。
申請タイトル、申請内容、却下・差戻しコメント本文、氏名、メールアドレス、自由入力本文がログに含まれない。

## 確認方法

- `cd apps/web && .\mvnw.cmd test`
- 申請作成、編集、提出、承認、却下、差戻し、取下げの成功時に `OperationLogger` が呼ばれることを確認する
- 申請の認可拒否、本人条件不一致、会社境界不一致、状態不一致で `OperationLogger` が WARN 用に呼ばれることを確認する
- 却下・差戻しではコメント本文ではなく `reasonCommentPresent=true` が渡されることを確認する
- 申請タイトル、申請内容、コメント本文がログ項目として渡されないことを確認する

## 実装時の記録

### 実装方針

- `RequestCommandService` に `OperationLogger` を constructor injection で追加する
- 申請の作成、編集、提出、承認、却下、差戻し、取下げの成功時は、更新処理完了後に `logSuccess` を呼ぶ
- 作成成功ログの `targetId` は `findLastInsertId()` で取得した申請IDを使う
- 編集、提出、承認、却下、差戻し、取下げの成功ログの `targetId` はメソッド引数の申請IDを使う
- 却下、差戻しだけ `reviewComment` の有無を `reasonCommentPresent` として渡し、本文は渡さない
- `@PreAuthorize` による権限不足は Service メソッド本体に入る前に拒否されるため、M7-02 の業務操作ログ対象外とする
- Service 内で判定できる会社境界、本人条件、ステータス、申請種別、関連資産の想定内拒否だけを `logRejected` で出力する
- 参照系の `findDraftForEdit`、`findSubmittedForReject`、`findSubmittedForRemand` では業務操作ログを出さない

### 変更ファイル

- `apps/web/src/main/java/com/example/workops/request/service/RequestCommandService.java`
- `apps/web/src/test/java/com/example/workops/request/service/RequestCommandServiceTests.java`
- `apps/web/src/test/java/com/example/workops/request/service/RequestAssetLinkServiceTests.java`
- `docs/implementation/mvp/agent-tasks/M7/M7-02-request-operation-logs.md`

### 実装結果

- `RequestCommandService` の申請作成、編集、提出、承認、却下、差戻し、取下げ成功時に `OperationLogger#logSuccess` を呼ぶようにした
- `REQUEST_CREATE`、`REQUEST_UPDATE`、`REQUEST_SUBMIT`、`REQUEST_APPROVE`、`REQUEST_REJECT`、`REQUEST_REMAND`、`REQUEST_WITHDRAW` の operation を固定した
- targetType はすべて `REQUEST` とした
- 会社境界不一致は `COMPANY_MISMATCH`、本人条件不一致は `REQUESTER_MISMATCH`、状態不一致は `STATUS_MISMATCH` として `OperationLogger#logRejected` を呼ぶようにした
- 申請種別不正は `INVALID_REQUEST_TYPE`、関連資産不正は `INVALID_ASSET` として `OperationLogger#logRejected` を呼ぶようにした
- 拒否時の `exceptionType` は、`AccessDeniedException` または `ResponseStatusException` を渡すようにした
- `RequestCommandServiceTests` に `OperationLogger` mock を追加し、成功ログと拒否ログの `OperationLogRecord` を検証するようにした
- `RequestAssetLinkServiceTests` のテスト用 Bean 定義を、`OperationLogger` 依存を含む新しい `RequestCommandService` コンストラクタに合わせた
- 申請タイトル、申請内容、却下・差戻しコメント本文は `OperationLogRecord` に渡していない

### 確認結果

- `cd apps/web && .\mvnw.cmd test` 成功
- 全 77 件が成功
- `RequestCommandServiceTests` は 18 件成功
- `RequestAssetLinkServiceTests` は 8 件成功
- `git diff --check` で空白エラーなし

### 残課題

- `@PreAuthorize` による権限不足は、M7-02 では業務操作ログとして出さず、Spring Security / 既存例外処理側の拒否ログとして扱う
- 資産 Service への組み込みは M7-03 で扱う
- マスタ Service への組み込みは M7-04 で扱う
- Mapper / Testcontainers 強化は M8 で扱う
