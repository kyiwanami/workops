# P2-2-01 RDS / Secret / Data Stack

## 目的

AWS dev 環境に WorkOps の業務 DB 正本となる RDS for MySQL 8.4 LTS と、DB 接続に必要な Secrets Manager / SSM Parameter を作成する。

## ユーザー要求

- P2-2 の実装用 task Markdown を作成する
- 今回は CDK 実装そのものは行わない
- `docs/implementation/phase2/agent-tasks/P2-2/` 配下に詳細 task Markdown を置く
- P2-2 で固めた設計判断を、実装エージェントが迷わない粒度で明記する
- MySQL はローカル、Testcontainers、RDS for MySQL のすべてで 8.4 LTS に固定する
- Phase 2 の migration は `apps/web` の Spring Boot 起動時 Flyway 実行を維持する
- migration 用 ECS Task 分離は Phase 2α で扱う
- 初期 task Markdown には `実装時の記録` テンプレ欄を入れない

## P2-2 共通方針

- CDK 実装言語は TypeScript とする
- package manager は npm とする
- CDK app は dotenv を使わない
- AWS profile と region は CDK CLI 実行環境に任せる
- `stage` は `WORKOPS_STAGE` 環境変数で指定する
- CDK code は `stage` の runtime 未指定チェック、空文字チェック、形式制限、バリデーションを行わない
- Phase 2 の確認例は `WORKOPS_STAGE=dev` とする
- Stack class 名に `dev` / `stg` / `prod` を入れない
- CloudFormation stackName は `workops-${stage}-...` 形式で生成する
- 共通タグは `Project=WorkOps`、`Environment={stage}`、`ManagedBy=CDK` とする
- Git commit は実行しない

## 案

- `SecretStack` を追加し、P2-2 時点では将来の独立 secret 置き場として空 Stack にする
- `DataStack` を追加し、RDS、DB Subnet Group、RDS master secret、SSM Parameter を配置する
- `FoundationStack` の VPC、db subnet、`app-sg`、`db-sg` を参照して RDS を構成する
- RDS master secret は `DataStack` で RDS と一体に管理する
- SSM Parameter は非機密値だけを置く
- DB access host は P2-2-02 で扱う
- Spring Boot / Flyway / seed 分離は P2-2-03 で扱う
- 検証と README は P2-2-04 で扱う

## ADR

- MySQL は 8.4 LTS 固定とする。RDS for MySQL で 9.x を選べず、8.0 は標準サポート終了と Extended Support 課金リスクがあるため
- ローカル、Testcontainers、RDS の MySQL メジャーバージョンを分けない。Flyway migration と SQL 挙動を揃えるため
- RDS は Phase 2 dev 用の最小構成にする。ポートフォリオ用 dev 環境であり、可用性より低コストと後始末を優先するため
- RDS master secret は `DataStack` に置く。RDS のライフサイクルと強く結びつく secret であり、DB 削除と整合させるため
- `SecretStack` は P2-2 で作成するが、P2-2 時点では app 用 secret や migration 用 secret を作らない。将来の独立 secret 置き場を先に確保するため
- DB username と DB password は Secrets Manager、DB URL や DB 名などの非機密設定は SSM Parameter Store Standard に分ける
- Phase 2 では migration 用 ECS Task を作らない。現行 `apps/web` の Spring Boot 起動時 Flyway 実行を維持し、Phase 2α で分離するため

## 対応範囲

