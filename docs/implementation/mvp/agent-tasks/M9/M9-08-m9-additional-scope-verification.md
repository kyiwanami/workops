# M9-08 M9 追加スコープ確認 / 記録

## 目的

Notion 側で拡張された M9 の会社管理スコープが、M9-06 と M9-07 の追加実装込みで完了条件を満たしていることを確認し、リポジトリ側の記録を現行実装と一致させる。

## M9 共通方針

- M9 の範囲は、会社一覧・詳細・作成・編集・論理削除、削除済み会社の表示、部署一覧・作成・編集・論理削除、削除済み部署の表示、会社作成時の会社別 `generic_master_values` 初期投入までとする
- 会社削除は物理削除せず、`companies.is_deleted = TRUE` の論理削除として扱う
- 削除済み会社コードは再利用しない
- 会社を論理削除しても、部署、ユーザー、申請、資産、会社別マスタ値などの配下データは物理削除しない
- 配下データが存在していても、警告表示したうえで会社の論理削除は可能とする
- 削除済み会社は通常の新規作成・選択導線には出さず、管理画面では削除済みであることを区別して表示する
- 初期 TENANT_MANAGER 作成、ユーザー作成、権限割当・変更、local Cognito Fake Bean、疑似 `cognito_sub` 発行は M10 で扱う

## 対応範囲

- M9-06 と M9-07 の実装結果を確認する
- PLATFORM_ADMIN の会社一覧を確認する
- PLATFORM_ADMIN の会社詳細を確認する
- PLATFORM_ADMIN の会社編集を確認する
- PLATFORM_ADMIN の会社論理削除を確認する
- 削除済み会社の表示を確認する
- 削除済み会社が通常の新規作成・選択導線から除外されることを確認する
- 配下データが存在する会社でも、警告表示したうえで会社を論理削除できることを確認する
- 会社論理削除時に配下データが物理削除されないことを確認する
- 削除済み会社コードを再利用できないことを確認する
- 既存の部署管理が壊れていないことを確認する
- 既存の M10 ユーザー管理が、削除済み会社を通常の選択肢に出さないことを確認する
- M9-06 と M9-07 の agent task Markdown に `実装方針`、`変更ファイル`、`実装結果`、`確認結果`、`残課題` を記録する
- `docs/implementation/mvp/phases.md` と `docs/implementation/db/schema.md` が現行実装と矛盾しないことを確認する
- README に会社管理の主要導線を反映する
- M9-08 の実装時の記録に、M9 追加スコープ全体の確認結果を記録する

## 対応ファイル

- `README.md`
- `docs/implementation/db/schema.md`
- `docs/implementation/mvp/phases.md`
- `docs/implementation/mvp/agent-tasks/M9/M9-06-company-list-detail-deleted-view.md`
- `docs/implementation/mvp/agent-tasks/M9/M9-07-company-edit-delete.md`
- `docs/implementation/mvp/agent-tasks/M9/M9-08-m9-additional-scope-verification.md`
- `docs/implementation/**/*.md`

## 除外範囲

- 新規業務機能追加
- M10 実装変更
- 初期 TENANT_MANAGER 作成の仕様変更
- ユーザー作成の仕様変更
- 権限割当・変更の仕様変更
- 実 Cognito E2E
- Phase 2 設計変更

## 完了条件

M9 の会社管理スコープとして、会社一覧、会社詳細、会社作成、会社編集、会社論理削除、削除済み会社の表示が確認済みである。
M9 の部署管理スコープとして、部署一覧、部署作成、部署編集、部署論理削除、削除済み部署の表示が引き続き確認済みである。
会社論理削除が配下データを物理削除しないことを確認できている。
削除済み会社コードを再利用できないことを確認できている。
削除済み会社が通常の新規作成・選択導線から除外されることを確認できている。
M9-06 と M9-07 の実装記録が agent task Markdown に残っている。
README、DB 定義書、MVP phases、M9 agent task が現行実装と矛盾していない。

## 確認方法

