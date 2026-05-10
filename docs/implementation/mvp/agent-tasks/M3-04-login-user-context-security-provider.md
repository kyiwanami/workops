# M3-04 LoginUserContext / SecurityCurrentUserProvider

## 目的

OIDC / Cognito claim とアプリ DB の users / permission_sets を突合する前提で、アプリ内部の認証済み利用者コンテキストを定義する。

## 対応範囲

- `LoginUserContext` を作成する
- `CurrentUserProvider` を作成する
- `SecurityCurrentUserProvider` を作成する
- Spring Security の認証情報から OIDC / Cognito claim を読む
- アプリ DB の users / permission_sets / user_permission_sets との突合方針を実装する
- 未登録ユーザー、削除済みユーザー、権限未設定ユーザーの扱いを実装する

## 対応ファイル

- `apps/server/src/main/java/com/example/workops/common/security/`
- `apps/server/src/main/java/com/example/workops/master/mapper/`
- `apps/server/src/main/resources/mapper/`
- `docs/implementation/mvp/agent-tasks/M3-04-login-user-context-security-provider.md`

## 除外範囲

- local 代替ユーザー
- Service 層認可チェック
- Cognito Trigger
- Pre Token Generation
- PLATFORM / TENANT App Client 分離
- ユーザー管理画面

## 完了条件

Cognito ログイン済みユーザーから `LoginUserContext` を取得でき、DB上の会社IDと権限セットを参照できる。

## 確認方法

- Cognito ログイン後に `LoginUserContext` の内容を確認する
- DBに存在しないユーザーは利用不可になることを確認する
- `company_id` と `permission_sets` がDBから解決されることを確認する

## 実装時の記録

実装後に、次をこのファイルへ追記する。

- 実装方針
- 変更ファイル
- 確認結果
- 残課題
