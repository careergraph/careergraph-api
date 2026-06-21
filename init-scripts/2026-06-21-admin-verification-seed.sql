-- ============================================================
-- CareerGraph Admin Verification Seed Data
-- Safe to run multiple times (ON CONFLICT DO NOTHING)
-- Provides sample company verification requests and documents
-- for admin dashboard, verification queue, and company control
-- ============================================================

BEGIN;

-- 1) Create additional companies for verification testing
INSERT INTO parties (
    id,
    party_type,
    tagname,
    avatar,
    cover,
    no_of_followers,
    no_of_following,
    no_of_connections,
    created_date,
    last_modified_date,
    status
)
VALUES
    ('10000000-0000-0000-0000-000000000002', 'Company', 'techcorp-vn', '', '', 0, 0, 0, CURRENT_TIMESTAMP - INTERVAL '30 days', CURRENT_TIMESTAMP - INTERVAL '30 days', 'ACTIVE'),
    ('10000000-0000-0000-0000-000000000003', 'Company', 'innovate-solutions', '', '', 0, 0, 0, CURRENT_TIMESTAMP - INTERVAL '25 days', CURRENT_TIMESTAMP - INTERVAL '25 days', 'ACTIVE'),
    ('10000000-0000-0000-0000-000000000004', 'Company', 'global-tech', '', '', 0, 0, 0, CURRENT_TIMESTAMP - INTERVAL '20 days', CURRENT_TIMESTAMP - INTERVAL '20 days', 'ACTIVE'),
    ('10000000-0000-0000-0000-000000000005', 'Company', 'startup-hub', '', '', 0, 0, 0, CURRENT_TIMESTAMP - INTERVAL '15 days', CURRENT_TIMESTAMP - INTERVAL '15 days', 'ACTIVE')
ON CONFLICT (id) DO NOTHING;

-- 2) Create companies (subclass table)
INSERT INTO companies (
    id,
    size,
    name,
    website,
    ceo_name,
    no_of_members,
    year_founded,
    verification_status,
    operational_status,
    tax_code,
    legal_representative_name,
    verification_business_email,
    verification_website,
    verification_submitted_at
)
VALUES
    ('10000000-0000-0000-0000-000000000002', '201-500', 'TechCorp Vietnam', 'https://techcorp.vn', 'Tran Quoc Huy', 250, 2018, 'PENDING_REVIEW', 'ACTIVE', '0123456789', 'Tran Quoc Huy', 'hr@techcorp.vn', 'https://techcorp.vn', CURRENT_TIMESTAMP - INTERVAL '28 days'),
    ('10000000-0000-0000-0000-000000000003', '51-200', 'Innovate Solutions', 'https://innovate.com.vn', 'Nguyen Thi Huong', 150, 2019, 'PENDING_REVIEW', 'ACTIVE', '0987654321', 'Nguyen Thi Huong', 'contact@innovate.com.vn', 'https://innovate.com.vn', CURRENT_TIMESTAMP - INTERVAL '23 days'),
    ('10000000-0000-0000-0000-000000000004', '1-50', 'Global Tech Solutions', 'https://globaltech.vn', 'Pham Duc Minh', 45, 2020, 'APPROVED', 'ACTIVE', '1111111111', 'Pham Duc Minh', 'team@globaltech.vn', 'https://globaltech.vn', CURRENT_TIMESTAMP - INTERVAL '18 days'),
    ('10000000-0000-0000-0000-000000000005', '51-200', 'Startup Hub Vietnam', 'https://startuphub.vn', 'Le Hoang Nam', 180, 2021, 'REJECTED', 'ACTIVE', '2222222222', 'Le Hoang Nam', 'hr@startuphub.vn', 'https://startuphub.vn', CURRENT_TIMESTAMP - INTERVAL '10 days')
ON CONFLICT (id) DO NOTHING;

