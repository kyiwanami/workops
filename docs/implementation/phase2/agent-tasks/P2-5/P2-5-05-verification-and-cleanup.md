# P2-5-05 verification / cleanup

## 目的

P2-5 の最終構成、AWS dev 実確認結果、短命 Stack cleanup 結果、P2-6 への引き継ぎを整理する。

この task Markdown は、P2-5-05 の手順正本であり、実 AWS 確認後は P2-5 の完了記録として扱う。

## 最終状態

- Cognito User Pool / Hosted UI domain / App Client は `IdentityStack` で維持する
- Cognito Hosted UI は Managed Login v2 と Cognito 提供のデフォルトブランディングを使う
- Cognito App Client は `IdentityStack` 作成時に placeholder callback/logout/default redirect URI を持つ
- `EdgeStack` の Custom Resource が CloudFront default domain を Cognito App Client の callback/logout/default redirect URI へ反映する
- `EdgeStack` は CloudFront VPC Origin と internal ALB を接続する
- ALB Security Group は AWS managed prefix list `com.amazonaws.global.cloudfront.origin-facing` から HTTP 80 を許可する
- `AppRuntimeStack` は dev profile で起動し、Cognito User Pool ID / App Client ID / CloudFront callback URL を ECS 環境変数で受け取る
- `apps/web` は CloudFront 配下で相対 OAuth2 redirect を返し、internal ALB host をブラウザへ露出しない
- AWS dev 実確認では `platform-admin` の Hosted UI ログイン、`users.cognito_sub` 突合、DB 由来 `PLATFORM_ADMIN` 権限での画面到達を確認済み
- 実確認後、課金対象の短命 Stack は削除済みである
- `IdentityStack` は削除せず、Cognito User Pool / Hosted UI domain / App Client を維持する

## ユーザー要求

- P2-5 の実 AWS 確認は `platform-admin` の成功ログイン、`users.cognito_sub` 突合、DB 由来権限での画面利用に限定する
- 突合失敗時の HTTP 403 確認は P2-5 task に入れない
- P2-5 実行確認後は `AppRuntimeStack`、`EdgeStack`、`EgressStack`、`DataStack` を削除する
- `IdentityStack` は削除しない
- RDS は短命運用とし、次回作成時は `platform-admin` の `users.cognito_sub` を再度 SQL で投入する
- Custom Resource Delete 時は Cognito App Client を変更しない
- 次回 `EdgeStack` 作成時に新しい CloudFront default domain で App Client URL が自動更新されることを確認手順に入れる
- P2-5-05 の手順作成時点では AWS の `cdk deploy` / `cdk destroy` を実行しない
- 実 AWS 確認と cleanup は、ユーザーの明示指示後に実行した

## 前提

- P2-5-01 で `IdentityStack` が作成されている
- P2-5-02 で `EdgeStack` に Custom Resource が追加されている
- P2-5-03 で `AppRuntimeStack` が dev profile / Cognito 設定で起動する
- P2-5-04 で `platform-admin` の `users.cognito_sub` が実 Cognito `sub` と紐付いている
- 維持前提 Stack は `FoundationStack`、`SecretStack`、`ConfigStack`、`RegistryStack`、`LogsStack`、`IdentityStack` である
- 短命実行確認 Stack は `DataStack`、`EgressStack`、`EdgeStack`、`AppRuntimeStack` である
- Cognito User Pool / Hosted UI domain / App Client は `IdentityStack` として維持対象にする

## 案

