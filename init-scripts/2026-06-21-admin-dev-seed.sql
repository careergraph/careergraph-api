BEGIN;

-- Dev admin account seed for CareerGraph.
-- Safe to run multiple times.
-- Plaintext password for this dev admin seed is: Password@123

INSERT INTO accounts (
    id,
    email,
    password_hash,
    role,
    email_verified,
    candidate_id,
    company_id,
    created_date,
    last_modified_date,
    status
)
VALUES (
    'd0000001-0000-0000-0000-000000000001',
    'admin@careergraph.vn',
    '$2a$10$q7z/fgw4RG/e1QoUZJMX4OQiU8IHlb/hvp/Pe2M4c9dR.MmLDeTTy',
    'ADMIN',
    true,
    NULL,
    NULL,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP,
    'ACTIVE'
)
ON CONFLICT (email) DO UPDATE
SET
    password_hash = EXCLUDED.password_hash,
    role = 'ADMIN',
    email_verified = true,
    candidate_id = NULL,
    company_id = NULL,
    status = 'ACTIVE',
    last_modified_date = CURRENT_TIMESTAMP;

COMMIT;
