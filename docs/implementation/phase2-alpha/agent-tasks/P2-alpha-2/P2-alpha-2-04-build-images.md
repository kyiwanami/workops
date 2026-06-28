# P2-alpha-2-04 Build Images

## 目的

Web image を CodeBuild で build / Trivy scan / ECR push する Build Images stage を作る。

DB migration は image build 対象から外し、RDS と同じ Application Stage 側に置く VPC attached CodeBuild project で実行する。

## ユーザー要求

- P2-alpha-2 の task 分割は、`phases.md` の責務境界を最上位単位にする
- 既にコード、Notion、`phases.md` で分かることは質問しない
- 必要な場合は公式ドキュメントやベストプラクティスを確認する
- task ファイル末尾に `実装時の記録`、`実装結果`、`確認結果`、`残課題` を置く

## 前提

- P2-alpha-2-01 が完了している
- P2-alpha-2-02 が完了している
- P2-alpha-2-03 が完了している
- Build Images stage 前に RegistryStack が deploy される
- Web image は commit SHA tag を使う
- migration は Pipeline source artifact 内の同一 commit の SQL を使う
- cache tag は `buildcache` に固定する
- Trivy は image scan のみに限定し、MEDIUM / HIGH / CRITICAL で fail する
- migration CodeBuild は RDS と同じ VPC / private app subnet / migration security group で実行する

## 案

Build Images stage に次の CodeBuild project を定義する。

- BuildWebImage

migration image は作らない。

push する image は次にする。

```text
workops-${stage}-web:${commitSha}
```

cache image は次にする。

```text
workops-${stage}-web-cache:buildcache
```

Docker buildx registry cache は `--cache-from` と `--cache-to type=registry,mode=max` を使う。
初回 cache miss 用の事前存在確認、分岐、特別処理は作らない。

CodeBuild image は `aws/codebuild/amazonlinux-aarch64-standard:3.0` を使う。
CodeBuild compute は ARM_64 / Graviton 系 Medium とする。
Docker build と Trivy image scan のため `privileged: true` とする。

Build Images stage の合否判定は docker build、Trivy image scan、docker push の結果で行う。
ECR Enhanced Scanning は ECR 側設定として扱い、build gate にはしない。

Migration は次の形で実行する。

```text
CodePipeline Source artifact
  -> Migration CodeBuild action
       -> workops-${stage}-migration project
       -> VPC private app subnet から RDS MySQL へ Flyway migrate
```

Migration CodeBuild project は Application Stage 側の MigrationStack が作る。
PipelineStack は Stage 内 VPC を直接参照せず、固定 project name `workops-${stage}-migration` を CodeBuild action から参照する。

## 参考

- AWS CodeBuild の Docker layer cache は Linux 環境かつ privileged mode が必要で、privileged mode のセキュリティ影響を考慮する必要がある。
- `phases.md` では CodeBuild VPC mode を使わないため、Docker 実行は非 VPC build 前提で設計する。

## 判断メモ

- Web image と migration image を分ける。Web app 起動と DB migration の責務を分離するため。
- 同一 commit SHA tag を使う。Pipeline run の app / migration 対応関係を明確にするため。
- `WORKOPS_WEB_IMAGE_TAG` 単独運用は廃止する。Phase 2α では web / migration の tag を明示的に扱うため。
- Build Images 前に ECR repository の個別存在確認 step は置かない。RegistryStack deploy と push 結果で検出できるため。
- 2026-06-24: migration image / ECS one-shot task は廃止する。CLI実行だけにECS task definition / ECR image / image scan gateを持つより、CodeBuild jobでPipeline制御する方が軽い。
- 2026-06-24: S3 handoff と `start-build --source-location-override` は採用しない。CodePipeline の標準 artifact 連携では、Source artifact を CodeBuild action の input artifact として直接渡せるため。
- 2026-06-24: Migration CodeBuild project は RDS と同じ target 環境側に置く。VPC / subnet / SG は Application Stage の FoundationStack が所有し、CodeBuild project はその network を使う。
- 2026-06-24: migration 用 security group は FoundationStack が所有する。DB ingress は RDS を所有する DataStack で `app -> db` と `migration -> db` を定義し、MigrationStack から FoundationStack の DB SG を後付け変更しない。

