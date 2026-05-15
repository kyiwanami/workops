# WorkOps MVP 動作確認シナリオ

## 位置づけ

このファイルは、WorkOps MVP をローカルで再現し、主要な業務操作を説明するための確認シナリオです。
最終的な UI の見た目や操作感の判断はユーザーが行います。
コーディングエージェントは、`mvnw test` などの機械的検証と、手順が現行実装と一致していることの確認までを担当します。

## 前提

README の手順で MySQL と Spring Boot を起動します。

```powershell
cd C:\git\workops
docker compose up -d workops-mysql

cd C:\git\workops\apps\web
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=local"
```

local profile では `WORKOPS_LOCAL_COGNITO_SUB` で現在ユーザーを切り替えます。
未設定時は北浜精密機器株式会社の管理者で起動します。

## ユーザー切替

Spring Boot を停止し、同じ PowerShell セッションで `WORKOPS_LOCAL_COGNITO_SUB` を設定してから再起動します。

```powershell
$env:WORKOPS_LOCAL_COGNITO_SUB = "00000000-0000-0000-0000-000000000002"
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=local"
```

| 用途 | cognito_sub | 権限 |
| --- | --- | --- |
| 北浜の閲覧者確認 | `00000000-0000-0000-0000-000000000001` | TENANT_VIEWER |
| 北浜の編集者確認 | `00000000-0000-0000-0000-000000000002` | TENANT_EDITOR |
| 北浜の管理者確認 | `00000000-0000-0000-0000-000000000003` | TENANT_MANAGER |
| 青葉の閲覧者確認 | `00000000-0000-0000-0000-000000000004` | TENANT_VIEWER |
| 青葉の編集者確認 | `00000000-0000-0000-0000-000000000005` | TENANT_EDITOR |
| 青葉の管理者確認 | `00000000-0000-0000-0000-000000000006` | TENANT_MANAGER |

## 申請管理

対象 URL:

- `/requests`
- `/requests/new`
- `/requests/{id}`
- `/requests/{id}/edit`
- `/requests/{id}/reject`
- `/requests/{id}/remand`

確認シナリオ:

1. 編集者で起動し、申請一覧から自社の申請だけが表示されることを確認する。
2. 新規申請を下書き保存し、一覧と詳細に表示されることを確認する。
3. 自分の下書き申請を編集できることを確認する。
4. 下書き申請を提出し、状態が `SUBMITTED` になることを確認する。
5. 自分の提出済み申請を取下げできることを確認する。
6. 管理者で起動し、提出済み申請を承認できることを確認する。
7. 管理者で起動し、提出済み申請を却下し、理由が詳細に表示されることを確認する。
8. 管理者で起動し、提出済み申請を差戻し、理由が詳細に表示されることを確認する。
9. 閲覧者で起動し、申請の参照はできるが作成・更新操作ができないことを確認する。

確認観点:

- 編集者は自分の下書きと提出済み申請を操作できる。
- 管理者は提出済み申請を承認、却下、差戻しできる。
- 閲覧者は参照のみできる。
- 他社の申請は一覧、詳細、更新対象に出ない。

## 資産管理

対象 URL:

- `/assets`
- `/assets/new`
- `/assets/{id}`
- `/assets/{id}/edit`
- `/assets/{id}/status`

確認シナリオ:

1. 編集者で起動し、資産一覧から自社の資産だけが表示されることを確認する。
2. 資産コード、資産名、資産分類、管理部署、ステータスで検索できることを確認する。
3. 資産を登録し、一覧と詳細に表示されることを確認する。
4. 登録済み資産を編集できることを確認する。
5. 資産ステータスを変更できることを確認する。
6. 管理者で起動し、資産を論理削除できることを確認する。
7. 閲覧者で起動し、資産の参照はできるが登録・編集・ステータス変更・削除ができないことを確認する。

確認観点:

- 削除済み資産は通常の一覧や選択肢に出ない。
- 削除済み資産を参照している親データは表示できる。
- 他社の資産は一覧、詳細、更新対象に出ない。
- 資産コードは同一会社内で重複登録できない。

## マスタ管理

対象 URL:

- `/masters/request-types`
- `/masters/request-types/new`
- `/masters/request-types/{id}/edit`
- `/masters/asset-categories`
- `/masters/asset-categories/new`
- `/masters/asset-categories/{id}/edit`

確認シナリオ:

1. 管理者で起動し、申請種別マスタを登録できることを確認する。
2. 管理者で起動し、申請種別マスタを編集、論理削除、復活できることを確認する。
3. 管理者で起動し、資産分類マスタを登録できることを確認する。
4. 管理者で起動し、資産分類マスタを編集、論理削除、復活できることを確認する。
5. 削除済み表示を有効にし、削除済みマスタ値が一覧に表示されることを確認する。
6. 編集者と閲覧者で起動し、マスタ変更操作ができないことを確認する。

確認観点:

- 申請種別と資産分類は会社別に管理される。
- 削除済みマスタ値は新規作成・編集用の選択肢から除外される。
- 削除済みマスタ値を参照している親データは表示できる。
- 削除済みコードは再利用できない。

## 業務操作ログ

確認対象:

- 申請操作
- 資産操作
- 申請種別マスタ操作
- 資産分類マスタ操作

確認シナリオ:

1. Spring Boot 起動中の PowerShell を開いたままにする。
2. 申請、資産、マスタの成功操作を実行し、`com.example.workops.operation` の `SUCCESS` ログが出ることを確認する。
3. 権限不足、状態不一致、不正な参照などの拒否操作を実行し、`REJECTED` ログが出ることを確認する。

確認観点:

- ログには `requestId`、`userId`、`companyId`、`actorType`、`authorities`、`operation`、`targetType`、`targetId`、`result`、`reasonCode`、`reasonCommentPresent`、`exceptionType` が出力される。
- `@PreAuthorize` によるアクセス拒否は、M7 方針どおり業務操作ログの対象外とする。
- MVP では監査ログテーブル、監査ログ閲覧画面、CloudWatch Logs 連携手順は扱わない。

## 機械的確認

Docker Desktop が起動している状態で、Maven テストを実行します。

```powershell
cd C:\git\workops\apps\web
.\mvnw.cmd test
```

このテストでは、Service テスト、業務操作ログテスト、Testcontainers MySQL を使う DB / Mapper 統合テストを確認します。
