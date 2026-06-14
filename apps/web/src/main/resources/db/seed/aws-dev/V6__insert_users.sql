-- AWS dev profile leaves Cognito sub unset until real Cognito users are connected.
INSERT INTO users (id, company_id, department_id, cognito_sub, username, name, email, actor_type, is_deleted, created_by, updated_by) VALUES
    (1, 1, 2, NULL, 'kthm-viewer', '北浜 閲覧者', 'kthm-viewer@example.local', 'TENANT', FALSE, NULL, NULL),
    (2, 1, 2, NULL, 'kthm-editor', '北浜 編集者', 'kthm-editor@example.local', 'TENANT', FALSE, NULL, NULL),
    (3, 1, 1, NULL, 'kthm-manager', '北浜 管理者', 'kthm-manager@example.local', 'TENANT', FALSE, NULL, NULL),
    (4, 2, 5, NULL, 'aoba-viewer', '青葉 閲覧者', 'aoba-viewer@example.local', 'TENANT', FALSE, NULL, NULL),
    (5, 2, 5, NULL, 'aoba-editor', '青葉 編集者', 'aoba-editor@example.local', 'TENANT', FALSE, NULL, NULL),
    (6, 2, 5, NULL, 'aoba-manager', '青葉 管理者', 'aoba-manager@example.local', 'TENANT', FALSE, NULL, NULL),
    (7, NULL, NULL, NULL, 'platform-admin', 'WorkOps 管理者', 'platform-admin@example.local', 'PLATFORM', FALSE, NULL, NULL);

INSERT INTO user_permission_sets (user_id, permission_set_id) VALUES
    (1, 1),
    (2, 2),
    (3, 3),
    (4, 1),
    (5, 2),
    (6, 3),
    (7, 4);
