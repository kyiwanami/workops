# P2-4-01 CloudFront edge stack

## 目的

P2-3 で作成した `EdgeStack` を拡張し、CloudFront default domain による HTTPS 入口を作る。
P2-4 では独自ドメイン、ACM 独自ドメイン証明書、Route 53、ALB HTTPS listener を使わず、CloudFront default certificate で viewer 向け HTTPS を成立させる。

## ユーザー要求

- `docs/implementation/phase2/agent-tasks/P2-4/` に P2-4 の task Markdown を作成する
- P2-4 の task は4本構成にする
- 既に判断済みの内容は質問しない
- P2-4 では既存 `EdgeStack` を拡張し、新しい `CloudFrontStack` は作らない
- CloudFront は CDN 最適化ではなく、独自ドメインなしの HTTPS 入口として使う
- P2-4 runtime は P2-3 と同じく `SPRING_PROFILES_ACTIVE=local` のままにする
- Cognito Hosted UI、users 突合、callback / sign-out path 確定は P2-5 で扱う
- タイムゾーン対応は P2-8 に移す

## 前提

- P2-3 が完了している
- P2-3 の cleanup により `AppRuntimeStack`、`EdgeStack`、`EgressStack`、`DataStack` は削除済みである
- 維持対象の `FoundationStack`、`RegistryStack`、`LogsStack` は AWS dev に残っている
- `EdgeStack` は既に ALB、HTTP listener、target group、`albDnsName` Output を持っている
- P2-4 の AWS 実操作はユーザー確認として扱う

## 案

- 既存 `infra/cdk/lib/edge-stack.ts` に CloudFront distribution を追加する
- CloudFront origin は `EdgeStack` 内の ALB とする
- Viewer から CloudFront は HTTPS とする
- CloudFront への HTTP request は HTTPS へ redirect する
- CloudFront から ALB は HTTP とする
- ALB から ECS task は HTTP のままにする
- CloudFront default certificate を使う
- CloudFront custom domain は設定しない
- Price class は `PRICE_CLASS_200` とする
- Allowed methods は全 HTTP メソッドとする
- Cached methods は GET / HEAD とする
- Cache policy は `CACHING_DISABLED` とする
- Origin request policy は `ALL_VIEWER_EXCEPT_HOST_HEADER` とする
- `cloudFrontDomainName` Output を追加する
- `cloudFrontHttpsUrl` Output を追加する
- `albDnsName` Output は origin 調査用として残す

## ADR

- CloudFront は HTTPS 入口のために採用し、キャッシュ最適化は P2-4 の目的にしない。WorkOps は Spring Boot + Thymeleaf の動的業務アプリであり、利用者、権限、Cookie、query string に依存する画面を CloudFront でキャッシュしないため。
- `EdgeStack` に CloudFront を含める。Phase2 docs で `EdgeStack` が CloudFront distribution、ALB、listener、target group を扱うと定義済みであり、実行確認セッションの削除ライフサイクルも揃うため。
- 新しい `CloudFrontStack` は作らない。P2-4 の責務は edge 入口の拡張であり、Stack 境界を増やす判断は既存設計に反するため。
- `PRICE_CLASS_200` を使う。AWS dev は `ap-northeast-1` 前提で確認し、日本からの確認に必要な edge location を含めるため。
- Allowed methods は全 HTTP メソッドとする。P2-5 以降で同じ HTTPS 入口を Cognito callback と業務 Web 入口に使うため、P2-4 で GET / HEAD に閉じない。
- Cached methods は GET / HEAD とする。CloudFront の cached methods としては標準的な最小範囲にし、実際の caching は `CACHING_DISABLED` で無効化するため。
- `cloudFrontHttpsUrl` を Output にする。P2-5 へ渡すベース URL を `https://` 付きで固定し、転記時の誤りを避けるため。
- 本 task の CloudFront から公開 ALB へ HTTP で到達する構成は、P2-4-05 で CloudFront VPC origin 経由の内部 ALB 構成へ置き換える。
- 既存データ移行と後方互換性は考慮しない。

## 対応範囲

- `EdgeStack` に CloudFront distribution を追加する
- `EdgeStack` に CloudFront default domain の Output を追加する
- `EdgeStack` に `https://{cloudFrontDomainName}` 形式の Output を追加する
- CloudFront viewer protocol policy を HTTPS redirect にする
- CloudFront origin protocol policy を HTTP only にする
- CloudFront cache policy を `CACHING_DISABLED` にする
- CloudFront origin request policy を `ALL_VIEWER_EXCEPT_HOST_HEADER` にする
- CloudFront allowed methods を全 HTTP メソッドにする
- CloudFront cached methods を GET / HEAD にする
- CDK assertion test に CloudFront distribution、policy、Output の検証を追加する

## 除外範囲

- 新規 `CloudFrontStack`
- CloudFront custom domain
- Route 53 public hosted zone
- Route 53 Alias record
- DNS validation
- ACM 独自ドメイン証明書
- ALB HTTPS listener
- CloudFront から ALB 間 HTTPS
- WAF
- ALB Cognito 認証
- Cognito Hosted UI ログイン
- users 突合
- callback / sign-out path 確定
- タイムゾーン対応
- GitHub Actions workflow
- git commit

## 完了条件

- `EdgeStack` が CloudFront distribution を持つ
- CloudFront distribution が ALB を origin にしている
- CloudFront default domain を利用できる
- viewer protocol policy が HTTPS redirect になっている
- origin protocol policy が HTTP only になっている
- price class が `PRICE_CLASS_200` になっている
- allowed methods が全 HTTP メソッドになっている
- cached methods が GET / HEAD になっている
- cache policy が `CACHING_DISABLED` になっている
- origin request policy が `ALL_VIEWER_EXCEPT_HOST_HEADER` になっている
- `cloudFrontDomainName` Output が存在する
- `cloudFrontHttpsUrl` Output が存在する
- `albDnsName` Output が残っている

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

- `AWS::CloudFront::Distribution` が1つ存在する
- default behavior の viewer protocol policy が `redirect-to-https`
- allowed methods が `GET`、`HEAD`、`OPTIONS`、`PUT`、`PATCH`、`POST`、`DELETE`
- cached methods が `GET`、`HEAD`
- cache policy が CloudFront managed policy `CachingDisabled`
- origin request policy が CloudFront managed policy `AllViewerExceptHostHeader`
- origin protocol policy が `http-only`
- price class が `PriceClass_200`
- `cloudFrontDomainName` Output がある
- `cloudFrontHttpsUrl` Output がある
- `albDnsName` Output がある

ユーザー確認:

- P2-4-03 で `EdgeStack` を deploy する
- P2-4-03 で `cloudFrontHttpsUrl` Output を取得する
