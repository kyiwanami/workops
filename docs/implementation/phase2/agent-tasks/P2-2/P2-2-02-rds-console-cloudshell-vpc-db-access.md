# P2-2-02 RDS Console CloudShell VPC DB 接続確認

## 目的

RDS Console integrated CloudShell VPC を使い、EC2 踏み台を作らずに AWS dev の private RDS へ接続確認できる状態にする。

## ユーザー要求

- P2-2-02 の private RDS 接続確認手段は EC2 踏み台ではなく RDS Console integrated CloudShell VPC を選択する
- 既存の未コミット EC2 access host 実装は採用しない
- RDS が private 配置でも、少なくともどこかから DB 内部を確認できる仕組みを用意する
- CloudShell VPC environment 自体は CDK 管理しない
- CDK では RDS Console CloudShell 接続確認用 Security Group と RDS への紐づけを管理する
- ユーザー確認は RDS コンソールの CloudShell 起動から `mysql` 接続し、`SELECT 1;` まで確認する
- 初期 task Markdown には `実装時の記録` テンプレ欄を入れない

## P2-2 共通方針

- CDK 実装言語は TypeScript とする
- package manager は npm とする
- CDK app は dotenv を使わない
- AWS profile と region は CDK CLI 実行環境に任せる
- `stage` は `WORKOPS_STAGE` 環境変数で指定する
- Phase 2 の確認例は `WORKOPS_STAGE=dev` とする

## 案

- `DataStack` に RDS Console CloudShell 接続確認専用 Security Group を追加する
- Security Group 名は `workops-${stage}-rds-console-cloudshell-sg` とする
- CloudShell 専用 Security Group は `allowAllOutbound=false` とする
- CloudShell 専用 Security Group は self-reference の TCP 3306 egress と ingress だけを許可する
- RDS `DatabaseInstance.securityGroups` に既存 DB SG と CloudShell 専用 SG を付与する
- DB credential は Secrets Manager からユーザーが取得し、RDS 統合 CloudShell の `mysql -p` prompt へ入力する
- RDS Console integrated CloudShell が提示する `/certs/global-bundle.pem` を使った SSL 接続コマンドで確認する
- RDS Console CloudShell 起動、TCP 到達確認、MySQL 接続、CloudShell 環境削除手順は P2-2-04 で README に整理する

## ADR

- private RDS を public にしない。DB の公開範囲を広げず、確認経路だけを限定するため
- RDS Console integrated CloudShell VPC を採用する。CloudShell VPC は追加料金なしで、EC2 踏み台よりコストと管理負荷が小さいため
- EC2 access host、SSM port forwarding、Session Manager Plugin は採用しない。RDS コンソール上の CloudShell 統合で AWS Console 内のユーザー確認が完結するため
- CloudShell VPC environment は CDK で作成しない。AWS 公式手順では CloudShell console / RDS console から作成する環境であり、CDK 管理対象は RDS 側の SG 準備に限定するため
- NAT Gateway は追加しない。RDS Console CloudShell から private RDS endpoint への VPC 内 TCP 3306 接続には NAT が不要であることを実地確認したため
- 既存 DB SG 本体に self-reference を追加しない。DB SG の意味を CloudShell 確認用に広げず、CloudShell 専用 SG へ閉じ込めるため
- CloudShell 専用 SG は他リソースへ流用しない。RDS Console integrated CloudShell の接続確認用途だけに限定するため
- CloudShell 専用 SG は self-reference の TCP 3306 egress と ingress を持つ。CloudShell ENI から RDS へ送信する 3306 と、RDS 側で受ける 3306 の両方が必要なため

## 対応範囲

