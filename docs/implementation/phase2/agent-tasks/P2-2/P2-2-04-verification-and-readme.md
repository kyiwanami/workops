# P2-2-04 P2-2 検証 / README 記録

## 目的

P2-2 の RDS、Secrets Manager、SSM Parameter、RDS Console CloudShell VPC DB 接続確認、Spring profile、Flyway / seed 分離の完了条件を横断確認し、README と Phase 2 文書の役割を分けて記録する。

## ユーザー要求

- P2-2-01 から P2-2-03 の実装結果を横断確認する
- RDS Console integrated CloudShell VPC DB 接続確認を P2-2 のユーザー確認として記録する
- Spring Boot 起動時 Flyway を Phase 2 では維持し、migration 用 ECS Task 分離は Phase 2α に残す
- local / AWS dev seed 分離を root README と Phase 2 文書に明記する
- P2-3 で確認すべき AWS dev Flyway / seed 確認手順を P2-2-04 task に明文化する
- エージェント確認とユーザー確認を混同しない
- `infra/cdk/README.md` は Phase 2 固有の判断記録や検証記録を置かず、CDK 操作用 README に限定する
- `infra/cdk/README.md` に `Related Documents` は置かない
- 既存データ移行と後方互換性は考慮しない

## 案

- `infra/cdk/README.md` は CDK project の概要、環境変数、build / test / synth / deploy / destroy、運用上の conventions に限定する
- root `README.md` の古い Flyway 表と `infra/cdk` の予約領域記述を現状に合わせる
- `docs/implementation/phase2/phases.md` は P2-2 の profile 別 Flyway seed 分割だけを補う
- P2-2-04 task に、確認済み項目、ユーザー確認済み項目、P2-3 へ残す項目を分けて記録する

## ADR

- P2-2-04 では CDK リソース、Spring Boot 実装、SQL seed 内容を変更しない。検証と文書整合が目的のため。
- P2-2 の AWS 実環境確認はユーザー確認として扱う。CloudShell VPC 起動、Secrets Manager からのパスワード取得、MySQL 接続、`SELECT 1;` はユーザー資格情報を伴うため。
- P2-2 では AWS dev RDS 上の Flyway 適用完了を完了条件にしない。`apps/web` AWS dev 起動は P2-3 の責務であり、P2-2-04 では P2-3 の確認 SQL を引き継ぐ。
- local / AWS dev seed 分離は Flyway locations で表現する。local と aws-dev の `V6__insert_users.sql` は同じ profile で同時に読まないため、同一 version とする。
- AWS dev RDS に古い `flyway_schema_history` がある場合は、既存データ移行を行わず、P2-3 前に空DBとして再作成またはリセットする。
- `infra/cdk/README.md` は CDK 操作に必要な恒久情報だけを置く。Phase 2 固有の判断、検証結果、CloudShell 手順、P2-3 引き継ぎ SQL は Phase 2 文書に置く。
- `infra/cdk/README.md` に関連文書リンク集は置かない。README から Phase 文書への誘導で役割境界を曖昧にしないため。

## 対応内容

- `infra/cdk/README.md`
  - Phase 2 固有の章、ADR、検証結果、CloudShell 詳細手順、P2-3 引き継ぎ SQL を削除した
  - CDK project の概要、環境変数、build / test / synth、stack 一覧、bootstrap、deploy、destroy、conventions に限定した
  - `Related Documents` は追加しない
- `README.md`
  - `infra/cdk` を Phase 2 AWS dev 基盤の CDK v2 プロジェクトとして記載した
  - Flyway / seed 表を `V1` から `V8` の現行構成へ更新した
  - local と AWS dev の `V6__insert_users.sql` が排他 locations であることを記載した
- `docs/implementation/phase2/phases.md`
  - P2-2 実装方針に `db/seed/common`、`db/seed/local`、`db/seed/aws-dev` の profile 別 seed 分割を追記した
  - local と AWS dev の `V6__insert_users.sql` を同一 version とする前提を追記した
