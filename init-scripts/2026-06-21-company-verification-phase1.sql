BEGIN;

ALTER TABLE companies
    ADD COLUMN IF NOT EXISTS verification_status varchar(50) NOT NULL DEFAULT 'NOT_SUBMITTED',
    ADD COLUMN IF NOT EXISTS operational_status varchar(50) NOT NULL DEFAULT 'ACTIVE',
    ADD COLUMN IF NOT EXISTS tax_code varchar(50),
    ADD COLUMN IF NOT EXISTS legal_representative_name varchar(255),
    ADD COLUMN IF NOT EXISTS verification_business_email varchar(255),
    ADD COLUMN IF NOT EXISTS verification_website varchar(500),
    ADD COLUMN IF NOT EXISTS verification_submitted_at timestamp,
    ADD COLUMN IF NOT EXISTS verification_reviewed_at timestamp,
    ADD COLUMN IF NOT EXISTS verification_reviewed_by_account_id uuid,
    ADD COLUMN IF NOT EXISTS verification_admin_note text,
    ADD COLUMN IF NOT EXISTS block_reason text,
    ADD COLUMN IF NOT EXISTS blocked_at timestamp,
    ADD COLUMN IF NOT EXISTS blocked_by_account_id uuid,
    ADD COLUMN IF NOT EXISTS unblocked_at timestamp,
    ADD COLUMN IF NOT EXISTS unblocked_by_account_id uuid;

ALTER TABLE companies
    DROP CONSTRAINT IF EXISTS fk_companies_verification_reviewed_by_account,
    ADD CONSTRAINT fk_companies_verification_reviewed_by_account
        FOREIGN KEY (verification_reviewed_by_account_id) REFERENCES accounts(id),
    DROP CONSTRAINT IF EXISTS fk_companies_blocked_by_account,
    ADD CONSTRAINT fk_companies_blocked_by_account
        FOREIGN KEY (blocked_by_account_id) REFERENCES accounts(id),
    DROP CONSTRAINT IF EXISTS fk_companies_unblocked_by_account,
    ADD CONSTRAINT fk_companies_unblocked_by_account
        FOREIGN KEY (unblocked_by_account_id) REFERENCES accounts(id);

CREATE TABLE IF NOT EXISTS company_verification_requests (
    id uuid PRIMARY KEY,
    created_date timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_modified_date timestamp,
    created_by varchar(255),
    last_modified_by varchar(255),
    status varchar(20) NOT NULL DEFAULT 'ACTIVE',
    company_id uuid NOT NULL REFERENCES companies(id),
    verification_status varchar(50) NOT NULL,
    tax_code varchar(50),
    company_name varchar(255),
    legal_representative_name varchar(255),
    business_email varchar(255),
    website varchar(500),
    submitted_by_account_id uuid REFERENCES accounts(id),
    submitted_at timestamp,
    reviewed_by_account_id uuid REFERENCES accounts(id),
    reviewed_at timestamp,
    admin_note text
);

CREATE INDEX IF NOT EXISTS idx_company_verification_requests_company_id
    ON company_verification_requests(company_id);

CREATE INDEX IF NOT EXISTS idx_company_verification_requests_status
    ON company_verification_requests(verification_status);

CREATE TABLE IF NOT EXISTS company_verification_documents (
    id uuid PRIMARY KEY,
    created_date timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_modified_date timestamp,
    created_by varchar(255),
    last_modified_by varchar(255),
    status varchar(20) NOT NULL DEFAULT 'ACTIVE',
    verification_request_id uuid NOT NULL REFERENCES company_verification_requests(id) ON DELETE CASCADE,
    document_type varchar(100),
    document_url varchar(1000) NOT NULL,
    original_file_name varchar(255),
    mime_type varchar(150)
);

CREATE INDEX IF NOT EXISTS idx_company_verification_documents_request_id
    ON company_verification_documents(verification_request_id);

ALTER TABLE notifications
    DROP CONSTRAINT IF EXISTS notifications_type_check;

ALTER TABLE notifications
    ADD CONSTRAINT notifications_type_check CHECK (
        type IN (
            'NEW_MESSAGE',
            'APPLICATION_STATUS_CHANGED',
            'NEW_APPLICATION',
            'APPLICATION_VIEWED',
            'APPLICATION_SHORTLISTED',
            'APPLICATION_REJECTED',
            'APPLICATION_INTERVIEW_SCHEDULED',
            'INTERVIEW_CONFIRMED',
            'INTERVIEW_DECLINED',
            'INTERVIEW_CANCELLED',
            'INTERVIEW_RESCHEDULE_PROPOSED',
            'INTERVIEW_RESCHEDULE_ACCEPTED',
            'INTERVIEW_RESCHEDULE_REJECTED',
            'APPLICATION_AI_SCREENING',
            'COMPANY_VERIFICATION_APPROVED',
            'COMPANY_VERIFICATION_REJECTED',
            'COMPANY_VERIFICATION_NEEDS_INFO',
            'COMPANY_BLOCKED',
            'COMPANY_UNBLOCKED'
        )
    );

ALTER TABLE notification_preferences
    DROP CONSTRAINT IF EXISTS notification_preferences_type_check;

ALTER TABLE notification_preferences
    ADD CONSTRAINT notification_preferences_type_check CHECK (
        type IN (
            'NEW_MESSAGE',
            'APPLICATION_STATUS_CHANGED',
            'NEW_APPLICATION',
            'APPLICATION_VIEWED',
            'APPLICATION_SHORTLISTED',
            'APPLICATION_REJECTED',
            'APPLICATION_INTERVIEW_SCHEDULED',
            'INTERVIEW_CONFIRMED',
            'INTERVIEW_DECLINED',
            'INTERVIEW_CANCELLED',
            'INTERVIEW_RESCHEDULE_PROPOSED',
            'INTERVIEW_RESCHEDULE_ACCEPTED',
            'INTERVIEW_RESCHEDULE_REJECTED',
            'APPLICATION_AI_SCREENING',
            'COMPANY_VERIFICATION_APPROVED',
            'COMPANY_VERIFICATION_REJECTED',
            'COMPANY_VERIFICATION_NEEDS_INFO',
            'COMPANY_BLOCKED',
            'COMPANY_UNBLOCKED'
        )
    );

COMMIT;