- P2-5-05 では Java / CDK / Flyway / seed は変更しない
- P2-5-05 の手順作成時点では AWS の `cdk deploy` / `cdk destroy` を実行しない
- 実 AWS 確認と cleanup はユーザーの明示指示後に実行し、結果をこの task Markdown に記録する
- エージェント確認では CDK build / test / synth を行う
- CDK synth は `IdentityStack`、`DataStack`、`EgressStack`、`EdgeStack`、`AppRuntimeStack` を対象にする
- エージェント確認では Spring Boot test を行う
- エージェント確認では task Markdown と README の整合を確認する
- ユーザー確認では Hosted UI ログインを確認する
- ユーザー確認では CloudFront callback を確認する
- ユーザー確認では `/auth/claims` で `platform-admin`、`PLATFORM`、`PLATFORM_ADMIN` を確認する
- ユーザー確認では `/admin/users` または `/admin/companies` に 200 応答で到達できることを確認する
- AWS dev 実確認 deploy 手順には `DataStack` を含める
- AWS dev 実確認 deploy 順序は `IdentityStack` 確認後に `DataStack`、`EgressStack`、`EdgeStack`、`AppRuntimeStack` とする
- P2-5 確認後、短命 Stack を `AppRuntimeStack`、`EdgeStack`、`EgressStack`、`DataStack` の順で削除する
- `IdentityStack` は維持する
- 次回 P2-5 実行確認時は `DataStack` を再作成し、`platform-admin` の `users.cognito_sub` を再投入する
- cleanup 後、Cognito User Pool / App Client / Hosted UI domain が残っていることを確認する
- 次回実行確認時は `EgressStack`、`EdgeStack`、`AppRuntimeStack` を再作成し、Custom Resource が App Client URL を更新する

## ADR

- P2-5-05 の手順作成時点では AWS deploy / destroy を実行しない。実 AWS 操作はユーザーの明示指示後に実行し、完了記録としてこの task Markdown に残すため。
- P2-5 の AWS 実環境確認は成功ログインに限定する。P2-5 の AWS 固有論点は Hosted UI、CloudFront callback、Cognito `sub`、RDS 上の `users.cognito_sub`、ECS dev profile 起動にあるため。
- `DataStack` を deploy 手順に含める。P2-5-04 の `platform-admin` SQL 紐付けには AWS dev RDS が必要であり、deploy 手順から抜けると Hosted UI 成功ログインの前提が成立しないため。
- `IdentityStack` は cleanup 対象にしない。Cognito 本体は維持対象であり、短命 Edge / Runtime / Egress の lifecycle と分けるため。
- `DataStack` は P2-5 確認後に削除する。RDS は短命運用であり、作り直すたびに `platform-admin` の `users.cognito_sub` を実 Cognito `sub` で再投入する前提にするため。
- 次回 Edge 作成時の App Client URL 更新を確認する。CloudFront default domain は `EdgeStack` 作成ごとに変わるため、Custom Resource の再更新が P2-5 方針の中心であるため。
- 既存データ移行と後方互換性は考慮しない。

## 対応範囲

- P2-5 エージェント確認手順の記載
- P2-5 ユーザー確認手順の記載
- Hosted UI 成功ログイン確認の記載
- `platform-admin` 画面利用確認の記載
- `DataStack` を含む AWS dev 実確認 deploy 手順の記載
- 短命 Stack cleanup 手順の記載
- `IdentityStack` 維持確認の記載
- 次回 `EdgeStack` 再作成時の App Client URL 更新確認の記載
- 次回 `DataStack` 再作成時の `users.cognito_sub` 再投入確認の記載
- P2-6 への引き継ぎ記載

## 除外範囲

- P2-5-05 手順作成時点での `cdk deploy`
- P2-5-05 手順作成時点での `cdk destroy`
- 突合失敗時の HTTP 403 実 AWS 確認
- TENANT ユーザー Hosted UI ログイン確認
- PLATFORM / TENANT App Client 分離
- `AdminCreateUser`
- GitHub Actions deploy
- CodePipeline
- CodeBuild
- CloudWatch Alarm
- Observability Dashboard
- git commit

## 完了条件

- CDK build / test / synth が成功している
- Spring Boot test が成功している
- AWS dev 実確認手順に `DataStack` deploy が含まれている
- cleanup 対象が `AppRuntimeStack`、`EdgeStack`、`EgressStack`、`DataStack` に限定されている
- `IdentityStack` を destroy しないことが明記されている
- `EdgeStack` deploy 時に Cognito App Client URL が CloudFront default domain へ更新されている
- `AppRuntimeStack` が dev profile で起動している
- Hosted UI で Cognito テストユーザーがログインできる
- CloudFront callback で `apps/web` に戻れる
- `/auth/claims` で `platform-admin`、`PLATFORM`、`PLATFORM_ADMIN` を確認できる
- `/admin/users` または `/admin/companies` に 200 応答で到達できる
- `AppRuntimeStack`、`EdgeStack`、`EgressStack`、`DataStack` の cleanup 手順が記録されている
- cleanup 後も `IdentityStack` が残る

