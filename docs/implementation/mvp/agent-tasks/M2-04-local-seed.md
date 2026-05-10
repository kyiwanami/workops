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

### 実装方針

- `V2__insert_local_seed.sql` として、Flyway で `V1` の後に local seed を投入する
- 2社は架空会社だが業態とデータ粒度を具体化し、適当な `サンプル株式会社A/B` は使わない
- 北浜精密機器株式会社は製造業として部署と資産カテゴリを多めにする
- 青葉ケアサービス株式会社は介護・生活支援サービスとして部署と資産カテゴリを少なめにする
- 汎用マスタ種別 `ASSET_CATEGORY` は `generic_master` に1件だけ投入する
- 会社ごとの資産カテゴリ差分は `generic_master_values.company_id` で表現する
- seed は明示 ID で投入する
- bootstrap seed のため `created_by` / `updated_by` は `NULL` にする
- 申請・資産・履歴・監査ログのサンプルデータは投入しない

### 変更ファイル

- `apps/server/src/main/resources/db/migration/V2__insert_local_seed.sql`
- `docs/implementation/mvp/agent-tasks/M2-04-local-seed.md`

### 確認結果

- `docker compose down -v` で local DB を初期化した
- `docker compose up -d workops-mysql` で MySQL を起動した
- `docker compose ps` で `workops-mysql` が `healthy` であることを確認した
- `cd apps/server && .\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=local"` で `V1` と `V2` の Flyway migration が成功することを確認した
- `flyway_schema_history` に `V2__insert_local_seed.sql` が success として登録されたことを確認した
- `companies` が2件であることを確認した
- `departments` が5件であることを確認した
- `users` が6件であることを確認した
- `permission_sets` が3件であることを確認した
- `user_permission_sets` が6件であることを確認した
- `common_master` が3件であることを確認した
- `common_master_values` が12件であることを確認した
- `generic_master` が1件であることを確認した
- `generic_master_values` が9件であることを確認した
- `ASSET_CATEGORY` の種別は1件で、会社別の値は `generic_master_values.company_id` で分かれていることを確認した
- `requests` が0件であることを確認した
- `assets` が0件であることを確認した
- `cd apps/server && .\mvnw.cmd test` が成功した

### 残課題

- なし
