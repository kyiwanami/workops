# M9-07 会社編集 / 会社論理削除

## 目的

PLATFORM_ADMIN が会社名を編集し、配下データを残したまま会社を論理削除できるようにする。
会社削除は必須だが、物理削除ではなく `companies.is_deleted = TRUE` による論理削除として扱う。

## M9 共通方針

- M9 の範囲は、会社一覧・詳細・作成・編集・論理削除、削除済み会社の表示、部署一覧・作成・編集・論理削除、削除済み部署の表示、会社作成時の会社別 `generic_master_values` 初期投入までとする
- 会社削除は物理削除せず、`companies.is_deleted = TRUE` の論理削除として扱う
- 削除済み会社コードは再利用しない
- 会社を論理削除しても、部署、ユーザー、申請、資産、会社別マスタ値などの配下データは物理削除しない
- 配下データが存在していても、警告表示したうえで会社の論理削除は可能とする
- 削除済み会社は通常の新規作成・選択導線には出さず、管理画面では削除済みであることを区別して表示する
- 初期 TENANT_MANAGER 作成、ユーザー作成、権限割当・変更、local Cognito Fake Bean、疑似 `cognito_sub` 発行は M10 で扱う
- Git commit は実行しない

## 対応範囲

- PLATFORM_ADMIN 専用の会社編集画面を作成する
- PLATFORM_ADMIN 専用の会社論理削除操作を作成する
- `GET /admin/companies/{companyId}/edit` を追加する
- `POST /admin/companies/{companyId}/edit` を追加する
- `POST /admin/companies/{companyId}/delete` を追加する
- Controller の会社編集・論理削除メソッドには `@PreAuthorize("hasAuthority('PLATFORM_ADMIN')")` を付ける
- Service の会社編集・論理削除 public メソッドにも `@PreAuthorize("hasAuthority('PLATFORM_ADMIN')")` を付ける
- 会社編集では `companies.name` だけを更新する
- 会社コードは表示専用にし、POST 値として受け付けない
- 会社編集時は `updated_by` に現在ユーザーの `userId` を設定する
- 会社論理削除時は `companies.is_deleted = TRUE` と `updated_by` を更新する
- 会社論理削除は確認モーダル経由で POST する
- 削除確認モーダルには部署数、ユーザー数、申請数、資産数、会社別マスタ値数を警告情報として表示する
- 配下データ件数は警告表示に使い、削除可否の拒否条件にしない
- 削除済み会社は編集対象外とする
- 削除済み会社は再削除対象外とする
- 削除済み会社の復活操作は作らない
- 会社論理削除後は `showDeleted=true` の会社一覧または削除済み会社詳細へ遷移し、削除完了を Toast で表示する
- 会社作成・編集で削除済み会社コードを再利用できない状態を維持する
- 削除済み会社は会社作成、ユーザー作成、部署作成、資産作成、申請作成、マスタ値作成などの通常選択肢に表示しない

## 対応ファイル

- `apps/web/src/main/java/com/example/workops/admin/company/web/CompanyAdminController.java`
- `apps/web/src/main/java/com/example/workops/admin/company/form/CompanyForm.java`
- `apps/web/src/main/java/com/example/workops/admin/company/service/CompanyAdminService.java`
- `apps/web/src/main/java/com/example/workops/admin/company/mapper/CompanyAdminMapper.java`
- `apps/web/src/main/java/com/example/workops/admin/company/model/*.java`
- `apps/web/src/main/resources/mapper/admin/company/CompanyAdminMapper.xml`
- `apps/web/src/main/resources/templates/admin/company/company-list.html`
- `apps/web/src/main/resources/templates/admin/company/company-detail.html`
- `apps/web/src/main/resources/templates/admin/company/company-form.html`
- `apps/web/src/test/java/com/example/workops/admin/company/**/*.java`
- `apps/web/src/test/java/com/example/workops/integration/CompanyAdminMapperIntegrationTests.java`
- `docs/implementation/mvp/agent-tasks/M9/M9-07-company-edit-delete.md`

## 除外範囲

- 会社コード変更
- 会社復活
- 会社物理削除
- 配下データ削除
- 削除済み会社コード再利用
- 削除済み会社配下データの自動無効化
- 部署復活
- ユーザー無効化
- 初期 TENANT_MANAGER 作成
- ユーザー作成
- 権限割当・変更
- Cognito 本物 API 呼び出し

## 完了条件