-- 3) Create HR accounts for new companies
INSERT INTO accounts (
    id,
    email,
    password_hash,
    role,
    email_verified,
    company_id,
    created_date,
    last_modified_date,
    status
)
VALUES
    ('40000000-0000-0000-0000-000000000002', 'hr@techcorp.vn', '$2a$10$xLmEZQaC3Q8mC4kg4wgzaOSU6ZpXEn1gy6LV/xjLZ0z3yXmqWvKSy', 'HR', true, '10000000-0000-0000-0000-000000000002', CURRENT_TIMESTAMP - INTERVAL '30 days', CURRENT_TIMESTAMP - INTERVAL '30 days', 'ACTIVE'),
    ('40000000-0000-0000-0000-000000000003', 'contact@innovate.com.vn', '$2a$10$xLmEZQaC3Q8mC4kg4wgzaOSU6ZpXEn1gy6LV/xjLZ0z3yXmqWvKSy', 'HR', true, '10000000-0000-0000-0000-000000000003', CURRENT_TIMESTAMP - INTERVAL '25 days', CURRENT_TIMESTAMP - INTERVAL '25 days', 'ACTIVE'),
    ('40000000-0000-0000-0000-000000000004', 'team@globaltech.vn', '$2a$10$xLmEZQaC3Q8mC4kg4wgzaOSU6ZpXEn1gy6LV/xjLZ0z3yXmqWvKSy', 'HR', true, '10000000-0000-0000-0000-000000000004', CURRENT_TIMESTAMP - INTERVAL '20 days', CURRENT_TIMESTAMP - INTERVAL '20 days', 'ACTIVE'),
    ('40000000-0000-0000-0000-000000000005', 'hr@startuphub.vn', '$2a$10$xLmEZQaC3Q8mC4kg4wgzaOSU6ZpXEn1gy6LV/xjLZ0z3yXmqWvKSy', 'HR', true, '10000000-0000-0000-0000-000000000005', CURRENT_TIMESTAMP - INTERVAL '15 days', CURRENT_TIMESTAMP - INTERVAL '15 days', 'ACTIVE')
ON CONFLICT (email) DO NOTHING;

-- 4) Create company verification requests
INSERT INTO company_verification_requests (
    id,
    company_id,
    verification_status,
    tax_code,
    company_name,
    legal_representative_name,
    business_email,
    website,
    submitted_by_account_id,
    submitted_at,
    created_date,
    last_modified_date,
    created_by,
    status
)
VALUES
    -- PENDING_REVIEW: TechCorp Vietnam
    ('f0000000-0000-0000-0000-000000000001', '10000000-0000-0000-0000-000000000002', 'PENDING_REVIEW', '0123456789', 'TechCorp Vietnam', 'Tran Quoc Huy', 'hr@techcorp.vn', 'https://techcorp.vn', '40000000-0000-0000-0000-000000000002', CURRENT_TIMESTAMP - INTERVAL '28 days', CURRENT_TIMESTAMP - INTERVAL '28 days', CURRENT_TIMESTAMP - INTERVAL '28 days', 'system', 'ACTIVE'),
    -- PENDING_REVIEW: Innovate Solutions
    ('f0000000-0000-0000-0000-000000000002', '10000000-0000-0000-0000-000000000003', 'PENDING_REVIEW', '0987654321', 'Innovate Solutions', 'Nguyen Thi Huong', 'contact@innovate.com.vn', 'https://innovate.com.vn', '40000000-0000-0000-0000-000000000003', CURRENT_TIMESTAMP - INTERVAL '23 days', CURRENT_TIMESTAMP - INTERVAL '23 days', CURRENT_TIMESTAMP - INTERVAL '23 days', 'system', 'ACTIVE'),
    -- APPROVED: Global Tech Solutions
    ('f0000000-0000-0000-0000-000000000003', '10000000-0000-0000-0000-000000000004', 'APPROVED', '1111111111', 'Global Tech Solutions', 'Pham Duc Minh', 'team@globaltech.vn', 'https://globaltech.vn', '40000000-0000-0000-0000-000000000004', CURRENT_TIMESTAMP - INTERVAL '18 days', CURRENT_TIMESTAMP - INTERVAL '18 days', CURRENT_TIMESTAMP - INTERVAL '18 days', 'system', 'ACTIVE'),
    -- REJECTED: Startup Hub Vietnam (with rejection reason)
    ('f0000000-0000-0000-0000-000000000004', '10000000-0000-0000-0000-000000000005', 'REJECTED', '2222222222', 'Startup Hub Vietnam', 'Le Hoang Nam', 'hr@startuphub.vn', 'https://startuphub.vn', '40000000-0000-0000-0000-000000000005', CURRENT_TIMESTAMP - INTERVAL '10 days', CURRENT_TIMESTAMP - INTERVAL '10 days', CURRENT_TIMESTAMP - INTERVAL '10 days', 'system', 'ACTIVE'),
    -- NEEDS_ADDITIONAL_INFO: Updated request from Innovate Solutions (to show multiple requests per company)
    ('f0000000-0000-0000-0000-000000000005', '10000000-0000-0000-0000-000000000003', 'NEEDS_ADDITIONAL_INFO', '0987654321', 'Innovate Solutions', 'Nguyen Thi Huong', 'contact@innovate.com.vn', 'https://innovate.com.vn', '40000000-0000-0000-0000-000000000003', CURRENT_TIMESTAMP - INTERVAL '5 days', CURRENT_TIMESTAMP - INTERVAL '5 days', CURRENT_TIMESTAMP - INTERVAL '5 days', 'system', 'ACTIVE')