- `docs/implementation/phase2/agent-tasks/P2-2/P2-2-04-verification-and-readme.md`
  - RDS Console CloudShell VPC の確認結果、P2-3 引き継ぎ SQL、主要 seed 件数期待値を保持した

## README から Phase 2 文書へ移した詳細

`infra/cdk/README.md` は CDK 操作用 README に限定するため、Phase 2 固有の詳細はこの task 文書で保持する。

### P2-1 で作るもの

- `FoundationStack`
  - VPC `workops-dev-vpc`
  - public / app / db subnet group
  - ALB / app / DB 用 Security Group
  - ECS Cluster `workops-dev-cluster`
- `RegistryStack`
  - ECR repository `workops-dev-web`
  - tagged image 最新2件保持、untagged image 1日後削除の lifecycle policy
- `LogsStack`
  - CloudWatch Logs log group `/workops/dev/web`
  - CloudWatch Logs log group `/workops/dev/migration`
- `ConfigStack`
  - P2-1 時点では空 Stack

### P2-2-01 で追加するもの

- `SecretStack`
  - P2-2-01 時点では将来の独立 secret 置き場として空 Stack
- `DataStack`
  - RDS for MySQL 8.4.9 `workops-dev-db`
  - DB Subnet Group `workops-dev-db-subnet-group`
  - RDS master secret `/workops/dev/db/master`
  - `app-sg` から `db-sg` への TCP 3306 inbound rule
- `ConfigStack`
  - `/workops/dev/spring/profile`
  - `/workops/dev/db/name`
  - `/workops/dev/db/port`
  - `/workops/dev/db/url`

### P2-2-01 で作らないもの

- Cognito
- ALB
- NAT Gateway
- ECS Service
- Fargate task
- Dockerfile
- Docker build / ECR push
- GitHub Actions workflow
- DB access host
- Flyway profile / seed 分割

### P2-2-02 で追加するもの

- `DataStack`
- RDS Console CloudShell Security Group `workops-dev-rds-console-cloudshell-sg`
- RDS Console CloudShell Security Group の self-reference TCP 3306 connection rule
- RDS への RDS Console CloudShell Security Group 追加付与

### P2-2-02 で作らないもの

- EC2 access host
- SSH key pair
- SSH inbound rule
- Elastic IP
- VPC Endpoint
- Session Manager port forwarding 前提
- CloudShell VPC environment の CDK 管理

### AWS Console 確認項目

CloudFormation:

- `workops-dev-foundation`
- `workops-dev-secret`
- `workops-dev-data`
- `workops-dev-config`
- `workops-dev-registry`
- `workops-dev-logs`

VPC:

- CIDR が `10.0.0.0/16`
- Name tag が `workops-dev-vpc`
- public / app / db 用の subnet が合計6つある
- NAT Gateway がない

Security Group:

- `workops-dev-alb-sg`
- `workops-dev-app-sg`
- `workops-dev-db-sg`
- `workops-dev-rds-console-cloudshell-sg`
- P2-2-01 後は `workops-dev-app-sg` から `workops-dev-db-sg` への TCP 3306 inbound rule がある
- P2-2-02 後は `workops-dev-rds-console-cloudshell-sg` が self-reference TCP 3306 egress / ingress を持つ
- `workops-dev-rds-console-cloudshell-sg` は RDS Console integrated CloudShell VPC 接続確認専用とし、他リソースへ流用しない

RDS:

- DB identifier が `workops-dev-db`
- Engine が MySQL 8.4.9
- Instance class が `db.t4g.micro`
- Storage が 20 GiB / gp2
- Single-AZ
- Public accessibility が false
- Backup retention が1日
- Deletion protection が false

Secrets Manager:

- `/workops/dev/db/master`
- secret JSON は `username` / `password` を持つ
- username は `workops_admin`

