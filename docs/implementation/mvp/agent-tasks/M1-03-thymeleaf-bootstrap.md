# M1-03 Thymeleaf / Bootstrap 最小画面と共通レイアウト作成

## 目的

サーバーサイド画面表示の前提を作る。

## 対応範囲

- Spring Initializr 相当で生成済みの Thymeleaf dependency を前提にする
- 最小トップ画面を表示する Controller を作成する
- 最小トップ画面テンプレートを作成する
- 共通レイアウトを作成する
- Bootstrap CDN と静的 CSS 配置の前提を作る

## 対応ファイル

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
- npm / Node.js による CSS build

## 完了条件

ブラウザで Bootstrap 適用済みのトップ画面を表示でき、以降の業務画面を追加できる土台がある。

## 確認方法

- Spring Boot アプリを起動する
- ブラウザでトップ画面を表示する
- 共通レイアウトがテンプレートとして参照できることを確認する

## 実装時の記録

### 実装方針

- Bootstrap 5.3.8 を CDN 固定で読み込む
- Thymeleaf 標準の fragment 参照だけを使い、追加レイアウトライブラリは導入しない
- `GET /` で最小トップ画面を表示する
- 業務データ、認証、DB 参照は扱わない

### 変更ファイル

- `docs/implementation/README.md`
- `docs/implementation/mvp/agent-tasks/M1-03-thymeleaf-bootstrap.md`
- `apps/server/src/main/java/com/example/workops/common/web/HomeController.java`
- `apps/server/src/main/resources/templates/index.html`
- `apps/server/src/main/resources/templates/layout.html`
- `apps/server/src/main/resources/static/css/app.css`

### 実装結果

- WorkOps のトップ画面を Thymeleaf で表示できるようにした
- 共通 head / header / footer を `layout.html` の fragment として定義した
- Bootstrap とアプリ固有 CSS を全画面で読み込む構成にした

### 確認結果

- `cd apps/server && .\mvnw.cmd test` が成功した
- `cd apps/server && .\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=local"` で起動した
- `http://localhost:8080/` が HTTP 200 を返した
- レスポンスに `WorkOps` と `bootstrap@5.3.8` が含まれることを確認した

### 残課題

- なし