ON CONFLICT (id) DO NOTHING;

-- 5) Update admin review info for approved and rejected requests
UPDATE company_verification_requests
SET reviewed_by_account_id = 'd0000001-0000-0000-0000-000000000001',
    reviewed_at = CURRENT_TIMESTAMP - INTERVAL '15 days',
    admin_note = 'Verified all documents. Tax code and representative information confirmed. Company meets all requirements.'
WHERE id = 'f0000000-0000-0000-0000-000000000003' AND verification_status = 'APPROVED';

UPDATE company_verification_requests
SET reviewed_by_account_id = 'd0000001-0000-0000-0000-000000000001',
    reviewed_at = CURRENT_TIMESTAMP - INTERVAL '8 days',
    admin_note = 'Invalid tax code format. Please provide corrected documentation.'
WHERE id = 'f0000000-0000-0000-0000-000000000004' AND verification_status = 'REJECTED';

UPDATE company_verification_requests
SET reviewed_by_account_id = 'd0000001-0000-0000-0000-000000000001',
    reviewed_at = CURRENT_TIMESTAMP - INTERVAL '3 days',
    admin_note = 'Need additional company registration documents and CEO identification'
WHERE id = 'f0000000-0000-0000-0000-000000000005' AND verification_status = 'NEEDS_ADDITIONAL_INFO';

