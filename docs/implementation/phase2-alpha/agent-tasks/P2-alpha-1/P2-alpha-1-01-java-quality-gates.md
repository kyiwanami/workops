# P2-alpha-1-01 Java 品質ゲート

## 目的

AWS リソースの作成・変更を行わず、`apps/web` の Maven project に Java 側の品質ゲートを導入し、P2-alpha-1 の完了条件に含まれる Java 系コマンドをすべて exit 0 にする。

## ユーザー要求

- P2-alpha-1 の中身を詰め切ってからファイル分けする
- 既にコードまたは Notion で分かっていることは質問しない
- Spotless による整形差分は許容し、整形専用の単独コミットとして扱う
- JaCoCo が line 80% / branch 70% に届かない場合は、閾値を下げずにテスト追加で達成する
- Dependency-Check、SpotBugs、FindSecBugs などの違反は P2-alpha-1 の範囲で即対応し、何を検出し、何を直し、どのコマンドで確認したかを記録する

## 前提

- P2-alpha-0 が完了している
- Notion の Phase 2α-1 は、AWS リソース作成・変更なしでコードベースを品質ゲート pass 状態に揃える計画である
- `apps/web/pom.xml` には、Spotless、SpotBugs、FindSecBugs、OWASP Dependency-Check、JaCoCo、CycloneDX の設定が未導入である
- `apps/web` は Spring Boot 4 / Java 25 / Maven project として既に存在する
- 既存データ移行と後方互換性は考慮しない

## 案

`apps/web/pom.xml` に次の Maven plugin 設定を追加する。

| gate | 方針 |
| --- | --- |
| Spotless | Java のみ対象、google-java-format を使う |
| SpotBugs | `spotbugs:check` で FAIL mode にする |
| FindSecBugs | SpotBugs plugin として組み込む |
| OWASP Dependency-Check | CVSS 7 以上で fail する |
| JaCoCo | line 80%、branch 70% で `jacoco:check` を fail させる |
| CycloneDX | SBOM 生成成功を gate とする |

Spotless により大量整形差分が出た場合は、整形のみの単独コミットとして扱う。
意味のある実装修正、依存更新、テスト追加は整形コミットに混ぜない。

JaCoCo 未達は、業務仕様を変更せずに不足分のテストを追加して解消する。
JaCoCo 除外で閾値達成を狙わない。

Dependency-Check や SpotBugs / FindSecBugs の違反は、依存更新または実装修正で解消する。
suppression が必要な場合だけ、最小範囲、理由、根拠、再検討条件を task の実装記録に残す。

## ADR

- P2-alpha-1 では Java 品質ゲートを `apps/web` に閉じる。リポジトリ root に親 pom や multi-module 構成を作らないため。
- Spotless の整形差分は単独コミットに分ける。機械整形と意味のある実装修正をレビュー上分離するため。
- JaCoCo 閾値は下げない。Phase 2α の品質ゲートは初回から FAIL mode とし、閾値の up/down 調整を行わないため。
- suppression は最終手段にする。品質ゲートの導入を、警告の広い無効化で成立させないため。

## 対応範囲

- `apps/web/pom.xml`
- Spotless 設定
- SpotBugs / FindSecBugs 設定
- OWASP Dependency-Check 設定
- JaCoCo 設定
- CycloneDX 設定
- 品質ゲート違反の修正
- JaCoCo 未達を埋めるためのテスト追加
- Java 側の生成物除外設定
- 本 task ファイルの実装結果・確認結果追記

## 除外範囲

- P2-alpha-2 の PipelineStack / MigrationStack
- buildspec 作成
- Docker buildx cache 設計
- Flyway SQL 正本移動
- apps/web 起動時 Flyway 廃止
- AWS deploy
- CodePipeline 実走
- 品質ゲート閾値の引き下げ
- JaCoCo 除外による閾値達成
- 広い suppression / ignore

## 完了条件

- `apps/web/pom.xml` に Java 品質ゲートが設定されている
- Spotless は Java のみを対象にしている
- Spotless の整形差分は、意味のある修正と分離できる状態になっている
- SpotBugs / FindSecBugs が FAIL mode で実行できる
- Dependency-Check が CVSS 7 以上で fail する
- JaCoCo が line 80% / branch 70% を満たす
- CycloneDX SBOM を生成できる
- 品質ゲート用の生成物は git 管理対象になっていない

## 確認方法

```powershell
cd C:\git\workops\apps\web
.\mvnw.cmd spotless:check
.\mvnw.cmd spotbugs:check
.\mvnw.cmd org.owasp:dependency-check-maven:check
.\mvnw.cmd jacoco:check
.\mvnw.cmd verify
.\mvnw.cmd cyclonedx:makeAggregateBom
```

## 実装時の記録

### 実装結果

未実施。

### 確認結果

未実施。

### 残課題

未実施。
