# P2-6-03 verification / cleanup

## 目的

P2-6 の CDK / Spring Boot 実装が、2 App Client 構成と login route / actor_type 整合チェックとして成立していることを確認する。

この task はローカル実装確認と、AWS dev 実確認を行う場合の手順境界を整理する。

## ユーザー要求

- 現在は有料スタックを消している
- 既に判断済みのことを再質問しない
- SaaS 管理者用入口は通常利用者へ表出しない
- 旧 `cognito` registration / App Client の互換を残さない

## 前提

- 維持対象の `IdentityStack` は残っている
- 課金対象の `DataStack`、`EgressStack`、`EdgeStack`、`AppRuntimeStack` は削除済みでよい
- ローカル検証では AWS へ接続しない

## 案

- エージェント確認では Java test を実行する
- エージェント確認では CDK build / test を実行する
- 必要に応じて CDK synth を行い、template 上の 2 App Client / Custom Resource / ECS env を確認する
- `node_modules` が存在しない、または依存コマンドが見つからない場合は、依存復旧せず状態を報告する
- AWS dev 実確認が必要になった場合だけ、`IdentityStack` update と短命 stack 再作成を行う
- AWS dev 実確認後は、課金対象 stack を削除し、`IdentityStack` は維持する

## 判断理由

- ローカルテストを先に完了させる。P2-6 の主な変更は CDK template と Spring Security routing であり、AWS 操作前に静的に検証できるため。
- AWS deploy は自動実行しない。現在は有料スタックを消している状態であり、再作成には費用と待ち時間が発生するため。
- cleanup 対象に `IdentityStack` を含めない。Cognito User Pool / Hosted UI domain / App Client は維持対象であり、短命 runtime stack と lifecycle を分けているため。

## 対応範囲

- Java test 実行
- CDK build / test 実行
- 必要な CDK synth 確認
- 旧 env var / 旧 registration ID が残っていないことの確認
- 実 AWS 確認手順の境界記載
- cleanup 対象の明記

## 除外範囲

- ユーザー指示なしの AWS deploy
- ユーザー指示なしの AWS destroy
- `npm install` / `npm ci` 等の依存復旧
- Cognito ユーザー作成
- RDS への `users.cognito_sub` 投入
- P2-7 以降の管理導線

## 完了条件

- Spring Boot test が成功している
- CDK build が成功している
- CDK test が成功している
- `IdentityStack` template が 2 App Client を含む
- `EdgeStack` template が `PlatformClientId` / `TenantClientId` を Custom Resource に渡す
- `AppRuntimeStack` template が platform / tenant 用の client ID と redirect URI を ECS env に渡す
- `/login/oauth2/code/cognito` が実装対象ソースから除去されている
- `WORKOPS_COGNITO_CLIENT_ID` と `WORKOPS_COGNITO_REDIRECT_URI` が実装対象ソースから除去されている

## 確認方法

Java:

```powershell
cd C:\git\workops\apps\web
.\mvnw.cmd test
```

CDK:

```powershell
cd C:\git\workops\infra\cdk
npm run build
npm test -- --runInBand
Remove-Item Env:AWS_ACCESS_KEY_ID -ErrorAction SilentlyContinue
Remove-Item Env:AWS_SECRET_ACCESS_KEY -ErrorAction SilentlyContinue
Remove-Item Env:AWS_SESSION_TOKEN -ErrorAction SilentlyContinue
$env:WORKOPS_STAGE='dev'
$env:AWS_PROFILE='amazon-connect'
$env:AWS_SDK_LOAD_CONFIG='1'
$env:AWS_REGION='ap-northeast-1'
$env:AWS_DEFAULT_REGION='ap-northeast-1'
$env:CDK_DEFAULT_ACCOUNT=(aws sts get-caller-identity --region $env:AWS_REGION --query Account --output text)
$env:CDK_DEFAULT_REGION='ap-northeast-1'
npm run cdk -- synth IdentityStack
npm run cdk -- synth EdgeStack
npm run cdk -- synth AppRuntimeStack
```

`EdgeStack` は CloudFront origin-facing managed prefix list を `PrefixList.fromLookup` で参照する。

AWS dev 実確認を行う場合:

```powershell
cd C:\git\workops\infra\cdk
Remove-Item Env:AWS_ACCESS_KEY_ID -ErrorAction SilentlyContinue
Remove-Item Env:AWS_SECRET_ACCESS_KEY -ErrorAction SilentlyContinue
Remove-Item Env:AWS_SESSION_TOKEN -ErrorAction SilentlyContinue
$env:WORKOPS_STAGE='dev'
$env:AWS_PROFILE='amazon-connect'
$env:AWS_SDK_LOAD_CONFIG='1'
$env:AWS_REGION='ap-northeast-1'
$env:AWS_DEFAULT_REGION='ap-northeast-1'
$env:CDK_DEFAULT_ACCOUNT=(aws sts get-caller-identity --region $env:AWS_REGION --query Account --output text)
$env:CDK_DEFAULT_REGION='ap-northeast-1'
npm run cdk -- deploy IdentityStack
npm run cdk -- deploy DataStack
npm run cdk -- deploy EgressStack
npm run cdk -- deploy EdgeStack
npm run cdk -- deploy AppRuntimeStack
```

実確認後の cleanup:

```powershell
cd C:\git\workops\infra\cdk
$env:WORKOPS_STAGE='dev'
npm run cdk -- destroy AppRuntimeStack
npm run cdk -- destroy EdgeStack
npm run cdk -- destroy EgressStack
npm run cdk -- destroy DataStack
```

維持対象:

```text
IdentityStack は destroy しない。
```
