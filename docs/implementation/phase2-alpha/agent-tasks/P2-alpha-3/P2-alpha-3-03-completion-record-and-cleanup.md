# P2-alpha-3-03 Completion record and cleanup

## 目的

P2-alpha-3 の確認結果、ベースライン、失敗記録、ADR 化候補、主要課金 4 stack destroy を集約し、Phase 2α の完了判定を記録する。

この task は、P2-alpha-3 全体の完了記録と cleanup を兼ねる。

## ユーザー要求

- Cleanup は独立 task にしない
- ベースラインは stage 単位の開始・終了時刻・所要時間だけ記録する
- ベースライン値は合否判定に使わない
- 失敗時は P2-alpha-3 の 03 に集約する
- 失敗しても自動的に P2-alpha-2 へ戻さない
- 失敗事象は、原因、対策、ADR 化候補、ユーザーとの壁打ち結果として記録する
- ADR 本文は作らず、ADR 化候補として残す
- 実 account ID、role ARN、credential、public IP、実 Cognito `sub`、DB 実データ値、CloudFront domain は git 管理文書に残さない

## 前提

- P2-alpha-3-01 が完了している
- P2-alpha-3-02 が完了している
- Pipeline が 1 周完走している
- 主要課金 4 stack の destroy を実行してよい状態である

## 案

P2-alpha-3 の完了記録は、本ファイルに集約する。

確認結果は、Pipeline、Migration、App、Alarm / Log、Cleanup に分けて記録する。
ベースラインは Pipeline stage 単位で記録し、合否判定には使わない。

失敗が発生した場合は、本ファイルに集約し、原因、対策、ADR 化候補、ユーザーとの壁打ち結果を残す。
ADR 本文を作る必要が出た場合は、Notion 側の更新要否をユーザーに確認する。

Cleanup では `EgressStack`、`DataStack`、`EdgeStack`、`AppRuntimeStack` のみを destroy 対象にする。
維持対象 Stack とリソースは destroy しない。

## 判断メモ

- Cleanup は独立 task にしない。P2-alpha-3 を3本に収め、完了記録と課金停止確認を一体で扱うため。
- 失敗記録は各 task に分散させない。最終的な判断と対策を一箇所で追えるようにするため。
- ADR 本文は作らない。ADR は Notion 側の責務であり、P2-alpha-3 では ADR 化候補として扱うため。

## 対応範囲

- P2-alpha-3 checklist 集約
- ベースライン記録
- 失敗事象の集約
- ADR 化候補の記録
- 主要課金 4 stack destroy 手順
- destroy 後の Stack 状態確認
- 維持対象 Stack / resource が残っていることの確認
- Phase 2α 完了判定

## 除外範囲

- CDK code 変更
- 追加品質ゲート tuning
- 数値 SLO 合格判定
- production-grade SLA 設定
- preview / staging / prod 環境
- cross-account
- ADR 本文作成
- Notion 更新

## 完了条件

- P2-alpha-3 の動作確認 checklist が全項目達成されている
- ベースライン記録が保存されている
- 失敗事象がある場合、原因、対策、ADR 化候補、ユーザーとの壁打ち結果が記録されている
- `EgressStack` が destroy 済みである
- `DataStack` が destroy 済みである
- `EdgeStack` が destroy 済みである
- `AppRuntimeStack` が destroy 済みである
- `PipelineStack`、`MigrationStack`、`FoundationStack`、`ConfigStack`、`RegistryStack`、`LogsStack`、`SecretStack`、Artifact bucket、ECR repository、CodeConnection が維持されている
- 実 account ID、role ARN、credential、public IP、実 Cognito `sub`、DB 実データ値、CloudFront domain を記録していない

## Checklist 集約

### Pipeline

| 項目 | 状態 | 根拠 |
| --- | --- | --- |
| Source が CodeConnections で `main` を取得して起動した | 未確認 | P2-alpha-3-02 |
| Synth stage で `cdk synth` が pass した | 未確認 | P2-alpha-3-02 |
| Build & Test stage で全品質ゲートが pass した | 未確認 | P2-alpha-3-02 |
| ManualApproval 通知メールが届いた | 未確認 | P2-alpha-3-01 |
| Deploy Registry stage で ECR repository 4 本が存在した | 未確認 | P2-alpha-3-02 |
| Web image と migration image が commit SHA tag で push された | 未確認 | P2-alpha-3-02 |
| Deploy Data / Network / Migration stage が完走した | 未確認 | P2-alpha-3-02 |
| Migration RunTask stage が完走した | 未確認 | P2-alpha-3-02 |
| Deploy AppRuntime stage が完走した | 未確認 | P2-alpha-3-02 |
| Pipeline 全体がエラーなく 1 周完走した | 未確認 | P2-alpha-3-02 |

