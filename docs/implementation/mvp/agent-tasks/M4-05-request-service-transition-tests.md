# M4-05 Service 状態遷移テスト / M4 確認

## 目的

M4 の申請ワークフローについて、Service の状態遷移、本人条件、権限、ステータス条件をテストで確認する。

## M4 共通方針

- M4 では Service の状態遷移テストまで入れる
- Mapper / Testcontainers 強化は M8 に寄せる
- 操作可否は本人条件・権限・ステータスで制御する
- 会社境界は authority ではなく Mapper SQL の `company_id` 条件で守る
- DRAFT 編集は申請者本人のみ許可する
- SUBMITTED の取下げは申請者本人のみ許可する
- TENANT_MANAGER は、自分の申請について TENANT_EDITOR 相当の作成・編集・提出・取下げができる
- TENANT_MANAGER は、自分の申請も承認・却下・差戻しできる
- 差戻しは `SUBMITTED -> DRAFT` とする
- 却下理由・差戻し理由は同じ `review_comment` に入れる
- 再提出時も `review_comment` はクリアしない

## 対応範囲

- `RequestCommandService` の Service テストを作成する
- 申請作成の正常系を確認する
- DRAFT 編集の正常系を確認する
- DRAFT 編集が申請者本人のみ許可されることを確認する
- 提出の正常系 `DRAFT -> SUBMITTED` を確認する
- 提出時に `submitted_at` が設定されることを確認する
- 提出時に `review_comment` がクリアされないことを確認する
- 取下げの正常系 `SUBMITTED -> WITHDRAWN` を確認する
- 取下げが申請者本人のみ許可されることを確認する
- 承認の正常系 `SUBMITTED -> APPROVED` を確認する
- 却下の正常系 `SUBMITTED -> REJECTED` を確認する
- 差戻しの正常系 `SUBMITTED -> DRAFT` を確認する
- 却下理由が `review_comment` に保存されることを確認する
- 差戻し理由が `review_comment` に保存されることを確認する
- TENANT_MANAGER が自分の申請を承認・却下・差戻しできることを確認する
- TENANT_VIEWER が更新系操作を実行できないことを確認する
- TENANT_EDITOR が承認・却下・差戻しを実行できないことを確認する
- ステータス不一致の操作が拒否されることを確認する
- 他社申請は Service から更新されないことを確認する
- M4-01 から M4-04 の agent task Markdown に実装結果と確認結果が記録されていることを確認する

## 対応ファイル

- `apps/web/src/test/java/com/example/workops/request/service/RequestCommandServiceTests.java`
- `apps/web/src/main/java/com/example/workops/request/service/RequestCommandService.java`
- `apps/web/src/main/java/com/example/workops/request/mapper/RequestMapper.java`
- `docs/implementation/mvp/agent-tasks/M4-05-request-service-transition-tests.md`
- `docs/implementation/mvp/agent-tasks/M4-01-request-list-detail.md`
- `docs/implementation/mvp/agent-tasks/M4-02-request-draft-create-edit.md`
- `docs/implementation/mvp/agent-tasks/M4-03-request-submit-withdraw.md`
- `docs/implementation/mvp/agent-tasks/M4-04-request-review-actions.md`

## 除外範囲

- Mapper / Testcontainers 強化
- Flyway DDL 変更
- seed 変更
- README 更新
- `docs/implementation/mvp/phases.md` の modified 表示対応
- 資産 CRUD
- 資産ステータス変更
- 申請承認による資産ステータス自動変更
- 申請履歴テーブル
- 監査ログテーブル

## 完了条件

M4 の申請状態遷移と操作可否が Service テストで確認されている。
M4-01 から M4-04 の実装結果と確認結果が各 agent task Markdown に記録されている。
Mapper / Testcontainers 強化、DB変更、README変更、`phases.md` の modified 表示対応は M4 作業に含まれていない。

## 確認方法

- `cd apps/web && .\mvnw.cmd test`
- Service テストで DRAFT 編集本人条件を確認する
- Service テストで提出、取下げ、承認、却下、差戻しの正常系を確認する
- Service テストで権限不足、会社不一致、ステータス不一致、本人不一致の拒否を確認する
- M4-01 から M4-04 の agent task Markdown に実装結果と確認結果が記録されていることを確認する

## 実装時の記録

実装後に、次をこのファイルへ追記する。

- 実装方針
- 変更ファイル
- 実装結果
- 確認結果
- 残課題
