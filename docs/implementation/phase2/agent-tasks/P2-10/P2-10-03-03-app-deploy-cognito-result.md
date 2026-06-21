# P2-10-03-03 app deploy / Cognito UI result

## 目的

Phase 2 AWS dev の `app-deploy-dev.yml` 実行後のユーザー確認結果を記録し、RDS deploy 直後の `users.cognito_sub` 未接続状態から、Platform login、会社作成、TENANT user 作成、Tenant login へ進む初回導線を明確にする。

## ユーザー要求

- app deploy は実施済み
- Cognito Hosted UI までは確認済み
- AWS dev RDS の `platform-admin` に Platform 管理者 Cognito user の `sub` を登録後、Platform / Tenant の導線が文句なしに動作した
- RDS deploy 直後は Cognito user と `users.cognito_sub` が未接続であることを導線として書く
- 初回導線は Platform 管理者 `sub` 登録、Platform login、会社作成、TENANT user 作成、Tenant login の順にする
- 実 Cognito `sub` は git 管理文書に残さない

## 前提

- AWS dev 実環境確認はユーザー確認として扱う
- `apps/web/src/main/resources/db/seed/aws-dev/V6__insert_users.sql` は AWS dev の `users.cognito_sub` を `NULL` で seed する
- コーディングエージェントは AWS dev RDS への MySQL / SQL 実行を、ユーザーの明示指示なしには行わない
- AWS dev RDS への MySQL / SQL 実行経路は RDS Console integrated CloudShell VPC に限定する
- MySQL 接続後は `USE workops;` で対象 database を明示する
- 既存データ移行と後方互換性は考慮しない

## 案

現時点の確認状態を次のように扱う。

| 確認項目 | 状態 | 扱い |
| --- | --- | --- |
| `app-deploy-dev.yml` | ユーザーが deploy 済み | ユーザー確認済みとして記録する |
| Cognito Hosted UI 到達 | ユーザーが確認済み | ユーザー確認済みとして記録する |
| Cognito login 後の業務画面遷移 | Platform / Tenant ともにユーザー確認済み | ユーザー確認済みとして記録する |
| DB 由来権限の画面確認 | トップページ導線と権限外 URL の 403 をユーザー確認済み | ユーザー確認済みとして記録する |
| 実 Cognito `sub` | 記録しない | git 管理文書へ残さない |

AWS dev 初回接続導線は次の順に固定する。

1. AWS dev RDS の `platform-admin` に Platform 管理者 Cognito user の `sub` を登録する
2. `/login/platform` で Platform 用 login を実行する
3. Platform 画面で会社を作成する
4. 作成した会社に所属する TENANT user を作成する
5. `/login/tenant` で作成した TENANT user としてログインする
6. トップページの導線と権限外 URL の 403 を確認する

初回の Platform 管理者紐付け SQL は placeholder だけを文書に残す。

```sql
USE workops;

UPDATE users
SET cognito_sub = '<platform-admin-cognito-sub>'
WHERE username = 'platform-admin'
  AND email = 'platform-admin@example.local'
  AND actor_type = 'PLATFORM'
  AND cognito_sub IS NULL;

SELECT ROW_COUNT() AS updated_rows;
```

TENANT user は Platform 画面のユーザー作成処理で Cognito user 作成と `users.cognito_sub` 登録を行う。
そのため、TENANT user の `sub` を手動 SQL で先に登録する導線にはしない。

## ADR

- Cognito Hosted UI 到達は確認済みとして記録する。ユーザーが実 AWS dev の Hosted UI まで確認したため。
- 業務画面確認は未完了として扱う。`users.cognito_sub` 未登録により、Cognito `sub` と DB user の突合が成立していないため。
- AWS dev 初回は `platform-admin` だけを手動で Cognito user に紐付ける。RDS deploy 直後は Cognito と DB user が未接続であり、Platform login ができないと会社作成と TENANT user 作成に進めないため。
- TENANT user は画面から作成する。アプリのユーザー作成処理で Cognito user 作成と `users.cognito_sub` 登録を確認することが Phase 2 の確認対象だから。
- 実 Cognito `sub` は記録しない。git 管理文書に実ユーザー識別子を残さないため。
- 本記録は P2-10-04 の完了記録へ引き継ぐ。確認済み事項と未確認事項を混同しないため。

## 実施結果

- ユーザーが `app-deploy-dev.yml` による app deploy を実施した
- ユーザーが Cognito Hosted UI 到達を確認した
- AWS dev RDS の `platform-admin` に Platform 管理者 Cognito user の `sub` を登録後、Platform / Tenant の導線が文句なしに動作した
- 実 Cognito `sub` は記録していない

## 未完了事項

- P2-10-03-03 の範囲では未完了事項なし

## 次アクション

1. P2-10-03 の README / 実確認結果に漏れがないか確認する
2. P2-10-03 完了後に P2-10-04 completion record へ進む

## 検証結果

- 本ファイルには実 account ID、role ARN、credential、CloudFront domain、public IP、実 Cognito `sub` を記録していない
- 本ファイルはユーザー確認結果の記録であり、エージェントによる AWS dev 実操作は行っていない
