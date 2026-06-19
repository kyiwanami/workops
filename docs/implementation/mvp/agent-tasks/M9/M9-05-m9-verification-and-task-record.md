# M9-05 旧M9確認 / 記録

## 目的

旧M9スコープで実装した会社作成、テナント初期化、部署管理が当時の完了条件を満たしていることを確認し、各 agent task Markdown に実装結果を記録する。
Notion 側で M9 が会社管理全体へ拡張された後は、会社一覧・詳細・編集・論理削除・削除済み表示を M9 内の追加 task として扱う。

## M9 共通方針

- M9 の範囲は、会社一覧・詳細・作成・編集・論理削除、削除済み会社の表示、部署一覧・作成・編集・論理削除、削除済み部署の表示、会社作成時の会社別 `generic_master_values` 初期投入までとする
- 会社削除は物理削除せず、`companies.is_deleted = TRUE` の論理削除として扱う
- 削除済み会社コードは再利用しない
- 会社を論理削除しても、部署、ユーザー、申請、資産、会社別マスタ値などの配下データは物理削除しない
- 配下データが存在していても、警告表示したうえで会社の論理削除は可能とする
- 削除済み会社は通常の新規作成・選択導線には出さず、管理画面では削除済みであることを区別して表示する
- 初期 TENANT_MANAGER 作成、ユーザー作成、権限割当・変更、local Cognito Fake Bean、疑似 `cognito_sub` 発行は M10 で扱う
- PLATFORM_ADMIN 疑似ユーザーは M9 前提として扱う
- ユーザー情報は既存の `users` テーブルを使い、別のユーザーテーブルは作らない
- `users.actor_type = 'PLATFORM'` のユーザーは `company_id = NULL` とする
- `users.actor_type = 'TENANT'` のユーザーは `company_id` 必須とする
- 会社作成時の会社別 `generic_master_values` 初期投入は、会社作成に伴うテナント初期化処理として扱う
- `generic_master` 種別管理やマスタ管理画面の拡張は M9 で扱わない

## 対応範囲

- M9-01 から M9-04 の実装結果を確認する
- PLATFORM_ADMIN 疑似ユーザーの local profile 動作を確認する
- 会社作成と会社別 `generic_master_values` 初期投入を確認する
- PLATFORM_ADMIN の部署管理操作を確認する
- TENANT_MANAGER の自社部署管理操作を確認する
- TENANT_MANAGER が他社部署を管理できないことを確認する
- 既存の申請、資産、申請種別マスタ、資産分類マスタの主要画面が壊れていないことを確認する
- M9 で変更した DB 定義と `docs/implementation/db/schema.md` の整合を確認する
- Notion 側の M9 スコープ更新により不足している会社管理 task を明記する
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

## 完了条件

旧M9スコープの完了条件を満たしたことが、テスト結果、画面またはHTTP確認、DB確認、agent task Markdown の記録で説明できる。
Notion 側の M9 スコープ更新により追加実装が必要な会社管理 task が明確になっている。
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

## 追加 agent task 提案

Notion 側の M9 スコープ更新により、既存 M9-01 から M9-05 では会社管理の一部が不足している。
追加する場合は、既存 task の完了記録を変更せず、次の追加 task として扱う。

### M9-06 会社一覧 / 詳細 / 削除済み会社表示

- 会社一覧を追加する
- 会社詳細を追加する
- 削除済み会社を管理画面で区別して表示する
- 通常の会社選択・新規作成導線では削除済み会社を表示しない
- 会社ごとの部署一覧へ遷移できる導線を整理する

### M9-07 会社編集 / 会社論理削除

- 会社名を編集できるようにする
- 会社コードは変更不可とする
- 会社論理削除を追加する
- 配下データが存在する会社でも、警告表示したうえで論理削除できるようにする
- 会社論理削除時に部署、ユーザー、申請、資産、会社別マスタ値などの配下データを物理削除しない
- 削除済み会社コードは再利用不可のままとする

### M9-08 M9 追加スコープ確認 / 記録

- M9-06 と M9-07 の実装結果を確認する
- 会社一覧・詳細・編集・論理削除・削除済み表示が Notion 側の M9 完了条件を満たすことを確認する
- README、DB 定義書、agent task Markdown の記録を現行実装と一致させる

## 実装時の記録

### 実装方針

- M9-05 では新規業務機能を追加せず、M9-01 から M9-04 の実装結果を自動テスト、local HTTP 確認、DB 確認、agent task 記録で確認した
- local DB は reset せず、M9-05 確認用の会社コードと部署コードを一意にして追加した
- seed データを壊さないため、所属ユーザーあり部署の警告は seed 部署の削除確認モーダル表示で確認し、seed 部署の削除は実行しなかった
- M9-02 の会社作成後リダイレクト記録を、M9-03 実装後の最終状態に合わせて補正した
- README は M9 実装後の事実に合わせ、V5 migration、PLATFORM_ADMIN 疑似ユーザー、会社・部署管理導線を最小追記した

