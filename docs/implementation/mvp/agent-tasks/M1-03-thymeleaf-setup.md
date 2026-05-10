# M1-03 Thymeleaf 最小画面と共通レイアウト作成

## 目的

サーバーサイド画面表示の前提を作る。

## 対応範囲

- Spring Initializr 相当で生成済みの Thymeleaf dependency を前提にする
- 最小トップ画面を表示する Controller を作成する
- 最小トップ画面テンプレートを作成する
- 共通レイアウトを作成する
- 静的 CSS 配置または Tailwind 導入前提を作る

## 対応ファイル

- `apps/server/pom.xml`
- `apps/server/src/main/java/com/example/workops/common/web/HomeController.java`
- `apps/server/src/main/resources/templates/index.html`
- `apps/server/src/main/resources/templates/layout.html`
- `apps/server/src/main/resources/static/css/app.css`

## 除外範囲

- 申請画面
- 資産画面
- マスタ画面
- 認証画面
- 業務テーブル
- 業務データ表示

## 完了条件

ブラウザでトップ画面を表示でき、以降の業務画面を追加できる土台がある。

## 確認方法

- Spring Boot アプリを起動する
- ブラウザでトップ画面を表示する
- 共通レイアウトがテンプレートとして参照できることを確認する

## 実装時の記録

実装後に、次をこのファイルへ追記する。

- 実装方針
- 変更ファイル
- 確認結果
- 残課題
