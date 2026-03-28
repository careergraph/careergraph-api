-- ============================================================
-- CareerGraph Basic Seed Data
-- Safe to run multiple times (ON CONFLICT DO NOTHING)
-- NOTE: Company/Candidate/Education use JOINED inheritance.
--       Base fields (created_date/status/...) are stored in parties.
-- ============================================================

-- 1) Parties
INSERT INTO parties (
    id, party_type, tagname, avatar, cover,
    no_of_followers, no_of_following, no_of_connections,
    created_date, last_modified_date, status
)
VALUES
  ('10000000-0000-0000-0000-000000000001', 'Company', 'careergraph', '', '', 0, 0, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'ACTIVE'),
  ('20000000-0000-0000-0000-000000000001', 'Candidate', 'nguyen-van-a', '', '', 0, 0, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'ACTIVE'),
  ('20000000-0000-0000-0000-000000000002', 'Candidate', 'tran-thi-b', '', '', 0, 0, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'ACTIVE'),
  ('20000000-0000-0000-0000-000000000003', 'Candidate', 'le-van-c', '', '', 0, 0, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'ACTIVE'),
  ('30000000-0000-0000-0000-000000000001', 'Education', 'hcmut', '', '', 0, 0, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'ACTIVE')
ON CONFLICT (id) DO NOTHING;

-- 2) Companies (subclass table: only subclass columns + id)
INSERT INTO companies (id, size, name, website, ceo_name, no_of_members, year_founded)
VALUES
  ('10000000-0000-0000-0000-000000000001', '51-200', 'CareerGraph', 'https://careergraph.vn', 'Nguyen Cong Quy', 120, 2020)
ON CONFLICT (id) DO NOTHING;

-- 3) Candidates (subclass table: only subclass columns + id)
INSERT INTO candidates (
    id, first_name, last_name, date_of_birth, gender, current_job_title,
    desired_position, current_company, industry, years_of_experience,
    work_location, is_open_to_work, is_open_to_notify_new_job,
    summary, is_married, salary_expectation_min, salary_expectation_max,
    education_level, current_position
)
VALUES
  (
    '20000000-0000-0000-0000-000000000001', 'Nguyen', 'Van A', '1998-04-10', 'MALE',
    'Backend Developer', 'Backend Engineer', 'Startup One', 'Information Technology', 2,
    'Ho Chi Minh', true, true,
    'Backend-focused engineer with Spring Boot and PostgreSQL experience.',
    false, 800, 1500, 'BACHELOR', 'Backend Developer'
  ),
  (
    '20000000-0000-0000-0000-000000000002', 'Tran', 'Thi B', '1999-09-22', 'FEMALE',
    'Frontend Developer', 'Frontend Engineer', 'Product Lab', 'Information Technology', 1,
    'Ha Noi', true, true,
    'Frontend engineer with React and TypeScript foundation.',
    false, 700, 1300, 'BACHELOR', 'Frontend Developer'
  ),
  (
    '20000000-0000-0000-0000-000000000003', 'Le', 'Van C', '1996-12-01', 'MALE',
    'DevOps Engineer', 'DevOps Engineer', 'Cloud Team', 'Information Technology', 4,
    'Da Nang', true, false,
    'DevOps engineer with CI/CD and Kubernetes operations experience.',
    true, 1200, 2200, 'BACHELOR', 'DevOps Engineer'
  )
ON CONFLICT (id) DO NOTHING;

-- 4) Education organizations (subclass table: only subclass columns + id)
INSERT INTO educations (
    id, official_name, short_name, established_year,
    university_type, level, website, overview, accreditation
)
VALUES
  (
    '30000000-0000-0000-0000-000000000001',
    'Ho Chi Minh City University of Technology',
    'HCMUT',
    1957,
    'PUBLIC',
    'UNIVERSITY',
    'https://hcmut.edu.vn',
    'Leading engineering university in Ho Chi Minh City.',
    'ABET'
  )
ON CONFLICT (id) DO NOTHING;

-- 5) Accounts
INSERT INTO accounts (id, email, password_hash, role, email_verified, company_id, created_date, last_modified_date, status)
VALUES
  ('40000000-0000-0000-0000-000000000001', 'hr@careergraph.vn', '$2a$10$xLmEZQaC3Q8mC4kg4wgzaOSU6ZpXEn1gy6LV/xjLZ0z3yXmqWvKSy', 'HR', true, '10000000-0000-0000-0000-000000000001', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'ACTIVE')
ON CONFLICT (id) DO NOTHING;

