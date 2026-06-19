# P2-5-02 Cognito App Client URL updater Custom Resource

## 目的

`EdgeStack` 作成時に、CloudFront default domain を Cognito App Client の callback URL / logout URL へ自動反映する。

CloudFront default domain は `EdgeStack` 作成ごとに変わるため、手動パッチではなく CDK 管理の Custom Resource で更新する。

## ユーザー要求

- 手動パッチは禁止する
- CloudFront は短命 `EdgeStack` のままにする
- Cognito は長命 `IdentityStack` として残す
- `IdentityStack` から `EdgeStack` への依存は禁止する
- 依存方向は `EdgeStack -> IdentityStack` だけにする
- Custom Resource は `EdgeStack` に置く
- Custom Resource Lambda は `infra/cdk/custom-resources/cognito-client-url-updater/` 配下の別ファイルに分離する
- Update 時は `DescribeUserPoolClient` で現行設定を読み、URL 項目だけ差し替える
- Delete 時は Cognito App Client を変更しない

## 前提

- `IdentityStack` が Cognito User Pool と App Client を所有している
- `EdgeStack` が CloudFront distribution を所有している
- `EdgeStack.cloudFrontHttpsUrl` が P2-5 の正規入口 URL である
- P2-5 では CloudFront default domain を Cognito callback URL と sign-out URL に使う
- CloudFront custom domain は使わない

## 案

- Custom Resource Lambda を `infra/cdk/custom-resources/cognito-client-url-updater/` に作る
- Lambda は Node.js runtime で実装する
- Lambda は CloudFormation Custom Resource event を受け取る
- `Create` と `Update` では Cognito App Client URL を更新する
- `Delete` では何もしない
- Lambda は `DescribeUserPoolClient` で現行 App Client 設定を読む
- Lambda は `UpdateUserPoolClient` に必要な既存設定を保持する
- Lambda は次の URL 項目だけを CloudFront default domain 由来の値へ差し替える
  - `CallbackURLs`
  - `LogoutURLs`
  - `DefaultRedirectURI`
- `CallbackURLs` は `https://{cloudFrontDomainName}/login/oauth2/code/cognito` とする
- `LogoutURLs` は `https://{cloudFrontDomainName}/` とする
- `DefaultRedirectURI` は `https://{cloudFrontDomainName}/login/oauth2/code/cognito` とする
- `EdgeStack` は Custom Resource に User Pool ID、App Client ID、CloudFront domain name を渡す
- `EdgeStack` は `IdentityStack` の User Pool ID と App Client ID を参照してよい
- Custom Resource Lambda の IAM 権限は対象 User Pool / App Client 更新に必要な Cognito IDP 権限に限定する

## ADR

- Custom Resource を `EdgeStack` に置く。CloudFront default domain を生成する Stack が、その生成値を Cognito App Client へ反映する責務を持つため。
- `IdentityStack` から `EdgeStack` を参照しない。長命 Cognito Stack が短命 CloudFront Stack に依存すると、Edge 削除時に Cognito 側が巻き込まれるため。
- `UpdateUserPoolClient` の単純パッチは使わない。未指定項目がデフォルト値に戻る危険があるため、`DescribeUserPoolClient` で現行設定を読んで URL 項目だけ差し替える。
- Delete 時は何もしない。Edge 削除中に古い CloudFront URL が App Client に残っても、次回 Edge 作成時に新しい URL で上書きされるため。
- CloudFront 単体永続化は採用しない。CloudFront VPC origin は内部 ALB に依存し、CloudFront だけ残しても origin 更新問題が残るため。
- 既存データ移行と後方互換性は考慮しない。

## 対応範囲

- Custom Resource Lambda の追加
- Custom Resource provider の追加
- `EdgeStack` への Custom Resource 追加
- `EdgeStack` から `IdentityStack` への User Pool ID / App Client ID 参照追加
- Cognito App Client URL 更新の CDK assertion test 追加
- Custom Resource Lambda の単体テスト追加
- P2-5 README / task 記録への自動更新手順追加

## 除外範囲

- 手動 `aws cognito-idp update-user-pool-client` パッチ
- CloudFormation Parameter による手動 URL 注入
- SSM Parameter Store を主経路にした URL 受け渡し
- `IdentityStack` から `EdgeStack` への CloudFront URL 参照
- Custom Resource Delete 時の App Client URL 消去
- CloudFront custom domain
- Route 53
- ACM 独自ドメイン証明書
- Lambda@Edge
- CloudFront Functions

## 完了条件

- `EdgeStack` に Cognito App Client URL 更新 Custom Resource がある
- Custom Resource Lambda が `infra/cdk/custom-resources/cognito-client-url-updater/` に分離されている
- `Create` と `Update` で App Client URL を更新する
- `Delete` で Cognito App Client を変更しない
- `CallbackURLs` が CloudFront default domain の callback path になる
- `LogoutURLs` が CloudFront default domain の `/` になる
- `DefaultRedirectURI` が CloudFront default domain の callback path になる
- `IdentityStack` は `EdgeStack` を参照していない
- `EdgeStack` は `IdentityStack` の User Pool ID / App Client ID を参照している

## 確認方法

エージェント確認:

```powershell
cd C:\git\workops\infra\cdk
npm run build
npm test -- --runInBand
$env:WORKOPS_STAGE='dev'
npm run cdk -- synth EdgeStack
```

Custom Resource Lambda テストで確認する代表条件:

- `Create` event で `UpdateUserPoolClient` が呼ばれる
- `Update` event で `UpdateUserPoolClient` が呼ばれる
- `Delete` event で `UpdateUserPoolClient` が呼ばれない
- `DescribeUserPoolClient` の戻り値から URL 以外の設定が保持される
- callback URL が `https://{cloudFrontDomainName}/login/oauth2/code/cognito` になる
- logout URL が `https://{cloudFrontDomainName}/` になる

CDK assertions で確認する代表条件:

- `EdgeStack` が Custom Resource Lambda を含む
- Custom Resource Lambda に Cognito IDP 権限がある
- `EdgeStack` template に `AWS::CloudFront::Distribution` が存在する
- `EdgeStack` template に `AWS::CloudFront::VpcOrigin` が存在する
- `IdentityStack` template に CloudFront distribution 参照が存在しない

ユーザー確認:

- `EdgeStack` deploy 後、Cognito App Client の callback URL が CloudFront default domain になっている
- `EdgeStack` deploy 後、Cognito App Client の logout URL が CloudFront default domain になっている
- `EdgeStack` destroy 後、Cognito App Client は削除されない
- 次回 `EdgeStack` deploy 後、新しい CloudFront default domain で App Client URL が更新される
