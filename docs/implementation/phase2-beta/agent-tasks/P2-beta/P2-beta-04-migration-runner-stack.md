# P2-beta-04 MigrationRunnerStack

## Summary

既存の `MigrationStack` / migration image / ECS RunTask方式を廃止し、`MigrationRunnerStack` の VPC attached CodeBuild で Maven Flyway を実行する基盤へ置き換える。

## ユーザー要件

- RunMigration CodeBuild は Docker を使わない。
- RunMigration CodeBuild は `cd db; mvn -Pdev flyway:migrate` を実行する。
- Pipeline は RDS start / stop 運用に関心を持たない。
- RDSが停止中なら RunMigration は失敗してよい。

## 案

- `MigrationRunnerStack` は RunMigration CodeBuild project と IAM だけを所有する。
- migration実行そのものは Pipeline stage/action が行う。
- Stack deployだけでは migration を実行しない。
- `MigrationRunnerStack` はライフサイクル分類上は撤去分類とする。

## ADR

- CodeBuild は VPC mode で app private subnet に配置する。
- Security Group は `migration-sg` を使う。
- RDS への接続は `db-sg` ingress from `migration-sg`:3306 を使う。
- CodeArtifact、STS、Secrets Manager、CloudWatch Logs への外向き通信は NAT 経由にする。
- Interface Endpoint は作らない。

## Implementation

- RunMigration CodeBuild は次を持つ。
  - Source artifact 参照
  - VPC config
  - app private subnet
  - `migration-sg`
  - `privileged: false`
  - `WORKOPS_DB_URL` from SSM `/workops/${stage}/db/url`
  - `WORKOPS_DB_USERNAME` / `WORKOPS_DB_PASSWORD` from Secrets Manager `/workops/${stage}/db/master`
  - CodeArtifact Maven 設定 helper 呼び出し
  - `cd db; mvn -Pdev flyway:migrate`
- 成功判定は CodeBuild build status に一本化する。
- ECS RunTask exit code 解釈、taskArn追跡、migration image tag は削除する。

## Verification

- CDK build/test/synth を実行する。
- Templateで RunMigration CodeBuild project の VPC config、environment、IAM、`privileged: false` を確認する。
- `MigrationStack` / ECS RunTask / migration image が正本実装から消えていることを確認する。
- RunMigration失敗時のトラブルシュートとしてRDS状態確認コマンドを文書化する。
- 実 RunMigration は `P2-beta-08` で確認する。

## Assumptions

- RDS起動/停止はPipeline責務外。
- RDS停止中のRunMigration失敗は正常な失敗原因候補として扱う。

