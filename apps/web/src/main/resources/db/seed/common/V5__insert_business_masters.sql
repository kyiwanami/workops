-- Business masters are authentication-independent seed data.
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
