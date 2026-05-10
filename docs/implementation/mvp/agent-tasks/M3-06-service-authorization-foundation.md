# M3-06 Service authorization foundation

## 目的

Service 層で会社境界と権限セットに基づく簡易認可を行う土台を作る。

## 対応範囲

- Service 層から `CurrentUserProvider` を参照できる構成にする
- 会社境界チェックの共通方針を作る
- `TENANT_VIEWER` / `TENANT_EDITOR` / `TENANT_MANAGER` の権限判定方針を作る
- 他社データ参照・更新を拒否する基盤を作る
- 権限不足時の例外方針を作る
- 操作ログをアプリケーションログとして出す方針を作る

## 対応ファイル

- `apps/web/src/main/java/com/example/workops/common/security/`
- `apps/web/src/main/java/com/example/workops/common/exception/`
- `docs/implementation/mvp/agent-tasks/M3-06-service-authorization-foundation.md`

## 除外範囲

- 申請管理の個別認可実装
- 資産管理の個別認可実装
- マスタ管理の個別認可実装
- 監査ログテーブル
- 履歴テーブル
- PLATFORM 管理者の本格認可

## 完了条件

Service 層で使う会社境界・権限判定の土台が作られ、M4以降の業務Serviceから利用できる。

## 確認方法

- `TENANT_VIEWER` / `TENANT_EDITOR` / `TENANT_MANAGER` の判定ができることを確認する
- 利用者の `company_id` と対象データの `company_id` を比較できることを確認する
- 権限不足・会社不一致時に利用を拒否できることを確認する

## 実装時の記録

### 実装方針

- URL / 機能認可は Spring Security method security で扱う。
- `SecurityConfig` で `@EnableMethodSecurity` を有効化する。
- local profile では `LocalAuthenticationFilter` でDB由来ユーザーを `Authentication` として `SecurityContext` に設定する。
- `LoginUserContext` は `Authentication.principal` に設定する。
- `permission_sets.code` は `GrantedAuthority` に変換する。
- M3-06 ではテスト専用 Service に `@PreAuthorize` を付け、method security が `hasAuthority` で動くことを確認する。
- Webページ単位の確認用に、`TENANT_MANAGER` のみ表示できる `@PreAuthorize("hasAuthority('TENANT_MANAGER')")` 付きControllerを作る。
- 確認用ページは `/auth/authorization/manager` とする。
- 権限セットコードは `PermissionSetCode` enum で固定する。
- 権限判定は Spring Security の `Authentication.authorities` を見る。
- Cognito group / scope は MVP 業務認可の正本にしない。
- DB の `permission_sets` を権限の正本にする。
- 会社境界は Interceptor では扱わない。
- 会社境界は M4 / M5 の Mapper SQL で `company_id` を WHERE 句に入れて扱う。
- 一覧は `WHERE company_id = ?` を必須にする。
- 詳細取得は `WHERE id = ? AND company_id = ?` を必須にする。
- 更新 / 削除は `WHERE id = ? AND company_id = ?` を必須にする。
- `PermissionEvaluator` / ACL は MVP では過剰なため採用しない。
- 例外クラスは M3-06 では増やさない。
- 業務 Service 実装時に必要になった時点で、Spring Security 標準の `AccessDeniedException` を使う。

### M4 / M5 での使用例

```java
requestMapper.findByIdAndCompanyId(id, currentUser.companyId());
assetMapper.updateByIdAndCompanyId(id, currentUser.companyId(), ...);
```

```java
@PreAuthorize("hasAuthority('TENANT_MANAGER')")
public void approve(Long requestId) {
    ...
}
```

### 変更ファイル

- `apps/web/src/main/java/com/example/workops/common/security/SecurityConfig.java`
- `apps/web/src/main/java/com/example/workops/common/security/PermissionSetCode.java`
- `apps/web/src/main/java/com/example/workops/common/security/WorkOpsAuthenticationFactory.java`
- `apps/web/src/main/java/com/example/workops/common/security/LocalAuthenticationFilter.java`
- `apps/web/src/main/java/com/example/workops/common/security/CurrentUserProvider.java`
- `apps/web/src/main/java/com/example/workops/common/web/AuthAuthorizationController.java`
- `apps/web/src/main/resources/templates/auth/authorization-manager.html`
- `apps/web/src/main/resources/templates/index.html`
- `apps/web/src/test/java/com/example/workops/common/security/MethodSecurityPreAuthorizeTests.java`
- `docs/implementation/mvp/agent-tasks/M3-06-service-authorization-foundation.md`

### 確認結果

- `cd apps/web && .\mvnw.cmd test` が成功した。
- `MethodSecurityPreAuthorizeTests` で `TENANT_MANAGER` authority を持つ `Authentication` が `@PreAuthorize("hasAuthority('TENANT_MANAGER')")` 付きメソッドを通過することを確認した。
- `MethodSecurityPreAuthorizeTests` で `TENANT_VIEWER` authority のみを持つ `Authentication` が `AccessDeniedException` になることを確認した。
- local profile 起動で、既定ユーザー `kthm-manager` の `permission_sets.code` が `GrantedAuthority` の `TENANT_MANAGER` に変換されることを `/auth/claims` で確認した。
- local profile 起動で、既定ユーザー `kthm-manager` が `/auth/authorization/manager` へアクセスすると `200` を返すことを確認した。
- `WORKOPS_LOCAL_COGNITO_SUB=00000000-0000-0000-0000-000000000001` で、`kthm-viewer` の `permission_sets.code` が `GrantedAuthority` の `TENANT_VIEWER` に変換されることを `/auth/claims` で確認した。
- `WORKOPS_LOCAL_COGNITO_SUB=00000000-0000-0000-0000-000000000001` で、`kthm-viewer` が `/auth/authorization/manager` へアクセスすると `403` を返すことを確認した。
- 起動確認用Javaプロセスが残っていないことを確認した。

### 残課題

- M4 / M5 の業務 Service 実装時に、Mapper SQL の `company_id` 条件を具体実装する。
- Cognito OAuth2 Login 側も、後続タスクでDB由来 `LoginUserContext` と `GrantedAuthority` を `Authentication` に反映する。

### Notion へ反映する方針変更

- WorkOps の認証・認可基盤は Spring Security の `SecurityContext` / `Authentication` / `GrantedAuthority` を正本にする。
- `LoginUserContext` は `Authentication.principal` として保持する。
- `permission_sets.code` は `GrantedAuthority` として保持する。
- 業務機能認可は `@PreAuthorize("hasAuthority('TENANT_MANAGER')")` のように Spring Security 標準の authority 判定を使う。
- local profile では `LocalAuthenticationFilter` が local sub からDBユーザーを取得し、`Authentication` を作成する。
- Cognito OAuth2 Login 側も後続タスクで `OidcUser.sub -> users.cognito_sub -> LoginUserContext / GrantedAuthority` の流れへ寄せる。
- `CurrentUserAuthorization` / `WorkOpsAuthorization` のような独自認可Beanを業務認可の中心にしない。
- 会社境界は引き続き M4 / M5 の Mapper SQL で `company_id` を WHERE 句に入れて守る。