Systems Manager Parameter Store:

- `/workops/dev/spring/profile`
- `/workops/dev/db/name`
- `/workops/dev/db/port`
- `/workops/dev/db/url`

ECS:

- Cluster 名が `workops-dev-cluster`

ECR:

- repository 名が `workops-dev-web`
- lifecycle policy が設定されている
- stack削除時に repository 内の image を空にして削除できる

CloudWatch Logs:

- `/workops/dev/web`
- `/workops/dev/migration`
- retention が7日

CloudFormation Outputs:

- FoundationStack の VPC / subnet / Security Group / ECS Cluster 情報
- DataStack の RDS identifier / endpoint / port / DB name / subnet group / master secret ARN / RDS Console CloudShell Security Group id
- RegistryStack の repository 情報
- LogsStack の log group 名

### RDS Console CloudShell DB 接続確認手順

P2-2-02 の private RDS 接続確認は、RDS Console integrated CloudShell VPC で行う。
EC2 access host、SSM port forwarding、Session Manager Plugin は使わない。

AWS Console で RDS `workops-dev-db` を開き、接続方法から CloudShell を起動する。
CloudShell VPC 環境の Security Group に `workops-dev-rds-console-cloudshell-sg` が含まれていることを確認する。

CloudShell で TCP 到達性を確認する。

```bash
timeout 5 bash -c 'cat < /dev/null > /dev/tcp/<rds-endpoint>/3306' \
  && echo tcp-ok \
  || echo tcp-ng
```

`tcp-ok` の場合だけ、RDS Console が提示する `mysql` コマンドで接続する。

```bash
mysql -h <rds-endpoint> \
  -P 3306 \
  --ssl-ca /certs/global-bundle.pem \
  --ssl-verify-server-cert \
  -u workops_admin \
  -p
```

パスワードは Secrets Manager の `/workops/dev/db/master` からユーザーが取得し、`Enter password:` prompt に入力する。
入力中は文字も `*` も表示されない。

MySQL 接続後、最低限次を確認する。

```sql
SELECT 1;
```

確認後、CloudShell VPC 環境を削除する。
CloudShell 管理 ENI が Security Group を掴んでいる間は Security Group を削除できないため、確認用の一時環境は残さない。

### P2-3 への DB 確認引き継ぎ

P2-2 の CloudShell 確認で保証するのは、private RDS endpoint への TCP 3306 到達性と MySQL 接続、`SELECT 1;` までである。
P2-2 では AppRuntimeStack、ECS Service、Fargate task を作らないため、RDS 上の Flyway migration / AWS dev seed 適用完了は P2-3 の `apps/web` AWS dev 起動後に確認する。

P2-3 で `apps/web` を起動した後、RDS Console integrated CloudShell VPC から MySQL に接続し、最低限次を確認する。

```sql
SELECT installed_rank, version, description, success
FROM flyway_schema_history
ORDER BY installed_rank;
```

AWS dev seed の主要テーブルは、P2-2-03 で分離した `db/seed/aws-dev` の内容に合わせて確認する。
P2-2-02 の ADR は、P2-3 以降のアプリ実行経路を変更しない。
`apps/web`、migration、通常のDB接続は `app-sg` から `db-sg` への TCP 3306 許可を使う。

### 設計意図

