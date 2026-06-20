# P2-10-02 home authorization

## 目的

トップページ `/` を WorkOps の業務入口として整理し、ログイン済み Cognito user と WorkOps DB user の突合済み状態、および DB 由来の権限セットに応じた表示制御を徹底する。

「トップページに見えているボタンを押すと 403 になる」状態を、トップページの表示制御で解消する。

## ユーザー要求

- P2-10 では既知課題の修正も扱う
- `/` は DB 突合済み `LoginUserContext` principal 必須にする
- トップページのボタンは既存画面と同じ boolean model で表示制御する
- `sec:authorize` は使わない
- `thymeleaf-extras-springsecurity` は追加しない
- 認可の正本は Controller / Service の `@PreAuthorize` とする
- `Cognito Claims` と `Manager Authorization` の検証リンクはトップから削除する
- 実 Cognito `sub` は git 管理文書に記録しない

## 統合した既知課題

ログイン後の `/` について、トップページに「申請一覧」「会社管理」などのボタンが無条件表示され、押した先で 403 になる状態が起こりうる。

これは単に Cognito `sub` 未紐づけ時の問題ではなく、トップページ `/` の認可粒度とボタン表示制御の問題である。

P2-10 では、次を同時に扱う。

- `/` は `LoginUserContext` principal 必須にする
- `LoginUserContext` 以外の OAuth2 / OIDC principal では `/` を表示させない
- トップページのボタンはリンク先 `@PreAuthorize` と一致する boolean model で表示制御する
- `Cognito Claims` と `Manager Authorization` の検証リンクはトップから削除する
- `/actuator/health`、login、static assets の public 境界は維持する
- 実 Cognito `sub` は git 管理文書に記録しない

## 前提

- Cognito login success handler は Cognito `sub` と `users.cognito_sub` を突合する
- DB 突合に成功した場合、`Authentication.principal` は `LoginUserContext` になる
- DB 突合に失敗した場合は 403 を返し、CloudWatch Logs で理由を確認する
- 既存画面の表示制御は `canCreate`、`canEdit`、`canReview`、`platformAdmin` などの boolean model を使っている
- 既存データ移行と後方互換性は考慮しない

## 案

`/` は Spring Security の URL 認可で `LoginUserContext` principal を必須にする。

トップページのボタンはリンク単位の boolean model で表示制御する。boolean は `HomeController` から `Model` へ渡す。

| ボタン | 表示条件 |
| --- | --- |
| 申請一覧 | `TENANT_VIEWER`, `TENANT_EDITOR`, `TENANT_MANAGER` |
| 資産一覧 | `TENANT_VIEWER`, `TENANT_EDITOR`, `TENANT_MANAGER` |
| 申請種別一覧 | `TENANT_MANAGER` |
| 資産分類一覧 | `TENANT_MANAGER` |
| 会社管理 | `PLATFORM_ADMIN` |
| 部署管理 Platform | `PLATFORM_ADMIN` |
| 部署管理 Tenant | `TENANT_MANAGER` |
| ユーザー管理 Platform | `PLATFORM_ADMIN` |
| ユーザー管理 Tenant | `TENANT_MANAGER` |

`Cognito Claims` と `Manager Authorization` はトップページから削除する。検証が必要な場合は README の確認手順に URL を記載する。

## ADR

- トップページの表示制御は boolean model で行う。既存画面が `canCreate`、`canEdit`、`canReview`、`platformAdmin` で統一されているため。
- `sec:authorize` は採用しない。トップページだけ表示制御方式を変えると、画面ごとの実装流儀が分かれるため。
- `thymeleaf-extras-springsecurity` は追加しない。P2-10 の目的に対して新規 dependency は不要であり、既存流儀で解決できるため。
- boolean は表示制御であり、セキュリティ境界ではない。認可の正本は Controller / Service の `@PreAuthorize` とする。
- `/` は `LoginUserContext` principal 必須にする。Cognito 認証済みだけでは WorkOps 利用者として扱わないため。
- `PLATFORM_ADMIN` のトップページに TENANT 通常業務導線は出さない。`PLATFORM_ADMIN` は `company_id` を持たないため。
- `TENANT_MANAGER` は TENANT 通常業務と TENANT 管理導線を両方表示する。既存の `@PreAuthorize` と一致させるため。
- `TENANT_VIEWER` / `TENANT_EDITOR` は申請一覧と資産一覧だけ表示する。管理導線は `TENANT_MANAGER` 専用のため。

## 対応範囲

- `/` の URL 認可を `LoginUserContext` principal 必須にする
- `HomeController` で現在ユーザーを取得し、リンク単位 boolean を `Model` に追加する
- `index.html` のボタンに `th:if` を追加する
- `index.html` から `Cognito Claims` と `Manager Authorization` のリンクを削除する
- トップページ表示制御の MockMvc test を追加する
- `LoginUserContext` 以外の認証 principal が `/` を表示できない test を追加する
- README の確認手順に必要な検証 URL を整理する

## 除外範囲

- `thymeleaf-extras-springsecurity` 追加
- `sec:authorize` 導入
- 新規エラー画面
- onboarding / 再同期 / 自動復旧導線
- Cognito Trigger
- Pre Token Generation
- Cognito group / scope を業務認可の正本にする変更
- 既存詳細画面の `canCreate` / `canEdit` / `canReview` の置き換え

## 完了条件

- `LoginUserContext` principal を持つユーザーだけが `/` を表示できる
- OAuth2 / OIDC principal だけの認証状態では `/` を表示できない
- `PLATFORM_ADMIN` には PLATFORM 管理導線だけが表示される
- `TENANT_MANAGER` には TENANT 通常業務と TENANT 管理導線が表示される
- `TENANT_VIEWER` / `TENANT_EDITOR` には申請一覧と資産一覧だけが表示される
- トップページから `Cognito Claims` と `Manager Authorization` が削除されている
- 表示される各ボタンの条件がリンク先 `@PreAuthorize` と一致している

## 確認方法

エージェント確認:

```powershell
cd C:\git\workops\apps\web
.\mvnw test
```

MockMvc 確認:

- `PLATFORM_ADMIN` で `/` を表示し、`/admin/companies`、`/admin/users` が含まれる
- `PLATFORM_ADMIN` で `/requests`、`/assets`、`/users` が含まれない
- `TENANT_MANAGER` で `/requests`、`/assets`、`/masters/request-types`、`/users` が含まれる
- `TENANT_VIEWER` / `TENANT_EDITOR` で `/requests`、`/assets` だけが含まれる
- OAuth2 / OIDC principal だけの認証状態で `/` が拒否される

ユーザー確認:

- AWS dev の CloudFront HTTPS URL で権限別トップページを確認する
- 表示されているボタンを押して、想定外の 403 が発生しないことを確認する
- 表示されない管理 URL へ直接アクセスした場合は、リンク先 `@PreAuthorize` により拒否されることを確認する
