# M2-04 local seed

## 目的

会社境界と権限確認の前提になる local seed を作る。

## 対応範囲

- 2社の seed を作成する
- 各社部署の seed を作成する
- 各社ユーザーの seed を作成する
- `TENANT_VIEWER` / `TENANT_EDITOR` / `TENANT_MANAGER` の権限セット seed を作成する
- ユーザー権限割当 seed を作成する
- 共通マスタ初期データ seed を作成する
- 汎用マスタ初期データ seed を作成する
- Flyway で schema 作成後に seed を適用できる構成にする

## 対応ファイル

- `apps/server/src/main/resources/db/migration/V2__insert_local_seed.sql`
- `docs/implementation/mvp/agent-tasks/M2-04-local-seed.md`

## 除外範囲

- サンプル申請
- サンプル資産
- 申請履歴サンプル
- 資産ステータス履歴サンプル
- 監査ログサンプル
- Mapper
- Service
- Controller
- Thymeleaf 画面

## 完了条件

2社の会社境界と `TENANT_VIEWER` / `TENANT_EDITOR` / `TENANT_MANAGER` の権限確認に使える seed が Flyway で投入される。

## 確認方法

- Flyway migration を実行する
- `companies` に2社のデータが存在することを確認する
- `users` に各社の確認用ユーザーが存在することを確認する
- `permission_sets` に3種類の権限セットが存在することを確認する
- `user_permission_sets` にユーザー権限割当が存在することを確認する
- 共通マスタと汎用マスタの初期データが存在することを確認する

## 実装時の記録

実装後に、次をこのファイルへ追記する。

- 実装方針
- 変更ファイル
- 確認結果
- 残課題