- `WORKOPS_STAGE` は CDK context ではなく環境変数で渡す
- ローカルと GitHub Actions は同じ `WORKOPS_STAGE` 名で stage を渡す
- GitHub Actions の OIDC と `aws-region` 指定は P2-8 で扱う
- `ConfigStack` は P2-2-01 から非機密 SSM Parameter を管理する
- `SecretStack` は P2-2-01 では空 Stack とし、app 用 secret や migration 用 secret を作らない
- RDS master secret は RDS lifecycle に合わせるため `DataStack` に置く
- RDS Console integrated CloudShell VPC を DB 接続確認手段にし、EC2 access host は作らない
- CloudShell VPC environment は CDK で作らず、RDS Console 上のユーザー確認として扱う
- RDS Console CloudShell Security Group は self-reference TCP 3306 egress / ingress だけを持つ
- RDS Console CloudShell Security Group は RDS に追加付与し、既存 DB SG 本体には self-reference を追加しない
- NAT Gateway は追加しない。RDS Console CloudShell から private RDS endpoint への VPC 内 TCP 3306 接続には不要である
- Stack 間参照は同一 CDK app 内の props 参照で渡し、cross-stack reference は `weak` に固定する
- `weak` により、生成テンプレートは `Fn::ImportValue` ではなく `Fn::GetStackOutput` を使う
- LogsStack は `AppRuntimeStack` 削除後も Phase 2 期間中の調査ログを残すため独立 Stack にする
- ECR image は再生成可能な成果物として扱い、非空 repository を理由に `destroy --all` を失敗させない
- SSM Parameter Store Standard のパス規約は `/workops/{stage}/...` とし、秘匿値は入れない

### CDK デフォルトに任せるもの

- subnet CIDR の具体割り当て
- subnet の Name tag
- DNS support
- DNS hostnames
- Internet Gateway
- public route table
- subnet route table association

### 明示設定するもの

- VPC CIDR は `10.0.0.0/16`
- VPC は2AZ構成
- subnet group は `public`、`app`、`db`
- `public` subnet は `PUBLIC`
- `app` と `db` subnet は P2-1 時点で `PRIVATE_ISOLATED`
- NAT Gateway は `0`
- Security Group は `alb-sg`、`app-sg`、`db-sg`
- P2-2-01 では `app-sg` から `db-sg` への TCP 3306 inbound rule を追加する
- P2-2-02 では `rds-console-cloudshell-sg` を作り、self-reference TCP 3306 egress / ingress を追加する
- P2-2-02 では RDS に `db-sg` と `rds-console-cloudshell-sg` の両方を付与する
- P2-2-02 では EC2 access host、EC2 IAM Role、Instance Profile を作らない
- migration task は `app-sg` を使い、`migration-sg` は作らない
- CloudWatch Logs retention は7日
- CloudWatch Logs は `RemovalPolicy.DESTROY`
- ECR repository は `RemovalPolicy.DESTROY` と `emptyOnDelete: true`
- `AWS_REGION`、`retentionDays`、ECR repository 名は SSM に入れない
- DB username、DB password は SSM に入れない

## エージェント確認

- CDK build
  - 実行: `cd C:\git\workops\infra\cdk; npm run build`
  - 結果: 成功
- CDK test
  - 実行: `cd C:\git\workops\infra\cdk; npm test`
  - 結果: 成功、9 tests passed
- CDK synth
  - 実行: `cd C:\git\workops\infra\cdk; $env:WORKOPS_STAGE='dev'; npm run cdk -- synth`
  - 結果: 成功
- Web test
  - 実行: `cd C:\git\workops\apps\web; .\mvnw.cmd clean test`
  - 結果: 成功、193 tests passed
- CDK template 静的確認
  - `AWS::EC2::Instance`: 0
  - `AWS::IAM::InstanceProfile`: 0
  - `AmazonSSMManagedInstanceCore`: 0
  - `Fn::ImportValue`: 0
  - RDS DBInstance に既存 DB SG と RDS Console CloudShell SG が付与されることを確認
  - RDS Console CloudShell SG の self-reference TCP 3306 ingress / egress を確認
  - DataStack Outputs に password、secret value、public IP、EC2 instance id が出ていないことを確認
