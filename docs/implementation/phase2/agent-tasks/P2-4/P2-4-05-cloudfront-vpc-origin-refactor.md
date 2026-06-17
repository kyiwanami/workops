# P2-4-05 CloudFront VPC origins refactor

## 目的

P2-4 の HTTPS 入口の最終状態を、公開 ALB + CloudFront origin-facing managed prefix list 制限から、内部 ALB + CloudFront VPC origin へ置き換える。

CloudFront default domain は引き続き P2-5 以降の Cognito callback URL と sign-out URL に使う HTTPS URL の正本とする。

## ユーザー要求

- `P2-4-05` として CloudFront VPC origins へリファクタリングする agent task Markdown を追加する
- P2-4 の最終状態を「公開 ALB + CloudFront origin-facing managed prefix list」から「内部 ALB + CloudFront VPC origin」へ差し替える
- Phase 2 全体の方針にも、ALB を CloudFront から private 到達させる意思を明記する
- `EdgeStack` の ALB を `internetFacing: false` へ変更する方針を固定する
- CloudFront origin を VPC origin 化する方針を固定する
- `publicSubnets` 前提の ALB 配置をやめ、app private subnet へ配置する方針にする
- CloudFront から ALB、ALB から ECS task は HTTP のまま維持する
- `npm run build`、`npm test -- --runInBand`、`npm run cdk -- synth EdgeStack` を実装時確認に含める
- CloudFormation テンプレートで `AWS::CloudFront::VpcOrigin`、内部 ALB、CloudFront distribution origin の VPC origin 設定を確認する
- 実 AWS では CloudFront default domain から `/actuator/health` に到達し、ALB DNS 直アクセスを正規入口にしないことを確認する

## 前提

- P2-4-01 で CloudFront distribution、default behavior、`cloudFrontDomainName`、`cloudFrontHttpsUrl` の基本構成が定義されている
- P2-4-02 で公開 ALB + CloudFront origin-facing managed prefix list による origin 制限が定義されている
- P2-4-03 で CloudFront default domain の HTTPS 到達確認手順が定義されている
- P2-4-04 で P2-4 の確認結果と cleanup が定義されている
- `FoundationStack` は public subnet と app private subnet を持つ
- `AppRuntimeStack` の ECS Fargate task は app private subnet に配置されている
- P2-4-05 は P2-4-01 / P2-4-02 の公開 ALB 前提を最終状態として残さない

## 案

- `EdgeStackProps` は `publicSubnets` ではなく `appSubnets` を受け取る
- `EdgeStack` の `ApplicationLoadBalancer` は `internetFacing: false` にする
- `EdgeStack` の ALB は app private subnet に配置する
- `EdgeStack` から CloudFront origin-facing managed prefix list の固定 ID `pl-58a04531` 参照を削除する
- `EdgeStack` から `AlbHttpIngress` の prefix list ingress を削除する
- CloudFront origin は `LoadBalancerV2Origin` ではなく `VpcOrigin.withApplicationLoadBalancer` で作る CloudFront VPC origin を使う
- CloudFront default behavior の allowed methods、cached methods、cache policy、origin request policy、viewer protocol policy は P2-4-01 / P2-4-02 の方針を維持する
- CloudFront から ALB は HTTP のままにする
- ALB から ECS task は HTTP のままにする
- `bin/cdk.ts` と CDK assertion test は `EdgeStack` の `appSubnets` 入力と CloudFront VPC origin 構成に合わせる
- `albDnsName` Output は origin 調査用として残すが、正規確認 URL にはしない

## ADR

- CloudFront VPC origins を採用する。公開 ALB を CloudFront origin-facing managed prefix list で制限するより、ALB をインターネットへ直接公開しない構成を Phase 2 の HTTPS 入口の最終状態にできるため。
- ALB は `internetFacing: false` にする。P2-4 以降の正規入口は CloudFront default domain であり、ALB DNS 直アクセスを利用者向け入口にしないため。
- ALB は app private subnet に配置する。CloudFront VPC origin は VPC 内の private origin へ到達する目的で使い、公開 subnet に ALB を残さないため。
- CloudFront から ALB は HTTP のままにする。P2-4 の目的は viewer 向け HTTPS 入口の成立であり、CloudFront から ALB 間 HTTPS、ACM 独自ドメイン証明書、ALB HTTPS listener は Phase 2 の範囲外であるため。
- P2-4-01 / P2-4-02 の CloudFront behavior 方針は維持する。WorkOps は Spring Boot + Thymeleaf の動的 Web アプリであり、Cookie、query string、`Host` 以外の viewer headers を origin へ渡し、CloudFront caching は無効化するため。
- `P2-4-05` は任意改善ではなく P2-4 の最終状態を置き換える必須 task とする。P2-5 以降の Cognito Hosted UI callback URL は CloudFront default domain を正本として使い続けるため、HTTPS 入口の origin 境界を P2-4 内で確定する必要があるため。
- 既存データ移行と後方互換性は考慮しない。

## 対応範囲

- `EdgeStackProps` の subnet 入力を `publicSubnets` から `appSubnets` へ変更する
- `EdgeStack` の ALB を internal ALB にする
- `EdgeStack` の ALB を app private subnet へ配置する
- CloudFront origin-facing managed prefix list 参照と prefix list ingress を削除する
- CloudFront origin を `VpcOrigin.withApplicationLoadBalancer` に変更する
- CDK entrypoint の `EdgeStack` props を `appSubnets` に変更する
- CDK assertion test を CloudFront VPC origin、internal ALB、VPC origin 付き distribution の検証へ変更する
- P2-4 README / task 記録で CloudFront default domain を正規確認 URL として扱う

