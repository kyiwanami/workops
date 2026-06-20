# P2-10-01 CDK entrypoints

## 目的

P2-9 の infra deploy で、deploy 対象外の runtime / app 側 Stack が CDK app 初期化時に定義される問題を解消する。

`infra-dev` 実行時に `DataStack`、`EgressStack`、`EdgeStack`、`AppRuntimeStack` の construct 作成、Lambda bundling、image tag 判定が走らない状態にする。

## ユーザー要求

- P2-10 では既知課題の修正も扱う
- GitHub workflow の単位は増やさない
- CDK entrypoint は用途別に分ける
- Stack 自体は統合しない
- `cdk-all.ts` は作らない
- CodePipeline / CodeBuild 化を妨げない構成にする
- 個別 Stack deploy の可能性は残す
- 実 account ID、role ARN、credential、CloudFront domain、public IP、実 Cognito `sub` は git 管理文書に残さない

## 統合した既知課題

`infra-dev.yml` は非 runtime Stack だけを deploy している。しかし現在の CDK app entrypoint は、CDK app 構築時に `DataStack`、`EgressStack`、`EdgeStack` まで常に `new` しているため、deploy 対象外の runtime / app 側処理が infra deploy ログに出る。

具体例:

- `EdgeStack` の Lambda bundling が infra deploy ログに出る
- `WORKOPS_WEB_IMAGE_TAG` 未設定時の `AppRuntimeStack` 判定が infra deploy ログに出る
- deploy 対象ではない Stack の construct が CDK app 初期化時に作られる

問題は `infra-dev.yml` の deploy 対象ではなく、CDK app entrypoint の Stack 定義境界である。非 runtime deploy 時は runtime Stack を定義しない構成に分ける。

## 前提

- P2-9 の GitHub Actions OIDC deploy は暫定 deploy 手段である
- Phase 2α で CodePipeline + CodeBuild へ移行する
- 現在の `bin/cdk.ts` は runtime Stack を常に `new` している
- `infra-dev.yml` は非 runtime / 維持対象 Stack だけを deploy する
- `app-deploy-dev.yml` は課金 runtime Stack を明示実行する
- 既存データ移行と後方互換性は考慮しない

## 案

CDK app entrypoint を次の3本に分ける。

| entrypoint | 定義する Stack | 用途 |
| --- | --- | --- |
| `bin/cdk-deploy.ts` | `DeployStack` | GitHub Actions OIDC role の初回手動 deploy |
| `bin/cdk-infra.ts` | `FoundationStack`, `SecretStack`, `ConfigStack`, `IdentityStack`, `RegistryStack`, `LogsStack` | 非 runtime / 維持基盤 deploy |
| `bin/cdk-runtime.ts` | `FoundationStack`, `ConfigStack`, `IdentityStack`, `RegistryStack`, `LogsStack`, `DataStack`, `EgressStack`, `EdgeStack`, `AppRuntimeStack` | 課金 runtime / app deploy |

`cdk-runtime.ts` は runtime Stack が参照するために必要な維持基盤 Stack も同じ CDK app 内で定義する。ただし deploy 対象は workflow 側で `DataStack`、`EgressStack`、`EdgeStack`、`AppRuntimeStack` に限定する。

`AppRuntimeStack` は `WORKOPS_WEB_IMAGE_TAG` が空の場合に定義しない。`cdk-runtime.ts` で `AppRuntimeStack` を synth / deploy する場合は、`WORKOPS_WEB_IMAGE_TAG` を必須にする。

`bin/cdk.ts` は使い続けない。`package.json` の CDK 実行 script は、entrypoint を明示できる形に変更する。

## ADR

- CDK entrypoint は用途別に分割する。`cdk deploy <StackName>` の対象指定だけでは、CDK app 初期化時に deploy 対象外 Stack の construct 作成や bundling が走るため。
- Stack は統合しない。`DeployStack`、維持基盤、RDS、egress、edge、app runtime は lifecycle と失敗時の復旧単位が異なるため。
- `cdk-all.ts` は作らない。総合確認は用途別 entrypoint の synth を個別に確認する。
- CodePipeline / CodeBuild 移行後も entrypoint 分割を維持する。pipeline の stage / action 側から用途別 entrypoint を呼べるため。
- 個別 Stack deploy は残す。entrypoint は定義対象を絞るための境界であり、Stack の個別 deploy 可否を奪わない。
- GitHub workflow ファイルは増やさない。既存の `ci.yml`、`infra-dev.yml`、`app-deploy-dev.yml` の中で呼び出す CDK entrypoint だけを切り替える。

## 対応範囲

- CDK entrypoint 追加
- 既存 `bin/cdk.ts` の扱い整理
- `package.json` の CDK script 更新
- `ci.yml` の synth 対象更新
- `infra-dev.yml` の CDK entrypoint 更新
- `app-deploy-dev.yml` の CDK entrypoint 更新
- CDK tests の更新
- README の CDK 実行手順更新

## 除外範囲

- Stack 統合
- `cdk-all.ts`
- CodePipeline / CodeBuild 実装
- GitHub workflow ファイルの追加
- GitHub Actions OIDC role の再設計
- runtime Stack の deploy 順序変更
- RDS / ECS / CloudFront / Cognito の設計変更

## 完了条件

- `cdk-deploy.ts` で `DeployStack` だけが定義される
- `cdk-infra.ts` で非 runtime / 維持対象 Stack だけが定義される
- `cdk-infra.ts` 実行時に `DataStack`、`EgressStack`、`EdgeStack`、`AppRuntimeStack` が定義されない
- `cdk-infra.ts` 実行時に `EdgeStack` の Lambda bundling が走らない
- `cdk-infra.ts` 実行時に `WORKOPS_WEB_IMAGE_TAG` 未設定 warning が出ない
- `cdk-runtime.ts` で runtime deploy に必要な Stack が定義される
- `app-deploy-dev.yml` が `cdk-runtime.ts` を使って runtime Stack を deploy する
- `infra-dev.yml` が `cdk-infra.ts` を使って非 runtime Stack を deploy する
- `DeployStack` 初回手動 deploy 手順が `cdk-deploy.ts` を使う

## 確認方法

エージェント確認:

```powershell
cd C:\git\workops\infra\cdk
npm run build
npm test -- --runInBand
$env:WORKOPS_STAGE = "dev"
$env:GITHUB_REPOSITORY = "owner/repo"
$env:CDK_DEFAULT_ACCOUNT = "000000000000"
$env:CDK_DEFAULT_REGION = "ap-northeast-1"
npm run cdk:deploy-app -- synth DeployStack
npm run cdk:infra -- synth FoundationStack SecretStack ConfigStack IdentityStack RegistryStack LogsStack
$env:WORKOPS_WEB_IMAGE_TAG = "test-sha"
npm run cdk:runtime -- synth DataStack EgressStack EdgeStack AppRuntimeStack
```

生成内容確認:

- `cdk:infra` synth の出力対象に `DataStack`、`EgressStack`、`EdgeStack`、`AppRuntimeStack` が含まれない
- `cdk:infra` 実行ログに runtime 側 bundling と image tag 判定が出ない
- `cdk:runtime` では `WORKOPS_WEB_IMAGE_TAG` を使って `AppRuntimeStack` が定義される

ユーザー確認:

- GitHub Actions の `infra-dev.yml` で非 runtime Stack deploy が成功する
- `infra-dev.yml` ログに runtime 側 Stack の bundling / image tag 判定が出ない
- `app-deploy-dev.yml` で runtime Stack deploy が成功する
