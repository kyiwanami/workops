# P2-4-02 origin security / request forwarding

## 目的

CloudFront を P2-4 以降の HTTPS 入口にするため、ALB 80 ingress を CloudFront origin-facing managed prefix list からの通信に制限する。
同時に、Spring Boot + Thymeleaf の動的 Web アプリに必要な headers、cookies、query strings を ALB origin へ渡す。

## ユーザー要求

- ALB 80 ingress は `0.0.0.0/0` から CloudFront origin-facing managed prefix list のみに変更する
- CloudFront から必要な headers / cookies / query strings を ALB へ渡す
- viewer の `Host` は origin 向けに転送しない
- ALB Cognito 認証、WAF、CloudFront custom header 制限は作らない
- P2-4 では既存 `EdgeStack` を拡張し、新しい `CloudFrontStack` は作らない

## 前提

- P2-4-01 の CloudFront distribution 方針が確定している
- CloudFront origin は `EdgeStack` 内の ALB である
- P2-3 の `EdgeStack` は ALB 80 ingress を `0.0.0.0/0` で持っている
- P2-4 では ALB 直アクセスを正規入口にしない
- P2-4 の正規確認 URL は `cloudFrontHttpsUrl` とする

## 案

- `EdgeStack` の ALB 80 ingress を CloudFront origin-facing managed prefix list のみにする
- CloudFront origin-facing managed prefix list は、`ap-northeast-1` の固定 ID `pl-58a04531` を使う
- CDK では `PrefixList.fromPrefixListId` で `pl-58a04531` を参照し、名前 lookup は使わない
- ALB 80 ingress に `0.0.0.0/0` を残さない
- CloudFront behavior は `OriginRequestPolicy.ALL_VIEWER_EXCEPT_HOST_HEADER` を使う
- CloudFront は cookies を origin request に含める
- CloudFront は query strings を origin request に含める
- CloudFront は viewer request headers を `Host` 以外 origin request に含める
- CloudFront が origin request 用の `Host` header を ALB origin domain に合わせる
- ALB listener rule に header 条件は追加しない

## ADR

- ALB 80 ingress は CloudFront origin-facing managed prefix list に限定する。P2-4 では CloudFront default domain を HTTPS 入口として成立させるため、ALB を直接の公開入口として残さない。
- CloudFront origin-facing managed prefix list は `ap-northeast-1` の固定 ID `pl-58a04531` を使う。名前 lookup は `cdk.context.json` と実行環境依存が発生するため、P2-4 の実装判断から除外する。
- `ALL_VIEWER_EXCEPT_HOST_HEADER` を採用する。Cookie、query string、viewer request header を渡しつつ、viewer の `Host` を ALB origin へ転送しないことで、origin 向け `Host` を ALB に合う形にするため。
- CloudFront custom header による origin 制限は採用しない。P2-4 の制限は Security Group と managed prefix list で表現でき、listener rule を増やす必要がないため。
- WAF は作らない。P2-4 は HTTPS 入口の成立確認であり、アプリケーション防御の設計は Phase2 の完了条件ではないため。
- ALB Cognito 認証は使わない。WorkOps の認証は P2-5 以降で Spring Security OAuth2 Login + Cognito Hosted UI として扱うため。
- 本 task の公開 ALB + CloudFront origin-facing managed prefix list による origin 制限は、P2-4-05 で CloudFront VPC origin 経由の内部 ALB 構成へ置き換える。
- 既存データ移行と後方互換性は考慮しない。

## 対応範囲

- `EdgeStack` の ALB 80 ingress を CloudFront origin-facing managed prefix list のみに変更する
- `EdgeStack` で `PrefixList.fromPrefixListId` を使い、`pl-58a04531` を参照する
- `EdgeStack` から ALB 80 ingress `0.0.0.0/0` を削除する
- CloudFront origin request policy に `ALL_VIEWER_EXCEPT_HOST_HEADER` を設定する
- CloudFront cache policy に `CACHING_DISABLED` を設定する
- CloudFront から ALB へ cookies、query strings、`Host` 以外の viewer headers を転送する
- CDK assertion test に ALB ingress と CloudFront policy の検証を追加する

## 除外範囲

- ALB 直アクセスを正規確認経路にすること
- `0.0.0.0/0` の ALB 80 ingress 維持
- ALB listener rule による custom header 検証
- CloudFront Functions
- Lambda@Edge
- WAF
- ALB Cognito 認証
- CloudFront signed URL
- CloudFront signed cookie
- CloudFront custom domain
- CloudFront から ALB 間 HTTPS
- Cognito Hosted UI
- users 突合
- `ec2.PrefixList.fromLookupByName`
- `cdk.context.json` に依存する prefix list lookup

## 完了条件

- ALB 80 ingress が CloudFront origin-facing managed prefix list のみになっている
- ALB 80 ingress が `ap-northeast-1` の CloudFront origin-facing managed prefix list ID `pl-58a04531` を参照している
- ALB 80 ingress に `0.0.0.0/0` が残っていない
- CloudFront が ALB を origin として使う
- CloudFront が `ALL_VIEWER_EXCEPT_HOST_HEADER` を使う
- CloudFront が `CACHING_DISABLED` を使う
- Cookie、query string、`Host` 以外の viewer headers を ALB origin へ送る構成になっている
- ALB Cognito 認証、WAF、custom header 制限が作られていない

## 確認方法

エージェント確認:

```powershell
cd C:\git\workops\infra\cdk
npm run build
npm test -- --runInBand
$env:WORKOPS_STAGE='dev'
npm run cdk -- synth EdgeStack
```

CDK assertions で確認する代表条件:

- ALB security group ingress の source が CloudFront origin-facing managed prefix list
- ALB security group ingress の source prefix list ID が `pl-58a04531`
- ALB security group ingress に `CidrIp: 0.0.0.0/0` が存在しない
- CloudFront default behavior の origin request policy が `ALL_VIEWER_EXCEPT_HOST_HEADER`
- CloudFront default behavior の cache policy が `CACHING_DISABLED`
- ALB listener に Cognito authenticate action が存在しない
- WAF association が存在しない

ユーザー確認:

- P2-4-03 で CloudFront 経由の HTTPS 到達を確認する
- P2-4-04 で cleanup 後に `workops-dev-alb-sg` の ingress が空であることを確認する

prefix list ID の確認コマンド:

```powershell
aws ec2 describe-managed-prefix-lists `
  --region ap-northeast-1 `
  --filters Name=prefix-list-name,Values=com.amazonaws.global.cloudfront.origin-facing `
  --query "PrefixLists[].{PrefixListId:PrefixListId,PrefixListName:PrefixListName,OwnerId:OwnerId}" `
  --output table
```

2026-06-16 時点の確認値:

```text
PrefixListId: pl-58a04531
PrefixListName: com.amazonaws.global.cloudfront.origin-facing
OwnerId: AWS
```
