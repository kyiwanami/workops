-- Demo tenants and departments are shared across local and AWS dev profiles.
INSERT INTO companies (id, code, name, is_deleted, created_by, updated_by) VALUES
    (1, 'KTHM_PRECISION', '北浜精密機器株式会社', FALSE, NULL, NULL),
    (2, 'AOBA_CARE', '青葉ケアサービス株式会社', FALSE, NULL, NULL);

INSERT INTO departments (id, company_id, code, name, is_deleted, created_by, updated_by) VALUES
    (1, 1, 'ADMIN', '総務部', FALSE, NULL, NULL),
    (2, 1, 'IT', '情報システム部', FALSE, NULL, NULL),
    (3, 1, 'MFG', '製造部', FALSE, NULL, NULL),
    (4, 1, 'SALES', '営業部', FALSE, NULL, NULL),
    (5, 2, 'OPS', '運営部', FALSE, NULL, NULL);
