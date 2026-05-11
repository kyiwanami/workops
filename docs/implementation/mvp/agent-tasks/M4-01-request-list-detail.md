# M4-01 申請一覧 / 詳細 / 参照基盤

## 目的

申請管理の参照基盤として、会社境界を守った申請一覧と申請詳細を作る。

## M4 共通方針

- M4 では申請ワークフローを MVP で成立させる
- M4 では申請種別マスタ管理は作らない
- `process_type_code` は申請処理タイプであり、画面上の申請種別管理そのものとして扱わない
- 申請種別の登録・編集・停止は M6 で扱う
- M4 では `asset_id` は任意・未選択中心で扱う
- 資産紐づけは、M5 で資産カタログが成立した後、M5 後半または M5 後の接続調整で扱う
- M4 では資産 CRUD、資産ステータス変更、申請承認による資産ステータス自動変更には踏み込まない
- 会社境界は authority ではなく Mapper SQL の `company_id` 条件で守る
- 画面操作確認用の申請データは Flyway seed として会社ごとに複数件投入する
- Mapper / Testcontainers 強化は M8 に寄せる
- `docs/implementation/mvp/phases.md` の modified 表示は、本文差分がない限り M4 作業に含めない

## 対応範囲

- 申請一覧を作成する
- 申請詳細を作成する
- 申請一覧は現在ユーザーの `companyId` で会社内申請に絞る
- 申請詳細は `id` と現在ユーザーの `companyId` で取得する
- Mapper SQL の一覧取得では `WHERE requests.company_id = #{companyId}` を必須にする
- Mapper SQL の詳細取得では `WHERE requests.id = #{id} AND requests.company_id = #{companyId}` を必須にする
- TENANT_VIEWER / TENANT_EDITOR / TENANT_MANAGER は所属会社内の申請を閲覧できる
- 一覧と詳細では、申請者、申請処理タイプ、ステータス、件名、提出日時、作成日時、更新日時、承認・却下・差戻しコメントを表示する
- `PROCESS_TYPE` と `REQUEST_STATUS` の表示名は M2 seed 済みの `common_master` / `common_master_values` を参照する
- 一覧から詳細へ遷移できるようにする
- トップ画面から申請一覧へ遷移できるようにする

## 対応ファイル

- `apps/web/src/main/java/com/example/workops/request/web/RequestController.java`
- `apps/web/src/main/java/com/example/workops/request/service/RequestQueryService.java`
- `apps/web/src/main/java/com/example/workops/request/mapper/RequestMapper.java`
- `apps/web/src/main/java/com/example/workops/request/model/RequestListItem.java`
- `apps/web/src/main/java/com/example/workops/request/model/RequestDetail.java`
- `apps/web/src/main/resources/mapper/request/RequestMapper.xml`
- `apps/web/src/main/resources/templates/request/list.html`
- `apps/web/src/main/resources/db/migration/V3__insert_request_sample_seed.sql`
- `apps/web/src/main/resources/templates/request/detail.html`
- `apps/web/src/main/resources/templates/index.html`
- `docs/implementation/mvp/agent-tasks/M4-01-request-list-detail.md`

## 除外範囲

- 申請作成
- 申請編集
- 下書き保存
- 申請提出
- 申請取下げ
- 承認
- 却下
- 差戻し
- 申請種別マスタ管理
- 資産 CRUD
- 資産ステータス変更
- 申請承認による資産ステータス自動変更
- 申請履歴テーブル
- 監査ログテーブル
- Mapper / Testcontainers 強化

## 完了条件

local profile でログイン中の DB 由来ユーザーの `companyId` に紐づく申請だけを一覧・詳細で表示できる。
他社の申請 ID を URL に指定しても、`id + company_id` 条件により詳細を取得できない。

## 確認方法

- `cd apps/web && .\mvnw.cmd test`
- local profile で Spring Boot を起動する
- `/requests` で所属会社内の申請一覧を表示できることを確認する
- `/requests/{id}` で所属会社内の申請詳細を表示できることを確認する
- 他社の申請 ID を指定しても詳細が表示されないことを確認する

