# M8-02 Service テスト補強

## 目的

M4 から M7 で追加済みの Service テストを前提に、MVP の重要な未テスト分岐だけを補強し、会社境界、権限、状態遷移、削除済み除外、現行 reasonCode の期待動作を確認する。

## M8 共通方針

- M8 は新機能追加ではなく、MVP をポートフォリオとして再現・説明できる状態に仕上げるフェーズとする
- Service テストは M4 から M7 で入っている前提のため、M8 では全面的な作り直しをしない
- M8 の Service テストは不足分補強と体系整理に留める
- テスト名や配置は最低限の整理だけ行う
- UI の最終的な目視確認・操作感確認はユーザーが行う
- M7 の拒否理由細分化は M8 で新たな実装課題として広げない
- ログ精度を上げるためだけの追加 Mapper 参照や、拒否理由の過度な細分化は M8 では行わない
- `apps/web` を正として進める

## 対応範囲

- 既存 Service テストの不足分を確認する
- 重要な未テスト分岐を補強する
- 会社境界の不足確認を補強する
- 権限チェックの不足確認を補強する
- 申請状態遷移の不足確認を補強する
- 資産状態変更と論理削除の不足確認を補強する
- マスタ値の削除済み除外、削除済み表示、復活の不足確認を補強する
- 業務操作ログの現行 reasonCode が期待通り出ることを確認する
- `@PreAuthorize` による拒否は M7 方針どおり業務操作ログ対象外であることを確認する
- テスト名や配置に明らかな分かりにくさがある場合だけ、最小限で整理する

## 対応ファイル

- `apps/web/src/test/java/com/example/workops/request/service/RequestCommandServiceTests.java`
- `apps/web/src/test/java/com/example/workops/asset/service/AssetCommandServiceTests.java`
- `apps/web/src/test/java/com/example/workops/master/service/RequestTypeMasterServiceTests.java`
- `apps/web/src/test/java/com/example/workops/master/service/AssetCategoryMasterServiceTests.java`
- `apps/web/src/test/java/com/example/workops/**/*.java`
- `docs/implementation/mvp/agent-tasks/M8/M8-02-service-test-hardening.md`

## 除外範囲

- Service テスト全体の大規模リライト
- テスト設計の全面刷新
- 新機能追加
- Controller テストの大規模追加
- Mapper / Testcontainers 強化
- Flyway DDL 変更
- seed 仕様変更
- M7 の拒否理由細分化
- ログ精度を上げるためだけの追加 Mapper 参照
- UI の最終品質判断

## 完了条件

既存 Service テストを大きく作り直さず、MVP の重要な未テスト分岐が補強されている。
会社境界、権限、状態遷移、削除済み除外、現行 reasonCode の期待動作について、M8 で確認すべき不足分がテストで確認されている。

## 確認方法

- `cd apps/web && .\mvnw.cmd test`
- 申請 Service テストで重要な状態遷移と拒否分岐を確認する
- 資産 Service テストで会社境界、状態変更、論理削除、削除済み除外を確認する
- マスタ Service テストで会社境界、権限、削除済み除外、復活、コード重複を確認する
- 業務操作ログの現行 reasonCode が既存方針どおりであることを確認する
- `@PreAuthorize` による拒否を業務操作ログとして扱わないことを確認する

## 実装時の記録

実装後に、次をこのファイルへ追記する。

- 実装方針
- 変更ファイル
- 実装結果
- 確認結果
- 残課題
