# P2-alpha-1-02 CDK TypeScript 品質ゲート

## 目的

AWS リソースの作成・変更を行わず、`infra/cdk` に ESLint / Prettier を導入し、現行 CDK entrypoint の `cdk synth` を成立させる。

## ユーザー要求

- P2-alpha-1 の中身を詰め切ってからファイル分けする
- 既にコードまたは Notion で分かっていることは質問しない
- Notion の方針に従い、P2-alpha-1 では現行構成の品質ゲートに限定する
- ESLint / Prettier 追加で `npm install` が必要な場合は、実行前に対象パッケージを提示し、ユーザーの明示許可を得る
- Prettier による整形差分は許容し、整形専用の単独コミットとして扱う
- `npm audit fix` や広い `npm update` は実行しない
- Notion の Phase 2α-1 から cdk-nag、CDK assertion、CycloneDX が外れたため、P2-alpha-1 の完了条件に含めない

## 前提

- P2-alpha-0 が完了している
- `infra/cdk` は現行 entrypoint として `cdk:deploy-app`、`cdk:infra`、`cdk:runtime` を持つ
- `PipelineStack`、`MigrationStack`、`bin/cdk-pipeline.ts` は P2-alpha-2 の成果物であり、P2-alpha-1 では作らない
- `infra/cdk/package.json` には ESLint / Prettier 用 script が未導入である
- 既存データ移行と後方互換性は考慮しない

## 案

`infra/cdk` に ESLint / Prettier を導入する。

`package.json` には次の script を追加する。

| script | command |
| --- | --- |
| `lint` | `eslint .` |
| `lint:fix` | `eslint . --fix` |
| `format:check` | `prettier --check .` |
| `format:write` | `prettier --write .` |

ESLint / Prettier の対象から、生成物、依存ディレクトリ、CDK 出力、coverage、dist を外す。

P2-alpha-1 では `cdk:pipeline` script を追加しない。
`cdk:pipeline` script、`bin/cdk-pipeline.ts`、`PipelineStack` は P2-alpha-2 で追加する。

`npm install` が必要な場合は、コーディングエージェントが対象パッケージとコマンドを提示し、ユーザーの明示許可を得てから実行する。
`npm audit fix` と広い `npm update` は実行しない。

## ADR

- P2-alpha-1 の CDK 対象は現行 entrypoint に限定する。Notion の Phase 2α-1 は Stack 構造変更なしの品質ゲート整備であり、PipelineStack は P2-alpha-2 の成果物であるため。
- `cdk:pipeline` は P2-alpha-2 に送る。存在しない `bin/cdk-pipeline.ts` に依存する script を P2-alpha-1 で先行追加しないため。
- npm 依存追加はユーザー許可後に行う。AGENTS.md で `npm install` 系操作は明示許可が必要なため。
- cdk-nag、CDK assertion、CycloneDX は P2-alpha-1 の品質ゲートから外す。Notion の Phase 2α-1 から削除済みであるため。

## 対応範囲

- `infra/cdk/package.json`
- `infra/cdk/package-lock.json`
- ESLint 設定
- Prettier 設定
- ESLint / Prettier ignore 設定
- TypeScript lint 違反の修正
- 現行 CDK entrypoint の synth 確認
- 本 task ファイルの実装結果・確認結果追記

## 除外範囲

- `PipelineStack`
- `MigrationStack`
- `bin/cdk-pipeline.ts`
- `cdk:pipeline` script
- cdk-nag 導入
- CDK assertion test の追加・補強
- DeployStack 撤去
- GitHub Actions workflow 撤去
- buildspec 作成
- AWS deploy
- CodePipeline 実走
- `npm audit fix`
- 広い `npm update`

## 完了条件

- `infra/cdk` に ESLint / Prettier が導入されている
- `npm run lint` が exit 0 になる
- `npm run format:check` が exit 0 になる
- `cdk:deploy-app`、`cdk:infra`、`cdk:runtime` の synth が現行構成で exit 0 になる
- 生成物、依存ディレクトリ、CDK 出力、coverage、dist が lint / format 対象から外れている

## 確認方法

