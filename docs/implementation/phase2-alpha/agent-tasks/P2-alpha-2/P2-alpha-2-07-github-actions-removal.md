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

### 実装結果

- 未実装。

### 確認結果

- 未確認。

### 残課題

- 未整理。