## 対応範囲

- Web image build 用 buildspec
- PipelineStack の Build Images stage
- CodeBuild project 定義
- ECR login / buildx builder 作成 / cache 設定
- Trivy image scan
- commit SHA tag の受け渡し
- MigrationStack の VPC attached CodeBuild project
- PipelineStack の migration CodeBuild action
- `infra/cdk/test/`
- 本 task ファイルの実装結果・確認結果追記

## 除外範囲

- Build & Test stage
- Migration RunTask
- AppRuntimeStack deploy
- ECR repository 作成
- AWS deploy
- 実 ECR push
- Trivy filesystem scan
- Trivy DB cache 専用の永続 cache
- ECR Enhanced Scanning を Pipeline gate にすること

## 完了条件

- BuildWebImage が CodeBuild project として定義されている
- migration ECR repository / migration cache repository が作られない
- BuildMigrationImage が Pipeline に存在しない
- web image が commit SHA tag を使う構成になっている
- registry cache が `buildcache` tag で定義されている
- buildx registry cache の `--cache-from` / `--cache-to` が buildspec に含まれている
- Trivy image scan が MEDIUM / HIGH / CRITICAL で fail する
- `WORKOPS_WEB_IMAGE_TAG` 単独運用が Pipeline 側に残っていない
- Migration CodeBuild project が Source `CODEPIPELINE`、VPC config、Secrets Manager / SSM env を持つ
- Pipeline の RunMigration action が Source artifact を直接 input artifact として渡す
- S3 handoff bucket / `start-migration-build.py` / ECS RunTask script が存在しない

## 確認方法

```powershell
cd C:\git\workops\infra\cdk
npm run test
npm run lint
npm run format:check
$env:WORKOPS_STAGE = "dev"
$env:CDK_DEFAULT_ACCOUNT = "000000000000"
$env:CDK_DEFAULT_REGION = "ap-northeast-1"
npm run cdk:pipeline -- synth
```

## 実装時の記録

