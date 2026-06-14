-- PLATFORM users are WorkOps operators and do not belong to a tenant company.
ALTER TABLE users
    MODIFY company_id BIGINT NULL;

ALTER TABLE users
    ADD CONSTRAINT ck_users_actor_company
        CHECK (
            (actor_type = 'PLATFORM' AND company_id IS NULL)
            OR (actor_type = 'TENANT' AND company_id IS NOT NULL)
        );
