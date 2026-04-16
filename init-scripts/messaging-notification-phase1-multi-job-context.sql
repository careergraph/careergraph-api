BEGIN;

ALTER TABLE messages
    ADD COLUMN IF NOT EXISTS job_context_id VARCHAR(255) REFERENCES jobs (id);

CREATE INDEX IF NOT EXISTS idx_messages_job_context
    ON messages (thread_id, job_context_id);

COMMIT;