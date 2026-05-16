# M9-05 M9 確認 / 記録

## 目的

M9 の会社作成、テナント初期化、部署管理が完了条件を満たしていることを確認し、各 agent task Markdown に実装結果を記録する。

## M9 共通方針

- M9 の範囲は、会社作成、部署管理、会社作成時の会社別 `generic_master_values` 初期投入までとする
- 初期 TENANT_MANAGER 作成、ユーザー作成、権限割当・変更、local Cognito Fake Bean、疑似 `cognito_sub` 発行は M10 で扱う
- PLATFORM_ADMIN 疑似ユーザーは M9 前提として扱う
- ユーザー情報は既存の `users` テーブルを使い、別のユーザーテーブルは作らない
- `users.actor_type = 'PLATFORM'` のユーザーは `company_id = NULL` とする
- `users.actor_type = 'TENANT'` のユーザーは `company_id` 必須とする
- 会社作成時の会社別 `generic_master_values` 初期投入は、会社作成に伴うテナント初期化処理として扱う
- `generic_master` 種別管理やマスタ管理画面の拡張は M9 で扱わない
- Git commit は実行しない

## 対応範囲

- M9-01 から M9-04 の実装結果を確認する
- PLATFORM_ADMIN 疑似ユーザーの local profile 動作を確認する
- 会社作成と会社別 `generic_master_values` 初期投入を確認する
- PLATFORM_ADMIN の部署管理操作を確認する
- TENANT_MANAGER の自社部署管理操作を確認する
- TENANT_MANAGER が他社部署を管理できないことを確認する
- 既存の申請、資産、申請種別マスタ、資産分類マスタの主要画面が壊れていないことを確認する
- M9 で変更した DB 定義と `docs/implementation/db/schema.md` の整合を確認する
- M9 で変更した README または実装ドキュメントがある場合は、現行実装と矛盾しないことを確認する
- M9-01 から M9-04 の agent task Markdown に `実装方針`、`変更ファイル`、`実装結果`、`確認結果`、`残課題` を記録する
- M10 に回す残課題を明記する

## 対応ファイル

- `docs/implementation/mvp/agent-tasks/M9/M9-01-platform-admin-local-prerequisite.md`
- `docs/implementation/mvp/agent-tasks/M9/M9-02-company-create-tenant-initialization.md`
- `docs/implementation/mvp/agent-tasks/M9/M9-03-department-list-create.md`
- `docs/implementation/mvp/agent-tasks/M9/M9-04-department-edit-delete-deleted-view.md`
- `docs/implementation/mvp/agent-tasks/M9/M9-05-m9-verification-and-task-record.md`
- `docs/implementation/db/schema.md`
- `README.md`
- `docs/implementation/**/*.md`

## 除外範囲

- M10 の agent task 作成
- 初期 TENANT_MANAGER 作成
- ユーザー作成
- 権限割当・変更
- local Cognito Fake Bean
- 疑似 `cognito_sub` 発行
- Cognito 本物 API 呼び出し
- Phase 2 設計変更
- README / docs の無断拡張
- git commit

## 完了条件

M9 の完了条件を満たしたことが、テスト結果、画面またはHTTP確認、DB確認、agent task Markdown の記録で説明できる。
M10 へ回す範囲が、初期 TENANT_MANAGER 作成、ユーザー作成、権限割当・変更、local Cognito Fake Bean、疑似 `cognito_sub` 発行として明確になっている。

## 確認方法

- `cd apps/web && .\mvnw.cmd test`
- local profile の PLATFORM_ADMIN で `/auth/claims` が HTTP 200 を返すことを確認する
- local profile の PLATFORM_ADMIN で会社を作成できることを確認する
- 作成会社に会社別 `REQUEST_TYPE` 初期値が投入されることを確認する
- 作成会社に会社別 `ASSET_CATEGORY` 初期値が投入されることを確認する
- local profile の PLATFORM_ADMIN で作成会社の部署を作成・編集・論理削除できることを確認する
- local profile の TENANT_MANAGER で自社部署を作成・編集・論理削除できることを確認する
- local profile の TENANT_MANAGER が他社部署を作成・編集・論理削除できないことを確認する
- 所属ユーザーがいる部署を警告表示したうえで論理削除できることを確認する
- 削除済み部署コードを同一会社で再利用できないことを確認する
- 既存の `/requests`、`/assets`、`/masters/request-types`、`/masters/asset-categories` が HTTP 200 を返すことを確認する
- `git diff --check`

## 実装時の記録

実装後に、次をこのファイルへ追記する。

- 実装方針
- 変更ファイル
- 実装結果
- 確認結果
- 残課題
