-- Mock data for CareerGraph application
-- NOTE: This script inserts sample rows for development/testing.
-- Review and adapt timestamps / UUID generation for your DB engine if necessary.

-- UUIDs chosen deterministically for reproducibility.

-- 1) Parties (companies, candidates, educations use JOINED inheritance)
INSERT INTO parties (id, party_type, tagname, avatar, cover, no_of_followers, no_of_following, no_of_connections, status)
VALUES
  ('00000000-0000-0000-0000-000000000100','COMPANY','careergraph','', '', 0,0,0,'ACTIVE'),
  ('00000000-0000-0000-0000-000000000200','CANDIDATE','candidate1','', '', 0,0,0,'ACTIVE'),
  ('00000000-0000-0000-0000-000000000201','CANDIDATE','candidate2','', '', 0,0,0,'ACTIVE'),
  ('00000000-0000-0000-0000-000000000202','CANDIDATE','candidate3','', '', 0,0,0,'ACTIVE'),
  ('00000000-0000-0000-0000-000000000203','CANDIDATE','candidate4','', '', 0,0,0,'ACTIVE'),
  ('00000000-0000-0000-0000-000000000204','CANDIDATE','candidate5','', '', 0,0,0,'ACTIVE'),
  ('00000000-0000-0000-0000-000000000205','CANDIDATE','candidate6','', '', 0,0,0,'ACTIVE'),
  ('00000000-0000-0000-0000-000000000206','CANDIDATE','candidate7','', '', 0,0,0,'ACTIVE'),
  ('00000000-0000-0000-0000-000000000207','CANDIDATE','candidate8','', '', 0,0,0,'ACTIVE'),
  ('00000000-0000-0000-0000-000000000208','CANDIDATE','candidate9','', '', 0,0,0,'ACTIVE'),
  ('00000000-0000-0000-0000-000000000209','CANDIDATE','candidate10','', '', 0,0,0,'ACTIVE');

-- 2) Companies (inherit from parties) - CareerGraph employer
INSERT INTO companies (id, size, name, website, ceo_name, no_of_members, year_founded, status)
VALUES
  ('00000000-0000-0000-0000-000000000100','51-200','CareerGraph','https://careergraph.com','Nguyen Cong Quy',120,2020,'ACTIVE');

-- 3) Candidates (inherit from parties)
INSERT INTO candidates (id, first_name, last_name, date_of_birth, gender, current_job_title, desired_position, current_company, industry, years_of_experience, work_location, is_open_to_work, summary, is_married, salary_expectation_min, salary_expectation_max, education_level, current_position, status)
VALUES
  ('00000000-0000-0000-0000-000000000200','Nguyen','Van','1995-04-12','Male','Junior Developer','Backend Engineer','Startup X','Information Technology',2,'Ho Chi Minh',true,'Software engineer with 2 years experience in Java/Spring',false,500,1200,'Bachelor','Junior Developer','ACTIVE'),
  ('00000000-0000-0000-0000-000000000201','Tran','Thi','1992-07-20','Female','Frontend Developer','Frontend Engineer','Company Y','Information Technology',4,'Ha Noi',true,'Skilled in React and TypeScript',false,800,1500,'Bachelor','Frontend Developer','ACTIVE'),
  ('00000000-0000-0000-0000-000000000202','Le','Van','1990-01-15','Male','DevOps Engineer','DevOps Engineer','Company Z','Information Technology',5,'Da Nang',true,'Experienced in AWS, Docker, Kubernetes',false,1000,2000,'Bachelor','DevOps Engineer','ACTIVE'),
  ('00000000-0000-0000-0000-000000000203','Pham','Thi','1997-11-30','Female','Data Analyst','Data Scientist','Startup A','Information Technology',3,'Ho Chi Minh',true,'Data analyst with SQL and Python skills',false,700,1400,'Master','Data Analyst','ACTIVE'),
  ('00000000-0000-0000-0000-000000000204','Hoang','Van','1993-05-05','Male','Backend Developer','Senior Backend Engineer','Company B','Information Technology',6,'Ho Chi Minh',false,'Backend engineer specializing in microservices',true,1500,3000,'Bachelor','Senior Backend Engineer','ACTIVE'),
  ('00000000-0000-0000-0000-000000000205','Vu','Thi','1998-09-09','Female','QA Engineer','QA Engineer','Company C','Information Technology',2,'Da Nang',true,'Quality assurance and automation enthusiast',false,500,1000,'Bachelor','QA Engineer','ACTIVE'),
  ('00000000-0000-0000-0000-000000000206','Nguyen','Thi','1996-02-14','Female','Fullstack Developer','Fullstack Engineer','Freelancer','Information Technology',3,'Ha Noi',true,'Fullstack developer with React + Spring Boot',false,900,1600,'Bachelor','Fullstack Developer','ACTIVE'),
  ('00000000-0000-0000-0000-000000000207','Do','Van','1988-12-01','Male','Senior Data Scientist','Lead Data Scientist','Company D','Information Technology',8,'Ho Chi Minh',false,'Expert in ML, Python and deployment',true,2000,4000,'PhD','Data Scientist','ACTIVE'),
  ('00000000-0000-0000-0000-000000000208','Bui','Thi','1994-03-21','Female','Mobile Developer','Mobile Engineer','Company E','Information Technology',4,'Ha Noi',true,'Android and iOS cross-platform experience',false,1000,1800,'Bachelor','Mobile Developer','ACTIVE'),
  ('00000000-0000-0000-0000-000000000209','Mac','Van','1991-08-08','Male','Security Engineer','Security Engineer','Company F','Information Technology',5,'Ho Chi Minh',false,'Security and pentesting background',true,1200,2500,'Bachelor','Security Engineer','ACTIVE');

