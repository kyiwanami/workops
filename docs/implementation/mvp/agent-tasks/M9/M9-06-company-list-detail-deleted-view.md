# M9-06 会社一覧 / 詳細 / 削除済み会社表示

## 目的

PLATFORM_ADMIN が会社を一覧・詳細で確認でき、削除済み会社も管理画面で区別して表示できるようにする。
M9 は会社作成だけではなく会社管理全体を含むため、M9-06 では既存の会社作成・部署管理に、会社一覧・会社詳細・削除済み会社表示を追加する。

## M9 共通方針

- M9 の範囲は、会社一覧・詳細・作成・編集・論理削除、削除済み会社の表示、部署一覧・作成・編集・論理削除、削除済み部署の表示、会社作成時の会社別 `generic_master_values` 初期投入までとする
- 会社削除は物理削除せず、`companies.is_deleted = TRUE` の論理削除として扱う
- 削除済み会社コードは再利用しない
- 会社を論理削除しても、部署、ユーザー、申請、資産、会社別マスタ値などの配下データは物理削除しない
- 配下データが存在していても、警告表示したうえで会社の論理削除は可能とする
- 削除済み会社は通常の新規作成・選択導線には出さず、管理画面では削除済みであることを区別して表示する
- 初期 TENANT_MANAGER 作成、ユーザー作成、権限割当・変更、local Cognito Fake Bean、疑似 `cognito_sub` 発行は M10 で扱う

## 対応範囲

- PLATFORM_ADMIN 専用の会社一覧画面を作成する
- PLATFORM_ADMIN 専用の会社詳細画面を作成する
- `GET /admin/companies` を追加する
- `GET /admin/companies/{companyId}` を追加する
- Controller の会社一覧・詳細メソッドには `@PreAuthorize("hasAuthority('PLATFORM_ADMIN')")` を付ける
- Service の会社一覧・詳細 public メソッドにも `@PreAuthorize("hasAuthority('PLATFORM_ADMIN')")` を付ける
- 通常の会社一覧では `companies.is_deleted = FALSE` の会社だけを表示する
- `showDeleted=true` の会社一覧では削除済み会社も表示する
- 削除済み会社は一覧と詳細で「削除済み」と区別して表示する
- 会社一覧には会社ID、会社コード、会社名、削除状態、部署数、ユーザー数、申請数、資産数、更新日時を表示する
- 会社詳細には会社ID、会社コード、会社名、削除状態、部署数、ユーザー数、申請数、資産数、会社別マスタ値数、作成日時、更新日時を表示する
- 会社詳細から対象会社の部署一覧 `/admin/companies/{companyId}/departments` へ遷移できるようにする
- ホームまたは既存ナビゲーションから会社一覧へ遷移できるようにする
- 会社登録完了後のリダイレクト先は、対象会社の部署一覧または会社詳細のどちらにするかを実装前に固定し、実装記録に残す
- 会社詳細では削除済み会社も表示できる
- 削除済み会社の部署一覧への導線は表示してよいが、後続の部署作成・編集操作では削除済み会社を対象外にする

## 対応ファイル

- `apps/web/src/main/java/com/example/workops/admin/company/web/CompanyAdminController.java`
- `apps/web/src/main/java/com/example/workops/admin/company/service/CompanyAdminService.java`
- `apps/web/src/main/java/com/example/workops/admin/company/mapper/CompanyAdminMapper.java`
- `apps/web/src/main/java/com/example/workops/admin/company/model/*.java`
- `apps/web/src/main/resources/mapper/admin/company/CompanyAdminMapper.xml`
- `apps/web/src/main/resources/templates/admin/company/company-list.html`
- `apps/web/src/main/resources/templates/admin/company/company-detail.html`
- `apps/web/src/main/resources/templates/index.html`
- `apps/web/src/test/java/com/example/workops/admin/company/**/*.java`
- `apps/web/src/test/java/com/example/workops/integration/CompanyAdminMapperIntegrationTests.java`
- `docs/implementation/mvp/agent-tasks/M9/M9-06-company-list-detail-deleted-view.md`

## 除外範囲

- 会社編集
- 会社論理削除
- 会社復活
- 会社物理削除
- 配下データ削除
- 削除済み会社コード再利用
- 部署作成・編集・論理削除の仕様変更
- 初期 TENANT_MANAGER 作成
- ユーザー作成
- 権限割当・変更
- Cognito 本物 API 呼び出し

## 完了条件

