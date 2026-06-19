# P2-6-01 IdentityStack / App Client split / URL updater

## 目的

P2-5 の単一 Cognito App Client を、PLATFORM 用と TENANT 用の 2 App Client に分離する。

同一 User Pool / Hosted UI domain は維持し、CloudFront default domain は `EdgeStack` の Custom Resource が両 App Client へ反映する。

## ユーザー要求

- SaaS 運営者用 login route を通常利用者へ表出しない
- `/login` は通常のログイン入口にする
- PLATFORM login は直接URLの `/login/platform` に限定する
- TENANT login は `/login/tenant` にする
- Cognito User Pool は分けない
- App Client は `platform` と `tenant` の 2 つに分ける
- 旧 `cognito` registration / App Client の互換は残さない
- client secret は使わない
- Cognito group / scope を業務認可の正本にしない
- 有料スタックは実装確認後に維持しない

## 前提

- P2-5 で `IdentityStack` が Cognito User Pool / Hosted UI domain / Managed Login v2 を維持している
- P2-5 で `EdgeStack` Custom Resource が App Client URL を CloudFront default domain へ更新している
- P2-5 で `AppRuntimeStack` は ECS 環境変数から Cognito 設定を受け取っている
- P2-6 では User Pool / Hosted UI domain の lifecycle は変更しない

## 案

- `IdentityStack` の単一 `WebClient` を削除する
- `IdentityStack` に `PlatformClient` と `TenantClient` を追加する
- App Client 名は `workops-${stage}-platform-client` と `workops-${stage}-tenant-client` にする
- 両 App Client は authorization code flow、`openid` / `email` scope、client secret なしにする
- platform callback は `/login/oauth2/code/platform` にする
- tenant callback は `/login/oauth2/code/tenant` にする
- logout URL は両方 `/login` にする
- Managed Login Branding は App Client ごとに作る
- `IdentityStack` output は `platformUserPoolClientId` と `tenantUserPoolClientId` にする
- `EdgeStack` props は `cognitoPlatformUserPoolClientId` と `cognitoTenantUserPoolClientId` を受け取る
- Custom Resource は `PlatformClientId` と `TenantClientId` を受け取り、Create / Update で両方更新する
- Custom Resource Delete 時は Cognito App Client を変更しない

## 判断理由

- App Client 分離を採用する。OAuth2 callback route と Spring Security registration を利用者種別ごとに分け、ログイン経路と `actor_type` の整合をアプリ側で検証できるため。
- User Pool は分けない。P2-6 の目的は login route 分離であり、ユーザーディレクトリ自体を分割すると運用と後続の管理導線が過剰に増えるため。
- `/login/platform` は通常画面からリンクしない。SaaS 管理者の入口をエンドユーザーへ見せる必要がなく、直接URLに限定する方が運用上の露出が少ないため。
- logout URL は `/login` に統一する。ログアウト後に actor 種別を再提示せず、通常利用者向け入口へ戻すため。
- 旧 `cognito` registration は残さない。P2-6 時点で後方互換を維持する既存本番利用者がなく、旧導線を残すと actor_type 整合チェックの抜け道になるため。

## 対応範囲

- `IdentityStack` の 2 App Client 化
- `ManagedLoginBranding` の App Client ごとの作成
- `IdentityStack` output の更新
- `EdgeStack` props / Custom Resource properties の更新
- `cognito-client-url-updater` の両 App Client 更新対応
- CDK assertion test の更新

## 除外範囲

- Cognito User Pool 分割
- 企業ごとの App Client
- Cognito Trigger
- Pre Token Generation
- Cognito group / scope による業務認可
- WorkOps 管理画面からの Cognito ユーザー作成
- ユーザー停止 / 権限割当の実装
- AWS deploy / destroy

## 完了条件

- `IdentityStack` が 2 つの `AWS::Cognito::UserPoolClient` を作る
- platform client の callback が `/login/oauth2/code/platform` である
- tenant client の callback が `/login/oauth2/code/tenant` である
- 両 App Client の logout URL が `/login` である
- `IdentityStack` が `platformUserPoolClientId` と `tenantUserPoolClientId` を output する
- `EdgeStack` Custom Resource が `PlatformClientId` と `TenantClientId` を受け取る
- Custom Resource Create / Update が両 App Client を更新する
- Custom Resource Delete が Cognito を変更しない

## 確認方法

```powershell
cd C:\git\workops\infra\cdk
npm run build
npm test -- --runInBand
$env:WORKOPS_STAGE='dev'
npm run cdk -- synth IdentityStack
npm run cdk -- synth EdgeStack
```