### Migration

| 項目 | 状態 | 根拠 |
| --- | --- | --- |
| ECS RunTask が exit code 0 で終了した | 未確認 | P2-alpha-3-02 |
| CloudWatch Logs 上で Flyway schema 適用完了を確認した | 未確認 | P2-alpha-3-02 |
| Flyway 適用後、アプリケーションから DB 接続が成功した | 未確認 | P2-alpha-3-02 |

### App

| 項目 | 状態 | 根拠 |
| --- | --- | --- |
| Blue/Green 切替が完了した | 未確認 | P2-alpha-3-02 |
| ALB Target Group が healthy を示した | 未確認 | P2-alpha-3-02 |
| CloudFront default domain 経由で HTTP 200 を取得した | 未確認 | P2-alpha-3-02 |
| ブラウザでトップページ到達を確認した | 未確認 | P2-alpha-3-02 |
| ECS Task が `cpuArchitecture: ARM_64` で起動した | 未確認 | P2-alpha-3-02 |

### Alarm / Log

| 項目 | 状態 | 根拠 |
| --- | --- | --- |
| bake 後、連動 Alarm が `ALARM` ではなかった | 未確認 | P2-alpha-3-02 |
| CloudWatch Logs に致命的 ERROR がなかった | 未確認 | P2-alpha-3-02 |

### Cleanup

| 項目 | 状態 | 根拠 |
| --- | --- | --- |
| `EgressStack` が destroy 済みである | 確認済み | CloudFormation stack 一覧 |
| `DataStack` が destroy 済みである | 確認済み | CloudFormation stack 一覧 |
| `EdgeStack` が destroy 済みである | 確認済み | CloudFormation stack 一覧 |
| `AppRuntimeStack` が destroy 済みである | 確認済み | CloudFormation stack 一覧 |
| 維持対象 Stack / resource が残っている | 確認済み | CloudFormation stack 一覧 |

## ベースライン記録

以下は合否判定に使わない。

| 項目 | 開始 | 終了 | 所要時間 | 備考 |
| --- | --- | --- | --- | --- |
| Pipeline 全体 | 未記録 | 未記録 | 未記録 | 合否判定に使わない |
| Build & Test stage | 未記録 | 未記録 | 未記録 | 合否判定に使わない |
| Build Images stage | 未記録 | 未記録 | 未記録 | 並列 stage 全体 |
| Migration RunTask | 未記録 | 未記録 | 未記録 | 合否判定に使わない |
| Blue/Green 切替 | 未記録 | 未記録 | 未記録 | 合否判定に使わない |

## 失敗記録

失敗が発生した場合は、次の表に集約する。

