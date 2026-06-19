# P2-1-01 CDK TypeScript 基盤

## 目的

`infra/cdk` を AWS CDK v2 の TypeScript プロジェクトとして初期化し、P2-1 の各 Stack を実装できる土台を作る。

## ユーザー要求

- P2-1 の実装用 task Markdown を作成する
- 今回は CDK 実装そのものは行わない
- `docs/implementation/phase2/agent-tasks/P2-1/` 配下に詳細 task Markdown を置く
- 初期 task Markdown には `実装時の記録` テンプレ欄を入れない
- P2-1 で固めた設計判断を、実装エージェントが迷わない粒度で明記する
- `stage` は CDK context ではなく `WORKOPS_STAGE` 環境変数で渡す
- `app.node.tryGetContext('stage')` は使わない
- `stage` の runtime 未指定チェック、空文字チェック、形式バリデーションは実装しない
- `cdk init` 生成物から不要な差分を増やさず、`watch` script と CDK library version は維持する

## P2-1 共通方針

- CDK 実装言語は TypeScript とする
- package manager は npm とする
- `cdk init app --language typescript --generate-only --package-manager npm` で初期化する
- `package-lock.json` をコミット対象にする
- 再現実行と CI では `npm ci` を使う
- CDK app は dotenv を使わない
- CDK app は `.env.local` を読まない
- AWS profile と region は CDK CLI 実行環境に任せる
- `stage` は `WORKOPS_STAGE` 環境変数で指定する
- CDK code は `stage` の runtime 未指定チェック、空文字チェック、形式制限、バリデーションを行わない
- Phase 2 の確認例は `WORKOPS_STAGE=dev` とする
- Stack class 名に `dev` / `stg` / `prod` を入れない
- CloudFormation stackName は `workops-${stage}-...` 形式で生成する
- 共通タグは `Project=WorkOps`、`Environment={stage}`、`ManagedBy=CDK` とする
- タグに機密情報、個人情報、アカウント ID を入れない
- CDK assertions 用に Jest を使う
- Jest snapshot test は使わない
- alpha construct は使わない

## 案

- 既存 `infra/cdk/README.md` を同ディレクトリ内で一時退避し、`infra/cdk` で `cdk init app --language typescript --generate-only --package-manager npm` を実行してから README を戻す
- `package.json` は `cdk init` 生成物との差分を抑え、`build`、`watch`、`test`、`cdk` script を残す
- CDK app entrypoint は `process.env.WORKOPS_STAGE` を読み、4 Stack へ同じ `stage` を渡す
- `declare global` と `namespace NodeJS` による `ProcessEnv` 拡張は entrypoint に置き、stage 取得専用の小さな helper file は作らない
- Stack ごとの class は `lib/foundation-stack.ts`、`lib/config-stack.ts`、`lib/registry-stack.ts`、`lib/logs-stack.ts` に分ける

## ADR

- `stage` は CDK context ではなく `WORKOPS_STAGE` にする。ローカル、GitHub Actions、CDK code のいずれも同じ環境変数名を使うため
- CDK code で `stage` の runtime validation は行わない。`NodeJS.ProcessEnv` の型拡張でアプリ内部の型を固定し、実行環境の責務を環境変数供給へ寄せるため
- `watch` script は残す。`cdk init` 生成物との差分を減らし、TypeScript watch 利用を妨げないため
- `aws-cdk-lib` は `^2.238.0` を維持し、`aws-cdk` CLI は `2.1126.0` にする。CDK v2 の library と CLI を現在の実装前提に揃えるため
- `stage` 取得、共通タグ付与、Stack 生成は entrypoint に置く。P2-1-01 時点で小さな helper file を増やさず、後続 task が Stack 実装へ集中できるようにするため

## 対応範囲

- `infra/cdk` で `cdk init app --language typescript` を実行する
- 生成された CDK サンプル Stack を WorkOps 用の構成へ置き換えられる状態に整理する
- `package.json` の scripts を最小構成にする
- `build`、`watch`、`test`、`cdk` script を残す
- `synth:dev`、`diff:dev`、`deploy:dev` のような dev 固定 script は作らない
- TypeScript compiler 設定を CDK TypeScript プロジェクトとして成立する状態にする
- Jest と CDK assertions の実行準備を整える
- CDK app の entrypoint で `process.env.WORKOPS_STAGE` を読み取る
- CDK app で `app.node.tryGetContext('stage')` を使わない
- CDK app で `stage` の runtime 未指定チェック、空文字チェック、形式バリデーションを行わない
- `stage` 値は正規化せず、受け取った値を Stack props へ渡す
- `process.env.CDK_DEFAULT_ACCOUNT` と `process.env.CDK_DEFAULT_REGION` を CDK CLI が解決した deploy 先として扱う
- CDK app で `FoundationStack`、`ConfigStack`、`RegistryStack`、`LogsStack` を生成する前提を作る
- 全 Stack に共通タグを付与する

## 対応ファイル

- `infra/cdk/package.json`
- `infra/cdk/package-lock.json`
- `infra/cdk/tsconfig.json`
- `infra/cdk/jest.config.js`
- `infra/cdk/bin/*.ts`
- `infra/cdk/lib/*.ts`
- `infra/cdk/test/*.test.ts`
- `docs/implementation/phase2/agent-tasks/P2-1/P2-1-01-cdk-typescript-bootstrap.md`

## 除外範囲

- `FoundationStack` の具体リソース実装
- `RegistryStack` の具体リソース実装
- `LogsStack` の具体リソース実装
- `ConfigStack` の具体リソース実装
- VPC
- Subnet
- Security Group
- ECS Cluster
- ECR repository
- CloudWatch Logs log group
- SSM Parameter
- `infra/cdk/README.md` 更新
- ルート `README.md` 更新
- `docs/implementation/phase2/phases.md` 更新
- GitHub Actions workflow
- AWS deploy
- CDK bootstrap 実行

## 完了条件

- `infra/cdk` が TypeScript CDK プロジェクトとして成立している
- `npm ci` で依存関係を復元できる
- `npm run build` が成功する
- `npm test` が実行できる
- `WORKOPS_STAGE=dev` を指定して `npm run cdk -- synth` を実行できる CDK app 構成になっている
- CDK app が `app.node.tryGetContext('stage')` を使っていない
- CDK app が `stage` 値の runtime 未指定チェック、空文字チェック、形式バリデーションを行っていない
- CDK app が dotenv と `.env.local` に依存していない
- 共通タグの付与方針が CDK app または Stack 共通処理に入っている

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

- この task では AWS deploy 確認を行わない
- AWS 認証、bootstrap、deploy は P2-1-04 で確認する
