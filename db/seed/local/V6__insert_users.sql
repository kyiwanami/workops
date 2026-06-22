-- Local profile uses fixed Cognito sub values for repeatable login and mapper tests.
INSERT INTO users (id, company_id, department_id, cognito_sub, username, name, email, actor_type, is_deleted, created_by, updated_by) VALUES
    (1, 1, 2, '00000000-0000-0000-0000-000000000001', 'kthm-viewer', '北浜 閲覧者', 'kthm-viewer@example.local', 'TENANT', FALSE, NULL, NULL),
    (2, 1, 2, '00000000-0000-0000-0000-000000000002', 'kthm-editor', '北浜 編集者', 'kthm-editor@example.local', 'TENANT', FALSE, NULL, NULL),
    (3, 1, 1, '00000000-0000-0000-0000-000000000003', 'kthm-manager', '北浜 管理者', 'kthm-manager@example.local', 'TENANT', FALSE, NULL, NULL),
    (4, 2, 5, '00000000-0000-0000-0000-000000000004', 'aoba-viewer', '青葉 閲覧者', 'aoba-viewer@example.local', 'TENANT', FALSE, NULL, NULL),
    (5, 2, 5, '00000000-0000-0000-0000-000000000005', 'aoba-editor', '青葉 編集者', 'aoba-editor@example.local', 'TENANT', FALSE, NULL, NULL),
    (6, 2, 5, '00000000-0000-0000-0000-000000000006', 'aoba-manager', '青葉 管理者', 'aoba-manager@example.local', 'TENANT', FALSE, NULL, NULL),
    (7, NULL, NULL, '00000000-0000-0000-0000-000000000000', 'platform-admin', 'WorkOps 管理者', 'platform-admin@example.local', 'PLATFORM', FALSE, NULL, NULL);

INSERT INTO user_permission_sets (user_id, permission_set_id) VALUES
    (1, 1),
    (2, 2),
    (3, 3),
    (4, 1),
    (5, 2),
    (6, 3),
    (7, 4);
