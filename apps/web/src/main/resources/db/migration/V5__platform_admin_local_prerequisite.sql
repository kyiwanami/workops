-- PLATFORM users are WorkOps operators and do not belong to a tenant company.
ALTER TABLE users
    MODIFY company_id BIGINT NULL;

ALTER TABLE users
    ADD CONSTRAINT ck_users_actor_company
        CHECK (
            (actor_type = 'PLATFORM' AND company_id IS NULL)
            OR (actor_type = 'TENANT' AND company_id IS NOT NULL)
        );

INSERT INTO permission_sets (id, code, name, description, is_deleted, created_by, updated_by) VALUES
    (4, 'PLATFORM_ADMIN', 'WorkOps管理者', 'WorkOps全体を管理する運営側利用者', FALSE, NULL, NULL);

INSERT INTO users (
    id,
    company_id,
    department_id,
    cognito_sub,
    username,
    name,
    email,
    actor_type,
    is_deleted,
    created_by,
    updated_by
) VALUES (
    7,
    NULL,
    NULL,
    '00000000-0000-0000-0000-000000000000',
    'platform-admin',
    'WorkOps 管理者',
    'platform-admin@example.local',
    'PLATFORM',
    FALSE,
    NULL,
    NULL
);

INSERT INTO user_permission_sets (user_id, permission_set_id) VALUES
    (7, 4);
