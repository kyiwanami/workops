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

## Cognito 最小接続の前提

M3 では、Cognito を本格構築するのではなく、Spring Security OAuth2 Login と Cognito の接続境界だけを先に確認する。
この確認により、後続の `LoginUserContext` と `CurrentUserProvider` の形を Cognito claim 前提で決める。

### ユーザーが AWS 側で用意するもの

- ダミー Cognito User Pool
- client secret なしの App Client
- Hosted UI / managed login domain
- Cognito テストユーザー
- callback URL: `http://localhost:8080/login/oauth2/code/cognito`
- logout URL: `http://localhost:8080/`

### アプリ側で受け取る設定キー

M3-02 では、実際の Cognito 接続値をリポジトリに固定せず、次の環境変数で受け取る。

- `WORKOPS_COGNITO_ISSUER_URI`
- `WORKOPS_COGNITO_CLIENT_ID`
- `WORKOPS_COGNITO_REDIRECT_URI`

### 確認する claim

M3 では、Spring Security OAuth2 Login で Cognito `sub` を取得できることを確認する。
業務利用する Cognito claim は、DB `users` と突合するための `sub` だけに限定する。

- `sub`

### MVP / Phase 2 境界

MVP で扱うもの:

- Cognito OAuth2 Login の最小接続
- Cognito Hosted UI / managed login へのリダイレクト確認
- `localhost` callback 確認
- OIDC / Cognito claim 取得確認
- LoginUserContext 設計
- local 代替

Phase 2 で扱うもの:

- PLATFORM / TENANT App Client 分離
- Cognito Trigger
- Pre Token Generation
- CDK による Cognito 構築
- AWS dev 環境デプロイ
- 本格ユーザー管理

## 確認方法

- M3 の実装前提が `docs/implementation/mvp/phases.md` と矛盾していないことを確認する
- Cognito の最小接続確認で必要な設定値が洗い出されていることを確認する

## 実装時の記録

### 実装方針

- M3-01 では文書更新だけを行い、アプリ実装は変更しない
- Cognito 接続値の実値はリポジトリに書かない
- M3-02 で使う環境変数キーだけを文書化する
- client secret ありの App Client は MVP では使わない
- Cognito 本格連携は Phase 2 に残す

### 変更ファイル

- `docs/implementation/mvp/agent-tasks/M3-01-cognito-prerequisites.md`
- `docs/implementation/README.md`

### 確認結果

- AWS 側でユーザーが用意する Cognito リソースを文書化した
- callback URL と logout URL を文書化した
- client secret なしの App Client を前提として文書化した
- M3-02 で使う環境変数キーを文書化した
- M3 で確認する OIDC / Cognito claim を文書化した
- MVP と Phase 2 の境界を文書化した
- `docs/implementation/mvp/phases.md` と矛盾しないことを確認した
- `Cognito なし`、`Spring Security は最初から導入しません`、MVP 除外としての単独 `Cognito ログイン` が残っていないことを確認した
- Java / Spring Boot の実装ファイルは変更していない

### 残課題

- なし
