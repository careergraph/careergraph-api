BEGIN;

CREATE TABLE IF NOT EXISTS message_threads (
    id VARCHAR(255) PRIMARY KEY,
    created_date TIMESTAMP NOT NULL DEFAULT NOW(),
    last_modified_date TIMESTAMP,
    created_by VARCHAR(255),
    last_modified_by VARCHAR(255),
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    company_id VARCHAR(255) NOT NULL REFERENCES companies (id),
    candidate_id VARCHAR(255) NOT NULL REFERENCES candidates (id),
    application_id VARCHAR(255) REFERENCES applications (id),
    last_message_at TIMESTAMP,
    last_message_preview VARCHAR(255),
    CONSTRAINT uk_message_thread_company_candidate UNIQUE (company_id, candidate_id)
);

CREATE INDEX IF NOT EXISTS idx_message_thread_company ON message_threads (company_id);

CREATE INDEX IF NOT EXISTS idx_message_thread_candidate ON message_threads (candidate_id);

CREATE INDEX IF NOT EXISTS idx_message_thread_last_message ON message_threads (last_message_at DESC);

CREATE TABLE IF NOT EXISTS messages (
    id VARCHAR(255) PRIMARY KEY,
    created_date TIMESTAMP NOT NULL DEFAULT NOW(),
    last_modified_date TIMESTAMP,
    created_by VARCHAR(255),
    last_modified_by VARCHAR(255),
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    thread_id VARCHAR(255) NOT NULL REFERENCES message_threads (id) ON DELETE CASCADE,
    sender_id VARCHAR(255) NOT NULL REFERENCES accounts (id),
    content TEXT NOT NULL,
    content_type VARCHAR(20) NOT NULL DEFAULT 'TEXT',
    file_url VARCHAR(500),
    file_name VARCHAR(255),
    file_size BIGINT,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_messages_thread_created ON messages (thread_id, created_date DESC);

CREATE INDEX IF NOT EXISTS idx_messages_sender ON messages (sender_id);

CREATE TABLE IF NOT EXISTS message_reads (
    id VARCHAR(255) PRIMARY KEY,
    created_date TIMESTAMP NOT NULL DEFAULT NOW(),
    last_modified_date TIMESTAMP,
    created_by VARCHAR(255),
    last_modified_by VARCHAR(255),
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    thread_id VARCHAR(255) NOT NULL REFERENCES message_threads (id) ON DELETE CASCADE,
    account_id VARCHAR(255) NOT NULL REFERENCES accounts (id),
    last_read_message_id VARCHAR(255) REFERENCES messages (id),
    last_read_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_message_read_thread_account UNIQUE (thread_id, account_id)
);

CREATE INDEX IF NOT EXISTS idx_message_read_account ON message_reads (account_id);

CREATE TABLE IF NOT EXISTS notifications (
    id VARCHAR(255) PRIMARY KEY,
    created_date TIMESTAMP NOT NULL DEFAULT NOW(),
    last_modified_date TIMESTAMP,
    created_by VARCHAR(255),
    last_modified_by VARCHAR(255),
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    recipient_id VARCHAR(255) NOT NULL REFERENCES accounts (id),
    type VARCHAR(50) NOT NULL,
    title VARCHAR(255) NOT NULL,
    body VARCHAR(500) NOT NULL,
    data JSONB,
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    read_at TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_notification_recipient_created ON notifications (
    recipient_id,
    created_date DESC
);

CREATE INDEX IF NOT EXISTS idx_notification_recipient_read ON notifications (recipient_id, is_read);

CREATE INDEX IF NOT EXISTS idx_notification_unread ON notifications (
    recipient_id,
    created_date DESC
)
WHERE
    is_read = FALSE;

CREATE TABLE IF NOT EXISTS notification_preferences (
    id VARCHAR(255) PRIMARY KEY,
    created_date TIMESTAMP NOT NULL DEFAULT NOW(),
    last_modified_date TIMESTAMP,
    created_by VARCHAR(255),
    last_modified_by VARCHAR(255),
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    account_id VARCHAR(255) NOT NULL REFERENCES accounts (id),
    type VARCHAR(50) NOT NULL,
    in_app_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    email_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT uk_notification_pref_account_type UNIQUE (account_id, type)
);

COMMIT;