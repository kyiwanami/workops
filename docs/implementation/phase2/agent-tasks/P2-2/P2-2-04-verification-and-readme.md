# P2-2-04 P2-2 検証 / README 記録

## 目的

P2-2 の RDS、Secrets Manager、SSM Parameter、RDS Console CloudShell VPC DB接続確認、Spring profile、Flyway / seed 分離の完了条件を横断確認し、README に構築、接続、確認、削除手順を整理する。

## ユーザー要求

- P2-2 の実装用 task Markdown を作成する
- 今回は検証と README 整理の task Markdown を作成する
- P2-2 で固めた設計判断を、実装エージェントが迷わない粒度で明記する
- AWS 実行確認が必要な項目は、エージェントによる機械確認とユーザーによるAWS確認を区別する
- Phase 2 の migration は `apps/web` 起動時 Flyway を維持し、migration 用 ECS Task 分離は Phase 2α で扱う
- P2-2-02 の DB 接続確認手段は EC2 access host ではなく RDS Console integrated CloudShell VPC を使う
- 初期 task Markdown には `実装時の記録` テンプレ欄を入れない

## P2-2 共通方針

- MySQL はローカル、Testcontainers、RDS for MySQL のすべてで 8.4 LTS に固定する
- MySQL 8.0 と 9.x は採用しない
- RDS は Phase 2 期間中維持する
- DB接続確認は RDS Console integrated CloudShell VPC で行う
- DB credential は CloudShell 環境に保存しない
- Phase 2 の AWS dev Flyway migration は `apps/web` の Spring Boot 起動時実行を維持する
- Phase 2α で migration 用 ECS Task へ分離する
- Git commit は実行しない

## 案

- P2-2-01 から P2-2-03 の実装結果を横断確認する
- `infra/cdk/README.md` に P2-2 の構築、確認、DB接続、CloudShell環境削除、P2-3引き継ぎ手順を追記する
- ルート `README.md` は、MySQL 8.4 LTS など既に必要な整合がある場合だけ更新する
- `docs/implementation/phase2/phases.md` は、P2-2-02 の ADR が後続フェーズの前提へ影響する範囲だけ更新する
- AWS deploy、RDS Console CloudShell 起動、MySQL 接続確認、CloudShell 環境削除はユーザー確認に分類する
- エージェントは CDK synth、assertions、Spring Boot設定、Flyway locations、README整合を確認する

## ADR

- P2-2 の AWS確認はユーザー確認に分類する。RDS、Secrets Manager、SSM Parameter、RDS Console CloudShell 操作は認証と実環境操作を伴うため
- P2-2 README には、設計意図、CDKで明示設定するもの、AWS/CDKデフォルトに任せるものを分けて記録する。後から判断理由を追えるようにするため
- CloudShell VPC 環境の削除手順を README に書く。CloudShell 管理 ENI が一時的に Security Group を掴むため
- RDSの停止手順を README に書く。無料利用枠を前提にしつつ、ユーザーがコスト管理できるようにするため
- P2-3で `apps/web` 起動時 migration を確認する手順へ明確に引き継ぐ。P2-2単体ではAppRuntimeStackを作らないため

## 対応範囲

- P2-2-01 の `SecretStack` / `DataStack` 実装結果を確認する
- P2-2-02 の RDS Console CloudShell Security Group 実装結果を確認する
- P2-2-03 の Spring profile / Flyway locations / seed 分離を確認する
- `npm run build` を確認する
- `npm test` を確認する
- `WORKOPS_STAGE=dev npm run cdk -- synth` を確認する
- CDK assertions が P2-2 の代表条件を検証していることを確認する
- `git diff --check` を確認する
- `infra/cdk/README.md` を更新する
- README に P2-2 の deploy 手順を書く
- README に P2-2 で作るものと作らないものを書く
- README に RDS 構成値を書く
- README に Secrets Manager と SSM Parameter Store の path 規約を書く
- README に RDS Console CloudShell Security Group の目的を書く
- README に RDS Console CloudShell 起動手順を書く
- README に TCP 到達確認、MySQL 接続、`SELECT 1;` の確認手順を書く
- README に、P2-3 で `apps/web` 起動後に確認する `flyway_schema_history` と主要seed確認SQLを書く
- README に CloudShell VPC 環境の削除手順を書く
- README に RDS の停止手順を書く
- README に P2-3 で `apps/web` 起動時 migration を確認する引き継ぎを書く
- README に Phase 2α で migration 用 ECS Task へ分離する前提を書く
- README に「設計意図」「CDKで明示設定するもの」「AWS/CDKデフォルトに任せるもの」を分けて書く

## README に記録する設計判断