-- 4) Accounts: HR account + candidate accounts
-- HR account (linked to CareerGraph company)
INSERT INTO accounts (id, email, password_hash, role, email_verified, company_id, status)
VALUES ('00000000-0000-0000-0000-000000000001','congquynguyen296.dev@gmail.com','12345678','HR',true,'00000000-0000-0000-0000-000000000100','ACTIVE');

-- Candidate accounts (use +n emails)
INSERT INTO accounts (id, email, password_hash, role, email_verified, candidate_id, status)
VALUES
 ('00000000-0000-0000-0000-000000000210','congquynguyen296.dev+1@gmail.com','12345678','USER',true,'00000000-0000-0000-0000-000000000200','ACTIVE'),
 ('00000000-0000-0000-0000-000000000211','congquynguyen296.dev+2@gmail.com','12345678','USER',true,'00000000-0000-0000-0000-000000000201','ACTIVE'),
 ('00000000-0000-0000-0000-000000000212','congquynguyen296.dev+3@gmail.com','12345678','USER',true,'00000000-0000-0000-0000-000000000202','ACTIVE'),
 ('00000000-0000-0000-0000-000000000213','congquynguyen296.dev+4@gmail.com','12345678','USER',true,'00000000-0000-0000-0000-000000000203','ACTIVE'),
 ('00000000-0000-0000-0000-000000000214','congquynguyen296.dev+5@gmail.com','12345678','USER',true,'00000000-0000-0000-0000-000000000204','ACTIVE'),
 ('00000000-0000-0000-0000-000000000215','congquynguyen296.dev+6@gmail.com','12345678','USER',true,'00000000-0000-0000-0000-000000000205','ACTIVE'),
 ('00000000-0000-0000-0000-000000000216','congquynguyen296.dev+7@gmail.com','12345678','USER',true,'00000000-0000-0000-0000-000000000206','ACTIVE'),
 ('00000000-0000-0000-0000-000000000217','congquynguyen296.dev+8@gmail.com','12345678','USER',true,'00000000-0000-0000-0000-000000000207','ACTIVE'),
 ('00000000-0000-0000-0000-000000000218','congquynguyen296.dev+9@gmail.com','12345678','USER',true,'00000000-0000-0000-0000-000000000208','ACTIVE'),
 ('00000000-0000-0000-0000-000000000219','congquynguyen296.dev+10@gmail.com','12345678','USER',true,'00000000-0000-0000-0000-000000000209','ACTIVE');

-- 5) Contacts & Addresses for company and candidates
INSERT INTO contacts (id, value, verified, is_primary, contact_type, party_id, status)
VALUES
 ('00000000-0000-0000-0000-000000000301','support@careergraph.com',true,true,'EMAIL','00000000-0000-0000-0000-000000000100','ACTIVE'),
 ('00000000-0000-0000-0000-000000000302','+84-28-1234-5678',true,true,'PHONE','00000000-0000-0000-0000-000000000100','ACTIVE');

INSERT INTO addresses (id, name, country, province, district, ward, is_primary, address_type, party_id, status)
VALUES
 ('00000000-0000-0000-0000-000000000401','CareerGraph Headquarter','Vietnam','Ho Chi Minh','District 1','Ward 1',true,'BUSINESS','00000000-0000-0000-0000-000000000100','ACTIVE');

