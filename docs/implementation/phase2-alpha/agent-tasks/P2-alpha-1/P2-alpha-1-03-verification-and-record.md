# P2-alpha-1-03 横断検証・記録

## 目的

P2-alpha-1 の Java 品質ゲート、CDK TypeScript 品質ゲート、Web image の Trivy scan を横断確認し、P2-alpha-1 の完了判定をエージェント確認として記録する。

## ユーザー要求

- P2-alpha-1 の中身を詰め切ってからファイル分けする
- 既にコードまたは Notion で分かっていることは質問しない
- Web image の Trivy scan は Docker で実行する
- 生成物は git 管理しない
- 完了記録は各 task ファイル末尾の「実装結果」「確認結果」「残課題」に追記する
- 全体の完了判定は横断確認 task にまとめる

## 前提

- P2-alpha-1-01 が完了している
- P2-alpha-1-02 が完了している
- AWS リソースの作成・変更は行わない
- Docker がローカルで利用できる
- 既存データ移行と後方互換性は考慮しない

## 案

P2-alpha-1 の最後に、Java、CDK、Docker image scan、生成物除外をまとめて確認する。

Trivy はローカルCLI前提にせず、Docker で Trivy image を実行する。
Web image は `apps/web` の Dockerfile からローカル build し、`MEDIUM,HIGH,CRITICAL` を fail 条件として scan する。

JaCoCo report、SpotBugs report、CDK 出力、coverage、dist などの生成物は確認には使うが git 管理しない。

各詳細 task には、それぞれの実装結果、確認結果、残課題を追記する。
本 task には P2-alpha-1 全体としての最終確認結果を追記する。

## ADR

- Trivy は Docker 実行に寄せる。確認者のローカルに Trivy CLI がなくても再現できるようにするため。
- 横断検証 task を置く。Java / CDK / Docker scan の完了条件を一箇所で確認し、P2-alpha-1 の完了判定を明確にするため。
- 生成物は git 管理しない。品質ゲートの report や SBOM は確認用の生成物であり、リポジトリの正本にしないため。

## 対応範囲

- Java 品質ゲートの最終確認
- CDK TypeScript 品質ゲートの最終確認
- Web image build
- Docker 経由の Trivy image scan
- 生成物が git 管理対象に入っていないことの確認
- P2-alpha-1 全体の確認結果記録

## 除外範囲

- P2-alpha-2 の PipelineStack / MigrationStack
- buildspec 作成
- ECR push
- AWS deploy
- CodePipeline 実走
- Trivy DB cache 専用の永続 cache
- Trivy filesystem scan
- ECR Enhanced Scanning の確認
- 数値 SLO 記録

## 完了条件

- P2-alpha-1-01 の確認コマンドがすべて exit 0 になっている
- P2-alpha-1-02 の確認コマンドがすべて exit 0 になっている
- Web image をローカル build できる
- Docker 経由の Trivy image scan が MEDIUM / HIGH / CRITICAL なしで exit 0 になる
- JaCoCo report、SpotBugs report、CDK 出力、coverage、dist が git 管理対象に入っていない
- 各 task ファイルに実装結果、確認結果、残課題が記録されている
- P2-alpha-1 全体の残課題が整理されている

## 確認方法

Java:

```powershell
cd C:\git\workops\apps\web
.\mvnw.cmd spotless:check
.\mvnw.cmd compile spotbugs:check
.\mvnw.cmd verify
```

CDK:

```powershell
cd C:\git\workops\infra\cdk
npm run lint
npm run format:check
Remove-Item Env:AWS_ACCESS_KEY_ID -ErrorAction SilentlyContinue
Remove-Item Env:AWS_SECRET_ACCESS_KEY -ErrorAction SilentlyContinue
Remove-Item Env:AWS_SESSION_TOKEN -ErrorAction SilentlyContinue
$env:WORKOPS_STAGE = "dev"
$env:GITHUB_REPOSITORY = "kyiwanami/workops"
npm run cdk:deploy-app -- synth
npm run cdk:infra -- synth

$env:AWS_PROFILE = "amazon-connect"
$env:AWS_REGION = "ap-northeast-1"
aws sts get-caller-identity --profile $env:AWS_PROFILE --region $env:AWS_REGION
$env:CDK_DEFAULT_ACCOUNT = "<sts Account result>"
$env:CDK_DEFAULT_REGION = $env:AWS_REGION
$env:WORKOPS_STAGE = "dev"
$env:WORKOPS_WEB_IMAGE_TAG = "test-sha"
npm run cdk:runtime -- synth --profile $env:AWS_PROFILE
```

Web image / Trivy:

```powershell
cd C:\git\workops\apps\web
docker build -t workops-web:p2-alpha-1 .
docker run --rm -v /var/run/docker.sock:/var/run/docker.sock aquasec/trivy:latest image --severity MEDIUM,HIGH,CRITICAL --exit-code 1 workops-web:p2-alpha-1
```

生成物確認:

```powershell
cd C:\git\workops
git status --short
```

## 実装時の記録

### 実装結果

- P2-alpha-1-01 の Java 品質ゲート確認を実行した。
- P2-alpha-1-02 の CDK TypeScript 品質ゲート確認を実行した。
- `apps/web/Dockerfile` の runtime base image を `eclipse-temurin:25-jre-alpine` に変更した。
- `apps/web/Dockerfile` の runtime stage に `apk upgrade --no-cache` を追加し、Alpine の修正版 OS package を取り込むようにした。
- `apps/web/pom.xml` で `netty.version` を 4.2.15.Final、`tomcat.version` を 11.0.22 に固定し、Trivy の jar scan で検出された MEDIUM / HIGH / CRITICAL を解消した。
- 初回 Trivy scan では `eclipse-temurin:25-jre` 由来の Ubuntu package と jar dependency に MEDIUM / HIGH / CRITICAL が残ったため、上記の Dockerfile / Maven dependency 対応を実施した。

### 確認結果

- Java:
  - `.\mvnw.cmd spotless:check` は成功。
  - `.\mvnw.cmd compile spotbugs:check` は成功。SpotBugs / FindSecBugs は 0 bugs。
  - `.\mvnw.cmd verify` は成功。387 tests passed。JaCoCo check は `All coverage checks have been met.`。
- CDK:
  - `npm run lint` は成功。
  - `npm run format:check` は成功。
  - `npm run cdk:deploy-app -- synth` は成功。
  - `npm run cdk:infra -- synth` は成功。
  - `npm run cdk:runtime -- synth --profile amazon-connect` は成功。AWS account は手元の AWS 認証で確認済み。実 account 値は git 管理文書に記録しない。
- Web image / Trivy:
  - `docker build --pull -t workops-web:p2-alpha-1 .` は成功。
  - `docker run --rm -v /var/run/docker.sock:/var/run/docker.sock aquasec/trivy:latest image --severity MEDIUM,HIGH,CRITICAL --exit-code 1 workops-web:p2-alpha-1` は成功。
  - Trivy report summary は OS package 0件、jar 0件。
- CDK synth を並列実行した際、`cdk.out/synth.lock` の競合で `cdk:deploy-app` が一度失敗した。逐次再実行では成功したため、実装上の残課題にはしない。

### 残課題

- なし。
