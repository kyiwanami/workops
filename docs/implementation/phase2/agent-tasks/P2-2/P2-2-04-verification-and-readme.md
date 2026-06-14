# P2-2-04 P2-2 検証 / README 記録

## 目的

P2-2 の RDS、Secrets Manager、SSM Parameter、DB access host、Spring profile、Flyway / seed 分離の完了条件を横断確認し、README に構築、接続、確認、停止手順を整理する。

## ユーザー要求

- P2-2 の実装用 task Markdown を作成する
- 今回は検証と README 整理の task Markdown を作成する
- P2-2 で固めた設計判断を、実装エージェントが迷わない粒度で明記する
- AWS 実行確認が必要な項目は、エージェントによる機械確認とユーザーによるAWS確認を区別する
- Phase 2 の migration は `apps/web` 起動時 Flyway を維持し、migration 用 ECS Task 分離は Phase 2α で扱う
- 初期 task Markdown には `実装時の記録` テンプレ欄を入れない

## P2-2 共通方針

- MySQL はローカル、Testcontainers、RDS for MySQL のすべてで 8.4 LTS に固定する
- MySQL 8.0 と 9.x は採用しない
- RDS は Phase 2 期間中維持する
- DB access host は SSM port forwarding 専用とする
- DB credential は DB access host に持たせない
- Phase 2 の AWS dev Flyway migration は `apps/web` の Spring Boot 起動時実行を維持する
- Phase 2α で migration 用 ECS Task へ分離する
- Git commit は実行しない

## 案

- P2-2-01 から P2-2-03 の実装結果を横断確認する
- `infra/cdk/README.md` に P2-2 の構築、確認、DB接続、停止、P2-3引き継ぎ手順を追記する
- ルート `README.md` は、MySQL 8.4 LTS など既に必要な整合がある場合だけ更新する
- `docs/implementation/phase2/phases.md` はP2-2実装中に変更しない
- AWS deploy と RDS / EC2 作成はユーザー確認に分類する
- エージェントは CDK synth、assertions、Spring Boot設定、Flyway locations、README整合を確認する

## ADR

- P2-2 の AWS確認はユーザー確認に分類する。RDS、EC2、Secrets Manager、SSM Parameter は認証と課金対象リソース作成を伴うため
- P2-2 README には、設計意図、CDKで明示設定するもの、AWS/CDKデフォルトに任せるものを分けて記録する。後から判断理由を追えるようにするため
- DB access host の停止手順を README に書く。Phase 2期間中維持するが、使わない時のコストを下げる運用を明確にするため
- RDSの停止手順を README に書く。無料利用枠を前提にしつつ、ユーザーがコスト管理できるようにするため
- P2-3で `apps/web` 起動時 migration を確認する手順へ明確に引き継ぐ。P2-2単体ではAppRuntimeStackを作らないため

## 対応範囲

- P2-2-01 の `SecretStack` / `DataStack` 実装結果を確認する
- P2-2-02 の DB access host 実装結果を確認する
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
- README に DB access host の目的を書く
- README に SSM port forwarding 手順を書く
- README にローカルDBクライアント接続手順を書く
- README に `flyway_schema_history` と主要seed確認SQLを書く
- README に DB access host の手動停止手順を書く
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
- DB access host は Session Manager port forwarding 専用である
- DB access host は inboundなし、key pairなし、DB credentialなしである
- DB access host は `AmazonSSMManagedInstanceCore` のみを持つ
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
- Secrets Manager
  - `/workops/dev/db/master`
  - JSON key が `username` / `password`
- Systems Manager Parameter Store
  - `/workops/dev/spring/profile`
  - `/workops/dev/db/name`
  - `/workops/dev/db/port`
  - `/workops/dev/db/url`
- EC2
  - `workops-dev-db-access-host`
  - instance type が `t3.micro`
  - Amazon Linux 2023
  - public subnet 配置
  - key pair なし
  - inbound rule なし
- IAM
  - DB access host role に `AmazonSSMManagedInstanceCore` がある
- Security Group
  - `db-sg` が `app-sg` から 3306 を許可している
  - `db-sg` が `db-access-host-sg` から 3306 を許可している
- Outputs
  - RDS endpoint
  - RDS port
  - DB name
  - DB access host instance id
  - RDS master secret ARN
  - password、secret value、public IP が出ていない

## 対応ファイル

- `infra/cdk/README.md`
- `README.md`
- `docs/implementation/phase2/agent-tasks/P2-2/P2-2-04-verification-and-readme.md`

## 除外範囲

- CDK 実装ファイルの新規実装
- Spring Boot 実装ファイルの新規実装
- `docs/implementation/phase2/phases.md` 更新
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
- `infra/cdk/README.md` に P2-2 の構築、確認、DB接続、停止、P2-3引き継ぎ手順が記載されている
- README に、P2-2では migration 用 ECS Task を作らず、Phase 2α で分離することが記載されている

## 確認方法

エージェント確認:

- `npm run build`
- `npm test`
- `WORKOPS_STAGE=dev npm run cdk -- synth`
- `git diff --check`
- README のコマンド、Stack名、Parameter path、Secret名、DB access host名が実装と一致することを確認する

ユーザー確認:

- `WORKOPS_STAGE=dev npm run cdk -- deploy workops-dev-secret workops-dev-data`
- AWS Console で RDS、Secrets Manager、SSM Parameter Store、EC2、IAM、Security Group、Outputs を確認する
- Session Manager port forwarding を開始する
- ローカルDBクライアントで RDS へ接続する
- `flyway_schema_history` を確認する
- AWS dev seed の主要テーブルを確認する
- DB access host を手動停止する
- P2-3へ進む場合、`workops-dev-secret` と `workops-dev-data` は destroy しない
