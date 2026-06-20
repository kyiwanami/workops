# P2-9-03 runtime deploy LogGroup failure

## 目的

`App Deploy Dev` の runtime deploy で発生した `EdgeStack` の LogGroup 重複失敗を記録し、再発しないように CDK / workflow の責務境界を修正する。

P2-9 の runtime deploy は暫定 deploy 手段である。Phase 2α では CodePipeline + CodeBuild へ移行し、この workflow は撤去する。

## ユーザー要求

- `DataStack` / `EgressStack` は既に課金側として作成済みだが、残して進める
- 失敗はドキュメントに記録する
- 対応方針は Web / AWS ベストプラクティスを踏まえる
- `destroy` すれば問題ない、という誤判断も失敗として扱う
- 実 account ID、role ARN、CloudFront domain、credential は git 管理文書に残さない

## 失敗内容

`App Deploy Dev` は `EdgeStack` deploy で失敗した。

失敗リソースは `CognitoClientUrlUpdaterProviderLogGroup` で、対象 log group は `/workops/dev/custom-resources/cognito-client-url-updater-provider` である。

AWS dev では、この log group が CloudFormation 管理外、retention 未設定、タグなしの状態で残っていた。CloudTrail では、custom resource Provider Lambda が log group を自動作成していた。

`RemovalPolicy.DESTROY` は CloudFormation 管理下の削除には使えるが、Stack 削除後または削除中に Lambda が再作成した CloudFormation 管理外の log group を削除する保証にはならない。`destroy` すれば問題ない、という判断は誤りだった。

`cdk diff` の default `--method=auto` は change set 作成に失敗した後、template diff に fallback した。そのため deploy 前に失敗を止められず、新規 `workops-dev-edge` stack が `REVIEW_IN_PROGRESS` で残った。

`DataStack` / `EgressStack` は作成済みで、課金 runtime の一部が残っている。今回はユーザー判断により `DataStack` / `EgressStack` を残して続行する。

## 案

`LogsStack` が custom resource 用 log group を管理する。

管理対象:

- `/workops/${stage}/custom-resources/cognito-client-url-updater`
- `/workops/${stage}/custom-resources/cognito-client-url-updater-provider`

`EdgeStack` は custom resource 用 `AWS::Logs::LogGroup` を作らず、`LogsStack` から渡された `ILogGroup` を `NodejsFunction.logGroup` と `Provider.logGroup` に渡す。

`app-deploy-dev.yml` の runtime diff は `--method=template` を使う。runtime workflow の diff は監査ログ用途であり、change set 作成による副作用を避ける。

runtime deploy 前には preflight を行い、非 runtime stack の正常状態と `workops-dev-edge` の残骸状態を確認する。`workops-dev-edge` が `REVIEW_IN_PROGRESS`、`ROLLBACK_COMPLETE`、`DELETE_FAILED`、その他の異常状態であれば workflow を失敗させ、cleanup が必要であることをログに出す。

## 復旧手順

実装を main に push して `infra-dev.yml` を動かす前に、AWS dev の残骸を cleanup する。

削除対象:

- `workops-dev-edge` が `REVIEW_IN_PROGRESS` の場合、その stack
- orphan log group `/workops/dev/custom-resources/cognito-client-url-updater-provider`
- orphan log group `/workops/dev/custom-resources/cognito-client-url-updater` が存在する場合、それも削除

削除しない対象:

- `workops-dev-data`
- `workops-dev-egress`
- `workops-dev-foundation`
- `workops-dev-config`
- `workops-dev-identity`
- `workops-dev-registry`
- `workops-dev-logs`
- ECR image

cleanup 操作は AWS profile / region を明示し、アクセスキー系 env を削除してから `aws sts get-caller-identity --region $env:AWS_REGION` で認証先を確認して行う。

## ADR

- custom resource Provider log group は `EdgeStack` ではなく `LogsStack` で管理する。runtime stack の作成/削除と、ログ保持リソースの lifecycle を分けるため。
- 既存 orphan log group は CloudFormation import しない。dev の失敗残骸であり、import には template / DeletionPolicy / drift detection の追加運用が必要で、今回の復旧として過大なため。
- `cdk diff` は runtime workflow では `--method=template` を使う。change set ベースの diff は正確だが、新規 stack で `REVIEW_IN_PROGRESS` を残し得るため、runtime workflow の監査ログ用途では副作用を避ける。
- `cdk deploy` は引き続き change set 経由の通常 deploy にする。実作成時の CloudFormation validation は deploy step で受ける。
- `DataStack` / `EgressStack` は削除しない。ユーザー判断により課金側リソースを残して続行する。
- `app-deploy-dev.yml` は `LogsStack` を直接 deploy しない。非 runtime stack は `infra-dev.yml` の責務として維持する。
- `RemovalPolicy.DESTROY` を、Lambda が再作成した管理外 log group の削除保証として扱わない。

## 参考

- [AWS Lambda custom log groups](https://docs.aws.amazon.com/lambda/latest/dg/monitoring-cloudwatchlogs-loggroups.html)
- [AWS CDK custom resource Provider logGroup](https://docs.aws.amazon.com/cdk/api/v2/python/aws_cdk.custom_resources/Provider.html)
- [AWS::Logs::LogGroup](https://docs.aws.amazon.com/AWSCloudFormation/latest/TemplateReference/aws-resource-logs-loggroup.html)
- [cdk diff --method](https://docs.aws.amazon.com/cdk/v2/guide/ref-cli-cmd-diff.html)