## 確認方法

エージェント確認:

```powershell
cd C:\git\workops\infra\cdk
npm run build
npm test -- --runInBand
$env:WORKOPS_STAGE='dev'
npm run cdk -- synth IdentityStack
npm run cdk -- synth DataStack
npm run cdk -- synth EgressStack
npm run cdk -- synth EdgeStack
npm run cdk -- synth AppRuntimeStack
```

Spring Boot 確認:

```powershell
cd C:\git\workops\apps\web
.\mvnw.cmd test
```

ユーザー確認:

```powershell
cd C:\git\workops\infra\cdk
$env:WORKOPS_STAGE='dev'
npm run cdk -- deploy IdentityStack
npm run cdk -- deploy DataStack
npm run cdk -- deploy EgressStack
npm run cdk -- deploy EdgeStack
npm run cdk -- deploy AppRuntimeStack
```

Cognito App Client URL 確認:

- Cognito App Client の callback URL が CloudFront default domain の `/login/oauth2/code/cognito` になっている
- Cognito App Client の logout URL が CloudFront default domain の `/` になっている
- `DefaultRedirectURI` が callback URL と一致している

実 AWS deploy 時間メモ:

| Stack | 成功時の目安 |
| --- | ---: |
| `FoundationStack` | 変更なし、ほぼ 0 秒 |
| `SecretStack` | 1 分未満 |
| `ConfigStack` | 1 分未満 |
| `RegistryStack` | 変更なし、ほぼ 0 秒 |
| `LogsStack` | 変更なし、ほぼ 0 秒 |
| `IdentityStack` | 約 16 秒 |
| `DataStack` | 約 7 分 32 秒 |
| `EgressStack` | 約 2 分 9 秒 |
| `EdgeStack` | 約 15 分 11 秒 |
| `AppRuntimeStack` | 約 3 分 18 秒 |
| CloudFront VPC Origin 到達性確認時の `FoundationStack` 更新 | 約 11 秒 |
| CloudFront VPC Origin 到達性確認時の `EdgeStack` 更新 | 約 11 秒 |
| OAuth2 redirect 設定反映時の ECS force deployment | 約 2 分 50 秒から 3 分 6 秒 |
| Managed Login v2 設定時の `IdentityStack` 更新 | deploy 約 16 秒、total 約 22 秒 |

補足:

- `IdentityStack` の時間は、callback URL 設定修正後に成功した再 deploy の値である
- `DataStack` は RDS 作成時間が大半である
- `EdgeStack` は CloudFront VPC Origin と Distribution 作成時間が大半である
- ECS force deployment の時間は、既存 `workops-dev-web:p2-3-manual` tag へ push 後に `aws ecs wait services-stable` が完了するまでの値である
- Managed Login v2 設定時の `IdentityStack` 更新は User Pool tier / User Pool Domain / Managed Login Branding の更新であり、RDS / CloudFront / ECS は更新していない

最終構成の実 AWS 確認:

- `workops-dev-identity` は `UPDATE_COMPLETE`
- Cognito User Pool は `ESSENTIALS` tier である
- Cognito User Pool Domain は Managed Login v2 である
- Cognito Managed Login Branding は Cognito provided values を使う
- Cognito App Client の callback URL は CloudFront default domain の `/login/oauth2/code/cognito` である
- Cognito App Client の logout URL は CloudFront default domain の `/` である
- Cognito App Client の `DefaultRedirectURI` は callback URL と一致する
- `EdgeStack` は `IdentityStack` へ依存し、`IdentityStack` から `EdgeStack` への依存はない
- ALB Security Group inbound は AWS managed prefix list `com.amazonaws.global.cloudfront.origin-facing` 由来の HTTP 80 を許可する
- Target Group は `healthy`
- ECS service は `ACTIVE`、desired 1、running 1、rollout `COMPLETED`
- CloudFront default domain の `/actuator/health` は 200 / `status: UP`
- CloudFront default domain の `/` は 302 / `Location: /oauth2/authorization/cognito`
- CloudFront default domain の `/oauth2/authorization/cognito` は Cognito Hosted UI へ 302 し、`redirect_uri` は CloudFront default domain の `/login/oauth2/code/cognito`
- Chrome で CloudFront default domain を開くと Cognito Hosted UI に到達し、ブラウザ URL に internal ALB host は含まれない

