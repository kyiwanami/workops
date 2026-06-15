# P2-3-05 検証 / cleanup / 記録

## 目的

P2-3 の AWS dev 実行確認結果を整理し、CloudWatch Logs、RDS 上の Flyway / seed、実行確認セッション終了後の Stack 削除を確認する。
確認結果は task Markdown と必要な README に記録し、エージェント確認とユーザー確認を分ける。

## ユーザー要求

- CloudWatch Logs を確認する
- `flyway_schema_history` を確認する
- seed 件数を確認する
- destroy 順序は `AppRuntimeStack`、`EdgeStack`、`EgressStack` とする
- P2-3 の一時実行確認で再deployした `DataStack` の扱いを明示する
- README / task に記録する
- `/workops/dev/migration` は P2-3 では使わない

## 前提

- P2-3-04 で AWS dev への手動 deploy が完了している
- HTTP ALB 経由で `/actuator/health` が HTTP 200 を返している
- ECS task が `RUNNING` で Target Group が healthy である
- RDS Console integrated CloudShell VPC による DB 接続確認はユーザー確認として扱う
- P2-3-04 で `DataStack` を再deployしている

## 案

- CloudWatch Logs `/workops/dev/web` を確認する
- `apps/web` 起動時 Flyway の成功ログを `/workops/dev/web` で確認する
- `/workops/dev/migration` は P2-3 では使わないことを確認する
- RDS Console integrated CloudShell VPC から MySQL に接続する
- `flyway_schema_history` で `V1` から `V8` の success を確認する
- local seed の主要件数を確認する
- 実行確認セッション終了後に `AppRuntimeStack`、`EdgeStack`、`EgressStack` の順で destroy する
- RDS 課金を止める場合は、P2-3 の一時実行確認で再deployした `DataStack` も destroy する
- P2-3 の確認結果と P2-4 への引き継ぎを記録する

## ADR

- Flyway / seed 確認は P2-3 で行う。P2-2 では AppRuntimeStack を作らず、`apps/web` 起動時 Flyway 適用は P2-3 の責務であるため。
- Flyway ログは `/workops/dev/web` に出す。P2-3 では migration 用 ECS Task を作らず、`apps/web` 起動時 Flyway を維持するため。
- `/workops/dev/migration` は P2-3 では使わない。migration 用 ECS Task 分離は Phase 2α の責務であるため。
- destroy 順序は `AppRuntimeStack`、`EdgeStack`、`EgressStack` とする。ECS service、ALB、NAT の依存関係に合わせるため。
- `EdgeStack` / `AppRuntimeStack` が所有する ALB 80 ingress と ALB から app 8080 ingress は、各 Stack destroy で削除する。実行確認セッション終了後に公開口とruntime接続ruleを残さないため。
- `AppRuntimeStack` は `desiredCount=0` で残さない。P2-3 の実行確認セッション終了後に削除する方針であるため。
- `DataStack` はRDSを含むため、残すとRDS課金が継続する。P2-3確認後に課金を止める目的では `DataStack` も destroy 対象にする。
- `DataStack` destroy は P2-3-04 で再deployした検証用RDSを削除する操作として扱う。既存データ移行と後方互換性は考慮しない。
- 既存データ移行と後方互換性は考慮しない。

## 対応範囲

- CloudWatch Logs 確認手順を記録する
- RDS 上の Flyway / seed 確認 SQL を記録する
- destroy 手順を記録する
- `DataStack` を残す場合と削除する場合の課金差分を記録する
- P2-3 のエージェント確認結果を記録する
- P2-3 のユーザー確認結果を記録する
- root README または `infra/cdk/README.md` に必要な恒久手順だけを反映する
- Phase 2 固有の判断、検証結果、詳細SQLは task Markdown に置く

## 除外範囲

- 新規 CDK Stack 実装
- Spring Boot 実装変更
- Dockerfile 変更
- GitHub Actions workflow
- CodeBuild
- CodePipeline
- HTTPS listener
- ACM certificate
- Route 53
- Cognito Hosted UI
- 業務画面ログイン確認
- users 突合
- migration 用 ECS Task
- CloudWatch Alarm
- Observability Dashboard
- git commit

## 完了条件