| 発生箇所 | 事象 | 原因 | 対策 | ADR 化候補 | 壁打ち結果 |
| --- | --- | --- | --- | --- | --- |
| P2-alpha-3-01 初回 `PipelineStack` deploy | `AWS::CodeStarNotifications::NotificationRule` 作成が `ConfigurationException` で失敗し、`workops-dev-pipeline` が rollback した | CodeStar Notifications の service-linked role が `NotificationRule` 作成直前に初回作成され、CloudFormation 依存関係上は role 作成後でも通知ルール作成側が失敗した | `AWS::IAM::ServiceLinkedRole` を CDK に明示定義し、`NotificationRule` がその作成後に実行される依存関係を持つようにした。初回失敗で残った service-linked role、rollback stack、空 artifact bucket は削除した。Artifact bucket は `RemovalPolicy.DESTROY` に変更した | CodeStar Notifications の service-linked role を PipelineStack の明示リソースとして管理する判断 | ユーザーと壁打ちし、Custom Resource ではなく `AWS::IAM::ServiceLinkedRole` を定義する方針に修正した |
| P2-alpha-3-01 `GitHubSource` 実行 | CodeConnection 接続後の Source action が権限で失敗した | Pipeline role に追加した `codeconnections:FullRepositoryId` / `codeconnections:BranchName` 不一致時の明示 Deny が `UseConnection` 実行を拒否した | 明示 Deny を削除し、repository / branch 固定は `CodePipelineSource.connection` の `githubRepository` / `main` 指定で扱う | CodeConnection の repository / branch 制限を IAM Deny ではなく Source action 定義へ寄せる判断 | ユーザー指摘を受け、認可済み接続を止めたのはCDK側のPipeline role policyと判断して修正した |
| P2-alpha-3-01 `Synth` 実行 | Source 成功後、`Synth` CodeBuild の `npm ci` が失敗した | `infra/cdk/package.json` と `infra/cdk/package-lock.json` が npm 10 / Linux arm64 の optional dependency 解決と同期しておらず、`@emnapi/core@1.11.1` / `@emnapi/runtime@1.11.1` が lock file に不足していた | CodeBuild と同じ npm 10.8.2 で `package-lock.json` を同期し、`npm ci --dry-run`、`npm run build`、`npm run lint`、`npm run test -- --runInBand` を確認した | Pipeline Synth は CodeBuild runtime と同じ npm major で lock 同期する運用 | ユーザー許可を受けて lock file のみ更新し、`npm audit fix` は実行しない方針で進めた |
| P2-alpha-3-01 `Synth` 実行 | lock 同期後、`npm run cdk:pipeline -- synth` が lookup 権限不足で失敗した | Synth CodeBuild role に、CDK context lookup で必要な `ec2:DescribeAvailabilityZones`、`ec2:DescribeManagedPrefixLists`、`ec2:GetManagedPrefixListEntries`、`cloudformation:ListResources` が無かった | Synth CodeBuild step の `rolePolicyStatements` に lookup 用 read-only 権限を追加し、`PipelineStack` を更新 deploy した | Synth role の lookup 用 read-only 権限を明示する判断 | ユーザー指摘のCodePipeline permission failureとして、失敗原因と対策を集約した |
| P2-alpha-3-01 `BuildAndTest` 実行 | `./mvnw spotless:check` が `google-java-format` の `NoClassDefFoundError` で失敗した | `apps/web` は Java 25 だが、BuildAndTest の CodeBuild runtime version を明示しておらず、Spotless が Java 25 前提の formatter を古い JDK 上で実行した | BuildAndTest の buildspec に `runtime-versions` / `java: corretto25` を明示し、`java -version` をログに出す | Java 25 の Maven 品質ゲートは CodeBuild runtime を明示する判断 | CodeBuild log と AWS 公式 runtime 対応表を確認し、プロジェクトの `java.version` に合わせる方針にした |
| P2-alpha-3 個別 stack deploy 前確認 | `DeployAppRuntime/AppRuntimeStack` の diff に `DataStack` / RDS secret 周辺の置換が出た | Pipeline の `DeployAppRuntime` stage が `FoundationStack`、`DataStack`、`IdentityStack`、`LogsStack`、`EgressStack`、`EdgeStack` を別 Stage path で再定義していた。同じ物理 stack を別 construct path から更新するため、CloudFormation logical ID が変わり、shared stack を冪等に扱えていなかった | `DeployDataNetworkMigration` 側の shared stack に明示 `Export` を追加し、`DeployAppRuntime` は `ConfigStack` と `AppRuntimeStack` だけを管理する構成にした。`AppRuntimeStack` は export import で既存 shared resource を参照する | Pipeline stage を分ける場合、同じ物理 stack を複数 Stage で再定義せず、stage 境界の参照値は明示 export/import で渡す判断 | ユーザー指示に従い、Pipeline 全体実行ではなく個別 stack diff / deploy の途中で停止し、RDS 置換を出さない構成へ修正した |
| P2-alpha-3 AppRuntime image build | `docker build --platform linux/arm64` が runtime stage の `RUN apk upgrade` で `exec format error` になり、ECR push できなかった | ローカル Docker の既定 builder が ARM64 実行を扱えず、ARM64 image の build 中に container command を実行できなかった | `tonistiigi/binfmt --install arm64` で ARM64 emulator を登録し、`docker-container` driver の buildx builder を作り直したうえで `docker buildx build --platform linux/arm64 --push` を実行した。ECR の対象 tag / digest / pushedAt を確認した | ARM64 ECS task 用 image は build 前に buildx builder の `linux/arm64` 対応を確認する判断 | AppRuntimeStack deploy 前に現在の `apps/web` から image build / ECR push / image 確認を実施するルールに従って対応した |
| P2-alpha-3 `AppRuntimeStack` deploy | `cdk deploy` が security-sensitive update の承認で停止した | CDK の `--require-approval` が有効なまま IAM / security group 追加を含む stack を非 TTY で deploy したため、対話承認を取得できなかった。さらに PowerShell pipe と `npm run` の組み合わせで引数渡しが崩れた | ユーザーから `--require-approval never` の明示承認を得た後、`npx cdk` を直接実行して `AppRuntimeStack` を deploy した | 非 TTY の CDK deploy で security-sensitive update を含む場合は、ユーザー明示承認後に direct `cdk` と `--require-approval never` を使う判断 | ユーザー承認後に AppRuntimeStack を作成し、CloudFormation `CREATE_COMPLETE`、ECS service `ACTIVE` / desired 1 / running 1 / rollout completed、CDK diff 0 を確認した |
| P2-alpha-3 cleanup `AppRuntimeStack` destroy | `AppRuntimeStack` の初回 destroy が green target group 削除で `DELETE_FAILED` になった | ECS service / listener rule の削除直後に target group の参照解除が ELB 側で反映されきっておらず、CloudFormation が target group を削除できなかった | ELB listener / rule から target group 参照が外れていることを確認し、同じ CDK destroy を再実行して `AppRuntimeStack` を削除した | ECS blue/green target group を含む stack destroy では、target group 参照解除の整合待ち後に再実行する運用 | ユーザー指示の課金 stack 停止として、AppRuntime、Edge、Egress、Data の順に destroy し、維持対象 stack が残ることを確認した |

