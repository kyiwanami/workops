# P2-beta-03 DB Maven Flyway

## Summary

DB migration 実行方式を migration image / ECS RunTask から `db/pom.xml` の Maven Flyway 実行へ移す。
この task では `db/seed/dev` への名寄せ、`db/pom.xml` 追加、READMEの恒久手順更新までを扱う。

## ユーザー要件

- 旧 AWS dev 用 seed directory は `db/seed/dev` に統一する。
- 旧 directory 名は残さない。
- `db/` に Maven wrapper は置かない。
- apps/web / apps/api / apps/batch に migration 責務を持たせない。
- 恒久手順に影響する変更は同じ task 内で README / 開発手順へ反映する。

## 案

- `db/pom.xml` を独立した最小 Maven project として追加する。
- Spring Boot parent、root parent、apps/web parent は継承しない。
- Java source / compile 対象は持たない。
- Flyway Maven plugin と MySQL driver version を property で明示する。
- local / dev profile に Flyway locations を固定する。

## ADR

- RunMigration は `cd db; mvn -Pdev flyway:migrate` とする。
- local 確認も Spring Boot 設定に依存せず `WORKOPS_DB_*` を使う。
- `apps/web` に Flyway dependency、Flyway plugin、`spring.flyway.*` を追加しない。
- 既存 DB の migration履歴互換は考慮しない。

## Implementation

SQL配置は次に揃える。

```text
db/
  migration/
  seed/
    common/
    local/
    dev/
```

Flyway locations は次に固定する。

```text
local:
  filesystem:migration
  filesystem:seed/common
  filesystem:seed/local

dev:
  filesystem:migration
  filesystem:seed/common
  filesystem:seed/dev
```

DB接続値は実行時に次で渡す。

```text
-Dflyway.url=$WORKOPS_DB_URL
-Dflyway.user=$WORKOPS_DB_USERNAME
-Dflyway.password=$WORKOPS_DB_PASSWORD
```

## Verification

- `rg` で旧 directory 名が正本として残っていないことを確認する。
- `db/pom.xml` の Maven profile と locations を確認する。
- local DB が利用可能な場合は `mvn -Plocal flyway:info` 相当を実行する。
- apps/web に Flyway dependency / plugin / `spring.flyway.*` が追加されていないことを確認する。
- README / 開発手順に新しい `db/` Maven Flyway 手順があることを確認する。
- dev RunMigration の実行確認は `P2-beta-08` で行う。

## Assumptions

- `db/` Maven wrapper は作らない。
- local DBが利用できない場合は、コマンド手順と未実施理由を記録する。
