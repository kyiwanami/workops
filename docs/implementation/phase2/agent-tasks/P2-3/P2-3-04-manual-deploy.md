# P2-3-04 手動 deploy / HTTP health 確認

## 目的

P2-3 で追加した Docker image と実行確認セッション Stack を使い、AWS dev 上で `apps/web` を手動 deploy して HTTP ALB 経由の `/actuator/health` を確認する。
AWS 実操作はユーザー確認として扱い、エージェント確認と混同しない。

## ユーザー要求

- Maven test を実行する
- Docker build を実行する
- ECR login / push を実行する
- `DataStack` を再deployして RDS、RDS master secret、DB接続SSM Parameter を作成し直す
- runtime deploy 順序は `EgressStack`、`EdgeStack`、`AppRuntimeStack` とする
- HTTP ALB 経由で `/actuator/health` を確認する
- AWS 実操作はユーザー確認として記録する

## 前提

- P2-3-01、P2-3-02、P2-3-03 が完了している
- P2-2 の維持対象 Stack が必要分 deploy 済みである
- `DataStack` は削除済みであるため、P2-3 runtime deploy の前に再deployする
- `DataStack` 再deploy後、RDS、RDS master secret、DB接続SSM Parameter が存在する
- AWS CLI の認証、ECR push、CDK deploy はユーザー確認として扱う
- AWS deploy 前に AWS account / region、CDK diff、既存Stack状態を確認する
- GitHub Actions deploy は使わない

## 案

- `apps/web` の Maven test を実行する
- `apps/web` の Docker image を `p2-3-manual` tag で build する
- `RegistryStack` の ECR repository `workops-${stage}-web` へ login する
- image を ECR へ push する
- AWS account / region を確認する
- deploy対象Stackの既存状態を確認する
- `DataStack`、`EgressStack`、`EdgeStack`、`AppRuntimeStack` の diff を確認する
- `DataStack` を deploy する
- `EgressStack` を deploy する
- `EdgeStack` を deploy する
- `AppRuntimeStack` を deploy する
- `EdgeStack` Output の `albDnsName` を確認する
- `http://<alb-dns-name>/actuator/health` を確認する

## ADR

- P2-3 の deploy は手動で行う。GitHub Actions OIDC deploy は P2-9 の責務であるため。
- AWS 実操作はユーザー確認として扱う。AWS 認証、課金対象リソース作成、ECR push、CDK deploy はユーザー資格情報と課金を伴うため。
- ECR image tag は `p2-3-manual` に固定する。P2-3 は手動 deploy であり、CI 由来 tag を使わないため。
- `DataStack` は runtime Stack より先に再deployする。ECS task の secrets 注入で参照する `/workops/${stage}/db/url` と `/workops/${stage}/db/master` が存在しないと web task が起動できないため。
- deploy 順序は `EgressStack`、`EdgeStack`、`AppRuntimeStack` とする。NAT、ALB、runtime の依存順に合わせるため。
- ALB DNS は `EdgeStack` の `albDnsName` Output から取得する。AWS Console だけに依存せず、手順上の取得元を固定するため。
- CDK deploy では `--require-approval never` を使わない。security group ingress や IAM 変更をユーザーが確認できるようにするため。
- CDK deploy 前に `aws sts get-caller-identity` と `cdk diff` を必ず実行する。削除済み `DataStack` の再作成、NAT Gateway、ALB、ECS、IAM、Security Group 変更を事前確認するため。
- ALB 80 ingress と ALB から app 8080 ingress は `EdgeStack` / `AppRuntimeStack` の `AWS::EC2::SecurityGroupIngress` として所有する。P2-3 の一時実行確認 Stack を destroy したときに、公開口とruntime接続ruleも同じライフサイクルで削除するため。
- CDK L2 の自動 Security Group 接続は一般には妥当だが、P2-3 では `FoundationStack` へ ingress rule を残さないことを優先する。課金対象の ALB / NAT / ECS と公開口を実行確認セッション終了時に閉じるため。
- `/actuator/health` だけを HTTP ALB 経由の成功確認にする。業務画面ログイン、Cognito Hosted UI、users 突合は P2-3 の責務ではないため。
- P2-3 の ECS runtime は `SPRING_PROFILES_ACTIVE=local` で起動する。Spring Boot の非local profileはCognito OAuth2 Client設定を起動時に要求するが、Cognito Hosted UI / users 突合はP2-3の責務ではないため。
- local profileでも RDS 接続は ECS secrets の `WORKOPS_DB_URL` / `WORKOPS_DB_USERNAME` / `WORKOPS_DB_PASSWORD` を使う。P2-3確認後に `DataStack` を削除する前提で、local seed が一時RDSへ入ることを許容する。
- 既存データ移行と後方互換性は考慮しない。