- Web / Flyway 静的確認
  - `WORKOPS_DB_HOST` / `WORKOPS_DB_PORT` / `WORKOPS_DB_NAME` が新設されていないことを確認
  - `mysql:8.0` / `mysql:9` が現行構成にないことを確認
  - `db/seed/aws-dev` に固定 UUID の `cognito_sub` がないことを確認
  - local active locations は `db/migration`、`db/seed/common`、`db/seed/local`
  - dev active locations は `db/migration`、`db/seed/common`、`db/seed/aws-dev`
  - local / dev とも active version は `V1` から `V8`
- ローカル起動確認
  - Docker `workops-mysql` が `healthy` であることを確認
  - local profile の Spring Boot が `http://localhost:8080/` で `200` を返すことを確認
  - ローカル起動ログで Flyway が `V1` から `V8` まで適用済みであることを確認
- 文書確認
  - `git diff --check`: 成功
  - root README に古い `V2__insert_local_seed.sql` から `V5__platform_admin_local_prerequisite.sql` の表が残っていないことを確認
  - P2-2 の採用手段として EC2 access host / SSM port forwarding を説明していないことを確認

## ユーザー確認

- P2-2-02 のユーザー確認済み結果
  - RDS Console integrated CloudShell VPC から `tcp-ok` を確認済み
  - RDS Console 提示の `mysql` コマンドで RDS に接続済み
  - MySQL 上で `SELECT 1;` を確認済み
  - CloudShell VPC environment は確認後に削除済み
- P2-2-04 では AWS deploy を新たに実行しない
- P2-2-04 では AWS dev RDS 上の Flyway / seed 適用完了を完了扱いにしない

## P2-3 引き継ぎ

P2-3 で `apps/web` を AWS dev 起動した後、RDS Console integrated CloudShell VPC から MySQL に接続して次を確認する。

```sql
SELECT installed_rank, version, description, success
FROM flyway_schema_history
ORDER BY installed_rank;

SELECT version, description, success
FROM flyway_schema_history
WHERE version IN ('1', '2', '3', '4', '5', '6', '7', '8')
ORDER BY installed_rank;

SELECT COUNT(*) AS aws_dev_cognito_sub_count
FROM users
WHERE cognito_sub IS NOT NULL;

SELECT COUNT(*) AS companies_count FROM companies;
SELECT COUNT(*) AS users_count FROM users;
SELECT COUNT(*) AS permission_sets_count FROM permission_sets;
SELECT COUNT(*) AS user_permission_sets_count FROM user_permission_sets;
SELECT COUNT(*) AS assets_count FROM assets;
SELECT COUNT(*) AS requests_count FROM requests;
```

期待値:

- `flyway_schema_history`: `V1` から `V8` が success
- `aws_dev_cognito_sub_count`: `0`
- `companies`: `2`
- `users`: `7`
- `permission_sets`: `4`
- `user_permission_sets`: `7`
- `assets`: `15`
- `requests`: `16`

## 除外範囲

- CDK 実装ファイルの新規実装
- Spring Boot 実装ファイルの新規実装
- Flyway SQL 内容の変更
- AWS deploy
- 既存RDSデータ移行
- 後方互換性対応
- migration 用 ECS Task
- AppRuntimeStack
- ECS Service
- ALB
- Cognito Hosted UI
- GitHub Actions workflow
- git commit

## 完了条件

- P2-2-01 から P2-2-03 の実装結果が P2-2 の設計判断と一致している
- `infra/cdk` の build、test、synth が成功している
- `apps/web` の `clean test` が成功している
- Spring profile と Flyway locations が P2-2 の方針どおりである
- RDS Console CloudShell VPC DB 接続確認のユーザー確認済み結果が記録されている
- `infra/cdk/README.md` が CDK 操作用 README に限定されている
- root `README.md` に現行の Flyway / seed 構成が記載されている
- P2-2-04 task に P2-2 の確認結果、RDS Console CloudShell VPC 接続確認、P2-3 引き継ぎ手順が記載されている
- P2-2 では migration 用 ECS Task を作らず、Phase 2α で分離することが記載されている
