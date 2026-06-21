# P2-10-03-01 README Phase2 deploy entry

## 目的

README を Phase 2 AWS dev 再現手順の入口として整理し、P2-10-01 の CDK entrypoint 分割後の実行単位と GitHub Actions OIDC deploy の位置づけを、README だけで追える状態にする。

P2-10-03 全体のうち、本タスクでは Phase 2 の構成概要、前提、初回 `DeployStack` 手動 deploy、GitHub Environment `dev` 設定、非 runtime / runtime deploy の入口までを扱う。

## ユーザー要求

- `10-3-1` の実装計画を作る
- P2-10 では README 整理を扱う
- README に従って Phase 2 を再現できる状態にする
- GitHub Actions OIDC deploy は Phase 2 の暫定手段として明記する
- Phase 2α で CodePipeline + CodeBuild へ移行し、GitHub Actions workflow を撤去する前提を明記する
- P2-10-01 の CDK entrypoint 分割と整合させる
- 実 account ID、role ARN、SSO role ARN、SSO user、credential、CloudFront domain、public IP、実 Cognito `sub` は git 管理文書に残さない
- AWS 実環境確認はユーザー確認として扱う

## 前提

- P2-10-01 で `infra/cdk/package.json` に `cdk:deploy-app`、`cdk:infra`、`cdk:runtime` が定義されている
- `.github/workflows` には `ci.yml`、`infra-dev.yml`、`app-deploy-dev.yml` が存在する
- `infra-dev.yml` は非 runtime / 維持対象 Stack の deploy を担う
- `app-deploy-dev.yml` は Docker image push と課金 runtime Stack deploy を担う
- README には既存の MVP ローカル再現手順がある
- 既存データ移行と後方互換性は考慮しない

## 案

README の既存 MVP ローカル再現手順は残す。Phase 2 AWS dev 手順は、MVP 手順とは別の章として README 後半に整理する。

本タスクで README に追加・整理する章は次の通り。

| 章 | 内容 |
| --- | --- |
| Phase 2 AWS dev 概要 | 維持基盤、runtime、app deploy、GitHub Actions OIDC deploy の関係 |
| Phase 2 前提ツール | AWS CLI、AWS profile、Docker Desktop、GitHub Environment `dev` |
| 初回 `DeployStack` 手動 deploy | `cdk:deploy-app` で GitHub Actions OIDC role を作る手順 |
| GitHub Environment `dev` 設定 | `AWS_ROLE_ARN` と `AWS_REGION` を Environment variable として設定する手順 |
| 非 runtime deploy | `infra-dev.yml` が deploy する Stack と実行条件 |
| runtime / app deploy | `app-deploy-dev.yml` が image push と runtime Stack deploy を行うこと |
| Phase 2α への移行前提 | CodePipeline + CodeBuild 化と GitHub Actions workflow 撤去 |

AWS CLI を使う README 手順は、必ず次の順にする。

```powershell
Remove-Item Env:AWS_ACCESS_KEY_ID -ErrorAction SilentlyContinue
Remove-Item Env:AWS_SECRET_ACCESS_KEY -ErrorAction SilentlyContinue
Remove-Item Env:AWS_SESSION_TOKEN -ErrorAction SilentlyContinue

$env:WORKOPS_STAGE = "dev"
$env:AWS_PROFILE = "<aws-profile>"
$env:AWS_SDK_LOAD_CONFIG = "1"
$env:AWS_REGION = "<aws-region>"
$env:AWS_DEFAULT_REGION = $env:AWS_REGION
$env:CDK_DEFAULT_ACCOUNT = (aws sts get-caller-identity --region $env:AWS_REGION --query Account --output text)
$env:CDK_DEFAULT_REGION = $env:AWS_REGION

aws sts get-caller-identity --region $env:AWS_REGION
```

README に書く CDK command は次に統一する。

```powershell
cd C:\git\workops\infra\cdk
npm run cdk:deploy-app -- diff DeployStack
npm run cdk:deploy-app -- deploy DeployStack
npm run cdk:infra -- synth FoundationStack SecretStack ConfigStack IdentityStack RegistryStack LogsStack
npm run cdk:runtime -- synth DataStack EgressStack EdgeStack AppRuntimeStack
```

`cdk:runtime` の synth 例では `WORKOPS_WEB_IMAGE_TAG` に placeholder を使う。実 commit SHA や image digest は README に記録しない。

## ADR

