# P2-1-04 P2-1 検証 / README 記録

## 目的

P2-1 の CDK 基盤、FoundationStack、RegistryStack、LogsStack、ConfigStack が完了条件を満たすことを確認し、`infra/cdk/README.md` に構築、確認、削除手順を整理する。

## ユーザー要求

- P2-1 の実装用 task Markdown を作成する
- 今回は CDK 実装そのものは行わない
- P2-1 の検証と README 整理に必要な判断を明記する
- `infra/cdk/README.md` 更新は P2-1-04 に限定する
- ルート `README.md` と `docs/implementation/phase2/phases.md` は変更対象にしない
- 初期 task Markdown には `実装時の記録` テンプレ欄を入れない

## P2-1 共通方針

- CDK 実装言語は TypeScript とする
- package manager は npm とする
- CDK app は dotenv を使わない
- AWS profile と region は CDK CLI 実行環境に任せる
- `stage` は `WORKOPS_STAGE` 環境変数で指定する
- CDK code は `stage` の runtime 未指定チェック、空文字チェック、形式制限、バリデーションを行わない
- Phase 2 の確認例は `WORKOPS_STAGE=dev` とする
- AWS deploy 確認は P2-1 完了条件に含める
- AWS deploy 確認は認証と課金対象リソース作成を伴うため、ユーザー確認に分類する

## 対応範囲

- P2-1-01 から P2-1-03 の実装結果を横断確認する
- `npm ci` で依存関係を復元できることを確認する
- `npm run build` を確認する
- `npm test` を確認する
- `WORKOPS_STAGE=dev` を指定して `npm run cdk -- synth` を確認する
- CDK assertions が P2-1 の設計判断を検証していることを確認する
- `git diff --check` を確認する
- `infra/cdk/README.md` を更新する
- README にローカル実行時の `WORKOPS_STAGE`、`AWS_PROFILE`、`AWS_REGION` 指定例を書く
- README の region 例は `ap-northeast-1` とする
- README に `npm ci`、`npm run build`、`npm test`、`npm run cdk -- synth` を書く
- README に `npm run cdk -- bootstrap` を書く
- `cdk bootstrap` は `WORKOPS_STAGE` を使わないことを書く
- README に `npm run cdk -- deploy --all` を標準 deploy 手順として書く
- README に `npm run cdk -- destroy --all` を P2-1 単体確認後の削除手順として書く
- P2-2 へ進む場合は P2-1 の Stack を削除しないことを書く
- README に AWS Console 確認項目を書く
- README に P2-1 で作るものと作らないものを書く
- README に「設計意図」「CDK デフォルトに任せるもの」「明示設定するもの」を分けて書く

## README に記録する設計判断

- CDK app は dotenv を使わず、AWS profile と region は実行環境側で指定する
- ローカル実行では PowerShell で `WORKOPS_STAGE`、`AWS_PROFILE`、`AWS_REGION` を指定する
- GitHub Actions の OIDC と `aws-region` 指定は P2-8 で扱う
- GitHub Actions では workflow の `env`、`vars`、`input` のいずれかから `WORKOPS_STAGE` を渡す
- `stage` は `WORKOPS_STAGE` 環境変数で指定する
- CDK code は `stage` の runtime 未指定チェック、空文字チェック、形式制限、バリデーションを行わない
- VPC CIDR は `10.0.0.0/16` とする
- 2AZ 構成とする
- subnet group は `public`、`app`、`db` とする
- subnet CIDR は CDK 自動割り当てに任せる
- P2-1 時点では `app` と `db` は `PRIVATE_ISOLATED` とする
- P2-3 で `app` subnet だけ NAT Gateway 経路を追加する
- `db` subnet は isolated を維持する
- DNS support、DNS hostnames、Internet Gateway、public route table は、CDK VPC construct のデフォルトが設計意図に合うため任せる
- Security Group は `alb-sg`、`app-sg`、`db-sg` を作る
- P2-1 では Security Group inbound rule を追加しない
- migration task は `app-sg` を使い、`migration-sg` は作らない
- LogsStack は `AppRuntimeStack` 削除後も調査ログを Phase 2 期間中残すため独立維持する
- ConfigStack は空 Stack として deploy 対象に含める
- P2-1 では SSM Parameter 実体を作らない
- region、retentionDays、ECR repository 名は SSM に入れない

## AWS Console 確認項目

ユーザー確認では、AWS dev 環境で次を確認する。

- CloudFormation Stack
  - `workops-dev-foundation`
  - `workops-dev-config`
  - `workops-dev-registry`
  - `workops-dev-logs`
- VPC
  - VPC CIDR が `10.0.0.0/16`
  - Name tag が `workops-dev-vpc`
  - public / app / db 用の 6 subnet がある
  - NAT Gateway がない
- Security Group
  - `workops-dev-alb-sg`
  - `workops-dev-app-sg`
  - `workops-dev-db-sg`
  - P2-1 時点の inbound rule が意図どおり最小である
- ECS
  - Cluster 名が `workops-dev-cluster`
- ECR
  - repository 名が `workops-dev-web`
  - lifecycle policy が設定されている
- CloudWatch Logs
  - `/workops/dev/web`
  - `/workops/dev/migration`
  - retention が 7 日
- CloudFormation Outputs
  - FoundationStack の主要 ID
  - RegistryStack の repository 情報
  - LogsStack の log group 名

## 対応ファイル

- `infra/cdk/README.md`
- `docs/implementation/phase2/agent-tasks/P2-1/P2-1-04-verification-and-readme.md`

## 除外範囲

- CDK 実装ファイルの新規実装
- ルート `README.md` 更新
- `docs/implementation/phase2/phases.md` 更新
- GitHub Actions workflow
- RDS
- Secrets Manager
- Cognito
- ALB
- NAT Gateway
- ECS Service
- Fargate task
- Dockerfile
- Docker build
- ECR push
- P2-2 以降の README 整理

## 完了条件

- P2-1-01 から P2-1-03 の実装結果が P2-1 の設計判断と一致している
- `npm ci` が成功する
- `npm run build` が成功する
- `npm test` が成功する
- `WORKOPS_STAGE=dev` を指定して `npm run cdk -- synth` が成功する
- `git diff --check` が成功する
- `infra/cdk/README.md` に P2-1 の構築、確認、削除手順が記載されている
- `infra/cdk/README.md` に CDK デフォルトに任せるものと明示設定するものが分けて記載されている
- ユーザーが `npm run cdk -- bootstrap` を実行できる
- ユーザーが `WORKOPS_STAGE=dev` を指定して `npm run cdk -- deploy --all` を実行できる
- ユーザーが AWS Console で P2-1 の作成結果を確認できる
- P2-2 へ進む場合に P2-1 Stack を削除しない判断が README から分かる

## 確認方法

エージェント確認:

- `cd infra/cdk && npm ci`
- `cd infra/cdk && npm run build`
- `cd infra/cdk && npm test`
- `cd infra/cdk`
- `$env:WORKOPS_STAGE = "dev"`
- `npm run cdk -- synth`
- `git diff --check`

ユーザー確認:

- `$env:WORKOPS_STAGE = "dev"`
- `$env:AWS_PROFILE = "your-profile"`
- `$env:AWS_REGION = "ap-northeast-1"`
- `cd infra/cdk`
- `npm run cdk -- bootstrap`
- `npm run cdk -- deploy --all`
- AWS Console で CloudFormation / VPC / Subnet / Security Group / ECS Cluster / ECR / CloudWatch Logs / Outputs を確認する
