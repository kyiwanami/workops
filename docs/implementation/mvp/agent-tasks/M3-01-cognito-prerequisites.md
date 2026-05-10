# M3-01 Cognito prerequisites

## 目的

Cognito OAuth2 Login の最小接続確認に必要な前提を整理し、リポジトリ側で参照できる形にする。

## 対応範囲

- M3 で確認する Cognito 最小接続の目的を整理する
- AWS マネジメントコンソールでユーザーが用意する Cognito リソースを整理する
- local callback URL を固定する
- client secret なしの App Client を前提にする
- M3 で確認する OIDC / Cognito claim を整理する
- M3 と Phase 2 の境界を整理する

## 対応ファイル

- `docs/implementation/mvp/agent-tasks/M3-01-cognito-prerequisites.md`
- `docs/implementation/mvp/phases.md`
- `docs/implementation/README.md`

## 除外範囲

- Spring Security の実装
- Cognito Hosted UI への実接続
- Cognito User Pool の CDK 実装
- Cognito Trigger
- Pre Token Generation
- PLATFORM / TENANT App Client 分離
- ユーザー管理画面

## 完了条件

Cognito 最小接続確認に必要なユーザー作業、callback URL、MVP / Phase 2 の境界が文書化されている。

## 確認方法

- M3 の実装前提が `docs/implementation/mvp/phases.md` と矛盾していないことを確認する
- Cognito の最小接続確認で必要な設定値が洗い出されていることを確認する

## 実装時の記録

実装後に、次をこのファイルへ追記する。

- 実装方針
- 変更ファイル
- 確認結果
- 残課題