INSERT INTO accounts (id, email, password_hash, role, email_verified, candidate_id, created_date, last_modified_date, status)
VALUES
  ('40000000-0000-0000-0000-000000000011', 'candidate1@careergraph.vn', '$2a$10$xLmEZQaC3Q8mC4kg4wgzaOSU6ZpXEn1gy6LV/xjLZ0z3yXmqWvKSy', 'USER', true, '20000000-0000-0000-0000-000000000001', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'ACTIVE'),
  ('40000000-0000-0000-0000-000000000012', 'candidate2@careergraph.vn', '$2a$10$xLmEZQaC3Q8mC4kg4wgzaOSU6ZpXEn1gy6LV/xjLZ0z3yXmqWvKSy', 'USER', true, '20000000-0000-0000-0000-000000000002', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'ACTIVE'),
  ('40000000-0000-0000-0000-000000000013', 'candidate3@careergraph.vn', '$2a$10$xLmEZQaC3Q8mC4kg4wgzaOSU6ZpXEn1gy6LV/xjLZ0z3yXmqWvKSy', 'USER', true, '20000000-0000-0000-0000-000000000003', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'ACTIVE')
ON CONFLICT (id) DO NOTHING;

-- 6) Contacts
INSERT INTO contacts (id, value, verified, is_primary, contact_type, party_id, created_date, last_modified_date, status)
VALUES
  ('50000000-0000-0000-0000-000000000001', 'support@careergraph.vn', true, true, 'EMAIL', '10000000-0000-0000-0000-000000000001', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'ACTIVE'),
  ('50000000-0000-0000-0000-000000000002', '+84-28-1234-5678', true, true, 'PHONE', '10000000-0000-0000-0000-000000000001', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'ACTIVE'),
  ('50000000-0000-0000-0000-000000000011', '0900000001', true, true, 'PHONE', '20000000-0000-0000-0000-000000000001', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'ACTIVE'),
  ('50000000-0000-0000-0000-000000000012', '0900000002', true, true, 'PHONE', '20000000-0000-0000-0000-000000000002', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'ACTIVE'),
  ('50000000-0000-0000-0000-000000000013', '0900000003', true, true, 'PHONE', '20000000-0000-0000-0000-000000000003', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'ACTIVE')
ON CONFLICT (id) DO NOTHING;

-- 7) Addresses
INSERT INTO addresses (id, name, country, province, district, ward, is_primary, address_type, party_id, created_date, last_modified_date, status)
VALUES
  ('60000000-0000-0000-0000-000000000001', 'CareerGraph Head Office', 'Viet Nam', 'Ho Chi Minh', 'District 1', 'Ward 1', true, 'HEADQUARTERS', '10000000-0000-0000-0000-000000000001', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'ACTIVE'),
  ('60000000-0000-0000-0000-000000000011', 'Candidate 1 Home', 'Viet Nam', 'Ho Chi Minh', 'District 7', NULL, true, 'HOME_ADDRESS', '20000000-0000-0000-0000-000000000001', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'ACTIVE'),
  ('60000000-0000-0000-0000-000000000012', 'Candidate 2 Home', 'Viet Nam', 'Ha Noi', 'Cau Giay', NULL, true, 'HOME_ADDRESS', '20000000-0000-0000-0000-000000000002', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'ACTIVE'),
  ('60000000-0000-0000-0000-000000000013', 'Candidate 3 Home', 'Viet Nam', 'Da Nang', 'Hai Chau', NULL, true, 'HOME_ADDRESS', '20000000-0000-0000-0000-000000000003', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'ACTIVE'),
  ('60000000-0000-0000-0000-000000000021', 'HCMUT Campus', 'Viet Nam', 'Ho Chi Minh', 'District 10', 'Ward 14', true, 'HEADQUARTERS', '30000000-0000-0000-0000-000000000001', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'ACTIVE')
ON CONFLICT (id) DO NOTHING;

-- 8) Skills
INSERT INTO skills (id, name, category, description, created_date, last_modified_date, status)
VALUES
  ('70000000-0000-0000-0000-000000000001', 'Java', 'Programming', 'Java programming language', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'ACTIVE'),
  ('70000000-0000-0000-0000-000000000002', 'Spring Boot', 'Framework', 'Spring Boot framework', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'ACTIVE'),
  ('70000000-0000-0000-0000-000000000003', 'React', 'Frontend', 'React library', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'ACTIVE'),
  ('70000000-0000-0000-0000-000000000004', 'PostgreSQL', 'Database', 'PostgreSQL database', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'ACTIVE'),
  ('70000000-0000-0000-0000-000000000005', 'Docker', 'DevOps', 'Containerization with Docker', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'ACTIVE'),
  ('70000000-0000-0000-0000-000000000006', 'Kubernetes', 'DevOps', 'Kubernetes orchestration', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'ACTIVE')
