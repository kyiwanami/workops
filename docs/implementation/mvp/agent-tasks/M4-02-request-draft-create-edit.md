# M4-02 申請作成 / 編集 / 下書き保存

## 目的

申請者本人が申請を作成し、DRAFT 状態の申請を編集・下書き保存できるようにする。

## M4 共通方針

- DRAFT 編集は申請者本人のみ許可する
- TENANT_MANAGER は、自分の申請について TENANT_EDITOR 相当の作成・編集・提出・取下げができる
- M4 では申請種別マスタ管理は作らない
- M4 で申請作成に選択肢が必要な場合は、M2 seed 済みの値を参照するだけにする
- `process_type_code` は申請処理タイプであり、画面上の申請種別管理そのものとして扱わない
- 申請種別の登録・編集・停止は M6 で扱う
- M4 では `asset_id` は任意・未選択中心で扱う
- 資産紐づけは、M5 で資産カタログが成立した後、M5 後半または M5 後の接続調整で扱う
- M4 では資産 CRUD、資産ステータス変更、申請承認による資産ステータス自動変更には踏み込まない
- 操作可否は本人条件・権限・ステータスで制御する
- 会社境界は authority ではなく Mapper SQL の `company_id` 条件で守る

## 対応範囲

- 申請作成画面を作成する
- 申請編集画面を作成する
- 下書き保存を実装する
- 新規作成時は `status_code = 'DRAFT'` とする
- 新規作成時は `company_id` に現在ユーザーの `companyId` を設定する
- 新規作成時は `requester_user_id` に現在ユーザーの `userId` を設定する
- 新規作成時は `created_by` と `updated_by` に現在ユーザーの `userId` を設定する
- 編集時は `updated_by` に現在ユーザーの `userId` を設定する
- 編集対象は `id + company_id` で取得する
- 編集保存は `id + company_id` を条件に更新する
- 編集保存は、申請者本人かつ `status_code = 'DRAFT'` の申請だけ許可する
- TENANT_EDITOR / TENANT_MANAGER は自分の申請を作成・編集できる
- TENANT_VIEWER は申請を作成・編集できない
- `process_type_code` は M2 seed 済みの `PROCESS_TYPE` 値 `PURCHASE` / `REPAIR` / `DISPOSAL` から選択する
- `asset_id` は M4 では入力させず、作成・編集時は `NULL` として扱う
- 入力項目は `process_type_code`、`title`、`content` とする
- `title` は必須とし、DB 定義に合わせて 100 文字以内にする
- `content` は任意とし、DB 定義に合わせて 2000 文字以内にする

## 対応ファイル

- `apps/web/src/main/java/com/example/workops/request/web/RequestController.java`
- `apps/web/src/main/java/com/example/workops/request/form/RequestForm.java`
- `apps/web/src/main/java/com/example/workops/request/service/RequestCommandService.java`
- `apps/web/src/main/java/com/example/workops/request/service/RequestQueryService.java`
- `apps/web/src/main/java/com/example/workops/request/mapper/RequestMapper.java`
- `apps/web/src/main/java/com/example/workops/request/model/RequestDetail.java`
- `apps/web/src/main/java/com/example/workops/request/model/RequestProcessTypeOption.java`
- `apps/web/src/main/resources/mapper/request/RequestMapper.xml`
- `apps/web/src/main/resources/templates/request/form.html`
- `apps/web/src/main/resources/templates/request/detail.html`
- `docs/implementation/mvp/agent-tasks/M4-02-request-draft-create-edit.md`

## 除外範囲

- 申請提出
- 申請取下げ
- 承認
- 却下
- 差戻し
- 申請種別の登録・編集・停止
- 資産選択 UI
- 資産 CRUD
- 資産ステータス変更
- 申請承認による資産ステータス自動変更
- Mapper / Testcontainers 強化

## 完了条件

TENANT_EDITOR または TENANT_MANAGER が自分の申請を DRAFT として作成できる。
申請者本人だけが DRAFT の申請を編集・下書き保存できる。
TENANT_VIEWER、他人の申請、DRAFT 以外の申請は編集・下書き保存できない。

## 確認方法

- `cd apps/web && .\mvnw.cmd test`
- local profile の TENANT_EDITOR で申請を作成できることを確認する
- local profile の TENANT_MANAGER で自分の申請を作成できることを確認する
- 申請者本人が DRAFT の申請を編集できることを確認する
- TENANT_VIEWER が申請作成・編集できないことを確認する
- 他人の DRAFT 申請を編集できないことを確認する
- DRAFT 以外の申請を編集できないことを確認する

## 実装時の記録

実装後に、次をこのファイルへ追記する。

- 実装方針
- 変更ファイル
- 実装結果
- 確認結果
- 残課題