- `/workops/dev/web` に ECS app log が出ている
- `/workops/dev/web` で Flyway 起動ログを確認できる
- `/workops/dev/migration` を P2-3 で使っていない
- `flyway_schema_history` で `V1` から `V8` が success になっている
- local seed の主要件数が期待値どおりである
- `AppRuntimeStack` を destroy できる
- `EdgeStack` を destroy できる
- `EgressStack` を destroy できる
- RDS課金を止める場合、`DataStack` を destroy できる
- P2-3 の確認結果が task Markdown に記録されている
- P2-4 への引き継ぎ事項が記録されている

## 確認方法

エージェント確認:

```powershell
cd C:\git\workops\infra\cdk
npm run build
npm test
$env:WORKOPS_STAGE='dev'
npm run cdk -- synth

cd C:\git\workops\apps\web
.\mvnw.cmd test
```

ユーザー確認:

CloudWatch Logs:

- Log group `/workops/dev/web`
- stream prefix `web`
- Spring Boot 起動ログ
- Flyway migration 成功ログ
- `/actuator/health` の応答に関する異常がないこと

RDS Console integrated CloudShell VPC から MySQL に接続して確認する:

```sql
SELECT installed_rank, version, description, success
FROM flyway_schema_history
ORDER BY installed_rank;

SELECT version, description, success
FROM flyway_schema_history
WHERE version IN ('1', '2', '3', '4', '5', '6', '7', '8')
ORDER BY installed_rank;

SELECT COUNT(*) AS local_cognito_sub_count
FROM users
WHERE cognito_sub IS NOT NULL;

SELECT COUNT(*) AS companies_count FROM companies;
SELECT COUNT(*) AS users_count FROM users;
SELECT COUNT(*) AS permission_sets_count FROM permission_sets;
SELECT COUNT(*) AS user_permission_sets_count FROM user_permission_sets;
SELECT COUNT(*) AS assets_count FROM assets;
SELECT COUNT(*) AS requests_count FROM requests;
```

期待値:

- `flyway_schema_history`: `V1` から `V8` が success
- `local_cognito_sub_count`: `7`
- `companies`: `2`
- `users`: `7`
- `permission_sets`: `4`
- `user_permission_sets`: `7`
- `assets`: `15`
- `requests`: `16`

destroy:

```powershell
cd C:\git\workops\infra\cdk
$env:WORKOPS_STAGE='dev'
npm run cdk -- destroy AppRuntimeStack
npm run cdk -- destroy EdgeStack
npm run cdk -- destroy EgressStack
```

RDS課金を止める場合は、実行確認セッション終了後に `DataStack` も削除する:

```powershell
cd C:\git\workops\infra\cdk
$env:WORKOPS_STAGE='dev'
npm run cdk -- destroy DataStack
```

削除後に確認する:

- `workops-dev-app-runtime` が削除済み
- `workops-dev-edge` が削除済み
- `workops-dev-egress` が削除済み
- `DataStack` を削除した場合、`workops-dev-data` が削除済み
- `FoundationStack` に P2-3 実行確認用の ALB 80 ingress と ALB から app 8080 ingress が残っていない
- `DataStack` を残した場合、RDS課金が継続する

---

## 実施時の記録

### P2-3-04からの引き継ぎ

- `AppRuntimeStack` は `SPRING_PROFILES_ACTIVE=local` でdeploy済み。
- `workops-dev-app-runtime` は `CREATE_COMPLETE`。
- ECS Service `workops-dev-web` は desired `1`、running `1`、pending `0`、rollout `COMPLETED`。
- Target Group target は `healthy`。
- HTTP ALB経由 `/actuator/health` は HTTP `200`。
- health response body は `{"groups":["liveness","readiness"],"status":"UP"}`。
- CloudWatch Logs `/workops/dev/web` で active profile `local`、RDS接続、Flyway `V1` から `V8` 適用、Tomcat 8080起動を確認済み。
- ユーザーがブラウザからWeb画面へ到達できることを確認済み。
- コンテナ / JVM の時刻表示は UTC に見える。P2-4以降で `TZ=Asia/Tokyo` または `JAVA_TOOL_OPTIONS=-Duser.timezone=Asia/Tokyo` の採否を決める。
- P2-3ではCognito Hosted UI / users突合を扱わないため、local profileでのECS起動確認に切り替えた。
- local seedが一時RDSに入っているため、P2-3確認後にRDS課金を止める場合は `DataStack` もdestroyする。