-- 6) Create verification documents for requests
INSERT INTO company_verification_documents (
    id,
    verification_request_id,
    document_type,
    document_url,
    original_file_name,
    mime_type,
    created_date,
    last_modified_date,
    status
)
VALUES
    -- TechCorp Vietnam documents
    ('d0100000-0000-0000-0000-000000000001', 'f0000000-0000-0000-0000-000000000001', 'BUSINESS_LICENSE', '/documents/techcorp-business-license.pdf', 'techcorp-business-license.pdf', 'application/pdf', CURRENT_TIMESTAMP - INTERVAL '28 days', CURRENT_TIMESTAMP - INTERVAL '28 days', 'ACTIVE'),
    ('d0100000-0000-0000-0000-000000000002', 'f0000000-0000-0000-0000-000000000001', 'TAX_CERTIFICATE', '/documents/techcorp-tax-cert.pdf', 'techcorp-tax-cert.pdf', 'application/pdf', CURRENT_TIMESTAMP - INTERVAL '28 days', CURRENT_TIMESTAMP - INTERVAL '28 days', 'ACTIVE'),
    ('d0100000-0000-0000-0000-000000000003', 'f0000000-0000-0000-0000-000000000001', 'CEO_ID', '/documents/techcorp-ceo-id.pdf', 'techcorp-ceo-id.pdf', 'application/pdf', CURRENT_TIMESTAMP - INTERVAL '28 days', CURRENT_TIMESTAMP - INTERVAL '28 days', 'ACTIVE'),
    -- Innovate Solutions documents
    ('d0100000-0000-0000-0000-000000000011', 'f0000000-0000-0000-0000-000000000002', 'BUSINESS_LICENSE', '/documents/innovate-business-license.pdf', 'innovate-business-license.pdf', 'application/pdf', CURRENT_TIMESTAMP - INTERVAL '23 days', CURRENT_TIMESTAMP - INTERVAL '23 days', 'ACTIVE'),
    ('d0100000-0000-0000-0000-000000000012', 'f0000000-0000-0000-0000-000000000002', 'TAX_CERTIFICATE', '/documents/innovate-tax-cert.pdf', 'innovate-tax-cert.pdf', 'application/pdf', CURRENT_TIMESTAMP - INTERVAL '23 days', CURRENT_TIMESTAMP - INTERVAL '23 days', 'ACTIVE'),
    -- Global Tech Solutions documents
    ('d0100000-0000-0000-0000-000000000021', 'f0000000-0000-0000-0000-000000000003', 'BUSINESS_LICENSE', '/documents/globaltech-business-license.pdf', 'globaltech-business-license.pdf', 'application/pdf', CURRENT_TIMESTAMP - INTERVAL '18 days', CURRENT_TIMESTAMP - INTERVAL '18 days', 'ACTIVE'),
    ('d0100000-0000-0000-0000-000000000022', 'f0000000-0000-0000-0000-000000000003', 'TAX_CERTIFICATE', '/documents/globaltech-tax-cert.pdf', 'globaltech-tax-cert.pdf', 'application/pdf', CURRENT_TIMESTAMP - INTERVAL '18 days', CURRENT_TIMESTAMP - INTERVAL '18 days', 'ACTIVE'),
    ('d0100000-0000-0000-0000-000000000023', 'f0000000-0000-0000-0000-000000000003', 'CEO_ID', '/documents/globaltech-ceo-id.pdf', 'globaltech-ceo-id.pdf', 'application/pdf', CURRENT_TIMESTAMP - INTERVAL '18 days', CURRENT_TIMESTAMP - INTERVAL '18 days', 'ACTIVE'),
    -- Startup Hub Vietnam documents
    ('d0100000-0000-0000-0000-000000000031', 'f0000000-0000-0000-0000-000000000004', 'BUSINESS_LICENSE', '/documents/startuphub-business-license.pdf', 'startuphub-business-license.pdf', 'application/pdf', CURRENT_TIMESTAMP - INTERVAL '10 days', CURRENT_TIMESTAMP - INTERVAL '10 days', 'ACTIVE'),
    ('d0100000-0000-0000-0000-000000000032', 'f0000000-0000-0000-0000-000000000004', 'TAX_CERTIFICATE', '/documents/startuphub-tax-cert.pdf', 'startuphub-tax-cert.pdf', 'application/pdf', CURRENT_TIMESTAMP - INTERVAL '10 days', CURRENT_TIMESTAMP - INTERVAL '10 days', 'ACTIVE'),
    -- Innovate Solutions resubmission documents
    ('d0100000-0000-0000-0000-000000000041', 'f0000000-0000-0000-0000-000000000005', 'BUSINESS_LICENSE', '/documents/innovate-v2-business-license.pdf', 'innovate-v2-business-license.pdf', 'application/pdf', CURRENT_TIMESTAMP - INTERVAL '5 days', CURRENT_TIMESTAMP - INTERVAL '5 days', 'ACTIVE'),
    ('d0100000-0000-0000-0000-000000000042', 'f0000000-0000-0000-0000-000000000005', 'TAX_CERTIFICATE', '/documents/innovate-v2-tax-cert.pdf', 'innovate-v2-tax-cert.pdf', 'application/pdf', CURRENT_TIMESTAMP - INTERVAL '5 days', CURRENT_TIMESTAMP - INTERVAL '5 days', 'ACTIVE'),
    ('d0100000-0000-0000-0000-000000000043', 'f0000000-0000-0000-0000-000000000005', 'CEO_ID', '/documents/innovate-v2-ceo-id.pdf', 'innovate-v2-ceo-id.pdf', 'application/pdf', CURRENT_TIMESTAMP - INTERVAL '5 days', CURRENT_TIMESTAMP - INTERVAL '5 days', 'ACTIVE')
ON CONFLICT (id) DO NOTHING;

-- 7) Update GlobalTech company to mark as verified in companies table
UPDATE companies
SET verification_status = 'APPROVED',
    verification_reviewed_at = CURRENT_TIMESTAMP - INTERVAL '15 days',
    verification_reviewed_by_account_id = 'd0000001-0000-0000-0000-000000000001',
    verification_admin_note = 'Verified all documents. Company is now approved for job posting.'
WHERE id = '10000000-0000-0000-0000-000000000004';

-- 8) Update rejected company
UPDATE companies
SET verification_status = 'REJECTED',
    verification_reviewed_at = CURRENT_TIMESTAMP - INTERVAL '8 days',
    verification_reviewed_by_account_id = 'd0000001-0000-0000-0000-000000000001',
    verification_admin_note = 'Invalid tax code format.',
    block_reason = 'Verification failed: Invalid tax code'
WHERE id = '10000000-0000-0000-0000-000000000005';

COMMIT;
