-- M2-04 local seed: tenant, user, permission, and master data only.
INSERT INTO companies (id, code, name, is_deleted, created_by, updated_by) VALUES
    (1, 'KTHM_PRECISION', '北浜精密機器株式会社', FALSE, NULL, NULL),
    (2, 'AOBA_CARE', '青葉ケアサービス株式会社', FALSE, NULL, NULL);

INSERT INTO departments (id, company_id, code, name, is_deleted, created_by, updated_by) VALUES
    (1, 1, 'ADMIN', '総務部', FALSE, NULL, NULL),
    (2, 1, 'IT', '情報システム部', FALSE, NULL, NULL),
    (3, 1, 'MFG', '製造部', FALSE, NULL, NULL),
    (4, 1, 'SALES', '営業部', FALSE, NULL, NULL),
    (5, 2, 'OPS', '運営部', FALSE, NULL, NULL);

INSERT INTO users (id, company_id, department_id, cognito_sub, username, name, email, actor_type, is_deleted, created_by, updated_by) VALUES
    (1, 1, 2, '00000000-0000-0000-0000-000000000001', 'kthm-viewer', '北浜 閲覧者', 'kthm-viewer@example.local', 'TENANT', FALSE, NULL, NULL),
    (2, 1, 2, '00000000-0000-0000-0000-000000000002', 'kthm-editor', '北浜 編集者', 'kthm-editor@example.local', 'TENANT', FALSE, NULL, NULL),
    (3, 1, 1, '00000000-0000-0000-0000-000000000003', 'kthm-manager', '北浜 管理者', 'kthm-manager@example.local', 'TENANT', FALSE, NULL, NULL),
    (4, 2, 5, '00000000-0000-0000-0000-000000000004', 'aoba-viewer', '青葉 閲覧者', 'aoba-viewer@example.local', 'TENANT', FALSE, NULL, NULL),
    (5, 2, 5, '00000000-0000-0000-0000-000000000005', 'aoba-editor', '青葉 編集者', 'aoba-editor@example.local', 'TENANT', FALSE, NULL, NULL),
    (6, 2, 5, '00000000-0000-0000-0000-000000000006', 'aoba-manager', '青葉 管理者', 'aoba-manager@example.local', 'TENANT', FALSE, NULL, NULL);

INSERT INTO permission_sets (id, code, name, description, is_deleted, created_by, updated_by) VALUES
    (1, 'TENANT_VIEWER', '閲覧者', '参照操作のみを行うテナント利用者', FALSE, NULL, NULL),
    (2, 'TENANT_EDITOR', '編集者', '申請や資産の登録・更新を行うテナント利用者', FALSE, NULL, NULL),
    (3, 'TENANT_MANAGER', '管理者', '承認や管理操作を行うテナント利用者', FALSE, NULL, NULL);

INSERT INTO user_permission_sets (user_id, permission_set_id) VALUES
    (1, 1),
    (2, 2),
    (3, 3),
    (4, 1),
    (5, 2),
    (6, 3);

INSERT INTO common_master (id, code, name, description, is_deleted, created_by, updated_by) VALUES
    (1, 'REQUEST_STATUS', '申請ステータス', '申請の現在状態を表す共通マスタ', FALSE, NULL, NULL),
    (2, 'ASSET_STATUS', '資産ステータス', '資産台帳の現在状態を表す共通マスタ', FALSE, NULL, NULL);

INSERT INTO common_master_values (id, common_master_id, code, name, sort_order, is_deleted, created_by, updated_by) VALUES
    (1, 1, 'DRAFT', '下書き', 10, FALSE, NULL, NULL),
    (2, 1, 'SUBMITTED', '提出済み', 20, FALSE, NULL, NULL),
    (3, 1, 'APPROVED', '承認済み', 30, FALSE, NULL, NULL),
    (4, 1, 'REJECTED', '却下', 40, FALSE, NULL, NULL),
    (5, 1, 'WITHDRAWN', '取下げ', 50, FALSE, NULL, NULL),
    (6, 2, 'AVAILABLE', '利用可能', 10, FALSE, NULL, NULL),
    (7, 2, 'LENT', '貸出中', 20, FALSE, NULL, NULL),
    (8, 2, 'REPAIRING', '修理中', 30, FALSE, NULL, NULL),
    (9, 2, 'DISPOSED', '廃棄済み', 40, FALSE, NULL, NULL);

INSERT INTO generic_master (id, code, name, description, is_deleted, created_by, updated_by) VALUES
    (1, 'ASSET_CATEGORY', '資産カテゴリ', '会社別に資産分類を定義する汎用マスタ', FALSE, NULL, NULL),
    (2, 'REQUEST_TYPE', '申請種別', '会社別に申請種別を定義する汎用マスタ', FALSE, NULL, NULL);

INSERT INTO generic_master_values (id, generic_master_id, company_id, code, name, sort_order, is_deleted, created_by, updated_by) VALUES
    (1, 1, 1, 'NOTE_PC', 'ノートPC', 10, FALSE, NULL, NULL),
    (2, 1, 1, 'DESKTOP_PC', 'デスクトップPC', 20, FALSE, NULL, NULL),
    (3, 1, 1, 'MONITOR', 'モニター', 30, FALSE, NULL, NULL),
    (4, 1, 1, 'TABLET', 'タブレット', 40, FALSE, NULL, NULL),
    (5, 1, 1, 'NETWORK_DEVICE', 'ネットワーク機器', 50, FALSE, NULL, NULL),
    (6, 1, 1, 'OTHER', 'その他', 60, FALSE, NULL, NULL),
    (7, 1, 2, 'PC', 'PC', 10, FALSE, NULL, NULL),
    (8, 1, 2, 'MOBILE', 'モバイル端末', 20, FALSE, NULL, NULL),
    (9, 1, 2, 'OTHER', 'その他', 30, FALSE, NULL, NULL),
    (10, 2, 1, 'EQUIPMENT_PURCHASE', '備品購入申請', 10, FALSE, NULL, NULL),
    (11, 2, 1, 'REPAIR_REQUEST', '修理依頼申請', 20, FALSE, NULL, NULL),
    (12, 2, 1, 'DISPOSAL_REQUEST', '廃棄申請', 30, FALSE, NULL, NULL),
    (13, 2, 2, 'PC_REQUEST', 'PC申請', 10, FALSE, NULL, NULL),
    (14, 2, 2, 'MOBILE_REQUEST', 'モバイル端末申請', 20, FALSE, NULL, NULL),
    (15, 2, 2, 'OTHER_REQUEST', 'その他申請', 30, FALSE, NULL, NULL);
