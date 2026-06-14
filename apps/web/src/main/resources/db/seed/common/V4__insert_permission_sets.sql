-- Permission sets are shared by local and AWS dev profiles.
INSERT INTO permission_sets (id, code, name, description, is_deleted, created_by, updated_by) VALUES
    (1, 'TENANT_VIEWER', '閲覧者', '参照操作のみを行うテナント利用者', FALSE, NULL, NULL),
    (2, 'TENANT_EDITOR', '編集者', '申請や資産の登録・更新を行うテナント利用者', FALSE, NULL, NULL),
    (3, 'TENANT_MANAGER', '管理者', '承認や管理操作を行うテナント利用者', FALSE, NULL, NULL),
    (4, 'PLATFORM_ADMIN', 'WorkOps管理者', 'WorkOps全体を管理する運営側利用者', FALSE, NULL, NULL);