## 実装時の記録

実装後に、次をこのファイルへ追記する。

- 実装方針
- 変更ファイル
- 実装結果
- 確認結果
- 残課題

### 実装方針

- 申請一覧と申請詳細は参照専用の `RequestQueryService` から取得する
- 現在ユーザーは `CurrentUserProvider.requireCurrentUser()` で取得し、未認証の場合の `AccessDeniedException` は `CurrentUserProvider` 側で統一する
- Controller は `TENANT_VIEWER` / `TENANT_EDITOR` / `TENANT_MANAGER` のいずれかの authority を持つユーザーだけを許可する
- 一覧取得は `requests.company_id = #{companyId}` を必須条件にする
- 詳細取得は `requests.id = #{id}` と `requests.company_id = #{companyId}` を必須条件にする
- 申請処理タイプと申請ステータスの表示名は `common_master` / `common_master_values` から取得する
- M4-01 では参照だけを実装し、作成・編集・提出・承認系操作は追加しない
- 画面操作確認用の申請 seed は Flyway `V3__insert_request_sample_seed.sql` として追加する
- README、`docs/implementation/mvp/phases.md` は変更しない

### 変更ファイル

- `apps/web/src/main/java/com/example/workops/request/mapper/RequestMapper.java`
- `apps/web/src/main/java/com/example/workops/request/model/RequestDetail.java`
- `apps/web/src/main/java/com/example/workops/request/model/RequestListItem.java`
- `apps/web/src/main/java/com/example/workops/request/service/RequestQueryService.java`
- `apps/web/src/main/java/com/example/workops/request/web/RequestController.java`
- `apps/web/src/main/resources/mapper/request/RequestMapper.xml`
- `apps/web/src/main/resources/db/migration/V3__insert_request_sample_seed.sql`
- `apps/web/src/main/resources/templates/request/detail.html`
- `apps/web/src/main/resources/templates/request/list.html`
- `apps/web/src/main/resources/templates/index.html`
- `docs/implementation/mvp/agent-tasks/M4-01-request-list-detail.md`

### 実装結果

- `GET /requests` で所属会社内の申請一覧を表示できるようにした
- `GET /requests/{id}` で所属会社内の申請詳細を表示できるようにした
- 申請が0件の場合は一覧画面に空状態メッセージを表示するようにした
- `V3__insert_request_sample_seed.sql` で北浜精密機器株式会社と青葉ケアサービス株式会社にそれぞれ8件、合計16件の申請 seed を投入するようにした
- `CurrentUserProvider.requireCurrentUser()` を使い、業務Service側で同じ未認証判定を繰り返さないようにした
- トップ画面から申請一覧へ遷移できるリンクを追加した
- `PROCESS_TYPE` と `REQUEST_STATUS` は M2 seed 済みの共通マスタ値を表示名として参照するようにした
- `asset_id` は値がある場合に ID として表示し、資産名 JOIN や資産詳細リンクは追加していない

### 確認結果

- `cd apps/web && .\mvnw.cmd test` が成功した
- local profile 起動で `/requests` が HTTP 200 を返すことを確認した
- 画面操作確認用の手動 INSERT 申請を削除した
- `V3__insert_request_sample_seed.sql` で会社ごとに8件、合計16件の申請が投入されることを確認した
- local profile の既定ユーザー `kthm-manager` で `/requests` に北浜精密機器株式会社の申請8件が表示されることを確認した
- local profile の既定ユーザー `kthm-manager` で青葉ケアサービス株式会社の申請詳細が HTTP 404 を返すことを確認した
- ユーザーの画面操作確認用に Spring Boot を起動したままにした

### 残課題

- 申請作成・編集・下書き保存は M4-02 で扱う
- 申請提出・取下げは M4-03 で扱う
- 承認・却下・差戻しは M4-04 で扱う
- Service 状態遷移テストと M4 全体確認は M4-05 で扱う
