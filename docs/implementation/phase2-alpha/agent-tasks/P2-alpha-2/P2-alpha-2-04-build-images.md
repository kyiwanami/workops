# P2-alpha-2-04 Build Images

## 目的

Web image と migration image を別々の CodeBuild project で並列 build し、同一 commit SHA tag で ECR へ push する Build Images stage を作る。

## ユーザー要求

- P2-alpha-2 の task 分割は、`phases.md` の責務境界を最上位単位にする
- 既にコード、Notion、`phases.md` で分かることは質問しない
- 必要な場合は公式ドキュメントやベストプラクティスを確認する
- task ファイル末尾に `実装時の記録`、`実装結果`、`確認結果`、`残課題` を置く

## 前提

- P2-alpha-2-01 が完了している
- P2-alpha-2-02 が完了している
- P2-alpha-2-03 が完了している
- Build Images stage 前に RegistryStack が deploy される
- Web image と migration image は同じ commit SHA tag を使う
- cache tag は `buildcache` に固定する
- Trivy は image scan のみに限定し、MEDIUM / HIGH / CRITICAL で fail する

## 案

Build Images stage に次の CodeBuild project を定義する。

- BuildWebImage
- BuildMigrationImage

2 project は並列実行する。
1 つの buildspec にまとめない。

push する image は次にする。

```text
workops-${stage}-web:${commitSha}
workops-${stage}-migration:${commitSha}
```

cache image は次にする。

```text
workops-${stage}-web-cache:buildcache
workops-${stage}-migration-cache:buildcache
```

Docker buildx registry cache は `--cache-from` と `--cache-to type=registry,mode=max` を使う。
初回 cache miss 用の事前存在確認、分岐、特別処理は作らない。

CodeBuild image は `aws/codebuild/amazonlinux-aarch64-standard:3.0` を使う。
CodeBuild compute は ARM_64 / Graviton 系 Medium とする。
Docker build と Trivy image scan のため `privileged: true` とする。

Build Images stage の合否判定は docker build、Trivy image scan、docker push の結果で行う。
ECR Enhanced Scanning は ECR 側設定として扱い、build gate にはしない。

## 参考

- AWS CodeBuild の Docker layer cache は Linux 環境かつ privileged mode が必要で、privileged mode のセキュリティ影響を考慮する必要がある。
- `phases.md` では CodeBuild VPC mode を使わないため、Docker 実行は非 VPC build 前提で設計する。

## 判断メモ

- Web image と migration image を分ける。Web app 起動と DB migration の責務を分離するため。
- 同一 commit SHA tag を使う。Pipeline run の app / migration 対応関係を明確にするため。
- `WORKOPS_WEB_IMAGE_TAG` 単独運用は廃止する。Phase 2α では web / migration の tag を明示的に扱うため。
- Build Images 前に ECR repository の個別存在確認 step は置かない。RegistryStack deploy と push 結果で検出できるため。

## 対応範囲

- Web image build 用 buildspec
- migration image build 用 buildspec
- PipelineStack の Build Images stage
- CodeBuild project 定義
- ECR login / buildx builder 作成 / cache 設定
- Trivy image scan
- commit SHA tag の受け渡し
- `infra/cdk/test/`
- 本 task ファイルの実装結果・確認結果追記

## 除外範囲

- Build & Test stage
- Migration RunTask
- AppRuntimeStack deploy
- ECR repository 作成
- AWS deploy
- 実 ECR push
- Trivy filesystem scan
- Trivy DB cache 専用の永続 cache
- ECR Enhanced Scanning を Pipeline gate にすること

## 完了条件

- BuildWebImage と BuildMigrationImage が別 CodeBuild project として定義されている
- Build Images stage で 2 project が並列実行される構成になっている
- 両 image が同じ commit SHA tag を使う構成になっている
- registry cache が `buildcache` tag で定義されている
- buildx registry cache の `--cache-from` / `--cache-to` が buildspec に含まれている
- Trivy image scan が MEDIUM / HIGH / CRITICAL で fail する
- `WORKOPS_WEB_IMAGE_TAG` 単独運用が Pipeline 側に残っていない

## 確認方法

```powershell
cd C:\git\workops\infra\cdk
npm run test
npm run lint
npm run format:check
$env:WORKOPS_STAGE = "dev"
$env:CDK_DEFAULT_ACCOUNT = "000000000000"
$env:CDK_DEFAULT_REGION = "ap-northeast-1"
npm run cdk:pipeline -- synth
```

## 実装時の記録

### 実装結果

- 未実装。

### 確認結果

- 未確認。

### 残課題

- 未整理。
