# M10-02 ユーザー一覧 / 詳細

## 目的

PLATFORM_ADMIN と TENANT_MANAGER が、許可された範囲のユーザーを一覧・詳細で確認できるようにする。
削除済み部署に所属するユーザーは、ユーザー表示側で `部署名（削除済み）` と表示する。

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

- PLATFORM_ADMIN 向けユーザー一覧を作成する
- PLATFORM_ADMIN 向けユーザー詳細を作成する
- TENANT_MANAGER 向け自社ユーザー一覧を作成する
- TENANT_MANAGER 向け自社ユーザー詳細を作成する
- PLATFORM_ADMIN は PLATFORM ユーザーと全テナントの TENANT ユーザーを参照できる
- TENANT_MANAGER は現在ユーザーの `companyId` と一致する TENANT ユーザーだけを参照できる
- ユーザー一覧・詳細に `actor_type`、会社、所属部署、権限セットを表示する
- 削除済み部署に所属するユーザーは、部署名の末尾に `（削除済み）` を付けて表示する
- 部署未所属ユーザーは部署未設定として表示する
- Controller と Service の PLATFORM_ADMIN 向け public メソッドには `@PreAuthorize("hasAuthority('PLATFORM_ADMIN')")` を付ける
- Controller と Service の TENANT_MANAGER 向け public メソッドには `@PreAuthorize("hasAuthority('TENANT_MANAGER')")` を付ける

## 対応ファイル

- `apps/web/src/main/java/com/example/workops/admin/user/web/UserAdminController.java`
- `apps/web/src/main/java/com/example/workops/admin/user/service/UserAdminService.java`
- `apps/web/src/main/java/com/example/workops/admin/user/mapper/UserAdminMapper.java`
- `apps/web/src/main/java/com/example/workops/admin/user/model/*.java`
- `apps/web/src/main/resources/mapper/admin/user/UserAdminMapper.xml`
- `apps/web/src/main/resources/templates/admin/user/user-list.html`
- `apps/web/src/main/resources/templates/admin/user/user-detail.html`
- `apps/web/src/main/resources/templates/index.html`
- `apps/web/src/test/java/com/example/workops/admin/user/**/*.java`
- `docs/implementation/mvp/agent-tasks/M10/M10-02-user-list-detail.md`

## 除外範囲

- ユーザー作成
- ユーザー編集
- 権限割当・変更
- ユーザー無効化
- 会社一覧
- 会社詳細
- 会社編集
- 会社論理削除
- Cognito 本物 API 呼び出し

## 完了条件

PLATFORM_ADMIN が PLATFORM ユーザーと全テナントの TENANT ユーザーを一覧・詳細表示できる。
TENANT_MANAGER が自社 TENANT ユーザーだけを一覧・詳細表示できる。
TENANT_MANAGER は他社ユーザー詳細を参照できない。
削除済み部署に所属するユーザーは、一覧・詳細で `部署名（削除済み）` と表示される。

## 確認方法

- `cd apps/web && .\mvnw.cmd test`
- local profile の PLATFORM_ADMIN でユーザー一覧・詳細が HTTP 200 を返すことを確認する
- local profile の PLATFORM_ADMIN で PLATFORM ユーザーと複数会社の TENANT ユーザーを表示できることを確認する
- local profile の TENANT_MANAGER で自社ユーザー一覧・詳細が HTTP 200 を返すことを確認する
- local profile の TENANT_MANAGER が他社ユーザー詳細を参照できないことを確認する
- 削除済み部署に所属するユーザーの部署名が `部署名（削除済み）` と表示されることを確認する

## 実装時の記録

M10-02 実装時に、実装方針、変更ファイル、実装結果、確認結果、残課題を記録する。
