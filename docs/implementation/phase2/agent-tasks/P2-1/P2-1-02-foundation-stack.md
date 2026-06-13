# P2-1-02 FoundationStack

## 目的

P2-2 以降の RDS、Cognito、ECS を載せるための AWS dev 基盤として、VPC、Subnet、Security Group、ECS Cluster を `FoundationStack` に実装する。

## ユーザー要求

- P2-1 の実装用 task Markdown を作成する
- 今回は CDK 実装そのものは行わない
- P2-1 で固めた FoundationStack の設計判断を漏らさず明記する
- 初期 task Markdown には `実装時の記録` テンプレ欄を入れない

## P2-1 共通方針

- CDK 実装言語は TypeScript とする
- package manager は npm とする
- CDK app は dotenv を使わない
- AWS profile と region は CDK CLI 実行環境に任せる
- `stage` は `WORKOPS_STAGE` 環境変数で指定する
- CDK code は `stage` の runtime 未指定チェック、空文字チェック、形式制限、バリデーションを行わない
- Stack class 名に環境名を入れない
- 共通タグは `Project=WorkOps`、`Environment={stage}`、`ManagedBy=CDK` とする
- 後続 Stack からの参照は CloudFormation Export や SSM ではなく、同一 CDK app 内の props 参照を使う
- CloudFormation Output は人間確認用に出す
- Git commit は実行しない

## 対応範囲

- `FoundationStack` class を作成する
- CloudFormation stackName を `workops-${stage}-foundation` にする
- VPC を作成する
- VPC CIDR は `10.0.0.0/16` とする
- VPC の Name tag は `workops-${stage}-vpc` とする
- `maxAzs` は `2` とする
- Subnet group は `public`、`app`、`db` の 3 種類にする
- `public` subnet は `PUBLIC` とする
- `app` subnet は P2-1 時点で `PRIVATE_ISOLATED` とする
- `db` subnet は `PRIVATE_ISOLATED` とする
- 各 subnet group の `cidrMask` は `/24` とする
- subnet CIDR は CDK の自動割り当てに任せる
- NAT Gateway は `0` にする
- Internet Gateway、public route table、subnet route table association は CDK VPC construct に任せる
- DNS support と DNS hostnames は CDK デフォルトに任せる
- RDS、ECS、Cognito、AWS SDK 利用のため、VPC 内 DNS 解決が有効である前提を README で説明できる状態にする
- Subnet の Name tag は CDK 自動に任せる
- ALB 用 Security Group を作成する
- App 用 Security Group を作成する
- DB 用 Security Group を作成する
- Security Group 名は `workops-${stage}-alb-sg`、`workops-${stage}-app-sg`、`workops-${stage}-db-sg` とする
- P2-1 では Security Group の inbound rule を追加しない
- Security Group の outbound は `allowAllOutbound: true` のままにする
- migration task 専用 Security Group は作らない
- migration task は P2-2 以降で `app-sg` を使う
- ECS Cluster を作成する
- ECS Cluster 名は `workops-${stage}-cluster` とする
- `FoundationStack` から VPC、subnet、Security Group、ECS Cluster を後続 Stack へ props 参照できるようにする

## 設計判断

- VPC CIDR は、外部ネットワーク接続要件が存在しないため `10.0.0.0/16` とする
- app subnet と db subnet は分ける。P2-3 で app subnet に NAT Gateway 経路を追加しても、db subnet を isolated のまま維持するため
- P2-1 時点では NAT Gateway を作らない。外向き通信は P2-3 の `EgressStack` で扱うため
- DNS と Internet Gateway は、CDK VPC construct のデフォルトが設計意図に合うため明示定義しない
- Security Group は用途別に作るが、通信ルールは対象リソースができる後続フェーズで追加する
- migration task は Web アプリと同じアプリ実行主体として RDS へ接続する短命タスクであり、Phase 2 dev では専用 SG を作らない

## 対応ファイル

- `infra/cdk/lib/foundation-stack.ts`
- `infra/cdk/bin/*.ts`
- `infra/cdk/test/*.test.ts`
- `docs/implementation/phase2/agent-tasks/P2-1/P2-1-02-foundation-stack.md`

## 除外範囲

- NAT Gateway
- ALB
- ECS Service
- Fargate task
- RDS
- Secrets Manager
- Cognito
- ECR repository
- CloudWatch Logs log group
- SSM Parameter
- VPC Endpoint
- Container Insights
- API 用 ECS Service
- app subnet への NAT route 追加
- `0.0.0.0/0 -> alb-sg 80/443`
- `alb-sg -> app-sg`
- `app-sg -> db-sg 3306`
- migration 専用 Security Group
- `infra/cdk/README.md` 更新
- AWS deploy
- git commit

## Outputs

`FoundationStack` は次の CloudFormation Output を出す。

- `vpcId`
- `publicSubnetIds`
- `appSubnetIds`
- `dbSubnetIds`
- `ecsClusterName`
- `albSecurityGroupId`
- `appSecurityGroupId`
- `dbSecurityGroupId`

Output は非機密値だけに限定し、Stack 間参照の正本にはしない。

## 完了条件

- `FoundationStack` を CDK app から生成できる
- `FoundationStack` の stackName が `workops-${stage}-foundation` になる
- VPC CIDR が `10.0.0.0/16` になる
- 2AZ 構成になる
- `public`、`app`、`db` の subnet group が作成される
- 合計 6 subnet が作成される
- NAT Gateway が作成されない
- ECS Cluster 名が `workops-${stage}-cluster` になる
- `alb-sg`、`app-sg`、`db-sg` の 3 Security Group が作成される
- P2-1 時点で Security Group inbound rule が追加されていない
- 後続 Stack が VPC、subnet、Security Group、ECS Cluster を props 参照できる

## 確認方法

エージェント確認:

- `cd infra/cdk && npm run build`
- `cd infra/cdk && npm test`
- `cd infra/cdk`
- `$env:WORKOPS_STAGE = "dev"`
- `npm run cdk -- synth`
- CDK assertions で VPC、6 subnet、NAT Gateway なし、ECS Cluster、3 Security Group、Output を確認する
- `git diff --check`

ユーザー確認:

- この task 単体では AWS deploy 確認を行わない
- AWS deploy と Console 確認は P2-1-04 で行う