-- 6) Skills
INSERT INTO skills (id, name, category, description, status)
VALUES
 ('00000000-0000-0000-0000-000000000501','Java','Programming','Java programming language','ACTIVE'),
 ('00000000-0000-0000-0000-000000000502','Spring Boot','Framework','Spring Boot framework','ACTIVE'),
 ('00000000-0000-0000-0000-000000000503','React','Frontend','React.js library','ACTIVE'),
 ('00000000-0000-0000-0000-000000000504','AWS','Cloud','Amazon Web Services','ACTIVE'),
 ('00000000-0000-0000-0000-000000000505','Docker','DevOps','Containerization with Docker','ACTIVE'),
 ('00000000-0000-0000-0000-000000000506','Kubernetes','DevOps','Kubernetes orchestration','ACTIVE'),
 ('00000000-0000-0000-0000-000000000507','Python','Programming','Python programming language','ACTIVE'),
 ('00000000-0000-0000-0000-000000000508','SQL','Database','Relational database SQL','ACTIVE');

-- 7) Candidate skills (link candidates to skills)
INSERT INTO candidate_skill (id, proficiency_level, years_of_experience, is_verified, endorsed_by, endorsement_date, endorsement_count, candidate_id, skill_id, status)
VALUES
 ('00000000-0000-0000-0000-000000000601','Intermediate',2,true,'colleague1',NULL,1,'00000000-0000-0000-0000-000000000200','00000000-0000-0000-0000-000000000501','ACTIVE'),
 ('00000000-0000-0000-0000-000000000602','Intermediate',2,true,'colleague2',NULL,0,'00000000-0000-0000-0000-000000000200','00000000-0000-0000-0000-000000000502','ACTIVE'),
 ('00000000-0000-0000-0000-000000000603','Advanced',4,true,'lead',NULL,3,'00000000-0000-0000-0000-000000000201','00000000-0000-0000-0000-000000000503','ACTIVE'),
 ('00000000-0000-0000-0000-000000000604','Advanced',5,true,'manager',NULL,5,'00000000-0000-0000-0000-000000000202','00000000-0000-0000-0000-000000000504','ACTIVE');

-- 8) Educations
INSERT INTO educations (id, official_name, short_name, established_year, university_type, level, website, overview, accreditation, status)
VALUES
 ('00000000-0000-0000-0000-000000000701','Ho Chi Minh University of Technology','HCMUT',1995,'PUBLIC','BACHELOR','https://hcmut.edu.vn','Leading technical university','ABET','ACTIVE');

-- 9) Candidate education records
INSERT INTO candidate_education (id, start_date, end_date, degree_title, major, is_current, description, candidate_id, education_id, status)
VALUES
 ('00000000-0000-0000-0000-000000000801','2013-09-01','2017-06-30','B.Sc. Computer Science','Computer Science',false,'Graduated with honors','00000000-0000-0000-0000-000000000200','00000000-0000-0000-0000-000000000701','ACTIVE');

-- 10) Candidate experience (work history)
INSERT INTO candidate_experience (id, start_date, end_date, salary, job_title, is_current, description, candidate_id, company_id, status)
VALUES
 ('00000000-0000-0000-0000-000000000901','2018-08-01','2020-12-31',800,'Junior Developer',false,'Worked on backend services','00000000-0000-0000-0000-000000000200','00000000-0000-0000-0000-000000000100','ACTIVE'),
 ('00000000-0000-0000-0000-000000000902','2021-01-01',NULL,1200,'Backend Developer',true,'Working on microservices','00000000-0000-0000-0000-000000000204','00000000-0000-0000-0000-000000000100','ACTIVE');

-- 11) Jobs (under CareerGraph company) - responsibilities/qualifications stored as JSON arrays
INSERT INTO jobs (id, title, description, department, responsibilities, qualifications, minimum_qualifications, benefits, salary_range, min_experience, max_experience, experience_level, employment_type, job_category, education, posted_date, expiry_date, number_of_positions, contact_email, contact_phone, promotion_type, state, city, district, address, remote_job, views, applicants, saved, liked, shared, resume, cover_letter, status, company_id)
VALUES
 ('00000000-0000-0000-0000-000000001001','Software Engineer','Develop backend services for our core platform','Engineering','["Design and implement APIs","Write unit and integration tests","Collaborate with cross-functional teams"]','["3+ years experience","Proficiency in Java","Experience with Spring Boot"]','["Bachelor degree in CS or related","2+ years in backend"]','["Health insurance","Flexible time","Remote allowance"]','1000-2000',2,5,'MID','FULL_TIME','SOFTWARE','BACHELOR','2025-11-01','2026-01-01',2,'hr@careergraph.com','+84-28-1234-5678','free','HCM','District 1','Ward 1','123 Le Loi',false,120,10,5,2,1,true,true,'ACTIVE','00000000-0000-0000-0000-000000000100'),
 ('00000000-0000-0000-0000-000000001002','Frontend Engineer','Build beautiful and responsive UI','Product','["Develop React components","Optimize for performance","Collaborate with designers"]','["2+ years experience","Proficiency in React","Good CSS skills"]','["Portfolio of web apps"]','["Training budget","Gym"]','800-1500',1,4,'JUNIOR','FULL_TIME','SOFTWARE','BACHELOR','2025-11-05','2026-02-01',1,'frontend@careergraph.com','+84-28-1234-0000','free','HCM','District 1','Ward 1','123 Le Loi',false,80,6,4,1,1,true,true,'ACTIVE','00000000-0000-0000-0000-000000000100'),
 ('00000000-0000-0000-0000-000000001003','DevOps Engineer','Maintain CI/CD pipelines and cloud infrastructure','Infrastructure','["Maintain infrastructure","Implement CI/CD","Monitor systems"]','["Experience with Docker/Kubernetes","AWS experience"]','["3+ years experience"]','["Competitive salary","Remote work"]','1200-2500',3,7,'SENIOR','FULL_TIME','OPS','BACHELOR','2025-10-15','2026-03-01',1,'devops@careergraph.com','+84-28-2222-3333','paid','HCM','District 1','Ward 1','123 Le Loi',true,200,15,8,3,1,true,true,'ACTIVE','00000000-0000-0000-0000-000000000100');

