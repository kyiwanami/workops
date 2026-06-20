# P2-10-03 Phase2 README

## 目的

Phase 2 の構築、実行確認、削除、再 deploy、調査を README から再現できる状態にする。

P2-1 から P2-9 までに分散した手順を整理し、P2-10 の総合確認入口として README を更新する。

## ユーザー要求

- P2-10 では README 整理を扱う
- README に従って Phase 2 を再現できる状態にする
- GitHub Actions OIDC deploy は Phase 2 の暫定手段として明記する
- Phase 2α で CodePipeline + CodeBuild へ移行し、GitHub Actions workflow を撤去する前提を明記する
- 実 account ID、role ARN、SSO role ARN、SSO user、credential、CloudFront domain、public IP、実 Cognito `sub` は git 管理文書に残さない
- AWS 実環境確認はユーザー確認として扱う

## 前提

- P2-1 から P2-9 の実装が完了している
- P2-10-01 で CDK entrypoint が用途別に分割されている
- P2-10-02 でトップページと認可表示制御が整理されている
- `infra-dev.yml` は非 runtime / 維持対象 Stack の deploy を担う
- `app-deploy-dev.yml` は課金 runtime Stack と `apps/web` deploy を手動実行する
- 既存データ移行と後方互換性は考慮しない

## 案

README は次の流れで整理する。

1. Phase 2 構成概要
2. 事前準備
3. `DeployStack` 初回手動 deploy
4. GitHub Environment `dev` 設定
5. 非 runtime / 維持基盤 deploy
6. runtime / app deploy
7. Cognito Hosted UI login 確認
8. PLATFORM / TENANT 管理導線確認
9. CloudWatch Logs 調査
10. 再 deploy
11. 実行確認セッション終了時の削除
12. トラブル時確認
13. Phase 2α への引き継ぎ

AWS 操作手順では、profile と region を明示し、アクセスキー系環境変数を削除してから `aws sts get-caller-identity --region $env:AWS_REGION` で認証先を確認する。

README は実値ではなく placeholder を使う。

## ADR

- README を Phase 2 総合確認の入口にする。P2-1 から P2-9 の個別 task に分散した手順だけでは第三者が再現しづらいため。
- 初回構築、通常 deploy、実行確認、削除、トラブル対応を分けて書く。課金 runtime Stack と維持基盤 Stack の lifecycle が異なるため。
- GitHub Actions OIDC deploy は暫定手段として明記する。Phase 2α で CodePipeline + CodeBuild へ移行するため。
- 実環境固有値は README に記録しない。credential、実 account ID、実 Cognito `sub`、CloudFront domain などは git 管理文書へ残さないため。
- AWS dev の実操作結果はユーザー確認として扱う。エージェントの CDK synth、test、workflow 静的確認を実環境確認の代替にしないため。

## 対応範囲

- README の Phase 2 セクション整理
- CDK entrypoint 変更後のコマンド反映
- GitHub Actions OIDC deploy 手順整理
- `infra-dev.yml` と `app-deploy-dev.yml` の実行条件整理
- CloudFront HTTPS health check 手順整理
- Cognito Hosted UI login / logout 確認手順整理
- PLATFORM / TENANT ユーザー管理確認手順整理
- CloudWatch Logs 確認手順整理
- 実行確認セッション終了時の削除手順整理
- Phase 2α への引き継ぎ事項整理

## 除外範囲

- Phase 2α の CodePipeline / CodeBuild 実装
- GitHub Actions workflow 撤去
- 本番運用設計
- Phase 3 の `apps/api`
- Phase 4 の PDF
- Phase 5 のメール / バッチ / Lambda
- CloudWatch Alarm
- Observability Dashboard
- OpenTelemetry

## 完了条件

- README から Phase 2 の構成概要を説明できる
- README に従って `DeployStack` 初回手動 deploy 手順を実行できる
- README に従って GitHub Environment `dev` を設定できる
- README に従って非 runtime / 維持基盤 deploy を実行できる
- README に従って runtime / app deploy を実行できる
- README に従って CloudFront HTTPS `/actuator/health` を確認できる
- README に従って Cognito Hosted UI login / logout を確認できる
- README に従って PLATFORM / TENANT 管理導線を確認できる
- README に従って CloudWatch Logs を調査できる
- README に従って実行確認セッション Stack を削除できる
- README に Phase 2α で移行する内容が明記されている
- README に実環境固有値や credential が残っていない

## 確認方法

エージェント確認:

```powershell
cd C:\git\workops
git diff --check
```

README 整合確認:

- Stack 名が実装済み CDK Stack 名と一致している
- workflow 名が `.github/workflows` の実ファイル名と一致している
- CDK command が P2-10-01 の entrypoint 分割後の script と一致している
- AWS 操作手順に profile / region / caller identity 確認が含まれる
- secret、password、credential、実 account ID、role ARN、CloudFront domain、実 Cognito `sub` が含まれない

ユーザー確認:

- README 手順に従い、AWS dev で構築、login、主要業務操作、ユーザー管理、ログ調査、GitHub Actions deploy、削除を確認する
- 実行できなかった確認は、P2-10-04 の完了記録で未確認事項として扱う