実 AWS deploy 時のトラブル:

| 事象 | 性質 | 原因 | 最終状態 |
| --- | --- | --- | --- |
| `IdentityStack` 初回 deploy 失敗 | 一時障害ではなく設計ミス | Cognito App Client で authorization code flow を有効にしながら callback URL が未設定だった | `IdentityStack` 作成時は placeholder callback/logout/default redirect URI を持ち、`EdgeStack` Custom Resource が CloudFront URL で上書きする |
| CloudFront default domain から 504 | 一時障害ではなく到達性設計漏れ | internal ALB + CloudFront VPC Origin 構成で ALB Security Group inbound が空だった | AWS managed prefix list `com.amazonaws.global.cloudfront.origin-facing` を CDK lookup し、ALB HTTP 80 inbound を許可する |
| 未認証アクセス時に internal ALB URL へリダイレクト | 一時障害ではなく proxy 配下の redirect 設計漏れ | CloudFront -> internal ALB -> ECS の HTTP origin で、Tomcat の `sendRedirect` が origin host の絶対 Location を返していた | `server.tomcat.use-relative-redirects=true` を有効化し、OAuth2 Login 開始 URL は `/oauth2/authorization/cognito` の相対 Location として返す |

ログイン確認:

- Cognito テストユーザーで Hosted UI へログインする
- CloudFront callback で `apps/web` に戻る
- `/auth/claims` で `platform-admin`、`PLATFORM`、`PLATFORM_ADMIN` を確認する
- `/admin/users` または `/admin/companies` に 200 応答で到達できる

P2-5 実 AWS 確認結果:

- ユーザー確認により、Cognito Hosted UI で `platform-admin` としてログインできた
- CloudFront default domain から Cognito Hosted UI へのログイン導線が成立した
- P2-5 の実 AWS 確認は完了扱いとする
- 実 Cognito `sub`、パスワード、RDS の実更新値は task Markdown に記録しない
- 今後の AWS dev RDS への MySQL 実行は、PJ 横断ルールに従い、ユーザー実施またはユーザーの明示指示時のみ RDS Console integrated CloudShell VPC から行う

実行確認セッション終了時の cleanup:

```powershell
cd C:\git\workops\infra\cdk
$env:WORKOPS_STAGE='dev'
npm run cdk -- destroy AppRuntimeStack
npm run cdk -- destroy EdgeStack
npm run cdk -- destroy EgressStack
npm run cdk -- destroy DataStack
```

維持対象確認:

```powershell
aws cloudformation describe-stacks --stack-name workops-dev-identity
aws cognito-idp list-user-pools --max-results 10
```

cleanup 後に確認すること:

- `workops-dev-identity` が存在する
- Cognito User Pool が存在する
- Cognito Hosted UI domain が存在する
- Cognito App Client が存在する
- `workops-dev-app-runtime`、`workops-dev-edge`、`workops-dev-egress`、`workops-dev-data` が削除されている

実 AWS cleanup 結果:

- ユーザー指示により、課金対象の短命 Stack を削除した
- `workops-dev-app-runtime` は削除済みである
- `workops-dev-edge` は削除済みである
- `workops-dev-egress` は削除済みである
- `workops-dev-data` は削除済みである
- `workops-dev-identity` は `UPDATE_COMPLETE` で維持されている
- Cognito User Pool / Hosted UI domain / App Client は `IdentityStack` 側の維持対象として残す

次回実行確認時:

```text
DataStack、EgressStack、EdgeStack、AppRuntimeStack を再作成する。
platform-admin の users.cognito_sub は、実 Cognito sub を使って再投入する。
EdgeStack deploy 時に Custom Resource が新しい CloudFront default domain を Cognito App Client に反映する。
```

## P2-6 引き継ぎ

- P2-6 では PLATFORM / TENANT App Client 分離を扱う
- P2-6 では単一 App Client で成立した P2-5 の Hosted UI / `sub` 突合を前提にする
- P2-6 では actor_type とログイン導線の整合チェックを扱う
- P2-7 では WorkOps 管理導線から `AdminCreateUser` と `users.cognito_sub` 登録を扱う