-- 12) Applications: candidates apply to jobs
INSERT INTO applications (id, cover_letter, resume_url, rating, notes, applied_date, current_stage, stage_changed_at, current_stage_note, candidate_id, job_id, status)
VALUES
 ('00000000-0000-0000-0000-000000010001','I am very interested in this role','/resumes/200.pdf',NULL,'Strong Java background','2025-11-02','APPLIED',NULL,NULL,'00000000-0000-0000-0000-000000000200','00000000-0000-0000-0000-000000001001','ACTIVE'),
 ('00000000-0000-0000-0000-000000010002','Looking forward to contribute to frontend','/resumes/201.pdf',NULL,'React projects included','2025-11-06','SCREENING',NULL,NULL,'00000000-0000-0000-0000-000000000201','00000000-0000-0000-0000-000000001002','ACTIVE'),
 ('00000000-0000-0000-0000-000000010003','DevOps and infra experience','/resumes/202.pdf',NULL,'Experience with Kubernetes','2025-11-08','INTERVIEW',NULL,NULL,'00000000-0000-0000-0000-000000000202','00000000-0000-0000-0000-000000001003','ACTIVE');

-- 13) Application stage histories (simulate progression)
INSERT INTO application_stage_history (id, application_id, from_stage, to_stage, note, changed_by, changed_at, status)
VALUES
 ('00000000-0000-0000-0000-000000020001','00000000-0000-0000-0000-000000010001',NULL,'APPLIED','Candidate submitted application','00000000-0000-0000-0000-000000000210',CURRENT_TIMESTAMP,'ACTIVE'),
 ('00000000-0000-0000-0000-000000020002','00000000-0000-0000-0000-000000010002',NULL,'APPLIED','Candidate submitted application','00000000-0000-0000-0000-000000000211',CURRENT_TIMESTAMP,'ACTIVE'),
 ('00000000-0000-0000-0000-000000020003','00000000-0000-0000-0000-000000010002','APPLIED','SCREENING','Profile matched basic requirements','00000000-0000-0000-0000-000000000001',CURRENT_TIMESTAMP,'ACTIVE'),
 ('00000000-0000-0000-0000-000000020004','00000000-0000-0000-0000-000000010003',NULL,'APPLIED','Candidate submitted application','00000000-0000-0000-0000-000000000212',CURRENT_TIMESTAMP,'ACTIVE'),
 ('00000000-0000-0000-0000-000000020005','00000000-0000-0000-0000-000000010003','APPLIED','SCREENING','Initial screen passed','00000000-0000-0000-0000-000000000001',CURRENT_TIMESTAMP,'ACTIVE'),
 ('00000000-0000-0000-0000-000000020006','00000000-0000-0000-0000-000000010003','SCREENING','INTERVIEW','Scheduled interview with hiring manager','00000000-0000-0000-0000-000000000001',CURRENT_TIMESTAMP,'ACTIVE');

-- 14) Connections (sample)
INSERT INTO connections (id, note, connection_type, has_seen, disable_notification, candidate_id, connected_company_id, status)
VALUES
 ('00000000-0000-0000-0000-000000030001','Following CareerGraph','FOLLOW',true,false,'00000000-0000-0000-0000-000000000200','00000000-0000-0000-0000-000000000100','ACTIVE');

-- End of mock data

-- IMPORTANT: Depending on your DB constraints (NOT NULL and timestamps), you may need to adjust the INSERT statements (set created_date, last_modified_date) or remove columns.
