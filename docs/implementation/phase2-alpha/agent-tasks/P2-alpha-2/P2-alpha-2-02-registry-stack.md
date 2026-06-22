# P2-alpha-2-02 RegistryStack

## 目的

`RegistryStack` を Phase 2α の 4 repository 構成に変更し、web image、migration image、各 buildx cache を分けて管理できるようにする。

## ユーザー要求

- P2-alpha-2 の task 分割は、`phases.md` の責務境界を最上位単位にする
- 既にコード、Notion、`phases.md` で分かることは質問しない
- task ファイル末尾に `実装時の記録`、`実装結果`、`確認結果`、`残課題` を置く

## 前提

- P2-alpha-2-01 が完了している
- 現行 `RegistryStack` は `workops-${stage}-web` の単一 repository を公開している
- Phase 2α では本 image repository は immutable tag、commit SHA tag、最新 10 個保持にする
- Phase 2α では cache repository は mutable tag、`buildcache` tag 上書き、最新 5 個保持にする
- ECR Enhanced Scanning は ECR 側設定であり、Pipeline gate にはしない

## 案

`RegistryStack` に次の repository を作る。

- `workops-${stage}-web`
- `workops-${stage}-migration`
- `workops-${stage}-web-cache`
- `workops-${stage}-migration-cache`

公開 property は責務が分かる名前にする。

- `webRepository`
- `migrationRepository`
- `webCacheRepository`
- `migrationCacheRepository`

既存の `repository` 単一公開は廃止する。

本 image repository は immutable tag とし、最新 10 個保持にする。
cache repository は mutable tag とし、`buildcache` tag を上書きできるようにする。

AppRuntimeStack は `webRepository` を受け取る。
MigrationStack は `migrationRepository` を受け取る。
cache repository は Pipeline / CodeBuild から参照し、AppRuntimeStack / MigrationStack からは参照しない。

## 判断メモ

- repository を 4 本に分ける。成果物 image と build cache の lifecycle、immutability、利用者が異なるため。
- `latest`、`dev`、phase 名 tag、manual tag は使わない。Pipeline の実行と image を commit SHA で対応させるため。
- RegistryStack deploy 前に個別存在確認 step は置かない。RegistryStack deploy と Build Images stage の失敗で検出できるため。

## 対応範囲

- `infra/cdk/lib/registry-stack.ts`
- `infra/cdk/bin/cdk-infra.ts`
- `infra/cdk/bin/cdk-runtime.ts`
- `infra/cdk/test/`
- repository output
- AppRuntimeStack の repository props 呼び出し側
- 本 task ファイルの実装結果・確認結果追記

## 除外範囲

- PipelineStack
- Build Images CodeBuild project
- migration image buildspec
- ECR push
- AWS deploy
- 実 ECR repository の存在確認

## 完了条件

- RegistryStack が 4 repository 構成になっている
- 本 image repository が immutable tag、最新 10 個保持になっている
- cache repository が mutable tag、最新 5 個保持になっている
- `registryStack.repository` 単一公開が残っていない
- AppRuntimeStack 呼び出し側が `webRepository` を使っている
- CDK test が 4 repository 構成を検証している

## 確認方法

```powershell
cd C:\git\workops\infra\cdk
npm run test
npm run lint
npm run format:check
npm run build
```

## 実装時の記録

### 実装結果

- `RegistryStack` を 4 repository 構成に変更した。
  - `workops-${stage}-web`
  - `workops-${stage}-migration`
  - `workops-${stage}-web-cache`
  - `workops-${stage}-migration-cache`
- 公開 property を責務別に変更した。
  - `webRepository`
  - `migrationRepository`
  - `webCacheRepository`
  - `migrationCacheRepository`
- 既存の `repository` 単一公開を削除した。
- 本 image repository は immutable tag、最新 10 個保持、untagged image 1日削除にした。
- cache repository は mutable tag、最新 5 個保持、untagged image 1日削除にした。
- repository output は 4 repository それぞれの name / uri を明示名で出す形にした。
- `cdk-runtime.ts` の AppRuntimeStack 呼び出し側は `registryStack.webRepository` を渡す形に変更した。
- CDK test は 4 repository 構成、immutability、lifecycle、outputs を検証する形に更新した。

### 確認結果

- `cd C:\git\workops\infra\cdk; npm run build`
  - 成功。
- `cd C:\git\workops\infra\cdk; npm run test`
  - 成功。Test Suites: 2 passed, Tests: 21 passed。
- `cd C:\git\workops\infra\cdk; npm run lint`
  - 成功。
- `cd C:\git\workops\infra\cdk; npm run format:check`
  - 成功。
- 実装対象の静的検索で `registryStack.repository`、`readonly repository`、ECR repository 1本前提の test が残っていないことを確認した。
- `cd C:\git\workops\infra\cdk; npm run cdk:infra -- synth --profile amazon-connect`
  - 成功。AWS account は手元の AWS 認証で確認済み。region は `ap-northeast-1`。
- `cd C:\git\workops\infra\cdk; npm run cdk:runtime -- synth --profile amazon-connect`
  - 成功。`WORKOPS_WEB_IMAGE_TAG=test-sha` を指定した。AWS account は手元の AWS 認証で確認済み。region は `ap-northeast-1`。

### 残課題

- MigrationStack への `migrationRepository` 受け渡しは、P2-alpha-2-05 で扱う。
