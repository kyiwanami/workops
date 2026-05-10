# M1-04 MyBatis 利用前提の追加

## 目的

MyBatis Mapper を追加できる前提を作る。

## 対応範囲

- Spring Initializr 相当で生成済みの MyBatis dependency を前提にする
- Mapper scan 設定を作成する
- Mapper XML の配置方針をリポジトリ上に作る
- 設定確認用の最小構成を作る

## 対応ファイル

- `apps/web/pom.xml`
- `apps/web/src/main/java/com/example/workops/common/config/MyBatisConfig.java`
- `apps/web/src/main/resources/application.yml`
- `apps/web/src/main/resources/mapper/.gitkeep`

## 除外範囲

- 業務 SQL
- 業務 Mapper
- 業務テーブル
- 検索
- 履歴取得
- Flyway 初期 DDL
- seed

## 完了条件

MyBatis を使うための依存関係と配置方針がリポジトリ上にできている。
業務 SQL や業務 Mapper を追加せず、M2 以降で Mapper を実装できる前提だけができている。

## 確認方法

- Maven build を実行する
- MyBatis 関連の設定ファイルと Mapper XML 配置先が存在することを確認する

## 実装時の記録

### 実装方針

- `mybatis-spring-boot-starter 4.0.1` は既存の依存関係を維持する
- `@Mapper` が付いた interface だけを MyBatis Mapper として検出する
- Mapper interface は `com.example.workops.<module>.mapper` 配下に置く方針にする
- Mapper XML は `classpath*:mapper/**/*.xml` で読み込む
- 業務 Mapper、業務 SQL、Mapper XML 本体は追加しない

### 変更ファイル

- `apps/web/src/main/java/com/example/workops/common/config/MyBatisConfig.java`
- `apps/web/src/main/resources/application.yml`
- `apps/web/src/main/resources/mapper/.gitkeep`
- `docs/implementation/mvp/agent-tasks/M1-04-mybatis-setup.md`

### 確認結果

- `cd apps/web && .\mvnw.cmd test` が成功した
- `cd apps/web && .\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=local"` で起動した
- `http://localhost:8080/` が HTTP 200 を返した
- `apps/web/src/main/resources/mapper/.gitkeep` が存在することを確認した

### 残課題

- なし
