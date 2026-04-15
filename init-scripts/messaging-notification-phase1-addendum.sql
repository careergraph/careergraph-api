BEGIN;

CREATE TABLE IF NOT EXISTS thread_deletions (
    id VARCHAR(255) PRIMARY KEY,
    created_date TIMESTAMP NOT NULL DEFAULT NOW(),
    last_modified_date TIMESTAMP,
    created_by VARCHAR(255),
    last_modified_by VARCHAR(255),
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    thread_id VARCHAR(255) NOT NULL REFERENCES message_threads(id) ON DELETE CASCADE,
    account_id VARCHAR(255) NOT NULL REFERENCES accounts(id),
    deleted_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_thread_deletion_thread_account UNIQUE (thread_id, account_id)
);

CREATE INDEX IF NOT EXISTS idx_thread_deletion_account ON thread_deletions(account_id);

ALTER TABLE message_threads
    ADD COLUMN IF NOT EXISTS archived_by_company BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS archived_by_company_at TIMESTAMP,
    ADD COLUMN IF NOT EXISTS archived_by_candidate BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS archived_by_candidate_at TIMESTAMP;

CREATE TABLE IF NOT EXISTS user_blocks (
    id VARCHAR(255) PRIMARY KEY,
    created_date TIMESTAMP NOT NULL DEFAULT NOW(),
    last_modified_date TIMESTAMP,
    created_by VARCHAR(255),
    last_modified_by VARCHAR(255),
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    blocker_account_id VARCHAR(255) NOT NULL REFERENCES accounts(id),
    blocked_account_id VARCHAR(255) NOT NULL REFERENCES accounts(id),
    reason VARCHAR(255),
    blocked_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_user_block_blocker_blocked UNIQUE (blocker_account_id, blocked_account_id)
);

CREATE INDEX IF NOT EXISTS idx_user_block_blocker ON user_blocks(blocker_account_id);
CREATE INDEX IF NOT EXISTS idx_user_block_blocked ON user_blocks(blocked_account_id);

ALTER TABLE messages
    ADD COLUMN IF NOT EXISTS delete_type VARCHAR(20);

COMMIT;