### 変更ファイル

- `README.md`
- `docs/implementation/mvp/agent-tasks/M9/M9-02-company-create-tenant-initialization.md`
- `docs/implementation/mvp/agent-tasks/M9/M9-05-m9-verification-and-task-record.md`

### 実装結果

- M9-01 から M9-04 の agent task Markdown を確認し、M9-02 の古い会社作成後遷移記録を補正した
- `docs/implementation/db/schema.md` は Flyway 正本の `V1__create_mvp_schema.sql` と `V5__platform_admin_local_prerequisite.sql` を参照しており、M9-05 で追加修正は不要だった
- `README.md` に `V5__platform_admin_local_prerequisite.sql`、local PLATFORM_ADMIN 疑似ユーザー、会社登録・部署管理の主要 URL を追記した
- M10 繰越項目は、初期 TENANT_MANAGER 作成、ユーザー作成、権限割当・変更、local Cognito Fake Bean、疑似 `cognito_sub` 発行として再確認した

### 確認結果

- `cd apps/web && .\mvnw.cmd test`
  - `Tests run: 130, Failures: 0, Errors: 0, Skipped: 0`
- PLATFORM_ADMIN local profile
  - `WORKOPS_LOCAL_COGNITO_SUB=00000000-0000-0000-0000-000000000000`
  - `/auth/claims`: 200
  - 会社作成確認用コード: `M9V_20260516201528`
  - 作成会社 ID: `6`
  - 会社作成後、作成会社の部署一覧へ遷移し、`会社を登録しました。` の Toast を表示
  - 作成会社の `generic_master_values`: `ASSET_CATEGORY` 6件、`REQUEST_TYPE` 3件
  - 部署作成確認用コード: `M9P_20260516201528`
  - 作成部署 ID: `12`
  - 作成会社の部署を作成、編集、論理削除できることを確認した
  - 通常一覧から削除済み部署が消え、`showDeleted=true` で削除済み部署と「削除済み」badge が表示されることを確認した
  - 削除済み部署コード `M9P_20260516201528` の再利用が `部署コードは既に使用されています。` のフォームエラーになることを確認した
  - 会社 ID `1` の部署一覧で、所属ユーザーあり部署の削除確認モーダルに `この部署には未削除ユーザーが` の警告が表示されることを確認した
- TENANT_MANAGER local profile
  - `WORKOPS_LOCAL_COGNITO_SUB=00000000-0000-0000-0000-000000000003`
  - `/auth/claims`: 200
  - `/requests`: 200
  - `/assets`: 200
  - `/masters/request-types`: 200
  - `/masters/asset-categories`: 200
  - 部署作成確認用コード: `M9T_20260516201640`
  - 作成部署 ID: `13`
  - 自社部署を作成、編集、論理削除できることを確認した
  - 通常一覧から削除済み部署が消え、`showDeleted=true` で削除済み部署と「削除済み」badge が表示されることを確認した
  - `/admin/companies/2/departments`: 403
  - 他社部署 ID `5` に対する `/departments/5/edit`: 404
  - 他社部署 ID `5` に対する `/departments/5/delete`: 404
- `git diff --check`
  - 空白エラーなし

### 残課題

- 初期 TENANT_MANAGER 作成、ユーザー作成、権限割当・変更、local Cognito Fake Bean、疑似 `cognito_sub` 発行は M10 で扱う
- 会社一覧、会社詳細、会社編集、会社論理削除、削除済み会社表示は、Notion 側の M9 スコープ更新により M9 内の追加 task として扱う
- 部署復活、物理削除、所属ユーザーの部署自動解除は M9 では扱わない
- ユーザー管理画面での削除済み部署名表示は M10-02 で対応済み

## M9 スコープ追従記録

### 追従方針

- Notion 側で M9 が会社管理全体へ拡張されたため、リポジトリ側の M9 共通方針を更新した
- 既存 M9-01 から M9-05 は旧スコープの実装記録として残し、会社管理の不足分は追加 task として扱う
- 追加 task ファイルはこの時点では作成せず、M9-06 から M9-08 の提案だけを記録する

### 追従対象

- `docs/implementation/mvp/phases.md`
- `docs/implementation/db/schema.md`
- `docs/implementation/mvp/agent-tasks/M9/*.md`
- `docs/implementation/mvp/agent-tasks/M10/*.md`
- `README.md`
