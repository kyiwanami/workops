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
