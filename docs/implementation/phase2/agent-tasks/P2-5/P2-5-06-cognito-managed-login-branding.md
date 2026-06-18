# P2-5-06 Cognito Managed Login / Branding

## Summary

P2-5-06 では、IdentityStack の Cognito Hosted UI を classic hosted UI ではなく Managed Login v2 に切り替え、Cognito 提供のデフォルトブランディングを使う。CloudFront / EdgeStack / AppRuntimeStack との依存方向は変えず、IdentityStack 単体で Cognito 側のログイン UI 設定を所有する。

参考:

- [AWS::Cognito::UserPoolDomain ManagedLoginVersion](https://docs.aws.amazon.com/AWSCloudFormation/latest/TemplateReference/aws-resource-cognito-userpooldomain.html)
- [AWS::Cognito::ManagedLoginBranding](https://docs.aws.amazon.com/AWSCloudFormation/latest/TemplateReference/aws-resource-cognito-managedloginbranding.html)

## User Requirements

- Cognito Hosted UI は新しい Managed Login UI を使う。
- Branding は Cognito 提供のデフォルト値を使う。
- Custom Resource は増やさない。
- EdgeStack から IdentityStack への既存依存は増やさない。
- CloudFront custom domain、Route 53、ACM、独自 branding asset は P2-5-06 では扱わない。

## Plan

- `IdentityStack` の User Pool に `FeaturePlan.ESSENTIALS` を明示する。
- User Pool Domain に `ManagedLoginVersion.NEWER_MANAGED_LOGIN` を設定する。
- `AWS::Cognito::ManagedLoginBranding` を `IdentityStack` に追加する。
- `ManagedLoginBranding` は `useCognitoProvidedValues: true` とし、`Settings` / `Assets` は指定しない。
- `UserPoolId` と `ClientId` は、同じ IdentityStack 内の User Pool / App Client から直接渡す。
- Java / EdgeStack / AppRuntimeStack / Custom Resource Lambda は変更しない。

## ADR

- Managed Login v2 は User Pool Domain の `ManagedLoginVersion=2` で有効化する。CloudFormation 管理に寄せることで、Console 手動変更を正本にしない。
- Branding は `AWS::Cognito::ManagedLoginBranding` を使う。Cognito の Hosted UI 表示設定であり、EdgeStack の CloudFront lifecycle とは分ける。
- `useCognitoProvidedValues: true` を採用し、`Settings` / `Assets` は指定しない。P2-5 の目的はログイン導線確認であり、独自ブランドの調整は不要なため。
- `FeaturePlan.ESSENTIALS` を明示する。Managed Login / branding editor を使う前提を template 上に残し、Cognito User Pool の plan を暗黙値にしない。
- 既存データ移行と後方互換性は考慮しない。

## Test Plan

- CDK assertion:
  - `AWS::Cognito::UserPool` に `UserPoolTier: ESSENTIALS` がある。
  - `AWS::Cognito::UserPoolDomain` に `ManagedLoginVersion: 2` がある。
  - `AWS::Cognito::ManagedLoginBranding` が 1 件ある。
  - `ManagedLoginBranding` に `UserPoolId`、`ClientId`、`UseCognitoProvidedValues: true` がある。
  - `ManagedLoginBranding` に `Settings` / `Assets` がない。
  - `IdentityStack` に CloudFront / ALB / ECS / NAT が存在しない。
- Verification commands:
  - `cd C:\git\workops\infra\cdk`
  - `npm run build`
  - `npm test -- --runInBand`
  - `$env:WORKOPS_STAGE='dev'; npm run cdk -- synth IdentityStack`

## AWS dev 実施結果

P2-5-06 では、ユーザー指示により `IdentityStack` を AWS dev へ deploy した。

実行コマンド:

```powershell
cd C:\git\workops\infra\cdk
$env:WORKOPS_STAGE='dev'
$env:AWS_PROFILE='amazon-connect'
$env:AWS_REGION='ap-northeast-1'
$env:AWS_DEFAULT_REGION='ap-northeast-1'
$env:CDK_DEFAULT_ACCOUNT='<AWS_ACCOUNT_ID>'
$env:CDK_DEFAULT_REGION='ap-northeast-1'
npm run cdk -- deploy IdentityStack --profile amazon-connect --require-approval never
```

結果:

- `workops-dev-identity` は `UPDATE_COMPLETE`
- deploy time は約 16 秒
- total time は約 22 秒
- `AWS::Cognito::UserPoolDomain` の `ManagedLoginVersion` は `2`
- `AWS::Cognito::ManagedLoginBranding` は作成済み
- `UseCognitoProvidedValues` は `true`
- `Assets` は空配列
- App Client の `CallbackURLs` / `LogoutURLs` / `DefaultRedirectURI` は CloudFront default domain のまま維持された
- ブラウザで Hosted UI の `Sign in` 画面に到達した

確認コマンド:

```powershell
aws cognito-idp describe-user-pool-domain --profile amazon-connect --region ap-northeast-1 --domain <HOSTED_UI_DOMAIN_PREFIX>
aws cognito-idp describe-user-pool-client --profile amazon-connect --region ap-northeast-1 --user-pool-id <USER_POOL_ID> --client-id <USER_POOL_CLIENT_ID>
aws cognito-idp describe-managed-login-branding-by-client --profile amazon-connect --region ap-northeast-1 --user-pool-id <USER_POOL_ID> --client-id <USER_POOL_CLIENT_ID>
```

実施時トラブル:

| 事象 | 性質 | 原因 | 対応 |
| --- | --- | --- | --- |
| `cdk synth IdentityStack` が `synth.lock` rename の `EPERM` で失敗 | ローカル実行環境の一時制約 | Codex の filesystem 権限が restricted だった | フルアクセス化後に解消 |
| AWS CLI が `InvalidClientTokenId` で失敗 | ローカル認証状態の一時問題 | 期限切れの一時 AWS credential が環境変数に残っていた | `AWS_ACCESS_KEY_ID` / `AWS_SECRET_ACCESS_KEY` / `AWS_SESSION_TOKEN` を外し、`amazon-connect` profile を使った |
| `PrefixList.fromLookup` が account / region 未指定で失敗 | 実行コマンド側の環境不足 | `CDK_DEFAULT_ACCOUNT` / `CDK_DEFAULT_REGION` が未設定だった | AWS account ID と `ap-northeast-1` を明示した |

## Assumptions

- P2-5-01 から P2-5-05 までの IdentityStack / EdgeStack / AppRuntimeStack 連携は完了している。
- P2-5-06 の AWS dev deploy は実施済み。
- Cognito App Client の callback/logout/default redirect URI は P2-5-02 の EdgeStack Custom Resource が引き続き更新する。
- P2-6 では PLATFORM / TENANT App Client 分離と actor_type / ログイン導線の整合を扱う。