```powershell
cd C:\git\workops\infra\cdk
npm run lint
npm run format:check
Remove-Item Env:AWS_ACCESS_KEY_ID -ErrorAction SilentlyContinue
Remove-Item Env:AWS_SECRET_ACCESS_KEY -ErrorAction SilentlyContinue
Remove-Item Env:AWS_SESSION_TOKEN -ErrorAction SilentlyContinue
$env:WORKOPS_STAGE = "dev"
$env:GITHUB_REPOSITORY = "kyiwanami/workops"
npm run cdk:deploy-app -- synth
npm run cdk:infra -- synth

Remove-Item Env:AWS_ACCESS_KEY_ID -ErrorAction SilentlyContinue
Remove-Item Env:AWS_SECRET_ACCESS_KEY -ErrorAction SilentlyContinue
Remove-Item Env:AWS_SESSION_TOKEN -ErrorAction SilentlyContinue
$env:AWS_PROFILE = "amazon-connect"
$env:AWS_REGION = "ap-northeast-1"
aws sts get-caller-identity --profile $env:AWS_PROFILE --region $env:AWS_REGION
$env:CDK_DEFAULT_ACCOUNT = "<sts Account result>"
$env:CDK_DEFAULT_REGION = $env:AWS_REGION
$env:WORKOPS_STAGE = "dev"
$env:WORKOPS_WEB_IMAGE_TAG = "test-sha"
npm run cdk:runtime -- synth --profile $env:AWS_PROFILE
```

AWS 実操作、`cdk deploy`、`cdk destroy` は実行しない。
`cdk:runtime` は `PrefixList.fromLookup` の context lookup があるため、AGENTS.md の AWS / CDK 操作ルールに従い、profile / region / `WORKOPS_STAGE` を明示して `sts get-caller-identity` で認証先を確認してから実行する。

## 実装時の記録

### 実装結果

- `infra/cdk/package.json` に次の script を追加した。
  - `lint`: `eslint .`
  - `lint:fix`: `eslint . --fix`
  - `format:check`: `prettier --check .`
  - `format:write`: `prettier --write .`
- ユーザー許可後に、次の devDependencies を追加した。
  - `eslint` 10.5.0
  - `typescript-eslint` 8.61.1
  - `prettier` 3.8.4
- 誤って追加した `cdk-nag` 3.0.1 は、Notion の Phase 2α-1 から外れているため削除した。
- `eslint.config.mjs` を追加し、TypeScript を対象に strict type checked lint を有効化した。
- `.prettierrc.json` と `.prettierignore` を追加した。
- `node_modules/`、`cdk.out/`、`cdk.out.*/`、`coverage/`、`dist/`、生成 JavaScript / declaration file は lint / format 対象から外した。
- Prettier による整形差分が発生した。
- lint 対応として、entrypoint の環境変数参照を `readRequiredEnv` helper に寄せた。
- lint 対応として、`test/cognito-client-url-updater.test.ts` の fake client は `async` なしで `Promise.resolve` を返す形にした。
- lint 対応として、`lib/egress-stack.ts` の route logical id 用 index を明示的に `String` 化した。
- `lib/identity-stack.ts` の Cognito password policy は既存仕様どおり `requireSymbols: false` に戻した。

### 確認結果

- `npm run lint`
  - 成功。
- `npm run format:check`
  - 成功。
- `npm run cdk:deploy-app -- synth`
  - 成功。`WORKOPS_STAGE=dev`、`GITHUB_REPOSITORY=kyiwanami/workops` を指定した。
- `npm run cdk:infra -- synth`
  - 成功。`WORKOPS_STAGE=dev` を指定した。
- `npm run cdk:runtime -- synth --profile amazon-connect`
  - 成功。AWS credential 環境変数を削除し、profile `amazon-connect`、region `ap-northeast-1`、`WORKOPS_STAGE=dev`、`WORKOPS_WEB_IMAGE_TAG=test-sha` を指定した。
  - 実 account 値は git 管理文書に記録しない。AWS account は手元の AWS 認証で確認済み。
- `cdk-nag`、CDK assertion、CycloneDX
  - Notion の Phase 2α-1 から外れているため、P2-alpha-1-02 の確認対象から外した。

### 残課題

- なし。
