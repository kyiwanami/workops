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
