# P2-5-04 platform-admin sub linking / login check

## 目的

AWS dev の実 Cognito テストユーザーの `sub` を、短命 RDS の `platform-admin` に紐付け、Hosted UI ログイン後に DB 由来の権限で業務画面へ入れることを確認する。

## ユーザー要求

- P2-5 の最初の確認ユーザーは `platform-admin` に固定する
- TENANT ユーザーの Hosted UI ログイン確認は P2-6 以降へ回す
- RDS は短命なので、作り直すたびに `users.cognito_sub` は NULL に戻る
- 実環境確認のたびに Cognito `sub` を SQL で `platform-admin` に入れる
- 実 Cognito `sub` は Flyway migration / seed に入れない
- SQL はファイル化せず、task Markdown に `:cognito_sub` パラメータ付き手順として記載する
- 更新前確認、UPDATE、更新後確認をセットにする
- P2-5 の実 AWS 確認は成功ログイン、`sub` 突合、DB 由来権限での画面利用に限定する
- 突合失敗時の HTTP 403 確認は P2-5 task に入れない

## 前提

- `db/seed/aws-dev/V6__insert_users.sql` は `users.cognito_sub` を NULL のまま投入する
- AWS dev RDS に `platform-admin` が存在する
- `platform-admin` は `actor_type = 'PLATFORM'` である
- `platform-admin` は PLATFORM_ADMIN 相当の権限セットを持つ
- Cognito テストユーザーはユーザーが AWS コンソールまたは AWS CLI で作成する
- Cognito テストユーザーの `sub` はユーザーが取得する
- P2-7 までは WorkOps 管理導線からの `AdminCreateUser` を使わない

## 案

- AWS dev 用 seed は変更しない
- Cognito テストユーザー作成はユーザー確認手順にする
- Cognito テストユーザーの `sub` 取得はユーザー確認手順にする
- RDS 接続は P2-2 / P2-3 の RDS Console integrated CloudShell VPC 手順を使う
- 更新前に `platform-admin` の現在値を確認する
- `:cognito_sub` パラメータ付き SQL で `platform-admin` の `users.cognito_sub` を更新する
- 更新後に `platform-admin` の `cognito_sub` が更新されたことを確認する
- Hosted UI からログインする
- CloudFront callback で `apps/web` に戻る
- `platform-admin` として業務画面へ入れることを確認する
- DB 由来の権限で PLATFORM_ADMIN 向け画面に到達できることを確認する

## ADR

- 実 Cognito `sub` は Flyway に入れない。User Pool やユーザーを作り直すたびに変わる環境生成値であり、DB 正本の seed に固定できないため。
- `platform-admin` を最初の確認ユーザーにする。P2-5 は単一 App Client での Hosted UI ログインと `sub` 突合の成立確認に限定し、PLATFORM / TENANT の導線分離を扱わないため。
- 一時 SQL はファイル化しない。環境ごとに変わる `sub` を入れる確認手順であり、実行資産や Flyway seed と誤認させないため。
- 突合失敗時の HTTP 403 確認は P2-5 task に入れない。AWS 固有の確認ではなく、アプリ側挙動の確認であるため。
- 既存データ移行と後方互換性は考慮しない。

## 対応範囲

- `platform-admin` の `cognito_sub` 更新手順記載
- 更新前確認 SQL 記載
- `:cognito_sub` パラメータ付き UPDATE SQL 記載
- 更新後確認 SQL 記載
- Hosted UI 成功ログイン確認手順記載
- DB 由来権限での画面利用確認手順記載
- P2-7 で不要になる一時手順であることの記録

## 除外範囲

- Flyway migration への実 Cognito `sub` 追加
- `db/seed/aws-dev` への実 Cognito `sub` 追加
- SQL ファイル追加
- Cognito テストユーザーの CDK 自動作成
- WorkOps 管理導線からの Cognito ユーザー作成
- TENANT ユーザーの Hosted UI ログイン確認
- PLATFORM / TENANT App Client 分離
- 突合失敗時の HTTP 403 実 AWS 確認
- git commit

## SQL 手順

更新前確認:

```sql
SELECT
    id,
    username,
    actor_type,
    cognito_sub,
    is_deleted
FROM users
WHERE username = 'platform-admin'
  AND actor_type = 'PLATFORM'
  AND is_deleted = FALSE;
```

`platform-admin` の `cognito_sub` 更新:

```sql
UPDATE users
SET cognito_sub = :cognito_sub
WHERE username = 'platform-admin'
  AND actor_type = 'PLATFORM'
  AND is_deleted = FALSE;
```

更新後確認:

```sql
SELECT
    COUNT(*) AS linked_platform_admin_count
FROM users
WHERE username = 'platform-admin'
  AND actor_type = 'PLATFORM'
  AND cognito_sub = :cognito_sub
  AND is_deleted = FALSE;
```

更新後確認の期待値:

```text
linked_platform_admin_count = 1
```

## 完了条件

- Cognito テストユーザーの `sub` を取得できる
- AWS dev RDS の `platform-admin` に `:cognito_sub` を反映できる
- 更新後確認で `linked_platform_admin_count = 1` になる
- Hosted UI で Cognito テストユーザーとしてログインできる
- CloudFront callback で `apps/web` に戻れる
- `platform-admin` として業務画面へ入れる
- DB 由来の PLATFORM_ADMIN 権限で画面利用できる
- 実 Cognito `sub` が Flyway migration / seed / SQL ファイルに残っていない

## 確認方法

エージェント確認:

- `db/seed/aws-dev/V6__insert_users.sql` が `cognito_sub = NULL` のままであることを確認する
- P2-5 task に SQL 手順が記載されていることを確認する
- 実 Cognito `sub` がリポジトリ内に固定値として記録されていないことを確認する

ユーザー確認:

- Cognito テストユーザーを作成する
- Cognito テストユーザーの `sub` を取得する
- RDS Console integrated CloudShell VPC から RDS に接続する
- 更新前確認 SQL を実行する
- `:cognito_sub` に実 Cognito `sub` を渡して UPDATE を実行する
- 更新後確認 SQL を実行する
- Hosted UI からログインする
- `platform-admin` として業務画面へ入れることを確認する

P2-7 引き継ぎ:

```text
P2-7 で WorkOps 管理導線から AdminCreateUser と users.cognito_sub 登録が成立したら、この一時 SQL 手順は不要にする。
```