- `SecretStack` class を追加する
- `SecretStack` の stackName は `workops-${stage}-secret` とする
- P2-2 時点の `SecretStack` に Secrets Manager secret 実体を作らない
- `DataStack` class を追加する
- `DataStack` の stackName は `workops-${stage}-data` とする
- `DataStack` は `FoundationStack` の VPC、db subnet、`app-sg`、`db-sg` を参照する
- DB Subnet Group を作成する
- DB Subnet Group 名は `workops-${stage}-db-subnet-group` とする
- RDS DB identifier は `workops-${stage}-db` とする
- DB 名は `workops` とする
- RDS engine は MySQL 8.4 LTS とする
- RDS instance class は `db.t4g.micro` とする
- allocated storage は 20 GiB とする
- storage type は `gp2` とする
- storage autoscaling は無効にする
- Single-AZ とする
- publicly accessible は false とする
- deletion protection は false とする
- backup retention は 1 日とする
- final snapshot は skip する
- encryption は有効にする
- encryption key は AWS managed key を使う
- maintenance window と backup window は明示しない
- RDS port は 3306 とする
- master username は `workops_admin` とする
- RDS master secret 名は `/workops/${stage}/db/master` とする
- secret JSON key は `username` / `password` とする
- `db-sg` は `app-sg` から 3306 inbound を許可する
- Security Group outbound は既存 P2-1 方針どおり許可のままにする
- SSM Parameter Store Standard に `/workops/${stage}/spring/profile` を作成し、値は `dev` とする
- SSM Parameter Store Standard に `/workops/${stage}/db/name` を作成し、値は `workops` とする
- SSM Parameter Store Standard に `/workops/${stage}/db/port` を作成し、値は `3306` とする
- SSM Parameter Store Standard に `/workops/${stage}/db/url` を作成する
- DB URL は `jdbc:mysql://{rds-endpoint}:3306/workops?useSSL=true&serverTimezone=Asia/Tokyo` とする
- CloudFormation Outputs に RDS instance identifier、endpoint address、port、DB 名、DB Subnet Group 名、RDS master secret ARN を出す
- CloudFormation Outputs に password、secret value、DB username 値そのものを出さない
- CDK assertions を追加する

## 対応ファイル

- `infra/cdk/bin/cdk.ts`
- `infra/cdk/lib/secret-stack.ts`
- `infra/cdk/lib/data-stack.ts`
- `infra/cdk/test/*.test.ts`
- `docs/implementation/phase2/agent-tasks/P2-2/P2-2-01-rds-secret-data-stacks.md`

## 除外範囲

- migration 用 ECS Task
- migration 専用 Security Group
- DB access host
- AppRuntimeStack
- ECS Service
- ALB
- Dockerfile
- ECR push
- Cognito User Pool
- Cognito user 作成
- `users.cognito_sub` の実 Cognito 突合
- RDS parameter group
- RDS option group
- Performance Insights
- Enhanced Monitoring
- RDS DB log export
- RDS Multi-AZ
- storage autoscaling
- read replica
- IAM DB authentication
- Kerberos authentication
- Secrets Manager への app 用 secret 作成
- Secrets Manager への migration 用 secret 作成
- SSM Parameter Store への password 保存
- SSM Parameter Store への username 保存
- SSM Parameter Store への region 保存
- SSM Parameter Store への retentionDays 保存
- SSM Parameter Store への ECR repository 名保存
- git commit

## 完了条件

- `SecretStack` が CDK app に追加されている
- `DataStack` が CDK app に追加されている
- `SecretStack` は P2-2 時点で secret 実体を作らない
- RDS for MySQL 8.4 LTS が `DataStack` に定義されている
- RDS が private db subnet に配置されている
- RDS が publicly accessible false になっている
- RDS が `db.t4g.micro`、20 GiB、`gp2`、Single-AZ、backup retention 1 日、deletion protection false になっている
- RDS master secret 名が `/workops/${stage}/db/master` になっている
- `db-sg` が `app-sg` から 3306 inbound を許可している
- 非機密設定だけが SSM Parameter Store Standard に作成されている
- password、secret value、DB username 値そのものが Output に出ていない
- CDK assertions が代表条件を検証している

## 確認方法

エージェント確認:

- `npm run build`
- `npm test`
- `WORKOPS_STAGE=dev npm run cdk -- synth`
- `git diff --check`

CDK assertions で確認する代表条件:

- `SecretStack` が存在する
- `DataStack` が存在する
- RDS engine が MySQL 8.4 LTS である
- RDS instance class が `db.t4g.micro` である
- allocated storage が 20 GiB である
- storage type が `gp2` である
- Multi-AZ が無効である
- publicly accessible が false である
- deletion protection が false である
- backup retention が 1 日である
- storage encryption が有効である
- DB Subnet Group が db subnet を使う
- `db-sg` inbound に `app-sg` から 3306 がある
- SSM Parameter が期待した path で作成される
- Secret value と password が Output に出ていない

ユーザー確認:

- `WORKOPS_STAGE=dev npm run cdk -- deploy workops-dev-secret workops-dev-data`
- CloudFormation で `workops-dev-secret` と `workops-dev-data` を確認する
- RDS Console で DB identifier、engine、instance class、storage、subnet、public accessibility、backup retention を確認する
- Secrets Manager で `/workops/dev/db/master` を確認する
- Systems Manager Parameter Store で `/workops/dev/...` の非機密値を確認する
