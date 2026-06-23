# WorkOps Codex Instructions

## Git Operation Rule

- Codex は `git commit` を実行しない。
- 作業の区切りや検証完了時には、ユーザーにコミットを促す。
- コミット対象ファイルやコミットメッセージ案は提示してよい。

## Technical Term Log Rule

- ユーザーがチャット内で質問した技術用語だけを、root の `TECH_TERMS.md` に記録する。
- `TECH_TERMS.md` に追記する前に、必ずユーザーへ記録対象の用語を提示し、記録してよいか確認する。
- ユーザーが明示的に了承した場合だけ、`TECH_TERMS.md` に追記する。
- Codex が説明中に使っただけの用語、実装上たまたま出てきた用語、Notion やコードから抽出した用語は記録しない。
- 1つの質問で複数の技術用語が明示された場合は、それぞれ別行で記録する。
- 記録するときは、用語、質問内容、質問日、出たタイミング、回答・用語解説、短いメモを残す。
- 回答・用語解説には、ユーザーの質問に対する短い回答または用語の意味を書く。
- メモには、回答・用語解説に入らない補足だけを書く。
- 回答・用語解説とメモは、仕様判断や採用方針の正本にしない。
- 採用技術や実装方針を変更する必要がある場合は、`TECH_TERMS.md` ではなく該当する実装文書に反映する。

## AWS Operation Rule

- AWS CLI、CDK、ECR、ECS、CloudFormation、RDS、SSM、Secrets Manager など AWS に接続する操作では、実行前に AWS profile と region を明示する。
- AWS 操作前に `AWS_ACCESS_KEY_ID`、`AWS_SECRET_ACCESS_KEY`、`AWS_SESSION_TOKEN` を削除してから profile を使う。
- AWS 実操作の前には `aws sts get-caller-identity --region $env:AWS_REGION` で認証先を確認する。
- AWS CLI コマンドには原則として `--region $env:AWS_REGION` を付ける。
- 実 account ID、SSO role ARN、SSO ユーザー名、credential 値は git 管理文書に記録しない。
- 確認結果を記録する場合は、実値を書かず「AWS account は手元の AWS 認証で確認済み」と書く。

## CDK Operation Rule

- CDK 実行時は `WORKOPS_STAGE` を明示する。
- 同一 workspace / 同一 `infra/cdk` working directory で複数の `cdk synth` を並列実行しない。CDK は `cdk.out` に `synth.lock` と生成物を書き込むため、entrypoint が違っても lock rename や出力更新が競合し、Windows では `EPERM` で失敗し得る。
- 複数 entrypoint の synth 確認が必要な場合は、`cdk:infra`、`cdk:runtime`、`cdk:pipeline` を逐次実行する。
- `CDK_DEFAULT_ACCOUNT` は実行時に `aws sts get-caller-identity` から設定し、git 管理文書に実 account ID を書かない。
- `cdk deploy`、`cdk destroy`、ECR push、課金対象リソース作成、課金対象リソース削除は、ユーザーから明示的な実行指示が出た場合だけ行う。
- `cdk deploy` 前に AWS account、region、既存 Stack 状態、`cdk diff` を確認する。
- `cdk deploy` では原則として `--require-approval never` を使わない。ユーザーがその option まで明示した場合だけ例外とする。

## AWS dev App Image Rule

- Spring Boot 側の変更を AWS dev の `AppRuntimeStack` で確認する前に、必ず現在の `apps/web` から Docker image を build し、ECR へ push する。
- `AppRuntimeStack` deploy 前に、ECR の対象 image tag / digest / pushedAt を確認し、古い image で ECS を起動しない。

## AWS dev RDS / MySQL Rule

- AWS dev RDS に対する MySQL / SQL 実行は、ユーザーが実施するか、ユーザーから明示的な実行指示が出た場合だけ行う。
- AWS dev RDS への MySQL / SQL 実行は、RDS Console integrated CloudShell VPC から実施する。
- 一時 ECS task、Lambda、踏み台、ローカル直結、SSM port forwarding、Session Manager Plugin など、別経路での MySQL / SQL 実行へ勝手に切り替えない。
- MySQL / SQL の実行手順を案内するときは、接続後の prompt が `MySQL [(none)]>` になり得るため、必ず対象 database を `USE workops;` で選択するか、接続時に database 名を指定する手順を含める。
- CloudShell VPC environment は確認後に削除する。

## Verification Responsibility Rule

- コーディングエージェントは、`mvnw test`、CDK build / test / synth、生成 template 確認などの機械的検証を担当する。
- UI の最終操作確認、実 Cognito ログイン、実 AWS 確認はユーザー確認として扱い、コーディングエージェントが勝手に完了扱いにしない。

## Sensitive Output Rule

- CloudFormation Output や git 管理文書に password、secret value、credential 値、実 Cognito `sub`、RDS の実更新値、public IP、EC2 instance id を残さない。

## Requirements Change Rule

- Notion 側の要件、スコープ、ADR を変更する必要がある場合は、リポジトリ側で確定せず、ユーザー確認を挟む。
