# M10-06 M10 確認 / 記録

## 目的

M10 の初期 TENANT_MANAGER 作成、ユーザー作成、権限割当・変更、local Cognito Fake Bean、疑似 `cognito_sub` 発行・登録が完了条件を満たしていることを確認し、各 agent task Markdown に実装結果を記録する。

## M10 共通方針

- M10 の範囲は、初期 TENANT_MANAGER 作成、ユーザー作成、権限割当・変更、local Cognito Fake Bean、疑似 `cognito_sub` 発行・登録に限定する
- M10 で使う Cognito 本物 API は `AdminCreateUser` に限定する
- Cognito Hosted UI 本格確認、PLATFORM / TENANT App Client 分離、Cognito Trigger、Pre Token Generation は Phase 2 で扱う
- M9 では会社・部署管理を扱い、M10 で会社作成フローへ初期 TENANT_MANAGER 作成を後付け連携する
- 最終仕様として、会社作成時に初期 TENANT_MANAGER を必ず 1 人作る
- ユーザー作成後に編集できる範囲は、表示名、email、所属部署、権限セットだけとする
- `actor_type` と `company_id` は作成後変更不可とする
- TENANT_MANAGER 数が 0 になる権限変更は、認可ではなく、更新後 count による業務バリデーションで拒否する
- 削除済み部署に所属するユーザーは、ユーザー一覧・詳細で `部署名（削除済み）` と表示する
- Git commit は実行しない

## 対応範囲

- M10-01 から M10-05 の実装結果を確認する
- local Cognito Fake Bean と疑似 `cognito_sub` 発行・登録を確認する
- PLATFORM_ADMIN による会社作成時の初期 TENANT_MANAGER 作成を確認する
- PLATFORM_ADMIN による PLATFORM ユーザー作成を確認する
- PLATFORM_ADMIN による任意会社の TENANT ユーザー作成を確認する
- TENANT_MANAGER による自社 TENANT ユーザー作成を確認する
- PLATFORM_ADMIN のユーザー一覧・詳細・編集・権限変更を確認する
- TENANT_MANAGER の自社ユーザー一覧・詳細・編集・権限変更を確認する
- TENANT_MANAGER が他社ユーザー、PLATFORM ユーザー、`PLATFORM_ADMIN` を扱えないことを確認する
- TENANT_MANAGER 数が 0 になる権限変更が拒否されることを確認する
- 削除済み部署に所属するユーザーが `部署名（削除済み）` と表示されることを確認する
- M10-01 から M10-05 の agent task Markdown に `実装方針`、`変更ファイル`、`実装結果`、`確認結果`、`残課題` を記録する
- Phase 2 に回す残課題を明記する
- M10 で README または実装ドキュメントを更新した場合は、現行実装と矛盾しないことを確認する

## 対応ファイル

- `docs/implementation/mvp/agent-tasks/M10/M10-01-local-cognito-fake-sub-issuer.md`
- `docs/implementation/mvp/agent-tasks/M10/M10-02-user-list-detail.md`
- `docs/implementation/mvp/agent-tasks/M10/M10-03-platform-admin-user-create-and-initial-manager.md`
- `docs/implementation/mvp/agent-tasks/M10/M10-04-tenant-manager-user-create.md`
- `docs/implementation/mvp/agent-tasks/M10/M10-05-user-edit-permission-change.md`
- `docs/implementation/mvp/agent-tasks/M10/M10-06-m10-verification-and-task-record.md`
- `README.md`
- `docs/implementation/**/*.md`

## 除外範囲

- M10 の追加 agent task 作成
- Phase 2 設計の固定
- Cognito 本物 API 呼び出し
- Cognito Hosted UI 本格確認
- PLATFORM / TENANT App Client 分離
- Cognito Trigger
- Pre Token Generation
- Cognito 側ユーザー属性変更
- ユーザー無効化
- 権限セット定義そのものの CRUD
- git commit

## 完了条件

M10 の完了条件を満たしたことが、テスト結果、画面または HTTP 確認、DB 確認、agent task Markdown の記録で説明できる。
M10 の範囲が、初期 TENANT_MANAGER 作成、ユーザー作成、権限割当・変更、local Cognito Fake Bean、疑似 `cognito_sub` 発行・登録に限定されている。
Phase 2 へ回す範囲が、Cognito 本物 API 呼び出し、Cognito Hosted UI 本格確認、PLATFORM / TENANT App Client 分離として明確になっている。
作業区切りとしてユーザーにコミットを促せる状態になっている。

## 確認方法

- `cd apps/web && .\mvnw.cmd test`
- local profile の PLATFORM_ADMIN で会社作成時に初期 TENANT_MANAGER が作成されることを確認する
- local profile の PLATFORM_ADMIN で PLATFORM ユーザーを作成できることを確認する
- local profile の PLATFORM_ADMIN で任意会社の TENANT ユーザーを作成できることを確認する
- local profile の TENANT_MANAGER で自社 TENANT ユーザーを作成できることを確認する
- local profile の PLATFORM_ADMIN でユーザー一覧・詳細・編集・権限変更を確認する
- local profile の TENANT_MANAGER で自社ユーザー一覧・詳細・編集・権限変更を確認する
- TENANT_MANAGER が他社ユーザー、PLATFORM ユーザー、`PLATFORM_ADMIN` を扱えないことを確認する
- TENANT_MANAGER 数が 0 になる権限変更が拒否されることを確認する
- 削除済み部署に所属するユーザーが `部署名（削除済み）` と表示されることを確認する
- `git diff --check`

## 実装時の記録

M10-06 実装時に、実装方針、変更ファイル、実装結果、確認結果、残課題を記録する。