- MySQL は local / Testcontainers / RDS for MySQL で 8.4 LTS に固定する
- MySQL 8.0 / 9.x は使わない
- RDS は Single-AZ、`db.t4g.micro`、20 GiB、`gp2` とする
- RDS storage autoscaling は無効にする
- RDS deletion protection は無効にする
- RDS backup retention は 1 日とする
- final snapshot は skip する
- RDS encryption は有効にし、AWS managed key を使う
- RDS parameter group と option group は作らない
- Performance Insights と Enhanced Monitoring は P2-2 では入れない
- RDS DB log export は P2-7 で扱う
- DB username / password は Secrets Manager に置く
- DB URL / DB 名 / DB port / Spring profile は SSM Parameter Store Standard に置く
- SSM に password、username、region、retentionDays、ECR repository 名、Cognito issuer URI を置かない
- RDS Console integrated CloudShell VPC を DB 接続確認手段にする
- EC2 access host、SSM port forwarding、Session Manager Plugin は使わない
- RDS Console CloudShell Security Group は self-reference TCP 3306 egress / ingress だけを持つ
- CloudShell VPC environment は CDK 管理せず、ユーザー確認で作成と削除を行う
- Phase 2 の migration は `apps/web` 起動時Flywayを維持する
- migration 用 ECS Task は Phase 2α で作る

## AWS Console 確認項目

ユーザー確認では、AWS dev 環境で次を確認する。

- CloudFormation Stack
  - `workops-dev-secret`
  - `workops-dev-data`
- RDS
  - DB identifier が `workops-dev-db`
  - engine が MySQL 8.4 LTS
  - instance class が `db.t4g.micro`
  - storage が 20 GiB / `gp2`
  - Single-AZ
  - publicly accessible が false
  - deletion protection が false
  - backup retention が 1 日
  - encryption が有効
  - RDS に `db-sg` と `workops-dev-rds-console-cloudshell-sg` が付いている
- Secrets Manager
  - `/workops/dev/db/master`
  - JSON key が `username` / `password`
- Systems Manager Parameter Store
  - `/workops/dev/spring/profile`
  - `/workops/dev/db/name`
  - `/workops/dev/db/port`
  - `/workops/dev/db/url`
- Security Group
  - `db-sg` が `app-sg` から 3306 を許可している
  - `workops-dev-rds-console-cloudshell-sg` が self-reference TCP 3306 egress / ingress を持つ
- EC2
  - DB access host が存在しない
- IAM
  - DB access host role が存在しない
- Outputs
  - RDS endpoint
  - RDS port
  - DB name
  - RDS Console CloudShell Security Group id
  - RDS master secret ARN
  - password、secret value、public IP、EC2 instance id が出ていない

## 対応ファイル

- `infra/cdk/README.md`
- `README.md`
- `docs/implementation/phase2/phases.md`
- `docs/implementation/phase2/agent-tasks/P2-2/P2-2-04-verification-and-readme.md`

## 除外範囲

- CDK 実装ファイルの新規実装
- Spring Boot 実装ファイルの新規実装
- P2-2-02 の ADR と関係しない `docs/implementation/phase2/phases.md` 更新
- EC2 access host
- migration 用 ECS Task
- migration 専用 Security Group
- AppRuntimeStack
- ECS Service
- ALB
- Cognito Hosted UI
- GitHub Actions workflow
- CodePipeline
- CodeBuild
- CodeDeploy
- Blue/Green deploy
- Canary deploy
- 本番 DB 構成
- git commit

## 完了条件

- P2-2-01 から P2-2-03 の実装結果が P2-2 の設計判断と一致している
- `npm run build` が成功する
- `npm test` が成功する
- `WORKOPS_STAGE=dev npm run cdk -- synth` が成功する
- CDK assertions が P2-2 の代表条件を検証している
- Spring profile と Flyway locations が P2-2 の方針どおりである
- `git diff --check` が成功する
- `infra/cdk/README.md` に P2-2 の構築、確認、DB接続、CloudShell環境削除、P2-3引き継ぎ手順が記載されている
- README に、P2-2では migration 用 ECS Task を作らず、Phase 2α で分離することが記載されている

## 確認方法

エージェント確認:

- `npm run build`
- `npm test`
- `WORKOPS_STAGE=dev npm run cdk -- synth`
- `git diff --check`
- README のコマンド、Stack名、Parameter path、Secret名、RDS Console CloudShell Security Group名が実装と一致することを確認する

ユーザー確認:

- `WORKOPS_STAGE=dev npm run cdk -- deploy workops-dev-secret workops-dev-data`
- AWS Console で RDS、Secrets Manager、SSM Parameter Store、Security Group、Outputs を確認する
- RDS Console integrated CloudShell VPC を起動する
- CloudShell で TCP 到達確認を実行し `tcp-ok` を確認する
- RDS Console が提示する `mysql` コマンドで RDS へ接続する
- MySQL 上で `SELECT 1;` を確認する
- CloudShell VPC 環境を削除する
- P2-3 で `apps/web` を AWS dev 起動した後、CloudShell VPC から `flyway_schema_history` と AWS dev seed の主要テーブルを確認する
- P2-3へ進む場合、`workops-dev-secret` と `workops-dev-data` は destroy しない