## 除外範囲

- 独自ドメイン
- Route 53 public hosted zone
- Route 53 Alias record
- DNS validation
- ACM 独自ドメイン証明書
- ALB HTTPS listener
- CloudFront から ALB 間 HTTPS
- ALB DNS 直アクセスを正規確認経路にすること
- CloudFront origin-facing managed prefix list を最終防御境界にすること
- WAF
- ALB Cognito 認証
- CloudFront custom header による origin 制限
- CloudFront Functions
- Lambda@Edge
- Cognito Hosted UI ログイン本体
- users 突合
- git commit

## 完了条件

- `EdgeStack` の ALB が `internetFacing: false` になっている
- `EdgeStack` の ALB が app private subnet に配置されている
- `EdgeStack` が CloudFront VPC origin を作成している
- CloudFront distribution の default behavior が VPC origin 経由で ALB を origin にしている
- CloudFront から ALB への origin protocol policy が HTTP only のままである
- CloudFront cache policy が `CACHING_DISABLED` のままである
- CloudFront origin request policy が `ALL_VIEWER_EXCEPT_HOST_HEADER` のままである
- ALB から ECS task への HTTP 8080 通信が維持されている
- CloudFront origin-facing managed prefix list ID `pl-58a04531` に依存していない
- ALB HTTP 80 ingress を CloudFront origin-facing managed prefix list で制限する設計が P2-4 の最終状態として残っていない
- `cloudFrontHttpsUrl` が P2-5 へ渡す HTTPS URL の正本として残っている
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

- `AWS::CloudFront::VpcOrigin` が存在する
- `AWS::ElasticLoadBalancingV2::LoadBalancer` が internal ALB として synth される
- ALB が app private subnet を参照する
- CloudFront distribution の origin が VPC origin 設定を持つ
- CloudFront default behavior の cache policy が `CACHING_DISABLED`
- CloudFront default behavior の origin request policy が `ALL_VIEWER_EXCEPT_HOST_HEADER`
- CloudFront origin-facing managed prefix list ID `pl-58a04531` がテンプレートに出ない
- ALB listener に Cognito authenticate action が存在しない
- WAF association が存在しない

ユーザー確認:

- CloudFront default domain の HTTPS URL から `/actuator/health` に到達できる
- ALB DNS 直アクセスを正規確認 URL として使わない
- Cognito callback URL と sign-out URL に使う値として `cloudFrontHttpsUrl` を確認する
- 確認後に `EgressStack`、`EdgeStack`、`AppRuntimeStack` を cleanup する

## 実装時の記録

### 実装方針

P2-4 の公開 ALB + managed prefix list 依存を、内部 ALB + CloudFront VPC Origin へ置き換えるため、EdgeStack 側を `publicSubnets` 依存から `appSubnets` 依存へ移行した。

- `EdgeStackProps` を `publicSubnets` から `appSubnets` に変更し、ALB 配置を app private subnet に固定した。
- ALB を `internetFacing: false` に変更した。
- CloudFront origin を `VpcOrigin.withApplicationLoadBalancer` に変更して、`AWS::CloudFront::VpcOrigin` を経由する構成にした。
- P2-4-01 / P2-4-02 で入れた prefix list 参照 (`pl-58a04531`) と `AlbHttpIngress` を削除した。
- CloudFront の既存の methods/policy 方針（ALLOW_ALL、CACHE_GET_HEAD、CACHING_DISABLED、ALL_VIEWER_EXCEPT_HOST_HEADER、REDIRECT_TO_HTTPS）は維持した。
- `albDnsName` は引き続き output し、正規入口は `cloudFrontHttpsUrl` のみとする。

### 変更ファイル

1. `infra/cdk/lib/edge-stack.ts`
2. `infra/cdk/bin/cdk.ts`
3. `infra/cdk/test/workops-app.test.ts`
4. `docs/implementation/phase2/agent-tasks/P2-4/P2-4-05-cloudfront-vpc-origin-refactor.md`

### 実装結果

- `EdgeStackProps` の `publicSubnets` を `appSubnets` に置換し、ALB 配置と `EdgeStack` 生成時の参照を更新した。
- ALB を internal に変更し、`VpcOrigin.withApplicationLoadBalancer` により CloudFront VPC Origin を作成する形へ更新した。
- prefix list 由来の ingress 制御を削除し、ALB Security Group ingress を増やさない構成へ変更した。
- テストを VPC Origin と internal ALB 構成前提へ更新した。

### 確認結果

- `npm run build`
- `npm test -- --runInBand`
- `npm run cdk -- synth EdgeStack`（`WORKOPS_STAGE=dev`）
- CDK assertions 確認
  - `AWS::CloudFront::VpcOrigin` 存在
  - `AWS::ElasticLoadBalancingV2::LoadBalancer` の `Scheme` が `internal`
  - `CloudFront::Distribution` の origin が `VpcOriginConfig` を持つ
  - `pl-58a04531` がテンプレート本文に残っていない
  - `cloudFrontDomainName` / `cloudFrontHttpsUrl` / `albDnsName` output が存在

### 残課題

なし。
