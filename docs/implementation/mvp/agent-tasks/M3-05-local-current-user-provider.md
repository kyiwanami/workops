# M3-05 LocalCurrentUserProvider

## 目的

local profile でCognito claimを疑似再現せず、ローカル確認用に選択したDBユーザーから `LoginUserContext` を作る。

## 対応範囲

- `LocalCurrentUserProvider` を作成する
- local profile でのみ有効にする
- local user key で `users` を取得する
- `users` / `user_permission_sets` / `permission_sets` からDB由来情報を取得する
- TENANT_VIEWER / TENANT_EDITOR / TENANT_MANAGER の確認ユーザーを切り替えられるようにする
- 北浜精密機器株式会社 / 青葉ケアサービス株式会社のユーザーを切り替えられるようにする

## 対応ファイル

- `apps/server/src/main/java/com/example/workops/common/security/`
- `apps/server/src/main/java/com/example/workops/master/mapper/`
- `apps/server/src/main/resources/mapper/master/UserAccountMapper.xml`
- `docs/implementation/mvp/agent-tasks/M3-05-local-current-user-provider.md`

## 除外範囲

- Cognito claim の疑似再現
- Cognito Hosted UI 接続
- Service 層認可チェック
- ユーザー管理画面
- 権限割当画面

## 完了条件

local profile で、選択したseedユーザーからDB由来の `LoginUserContext` を取得できる。

## 確認方法

- local profile 起動時に `TENANT_VIEWER` / `TENANT_EDITOR` / `TENANT_MANAGER` 相当のユーザーを切り替える
- `company_id = 1` / `company_id = 2` のユーザーを切り替える
- `LoginUserContext` がCognito接続時と同じ構造で返ることを確認する

## 実装時の記録

実装後に、次をこのファイルへ追記する。

- 実装方針
- 変更ファイル
- 確認結果
- 残課題