- 2026-06-23: `PipelineStack` に `BuildImages` wave を追加し、`BuildWebImage` と `BuildMigrationImage` を同一 stage / 同一 run order の CodeBuild step として定義した。
- 2026-06-23: image tag は CodePipeline source の `CommitId` を `COMMIT_SHA` として渡し、web / migration 両方で同一 commit SHA tag を使う構成にした。
- 2026-06-23: buildx の registry cache は web / migration それぞれの cache repository に `buildcache` tag で push し、`mode=max` を使う構成にした。
- 2026-06-23: Trivy は `public.ecr.aws/aquasecurity/trivy:0.71.2` の container image を使い、MEDIUM / HIGH / CRITICAL を `--exit-code 1` で gate する構成にした。
- 2026-06-23: P2-alpha-2 の範囲どおり、AWS deploy / Pipeline 実走 / 実 ECR push は実施していない。
- 2026-06-23: P2-alpha-3 の Pipeline 実走で、Build Images stage の `docker buildx build` が web / migration ともに `invalid tag "...:$COMMIT_SHA"` で失敗した。原因は `IMAGE_URI` の CodeBuild environment variable に `:$COMMIT_SHA` を含め、env 定義内で別 env が展開される前提にしていたこと。CodeBuild の env は値の受け渡しに限定し、CodePipeline source variable の `CommitId` は `source.sourceAttribute('CommitId')` で `COMMIT_SHA` として渡し、image URI は build command 内で `export IMAGE_URI="$IMAGE_REPOSITORY_URI:$COMMIT_SHA"` として組み立てる方針に修正する。
- 2026-06-23: P2-alpha-3 の Pipeline 再実走で、web image build は成功したが migration image の Trivy image scan が MEDIUM / HIGH findings により失敗した。ローカルには Docker があるにもかかわらず、実装時確認が CDK build / test / lint / format と synth に偏り、Build Images の実 Docker build と Trivy scan をローカルで実行していなかったことが検出漏れ原因。Build Images 変更時は Pipeline 実走前に web / migration 両 image の `docker build` と Trivy `--exit-code 1 --severity MEDIUM,HIGH,CRITICAL` を必須確認にする。
- 2026-06-23: migration image は Flyway の web app 分離と ECS one-shot migration task を維持し、公式 Flyway CLI image だけを廃止する判断にした。Lambda は RDS 接続可能な代替だが、15分実行上限、Lambda Runtime Interface 前提、CloudFormation Custom Resource との結合リスクがあるため、P2-alpha-2 の現在構成では採用しない。
- 2026-06-23: `flyway/flyway:12.9.0-alpine` は汎用 CLI image として WorkOps の MySQL migration に不要な DB driver / 依存を含むため、`flyway-core`、`flyway-mysql`、`mysql-connector-j` だけを持つ Spring なしの Java runner に置き換える。
- 2026-06-23: Trivy gate は MEDIUM / HIGH / CRITICAL のまま維持し、`--ignore-unfixed`、`.trivyignore`、VEX suppression では通さない。
- 2026-06-24: ECS one-shot migration task と migration image は廃止し、Flyway migration は VPC attached CodeBuild project で実行する判断に変更した。CLI実行だけのためにECS task definition / ECR repository / image scan対象を増やすより、CodePipeline の CodeBuild action として実行し、失敗時に Pipeline を止める方が単純なため。
- 2026-06-24: Migration CodeBuild project は Application Stage 側の MigrationStack が作る。RDS と同じ target 環境側に置き、FoundationStack の VPC / private app subnet / migration security group を使う。
- 2026-06-24: Pipeline artifact は `Source artifact -> CodeBuild action input artifact -> build container` の標準連携を使う。S3 handoff bucket、`source-location-override`、`start-build` 自作起動は、どの commit の SQL を実行したかの設計が別途必要になるため採用しない。
- 2026-06-24: migration 用 security group は FoundationStack が所有し、RDS への ingress は DataStack が定義する。MigrationStack から DB security group を後付け変更しない。
- 2026-06-24: Maven runner / Java API runner は過剰なので撤回する。Flyway 自体は JVM 系ツールであり、公式 CLI でも内部的に Java は残る。ただし、Maven package、独自 runner、Spring、migration image、ECS task を migration 実行のために増やす必要はない。ここを曖昧にすると、また重い実行単位や不要な脆弱性 gate を増やすため、CodeBuild では固定 version の Redgate Flyway CLI を直接実行する。
- 2026-06-24: Redgate Flyway CLI の Linux 配布物は x64 を使うため、migration CodeBuild は ARM image ではなく x86_64 の `LinuxBuildImage.AMAZON_LINUX_2023_5` を使う。BuildWebImage は ARM build のままでよいが、migration 実行側まで ARM に寄せない。
- 2026-06-24: 根拠は Redgate Flyway CLI docs、Redgate Flyway Maven plugin docs、AWS の CodePipeline + CodeBuild + Secrets Manager によるDB deployment記事。CLI docs は「build toolに統合せずcommand-lineからmigrateする用途」を説明しており、今回の CodeBuild migration には Maven plugin / Java API runner より CLI 直接実行を優先する。
- 2026-06-24: Pipeline 実走で `RunMigration` が `test -d apps/web/src/main/resources/db/migration` に失敗した。WorkOps の SQL 正本は Spring resources 配下ではなく repository root の `db/migration`、`db/seed/common`、`db/seed/aws-dev` である。Source artifact を使う設計は正しいが、CodeBuild buildspec の locations が誤っていたため、`filesystem:db/...` へ修正する。

### 実装結果

