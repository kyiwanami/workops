# Technical Terms Asked By User

このファイルは、ユーザーがチャット内で質問した技術用語だけを記録する。
Codex が説明中に使っただけの用語や、コード・Notionから抽出しただけの用語は追加しない。

| 用語 | 質問内容 | 初回質問日 | 出たタイミング | 回答・用語解説 | メモ |
| --- | --- | --- | --- | --- | --- |
| Spring Initializr | イニシャライザは誰が作るのか、作成コマンドやpullのようなものがあるのか | 2026-05-10 | M1-01実装計画で、Spring Bootアプリ生成方法を確認したとき | Spring Bootプロジェクトの公式スキャフォールド生成手段。 |  |
| Eclipse Adoptium | Eclipse Adoptiumと表示されるが、Eclipse IDEも入ったのか | 2026-05-10 | JDKインストール後、インストール元表示の意味を確認したとき | Eclipse IDEではなく、AdoptiumのJDK配布元名。 |  |
| LTS | JavaやMySQLなど、指定されたバージョンが本当にLTSか | 2026-05-10 | MVP技術スタックのバージョン妥当性を確認したとき | 長期サポート版。一定期間、修正やセキュリティ更新が提供される版。 |  |
| CDN | CDNで入れるとは何か | 2026-05-10 | M1-03でBootstrapをCDN読み込みにした理由を確認したとき | 外部配信URLからCSSやJavaScriptをブラウザに読み込ませる方式。 | BootstrapはCDN固定で導入した。 |
| OAuth2 redirect URI / callback URL | `http://localhost:8080/login/oauth2/code/cognito` がCognitoのcallback URLに必要な理由 | 2026-05-10 | M3-02でSpring Security OAuth2 LoginからCognito Hosted UIへリダイレクト確認したとき | 認証後にCognitoがアプリへ認可コードを返す受け口。Spring Security OAuth2 Loginのデフォルトは `{baseUrl}/login/oauth2/code/{registrationId}`。 | 今回は `registrationId` が `cognito` のため、callback URL は `http://localhost:8080/login/oauth2/code/cognito`。 |
| Spring Bean / `@Component` | `@Component` が付いているクラスはBeanになるのか | 2026-05-10 | M3-04で `SecurityCurrentUserProvider` を `CurrentUserProvider` としてControllerへ注入する仕組みを確認したとき | Springコンテナが管理するオブジェクト。`@Component` が付いたクラスはコンポーネントスキャンで検出され、Beanとして生成・管理される。 | 今回は `SecurityCurrentUserProvider` がBeanになり、`CurrentUserProvider` 型の依存先として `AuthClaimsController` にコンストラクタ注入される。 |
| `_csrf` / CSRFトークン | `_csrf` が何か、CSRFトークンが自動生成されるものか | 2026-05-12 | M5-05の資産削除POSTフォームに hidden input として `_csrf` を置いた理由を確認したとき | CSRF対策用の検証トークン。Spring Security がセッションに紐づくランダム値を用意し、Thymeleaf では `${_csrf.parameterName}` と `${_csrf.token}` としてフォームに埋め込める。POST時にサーバー側の値と送信値を照合し、別サイトからの勝手なPOSTを防ぐ。 | 認可は `@PreAuthorize` が担い、CSRFトークンは正規フォーム由来のPOSTかを確認するためのもの。DB採番ではない。 |
