# M10-01 local Cognito Fake Bean / 疑似 cognito_sub 発行

## 目的

M10 のユーザー作成処理で利用する local profile 用 Cognito 代替境界を作る。
Cognito 本物 API は呼び出さず、local profile では Fake Bean が疑似 `cognito_sub` を発行し、`users.cognito_sub` へ登録できるようにする。

## M10 共通方針

- M10 の範囲は、初期 TENANT_MANAGER 作成、ユーザー作成、権限割当・変更、local Cognito Fake Bean、疑似 `cognito_sub` 発行・登録に限定する
- Cognito 本物 API 呼び出し、Cognito Hosted UI 本格確認、PLATFORM / TENANT App Client 分離は Phase 2 で扱う
- M9 では会社作成まで作成済みであり、M10 で会社作成フローへ初期 TENANT_MANAGER 作成を後付け連携する
- 最終仕様として、会社作成時に初期 TENANT_MANAGER を必ず 1 人作る
- ユーザー作成後に編集できる範囲は、表示名、email、所属部署、権限セットだけとする
- `actor_type` と `company_id` は作成後変更不可とする
- TENANT_MANAGER 数が 0 になる権限変更は、認可ではなく、更新後 count による業務バリデーションで拒否する
- 削除済み部署に所属するユーザーは、ユーザー一覧・詳細で `部署名（削除済み）` と表示する
- Git commit は実行しない

## 対応範囲

- local profile 用の Cognito 連携 Fake Bean を作成する
- 疑似 `cognito_sub` を発行するコンポーネントを作成する
- 発行値は `users.cognito_sub` に登録できる `CHAR(36)` 相当の形式にする
- 疑似 `cognito_sub` はユーザー作成ごとに一意になるようにする
- M10 のユーザー作成処理は、このコンポーネントから `cognito_sub` を受け取る
- Phase 2 で Cognito 本物 API 実装へ差し替えられるよう、ユーザー作成 Service は Fake 実装の具象クラスへ直接依存しない
- local profile 以外では Fake Bean を有効化しない

## 対応ファイル

- `apps/web/src/main/java/com/example/workops/admin/user/service/*.java`
- `apps/web/src/main/java/com/example/workops/common/security/*.java`
- `apps/web/src/test/java/com/example/workops/admin/user/service/*.java`
- `docs/implementation/mvp/agent-tasks/M10/M10-01-local-cognito-fake-sub-issuer.md`

## 除外範囲

- Cognito 本物 API 呼び出し
- Cognito Hosted UI 本格確認
- PLATFORM / TENANT App Client 分離
- Cognito Trigger
- Pre Token Generation
- 初回ログイン時の `users` 自動作成
- Cognito username 変更
- Cognito 側ユーザー属性変更
- ユーザー作成画面
- 権限割当・変更画面

## 完了条件

local profile で疑似 `cognito_sub` を発行できる。
ユーザー作成 Service から疑似 `cognito_sub` を取得できる。
発行した疑似 `cognito_sub` は `users.cognito_sub` へ登録可能な一意値として扱える。
local profile 以外では Fake Bean が有効にならない。

## 確認方法

- `cd apps/web && .\mvnw.cmd test`
- local profile で疑似 `cognito_sub` 発行コンポーネントを利用できることを確認する
- 発行値が `users.cognito_sub` に登録可能な形式であることを確認する
- 既存 local 認証ユーザーの解決が壊れていないことを確認する

## 実装時の記録

M10-01 実装時に、実装方針、変更ファイル、実装結果、確認結果、残課題を記録する。