ON CONFLICT (id) DO NOTHING;

-- 9) Candidate skills
INSERT INTO candidate_skill (id, proficiency_level, years_of_experience, is_verified, endorsed_by, endorsement_date, endorsement_count, candidate_id, skill_id, created_date, last_modified_date, status)
VALUES
  ('71000000-0000-0000-0000-000000000001', 'INTERMEDIATE', 2, true, 'mentor1', NULL, 2, '20000000-0000-0000-0000-000000000001', '70000000-0000-0000-0000-000000000001', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'ACTIVE'),
  ('71000000-0000-0000-0000-000000000002', 'INTERMEDIATE', 2, true, 'mentor2', NULL, 1, '20000000-0000-0000-0000-000000000001', '70000000-0000-0000-0000-000000000002', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'ACTIVE'),
  ('71000000-0000-0000-0000-000000000003', 'BEGINNER', 1, false, NULL, NULL, 0, '20000000-0000-0000-0000-000000000002', '70000000-0000-0000-0000-000000000003', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'ACTIVE'),
  ('71000000-0000-0000-0000-000000000004', 'INTERMEDIATE', 3, true, 'lead-devops', NULL, 3, '20000000-0000-0000-0000-000000000003', '70000000-0000-0000-0000-000000000005', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'ACTIVE')
ON CONFLICT (id) DO NOTHING;

-- 10) Candidate education
INSERT INTO candidate_education (id, start_date, end_date, degree_title, major, is_current, description, candidate_id, education_id, created_date, last_modified_date, status)
VALUES
  ('80000000-0000-0000-0000-000000000001', '2016-09-01', '2020-06-30', 'Bachelor of Computer Science', 'Computer Science', false, 'Graduated with good academic standing.', '20000000-0000-0000-0000-000000000001', '30000000-0000-0000-0000-000000000001', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'ACTIVE')
ON CONFLICT (id) DO NOTHING;

-- 11) Candidate experience
INSERT INTO candidate_experience (id, start_date, end_date, salary, job_title, is_current, description, candidate_id, company_id, created_date, last_modified_date, status)
VALUES
  ('81000000-0000-0000-0000-000000000001', '2022-01-01', '2024-01-31', 1200, 'Backend Developer', false, 'Built and maintained backend APIs using Spring Boot.', '20000000-0000-0000-0000-000000000001', '10000000-0000-0000-0000-000000000001', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'ACTIVE')
ON CONFLICT (id) DO NOTHING;

-- 12) Jobs
INSERT INTO jobs (
    id, title, description, department, responsibilities, qualifications,
    minimum_qualifications, benefits, salary_range, min_experience, max_experience,
    experience_level, employment_type, job_category, education,
    posted_date, expiry_date, number_of_positions, contact_email, contact_phone,
    promotion_type, state, city, district, address, remote_job,
    views, applicants, saved, liked, shared, resume, cover_letter,
    created_date, last_modified_date, status, company_id
)
VALUES
  (
    '90000000-0000-0000-0000-000000000001',
    'Backend Engineer (Spring Boot)',
    'Develop APIs and backend services for recruitment workflows.',
    'Engineering',
    '["Design and implement robust REST APIs.","Optimize SQL queries and data models.","Collaborate with frontend and QA teams."]',
    '["1+ years Java and Spring Boot.","Experience with PostgreSQL.","Good communication skills."]',
    '["Bachelor degree in IT or related field.","Basic understanding of microservices."]',
    '["Health insurance","Laptop support","Performance bonus"]',
    '1000-1800',
    1,
    4,
    'MIDDLE',
    'FULL_TIME',
    'ENGINEER',
    'BACHELORS_DEGREE',
    '2026-03-01',
    '2026-06-01',
    2,
    'hr@careergraph.vn',
    '+84-28-1234-5678',
    'free',
    'HCM',
    'District 1',
    'Ward 1',
    '123 Le Loi',
    false,
    0,
    0,
    0,
    0,
    0,
    true,
    true,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP,
    'ACTIVE',
    '10000000-0000-0000-0000-000000000001'
  ),
  (
    '90000000-0000-0000-0000-000000000002',
    'Frontend Engineer (React)',
    'Build performant and accessible interfaces for job seeker experiences.',
    'Engineering',
    '["Build reusable React components.","Integrate APIs and handle app state.","Improve UX and performance metrics."]',
    '["1+ years React experience.","Solid JavaScript/TypeScript.","Basic testing knowledge."]',
    '["Bachelor degree preferred.","Able to collaborate in agile team."]',
    '["Flexible work hours","Learning budget","Team building"]',
    '800-1500',
    1,
    3,
    'JUNIOR',
    'FULL_TIME',
    'ENGINEER',
    'BACHELORS_DEGREE',
    '2026-03-02',
    '2026-06-10',
    1,
    'hr@careergraph.vn',
    '+84-28-1234-5678',
    'free',
    'HN',
    'Cau Giay',
    'Dich Vong',
    '80 Duy Tan',
    true,
    0,
    0,
    0,
    0,
    0,
    true,
    true,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP,
    'ACTIVE',
    '10000000-0000-0000-0000-000000000001'
  ),
  (
    '90000000-0000-0000-0000-000000000003',
    'DevOps Engineer',
    'Maintain CI/CD pipelines and cloud infrastructure for platform reliability.',
    'Infrastructure',
    '["Maintain CI/CD and deployment automation.","Manage Kubernetes and observability stack.","Improve reliability and incident response."]',
    '["2+ years DevOps experience.","Strong Docker/Kubernetes knowledge.","Scripting skills (Bash/Python)."]',
    '["Hands-on Linux operations.","Experience with monitoring tools."]',
    '["On-call allowance","Hybrid work","Annual health check"]',
    '1200-2200',
    2,
    6,
    'SENIOR',
    'FULL_TIME',
    'ENGINEER',
    'BACHELORS_DEGREE',
    '2026-03-03',
    '2026-07-01',
    1,
    'hr@careergraph.vn',
    '+84-28-1234-5678',
    'paid',
    'DN',
    'Hai Chau',
    'Hai Chau 1',
    '12 Bach Dang',
    true,
    0,
    0,
    0,
    0,
    0,
    true,
    true,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP,
    'ACTIVE',
    '10000000-0000-0000-0000-000000000001'
  )
