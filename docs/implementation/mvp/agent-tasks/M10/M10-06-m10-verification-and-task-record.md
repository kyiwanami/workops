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

### 実装方針

- M10-06 では新規業務機能、DB 変更、Cognito 設定変更を行わない
- M10-01 から M10-05 の実装結果を横断確認し、M10 完了状態を記録する
- README と MVP 動作確認シナリオを、M10 のユーザー管理導線が分かる状態へ更新する
- M10-01 から M10-05 の古い残課題は、後続 M10 task で解消済みのものと Phase 2 残しに分けて整理する
- 実 Cognito E2E は行わず、ユーザー確認対象として残す
- Git commit は実行しない

### 変更ファイル

- `README.md`
- `docs/implementation/mvp/verification-scenario.md`
- `docs/implementation/mvp/agent-tasks/M10/M10-01-local-cognito-fake-sub-issuer.md`
- `docs/implementation/mvp/agent-tasks/M10/M10-02-user-list-detail.md`
- `docs/implementation/mvp/agent-tasks/M10/M10-03-platform-admin-user-create-and-initial-manager.md`
- `docs/implementation/mvp/agent-tasks/M10/M10-04-tenant-manager-user-create.md`
- `docs/implementation/mvp/agent-tasks/M10/M10-05-user-edit-permission-change.md`
- `docs/implementation/mvp/agent-tasks/M10/M10-06-m10-verification-and-task-record.md`

### 実装結果

- README の MVP 説明に会社・部署管理、ユーザー管理を含めた
- README の業務操作一覧に、PLATFORM_ADMIN / TENANT_MANAGER のユーザー管理と会社作成時の初期 TENANT_MANAGER 作成を追加した
- README の主要 URL に `/admin/users`、`/admin/users/new`、`/admin/users/{userId}`、`/admin/users/{userId}/edit`、`/users`、`/users/new`、`/users/{userId}`、`/users/{userId}/edit` を追加した
- README の Cognito 説明に、M10 では `AdminCreateUser` 呼び出し境界だけを扱い、実 Cognito E2E と Hosted UI 本格確認は後続確認・Phase 2 側へ残すことを明記した
- `verification-scenario.md` に PLATFORM_ADMIN の local 疑似ユーザーと、ユーザー・権限管理の確認シナリオを追加した
- M10-01 から M10-05 の残課題を確認し、M10 内で後続対応済みのものを対応済みとして整理した

### 確認結果

- local profile の PLATFORM_ADMIN で `/admin/users` を表示し、PLATFORM ユーザーと全会社の TENANT ユーザーが一覧表示されることを確認した
- local profile の PLATFORM_ADMIN で `/admin/users/new` から PLATFORM ユーザー `m1006-platform-2150` を作成し、詳細で local Fake の UUID 形式 `cognito_sub` と `PLATFORM_ADMIN` 権限が表示されることを確認した
- local profile の PLATFORM_ADMIN で PLATFORM ユーザー `m1006-platform-2150` の表示名を編集できることを確認した
- local profile の PLATFORM_ADMIN で `/admin/users/new` から北浜精密機器株式会社の TENANT ユーザー `m1006-tenant-2151` を作成し、部署、`TENANT_EDITOR` 権限、local Fake の `cognito_sub` が登録されることを確認した
- local profile の PLATFORM_ADMIN で `/admin/companies/new` から会社 `M1006_COMP_2152` と初期 TENANT_MANAGER `m1006-initial-manager-2152` を作成し、ユーザー一覧で `TENANT_MANAGER` 権限が表示されることを確認した
- local profile の PLATFORM_ADMIN で一時部署 `M1006DEL2153` と所属ユーザー `m1006-deleted-dept-user-2153` を作成し、部署論理削除後にユーザー一覧で `M10-06削除予定部署（削除済み）` と表示されることを確認した
- local profile の TENANT_MANAGER で `/users` を表示し、自社 TENANT ユーザーだけが一覧表示されることを確認した
- local profile の TENANT_MANAGER で `/users/new` から自社ユーザー `m1006-tenant-self-2156` を作成し、詳細と編集画面で自社スコープの情報だけを扱えることを確認した
- local profile の TENANT_MANAGER で `/users/4` は他社ユーザーとして `404`、`/users/7` は PLATFORM ユーザーとして `404`、`/admin/users` は `403` になることを確認した
- DB 確認で、作成した M10-06 確認用ユーザーに UUID 形式の `cognito_sub` と期待する権限セットが登録されていることを確認した
- Service テストと Mapper 統合テストの既存確認で、TENANT_MANAGER 数が 0 になる権限変更拒否、権限セット不整合拒否、他社部署・削除済み部署拒否、email / username 重複拒否を確認済み
- `cd apps/web && .\mvnw.cmd test`
  - `192 tests`
  - `BUILD SUCCESS`
- `git diff --check`
  - whitespace error なし

### 残課題

- 実 Cognito の `AdminCreateUser` を伴う招待メール送信、実ユーザーログイン、Hosted UI 確認はユーザーが手動で実施する
- `AdminCreateUser` 以外の Cognito API、Hosted UI 本格確認、PLATFORM / TENANT App Client 分離、Cognito Trigger、Pre Token Generation、初回ログイン時の WorkOps ユーザー自動作成は Phase 2 で扱う
- ユーザー無効化、Cognito 側 email 変更、Cognito 側 username 変更、権限セット定義そのものの CRUD は MVP 範囲外
