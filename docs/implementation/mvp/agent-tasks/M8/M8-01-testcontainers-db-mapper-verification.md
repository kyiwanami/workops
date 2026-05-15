# M8-01 Testcontainers DB / Mapper 検証

## 目的

MVP の DB / Mapper 実装について、Testcontainers MySQL 上で Flyway migration、seed、主要 Mapper、会社境界、DB 制約、論理削除済み参照先の扱いを確認する。

## M8 共通方針

- M8 は新機能追加ではなく、MVP をポートフォリオとして再現・説明できる状態に仕上げるフェーズとする
- M8 の中心は Testcontainers MySQL、Mapper 確認、Service テスト補強、README、ローカル起動手順、MVP 動作確認シナリオ、実装ドキュメント整理とする
- UI の最終的な目視確認・操作感確認はユーザーが行う
- コーディングエージェントは `mvnw test`、必要な自動テスト、起動確認が必要な場合の最小確認、README 手順の機械的整合確認までを行う
- Phase 2 の AWS デプロイ手順、Cognito 本格連携手順、ECS / RDS / CDK / GitHub Actions deploy 手順、PDF 出力手順、メール・バッチ手順、CloudWatch Logs 連携手順は M8 では扱わない
- M7 の拒否理由細分化は M8 で新たな実装課題として広げない
- DB 変更が必要な不備を見つけた場合は、DDL / migration を変更せず、発見した不備、影響範囲、変更が必要な DDL 案、変更しない場合の制約、M8 内で直すべきか後続 Phase に送るべきかを整理してユーザー確認を挟む
- `apps/web` を正として進める

## 対応範囲

- Testcontainers MySQL を使った DB / Mapper 検証テストを追加または整理する
- Flyway migration が Testcontainers MySQL 上で通ることを確認する
- seed 前提で最低限のデータを扱えることを確認する
- 主要 Mapper の検索を確認する
- 主要 Mapper の更新を確認する
- `company_id` による会社境界を確認する
- 他社データを取得できないことを確認する
- 他社データを更新できないことを確認する
- NOT NULL 制約を確認する
- 外部キー制約を確認する
- 一意制約を確認する
- `generic_master_id + company_id + code` の一意性を確認する
- 削除済みコード再利用禁止を確認する
- 論理削除済み資産を参照していても親申請を表示できることを確認する
- 論理削除済みマスタ値を参照していても親データを表示できることを確認する
- 新規作成・編集用の選択肢では削除済み資産や削除済みマスタ値を除外することを確認する

## 優先度

1. Flyway migration が Testcontainers MySQL 上で通ること、seed 前提で最低限のデータが扱えること
2. Mapper の主要検索・更新、`company_id` による会社境界、他社データを取得・更新できないこと
3. NOT NULL、外部キー、一意制約、`generic_master_id + company_id + code` の一意性、削除済みコード再利用禁止
4. 論理削除済み参照先の扱い、新規作成・編集用選択肢での削除済み除外

## 対応ファイル

- `apps/web/pom.xml`
- `apps/web/src/test/java/com/example/workops/**`
- `apps/web/src/main/resources/db/migration/V1__create_mvp_schema.sql`
- `apps/web/src/main/resources/db/migration/V2__insert_local_seed.sql`
- `apps/web/src/main/resources/mapper/**/*.xml`
- `docs/implementation/mvp/agent-tasks/M8/M8-01-testcontainers-db-mapper-verification.md`

## 除外範囲

- 新機能追加
- Flyway DDL 変更
- seed 仕様変更
- DB 境界変更
- Mapper SQL の大規模リライト
- Service テストの全面刷新
- M7 の拒否理由細分化
- ログ精度を上げるためだけの追加 Mapper 参照
- README 更新
- 実装ドキュメント整理
- UI の最終品質判断

## 完了条件

Testcontainers MySQL 上で Flyway migration と seed 前提データの最小確認が通る。
主要 Mapper について、検索・更新、会社境界、他社データの取得・更新不可、主要 DB 制約、削除済みコード再利用禁止、論理削除済み参照先の表示継続、選択肢での削除済み除外を確認できている。

## 確認方法