ON CONFLICT (id) DO NOTHING;

-- 13) Applications
INSERT INTO applications (
    id, cover_letter, resume_url, rating, notes, applied_date,
    current_stage, stage_changed_at, current_stage_note,
    candidate_id, job_id, created_date, last_modified_date, status
)
VALUES
  ('91000000-0000-0000-0000-000000000001', 'I am interested in backend engineering and excited to contribute.', '/resumes/candidate1.pdf', NULL, 'Initial application', '2026-03-04', 'APPLIED', CURRENT_TIMESTAMP, 'Awaiting screening', '20000000-0000-0000-0000-000000000001', '90000000-0000-0000-0000-000000000001', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'ACTIVE'),
  ('91000000-0000-0000-0000-000000000002', 'Strong frontend profile with React focus.', '/resumes/candidate2.pdf', NULL, 'Initial application', '2026-03-05', 'SCREENING', CURRENT_TIMESTAMP, 'Profile under review', '20000000-0000-0000-0000-000000000002', '90000000-0000-0000-0000-000000000002', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'ACTIVE'),
  ('91000000-0000-0000-0000-000000000003', 'Interested in operating production-grade systems.', '/resumes/candidate3.pdf', NULL, 'Initial application', '2026-03-06', 'INTERVIEW', CURRENT_TIMESTAMP, 'Interview scheduled', '20000000-0000-0000-0000-000000000003', '90000000-0000-0000-0000-000000000003', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'ACTIVE')
ON CONFLICT (id) DO NOTHING;

-- 14) Application stage history
INSERT INTO application_stage_history (
    id, application_id, from_stage, to_stage, note, changed_by,
    changed_at, created_date, last_modified_date, status
)
VALUES
  ('92000000-0000-0000-0000-000000000001', '91000000-0000-0000-0000-000000000001', NULL, 'APPLIED', 'Candidate submitted application', 'candidate1@careergraph.vn', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'ACTIVE'),
  ('92000000-0000-0000-0000-000000000002', '91000000-0000-0000-0000-000000000002', 'APPLIED', 'SCREENING', 'Passed basic profile screening', 'hr@careergraph.vn', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'ACTIVE'),
  ('92000000-0000-0000-0000-000000000003', '91000000-0000-0000-0000-000000000003', 'SCREENING', 'INTERVIEW', 'Shortlisted for technical interview', 'hr@careergraph.vn', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'ACTIVE')
ON CONFLICT (id) DO NOTHING;

-- 15) Candidate-to-company connection
INSERT INTO connections (
    id, note, connection_type, has_seen, disable_notification,
    candidate_id, connected_company_id, created_date, last_modified_date, status
)
VALUES
  ('93000000-0000-0000-0000-000000000001', 'Following CareerGraph for new opportunities', 'FOLLOWED', true, false, '20000000-0000-0000-0000-000000000001', '10000000-0000-0000-0000-000000000001', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'ACTIVE')
ON CONFLICT (id) DO NOTHING;