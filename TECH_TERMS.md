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
| スモークテスト | スモークテストってなんだっけ？ | 2026-05-16 | M10-01でCognito `AdminCreateUser` の実AWS確認を自動テストに含めるか確認したとき | 主要経路が最低限動くかを短時間で確認する軽量テスト。 | 今回のCognito確認は実AWS変更と招待メール送信を伴うため、通常の `mvnw test` には混ぜず、E2E確認としてユーザーが実施する。 |
| `declare global` | `declare global` と `namespace` の使い所、`export` との違いは何か | 2026-06-13 | P2-1-01のCDK stage設定で `process.env.WORKOPS_STAGE` の型付け方法を確認したとき | TypeScriptでグローバル型空間に型宣言を追加・拡張するための構文。importなしで見える既存グローバル型を拡張するときに使う。 | 実行時の値を作るものではなく、型宣言だけを拡張する。 |
| `namespace` | `declare global` と `namespace` の使い所、`export` との違いは何か | 2026-06-13 | P2-1-01のCDK stage設定で `NodeJS.ProcessEnv` の型拡張方法を確認したとき | TypeScriptで型や値を名前空間にまとめる構文。今回の `namespace NodeJS` は既存のNode.js型名前空間を開き、`ProcessEnv` を宣言マージで拡張するために使う。 | 現代の通常アプリコードでは `export` / `import` を優先し、既存グローバル型の拡張など限定的に使う。 |
| `valueFromLookup` | バリューフロムルックアップって何？ | 2026-06-14 | P2-2-01のCDK計画で、SSM Parameterを作成するか既存値をlookupするか確認したとき | AWS CDKがsynth時にAWS APIへ問い合わせ、既存のSSM Parameterなどの値を取得して `cdk.context.json` にキャッシュする仕組み。 | 今回のWorkOpsでは、既存SSMを読むのではなく `ConfigStack` がSSM Parameterを作成するため使わない。 |
| Actuator | readiness/liveness分離、Micrometer全体導入、Actuatorとは何か | 2026-06-15 | P2-3のALB health checkで `/actuator/health` を使う範囲を確認したとき | Spring Bootアプリに管理用エンドポイントを追加する仕組み。代表例の `/actuator/health` でアプリやDB接続の状態をHTTPで確認できる。 | P2-3ではhealth check用途に限定し、Actuator全体の本格活用はしない。 |
| readiness / liveness 分離 | readiness/liveness分離、Micrometer全体導入、Actuatorとは何か | 2026-06-15 | P2-3のALB health checkで `/actuator/health` を使う範囲を確認したとき | health checkを「プロセスが生きているか」と「リクエストを受けてよいか」に分ける考え方。 | P2-3では分離せず、`/actuator/health` だけで確認する。 |
| Micrometer全体導入 | readiness/liveness分離、Micrometer全体導入、Actuatorとは何か | 2026-06-15 | P2-3のALB health checkで `/actuator/health` を使う範囲を確認したとき | Spring BootでHTTP件数、処理時間、JVM、DB connection poolなどのメトリクスを扱う仕組みを本格的に設計・連携すること。 | P2-3ではCloudWatch連携、ダッシュボード、アラームなどの観測設計は扱わない。 |
