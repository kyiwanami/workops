# P2-beta-10 Outputs cleanup and docs

## Summary

Pipeline完走後の仕上げとして、CloudFormation Outputsを全Stackで棚卸しし、不要OutputsとSensitive Output Rule違反を削除する。
あわせて `phases.md`、README、task完了記録を更新する。

## ユーザー要件

- 不要なCloudFormation Outputsは基本的に削除してよい。
- Sensitive Output Ruleに反するOutputsは削除する。
- OutputsをStack間連携の正本にしない。
- 恒久手順に影響する変更はREADME/開発手順へ反映する。

## 案

- 対象は `infra/cdk/lib/*.ts` の全Stack。
- 残すOutputは用途を明記できるものだけにする。
- RDS endpoint、secret ARN、実値に近いもの、命名helperやSSMで再現できるものは削除する。
- `phases.md` に概要図と最終方針を反映する。
- 旧方式/旧名の残存を `rg` で確認する。

## ADR

- 非secret安定設定はSSM Parameter。
- secretはSecrets Manager。
- 命名規約で再現できる値はhelper。
- CDK内部で必要な値はprops / construct参照。
- CloudFormation Outputsは確認や連携の正本にしない。

## Implementation

- Outputs削除対象候補を一覧化し、用途がないものを削除する。
- 少なくとも次は削除対象として確認する。
  - RDS endpoint Output
  - RDS master secret ARN Output
  - RDS instance identifier Output
  - ALB DNS name Output
  - listener ARN Output
  - CloudFront URL Output
- README / 運用手順の旧Output参照をSSM / helper / AWS CLI確認へ置き換える。
- `phases.md` のP2-beta節と図を最終状態へ揃える。
- WAF、ECS Blue/Green alarm、認証・認可イベント metric filter の方針が `phases.md` とtask文書で矛盾していないことを確認する。

## Verification

- `cdk synth`
- `cdk diff --all`
- `git diff --check`
- `rg` で旧方式/旧名の残存確認。
- Sensitive Output Rule違反がないことを確認。
- `EdgeCloudFrontStack` / `EdgeAlbStack` が正本として残っていないことを確認。
- `DataPauseStack` に CloudTrail `LookupEvents` 方針が正本として残っていないことを確認。
- `db/seed/dev` と新SSM pathが文書に残っていることを確認。
- `WebAclStack`、AWS Managed Rules Common Rule Set、Count mode、WAF logging作成、stage 別 WAF log 方針 TODO が文書に残っていることを確認。
- ECS Blue/Green alarm が `HealthyHostCount` / `UnHealthyHostCount` 中心の2本として文書に残っていることを確認。
- 認証・認可イベント metric filter が Alarm / Dashboard なしの方針として文書に残っていることを確認。

## Assumptions

- このtaskの再deployは差分内容を確認して判断する。
- AWS実操作が必要な場合は、ユーザーの明示依頼を受けて実行する。
