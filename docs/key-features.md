# 主要機能

WorkOpsは、会社ごとのデータと操作権限を分けて、申請・資産・会社・部署・ユーザーを管理する業務アプリケーションです。AWSの設計・構築・デプロイ・運用を学ぶために個人で開発しています。

[README](../README.md) · [利用例・デモ](example-or-demo.md) · [コード・設計](code-and-design.md) · [テスト](testing.md)

## 業務機能

| 機能 | できること | 入口 |
|---|---|---|
| 申請管理 | 一覧・詳細、下書き作成・編集、提出、取下げ、承認、却下、差戻し | `/requests` |
| 資産管理 | 一覧・検索、詳細、登録・編集、ステータス変更、論理削除 | `/assets` |
| マスタ管理 | 申請種別・資産分類の登録・編集、論理削除・復活、削除済み表示 | `/masters/request-types`、`/masters/asset-categories` |
| 会社管理 | PLATFORM_ADMINによる会社の登録・編集、論理削除、会社別初期マスタと初期管理者の作成 | `/admin/companies` |
| 部署管理 | PLATFORM_ADMINによる会社指定の管理、TENANT_MANAGERによる自社の管理 | `/admin/companies/{companyId}/departments`、`/departments` |
| ユーザー管理 | PLATFORM_ADMINによる全体管理、TENANT_MANAGERによる自社ユーザーの作成・編集と権限管理 | `/admin/users`、`/users` |

## 会社境界と権限

- 会社IDを使い、申請・資産・会社別マスタ・ユーザー等の参照と更新を自社に限定する。
- 閲覧者、編集者、管理者で操作権限を分ける。
- Platform / Tenantのログイン経路とDB上の利用者区分を検査する。
- Serviceで操作権限と状態遷移を確認し、SQLでも会社ごとのデータを分ける。

会社を切り替えたときの表示範囲と権限別の操作は[利用例・デモ](example-or-demo.md)で確認できます。

## AWS・デプロイ・運用

| 領域 | 機能 |
|---|---|
| Web公開 | CloudFront VPC Originから内部ALBを経由し、ECS Fargateへ接続 |
| 認証 | CognitoのPlatform / Tenant用App Clientと業務DBの利用者・権限を連携 |
| IaC | CDKでネットワーク、認証、DB、実行基盤、CI/CDを管理 |
| CI/CD | 品質ゲート、イメージ作成・Trivyスキャン、承認、Flyway、ECSのBlue/Greenデプロイを連携 |
| ログ・監視 | 業務操作・認証・認可イベントを記録。CloudWatch Logsとデプロイ監視に利用 |
| 停止運用 | RDS停止、実行環境の撤去、EventBridge / Lambda / SSMによる自動起動後の再停止制御 |

構成図と採用理由は[コード・設計](code-and-design.md)、確認結果は[テスト](testing.md)にまとめています。

## 技術スタック

- アプリ: Java 25、Spring Boot 4.0.8、Spring Security、Thymeleaf、Bootstrap、MyBatis
- DB: MySQL 8.4、Flyway、Docker Compose
- 検証: JUnit、Mockito、Testcontainers、Spotless、SpotBugs、JaCoCo、Jest、Python unittest
- AWS / IaC: CDK、CloudFront、ALB、ECS Fargate、RDS、Cognito、CodePipeline、CodeBuild、CloudWatch
