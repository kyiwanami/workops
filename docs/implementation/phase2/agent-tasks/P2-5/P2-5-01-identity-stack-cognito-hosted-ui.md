# P2-5-01 IdentityStack / Cognito Hosted UI

## 目的

P2-5 で使う Cognito User Pool、Hosted UI domain、単一 App Client を `IdentityStack` として定義する。

`IdentityStack` は Cognito 本体の所有者とし、CloudFront、ALB、ECS、NAT Gateway の実行確認セッション Stack とは lifecycle を分ける。

## ユーザー要求

- P2-5 では Cognito Hosted UI ログインと `users.cognito_sub` 突合を扱う
- CloudFront / ALB / ECS / NAT Gateway は短命 Stack のままにする
- Cognito User Pool / Hosted UI domain / App Client は `IdentityStack` として維持対象にする
- 手動パッチは禁止する
- P2-5 では単一 App Client とする
- PLATFORM / TENANT App Client 分離は P2-6 へ残す
- `AdminCreateUser` と管理導線の実 Cognito 接続は P2-7 へ残す
- client secret なしの App Client を使う
- Cognito group / scope を業務認可の正本にしない

## 前提

- P2-4 が完了している
- P2-4 の HTTPS 入口は CloudFront default domain で成立している
- `EdgeStack` は短命 Stack として作成・削除される
- `IdentityStack` は `EdgeStack` を参照しない
- P2-5 では Hosted UI domain を使う
- P2-5 では Cognito ユーザー作成を CDK で自動化しない

## 案

- `IdentityStack` を `infra/cdk/lib/identity-stack.ts` に追加する
- `IdentityStack` は Cognito User Pool を作る
- `IdentityStack` は Cognito Hosted UI domain を作る
- `IdentityStack` は単一 App Client を作る
- App Client は client secret なしにする
- App Client は authorization code flow を有効にする
- App Client scope は `openid` と `email` に限定する
- App Client の callback URL / logout URL は初期値として localhost を持たせない
- App Client の callback URL / logout URL は P2-5-02 の Custom Resource が CloudFront default domain へ更新する
- `IdentityStack` は User Pool ID、App Client ID、Hosted UI domain を output する
- `bin/cdk.ts` に `IdentityStack` を追加する
- 維持対象 Stack に `IdentityStack` を追加する
- 実行確認セッション後の destroy 対象に `IdentityStack` を含めない

## ADR

- `IdentityStack` を維持対象にする。Cognito User Pool / Hosted UI domain / App Client は無料リソース中心であり、CloudFront / ALB / ECS / NAT Gateway の課金対象 lifecycle と分けるため。
- `IdentityStack` から `EdgeStack` を参照しない。CloudFront default domain は `EdgeStack` 作成ごとに変わる短命値であり、長命 Cognito Stack に CloudFormation 依存を作らないため。
- App Client 分離は P2-6 に残す。P2-5 の目的は単一 App Client で Hosted UI ログインと `users.cognito_sub` 突合を成立させることに限定するため。
- Cognito ユーザー作成は P2-7 に残す。P2-5 では `AdminCreateUser` 境界と管理導線を実 Cognito へ接続しないため。
- 既存データ移行と後方互換性は考慮しない。

## 対応範囲

- `IdentityStack` の追加
- Cognito User Pool の追加
- Cognito Hosted UI domain の追加
- client secret なし App Client の追加
- App Client の OAuth 設定
- `bin/cdk.ts` への `IdentityStack` 追加
- CDK assertion test への `IdentityStack` 検証追加
- README / task 記録への維持対象 Stack 追加

## 除外範囲

- PLATFORM / TENANT App Client 分離
- 企業ごとの App Client
- Cognito Trigger
- Pre Token Generation
- Cognito group による業務認可
- Cognito scope による業務認可
- CDK による Cognito テストユーザー作成
- WorkOps 管理導線からの `AdminCreateUser`
- Cognito 側ユーザー属性変更
- 独自ドメイン
- Route 53
- ACM 独自ドメイン証明書

## 完了条件

- `IdentityStack` が CDK app に追加されている
- `IdentityStack` が Cognito User Pool を定義している
- `IdentityStack` が Hosted UI domain を定義している
- `IdentityStack` が client secret なし App Client を定義している
- App Client は authorization code flow を使う
- App Client scope は `openid` と `email` を含む
- `IdentityStack` が User Pool ID と App Client ID を output している
- `IdentityStack` が `EdgeStack` を参照していない
- `EdgeStack` destroy 対象に `IdentityStack` が含まれていない

## 確認方法

エージェント確認:

```powershell
cd C:\git\workops\infra\cdk
npm run build
npm test -- --runInBand
$env:WORKOPS_STAGE='dev'
npm run cdk -- synth IdentityStack
```

CDK assertions で確認する代表条件:

- `AWS::Cognito::UserPool` が存在する
- `AWS::Cognito::UserPoolDomain` が存在する
- `AWS::Cognito::UserPoolClient` が存在する
- User Pool Client が client secret を生成しない
- User Pool Client が authorization code flow を有効にしている
- User Pool Client に `openid` と `email` scope がある
- `IdentityStack` template に `EdgeStack` 由来の CloudFront distribution 参照が存在しない

ユーザー確認:

- AWS dev に `workops-dev-identity` を deploy できる
- Cognito User Pool が作成されている
- Hosted UI domain が作成されている
- App Client が client secret なしで作成されている
- `IdentityStack` は P2-5 実行確認後も残す
