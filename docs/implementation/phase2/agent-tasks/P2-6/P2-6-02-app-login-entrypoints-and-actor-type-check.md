# P2-6-02 apps/web login entrypoints / actor_type check

## 目的

Spring Security の OAuth2 Login registration を `platform` と `tenant` に分離し、ログイン route と DB の `users.actor_type` が一致する場合だけ WorkOps session を作る。

## ユーザー要求

- 未認証の保護ページアクセスは `/login` へ遷移する
- `/login` には通常ログインボタンだけを表示する
- 通常ログインボタンは TENANT login へ進める
- SaaS 管理者用入口を通常利用者へ見せない
- SaaS 管理者は直接URL `/login/platform` から Cognito login に進む
- `registrationId=platform` は `actor_type=PLATFORM` のみ許可する
- `registrationId=tenant` は `actor_type=TENANT` のみ許可する
- 不一致は WARN log を出して HTTP 403 にする
- Cognito group / scope を業務認可の正本にしない

## 前提

- `users.cognito_sub` で Cognito user と WorkOps user を突合する
- `LoginUserContext.actorType()` は DB の `users.actor_type` を保持している
- 認可は既存どおり DB 由来の permission set を使う
- P2-6 では Cognito ユーザー作成や `users.cognito_sub` 登録導線は作らない

## 案

- `application.yml` の OAuth2 client registration を `platform` と `tenant` に分ける
- App Client ID と redirect URI は環境変数を分ける
- `SecurityConfig` の login page を `/login` にする
- `/login`、`/login/platform`、`/login/tenant` を未認証許可する
- `LoginController` を追加する
- `/login` は login template を返す
- `/login/platform` は `/oauth2/authorization/platform` へ redirect する
- `/login/tenant` は `/oauth2/authorization/tenant` へ redirect する
- login template には `/login/tenant` へ進む単一の `ログイン` ボタンだけを置く
- `LoginRouteActorType` を追加し、registration ID と actor_type の対応を一箇所に置く
- `CognitoAuthenticationSuccessHandler` で `OAuth2AuthenticationToken.getAuthorizedClientRegistrationId()` を取得する
- registration ID と `LoginUserContext.actorType()` が一致しない場合は WARN log と HTTP 403 にする
- 突合失敗時も WARN log と HTTP 403 を維持する

## 判断理由

- `/login` から PLATFORM login を見せない。一般利用者の画面に SaaS 運営導線を出す必要がなく、入口の露出を抑えられるため。
- 直接URLの `/login/platform` は残す。運営者がログイン不能になることを避けつつ、通常導線には出さない運用にできるため。
- actor_type の検証はアプリで行う。Cognito App Client は login route の識別には使えるが、業務上の利用者種別は WorkOps DB が正本であるため。
- 不一致は redirect ではなく HTTP 403 にする。認証自体は成立しているが、ログイン経路と業務利用者種別が矛盾しているため。

## 対応範囲

- `application.yml` の OAuth2 registration 更新
- `SecurityConfig` の login page / permit path 更新
- `/login` template 追加
- `LoginController` 追加
- `LoginRouteActorType` 追加
- `CognitoAuthenticationSuccessHandler` の route actor_type 検証追加
- Spring Boot test の追加 / 更新

## 除外範囲

- Cognito ユーザー作成
- `users.cognito_sub` 登録導線
- ユーザー停止
- 権限割当
- tenant 業務画面の追加
- 独自エラーページ
- Cognito Hosted UI の文言カスタマイズ

## 完了条件

- 未認証の保護ページアクセスが `/login` に向く
- `/login` が表示できる
- `/login` に PLATFORM 用リンクが表示されない
- `/login/tenant` が `/oauth2/authorization/tenant` へ redirect する
- `/login/platform` が `/oauth2/authorization/platform` へ redirect する
- `platform` login は `PLATFORM` user のみ session 作成する
- `tenant` login は `TENANT` user のみ session 作成する
- actor_type 不一致時は HTTP 403 になる
- actor_type 不一致時は WARN log が出る

## 確認方法

```powershell
cd C:\git\workops\apps\web
.\mvnw.cmd test
```

代表確認:

- `LoginControllerTests`
- `LoginRouteActorTypeTests`
- `CognitoAuthenticationSuccessHandlerTests`
- `SecurityConfigTests`

