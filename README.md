# WorkOps

WorkOpsは、AWSの設計・構築・デプロイ・運用を学ぶために個人で開発した業務アプリケーションです。Spring BootとThymeleafで、申請・資産・会社・部署・ユーザー管理を実装しています。

要件と構成は自ら決め、コード作成・機械的検証・画面E2Eにはコーディングエージェントを利用しています。

## 証跡

実AWSのCloudFront・Cognito・ECS・RDSを通して、申請の保存・提出・承認、資産検索、閲覧者の更新拒否、会社間の参照拒否を確認しています。動画3本とスクリーンショット10枚の[確認結果](docs/testing.md#実awsでの画面e2e)と、[画面付きのデモ](docs/example-or-demo.md#実awsの動作例)を参照してください。

![実AWSで保存・提出・承認した申請の詳細](docs/images/e2e/03-request-approved.png)

## 資料構成

| ドキュメント | 内容 |
|---|---|
| [主要機能](docs/key-features.md) | 業務機能、会社境界・権限、AWS・CI/CD、技術スタック |
| [セットアップと実行](docs/setup-and-run.md) | 前提ツール、ローカル起動、DB初期化、AWS構築・認証接続、停止手順 |
| [利用例・デモ](docs/example-or-demo.md) | 動作画面、申請・資産管理、権限・会社切替の操作手順 |
| [テスト](docs/testing.md) | 自動テスト・画面E2E・CI/CDの結果、テストの実行方法 |
| [コード・設計](docs/code-and-design.md) | 構成図、設計理由、問題への対応、主要コード |

## 成果の要約

- 会社ごとのデータ分離、権限別の操作、申請の状態遷移を実装。
- CloudFront VPC Origin、内部ALB、ECS Fargate、RDS、CognitoをCDKで構成。
- 品質ゲート、イメージ作成・スキャン、DBマイグレーション、ECSデプロイをCodePipelineで連携。
- ログ・監視、障害の切り分け、RDS停止・実行環境の撤去を実施。

AWSのdev環境は検証後に停止・片付けを行います。ローカルではAWS接続を用意せず、MySQLと疑似ユーザーで確認できます。[セットアップと実行](docs/setup-and-run.md)で起動し、[利用例・デモ](docs/example-or-demo.md)に沿って操作してください。

## ソースの入口

| 領域 | 場所 |
|---|---|
| Webアプリ・テスト | [apps/web](apps/web) |
| DBスキーマ・サンプルデータ | [db](db) |
| AWS IaC・テスト | [infra/cdk](infra/cdk) |