## Cleanup 確認方法

AWS 操作前:

```powershell
Remove-Item Env:AWS_ACCESS_KEY_ID -ErrorAction SilentlyContinue
Remove-Item Env:AWS_SECRET_ACCESS_KEY -ErrorAction SilentlyContinue
Remove-Item Env:AWS_SESSION_TOKEN -ErrorAction SilentlyContinue

$env:WORKOPS_STAGE = "<stage>"
$env:AWS_PROFILE = "<aws-profile>"
$env:AWS_SDK_LOAD_CONFIG = "1"
$env:AWS_REGION = "<aws-region>"
$env:AWS_DEFAULT_REGION = $env:AWS_REGION
$env:CDK_DEFAULT_ACCOUNT = (aws sts get-caller-identity --profile $env:AWS_PROFILE --region $env:AWS_REGION --query Account --output text)
$env:CDK_DEFAULT_REGION = $env:AWS_REGION

aws sts get-caller-identity --profile $env:AWS_PROFILE --region $env:AWS_REGION
```

主要課金 4 stack は削除済みのため、今後手順として個別 runtime CDK destroy は残さない。

destroy 後の状態確認:

```powershell
aws cloudformation describe-stacks `
  --profile $env:AWS_PROFILE `
  --region $env:AWS_REGION `
  --query "Stacks[?contains(StackName, 'workops')].[StackName,StackStatus]" `
  --output table
```

維持対象確認:

```powershell
aws cloudformation describe-stacks `
  --profile $env:AWS_PROFILE `
  --region $env:AWS_REGION `
  --query "Stacks[?contains(StackName, 'workops') && (contains(StackName, 'pipeline') || contains(StackName, 'migration') || contains(StackName, 'foundation') || contains(StackName, 'config') || contains(StackName, 'registry') || contains(StackName, 'logs') || contains(StackName, 'secret'))].[StackName,StackStatus]" `
  --output table
```

## 実装時の記録

### 実施結果

- 個別 stack deploy の手順に切り替え、Registry、Foundation、Identity、Logs、Data、Egress、Edge、Migration、AppRuntime の順に確認した。
- `DeployAppRuntime/AppRuntimeStack` の事前 diff で shared stack の二重管理による `DataStack` / RDS secret 周辺の置換差分を検出し、deploy 前に停止した。
- shared stack の必要値を CloudFormation export として公開し、AppRuntime 側は import 参照へ切り替えた。
- Foundation、Identity、Logs、Edge の export 追加を個別 deploy した。
- AppRuntime deploy 前に、現在の `apps/web` から ARM64 Docker image を build し、ECR に push した。
- `AppRuntimeStack` を個別 deploy した。
- 課金停止のため、`AppRuntimeStack`、`EdgeStack`、`EgressStack`、`DataStack` の順に個別 destroy した。
- `AppRuntimeStack` は初回 destroy が target group 削除で停止したが、ELB 側の参照解除を確認後、同じ destroy を再実行して削除した。

### 確認結果

- CDK local verification は `npm run build`、`npm run lint`、`npm run test -- --runInBand`、`npm run format:check` が成功した。
- `git diff --check` は whitespace error なし。
- `DeployAppRuntime/AppRuntimeStack` deploy 後の CloudFormation status は `CREATE_COMPLETE`。
- ECS service は `ACTIVE`、desired 1、running 1、pending 0、rollout completed。
- `DeployAppRuntime/AppRuntimeStack` deploy 後の CDK diff は 0。
- `AppRuntimeStack`、`EdgeStack`、`EgressStack`、`DataStack` は CloudFormation stack 一覧に表示されない。
- `PipelineStack`、`MigrationStack`、`FoundationStack`、`ConfigStack`、`IdentityStack`、`RegistryStack`、`LogsStack`、`SecretStack` は残っている。

### 残課題

- Pipeline 全体の 1 周完走確認は未実施。
- 現在の修正は未 commit / 未 push。Pipeline は source から `main` を取得するため、Pipeline 全体確認前に commit / push が必要。