## 対応範囲

- 手動 deploy 手順を task に記録する
- Maven test 手順を記録する
- Docker build 手順を記録する
- ECR login / tag / push 手順を記録する
- AWS account / region / stack state / CDK diff の事前確認手順を記録する
- DataStack 再deploy手順を記録する
- CDK deploy 順序を記録する
- `albDnsName` Output の確認手順を記録する
- `/actuator/health` の HTTP 確認手順を記録する
- ECS service / task / target group health の確認手順を記録する

## 除外範囲

- GitHub Actions workflow
- CodeBuild
- CodePipeline
- main push 自動 deploy
- 本番 deploy
- HTTPS listener
- ACM certificate
- Route 53
- Cognito Hosted UI
- 業務画面ログイン確認
- users 突合
- seed 件数確認
- Cognito 用 dev profile 起動確認
- destroy 手順
- git commit

## 完了条件

- `apps/web` の Maven test が成功している
- Docker image を `p2-3-manual` tag で build できる
- ECR repository `workops-${stage}-web` に `p2-3-manual` tag を push できる
- `DataStack` を再deployできる
- `EdgeStack` が `albDnsName` Output を持つ
- `EgressStack` を deploy できる
- `EdgeStack` を deploy できる
- `AppRuntimeStack` を deploy できる
- ECS Service が task を起動できる
- Target Group が healthy になる
- HTTP ALB 経由で `/actuator/health` が HTTP 200 を返す

## 確認方法

エージェント確認:

```powershell
cd C:\git\workops\apps\web
.\mvnw.cmd test
docker build -t workops-web:p2-3-manual .

cd C:\git\workops\infra\cdk
npm run build
npm test
$env:WORKOPS_STAGE='dev'
npm run cdk -- synth
```

ユーザー確認:

```powershell
$env:WORKOPS_STAGE='dev'
$env:AWS_REGION='<aws-region>'
$env:AWS_ACCOUNT_ID='<aws-account-id>'

aws ecr get-login-password --region $env:AWS_REGION |
  docker login --username AWS --password-stdin "$env:AWS_ACCOUNT_ID.dkr.ecr.$env:AWS_REGION.amazonaws.com"

docker tag workops-web:p2-3-manual "$env:AWS_ACCOUNT_ID.dkr.ecr.$env:AWS_REGION.amazonaws.com/workops-dev-web:p2-3-manual"
docker push "$env:AWS_ACCOUNT_ID.dkr.ecr.$env:AWS_REGION.amazonaws.com/workops-dev-web:p2-3-manual"

cd C:\git\workops\infra\cdk
aws sts get-caller-identity
aws configure get region
aws cloudformation describe-stacks --stack-name workops-dev-foundation
aws cloudformation describe-stacks --stack-name workops-dev-registry
aws cloudformation describe-stacks --stack-name workops-dev-logs
# workops-dev-data は削除済み前提のため、ValidationError の場合は想定どおり。
aws cloudformation describe-stacks --stack-name workops-dev-data

npm run cdk -- diff DataStack
npm run cdk -- diff EgressStack
npm run cdk -- diff EdgeStack
npm run cdk -- diff AppRuntimeStack

npm run cdk -- deploy DataStack
npm run cdk -- deploy EgressStack
npm run cdk -- deploy EdgeStack --outputs-file .\p2-3-04-outputs.json
npm run cdk -- deploy AppRuntimeStack
```

HTTP health 確認:

```powershell
$outputs = Get-Content -LiteralPath .\p2-3-04-outputs.json -Encoding UTF8 | ConvertFrom-Json
$albDnsName = $outputs.EdgeStack.albDnsName
Invoke-WebRequest -Uri "http://$albDnsName/actuator/health" -UseBasicParsing
```

AWS Console 確認:

- ECR repository `workops-dev-web` に `p2-3-manual` tag がある
- CloudFormation Stack `workops-dev-data` が作成済み
- RDS instance `workops-dev-db` が作成済み
- Secrets Manager secret `/workops/dev/db/master` がある
- SSM Parameter `/workops/dev/db/url` がある
- CloudFormation Stack `workops-dev-egress` が作成済み
- CloudFormation Stack `workops-dev-edge` が作成済み
- CloudFormation Stack `workops-dev-app-runtime` が作成済み
- ECS Service が `desiredCount=1` である
- ECS task が `RUNNING` である
- Target Group target が healthy である
- CloudWatch Logs `/workops/dev/web` に Spring Boot 起動ログが出ている

---

## 実装時の記録

### 実装方針

`EdgeStack` に ALB DNS name の CloudFormation Output を追加し、P2-3-04 のHTTP health確認で参照する値を固定する。
`DataStack` は削除済みのため、ECR push後、runtime Stack deploy前に再deployする手順へ変更する。
AWS実操作はユーザー確認として扱い、エージェントはローカル検証と手順整備までを行う。

