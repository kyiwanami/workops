# P2-1-03 RegistryStack / LogsStack / ConfigStack

## 目的

P2-3 以降の Web アプリ deploy と調査ログ確認に必要な ECR、CloudWatch Logs、非機密設定の置き場を作る。

## ユーザー要求

- P2-1 の実装用 task Markdown を作成する
- 今回は CDK 実装そのものは行わない
- RegistryStack、LogsStack、ConfigStack の設計判断を明記する
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
- CloudFormation Output は人間確認用に出す

## 対応範囲

### RegistryStack

- `RegistryStack` class を作成する
- CloudFormation stackName を `workops-${stage}-registry` にする
- ECR private repository を作成する
- repository 名は `workops-${stage}-web` とする
- tagged image は最新 2 件だけ保持する
- untagged image は 1 日後に削除する
- P2-3 で `apps/web` の Docker image を push する前提で構成する
- repository name と repository URI を Output に出す

### LogsStack

- `LogsStack` class を作成する
- CloudFormation stackName を `workops-${stage}-logs` にする
- Web アプリ用 log group を作成する
- migration task 用 log group を作成する
- Web アプリ用 log group 名は `/workops/${stage}/web` とする
- migration task 用 log group 名は `/workops/${stage}/migration` とする
- retention は 7 日にする
- RemovalPolicy は `DESTROY` にする
- log group 名を Output に出す

### ConfigStack

- `ConfigStack` class を作成する
- CloudFormation stackName を `workops-${stage}-config` にする
- P2-1 では SSM Parameter の実体を作らない
- 空 Stack として CDK app の deploy 対象に含める
- SSM Parameter Store Standard のパス規約は `/workops/{stage}/...` とする
- P2-3 以降で ALB URL などの非機密値を追加する
- P2-4 で HTTPS URL などの非機密値を追加する
- P2-5 以降で Cognito User Pool ID、Client ID、Redirect URI などの非機密値を追加する

## 設計判断

- ECR repository 名は `workops-${stage}-web` とし、dev では `workops-dev-web` になる
- ECR lifecycle は、deploy で使う tagged image を直近 2 世代に絞り、不要な untagged image を 1 日後に削除する
- LogsStack は ECS Task Definition に付属させず、独立 Stack として作る
- LogsStack を独立させる理由は、P2-3 で `AppRuntimeStack` を削除しても、調査ログを Phase 2 期間中残すため
- Log retention は長期監査ではなく dev 調査用途のため 7 日にする
- Stack 削除時は dev 環境の掃除漏れを避けるため log group も削除する
- P2-1 時点では SSM に入れる確定済み非機密アプリ設定がないため、ConfigStack は SSM Parameter を作らない
- `AWS_REGION` は SSM に入れない。SSM を読むためにも region が必要であり、region は CDK / ECS / AWS SDK の実行環境側で扱う
- `retentionDays` は SSM に入れない。これは `LogsStack` の CDK プロパティであるため
- ECR repository 名は SSM に入れない。これは CDK リソース名または Output で扱うため
- DB 接続情報は P2-2 の Secrets Manager で扱い、SSM に入れない

## 対応ファイル

- `infra/cdk/lib/registry-stack.ts`
- `infra/cdk/lib/logs-stack.ts`
- `infra/cdk/lib/config-stack.ts`
- `infra/cdk/bin/*.ts`
- `infra/cdk/test/*.test.ts`
- `docs/implementation/phase2/agent-tasks/P2-1/P2-1-03-registry-logs-config-stacks.md`

## 除外範囲

- Dockerfile
- Docker build
- ECR push
- ECS Task Definition
- ECS Service
- Fargate task
- migration task
- ALB
- NAT Gateway
- RDS
- Secrets Manager
- Cognito
- SSM Parameter 実体
- CloudWatch Alarm
- Observability Dashboard
- Container Insights
- OpenTelemetry
- 分散トレーシング
- GitHub Actions workflow
- `infra/cdk/README.md` 更新
- AWS deploy

## Outputs

`RegistryStack` は次の CloudFormation Output を出す。

- `repositoryName`
- `repositoryUri`

`LogsStack` は次の CloudFormation Output を出す。

- `webLogGroupName`
- `migrationLogGroupName`

`ConfigStack` は P2-1 時点では Output を出さない。

## 完了条件

- `RegistryStack` を CDK app から生成できる
- `RegistryStack` の stackName が `workops-${stage}-registry` になる
- ECR repository 名が `workops-${stage}-web` になる
- ECR lifecycle policy が tagged 最新 2 件保持、untagged 1 日後削除になっている
- `LogsStack` を CDK app から生成できる
- `LogsStack` の stackName が `workops-${stage}-logs` になる
- `/workops/${stage}/web` と `/workops/${stage}/migration` の log group が作成される
- log group retention が 7 日になっている
- log group の RemovalPolicy が `DESTROY` になっている
- `ConfigStack` を CDK app から生成できる
- `ConfigStack` の stackName が `workops-${stage}-config` になる
- P2-1 時点で `ConfigStack` に SSM Parameter が存在しない

## 確認方法

エージェント確認:

- `cd infra/cdk && npm run build`
- `cd infra/cdk && npm test`
- `cd infra/cdk`
- `$env:WORKOPS_STAGE = "dev"`
- `npm run cdk -- synth`
- CDK assertions で ECR repository、lifecycle policy、log groups、retention、ConfigStack に SSM Parameter がないことを確認する
- `git diff --check`

ユーザー確認:

- この task 単体では AWS deploy 確認を行わない
- AWS deploy と Console 確認は P2-1-04 で行う
