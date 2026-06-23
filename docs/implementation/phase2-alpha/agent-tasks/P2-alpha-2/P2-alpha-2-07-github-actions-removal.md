# P2-alpha-2-07 GitHub Actions removal

## 目的

GitHub Actions OIDC 暫定 deploy 経路を撤去し、`DeployStack`、`bin/cdk-deploy.ts`、GitHub Actions workflow をリポジトリから削除する。

## ユーザー要求

- P2-alpha-2 の task 分割は、`phases.md` の責務境界を最上位単位にする
- 既にコード、Notion、`phases.md` で分かることは質問しない
- task ファイル末尾に `実装時の記録`、`実装結果`、`確認結果`、`残課題` を置く

## 前提

- P2-alpha-2-03 が完了している
- P2-alpha-2-04 が完了している
- P2-alpha-2-05 が完了している
- P2-alpha-2-06 が完了している
- Phase 2α では GitHub Actions workflow を PR チェック用途にも残さない
- Phase 2α では DeployStack を廃止し、同じ責務を PipelineStack に移す

## 案

次を削除する。

- `infra/cdk/lib/deploy-stack.ts`
- `infra/cdk/bin/cdk-deploy.ts`
- `infra/cdk/package.json` の `cdk:deploy-app`
- `.github/workflows/ci.yml`
- `.github/workflows/infra-dev.yml`
- `.github/workflows/app-deploy-dev.yml`
- `.github/workflows/.gitkeep`

TypeScript build で生成済みの `.js` / `.d.ts` が git 管理されている場合は、対応する生成物も整合させる。

既存 test から DeployStack / GitHub Actions workflow 前提の検証を削除し、PipelineStack 前提の検証に置き換える。

README / CDK README に GitHub Actions OIDC deploy の操作説明が残っている場合は、Phase 2α の PipelineStack 初回 deploy / P2-alpha-3 手順へ更新する。

## 判断メモ

- GitHub Actions workflow は残さない。Phase 2α では AWS ネイティブ CI/CD を正本にするため。
- `.github/workflows/.gitkeep` も残さない。workflow ディレクトリを空で維持する必要がないため。
- DeployStack の互換 entrypoint は残さない。暫定 deploy 経路の誤使用を避けるため。

## 対応範囲

- `infra/cdk/lib/deploy-stack.ts`
- `infra/cdk/bin/cdk-deploy.ts`
- `infra/cdk/package.json`
- `.github/workflows/`
- `infra/cdk/test/`
- README / CDK README の該当箇所
- 本 task ファイルの実装結果・確認結果追記

## 除外範囲

- PipelineStack 実装
- Build Images 実装
- MigrationStack 実装
- AppRuntimeStack Blue/Green 実装
- `cdk deploy`
- GitHub repository settings 変更
- GitHub branch protection 変更

## 完了条件

- `DeployStack` が存在しない
- `bin/cdk-deploy.ts` が存在しない
- `cdk:deploy-app` script が存在しない
- `.github/workflows` 配下に workflow と `.gitkeep` が存在しない
- test に DeployStack / GitHub Actions workflow 前提の期待値が残っていない
- README に GitHub Actions OIDC deploy を現行手順として案内する記述が残っていない

## 確認方法

```powershell
cd C:\git\workops
Get-ChildItem -LiteralPath .github\workflows -Force -ErrorAction SilentlyContinue

cd C:\git\workops\infra\cdk
npm run test
npm run lint
npm run format:check
```

## 実装時の記録

- 2026-06-23: `DeployStack`、`cdk-deploy.ts`、`cdk:deploy-app`、`.github/workflows` 配下の workflow / `.gitkeep` を撤去対象として確認した。
- 2026-06-23: README / CDK README / AGENTS に残っていた現行手順としての GitHub Actions OIDC deploy / `cdk:deploy-app` 案内を PipelineStack 前提へ更新した。
- 2026-06-23: AWS deploy、Pipeline 実走、CodeConnection OAuth 認可、SNS メール受信、ManualApproval は P2-alpha-2 の除外範囲のため実施していない。

### 実装結果

- `infra/cdk/lib/deploy-stack.ts` と `infra/cdk/bin/cdk-deploy.ts` を削除した。
- `infra/cdk/package.json` から `cdk:deploy-app` script を削除した。
- `.github/workflows/ci.yml`、`.github/workflows/infra-dev.yml`、`.github/workflows/app-deploy-dev.yml`、`.github/workflows/.gitkeep` を削除した。
- CDK test から DeployStack / GitHub Actions workflow 内容前提の検証を削除し、撤去状態を検証する test に置き換えた。
- root README、`infra/cdk/README.md`、`AGENTS.md` を Phase 2α の PipelineStack / CodePipeline 前提へ更新した。

### 確認結果

- `npm run build` 成功。
- `npm run test -- --runInBand` 成功。22 tests passed。
- `npm run lint` 成功。
- `npm run format:check` 成功。
- AWS profile `amazon-connect` / region `ap-northeast-1` で `aws sts get-caller-identity` により認証先を確認済み。
- `WORKOPS_STAGE=dev`、`WORKOPS_IMAGE_TAG=test-sha` で `npm run cdk:runtime -- synth --quiet --profile amazon-connect` 成功。
- `WORKOPS_STAGE=dev`、`WORKOPS_IMAGE_TAG=test-sha`、`GITHUB_REPOSITORY=kyiwanami/workops`、`WORKOPS_PIPELINE_NOTIFICATION_EMAIL` 設定で `npm run cdk:pipeline -- synth --quiet --profile amazon-connect` 成功。
- `WORKOPS_STAGE=dev` で `npm run cdk:infra -- synth --quiet --profile amazon-connect` 成功。
- `DeployStack` / `cdk-deploy.ts` は存在しないことを確認した。
- `.github/workflows` 配下に workflow / `.gitkeep` が存在しないことを確認した。
- 実 AWS deploy、Pipeline 実走、ECR push、ECS RunTask は実施していない。

### 残課題

- 既存 AWS dev に残っている可能性がある `workops-{stage}-deploy` stack や GitHub Environment 変数の整理は P2-alpha-3 の実環境作業で扱う。
- CodeConnection OAuth 認可、Pipeline 自走、SNS メール受信、ManualApproval は P2-alpha-3 でユーザー確認として扱う。