### 変更ファイル

- `infra/cdk/lib/edge-stack.ts`
- `infra/cdk/test/workops-app.test.ts`
- `docs/implementation/phase2/agent-tasks/P2-3/P2-3-04-manual-deploy.md`

### 実装結果

- `EdgeStack` に `albDnsName` Output を追加した。
- CDK assertion で `albDnsName` Output の存在を検証するようにした。
- P2-3-04 の手順に `DataStack` 再deployを追加した。
- AWS deploy 前の account / region / stack state / CDK diff 確認を追加した。
- `EdgeStack` deploy時に `--outputs-file .\p2-3-04-outputs.json` を使い、OutputからALB DNSを取得する手順にした。
- ALB 80 ingress と ALB から app 8080 ingress は `EdgeStack` / `AppRuntimeStack` が所有する方針を明記した。
- `FoundationStack` へ実行確認用 ingress rule を残さない理由を、P2-3一時Stackの削除ライフサイクルと課金停止の観点で明記した。
- `AppRuntimeStack` の ECS runtime は `SPRING_PROFILES_ACTIVE=local` で起動する方針にした。P2-3ではCognitoを構築せず、HTTP health / RDS / Flyway確認に限定するため。

### AWS deploy中の確認

- `AppRuntimeStack` の初回deployでは `SPRING_PROFILES_ACTIVE=dev` のまま起動し、Spring Boot OAuth2 Client設定で `Client id of registration 'cognito' must not be empty.` が発生した。
- ECS deployment circuit breaker が発動し、`workops-dev-app-runtime` は `ROLLBACK_COMPLETE` になった。
- ECS Service は `DRAINING`、desired / running / pending count はすべて `0` になり、起動ループは停止した。
- DB接続、HikariPool起動、Flyway `V1` から `V8` の確認はログ上成功していたため、失敗原因はRDS / NAT / ECR / secret injectionではなくCognito OAuth2 Client設定不足と判断した。
- P2-3ではCognito Hosted UI / users突合を扱わないため、ECS runtimeをlocal profileへ変更して再deployする。
- `dev` profileで一度Flyway履歴とaws-dev seedが入ったRDSへlocal profileを再適用するとmigration履歴が競合するため、`AppRuntimeStack` を削除後に `DataStack` を削除し、RDSを空の状態で再作成した。
- `DataStack` 再作成後、`SPRING_PROFILES_ACTIVE=local` の `AppRuntimeStack` をdeployし直した。
- `workops-dev-app-runtime` は `CREATE_COMPLETE` になった。
- ECS Service `workops-dev-web` は desired `1`、running `1`、pending `0`、rollout `COMPLETED` になった。
- Target Group `workops-dev-web-tg` の target は `healthy` になった。
- `http://workops-dev-web-alb-1694317515.ap-northeast-1.elb.amazonaws.com/actuator/health` は HTTP `200` を返した。
- response body は `{"groups":["liveness","readiness"],"status":"UP"}` だった。
- CloudWatch Logs `/workops/dev/web` で active profile `local`、RDS接続、空schemaからFlyway `V1` から `V8` の適用、Tomcat 8080起動を確認した。
- ユーザーがブラウザからWeb画面へ到達できることを確認した。
- コンテナ / JVM の時刻表示は UTC に見える状態だった。P2-3ではhealth / ECS / ALB / RDS疎通確認を優先し、`TZ=Asia/Tokyo` や `JAVA_TOOL_OPTIONS=-Duser.timezone=Asia/Tokyo` の採否はP2-4以降で判断する。

### 確認結果

- `cd C:\git\workops\apps\web; .\mvnw.cmd test`
  - 結果: 成功
  - Tests run: 193, Failures: 0, Errors: 0, Skipped: 0
- `cd C:\git\workops\apps\web; docker build -t workops-web:p2-3-manual .`
  - 結果: 成功
  - Docker build 内で `./mvnw -DskipTests package` が実行され、`workops-web:p2-3-manual` image を作成できた。
- `cd C:\git\workops\infra\cdk; npm run build`
  - 結果: 成功
- `cd C:\git\workops\infra\cdk; npm test`
  - 結果: 成功
  - Test Suites: 1 passed
  - Tests: 12 passed
- `cd C:\git\workops\infra\cdk; $env:WORKOPS_STAGE='dev'; npm run cdk -- synth`
  - 結果: 成功
  - `albDnsName` Output を含む `EdgeStack` をsynthできた。

### 残課題

- P2-3-05で実行確認セッション終了後に `AppRuntimeStack`、`EdgeStack`、`EgressStack` をdestroyする。
- RDS課金を止める場合は、P2-3-05で `DataStack` もdestroyする。