PLATFORM_ADMIN が会社一覧を表示できる。
通常一覧では未削除会社だけが表示される。
`showDeleted=true` では削除済み会社も表示され、削除済みであることを区別できる。
PLATFORM_ADMIN が会社詳細を表示できる。
会社詳細で会社基本情報、削除状態、配下データ件数、作成日時、更新日時を確認できる。
会社詳細から対象会社の部署一覧へ遷移できる。

## 確認方法

- `cd apps/web && .\mvnw.cmd test`
- local profile の PLATFORM_ADMIN で `GET /admin/companies` が HTTP 200 を返すことを確認する
- local profile の PLATFORM_ADMIN で `GET /admin/companies?showDeleted=true` が HTTP 200 を返すことを確認する
- 通常一覧に削除済み会社が表示されないことを確認する
- `showDeleted=true` の一覧に削除済み会社と削除済み badge が表示されることを確認する
- local profile の PLATFORM_ADMIN で `GET /admin/companies/{companyId}` が HTTP 200 を返すことを確認する
- 会社詳細で部署数、ユーザー数、申請数、資産数、会社別マスタ値数が表示されることを確認する
- local profile の TENANT_MANAGER が `GET /admin/companies` へアクセスできないことを確認する

## 実装時の記録

### 実装方針

- 会社作成処理、初期 TENANT_MANAGER 作成連携、作成後リダイレクトは変更しない
- `GET /admin/companies` と `GET /admin/companies/{companyId}` を追加し、Controller / Service の public メソッド双方で `PLATFORM_ADMIN` を要求する
- 通常一覧は未削除会社のみ、`showDeleted=true` の場合だけ削除済み会社も含める
- 会社詳細は削除済み会社も取得できる
- 配下データ件数は `company_id` 紐づきの全件数として数え、論理削除済み行も含める
- ホームの会社導線は `/admin/companies` に変更し、会社登録は会社一覧画面の「新規作成」から遷移する

### 変更ファイル

- `apps/web/src/main/java/com/example/workops/admin/company/form/CompanySearchForm.java`
- `apps/web/src/main/java/com/example/workops/admin/company/model/CompanyListItem.java`
- `apps/web/src/main/java/com/example/workops/admin/company/model/CompanyDetail.java`
- `apps/web/src/main/java/com/example/workops/admin/company/web/CompanyAdminController.java`
- `apps/web/src/main/java/com/example/workops/admin/company/service/CompanyAdminService.java`
- `apps/web/src/main/java/com/example/workops/admin/company/mapper/CompanyAdminMapper.java`
- `apps/web/src/main/resources/mapper/admin/company/CompanyAdminMapper.xml`
- `apps/web/src/main/resources/templates/admin/company/company-list.html`
- `apps/web/src/main/resources/templates/admin/company/company-detail.html`
- `apps/web/src/main/resources/templates/index.html`
- `apps/web/src/test/java/com/example/workops/admin/company/service/CompanyAdminServiceTests.java`
- `apps/web/src/test/java/com/example/workops/integration/CompanyAdminMapperIntegrationTests.java`
- `docs/implementation/mvp/agent-tasks/M9/M9-06-company-list-detail-deleted-view.md`

### 実装結果

- PLATFORM_ADMIN 用の会社一覧画面を追加した
- PLATFORM_ADMIN 用の会社詳細画面を追加した
- 一覧に会社ID、会社コード、会社名、削除状態、部署数、ユーザー数、申請数、資産数、更新日時を表示するようにした
- 詳細に会社ID、会社コード、会社名、削除状態、部署数、ユーザー数、申請数、資産数、会社別マスタ値数、作成日時、更新日時を表示するようにした
- 会社詳細から対象会社の部署一覧へ遷移できるようにした
- Service テストで PLATFORM_ADMIN の取得、TENANT_MANAGER の拒否、未存在詳細の `404` を確認した
- Mapper 統合テストで削除済み会社の一覧条件、削除済み会社詳細、配下データ件数を確認した

### 確認結果

- `cd apps/web && .\mvnw.cmd test`
  - `183 tests`
  - `BUILD SUCCESS`
- `git diff --check`
  - whitespace error なし
- local profile / `http://localhost:8081/admin/companies`
  - 会社一覧が表示されることを確認
- local profile / `http://localhost:8081/admin/companies?showDeleted=true`
  - 削除済み表示チェックが有効な一覧として表示されることを確認
- local profile / `http://localhost:8081/admin/companies/1`
  - 会社詳細と部署一覧リンクが表示されることを確認

### 残課題

- 会社編集、会社論理削除、削除時の警告表示は M9-07 で対応済み