- `infra/cdk/lib/pipeline-stack.ts`
  - `BuildImages` wave を `DeployRegistry` stage の後に追加し、`BuildWebImage` だけを CodeBuild project として定義した。
  - ECR login、buildx builder 作成、buildx build、Trivy image scan、docker push の command を CodeBuildStep に定義した。
  - ECR push / pull 用 IAM policy を web image repository と web cache repository の ARN に限定して追加した。
  - GitHub source artifact を明示的に作り、Synth / BuildAndTest / BuildWebImage / RunMigration が同じ Source artifact を使う構成にした。
  - `RunMigration` は CDK Pipelines の `CodeBuildStep` ではなく、低レベル CodePipeline の `CodeBuildAction` として追加した。
  - `RunMigration` は固定 project name `workops-${stage}-migration` を参照し、PipelineStack から Stage 内 VPC を直接参照しない。
- `infra/cdk/test/workops-app.test.ts`
  - `BuildImages` stage、commit SHA env、buildx registry cache、Trivy gate、repository 名、Dockerfile、context、ECR IAM policy の検証を追加した。
  - `WORKOPS_WEB_IMAGE_TAG`、`:latest`、`:dev` が PipelineStack template に出ないことを検証した。
  - migration ECR repository / migration cache repository / BuildMigrationImage / ECS RunTask script / S3 handoff bucket が template に残らないことを検証した。
  - Migration CodeBuild project が Source `CODEPIPELINE`、VPC config、Secrets Manager / SSM env を持つことを検証した。
  - Pipeline の `RunMigration` action が Source artifact を input artifact として渡すことを検証した。
- `infra/cdk/lib/foundation-stack.ts`
  - FoundationStack に `workops-${stage}-migration-sg` を追加し、migration CodeBuild の outbound 用 SG として公開した。
- `infra/cdk/lib/data-stack.ts`
  - RDS security group に、migration security group から MySQL 3306 を許可する ingress を追加した。
- `infra/cdk/lib/migration-stack.ts`
  - ECS task definition / container image input を廃止し、VPC attached `PipelineProject` を作成する構成にした。
  - `WORKOPS_DB_URL` は SSM Parameter Store、`WORKOPS_DB_USERNAME` / `WORKOPS_DB_PASSWORD` は Secrets Manager から CodeBuild env として渡す。
  - Source は `CODEPIPELINE` とし、Pipeline action の input artifact 内 SQL を使う。
  - Flyway は Maven runner ではなく Redgate Flyway CLI `12.9.0` を CodeBuild 内で取得して直接 `flyway migrate` を実行する。
  - secrets は CLI 引数へ直書きせず、`FLYWAY_URL`、`FLYWAY_USER`、`FLYWAY_PASSWORD`、`FLYWAY_LOCATIONS` に export して渡す。
  - migration CodeBuild は Flyway CLI Linux x64 配布物に合わせ、x86_64 の Amazon Linux 2023 standard image を使う。
  - SQL locations は repository root の `db/migration`、`db/seed/common`、`db/seed/aws-dev` を使う。
- `infra/cdk/lib/registry-stack.ts`
  - migration repository と migration cache repository を削除し、web repository と web cache repository のみを作成する構成にした。
- `compose.yaml`
  - 削除した migration Dockerfile を参照しないようにし、local migration は Maven image / 独自 runner を使わず Redgate Flyway CLI image で実行する構成にした。
- 削除したもの
  - `infra/docker/migration/Dockerfile`
  - `infra/docker/migration/entrypoint.sh`
  - `infra/cdk/scripts/run-migration-task.py`
  - `infra/cdk/scripts/start-migration-build.py`
  - `infra/migration-runner`

### 確認結果

- `cd C:\git\workops\infra\cdk; npm run build`
  - 成功。
- `cd C:\git\workops\infra\cdk; npm run test`
  - 成功。2 test suites / 22 tests passed。
- `cd C:\git\workops\infra\cdk; npm run lint`
  - 成功。
- `cd C:\git\workops\infra\cdk; npm run format:check`
  - 成功。
- AWS credential 環境変数を削除し、dummy account / dummy email で `npm run cdk:pipeline -- synth --quiet`
  - 成功。