- README は MVP ローカル再現と Phase 2 AWS dev 再現を章で分ける。MVP と AWS dev は必要な前提、認証、課金リソース、確認責務が異なるため。
- P2-10-03-01 は deploy entry までに限定する。Cognito Hosted UI の実ブラウザ確認、CloudWatch Logs 調査、削除手順、完了記録まで同時に扱うと README 更新の確認範囲が大きくなりすぎるため。
- GitHub Actions OIDC deploy は暫定手段として明記する。Phase 2α では CodePipeline + CodeBuild へ移行し、GitHub Actions workflow を撤去するため。
- CDK command は `cdk:deploy-app`、`cdk:infra`、`cdk:runtime` を使う。P2-10-01 で CDK entrypoint を用途別に分割したため。
- 実環境固有値は placeholder だけを書く。README は git 管理文書であり、credential、実 account ID、role ARN、CloudFront domain、実 Cognito `sub` を残さないため。
- AWS dev 実環境の成功確認はユーザー確認として扱う。コーディングエージェントの静的確認や synth は、実 AWS deploy 成功の代替ではないため。

## 対応範囲

- README に Phase 2 AWS dev 概要章を追加・整理する
- README に Phase 2 前提ツールと AWS profile / region / caller identity 確認手順を追加する
- README の `DeployStack` 初回手動 deploy 手順を `cdk:deploy-app` に合わせる
- README の GitHub Environment `dev` 設定説明を整理する
- README の `infra-dev.yml` と `app-deploy-dev.yml` の責務を P2-10-01 後の Stack 境界に合わせる
- README に Phase 2α で CodePipeline + CodeBuild へ移行する前提を明記する
- README から実環境固有値や credential が残らないことを確認する

## 除外範囲

- Cognito Hosted UI login / logout の詳細確認手順
- PLATFORM / TENANT 管理導線の画面確認手順
- CloudWatch Logs 調査手順
- 実行確認セッション終了時の Stack 削除手順
- P2-10 完了記録作成
- AWS dev 実操作の代行
- GitHub Actions 手動実行代行
- CodePipeline / CodeBuild 実装
- GitHub Actions workflow 撤去
- 本番運用設計

## 完了条件

- README に Phase 2 AWS dev の構成概要がある
- README の AWS 操作手順に profile、region、credential 環境変数削除、caller identity 確認が含まれる
- README の `DeployStack` 初回手動 deploy 手順が `npm run cdk:deploy-app -- deploy DeployStack` を使う
- README に GitHub Environment `dev` の `AWS_ROLE_ARN` と `AWS_REGION` 設定手順がある
- README に `infra-dev.yml` が非 runtime Stack を deploy することが明記されている
- README に `app-deploy-dev.yml` が Docker image push と runtime Stack deploy を行うことが明記されている
- README に Phase 2α で CodePipeline + CodeBuild 化し、GitHub Actions workflow を撤去する前提がある
- README に実 account ID、role ARN、SSO role ARN、SSO user、credential、CloudFront domain、public IP、実 Cognito `sub` が残っていない

## 確認方法

エージェント確認:

```powershell
cd C:\git\workops
git diff --check
```

README 整合確認:

- `.github/workflows` の実ファイル名が README に書かれている workflow 名と一致する
- `infra/cdk/package.json` の script 名が README の CDK command と一致する
- Stack 名が CDK 実装済み Stack 名と一致する
- `AWS_ACCESS_KEY_ID`、`AWS_SECRET_ACCESS_KEY`、`AWS_SESSION_TOKEN` を削除する手順がある
- `aws sts get-caller-identity --region $env:AWS_REGION` が含まれる
- secret、password、credential、実 account ID、role ARN、CloudFront domain、public IP、実 Cognito `sub` が含まれない

ユーザー確認:

- README の `DeployStack` 初回手動 deploy 手順を AWS dev で実行できる
- README に従って GitHub Environment `dev` を設定できる
- README の説明と GitHub Actions の `infra-dev.yml` / `app-deploy-dev.yml` の役割が一致している
## 実施結果

- README の `GitHub Actions OIDC deploy` 章を `Phase 2 AWS dev` 章へ整理した
- Phase 2 AWS dev の deploy 単位、前提ツール、AWS profile / region / caller identity 確認、`DeployStack` 初回手動 deploy、GitHub Environment `dev` 設定、GitHub Actions workflow の役割を README に記載した
- GitHub Actions OIDC deploy は Phase 2 の暫定手段であり、Phase 2α で CodePipeline + CodeBuild へ移行して workflow を撤去する前提を README に記載した
- README の CDK command を `cdk:deploy-app`、`cdk:infra`、`cdk:runtime` に統一した
- 実 account ID、role ARN、SSO role ARN、SSO user、credential、CloudFront domain、public IP、実 Cognito `sub` を README に記録しない形にした

## 検証結果

- `.github/workflows/ci.yml`、`.github/workflows/infra-dev.yml`、`.github/workflows/app-deploy-dev.yml` の workflow 名と README の記載が一致することを確認した
- `infra/cdk/package.json` の `cdk:deploy-app`、`cdk:infra`、`cdk:runtime` と README の CDK command が一致することを確認した
- `git diff --check` で whitespace error がないことを確認した
- README / docs 変更のみのため、Maven test や CDK synth は実行しない