- `cd apps/web && .\mvnw.cmd test`
- local profile の PLATFORM_ADMIN で `/admin/companies` が HTTP 200 を返すことを確認する
- local profile の PLATFORM_ADMIN で `/admin/companies?showDeleted=true` が HTTP 200 を返すことを確認する
- local profile の PLATFORM_ADMIN で `/admin/companies/{companyId}` が HTTP 200 を返すことを確認する
- local profile の PLATFORM_ADMIN で `/admin/companies/{companyId}/edit` が HTTP 200 を返すことを確認する
- local profile の PLATFORM_ADMIN で会社名を編集できることを確認する
- local profile の PLATFORM_ADMIN で配下データあり会社の削除確認警告を確認する
- local profile の PLATFORM_ADMIN で配下データあり会社を論理削除できることを確認する
- 削除済み会社が通常一覧から消え、`showDeleted=true` の一覧に表示されることを確認する
- 削除済み会社コードを再利用できないことを確認する
- local profile の PLATFORM_ADMIN で既存の部署管理 URL が HTTP 200 を返すことを確認する
- local profile の TENANT_MANAGER で自社部署管理 URL が HTTP 200 を返すことを確認する
- local profile の TENANT_MANAGER が会社管理 URL へアクセスできないことを確認する
- `git diff --check`

## 実装時の記録

### 実装方針

- M9-08 では新規業務機能を追加しない
- M9-06 / M9-07 の実装結果を横断確認し、M9 追加スコープの完了状態を記録する
- README、MVP phases、DB schema、M9 agent task が現行実装と矛盾しないことを確認する
- README は利用者が確認する主要導線だけを現行実装に合わせて更新する
- 実 Cognito E2E は行わない

### 変更ファイル

- `README.md`
- `docs/implementation/mvp/agent-tasks/M9/M9-06-company-list-detail-deleted-view.md`
- `docs/implementation/mvp/agent-tasks/M9/M9-08-m9-additional-scope-verification.md`

### 実装結果

- README の会社管理説明を、会社一覧、詳細、登録、編集、論理削除、削除済み表示、会社別初期マスタ投入に更新した
- README の主要 URL に `/admin/companies`、会社詳細、会社編集を追加した
- M9-06 の残課題を、M9-07 対応済みとして更新した
- `docs/implementation/mvp/phases.md` は M9 追加スコープ、会社論理削除方針、部署管理方針、M9-06 / M9-07 / M9-08 の追加 task と整合していることを確認した
- `docs/implementation/db/schema.md` は `companies`、`departments`、主要制約方針が現行 DB 実装と整合していることを確認した

### 確認結果

- M9-06 / M9-07 / M9-08 の Service テスト、Mapper 統合テスト、画面確認により以下を確認済み
  - PLATFORM_ADMIN が会社一覧・詳細を取得できる
  - PLATFORM_ADMIN が未削除会社を編集できる
  - PLATFORM_ADMIN が配下データありの会社を論理削除できる
  - 会社論理削除時に部署、ユーザー、申請、資産、会社別マスタ値の件数が減らない
  - 削除済み会社コードを再利用できない
  - TENANT_MANAGER は会社管理 Service を利用できない
- M9-06 / M9-07 の画面確認により以下を確認済み
  - `/admin/companies`
  - `/admin/companies?showDeleted=true`
  - `/admin/companies/{companyId}`
  - `/admin/companies/{companyId}/edit`
  - 会社詳細の削除確認モーダルに配下データ件数警告が表示される
- M9-08 の画面確認で、確認用会社 `M908_20260519213410` を作成し、会社名編集、論理削除、削除済み一覧表示、通常一覧からの除外、削除済みコード再利用拒否を確認した
- M9-08 の DB 確認で、確認用会社 `M908_20260519213410` が `is_deleted=true` になり、配下件数が `departments=0`、`users=1`、`requests=0`、`assets=0`、`generic_master_values=9` として残っていることを確認した
- `cd apps/web && .\mvnw.cmd test`
  - `192 tests`
  - `BUILD SUCCESS`
- `git diff --check`
  - whitespace error なし

### 残課題

- 会社復活、会社物理削除、削除済み会社配下データの自動無効化は MVP 範囲外
- M10 以降のユーザー作成・編集では、削除済み会社と削除済み部署を通常選択肢に出さない方針を維持する
