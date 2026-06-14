# P2-2-02 DB Access Host / SSM Port Forwarding

## 目的

ローカル PC から private RDS を確認するため、Session Manager port forwarding 専用の DB access host を AWS dev 環境に作成する。

## ユーザー要求

- P2-2 の実装用 task Markdown を作成する
- 今回は CDK 実装そのものは行わない
- RDS が private 配置でも、少なくともどこかから DB 内部を確認できる仕組みを用意する
- 踏み台用途は AWS Systems Manager Session Manager の port forwarding を使う
- EC2 instance type は `t3.micro` とする
- DB access host に DB credential を持たせない
- ローカル DB クライアントは A5:SQL Mk-2 など任意とし、特定製品依存の実装にしない
- 初期 task Markdown には `実装時の記録` テンプレ欄を入れない

## P2-2 共通方針

- CDK 実装言語は TypeScript とする
- package manager は npm とする
- CDK app は dotenv を使わない
- AWS profile と region は CDK CLI 実行環境に任せる
- `stage` は `WORKOPS_STAGE` 環境変数で指定する
- Phase 2 の確認例は `WORKOPS_STAGE=dev` とする
- Git commit は実行しない

## 案

- `DataStack` に DB access host を追加する
- DB access host は SSM port forwarding 専用とし、通常のSSH踏み台にしない
- DB access host は public subnet に置く
- DB access host は inbound rule なし、key pair なしにする
- DB access host には `AmazonSSMManagedInstanceCore` のみを付与する
- `db-access-host-sg` から `db-sg` へ 3306 inbound を許可する
- DB credential は Secrets Manager からユーザーが取得し、ローカルDBクライアントへ入力する
- port forwarding 手順と DB 確認手順は P2-2-04 で README に整理する

## ADR

- RDS for MySQL 自体には、EC2 の Session Manager のようにローカルから直接入る仕組みはない
- private RDS を public にしない。DB の公開範囲を広げず、確認経路だけを限定するため
- DB access host は SSM port forwarding 専用にする。SSH key、public inbound、DB credential 常駐を避けるため
- DB access host は public subnet に置く。P2-2 時点で VPC Endpoint を作らず、SSM Agent が AWS APIs へ到達できる経路を確保するため
- Elastic IP は使わない。ローカルから直接接続する先は EC2 の public IP ではなく、Session Manager session であるため
- DB credential は EC2 に置かない。DB access host が漏えい時の credential 保持点にならないようにするため
- DB access host は Phase 2 期間中維持するが、使わない時はユーザーが手動停止できるものとする

## 対応範囲

- `DataStack` に DB access host 用 Security Group を追加する
- DB access host Security Group 名は `workops-${stage}-db-access-host-sg` とする
- DB access host Security Group は inbound rule を持たない
- DB access host Security Group の outbound は許可のままにする
- `db-sg` に `db-access-host-sg` から 3306 inbound を追加する
- `DataStack` に DB access host 用 IAM Role を追加する
- IAM Role には AWS managed policy `AmazonSSMManagedInstanceCore` のみを付与する
- `DataStack` に EC2 instance を追加する
- EC2 instance Name tag は `workops-${stage}-db-access-host` とする
- EC2 instance type は `t3.micro` とする
- AMI は Amazon Linux 2023 latest を使う
- subnet は public subnet を使う
- key pair は指定しない
- user data に DB credential を書かない
- EC2 instance profile に DB credential 読み取り権限を付けない
- EC2 instance は deploy 直後起動状態とする
- CloudFormation Outputs に DB access host instance id と DB access host Security Group id を出す
- CloudFormation Outputs に public IP を出さない
- CDK assertions を追加する

## 対応ファイル

- `infra/cdk/lib/data-stack.ts`
- `infra/cdk/test/*.test.ts`
- `docs/implementation/phase2/agent-tasks/P2-2/P2-2-02-db-access-host-ssm-port-forwarding.md`

## 除外範囲

- RDS 本体の作成
- SecretStack の作成
- SSM Parameter の作成
- migration 用 ECS Task
- migration 専用 Security Group
- SSH key pair
- SSH inbound rule
- DB access host への DB credential 配置
- DB access host への Secrets Manager 読み取り権限付与
- Elastic IP
- Bastion 用の独自 AMI
- VPC Endpoint
- Client VPN
- AWS Cloud9
- phpMyAdmin
- RDS public access
- RDS Proxy
- IAM DB authentication
- git commit

## 完了条件

- DB access host が `DataStack` に定義されている
- DB access host が Amazon Linux 2023 latest、`t3.micro`、public subnet 配置になっている
- DB access host に key pair が設定されていない
- DB access host Security Group に inbound rule がない
- DB access host IAM Role に `AmazonSSMManagedInstanceCore` が付与されている
- DB access host IAM Role に DB credential 読み取り権限が付いていない
- `db-sg` が `db-access-host-sg` から 3306 inbound を許可している
- CloudFormation Outputs に instance id と Security Group id が出ている
- CloudFormation Outputs に public IP、password、secret value が出ていない
- CDK assertions が代表条件を検証している

## 確認方法

エージェント確認:

- `npm run build`
- `npm test`
- `WORKOPS_STAGE=dev npm run cdk -- synth`
- `git diff --check`

CDK assertions で確認する代表条件:

- EC2 instance type が `t3.micro` である
- Amazon Linux 2023 AMI を使う
- public subnet に配置される
- key pair が設定されていない
- Security Group inbound がない
- IAM Role に `AmazonSSMManagedInstanceCore` がある
- DB credential 読み取り権限がない
- `db-sg` inbound に `db-access-host-sg` から 3306 がある
- public IP、password、secret value が Output に出ていない

ユーザー確認:

- AWS Console で DB access host が起動していることを確認する
- Systems Manager Fleet Manager または Managed Instances で DB access host が管理対象になっていることを確認する
- AWS CLI で Session Manager port forwarding session を開始する
- ローカルDBクライアントから `localhost` の転送ポートへ接続する
- Secrets Manager から取得した username / password で接続する
- `flyway_schema_history` を参照できることを確認する
- AWS dev seed の主要テーブルを SELECT できることを確認する
