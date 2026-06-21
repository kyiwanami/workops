# P2-10-04 completion record

## 目的

Phase 2 の完了判定を、エージェント確認とユーザー確認に分けて記録する。

P2-10-01 から P2-10-03 までの結果を整理し、Phase 2α へ引き継ぐ事項を明確にする。

## ユーザー要求

- P2-10-04 では Phase 2 完了記録を作る
- P2-10-03 までの実装、README 整理、AWS dev 実確認結果を記録する
- エージェント確認とユーザー確認を混同しない
- 未確認事項を確認済みとして記載しない
- `app-deploy-dev.yml`、Cognito Hosted UI、`platform-admin` の `users.cognito_sub` 紐付け、`/login/platform`、会社作成、TENANT user 作成、`/login/tenant`、トップページ導線、権限外 URL の 403 をユーザー確認済みとして記録する
- GitHub Actions OIDC deploy は暫定手段として扱う
- Phase 2α への引き継ぎとして CodePipeline / CodeBuild 化、GitHub Actions workflow 撤去、migration / seed 実行境界整理を記録する
- 課金 runtime stack は P2-10-03-03 の実確認が完了済みなので停止してよい
- P2-10-04 では AWS 実操作を行わず、完了記録の作成だけを行う
- 実 account ID、role ARN、SSO role ARN、SSO user、credential、CloudFront domain、public IP、実 Cognito `sub` は git 管理文書に残さない

## 前提

- P2-10-01 で CDK entrypoint は `cdk:deploy-app`、`cdk:infra`、`cdk:runtime` に分割済み
- P2-10-02 でトップページ導線と認可表示制御は整理済み
- P2-10-03 で README の Phase 2 AWS dev 導線と実確認結果は整理済み
- AWS dev 実環境確認はユーザー確認として扱う
- コーディングエージェントは機械的検証と記録整理を担当する
- 既存データ移行と後方互換性は考慮しない

## 案

Phase 2 完了状態を次の区分で記録する。

| 区分 | 状態 | 根拠 |
| --- | --- | --- |
| CDK deploy entrypoint | 完了 | P2-10-01 |
| トップページ認可導線 | 完了 | P2-10-02 |
| README Phase 2 AWS dev 導線 | 完了 | P2-10-03-01 |
| app deploy / Cognito UI / Platform / Tenant 導線 | ユーザー確認済み | P2-10-03-03 |
| Phase 2 完了記録 | 本ファイルで完了 | P2-10-04 |

## ADR

- 完了記録はエージェント確認とユーザー確認を分離する。CDK synth、test、workflow 静的確認は AWS dev 実環境確認の代替ではないため。
- P2-10-03-03 の実確認結果はユーザー確認済みとして扱う。ユーザーが AWS dev 上で Platform / Tenant 導線を確認し、「文句なしに動いた」と報告したため。
- CloudWatch Logs は確認済みにしない。今回の正常系確認では追加調査が不要だったが、ユーザー確認済みとは明示されていないため。
- 課金 runtime は停止してよい。P2-10-03-03 の実確認が完了済みであり、P2-10-04 は記録作成だけを行うため。
- Phase 2α 引き継ぎを完了記録に含める。GitHub Actions OIDC deploy は暫定手段であり、後続で AWS ネイティブ CI/CD へ移行するため。
- 実環境固有値は記録しない。git 管理文書に credential、CloudFront domain、実 Cognito `sub` を残さないため。

## エージェント確認

| 確認項目 | 状態 |
| --- | --- |
| README の Phase 2 AWS dev 構成概要 | 記録済み |
| `DeployStack` 初回手動 deploy 導線 | README に記録済み |
| GitHub Environment `dev` 設定導線 | README に記録済み |
| `infra-dev.yml` の非 runtime Stack deploy 境界 | README に記録済み |
| `app-deploy-dev.yml` の runtime / app deploy 境界 | README に記録済み |
| AWS dev 初回接続導線 | README に記録済み |
| P2-10-01 CDK entrypoint 分割との整合 | `cdk:deploy-app`、`cdk:infra`、`cdk:runtime` で整合確認済み |
| workflow 名との整合 | `ci.yml`、`infra-dev.yml`、`app-deploy-dev.yml` で整合確認済み |
| whitespace | `git diff --check` で確認済み |

## ユーザー確認

| 確認項目 | 状態 |
| --- | --- |
| `app-deploy-dev.yml` 実行 | ユーザー確認済み |
| Cognito Hosted UI 到達 | ユーザー確認済み |
| AWS dev RDS の `platform-admin` と Platform 管理者 Cognito user の `sub` 紐付け | ユーザー確認済み |
| `/login/platform` | ユーザー確認済み |
| Platform 画面での会社作成 | ユーザー確認済み |
| 作成した会社に所属する TENANT user 作成 | ユーザー確認済み |
| `/login/tenant` | ユーザー確認済み |
| トップページ導線 | ユーザー確認済み |
| 権限外 URL の 403 | ユーザー確認済み |

## 未確認事項

- CloudWatch Logs の個別確認は未実施。今回の正常系確認では追加調査が不要だったため、Phase 2 完了を妨げる未確認事項としては扱わない。
- 課金 runtime 停止後の再 deploy 確認は未実施。P2-10-04 では AWS 実操作を行わないため、Phase 2α 以降の必要時に確認する。

## 課金 runtime 停止

P2-10-03-03 の実確認が完了済みのため、課金 runtime stack は停止してよい。

停止対象は runtime / app 側に限定する。
維持基盤、OIDC role、GitHub Environment 前提、ECR repository、logs は Phase 2α 引き継ぎのため残す。

AWS 操作を行う場合は、実行前に AWS profile、region、account を確認する。
本タスクでは AWS 実操作を行わない。

## Phase 2α 引き継ぎ

- GitHub Actions OIDC deploy は Phase 2 の暫定手段として扱う
- CodePipeline + CodeBuild 化を行う
- GitHub Actions workflow を撤去する
- migration / seed 実行境界を整理する
- 実環境固有値を git 管理文書へ残さない運用を継続する

## 完了判定

Phase 2 は、P2-10-03-03 までのユーザー確認と本完了記録により完了扱いとする。

ただし、CloudWatch Logs の個別確認と停止後再 deploy 確認は、本タスクでは確認済みにしない。
これらは Phase 2 完了を妨げる未確認事項ではなく、必要時または Phase 2α 以降に確認する。

## 検証結果

- `git diff --check` で whitespace error がないことを確認した
- 本ファイルには実 account ID、role ARN、SSO role ARN、SSO user、credential、CloudFront domain、public IP、実 Cognito `sub` を記録しない
- 本ファイルは完了記録であり、AWS 実操作は行わない