- 2026-06-23 P2-alpha-3 Pipeline 実走:
  - `DeployRegistry` まで成功。
  - `BuildImages` で web / migration ともに `docker buildx build` が失敗。
  - 失敗理由は Trivy scan ではなく、Docker tag に literal `$COMMIT_SHA` が残ったことによる `invalid reference format`。
  - 修正後は `npm run build`、`npm run test -- --runInBand`、`npm run lint`、`npm run format:check` 成功。
- 2026-06-23 P2-alpha-3 Pipeline 再実走:
  - `BuildWebImage` は Docker build、Trivy image scan、ECR push まで成功。
  - `BuildMigrationImage` は Docker build 後の Trivy image scan で失敗。
  - 検出対象は `flyway/flyway:12.9.0-alpine` 由来の Alpine OpenSSL と Flyway 配布物内の Java 依存で、WorkOps の MySQL migration には不要な同梱物も含まれていた。
- 2026-06-23 ローカル再現確認:
  - `docker build -f infra/docker/migration/Dockerfile -t workops-migration:local .` 成功。
  - `docker run --rm -v /var/run/docker.sock:/var/run/docker.sock public.ecr.aws/aquasecurity/trivy:0.71.2 image --exit-code 1 --severity MEDIUM,HIGH,CRITICAL --no-progress workops-migration:local` 失敗。Alpine 3.23.4 で 17 件、Java jar で 8 件の MEDIUM / HIGH findings を確認。
  - `docker build -f apps/web/Dockerfile -t workops-web:local apps/web` 成功。
  - `docker run --rm -v /var/run/docker.sock:/var/run/docker.sock public.ecr.aws/aquasecurity/trivy:0.71.2 image --exit-code 1 --severity MEDIUM,HIGH,CRITICAL --no-progress workops-web:local` 成功。Alpine 3.23.5 と `app/workops-web.jar` は 0 findings。
- 2026-06-23 migration image 最小化後の確認:
  - `docker build -f infra/docker/migration/Dockerfile -t workops-migration:local .` 成功。
  - `docker run --rm -v /var/run/docker.sock:/var/run/docker.sock public.ecr.aws/aquasecurity/trivy:0.71.2 image --exit-code 1 --severity MEDIUM,HIGH,CRITICAL --no-progress workops-migration:local` 成功。Alpine 3.23.5 と `workops/migration-runner.jar` は 0 findings。
  - `docker build -f apps/web/Dockerfile -t workops-web:local apps/web` 成功。
  - `docker run --rm -v /var/run/docker.sock:/var/run/docker.sock public.ecr.aws/aquasecurity/trivy:0.71.2 image --exit-code 1 --severity MEDIUM,HIGH,CRITICAL --no-progress workops-web:local` 成功。Alpine 3.23.5 と `app/workops-web.jar` は 0 findings。
  - `cd C:\git\workops\infra\cdk; npm run build` 成功。
  - `cd C:\git\workops\infra\cdk; npm run test -- --runInBand` 成功。2 test suites / 22 tests passed。
  - `cd C:\git\workops\infra\cdk; npm run lint` 成功。
  - `cd C:\git\workops\infra\cdk; npm run format:check` 成功。
  - `cd C:\git\workops\apps\web; .\mvnw clean test` 成功。388 tests、Failures 0、Errors 0。
- 2026-06-24 Migration CodeBuild 切り替え後の確認:
  - `cd C:\git\workops\infra\cdk; npm run build` 成功。
  - `cd C:\git\workops\infra\cdk; npm run test -- --runInBand` 成功。2 test suites / 22 tests passed。
  - `cd C:\git\workops\infra\cdk; npm run lint` 成功。
  - `cd C:\git\workops\infra\cdk; npm run format:check` 成功。
  - AWS credential 環境変数を削除し、AWS account は手元の AWS 認証で確認済み。`WORKOPS_STAGE=dev` を明示して `cd C:\git\workops\infra\cdk; npm run cdk:pipeline -- synth --quiet` 成功。
  - 生成 template で migration ECR repository / migration cache repository / migration source bucket が存在しないことを確認。
  - 生成 template で Migration CodeBuild Project が Source `CODEPIPELINE`、VPC config、security group config を持つことを確認。
  - 生成 template で Pipeline の `RunMigration` action が `workops-dev-migration` project を呼び、`SourceArtifact` を input artifact として渡すことを確認。
  - `cd C:\git\workops\apps\web; .\mvnw.cmd clean test` 成功。388 tests、Failures 0、Errors 0。
