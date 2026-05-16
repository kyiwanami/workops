# M10-03 PLATFORM_ADMIN ユーザー作成 / 会社作成時初期 TENANT_MANAGER 連携

## 目的

PLATFORM_ADMIN が PLATFORM ユーザーと任意会社の TENANT ユーザーを作成できるようにする。
また、M9 で作成済みの会社作成フローへ初期 TENANT_MANAGER 作成を後付け連携し、最終仕様として会社作成時に初期 TENANT_MANAGER を必ず 1 人作る。

## M10 共通方針

- M10 の範囲は、初期 TENANT_MANAGER 作成、ユーザー作成、権限割当・変更、local Cognito Fake Bean、疑似 `cognito_sub` 発行・登録に限定する
- Cognito 本物 API 呼び出し、Cognito Hosted UI 本格確認、PLATFORM / TENANT App Client 分離は Phase 2 で扱う
- M9 では会社作成まで作成済みであり、M10 で会社作成フローへ初期 TENANT_MANAGER 作成を後付け連携する
- 最終仕様として、会社作成時に初期 TENANT_MANAGER を必ず 1 人作る
- ユーザー作成後に編集できる範囲は、表示名、email、所属部署、権限セットだけとする
- `actor_type` と `company_id` は作成後変更不可とする
- TENANT_MANAGER 数が 0 になる権限変更は、認可ではなく、更新後 count による業務バリデーションで拒否する
- 削除済み部署に所属するユーザーは、ユーザー一覧・詳細で `部署名（削除済み）` と表示する
- Git commit は実行しない

## 対応範囲

- PLATFORM_ADMIN による PLATFORM ユーザー作成を作成する
- PLATFORM_ADMIN による任意会社の TENANT ユーザー作成を作成する
- 会社作成画面へ初期 TENANT_MANAGER 入力欄を追加する
- 会社作成時の入力項目として、会社コード、会社名、初期 TENANT_MANAGER の username、表示名、email、所属部署を扱う
- 初期 TENANT_MANAGER の所属部署は任意とし、作成会社内の部署は会社作成時点では存在しないため未設定で登録する
- 会社作成トランザクション内で、会社作成、会社別初期マスタ投入、初期 TENANT_MANAGER 作成、TENANT_MANAGER 権限割当を完了する
- 会社作成時に初期 TENANT_MANAGER 作成が失敗した場合、会社作成全体を完了させない
- 作成ユーザーには M10-01 の Fake Bean が発行した疑似 `cognito_sub` を登録する
- PLATFORM ユーザーは `actor_type = 'PLATFORM'`、`company_id = NULL` で登録する
- TENANT ユーザーは `actor_type = 'TENANT'`、対象会社の `company_id` で登録する
- PLATFORM_ADMIN は PLATFORM ユーザーに `PLATFORM_ADMIN` を割り当てできる
- PLATFORM_ADMIN は TENANT ユーザーに `TENANT_VIEWER`、`TENANT_EDITOR`、`TENANT_MANAGER` を割り当てできる
- Controller と Service の public メソッドには `@PreAuthorize("hasAuthority('PLATFORM_ADMIN')")` を付ける

## 対応ファイル

- `apps/web/src/main/java/com/example/workops/admin/company/form/CompanyForm.java`
- `apps/web/src/main/java/com/example/workops/admin/company/service/CompanyAdminService.java`
- `apps/web/src/main/java/com/example/workops/admin/company/web/CompanyAdminController.java`
- `apps/web/src/main/java/com/example/workops/admin/user/form/UserForm.java`
- `apps/web/src/main/java/com/example/workops/admin/user/service/UserAdminService.java`
- `apps/web/src/main/java/com/example/workops/admin/user/mapper/UserAdminMapper.java`
- `apps/web/src/main/java/com/example/workops/admin/user/model/*.java`
- `apps/web/src/main/resources/mapper/admin/company/CompanyAdminMapper.xml`
- `apps/web/src/main/resources/mapper/admin/user/UserAdminMapper.xml`
- `apps/web/src/main/resources/templates/admin/company/company-form.html`
- `apps/web/src/main/resources/templates/admin/user/user-form.html`
- `apps/web/src/test/java/com/example/workops/admin/company/**/*.java`
- `apps/web/src/test/java/com/example/workops/admin/user/**/*.java`
- `docs/implementation/mvp/agent-tasks/M10/M10-03-platform-admin-user-create-and-initial-manager.md`

## 除外範囲

- TENANT_MANAGER による自社ユーザー作成
- ユーザー作成後の編集
- 権限変更単独画面
- ユーザー無効化
- Cognito 本物 API 呼び出し
- Cognito Hosted UI 本格確認
- PLATFORM / TENANT App Client 分離
- Cognito 側ユーザー属性変更

## 完了条件

PLATFORM_ADMIN が PLATFORM ユーザーを作成できる。
PLATFORM_ADMIN が任意会社の TENANT ユーザーを作成できる。
会社作成時に初期 TENANT_MANAGER が必ず 1 人作成される。
会社作成時に、会社、会社別初期マスタ、初期 TENANT_MANAGER、TENANT_MANAGER 権限割当が同一トランザクションで成立する。
会社作成時に初期 TENANT_MANAGER 作成が失敗した場合、会社作成全体が完了しない。
作成ユーザーには疑似 `cognito_sub` が登録される。

## 確認方法

- `cd apps/web && .\mvnw.cmd test`
- local profile の PLATFORM_ADMIN で会社作成画面が初期 TENANT_MANAGER 入力欄を表示することを確認する
- local profile の PLATFORM_ADMIN で会社を作成し、会社、会社別初期マスタ、初期 TENANT_MANAGER、TENANT_MANAGER 権限割当が登録されることを確認する
- local profile の PLATFORM_ADMIN で PLATFORM ユーザーを作成できることを確認する
- local profile の PLATFORM_ADMIN で任意会社の TENANT ユーザーを作成できることを確認する
- 作成ユーザーに疑似 `cognito_sub` が登録されることを確認する

## 実装時の記録

M10-03 実装時に、実装方針、変更ファイル、実装結果、確認結果、残課題を記録する。
