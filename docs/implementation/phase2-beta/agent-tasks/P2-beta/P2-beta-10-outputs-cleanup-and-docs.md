# P2-beta-10 Outputs cleanup and docs

## Summary

Pipeline完走後の仕上げとして、CloudFormation Outputsを全Stackで棚卸しし、不要OutputsとSensitive Output Rule違反を削除する。
残すOutputは `cloudFrontHttpsUrl` と `rdsConsoleCloudShellSecurityGroupId` の2件に限定する。
あわせて、P2-betaの最初に予定した期待値と現状が一致しているかを確認し、変更済みの期待値は正本文書へ反映されていることを確認する。
あわせて `phases.md`、README、task完了記録を更新する。

## ユーザー要件

- 不要なCloudFormation Outputsは基本的に削除してよい。
- Sensitive Output Ruleに反するOutputsは削除する。
- OutputsをStack間連携の正本にしない。
- 必要な手動確認値まで雑に消さない。
- 恒久手順に影響する変更はREADME/開発手順へ反映する。
- 最初に予定した期待値に現状が合っているか確認する。
- 期待値が変わった場合は、変更後の期待値が正本文書へ反映済みであることを確認する。

## 案

- 対象は `infra/cdk/lib/*.ts` の全Stack。
- 残すOutputは用途を明記できる `cloudFrontHttpsUrl` と `rdsConsoleCloudShellSecurityGroupId` だけにする。
- RDS endpoint、secret ARN、実値に近いもの、命名helperやSSMで再現できるものは削除する。
- `cloudFrontDomainName` は `cloudFrontHttpsUrl` で代替できるため削除する。
- `phases.md` に概要図と最終方針を反映する。
- 旧方式/旧名の残存を `rg` で確認する。

## ADR

- 非secret安定設定はSSM Parameter。
- secretはSecrets Manager。
- 命名規約で再現できる値はhelper。
- CDK内部で必要な値はprops / construct参照。
- CloudFormation Outputsは確認や連携の正本にしない。
- `cloudFrontHttpsUrl` は改善後Pipeline完走後のアプリ到達確認で人間が直接使う値として残す。
- `rdsConsoleCloudShellSecurityGroupId` はRDS Console integrated CloudShell VPCのSG選択に使う値として残す。

## Implementation

- Outputs削除対象を一覧化し、用途がないものを削除する。
- 次は削除対象にする。
  - RDS endpoint Output
  - RDS master secret ARN Output
  - RDS instance identifier Output
  - DB名、port、DB subnet group Output
  - ALB DNS name Output
  - listener ARN Output
  - CloudFront domain name Output
  - Foundation / Identity / Registry / Logs / Pipeline / MigrationRunner / WebAcl / WebIngress の全Output
- 次は残す。
  - `WebDeliveryStack` の `cloudFrontHttpsUrl`
  - `DataStack` の `rdsConsoleCloudShellSecurityGroupId`
- README / 運用手順の旧Output参照をSSM / helper / AWS CLI確認へ置き換える。
- `phases.md` のP2-beta節と図を最終状態へ揃える。
- WAF、ECS Blue/Green alarm、認証・認可イベント metric filter の方針が `phases.md` とtask文書で矛盾していないことを確認する。

## Expected value audit