- 2026-06-24 Flyway CLI 直接実行への修正後の確認:
  - `infra/migration-runner` は削除したため runner Maven test は実施しない。
  - `cd C:\git\workops\infra\cdk; npm run build` 成功。
  - `cd C:\git\workops\infra\cdk; npm run test -- --runInBand` 成功。2 test suites / 22 tests passed。
  - `cd C:\git\workops\infra\cdk; npm run lint` 成功。
  - `cd C:\git\workops\infra\cdk; npm run format:check` 成功。
  - AWS credential 環境変数を削除し、AWS account は手元の AWS 認証で確認済み。`WORKOPS_STAGE=dev` を明示して `cd C:\git\workops\infra\cdk; npm run cdk:pipeline -- synth --quiet` 成功。
  - 生成 template で Maven runner 関連文字列が存在しないこと、Redgate Flyway CLI `12.9.0` の取得と `flyway migrate` が含まれることを確認。
  - 生成 template で migration CodeBuild が x86_64 standard image を使い、ARM image ではないことを確認。
  - `cd C:\git\workops\apps\web; .\mvnw.cmd clean test` 成功。388 tests、Failures 0、Errors 0。
  - `docker build -f apps/web/Dockerfile -t workops-web:local apps/web` 成功。
  - `docker run --rm -v /var/run/docker.sock:/var/run/docker.sock public.ecr.aws/aquasecurity/trivy:0.71.2 image --exit-code 1 --severity MEDIUM,HIGH,CRITICAL --no-progress workops-web:local` 成功。Alpine 3.23.5 と `app/workops-web.jar` は 0 findings。
  - `docker compose --profile migration config` 成功。migration service が Maven image ではなく Redgate Flyway CLI image を使い、`/bin/sh -c` の1コマンドとして `flyway migrate` を実行する構成であることを確認。
- 2026-06-24 Pipeline 実走失敗後の SQL path 修正確認:
  - `RunMigration` は `test -d apps/web/src/main/resources/db/migration` で失敗した。SQL 正本は root `db/` 配下であり、CodeBuild buildspec の path が誤っていた。
  - `WORKOPS_FLYWAY_LOCATIONS` を `filesystem:db/migration,filesystem:db/seed/common,filesystem:db/seed/aws-dev` に修正した。
  - `cd C:\git\workops\infra\cdk; npm run build` 成功。
  - `cd C:\git\workops\infra\cdk; npm run test -- --runInBand` 成功。2 test suites / 22 tests passed。
  - `cd C:\git\workops\infra\cdk; npm run lint` 成功。
  - `cd C:\git\workops\infra\cdk; npm run format:check` 成功。
  - `docker compose --profile migration config` 成功。local migration の locations も root `db/` 配下になっていることを確認。
  - AWS credential 環境変数を削除し、AWS account は手元の AWS 認証で確認済み。`WORKOPS_STAGE=dev` を明示して `cd C:\git\workops\infra\cdk; npm run cdk:pipeline -- synth --quiet` 成功。
  - 生成 template で `filesystem:db/migration`、`filesystem:db/seed/common`、`filesystem:db/seed/aws-dev` と `test -d db/migration` が含まれ、`apps/web/src/main/resources/db` が含まれないことを確認。

### 残課題

- Build Images の実 ECR push、Trivy 実 scan、AWS deploy、Pipeline 実走はローカル変更後に P2-alpha-3 で確認する。
- P2-alpha-3 で Pipeline を再実走し、BuildWebImage が Docker build、Trivy image scan、ECR push まで成功することを確認する。
- P2-alpha-3 で Pipeline を再実走し、RunMigration が Source artifact 内 SQL を使い、VPC attached CodeBuild project から RDS MySQL に接続して成功時だけ AppRuntime deploy へ進むことを確認する。
