# M3-05 LocalCurrentUserProvider

## 目的

local profile で Cognito に依存せず、`SecurityCurrentUserProvider` と同じ `LoginUserContext` を返す local 代替を作る。

## 対応範囲

- `LocalCurrentUserProvider` を作成する
- local profile でのみ有効にする
- `TENANT_VIEWER` / `TENANT_EDITOR` / `TENANT_MANAGER` を切り替えられるようにする
- 北浜精密機器株式会社 / 青葉ケアサービス株式会社のユーザーを切り替えられるようにする
- M2 seed の users / permission_sets と整合する値を使う

## 対応ファイル

- `apps/server/src/main/java/com/example/workops/common/security/`
- `apps/server/src/main/resources/application-local.yml`
- `docs/implementation/mvp/agent-tasks/M3-05-local-current-user-provider.md`

## 除外範囲

- Cognito Hosted UI 接続
- SecurityCurrentUserProvider の追加実装
- Service 層認可チェック
- ユーザー管理画面
- 権限割当画面

## 完了条件

local profile で、会社と権限セットを切り替えて `LoginUserContext` を取得できる。

## 確認方法

- local profile 起動時に `TENANT_VIEWER` / `TENANT_EDITOR` / `TENANT_MANAGER` を切り替える
- `company_id = 1` / `company_id = 2` を切り替える
- `LoginUserContext` が Cognito 接続時と同じ形で返ることを確認する

## 実装時の記録

実装後に、次をこのファイルへ追記する。

- 実装方針
- 変更ファイル
- 確認結果
- 残課題
