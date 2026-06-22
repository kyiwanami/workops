# P2-alpha-1-02 CDK TypeScript 品質ゲート

## 目的

AWS リソースの作成・変更を行わず、`infra/cdk` の TypeScript 品質ゲートを導入し、現行 CDK entrypoint の build / test / synth / cdk-nag を pass させる。

## ユーザー要求

- P2-alpha-1 の中身を詰め切ってからファイル分けする
- 既にコードまたは Notion で分かっていることは質問しない
- Notion の方針に従い、P2-alpha-1 では現行構成の品質ゲートに限定する
- ESLint / Prettier / cdk-nag 追加で `npm install` が必要な場合は、実行前に対象パッケージを提示し、ユーザーの明示許可を得る
- Prettier による整形差分は許容し、整形専用の単独コミットとして扱う
- cdk-nag suppression は違反が出た場合だけ、該当リソース直近に最小範囲で追加する
- `npm audit fix` や広い `npm update` は実行しない

## 前提

- P2-alpha-0 が完了している
- `infra/cdk` は現行 entrypoint として `cdk:deploy-app`、`cdk:infra`、`cdk:runtime` を持つ
- `PipelineStack`、`MigrationStack`、`bin/cdk-pipeline.ts` は P2-alpha-2 の成果物であり、P2-alpha-1 では作らない
- `infra/cdk/package.json` には ESLint / Prettier / cdk-nag 用 script が未導入である
- 既存データ移行と後方互換性は考慮しない

## 案

`infra/cdk` に ESLint / Prettier / cdk-nag を導入する。

`package.json` には次の script を追加する。

| script | command |
| --- | --- |
| `lint` | `eslint .` |
| `lint:fix` | `eslint . --fix` |
| `format:check` | `prettier --check .` |
| `format:write` | `prettier --write .` |

ESLint / Prettier の対象から、生成物、依存ディレクトリ、CDK 出力、coverage、dist を除外する。

cdk-nag は現行 entrypoint すべてに適用する。

- `cdk:deploy-app`
- `cdk:infra`
- `cdk:runtime`

P2-alpha-1 では `cdk:pipeline` script を追加しない。
`cdk:pipeline` script、`bin/cdk-pipeline.ts`、`PipelineStack` は P2-alpha-2 で追加する。

`npm install` が必要な場合は、コーディングエージェントが対象パッケージとコマンドを提示し、ユーザーの明示許可を得てから実行する。
`npm audit fix` と広い `npm update` は実行しない。

## ADR

- P2-alpha-1 の CDK 対象は現行 entrypoint に限定する。Notion の Phase 2α-1 は Stack 構造変更なしの品質ゲート整備であり、PipelineStack は P2-alpha-2 の成果物であるため。
- `cdk:pipeline` は P2-alpha-2 に送る。存在しない `bin/cdk-pipeline.ts` に依存する script を P2-alpha-1 で先行追加しないため。
- npm 依存追加はユーザー許可後に行う。AGENTS.md で `npm install` 系操作は明示許可が必要なため。
- cdk-nag suppression は違反が出た場合だけ該当リソース直近に置く。抑制理由と対象リソースを近くし、広い一括抑制を避けるため。

## 対応範囲

- `infra/cdk/package.json`
- `infra/cdk/package-lock.json`
- ESLint 設定
- Prettier 設定
- ESLint / Prettier ignore 設定
- cdk-nag 導入
- 現行 entrypoint の cdk-nag 適用
- CDK assertion test の不足補強
- TypeScript / lint / cdk-nag 違反の修正
- 本 task ファイルの実装結果・確認結果追記

## 除外範囲

- `PipelineStack`
- `MigrationStack`
- `bin/cdk-pipeline.ts`
- `cdk:pipeline` script
- DeployStack 撤去
- GitHub Actions workflow 撤去
- buildspec 作成
- AWS deploy
- CodePipeline 実走
- `npm audit fix`
- 広い `npm update`
- 最初からの cdk-nag suppression 作成

## 完了条件

- `infra/cdk` に ESLint / Prettier / cdk-nag が導入されている
- `npm run lint` が exit 0 になる
- `npm run format:check` が exit 0 になる
- `npm run build` が exit 0 になる
- `npm test -- --runInBand` が exit 0 になる
- `cdk:deploy-app`、`cdk:infra`、`cdk:runtime` の synth が現行構成で exit 0 になる
- 現行 entrypoint すべてで cdk-nag が pass する
- cdk-nag suppression を追加した場合、最小範囲、理由、根拠、再検討条件が task に記録されている
- 生成物、依存ディレクトリ、CDK 出力、coverage、dist が lint / format 対象から除外されている

## 確認方法

```powershell
cd C:\git\workops\infra\cdk
npm run lint
npm run format:check
npm run build
npm test -- --runInBand
$env:WORKOPS_STAGE = "dev"
npm run cdk:deploy-app -- synth
npm run cdk:infra -- synth
npm run cdk:runtime -- synth
```

AWS 実操作、`cdk deploy`、`cdk destroy` は実行しない。

## 実装時の記録

### 実装結果

未実施。

### 確認結果

未実施。

### 残課題

未実施。