PLATFORM_ADMIN が未削除会社の会社名を編集できる。
会社コード、`id`、作成日時、作成者は編集で変更されない。
PLATFORM_ADMIN が未削除会社を論理削除できる。
配下データが存在する会社でも、警告表示したうえで論理削除できる。
会社論理削除時に部署、ユーザー、申請、資産、会社別マスタ値は物理削除されない。
削除済み会社は通常一覧・通常選択肢から除外され、削除済み表示では区別して表示される。
削除済み会社コードは再利用できない。

## 確認方法

- `cd apps/web && .\mvnw.cmd test`
- local profile の PLATFORM_ADMIN で `GET /admin/companies/{companyId}/edit` が HTTP 200 を返すことを確認する
- local profile の PLATFORM_ADMIN で会社名を編集できることを確認する
- 会社コードが編集で変更されないことを確認する
- local profile の PLATFORM_ADMIN で会社削除確認モーダルに配下データ件数の警告が表示されることを確認する
- 配下データがある会社を論理削除できることを確認する
- 会社論理削除後、`companies.is_deleted = TRUE` になることを確認する
- 会社論理削除後、配下の部署、ユーザー、申請、資産、会社別マスタ値が物理削除されないことを確認する
- 削除済み会社が通常一覧に表示されないことを確認する
- 削除済み会社が `showDeleted=true` の一覧に表示されることを確認する
- 削除済み会社コードを再利用した会社作成がフォーム入力エラーになることを確認する
- local profile の TENANT_MANAGER が会社編集・論理削除 URL へアクセスできないことを確認する

## 実装時の記録

### 実装方針

- 会社編集は `companies.name` だけを更新する
- 会社コード、会社ID、作成日時、作成者、配下データは編集対象にしない
- 会社削除は `companies.is_deleted = TRUE` の論理削除だけを行う
- 会社論理削除時に部署、ユーザー、申請、資産、会社別マスタ値は更新・削除しない
- 編集・削除対象は未削除会社だけにし、未存在会社と削除済み会社は `404 NOT_FOUND` とする
- 削除後は `/admin/companies?showDeleted=true` に遷移し、削除済み会社を badge 付きで確認できるようにする
- M9-06 の一覧・詳細表示、会社作成後リダイレクト、初期 TENANT_MANAGER 作成連携、Cognito 境界は変更しない

### 変更ファイル

- `apps/web/src/main/java/com/example/workops/admin/company/form/CompanyEditForm.java`
- `apps/web/src/main/java/com/example/workops/admin/company/web/CompanyAdminController.java`
- `apps/web/src/main/java/com/example/workops/admin/company/service/CompanyAdminService.java`
- `apps/web/src/main/java/com/example/workops/admin/company/mapper/CompanyAdminMapper.java`
- `apps/web/src/main/resources/mapper/admin/company/CompanyAdminMapper.xml`
- `apps/web/src/main/resources/templates/admin/company/company-edit.html`
- `apps/web/src/main/resources/templates/admin/company/company-detail.html`
- `apps/web/src/test/java/com/example/workops/admin/company/service/CompanyAdminServiceTests.java`
- `apps/web/src/test/java/com/example/workops/integration/CompanyAdminMapperIntegrationTests.java`
- `docs/implementation/mvp/agent-tasks/M9/M9-07-company-edit-delete.md`

### 実装結果

- PLATFORM_ADMIN 用の会社編集画面を追加した
- PLATFORM_ADMIN 用の会社論理削除操作を追加した
- 会社詳細画面に、未削除会社だけ編集リンクと削除ボタンを表示するようにした
- 削除確認モーダルに部署数、ユーザー数、申請数、資産数、会社別マスタ値数を警告表示するようにした
- 会社編集・会社削除の成功ログ、未存在・削除済み対象の rejected log を追加した
- Service テストで編集、削除、削除済み対象の `404`、TENANT_MANAGER 拒否を確認した
- Mapper 統合テストで会社名更新、論理削除、配下データ件数維持、削除済み会社コード再利用不可を確認した

### 確認結果

- `cd apps/web && .\mvnw.cmd test`
  - `192 tests`
  - `BUILD SUCCESS`
- local profile / `http://localhost:8081/admin/companies/1/edit`
  - 会社編集画面が表示されることを確認
- local profile / `http://localhost:8081/admin/companies/1`
  - 編集リンクと削除ボタンが表示されることを確認
- 会社詳細の削除ボタン
  - 削除確認モーダルに配下データ件数警告が表示されることを確認

### 残課題

- 会社復活、会社物理削除、削除済み会社配下データの自動無効化はMVP範囲外
- M9-08で、M9追加スコープの取りこぼしがないかを横断確認する