- 既存の未コミット DB access host 実装差分を削除する
- `DataStackProps` から `publicSubnets` を削除する
- `DataStack` に RDS Console CloudShell 接続確認用 Security Group を追加する
- RDS Console CloudShell Security Group 名は `workops-${stage}-rds-console-cloudshell-sg` とする
- RDS Console CloudShell Security Group は `allowAllOutbound=false` とする
- RDS Console CloudShell Security Group に self-reference TCP 3306 egress / ingress を追加する
- RDS `DatabaseInstance.securityGroups` に既存 DB SG と RDS Console CloudShell Security Group を設定する
- CloudFormation Outputs に RDS Console CloudShell Security Group id を出す
- CDK assertions を追加する

## 対応ファイル

- `infra/cdk/lib/data-stack.ts`
- `infra/cdk/bin/cdk.ts`
- `infra/cdk/test/*.test.ts`
- `docs/implementation/phase2/agent-tasks/P2-2/P2-2-02-rds-console-cloudshell-vpc-db-access.md`

## 除外範囲

- RDS 本体の作成
- SecretStack の作成
- SSM Parameter の作成
- migration 用 ECS Task
- migration 専用 Security Group
- SSH key pair
- SSH inbound rule
- EC2 access host
- DB access host への DB credential 配置
- DB access host への Secrets Manager 読み取り権限付与
- Elastic IP
- Bastion 用の独自 AMI
- VPC Endpoint
- Client VPN
- AWS Cloud9
- phpMyAdmin
- RDS public access
- RDS Proxy
- IAM DB authentication

## 完了条件

- `DataStack` に RDS Console CloudShell Security Group が定義されている
- RDS Console CloudShell Security Group が self-reference TCP 3306 egress / ingress を持つ
- RDS DBInstance に既存 DB SG と RDS Console CloudShell Security Group が付与されている
- DB access host EC2、EC2 Role、Instance Profile が生成されていない
- CloudFormation Outputs に RDS Console CloudShell Security Group id が出ている
- CloudFormation Outputs に EC2 instance id、public IP、password、secret value が出ていない
- CDK assertions が代表条件を検証している

## 完了判定

- エージェント確認だけでは P2-2-02 を完了扱いにしない
- P2-2-02 の完了には、ユーザー確認の AWS dev 実接続確認を必須とする
- `npm run build`、`npm test`、`cdk synth`、生成テンプレート確認は、エージェント確認完了として扱う
- ユーザー確認が未実施の場合は、報告文で `P2-2-02 はエージェント確認完了、ユーザー確認未実施のためフェーズ未完了` と明記する
- P2-2-04 は手順整理と横断確認であり、P2-2-02 の初回実接続確認を代替しない

## 確認方法

エージェント確認:

- `npm run build`
- `npm test`
- `WORKOPS_STAGE=dev npm run cdk -- synth`
- `git diff --check`

CDK assertions で確認する代表条件:

- `AWS::EC2::Instance` が `0` である
- DB access host 用 `AWS::IAM::Role` と `AWS::IAM::InstanceProfile` が生成されていない
- `AmazonSSMManagedInstanceCore` が template に含まれていない
- `workops-dev-rds-console-cloudshell-sg` が生成される
- RDS Console CloudShell Security Group に self-reference TCP 3306 egress / ingress がある
- RDS DBInstance に既存 DB SG と RDS Console CloudShell Security Group が付与される
- Output に `rdsConsoleCloudShellSecurityGroupId` がある
- public IP、password、secret value、EC2 instance id が Output に出ていない

ユーザー確認:

- AWS dev に deploy 後、RDS コンソールの CloudShell 起動画面で RDS Console CloudShell Security Group が環境設定に含まれることを確認する
- CloudShell で TCP 到達確認を実行し `tcp-ok` を確認する
- RDS コンソール提示の `mysql ... --ssl-ca /certs/global-bundle.pem --ssl-verify-server-cert ...` で接続する
- MySQL 上で `SELECT 1;` が成功する
- 確認後、CloudShell VPC 環境を削除する
- RDS に既存 DB SG と RDS Console CloudShell Security Group のみが残り、EC2 access host が存在しないことを確認する