- `cd apps/web && .\mvnw.cmd test`
- Testcontainers MySQL 上で Flyway migration が成功することを確認する
- seed 前提の 2 社データ、権限セット、共通マスタ、汎用マスタの最低限データを確認する
- Mapper テストで所属会社内のデータだけ取得・更新できることを確認する
- Mapper テストで他社データを取得・更新できないことを確認する
- DB 制約テストで NOT NULL、外部キー、一意制約、削除済みコード再利用禁止を確認する
- 論理削除済み参照先を保持する親データの表示用取得を確認する
- 新規作成・編集用選択肢が削除済み参照先を除外することを確認する

## 実装時の記録

実装後に、次をこのファイルへ追記する。

- 実装方針
- 変更ファイル
- 実装結果
- 確認結果
- 残課題

## 実装時の記録 2026-05-15

### 実装方針

- `@SpringBootTest`、`@ActiveProfiles("local")`、`@Testcontainers` を使う Testcontainers MySQL 統合テスト基盤を追加した。
- Testcontainers の MySQL image は `compose.yaml` と同じ `mysql:9.7.0` を使った。
- `application-local.yml` は追加せず、`@DynamicPropertySource` で datasource を Testcontainers の接続先へ差し替えた。
- Spring TestContext の context cache と Testcontainers の停止タイミングがずれないよう、JVM 内で同一 MySQL コンテナを明示起動して再利用する構成にした。
- 各統合テストは `@Transactional` で実行し、テスト内で追加・更新したデータをロールバックする。
- Flyway migration / seed / Mapper / DB 制約の確認に限定し、DDL、seed、Mapper XML、README は変更しなかった。

### 変更ファイル

- `apps/web/src/test/java/com/example/workops/integration/MapperIntegrationTestBase.java`
- `apps/web/src/test/java/com/example/workops/integration/SeedMigrationIntegrationTests.java`
- `apps/web/src/test/java/com/example/workops/integration/RequestMapperIntegrationTests.java`
- `apps/web/src/test/java/com/example/workops/integration/AssetMapperIntegrationTests.java`
- `apps/web/src/test/java/com/example/workops/integration/BusinessMasterMapperIntegrationTests.java`
- `apps/web/src/test/java/com/example/workops/integration/DatabaseConstraintIntegrationTests.java`
- `docs/implementation/mvp/agent-tasks/M8/M8-01-testcontainers-db-mapper-verification.md`

### 実装結果

- Testcontainers MySQL 上で Flyway `V1` から `V4` までが適用され、MVP schema と local / sample seed が成立することを確認する統合テストを追加した。
- 2社、6ユーザー、3権限、`REQUEST_STATUS` / `ASSET_STATUS`、`ASSET_CATEGORY` / `REQUEST_TYPE` の seed 確認を追加した。
- `UserAccountMapper` で local 疑似ユーザー用 `cognito_sub` から `TENANT_MANAGER` ユーザーと権限を取得できることを確認した。
- `RequestMapper` で申請作成、一覧、詳細、提出、会社境界、他社データ更新不可、削除済み資産参照、削除済み申請種別参照、選択肢での削除済み除外を確認した。
- `AssetMapper` で資産作成、一覧、検索、詳細、ステータス更新、論理削除、会社境界、他社データ更新不可、削除済み資産分類参照、選択肢での削除済み除外を確認した。
- `RequestTypeMasterMapper` / `AssetCategoryMasterMapper` で登録、編集、論理削除、削除済み表示、復活、会社境界、削除済みコードの存在判定を確認した。
- `JdbcTemplate` で NOT NULL、外部キー、一意制約、`generic_master_id + company_id + code` の一意性、削除済みコード再利用禁止を確認した。

### 確認結果

- `cd apps/web && .\mvnw.cmd test` を実行し、93 tests / failures 0 / errors 0 を確認した。
- 初回実行では `@Container` 管理により最初の統合テストクラス終了後に MySQL コンテナが停止し、Spring context cache が停止済み JDBC URL を再利用して失敗した。
- 上記はテスト基盤の起動方式を JVM 内の明示起動コンテナ再利用へ変更して解消した。
- Flyway は MySQL 9.7 に対して「Flyway の最新検証済み MySQL は 8.1」という WARN を出すが、既存の WorkOps 方針どおり MySQL 9.7.0 を使い、migration とテストは成功した。

### 残課題

- なし。