| 確認対象 | 当初期待値 | 現状の正本 | 判定 |
| --- | --- | --- | --- |
| Outputsの役割 | 人間確認用に限定し、Stack間連携の正本にしない | `cloudFrontHttpsUrl` と `rdsConsoleCloudShellSecurityGroupId` だけを残す | 一致 |
| Web ingress連携 | `WebIngressStack` がALB contractをSSM Parameter Storeへ公開し、`WebDeliveryStack` がdeploy時に読む | `/workops/${stage}/web-ingress/origin/*` と listener / ALB full name contractをSSMで受け渡す | 一致 |
| CloudFront domain | `cloudFrontDomainName` Outputは残さず、到達確認はHTTPS URLで行う | `WebDeliveryStack.cloudFrontHttpsUrl` Outputだけを残す | 一致 |
| RDS接続情報 | endpoint、port、DB名、secret ARNをOutputに出さない | DB接続系はDataStack所有SSM ParameterとSecrets Manager、手動CloudShell用SGだけOutput | 一致 |
| Pipeline stage名 | 改善後の実Pipeline名をREADMEとphase docsへ反映する | `DeployFoundation`、`BuildWebImage`、`DeployRuntimeInfrastructure`、`RunMigration`、`DeployAppRuntime` を正本にする | 変更反映 |
| CDK全Stack指定 | `--all` ではなく `'*'` を使う | READMEとhandoffで `npx cdk diff '*'` / `npx cdk deploy '*'` を正本にする | 変更反映 |
| Cross-region / Web ACL | CloudFront用WAFはus-east-1、WebDeliveryへ渡す | `WebAclStack`はus-east-1、Web ACL ARNは同一Pipeline stage内のcross-region referenceで渡す | 変更反映 |

## Verification

- `cdk synth`
- `cdk diff '*'`
- `git diff --check`
- `rg` で旧方式/旧名の残存確認。
- `rg "new CfnOutput|CfnOutput" infra/cdk/lib` で `DataStack` と `WebDeliveryStack` 以外に残っていないことを確認。
- `rg "exportName\\(|stack-exports" infra/cdk` が0件であることを確認。
- `DataStack` Outputs は `rdsConsoleCloudShellSecurityGroupId` のみであることを確認。
- `WebDeliveryStack` Outputs は `cloudFrontHttpsUrl` のみであることを確認。
- Sensitive Output Rule違反がないことを確認。
- P2-betaの当初期待値と現状の差分が、変更済みの正本文書へ反映されていることを確認。
- `EdgeCloudFrontStack` / `EdgeAlbStack` が正本として残っていないことを確認。
- `DataPauseStack` に CloudTrail `LookupEvents` 方針が正本として残っていないことを確認。
- `db/seed/dev` と新SSM pathが文書に残っていることを確認。
- `WebAclStack`、AWS Managed Rules Common Rule Set、Count mode、WAF logging作成、stage 別 WAF log 方針 TODO が文書に残っていることを確認。
- ECS Blue/Green alarm が `HealthyHostCount` / `UnHealthyHostCount` 中心の2本として文書に残っていることを確認。
- 認証・認可イベント metric filter が Alarm / Dashboard なしの方針として文書に残っていることを確認。

## Completion result

2026-06-29 にP2-beta-10の棚卸しを実施した。

- `infra/cdk/lib` の `CfnOutput` は `DataStack.rdsConsoleCloudShellSecurityGroupId` と `WebDeliveryStack.cloudFrontHttpsUrl` の2件だけで、コード側の追加削除は不要だった。
- READMEのPipeline順序を実Pipelineの `BuildAndTest`、`DeployFoundation`、`BuildWebImage`、`DeployRuntimeInfrastructure`、`RunMigration`、`DeployAppRuntime` に更新した。
- README、`phase2-beta/phases.md`、`infra/cdk/README.md` に、CloudFormation OutputsをStack間連携の正本にしない方針を反映した。
- P2-betaの当初期待値と現状の照合表を追加し、一致している項目と変更反映済みの項目を分けて記録した。
- Windowsでは `npm run test` がWindowsAppsの `python3` launcherを踏まないように、pymanager実体を優先する `scripts/run-python-unittest.cjs` を追加した。
- `git diff --check`、`npm run build`、`npm run test`、`npm run lint`、`npm run format:check`、`cdk synth --quiet` は成功した。
- AWS account は手元のAWS認証で確認済み。実値は記録しない。
- `npx cdk diff '*' --profile amazon-connect` は、既存deploy設定と同じ `GITHUB_REPOSITORY` / notification settingを使って再実行し、差分0件だった。
- P2-beta-10では実deployを実行していない。

## Assumptions

- このtaskの再deployは差分内容を確認して判断する。
- AWS実操作が必要な場合は、ユーザーの明示依頼を受けて実行する。
