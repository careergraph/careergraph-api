-- ============================================
-- CareerGraph - Sample Data (Vietnamese Job Market)
-- Tương tự TopCV - Dữ liệu mẫu cho dev/demo
-- Matched to actual DB schema (JPA entities)
-- ============================================

-- =============================================
-- 1. SKILLS
-- =============================================
INSERT INTO skills (id, name, category, description, created_date, last_modified_date, status) VALUES
-- Programming Languages
('a0000001-0000-0000-0000-000000000001', 'Java', 'Programming Languages', 'Ngôn ngữ lập trình Java', NOW(), NOW(), 'ACTIVE'),
('a0000001-0000-0000-0000-000000000002', 'Python', 'Programming Languages', 'Ngôn ngữ lập trình Python', NOW(), NOW(), 'ACTIVE'),
('a0000001-0000-0000-0000-000000000003', 'JavaScript', 'Programming Languages', 'Ngôn ngữ lập trình JavaScript', NOW(), NOW(), 'ACTIVE'),
('a0000001-0000-0000-0000-000000000004', 'TypeScript', 'Programming Languages', 'Ngôn ngữ lập trình TypeScript', NOW(), NOW(), 'ACTIVE'),
('a0000001-0000-0000-0000-000000000005', 'C#', 'Programming Languages', 'Ngôn ngữ lập trình C#', NOW(), NOW(), 'ACTIVE'),
('a0000001-0000-0000-0000-000000000006', 'PHP', 'Programming Languages', 'Ngôn ngữ lập trình PHP', NOW(), NOW(), 'ACTIVE'),
('a0000001-0000-0000-0000-000000000007', 'Go', 'Programming Languages', 'Ngôn ngữ lập trình Go', NOW(), NOW(), 'ACTIVE'),
('a0000001-0000-0000-0000-000000000008', 'Rust', 'Programming Languages', 'Ngôn ngữ lập trình Rust', NOW(), NOW(), 'ACTIVE'),
('a0000001-0000-0000-0000-000000000009', 'Kotlin', 'Programming Languages', 'Ngôn ngữ lập trình Kotlin', NOW(), NOW(), 'ACTIVE'),
('a0000001-0000-0000-0000-000000000010', 'Swift', 'Programming Languages', 'Ngôn ngữ lập trình Swift', NOW(), NOW(), 'ACTIVE'),
('a0000001-0000-0000-0000-000000000011', 'Dart', 'Programming Languages', 'Ngôn ngữ lập trình Dart', NOW(), NOW(), 'ACTIVE'),
('a0000001-0000-0000-0000-000000000012', 'Ruby', 'Programming Languages', 'Ngôn ngữ lập trình Ruby', NOW(), NOW(), 'ACTIVE'),
-- Frameworks & Libraries
('a0000002-0000-0000-0000-000000000001', 'Spring Boot', 'Frameworks', 'Java Framework', NOW(), NOW(), 'ACTIVE'),
('a0000002-0000-0000-0000-000000000002', 'React', 'Frameworks', 'JavaScript UI Library', NOW(), NOW(), 'ACTIVE'),
('a0000002-0000-0000-0000-000000000003', 'Angular', 'Frameworks', 'TypeScript Framework', NOW(), NOW(), 'ACTIVE'),
('a0000002-0000-0000-0000-000000000004', 'Vue.js', 'Frameworks', 'JavaScript Framework', NOW(), NOW(), 'ACTIVE'),
('a0000002-0000-0000-0000-000000000005', 'Next.js', 'Frameworks', 'React Framework', NOW(), NOW(), 'ACTIVE'),
('a0000002-0000-0000-0000-000000000006', 'Node.js', 'Frameworks', 'JavaScript Runtime', NOW(), NOW(), 'ACTIVE'),
('a0000002-0000-0000-0000-000000000007', 'Django', 'Frameworks', 'Python Web Framework', NOW(), NOW(), 'ACTIVE'),
('a0000002-0000-0000-0000-000000000008', 'FastAPI', 'Frameworks', 'Python API Framework', NOW(), NOW(), 'ACTIVE'),
('a0000002-0000-0000-0000-000000000009', 'NestJS', 'Frameworks', 'Node.js Framework', NOW(), NOW(), 'ACTIVE'),
('a0000002-0000-0000-0000-000000000010', '.NET Core', 'Frameworks', 'Microsoft Framework', NOW(), NOW(), 'ACTIVE'),
('a0000002-0000-0000-0000-000000000011', 'Laravel', 'Frameworks', 'PHP Framework', NOW(), NOW(), 'ACTIVE'),
('a0000002-0000-0000-0000-000000000012', 'Flutter', 'Frameworks', 'Mobile UI Framework', NOW(), NOW(), 'ACTIVE'),
('a0000002-0000-0000-0000-000000000013', 'React Native', 'Frameworks', 'Mobile Framework', NOW(), NOW(), 'ACTIVE'),
('a0000002-0000-0000-0000-000000000014', 'TailwindCSS', 'Frameworks', 'CSS Framework', NOW(), NOW(), 'ACTIVE'),
('a0000002-0000-0000-0000-000000000015', 'Bootstrap', 'Frameworks', 'CSS Framework', NOW(), NOW(), 'ACTIVE'),
-- Databases
('a0000003-0000-0000-0000-000000000001', 'PostgreSQL', 'Databases', 'Relational Database', NOW(), NOW(), 'ACTIVE'),
('a0000003-0000-0000-0000-000000000002', 'MySQL', 'Databases', 'Relational Database', NOW(), NOW(), 'ACTIVE'),
('a0000003-0000-0000-0000-000000000003', 'MongoDB', 'Databases', 'NoSQL Database', NOW(), NOW(), 'ACTIVE'),
('a0000003-0000-0000-0000-000000000004', 'Redis', 'Databases', 'In-memory Database', NOW(), NOW(), 'ACTIVE'),
('a0000003-0000-0000-0000-000000000005', 'Elasticsearch', 'Databases', 'Search Engine', NOW(), NOW(), 'ACTIVE'),
('a0000003-0000-0000-0000-000000000006', 'Oracle', 'Databases', 'Enterprise Database', NOW(), NOW(), 'ACTIVE'),
('a0000003-0000-0000-0000-000000000007', 'SQL Server', 'Databases', 'Microsoft Database', NOW(), NOW(), 'ACTIVE'),
-- DevOps & Cloud
('a0000004-0000-0000-0000-000000000001', 'Docker', 'DevOps & Cloud', 'Container Platform', NOW(), NOW(), 'ACTIVE'),
('a0000004-0000-0000-0000-000000000002', 'Kubernetes', 'DevOps & Cloud', 'Container Orchestration', NOW(), NOW(), 'ACTIVE'),
('a0000004-0000-0000-0000-000000000003', 'AWS', 'DevOps & Cloud', 'Amazon Web Services', NOW(), NOW(), 'ACTIVE'),
('a0000004-0000-0000-0000-000000000004', 'Azure', 'DevOps & Cloud', 'Microsoft Azure', NOW(), NOW(), 'ACTIVE'),
('a0000004-0000-0000-0000-000000000005', 'GCP', 'DevOps & Cloud', 'Google Cloud Platform', NOW(), NOW(), 'ACTIVE'),
('a0000004-0000-0000-0000-000000000006', 'CI/CD', 'DevOps & Cloud', 'Continuous Integration/Deployment', NOW(), NOW(), 'ACTIVE'),
('a0000004-0000-0000-0000-000000000007', 'Jenkins', 'DevOps & Cloud', 'CI/CD Tool', NOW(), NOW(), 'ACTIVE'),
('a0000004-0000-0000-0000-000000000008', 'GitLab CI', 'DevOps & Cloud', 'CI/CD Platform', NOW(), NOW(), 'ACTIVE'),
('a0000004-0000-0000-0000-000000000009', 'Terraform', 'DevOps & Cloud', 'Infrastructure as Code', NOW(), NOW(), 'ACTIVE'),
('a0000004-0000-0000-0000-000000000010', 'Linux', 'DevOps & Cloud', 'Operating System', NOW(), NOW(), 'ACTIVE'),
('a0000004-0000-0000-0000-000000000011', 'Nginx', 'DevOps & Cloud', 'Web Server', NOW(), NOW(), 'ACTIVE'),
-- AI/ML
('a0000005-0000-0000-0000-000000000001', 'Machine Learning', 'AI/ML', 'Machine Learning', NOW(), NOW(), 'ACTIVE'),
('a0000005-0000-0000-0000-000000000002', 'Deep Learning', 'AI/ML', 'Deep Learning', NOW(), NOW(), 'ACTIVE'),
('a0000005-0000-0000-0000-000000000003', 'TensorFlow', 'AI/ML', 'ML Framework', NOW(), NOW(), 'ACTIVE'),
('a0000005-0000-0000-0000-000000000004', 'PyTorch', 'AI/ML', 'ML Framework', NOW(), NOW(), 'ACTIVE'),
('a0000005-0000-0000-0000-000000000005', 'NLP', 'AI/ML', 'Natural Language Processing', NOW(), NOW(), 'ACTIVE'),
('a0000005-0000-0000-0000-000000000006', 'Computer Vision', 'AI/ML', 'Computer Vision', NOW(), NOW(), 'ACTIVE'),
('a0000005-0000-0000-0000-000000000007', 'LLM', 'AI/ML', 'Large Language Models', NOW(), NOW(), 'ACTIVE'),
-- Soft Skills & Others
('a0000006-0000-0000-0000-000000000001', 'Git', 'Tools', 'Version Control', NOW(), NOW(), 'ACTIVE'),
('a0000006-0000-0000-0000-000000000002', 'Agile/Scrum', 'Methodology', 'Agile Development', NOW(), NOW(), 'ACTIVE'),
('a0000006-0000-0000-0000-000000000003', 'RESTful API', 'Architecture', 'API Design', NOW(), NOW(), 'ACTIVE'),
('a0000006-0000-0000-0000-000000000004', 'GraphQL', 'Architecture', 'API Query Language', NOW(), NOW(), 'ACTIVE'),
('a0000006-0000-0000-0000-000000000005', 'Microservices', 'Architecture', 'Microservices Architecture', NOW(), NOW(), 'ACTIVE'),
('a0000006-0000-0000-0000-000000000006', 'Unit Testing', 'Testing', 'Unit Testing', NOW(), NOW(), 'ACTIVE'),
('a0000006-0000-0000-0000-000000000007', 'Design Patterns', 'Architecture', 'Software Design Patterns', NOW(), NOW(), 'ACTIVE'),
('a0000006-0000-0000-0000-000000000008', 'System Design', 'Architecture', 'System Design', NOW(), NOW(), 'ACTIVE'),
('a0000006-0000-0000-0000-000000000009', 'Data Structures & Algorithms', 'Fundamentals', 'DSA', NOW(), NOW(), 'ACTIVE'),
('a0000006-0000-0000-0000-000000000010', 'Communication', 'Soft Skills', 'Communication Skills', NOW(), NOW(), 'ACTIVE'),
('a0000006-0000-0000-0000-000000000011', 'Teamwork', 'Soft Skills', 'Teamwork Skills', NOW(), NOW(), 'ACTIVE'),
('a0000006-0000-0000-0000-000000000012', 'Problem Solving', 'Soft Skills', 'Problem Solving Skills', NOW(), NOW(), 'ACTIVE'),
('a0000006-0000-0000-0000-000000000013', 'Leadership', 'Soft Skills', 'Leadership Skills', NOW(), NOW(), 'ACTIVE'),
('a0000006-0000-0000-0000-000000000014', 'Project Management', 'Soft Skills', 'Project Management', NOW(), NOW(), 'ACTIVE'),
('a0000006-0000-0000-0000-000000000015', 'English', 'Languages', 'English Language', NOW(), NOW(), 'ACTIVE'),
('a0000006-0000-0000-0000-000000000016', 'Japanese', 'Languages', 'Japanese Language', NOW(), NOW(), 'ACTIVE'),
('a0000006-0000-0000-0000-000000000017', 'Figma', 'Design', 'UI Design Tool', NOW(), NOW(), 'ACTIVE'),
('a0000006-0000-0000-0000-000000000018', 'UI/UX Design', 'Design', 'User Interface/Experience Design', NOW(), NOW(), 'ACTIVE'),
-- Data & Analytics
('a0000007-0000-0000-0000-000000000001', 'Power BI', 'Data & Analytics', 'Business Intelligence', NOW(), NOW(), 'ACTIVE'),
('a0000007-0000-0000-0000-000000000002', 'Tableau', 'Data & Analytics', 'Data Visualization', NOW(), NOW(), 'ACTIVE'),
('a0000007-0000-0000-0000-000000000003', 'Apache Kafka', 'Data & Analytics', 'Event Streaming Platform', NOW(), NOW(), 'ACTIVE'),
('a0000007-0000-0000-0000-000000000004', 'Apache Spark', 'Data & Analytics', 'Big Data Processing', NOW(), NOW(), 'ACTIVE'),
('a0000007-0000-0000-0000-000000000005', 'ETL', 'Data & Analytics', 'Extract Transform Load', NOW(), NOW(), 'ACTIVE'),
('a0000007-0000-0000-0000-000000000006', 'Data Warehousing', 'Data & Analytics', 'Data Warehousing', NOW(), NOW(), 'ACTIVE'),
-- Mobile
('a0000008-0000-0000-0000-000000000001', 'Android', 'Mobile', 'Android Development', NOW(), NOW(), 'ACTIVE'),
('a0000008-0000-0000-0000-000000000002', 'iOS', 'Mobile', 'iOS Development', NOW(), NOW(), 'ACTIVE'),
-- Security
('a0000009-0000-0000-0000-000000000001', 'OAuth2', 'Security', 'Authorization Framework', NOW(), NOW(), 'ACTIVE'),
('a0000009-0000-0000-0000-000000000002', 'JWT', 'Security', 'JSON Web Token', NOW(), NOW(), 'ACTIVE'),
('a0000009-0000-0000-0000-000000000003', 'Penetration Testing', 'Security', 'Security Testing', NOW(), NOW(), 'ACTIVE'),
('a0000009-0000-0000-0000-000000000004', 'Network Security', 'Security', 'Network Security', NOW(), NOW(), 'ACTIVE'),
-- Testing
('a0000010-0000-0000-0000-000000000001', 'Selenium', 'Testing', 'Browser Automation', NOW(), NOW(), 'ACTIVE'),
('a0000010-0000-0000-0000-000000000002', 'JUnit', 'Testing', 'Java Testing Framework', NOW(), NOW(), 'ACTIVE'),
('a0000010-0000-0000-0000-000000000003', 'Cypress', 'Testing', 'E2E Testing Framework', NOW(), NOW(), 'ACTIVE'),
('a0000010-0000-0000-0000-000000000004', 'Postman', 'Testing', 'API Testing Tool', NOW(), NOW(), 'ACTIVE'),
('a0000010-0000-0000-0000-000000000005', 'JMeter', 'Testing', 'Performance Testing', NOW(), NOW(), 'ACTIVE'),
('a0000010-0000-0000-0000-000000000006', 'Manual Testing', 'Testing', 'Manual Testing', NOW(), NOW(), 'ACTIVE'),
('a0000010-0000-0000-0000-000000000007', 'Automation Testing', 'Testing', 'Test Automation', NOW(), NOW(), 'ACTIVE')
ON CONFLICT (id) DO NOTHING;

-- =============================================
-- 2. PARTIES (for Companies) - Base table for Company (JOINED inheritance)
-- party_type = 'Company' (Hibernate default discriminator value = class name)
-- =============================================
INSERT INTO parties (id, party_type, tagname, avatar, cover, no_of_followers, no_of_following, no_of_connections, status, created_date, last_modified_date) VALUES
('c0000001-0000-0000-0000-000000000001', 'Company', 'fpt-software', 'https://upload.wikimedia.org/wikipedia/commons/thumb/1/11/FPT_logo_2010.svg/1200px-FPT_logo_2010.svg.png', NULL, 0, 0, 0, 'ACTIVE', NOW(), NOW()),
('c0000001-0000-0000-0000-000000000002', 'Company', 'vng-corporation', 'https://upload.wikimedia.org/wikipedia/vi/thumb/f/fe/VNG_Corporation_Logo.svg/1200px-VNG_Corporation_Logo.svg.png', NULL, 0, 0, 0, 'ACTIVE', NOW(), NOW()),
('c0000001-0000-0000-0000-000000000003', 'Company', 'viettel-group', 'https://upload.wikimedia.org/wikipedia/commons/thumb/b/bf/Viettel_logo_2021.svg/1200px-Viettel_logo_2021.svg.png', NULL, 0, 0, 0, 'ACTIVE', NOW(), NOW()),
('c0000001-0000-0000-0000-000000000004', 'Company', 'tiki', 'https://salt.tikicdn.com/ts/upload/e4/49/6c/3c5b7dfcf96ede1c6e16b960b49f1ded.png', NULL, 0, 0, 0, 'ACTIVE', NOW(), NOW()),
('c0000001-0000-0000-0000-000000000005', 'Company', 'shopee-vietnam', 'https://upload.wikimedia.org/wikipedia/commons/thumb/f/fe/Shopee.svg/1200px-Shopee.svg.png', NULL, 0, 0, 0, 'ACTIVE', NOW(), NOW()),
('c0000001-0000-0000-0000-000000000006', 'Company', 'momo', 'https://upload.wikimedia.org/wikipedia/vi/f/fe/MoMo_Logo.png', NULL, 0, 0, 0, 'ACTIVE', NOW(), NOW()),
('c0000001-0000-0000-0000-000000000007', 'Company', 'vnpay', 'https://vinadesign.vn/uploads/images/2023/05/vnpay-logo-vinadesign-25-12-57-55.jpg', NULL, 0, 0, 0, 'ACTIVE', NOW(), NOW()),
('c0000001-0000-0000-0000-000000000008', 'Company', 'kms-technology', 'https://kms-technology.com/wp-content/uploads/2021/01/KMS-Technology-Logo.png', NULL, 0, 0, 0, 'ACTIVE', NOW(), NOW()),
('c0000001-0000-0000-0000-000000000009', 'Company', 'nashtech', 'https://nashtechglobal.com/wp-content/uploads/2022/03/nashtech-logo.png', NULL, 0, 0, 0, 'ACTIVE', NOW(), NOW()),
('c0000001-0000-0000-0000-000000000010', 'Company', 'axon-active', 'https://axonactive.com/wp-content/uploads/2020/07/logo-axon.png', NULL, 0, 0, 0, 'ACTIVE', NOW(), NOW()),
('c0000001-0000-0000-0000-000000000011', 'Company', 'tma-solutions', 'https://tmasolutions.com/wp-content/uploads/2020/01/TMA-Logo.png', NULL, 0, 0, 0, 'ACTIVE', NOW(), NOW()),
('c0000001-0000-0000-0000-000000000012', 'Company', 'cmc-global', 'https://cmcglobal.com.vn/wp-content/uploads/2020/07/logo-cmc.png', NULL, 0, 0, 0, 'ACTIVE', NOW(), NOW()),
('c0000001-0000-0000-0000-000000000013', 'Company', 'vnlife', NULL, NULL, 0, 0, 0, 'ACTIVE', NOW(), NOW()),
('c0000001-0000-0000-0000-000000000014', 'Company', 'be-group', 'https://be.com.vn/wp-content/uploads/2022/07/logo-be.png', NULL, 0, 0, 0, 'ACTIVE', NOW(), NOW()),
('c0000001-0000-0000-0000-000000000015', 'Company', 'zalopay', NULL, NULL, 0, 0, 0, 'ACTIVE', NOW(), NOW()),
('c0000001-0000-0000-0000-000000000016', 'Company', 'en-solutions', NULL, NULL, 0, 0, 0, 'ACTIVE', NOW(), NOW()),
('c0000001-0000-0000-0000-000000000017', 'Company', 'grab-vietnam', 'https://upload.wikimedia.org/wikipedia/commons/thumb/f/f3/Grab_%28application%29_logo_2.svg/1200px-Grab_%28application%29_logo_2.svg.png', NULL, 0, 0, 0, 'ACTIVE', NOW(), NOW()),
('c0000001-0000-0000-0000-000000000018', 'Company', 'kpmg-vietnam', NULL, NULL, 0, 0, 0, 'ACTIVE', NOW(), NOW()),
('c0000001-0000-0000-0000-000000000019', 'Company', 'sendo', 'https://media3.scdn.vn/img4/2021/10_25/B3tCMcamVE5a2FEfz2wE.png', NULL, 0, 0, 0, 'ACTIVE', NOW(), NOW()),
('c0000001-0000-0000-0000-000000000020', 'Company', 'samsung-vietnam-rd', 'https://upload.wikimedia.org/wikipedia/commons/thumb/2/24/Samsung_Logo.svg/1200px-Samsung_Logo.svg.png', NULL, 0, 0, 0, 'ACTIVE', NOW(), NOW())
ON CONFLICT (id) DO NOTHING;

-- =============================================
-- 3. COMPANIES (joined table with parties)
-- =============================================
INSERT INTO companies (id, name, size, website, no_of_members, year_founded) VALUES
('c0000001-0000-0000-0000-000000000001', 'FPT Software', '5000+', 'https://fptsoftware.com', 30000, 1999),
('c0000001-0000-0000-0000-000000000002', 'VNG Corporation', '3000-5000', 'https://vng.com.vn', 4000, 2004),
('c0000001-0000-0000-0000-000000000003', 'Viettel Group', '5000+', 'https://viettel.com.vn', 50000, 1989),
('c0000001-0000-0000-0000-000000000004', 'Tiki', '1000-3000', 'https://tiki.vn', 2000, 2010),
('c0000001-0000-0000-0000-000000000005', 'Shopee Vietnam', '1000-3000', 'https://shopee.vn', 2500, 2015),
('c0000001-0000-0000-0000-000000000006', 'MoMo', '1000-3000', 'https://momo.vn', 1500, 2014),
('c0000001-0000-0000-0000-000000000007', 'VNPAY', '1000-3000', 'https://vnpay.vn', 1200, 2007),
('c0000001-0000-0000-0000-000000000008', 'KMS Technology', '1000-3000', 'https://kms-technology.com', 2000, 2009),
('c0000001-0000-0000-0000-000000000009', 'NashTech', '1000-3000', 'https://nashtechglobal.com', 1800, 2000),
('c0000001-0000-0000-0000-000000000010', 'Axon Active Vietnam', '500-1000', 'https://axonactive.com', 600, 2006),
('c0000001-0000-0000-0000-000000000011', 'TMA Solutions', '3000-5000', 'https://tmasolutions.com', 4000, 1997),
('c0000001-0000-0000-0000-000000000012', 'CMC Global', '1000-3000', 'https://cmcglobal.com.vn', 1500, 1993),
('c0000001-0000-0000-0000-000000000013', 'VNLife (VNPay Group)', '1000-3000', 'https://vnlife.vn', 1200, 2007),
('c0000001-0000-0000-0000-000000000014', 'BE Group (be)', '500-1000', 'https://be.com.vn', 800, 2018),
('c0000001-0000-0000-0000-000000000015', 'ZaloPay (by VNG)', '500-1000', 'https://zalopay.vn', 700, 2017),
('c0000001-0000-0000-0000-000000000016', 'Ến Solutions', '100-500', 'https://ens.vn', 200, 2015),
('c0000001-0000-0000-0000-000000000017', 'Grab Vietnam', '500-1000', 'https://grab.com', 800, 2012),
('c0000001-0000-0000-0000-000000000018', 'KPMG Vietnam', '500-1000', 'https://kpmg.com/vn', 600, 1994),
('c0000001-0000-0000-0000-000000000019', 'Sendo', '500-1000', 'https://sendo.vn', 700, 2012),
('c0000001-0000-0000-0000-000000000020', 'Samsung Vietnam R&D', '1000-3000', 'https://samsung.com/vn', 2500, 2012)
ON CONFLICT (id) DO NOTHING;

-- =============================================
-- 4. ADDRESSES (for Companies - HQ addresses)
-- =============================================
INSERT INTO addresses (id, name, country, province, district, ward, is_primary, address_type, party_id, status, created_date, last_modified_date) VALUES
('f0000001-0000-0000-0000-000000000001', 'Tòa nhà FPT, Phố Duy Tân, Cầu Giấy', 'Việt Nam', 'Hà Nội', 'Cầu Giấy', 'Duy Tân', true, 'HEADQUARTERS', 'c0000001-0000-0000-0000-000000000001', 'ACTIVE', NOW(), NOW()),
('f0000001-0000-0000-0000-000000000002', '182 Lê Đại Hành, Quận 11', 'Việt Nam', 'Hồ Chí Minh', 'Quận 11', NULL, true, 'HEADQUARTERS', 'c0000001-0000-0000-0000-000000000002', 'ACTIVE', NOW(), NOW()),
('f0000001-0000-0000-0000-000000000003', 'Số 1 Trần Hữu Dực, Nam Từ Liêm', 'Việt Nam', 'Hà Nội', 'Nam Từ Liêm', NULL, true, 'HEADQUARTERS', 'c0000001-0000-0000-0000-000000000003', 'ACTIVE', NOW(), NOW()),
('f0000001-0000-0000-0000-000000000004', '52 Út Tịch, Quận Tân Bình', 'Việt Nam', 'Hồ Chí Minh', 'Tân Bình', NULL, true, 'HEADQUARTERS', 'c0000001-0000-0000-0000-000000000004', 'ACTIVE', NOW(), NOW()),
('f0000001-0000-0000-0000-000000000005', 'Tầng 28, Bitexco Financial Tower, Quận 1', 'Việt Nam', 'Hồ Chí Minh', 'Quận 1', NULL, true, 'HEADQUARTERS', 'c0000001-0000-0000-0000-000000000005', 'ACTIVE', NOW(), NOW()),
('f0000001-0000-0000-0000-000000000006', '285 Cách Mạng Tháng 8, Quận 10', 'Việt Nam', 'Hồ Chí Minh', 'Quận 10', NULL, true, 'HEADQUARTERS', 'c0000001-0000-0000-0000-000000000006', 'ACTIVE', NOW(), NOW()),
('f0000001-0000-0000-0000-000000000007', '22 Láng Hạ, Ba Đình', 'Việt Nam', 'Hà Nội', 'Ba Đình', NULL, true, 'HEADQUARTERS', 'c0000001-0000-0000-0000-000000000007', 'ACTIVE', NOW(), NOW()),
('f0000001-0000-0000-0000-000000000008', '76 Lê Lai, Quận 1', 'Việt Nam', 'Hồ Chí Minh', 'Quận 1', NULL, true, 'HEADQUARTERS', 'c0000001-0000-0000-0000-000000000008', 'ACTIVE', NOW(), NOW()),
('f0000001-0000-0000-0000-000000000009', 'Tầng 9-10, Etown Central, Quận 4', 'Việt Nam', 'Hồ Chí Minh', 'Quận 4', NULL, true, 'HEADQUARTERS', 'c0000001-0000-0000-0000-000000000009', 'ACTIVE', NOW(), NOW()),
('f0000001-0000-0000-0000-000000000010', '2B Trường Sơn, Quận Tân Bình', 'Việt Nam', 'Hồ Chí Minh', 'Tân Bình', NULL, true, 'HEADQUARTERS', 'c0000001-0000-0000-0000-000000000010', 'ACTIVE', NOW(), NOW()),
('f0000001-0000-0000-0000-000000000011', 'Quang Trung Software City, Quận 12', 'Việt Nam', 'Hồ Chí Minh', 'Quận 12', NULL, true, 'HEADQUARTERS', 'c0000001-0000-0000-0000-000000000011', 'ACTIVE', NOW(), NOW()),
('f0000001-0000-0000-0000-000000000012', '11 Duy Tân, Cầu Giấy', 'Việt Nam', 'Hà Nội', 'Cầu Giấy', NULL, true, 'HEADQUARTERS', 'c0000001-0000-0000-0000-000000000012', 'ACTIVE', NOW(), NOW()),
('f0000001-0000-0000-0000-000000000013', '22 Láng Hạ, Ba Đình', 'Việt Nam', 'Hà Nội', 'Ba Đình', NULL, true, 'HEADQUARTERS', 'c0000001-0000-0000-0000-000000000013', 'ACTIVE', NOW(), NOW()),
('f0000001-0000-0000-0000-000000000014', 'Quận 1', 'Việt Nam', 'Hồ Chí Minh', 'Quận 1', NULL, true, 'HEADQUARTERS', 'c0000001-0000-0000-0000-000000000014', 'ACTIVE', NOW(), NOW()),
('f0000001-0000-0000-0000-000000000015', '182 Lê Đại Hành, Quận 11', 'Việt Nam', 'Hồ Chí Minh', 'Quận 11', NULL, true, 'HEADQUARTERS', 'c0000001-0000-0000-0000-000000000015', 'ACTIVE', NOW(), NOW()),
('f0000001-0000-0000-0000-000000000016', 'Quận 7', 'Việt Nam', 'Hồ Chí Minh', 'Quận 7', NULL, true, 'HEADQUARTERS', 'c0000001-0000-0000-0000-000000000016', 'ACTIVE', NOW(), NOW()),
('f0000001-0000-0000-0000-000000000017', 'Tầng 30, Bitexco Financial Tower, Quận 1', 'Việt Nam', 'Hồ Chí Minh', 'Quận 1', NULL, true, 'HEADQUARTERS', 'c0000001-0000-0000-0000-000000000017', 'ACTIVE', NOW(), NOW()),
('f0000001-0000-0000-0000-000000000018', 'Tầng 10, Sunwah Tower, Quận 1', 'Việt Nam', 'Hồ Chí Minh', 'Quận 1', NULL, true, 'HEADQUARTERS', 'c0000001-0000-0000-0000-000000000018', 'ACTIVE', NOW(), NOW()),
('f0000001-0000-0000-0000-000000000019', 'Số 7 Tôn Thất Thuyết, Cầu Giấy', 'Việt Nam', 'Hà Nội', 'Cầu Giấy', NULL, true, 'HEADQUARTERS', 'c0000001-0000-0000-0000-000000000019', 'ACTIVE', NOW(), NOW()),
('f0000001-0000-0000-0000-000000000020', 'Số 2 Thái Hà, Đống Đa', 'Việt Nam', 'Hà Nội', 'Đống Đa', NULL, true, 'HEADQUARTERS', 'c0000001-0000-0000-0000-000000000020', 'ACTIVE', NOW(), NOW())
ON CONFLICT (id) DO NOTHING;

-- =============================================
-- 5. CONTACTS (for Companies - email)
-- =============================================
INSERT INTO contacts (id, value, contact_type, is_primary, verified, party_id, status, created_date, last_modified_date) VALUES
('g0000001-0000-0000-0000-000000000001', 'hr@fptsoftware.com', 'EMAIL', true, true, 'c0000001-0000-0000-0000-000000000001', 'ACTIVE', NOW(), NOW()),
('g0000001-0000-0000-0000-000000000002', 'hr@vng.com.vn', 'EMAIL', true, true, 'c0000001-0000-0000-0000-000000000002', 'ACTIVE', NOW(), NOW()),
('g0000001-0000-0000-0000-000000000003', 'hr@viettel.com.vn', 'EMAIL', true, true, 'c0000001-0000-0000-0000-000000000003', 'ACTIVE', NOW(), NOW()),
('g0000001-0000-0000-0000-000000000004', 'hr@tiki.vn', 'EMAIL', true, true, 'c0000001-0000-0000-0000-000000000004', 'ACTIVE', NOW(), NOW()),
('g0000001-0000-0000-0000-000000000005', 'hr@shopee.vn', 'EMAIL', true, true, 'c0000001-0000-0000-0000-000000000005', 'ACTIVE', NOW(), NOW()),
('g0000001-0000-0000-0000-000000000006', 'hr@momo.vn', 'EMAIL', true, true, 'c0000001-0000-0000-0000-000000000006', 'ACTIVE', NOW(), NOW()),
('g0000001-0000-0000-0000-000000000007', 'hr@vnpay.vn', 'EMAIL', true, true, 'c0000001-0000-0000-0000-000000000007', 'ACTIVE', NOW(), NOW()),
('g0000001-0000-0000-0000-000000000008', 'hr@kms-technology.com', 'EMAIL', true, true, 'c0000001-0000-0000-0000-000000000008', 'ACTIVE', NOW(), NOW()),
('g0000001-0000-0000-0000-000000000009', 'hr@nashtechglobal.com', 'EMAIL', true, true, 'c0000001-0000-0000-0000-000000000009', 'ACTIVE', NOW(), NOW()),
('g0000001-0000-0000-0000-000000000010', 'hr@axonactive.com', 'EMAIL', true, true, 'c0000001-0000-0000-0000-000000000010', 'ACTIVE', NOW(), NOW()),
('g0000001-0000-0000-0000-000000000011', 'hr@tmasolutions.com', 'EMAIL', true, true, 'c0000001-0000-0000-0000-000000000011', 'ACTIVE', NOW(), NOW()),
('g0000001-0000-0000-0000-000000000012', 'hr@cmcglobal.com.vn', 'EMAIL', true, true, 'c0000001-0000-0000-0000-000000000012', 'ACTIVE', NOW(), NOW()),
('g0000001-0000-0000-0000-000000000017', 'hr@grab.com', 'EMAIL', true, true, 'c0000001-0000-0000-0000-000000000017', 'ACTIVE', NOW(), NOW()),
('g0000001-0000-0000-0000-000000000019', 'hr@sendo.vn', 'EMAIL', true, true, 'c0000001-0000-0000-0000-000000000019', 'ACTIVE', NOW(), NOW()),
('g0000001-0000-0000-0000-000000000020', 'hr@samsung.com', 'EMAIL', true, true, 'c0000001-0000-0000-0000-000000000020', 'ACTIVE', NOW(), NOW())
ON CONFLICT (id) DO NOTHING;

-- =============================================
-- 6. PARTIES (for Candidates) - Base table
-- =============================================
INSERT INTO parties (id, party_type, tagname, avatar, cover, no_of_followers, no_of_following, no_of_connections, status, created_date, last_modified_date) VALUES
('d0000003-0000-0000-0000-000000000001', 'Candidate', 'ngo-thanh-tung', NULL, NULL, 0, 0, 0, 'ACTIVE', NOW(), NOW()),
('d0000003-0000-0000-0000-000000000002', 'Candidate', 'vu-thi-huong', NULL, NULL, 0, 0, 0, 'ACTIVE', NOW(), NOW()),
('d0000003-0000-0000-0000-000000000003', 'Candidate', 'dang-quoc-viet', NULL, NULL, 0, 0, 0, 'ACTIVE', NOW(), NOW()),
('d0000003-0000-0000-0000-000000000004', 'Candidate', 'bui-thi-lan', NULL, NULL, 0, 0, 0, 'ACTIVE', NOW(), NOW()),
('d0000003-0000-0000-0000-000000000005', 'Candidate', 'ly-hoang-nam', NULL, NULL, 0, 0, 0, 'ACTIVE', NOW(), NOW()),
('d0000003-0000-0000-0000-000000000006', 'Candidate', 'mai-phuong-thao', NULL, NULL, 0, 0, 0, 'ACTIVE', NOW(), NOW()),
('d0000003-0000-0000-0000-000000000007', 'Candidate', 'truong-cong-thanh', NULL, NULL, 0, 0, 0, 'ACTIVE', NOW(), NOW()),
('d0000003-0000-0000-0000-000000000008', 'Candidate', 'dinh-thi-ngoc', NULL, NULL, 0, 0, 0, 'ACTIVE', NOW(), NOW()),
('d0000003-0000-0000-0000-000000000009', 'Candidate', 'phan-dinh-khoa', NULL, NULL, 0, 0, 0, 'ACTIVE', NOW(), NOW()),
('d0000003-0000-0000-0000-000000000010', 'Candidate', 'cao-thi-my', NULL, NULL, 0, 0, 0, 'ACTIVE', NOW(), NOW())
ON CONFLICT (id) DO NOTHING;

-- =============================================
-- 7. CANDIDATES (joined with parties)
-- =============================================
INSERT INTO candidates (id, first_name, last_name, gender, date_of_birth, work_location, is_open_to_work, is_open_to_notify_new_job, years_of_experience) VALUES
('d0000003-0000-0000-0000-000000000001', 'Ngô Thanh', 'Tùng', 'MALE', '1998-02-14', 'Hà Nội', true, true, 2),
('d0000003-0000-0000-0000-000000000002', 'Vũ Thị', 'Hương', 'FEMALE', '1999-06-30', 'Hồ Chí Minh', true, true, 1),
('d0000003-0000-0000-0000-000000000003', 'Đặng Quốc', 'Việt', 'MALE', '1997-12-05', 'Đà Nẵng', true, false, 3),
('d0000003-0000-0000-0000-000000000004', 'Bùi Thị', 'Lan', 'FEMALE', '2000-04-22', 'Cần Thơ', true, true, 0),
('d0000003-0000-0000-0000-000000000005', 'Lý Hoàng', 'Nam', 'MALE', '1996-09-08', 'Hồ Chí Minh', true, true, 5),
('d0000003-0000-0000-0000-000000000006', 'Mai Phương', 'Thảo', 'FEMALE', '2001-01-17', 'Hà Nội', true, true, 0),
('d0000003-0000-0000-0000-000000000007', 'Trương Công', 'Thành', 'MALE', '1995-10-30', 'Hồ Chí Minh', true, false, 6),
('d0000003-0000-0000-0000-000000000008', 'Đinh Thị', 'Ngọc', 'FEMALE', '1998-07-12', 'Đà Nẵng', true, true, 2),
('d0000003-0000-0000-0000-000000000009', 'Phan Đình', 'Khoa', 'MALE', '1997-03-28', 'Hà Nội', true, true, 4),
('d0000003-0000-0000-0000-000000000010', 'Cao Thị', 'Mỹ', 'FEMALE', '2000-11-05', 'Hồ Chí Minh', true, true, 1)
ON CONFLICT (id) DO NOTHING;

-- =============================================
-- 8. CONTACTS (for Candidates - phone & email)
-- =============================================
INSERT INTO contacts (id, value, contact_type, is_primary, verified, party_id, status, created_date, last_modified_date) VALUES
-- Candidate phones
('g0000002-0000-0000-0000-000000000001', '0900000010', 'PHONE', true, true, 'd0000003-0000-0000-0000-000000000001', 'ACTIVE', NOW(), NOW()),
('g0000002-0000-0000-0000-000000000002', '0900000011', 'PHONE', true, true, 'd0000003-0000-0000-0000-000000000002', 'ACTIVE', NOW(), NOW()),
('g0000002-0000-0000-0000-000000000003', '0900000012', 'PHONE', true, true, 'd0000003-0000-0000-0000-000000000003', 'ACTIVE', NOW(), NOW()),
('g0000002-0000-0000-0000-000000000004', '0900000013', 'PHONE', true, true, 'd0000003-0000-0000-0000-000000000004', 'ACTIVE', NOW(), NOW()),
('g0000002-0000-0000-0000-000000000005', '0900000014', 'PHONE', true, true, 'd0000003-0000-0000-0000-000000000005', 'ACTIVE', NOW(), NOW()),
('g0000002-0000-0000-0000-000000000006', '0900000015', 'PHONE', true, true, 'd0000003-0000-0000-0000-000000000006', 'ACTIVE', NOW(), NOW()),
('g0000002-0000-0000-0000-000000000007', '0900000016', 'PHONE', true, true, 'd0000003-0000-0000-0000-000000000007', 'ACTIVE', NOW(), NOW()),
('g0000002-0000-0000-0000-000000000008', '0900000017', 'PHONE', true, true, 'd0000003-0000-0000-0000-000000000008', 'ACTIVE', NOW(), NOW()),
('g0000002-0000-0000-0000-000000000009', '0900000018', 'PHONE', true, true, 'd0000003-0000-0000-0000-000000000009', 'ACTIVE', NOW(), NOW()),
('g0000002-0000-0000-0000-000000000010', '0900000019', 'PHONE', true, true, 'd0000003-0000-0000-0000-000000000010', 'ACTIVE', NOW(), NOW()),
-- Candidate emails
('g0000003-0000-0000-0000-000000000001', 'candidate1@gmail.com', 'EMAIL', false, true, 'd0000003-0000-0000-0000-000000000001', 'ACTIVE', NOW(), NOW()),
('g0000003-0000-0000-0000-000000000002', 'candidate2@gmail.com', 'EMAIL', false, true, 'd0000003-0000-0000-0000-000000000002', 'ACTIVE', NOW(), NOW()),
('g0000003-0000-0000-0000-000000000003', 'candidate3@gmail.com', 'EMAIL', false, true, 'd0000003-0000-0000-0000-000000000003', 'ACTIVE', NOW(), NOW()),
('g0000003-0000-0000-0000-000000000004', 'candidate4@gmail.com', 'EMAIL', false, true, 'd0000003-0000-0000-0000-000000000004', 'ACTIVE', NOW(), NOW()),
('g0000003-0000-0000-0000-000000000005', 'candidate5@gmail.com', 'EMAIL', false, true, 'd0000003-0000-0000-0000-000000000005', 'ACTIVE', NOW(), NOW()),
('g0000003-0000-0000-0000-000000000006', 'candidate6@gmail.com', 'EMAIL', false, true, 'd0000003-0000-0000-0000-000000000006', 'ACTIVE', NOW(), NOW()),
('g0000003-0000-0000-0000-000000000007', 'candidate7@gmail.com', 'EMAIL', false, true, 'd0000003-0000-0000-0000-000000000007', 'ACTIVE', NOW(), NOW()),
('g0000003-0000-0000-0000-000000000008', 'candidate8@gmail.com', 'EMAIL', false, true, 'd0000003-0000-0000-0000-000000000008', 'ACTIVE', NOW(), NOW()),
('g0000003-0000-0000-0000-000000000009', 'candidate9@gmail.com', 'EMAIL', false, true, 'd0000003-0000-0000-0000-000000000009', 'ACTIVE', NOW(), NOW()),
('g0000003-0000-0000-0000-000000000010', 'candidate10@gmail.com', 'EMAIL', false, true, 'd0000003-0000-0000-0000-000000000010', 'ACTIVE', NOW(), NOW())
ON CONFLICT (id) DO NOTHING;

-- =============================================
-- 9. ADDRESSES (for Candidates)
-- =============================================
INSERT INTO addresses (id, name, country, province, district, ward, is_primary, address_type, party_id, status, created_date, last_modified_date) VALUES
('f0000002-0000-0000-0000-000000000001', 'Thanh Xuân', 'Việt Nam', 'Hà Nội', 'Thanh Xuân', NULL, true, 'HOME_ADDRESS', 'd0000003-0000-0000-0000-000000000001', 'ACTIVE', NOW(), NOW()),
('f0000002-0000-0000-0000-000000000002', 'Quận 3', 'Việt Nam', 'Hồ Chí Minh', 'Quận 3', NULL, true, 'HOME_ADDRESS', 'd0000003-0000-0000-0000-000000000002', 'ACTIVE', NOW(), NOW()),
('f0000002-0000-0000-0000-000000000003', 'Hải Châu', 'Việt Nam', 'Đà Nẵng', 'Hải Châu', NULL, true, 'HOME_ADDRESS', 'd0000003-0000-0000-0000-000000000003', 'ACTIVE', NOW(), NOW()),
('f0000002-0000-0000-0000-000000000004', 'Ninh Kiều', 'Việt Nam', 'Cần Thơ', 'Ninh Kiều', NULL, true, 'HOME_ADDRESS', 'd0000003-0000-0000-0000-000000000004', 'ACTIVE', NOW(), NOW()),
('f0000002-0000-0000-0000-000000000005', 'Thủ Đức', 'Việt Nam', 'Hồ Chí Minh', 'Thủ Đức', NULL, true, 'HOME_ADDRESS', 'd0000003-0000-0000-0000-000000000005', 'ACTIVE', NOW(), NOW()),
('f0000002-0000-0000-0000-000000000006', 'Cầu Giấy', 'Việt Nam', 'Hà Nội', 'Cầu Giấy', NULL, true, 'HOME_ADDRESS', 'd0000003-0000-0000-0000-000000000006', 'ACTIVE', NOW(), NOW()),
('f0000002-0000-0000-0000-000000000007', 'Quận 7', 'Việt Nam', 'Hồ Chí Minh', 'Quận 7', NULL, true, 'HOME_ADDRESS', 'd0000003-0000-0000-0000-000000000007', 'ACTIVE', NOW(), NOW()),
('f0000002-0000-0000-0000-000000000008', 'Sơn Trà', 'Việt Nam', 'Đà Nẵng', 'Sơn Trà', NULL, true, 'HOME_ADDRESS', 'd0000003-0000-0000-0000-000000000008', 'ACTIVE', NOW(), NOW()),
('f0000002-0000-0000-0000-000000000009', 'Ba Đình', 'Việt Nam', 'Hà Nội', 'Ba Đình', NULL, true, 'HOME_ADDRESS', 'd0000003-0000-0000-0000-000000000009', 'ACTIVE', NOW(), NOW()),
('f0000002-0000-0000-0000-000000000010', 'Bình Thạnh', 'Việt Nam', 'Hồ Chí Minh', 'Bình Thạnh', NULL, true, 'HOME_ADDRESS', 'd0000003-0000-0000-0000-000000000010', 'ACTIVE', NOW(), NOW())
ON CONFLICT (id) DO NOTHING;

-- =============================================
-- 10. ACCOUNTS (login accounts)
-- Role enum: USER, ADMIN, HR, ASSISTANT
-- Password hash for "Password@123": $2a$10$xLmEZQaC3Q8mC4kg4wgzaOSU6ZpXEn1gy6LV/xjLZ0z3yXmqWvKSy
-- =============================================

-- Admin (no candidate/company link)
INSERT INTO accounts (id, email, password_hash, role, email_verified, status, created_date, last_modified_date) VALUES
('d0000001-0000-0000-0000-000000000001', 'admin@careergraph.vn',
 '$2a$10$xLmEZQaC3Q8mC4kg4wgzaOSU6ZpXEn1gy6LV/xjLZ0z3yXmqWvKSy',
 'ADMIN', true, 'ACTIVE', NOW(), NOW())
ON CONFLICT (id) DO NOTHING;

-- HR Accounts (linked to companies via company_id)
INSERT INTO accounts (id, email, password_hash, role, email_verified, company_id, status, created_date, last_modified_date) VALUES
('d0000002-0000-0000-0000-000000000001', 'hr.fpt@careergraph.vn',
 '$2a$10$xLmEZQaC3Q8mC4kg4wgzaOSU6ZpXEn1gy6LV/xjLZ0z3yXmqWvKSy',
 'HR', true, 'c0000001-0000-0000-0000-000000000001', 'ACTIVE', NOW(), NOW()),
('d0000002-0000-0000-0000-000000000002', 'hr.vng@careergraph.vn',
 '$2a$10$xLmEZQaC3Q8mC4kg4wgzaOSU6ZpXEn1gy6LV/xjLZ0z3yXmqWvKSy',
 'HR', true, 'c0000001-0000-0000-0000-000000000002', 'ACTIVE', NOW(), NOW()),
('d0000002-0000-0000-0000-000000000003', 'hr.shopee@careergraph.vn',
 '$2a$10$xLmEZQaC3Q8mC4kg4wgzaOSU6ZpXEn1gy6LV/xjLZ0z3yXmqWvKSy',
 'HR', true, 'c0000001-0000-0000-0000-000000000005', 'ACTIVE', NOW(), NOW()),
('d0000002-0000-0000-0000-000000000004', 'hr.momo@careergraph.vn',
 '$2a$10$xLmEZQaC3Q8mC4kg4wgzaOSU6ZpXEn1gy6LV/xjLZ0z3yXmqWvKSy',
 'HR', true, 'c0000001-0000-0000-0000-000000000006', 'ACTIVE', NOW(), NOW()),
('d0000002-0000-0000-0000-000000000005', 'hr.grab@careergraph.vn',
 '$2a$10$xLmEZQaC3Q8mC4kg4wgzaOSU6ZpXEn1gy6LV/xjLZ0z3yXmqWvKSy',
 'HR', true, 'c0000001-0000-0000-0000-000000000017', 'ACTIVE', NOW(), NOW())
ON CONFLICT (id) DO NOTHING;

-- Candidate Accounts (linked to candidates via candidate_id)
INSERT INTO accounts (id, email, password_hash, role, email_verified, candidate_id, status, created_date, last_modified_date) VALUES
('d0000004-0000-0000-0000-000000000001', 'candidate1@gmail.com',
 '$2a$10$xLmEZQaC3Q8mC4kg4wgzaOSU6ZpXEn1gy6LV/xjLZ0z3yXmqWvKSy',
 'USER', true, 'd0000003-0000-0000-0000-000000000001', 'ACTIVE', NOW(), NOW()),
('d0000004-0000-0000-0000-000000000002', 'candidate2@gmail.com',
 '$2a$10$xLmEZQaC3Q8mC4kg4wgzaOSU6ZpXEn1gy6LV/xjLZ0z3yXmqWvKSy',
 'USER', true, 'd0000003-0000-0000-0000-000000000002', 'ACTIVE', NOW(), NOW()),
('d0000004-0000-0000-0000-000000000003', 'candidate3@gmail.com',
 '$2a$10$xLmEZQaC3Q8mC4kg4wgzaOSU6ZpXEn1gy6LV/xjLZ0z3yXmqWvKSy',
 'USER', true, 'd0000003-0000-0000-0000-000000000003', 'ACTIVE', NOW(), NOW()),
('d0000004-0000-0000-0000-000000000004', 'candidate4@gmail.com',
 '$2a$10$xLmEZQaC3Q8mC4kg4wgzaOSU6ZpXEn1gy6LV/xjLZ0z3yXmqWvKSy',
 'USER', true, 'd0000003-0000-0000-0000-000000000004', 'ACTIVE', NOW(), NOW()),
('d0000004-0000-0000-0000-000000000005', 'candidate5@gmail.com',
 '$2a$10$xLmEZQaC3Q8mC4kg4wgzaOSU6ZpXEn1gy6LV/xjLZ0z3yXmqWvKSy',
 'USER', true, 'd0000003-0000-0000-0000-000000000005', 'ACTIVE', NOW(), NOW()),
('d0000004-0000-0000-0000-000000000006', 'candidate6@gmail.com',
 '$2a$10$xLmEZQaC3Q8mC4kg4wgzaOSU6ZpXEn1gy6LV/xjLZ0z3yXmqWvKSy',
 'USER', true, 'd0000003-0000-0000-0000-000000000006', 'ACTIVE', NOW(), NOW()),
('d0000004-0000-0000-0000-000000000007', 'candidate7@gmail.com',
 '$2a$10$xLmEZQaC3Q8mC4kg4wgzaOSU6ZpXEn1gy6LV/xjLZ0z3yXmqWvKSy',
 'USER', true, 'd0000003-0000-0000-0000-000000000007', 'ACTIVE', NOW(), NOW()),
('d0000004-0000-0000-0000-000000000008', 'candidate8@gmail.com',
 '$2a$10$xLmEZQaC3Q8mC4kg4wgzaOSU6ZpXEn1gy6LV/xjLZ0z3yXmqWvKSy',
 'USER', true, 'd0000003-0000-0000-0000-000000000008', 'ACTIVE', NOW(), NOW()),
('d0000004-0000-0000-0000-000000000009', 'candidate9@gmail.com',
 '$2a$10$xLmEZQaC3Q8mC4kg4wgzaOSU6ZpXEn1gy6LV/xjLZ0z3yXmqWvKSy',
 'USER', true, 'd0000003-0000-0000-0000-000000000009', 'ACTIVE', NOW(), NOW()),
('d0000004-0000-0000-0000-000000000010', 'candidate10@gmail.com',
 '$2a$10$xLmEZQaC3Q8mC4kg4wgzaOSU6ZpXEn1gy6LV/xjLZ0z3yXmqWvKSy',
 'USER', true, 'd0000003-0000-0000-0000-000000000010', 'ACTIVE', NOW(), NOW())
ON CONFLICT (id) DO NOTHING;

-- =============================================
-- 11. JOBS (Tin tuyển dụng thực tế - TopCV style)
-- =============================================
INSERT INTO jobs (id, title, description, responsibilities, qualifications, minimum_qualifications, benefits,
  salary_range, experience_level, employment_type, job_category, education,
  city, address, posted_date, expiry_date, number_of_positions, remote_job,
  views, applicants, saved, liked, shared,
  company_id, status, created_date, last_modified_date, created_by) VALUES

-- FPT Software Jobs
('e0000001-0000-0000-0000-000000000001',
 'Senior Java Developer (Spring Boot)',
 'Chúng tôi tìm kiếm Senior Java Developer để tham gia phát triển các hệ thống enterprise cho khách hàng Nhật Bản và châu Âu. Phát triển và bảo trì các ứng dụng web sử dụng Spring Boot, Microservices. Thiết kế và tối ưu cơ sở dữ liệu PostgreSQL/MySQL. Code review, mentoring junior developers.',
 '["Phát triển và bảo trì ứng dụng web sử dụng Spring Boot, Microservices","Thiết kế và tối ưu cơ sở dữ liệu PostgreSQL/MySQL","Code review, mentoring junior developers","Làm việc trực tiếp với khách hàng Nhật Bản qua tiếng Anh","Áp dụng CI/CD, automated testing vào quy trình phát triển"]',
 '["4+ năm kinh nghiệm Java, Spring Boot","Thành thạo Spring Security, Spring Data JPA, Spring Cloud","Kinh nghiệm Microservices, Docker, Kubernetes","Solid understanding of OOP, Design Patterns, SOLID principles","Kinh nghiệm PostgreSQL/MySQL, Redis","Tiếng Anh giao tiếp tốt"]',
 '["3+ năm kinh nghiệm Java","Hiểu biết Spring Boot cơ bản","Kinh nghiệm SQL databases"]',
 '["Lương: 25-45 triệu VND","Review lương 2 lần/năm","Thưởng tháng 13 + Performance bonus","Bảo hiểm sức khỏe FPT Care cho cả gia đình","15 ngày phép/năm + 3 ngày personal leave","Onsite Nhật Bản, châu Âu","Đào tạo AWS/Azure certification miễn phí","Phòng gym, canteen, xe đưa đón"]',
 '25-45 triệu VND', 'SENIOR', 'FULL_TIME', 'ENGINEER', 'BACHELORS_DEGREE',
 'Hà Nội', 'Tòa nhà FPT, Phố Duy Tân, Cầu Giấy', '2026-01-15', '2026-06-30', 3, false,
 120, 15, 8, 5, 2,
 'c0000001-0000-0000-0000-000000000001', 'ACTIVE', NOW(), NOW(), 'd0000002-0000-0000-0000-000000000001'),

('e0000001-0000-0000-0000-000000000002',
 'Frontend Developer (React/Next.js)',
 'Tham gia team product phát triển nền tảng digital banking cho ngân hàng top 5 Việt Nam. Phát triển giao diện web app sử dụng React, Next.js, TypeScript. Implement responsive design, cross-browser compatibility.',
 '["Phát triển giao diện web app sử dụng React, Next.js, TypeScript","Implement responsive design, cross-browser compatibility","Tích hợp RESTful API và GraphQL","Viết unit test, integration test","Optimize performance (lazy loading, code splitting, caching)"]',
 '["2+ năm kinh nghiệm React.js hoặc Next.js","Thành thạo TypeScript, HTML5, CSS3, SASS","Kinh nghiệm state management (Redux, Zustand, React Query)","Hiểu biết về responsive design, TailwindCSS","Kinh nghiệm Git, CI/CD"]',
 '["1+ năm kinh nghiệm React.js","HTML, CSS, JavaScript cơ bản vững"]',
 '["Lương: 15-30 triệu VND","Flexible working hours","MacBook Pro cho developer","Các khóa học Udemy, Coursera miễn phí","Team building hàng quý","Cơ hội onsite nước ngoài"]',
 '15-30 triệu VND', 'MIDDLE', 'FULL_TIME', 'ENGINEER', 'BACHELORS_DEGREE',
 'Hà Nội', 'FPT Tower, Keangnam, Nam Từ Liêm', '2026-01-20', '2026-05-31', 2, false,
 95, 22, 12, 8, 3,
 'c0000001-0000-0000-0000-000000000001', 'ACTIVE', NOW(), NOW(), 'd0000002-0000-0000-0000-000000000001'),

('e0000001-0000-0000-0000-000000000003',
 'DevOps Engineer',
 'Gia nhập team DevOps xây dựng hạ tầng cloud cho các dự án lớn. Thiết kế và triển khai CI/CD pipelines. Quản lý infrastructure trên AWS/Azure bằng Terraform.',
 '["Thiết kế và triển khai CI/CD pipelines","Quản lý infrastructure trên AWS/Azure bằng Terraform","Containerize ứng dụng với Docker, orchestration với Kubernetes","Monitoring, logging, alerting (Prometheus, Grafana, ELK)","Đảm bảo security, compliance cho hệ thống production"]',
 '["3+ năm kinh nghiệm DevOps/SRE","Thành thạo Docker, Kubernetes","Kinh nghiệm AWS (EC2, ECS, EKS, S3, RDS, CloudFront)","Terraform hoặc Ansible","Jenkins/GitLab CI/GitHub Actions","Linux administration","Scripting: Bash, Python"]',
 '["2+ năm kinh nghiệm DevOps","Kinh nghiệm Docker cơ bản","Linux administration"]',
 '["Lương: 20-40 triệu VND","Hỗ trợ thi chứng chỉ AWS/Azure","Work from home 2 ngày/tuần","Laptop + 2 màn hình","ESOP cho nhân viên xuất sắc"]',
 '20-40 triệu VND', 'MIDDLE', 'FULL_TIME', 'ENGINEER', 'BACHELORS_DEGREE',
 'Đà Nẵng', 'FPT Complex, Đà Nẵng', '2026-01-10', '2026-05-15', 2, false,
 78, 10, 5, 3, 1,
 'c0000001-0000-0000-0000-000000000001', 'ACTIVE', NOW(), NOW(), 'd0000002-0000-0000-0000-000000000001'),

-- VNG Jobs
('e0000002-0000-0000-0000-000000000001',
 'Backend Engineer (Go/Python) - Zalo Team',
 'Tham gia team Zalo phát triển hệ thống messaging phục vụ 75 triệu người dùng. Phát triển backend services xử lý hàng triệu request/giây.',
 '["Phát triển backend services xử lý hàng triệu request/giây","Thiết kế distributed systems, message queue","Tối ưu performance, latency cho real-time messaging","Implement caching strategies với Redis Cluster","Phát triển API cho Zalo Mini App ecosystem"]',
 '["3+ năm kinh nghiệm backend development","Thành thạo Go hoặc Python","Kinh nghiệm distributed systems, microservices","Redis, Kafka, RabbitMQ","PostgreSQL/MySQL optimization","Understanding of networking protocols"]',
 '["2+ năm kinh nghiệm backend","Go hoặc Python cơ bản"]',
 '["Lương: 30-60 triệu VND (competitive)","Equity/ESOP","Thưởng tháng 13-14","Bảo hiểm premium cho gia đình","Canteen miễn phí","Phòng gym, bể bơi","Hackathon, tech talk hàng tuần","MacBook Pro M3 Max"]',
 '30-60 triệu VND', 'SENIOR', 'FULL_TIME', 'ENGINEER', 'BACHELORS_DEGREE',
 'Hồ Chí Minh', '182 Lê Đại Hành, Quận 11', '2026-02-01', '2026-07-31', 2, false,
 200, 30, 25, 15, 8,
 'c0000001-0000-0000-0000-000000000002', 'ACTIVE', NOW(), NOW(), 'd0000002-0000-0000-0000-000000000002'),

('e0000002-0000-0000-0000-000000000002',
 'AI/ML Engineer - Zing MP3 Recommendation',
 'Phát triển hệ thống recommendation cho Zing MP3 với 20 triệu người dùng. Xây dựng và tối ưu recommendation engine.',
 '["Xây dựng và tối ưu recommendation engine","Training, fine-tuning deep learning models","A/B testing các thuật toán recommendation","Phát triển data pipeline xử lý hành vi người dùng","Research state-of-the-art papers về RecSys, NLP"]',
 '["2+ năm kinh nghiệm ML/AI","Python, PyTorch hoặc TensorFlow","Kinh nghiệm recommendation systems, collaborative filtering","NLP, embedding techniques","SQL, Spark, Airflow"]',
 '["1+ năm kinh nghiệm ML/AI","Python cơ bản","Hiểu biết về statistics"]',
 '["Lương: 25-50 triệu VND","GPU workstation cho research","Tài trợ tham gia conference quốc tế","Flexible hours","Budget học tập không giới hạn"]',
 '25-50 triệu VND', 'MIDDLE', 'FULL_TIME', 'ENGINEER', 'MASTERS_DEGREE',
 'Hồ Chí Minh', '182 Lê Đại Hành, Quận 11', '2026-01-25', '2026-06-30', 1, false,
 150, 18, 20, 12, 5,
 'c0000001-0000-0000-0000-000000000002', 'ACTIVE', NOW(), NOW(), 'd0000002-0000-0000-0000-000000000002'),

-- Shopee Jobs
('e0000003-0000-0000-0000-000000000001',
 'Senior Backend Engineer (Java) - Payment Team',
 'Phát triển hệ thống thanh toán xử lý hàng triệu giao dịch mỗi ngày cho Shopee khu vực Đông Nam Á.',
 '["Design và implement payment processing system","Đảm bảo data consistency trong distributed transactions","Performance tuning cho high-throughput system","Collaborate với teams across 7 countries","On-call rotation để đảm bảo 99.99% uptime"]',
 '["4+ năm kinh nghiệm Java/Go backend","Deep understanding of distributed systems","Kinh nghiệm payment/fintech là strong plus","MySQL, Redis, Kafka","System design capability","Tiếng Anh tốt (working language)"]',
 '["3+ năm kinh nghiệm Java backend","Kinh nghiệm distributed systems"]',
 '["Lương: $2,000-4,000 USD gross","RSU (Restricted Stock Unit)","18 ngày phép","Bảo hiểm quốc tế Cigna","Shuttle bus, canteen","Annual trip nước ngoài","Relocation support cho expat"]',
 '$2,000-4,000 USD', 'SENIOR', 'FULL_TIME', 'ENGINEER', 'BACHELORS_DEGREE',
 'Hồ Chí Minh', 'Tầng 28, Bitexco, Quận 1', '2026-02-05', '2026-08-31', 3, false,
 280, 35, 30, 20, 10,
 'c0000001-0000-0000-0000-000000000005', 'ACTIVE', NOW(), NOW(), 'd0000002-0000-0000-0000-000000000003'),

('e0000003-0000-0000-0000-000000000002',
 'Data Engineer - Search & Recommendation',
 'Xây dựng data infrastructure phục vụ Search và Recommendation trên Shopee.',
 '["Thiết kế và phát triển data pipelines (batch + streaming)","Xây dựng data warehouse, data lake","Optimize Spark jobs xử lý petabytes data","Develop real-time feature store cho ML models","Ensure data quality và governance"]',
 '["3+ năm kinh nghiệm Data Engineering","Python, Scala, SQL","Apache Spark, Kafka, Flink","Airflow, dbt","AWS/GCP cloud services","Kinh nghiệm xử lý big data (TB+ scale)"]',
 '["2+ năm kinh nghiệm Data Engineering","Python, SQL","Kinh nghiệm Spark cơ bản"]',
 '["Lương: $1,800-3,500 USD","RSU","Conference sponsorship","Education allowance","Health screening annually"]',
 '$1,800-3,500 USD', 'MIDDLE', 'FULL_TIME', 'ENGINEER', 'BACHELORS_DEGREE',
 'Hồ Chí Minh', 'Tầng 28, Bitexco, Quận 1', '2026-01-30', '2026-07-15', 2, false,
 180, 22, 18, 10, 6,
 'c0000001-0000-0000-0000-000000000005', 'ACTIVE', NOW(), NOW(), 'd0000002-0000-0000-0000-000000000003'),

-- MoMo Jobs
('e0000004-0000-0000-0000-000000000001',
 'Mobile Developer (React Native) - MoMo Super App',
 'Phát triển tính năng mới cho MoMo super app với 35 triệu người dùng.',
 '["Phát triển tính năng mới trên React Native","Tối ưu app performance, giảm crash rate","Implement deep linking, push notification","Tích hợp payment SDK, biometric authentication","A/B testing features cho millions of users"]',
 '["2+ năm React Native hoặc Flutter","TypeScript/JavaScript","Redux/MobX state management","Native modules (iOS/Android)","RESTful API integration","Kinh nghiệm app performance optimization"]',
 '["1+ năm React Native","JavaScript cơ bản"]',
 '["Lương: 18-35 triệu VND","iPhone/Android test devices","Flexible working","Stock option","Team building monthly","Snack bar, coffee miễn phí"]',
 '18-35 triệu VND', 'MIDDLE', 'FULL_TIME', 'ENGINEER', 'BACHELORS_DEGREE',
 'Hồ Chí Minh', '285 Cách Mạng Tháng 8, Quận 10', '2026-02-01', '2026-06-15', 2, false,
 130, 20, 15, 8, 4,
 'c0000001-0000-0000-0000-000000000006', 'ACTIVE', NOW(), NOW(), 'd0000002-0000-0000-0000-000000000004'),

('e0000004-0000-0000-0000-000000000002',
 'Security Engineer - MoMo',
 'Bảo vệ hệ thống tài chính phục vụ hàng triệu giao dịch mỗi ngày.',
 '["Penetration testing, vulnerability assessment","Security code review","Incident response và forensics","Implement security monitoring (SIEM)","Compliance PCI-DSS, ISO 27001","Security awareness training"]',
 '["3+ năm kinh nghiệm Information Security","OWASP Top 10, SANS Top 25","Penetration testing tools (Burp Suite, Metasploit)","Network security, firewall","Scripting (Python, Bash)","Chứng chỉ: CEH, OSCP, CISSP là plus"]',
 '["2+ năm kinh nghiệm Security","OWASP Top 10"]',
 '["Lương: 25-50 triệu VND","Hỗ trợ thi chứng chỉ security","Conference budget","Bug bounty rewards","Bảo hiểm premium"]',
 '25-50 triệu VND', 'SENIOR', 'FULL_TIME', 'ENGINEER', 'BACHELORS_DEGREE',
 'Hồ Chí Minh', '285 Cách Mạng Tháng 8, Quận 10', '2026-01-15', '2026-05-30', 1, false,
 90, 8, 6, 4, 2,
 'c0000001-0000-0000-0000-000000000006', 'ACTIVE', NOW(), NOW(), 'd0000002-0000-0000-0000-000000000004'),

-- Grab Jobs
('e0000005-0000-0000-0000-000000000001',
 'Staff Software Engineer - Maps & Geospatial',
 'Phát triển platform bản đồ và geospatial cho Grab toàn Đông Nam Á.',
 '["Thiết kế hệ thống geospatial xử lý real-time location data","Phát triển routing algorithms, ETA prediction","Map matching, geocoding services","Process billions of GPS data points daily","Collaborate với Singapore HQ"]',
 '["5+ năm software engineering","Go, Java, hoặc C++","Distributed systems at scale","Geospatial data (PostGIS, H3)","ML basics (regression, classification)","System design cho large-scale systems","Fluent English"]',
 '["4+ năm software engineering","Go hoặc Java","Distributed systems experience"]',
 '["Lương: $3,000-6,000 USD","Grab RSU program","Unlimited GrabCar/GrabFood credits","20 ngày phép + birthday leave","Health insurance (Cigna)","Relocation package to Singapore possible","Learning & development budget"]',
 '$3,000-6,000 USD', 'SENIOR', 'FULL_TIME', 'ENGINEER', 'BACHELORS_DEGREE',
 'Hồ Chí Minh', 'Tầng 30, Bitexco, Quận 1', '2026-02-10', '2026-09-30', 1, false,
 300, 25, 35, 22, 12,
 'c0000001-0000-0000-0000-000000000017', 'ACTIVE', NOW(), NOW(), 'd0000002-0000-0000-0000-000000000005'),

('e0000005-0000-0000-0000-000000000002',
 'Platform Engineer (Kubernetes) - Grab',
 'Xây dựng và vận hành platform infrastructure cho toàn bộ Grab engineering.',
 '["Quản lý multi-cluster Kubernetes (1000+ nodes)","Develop internal developer platform (IDP)","Service mesh (Istio), observability (OpenTelemetry)","Cost optimization cho cloud infrastructure","Automate everything"]',
 '["4+ năm platform/SRE/DevOps","Expert Kubernetes","Go programming","Terraform, Helm","AWS EKS, networking","Prometheus, Grafana, Jaeger","Strong Linux fundamentals"]',
 '["3+ năm DevOps/SRE","Kubernetes experience","Linux administration"]',
 '["Lương: $2,500-5,000 USD","RSU","CKA/CKAD certification support","International travel","Top-tier benefits package"]',
 '$2,500-5,000 USD', 'SENIOR', 'FULL_TIME', 'ENGINEER', 'BACHELORS_DEGREE',
 'Hồ Chí Minh', 'Tầng 30, Bitexco, Quận 1', '2026-02-05', '2026-08-15', 1, false,
 220, 18, 28, 15, 7,
 'c0000001-0000-0000-0000-000000000017', 'ACTIVE', NOW(), NOW(), 'd0000002-0000-0000-0000-000000000005'),

-- Samsung R&D Jobs
('e0000006-0000-0000-0000-000000000001',
 'Android Framework Developer - Samsung',
 'Phát triển Android framework cho Samsung Galaxy devices.',
 '["Phát triển và customize Android AOSP framework","Implement One UI features","Performance optimization cho Samsung devices","Bug fixing, stability improvement","Collaborate với global R&D teams (Korea, India, Poland)"]',
 '["3+ năm kinh nghiệm Android development","Java, Kotlin","Android SDK, NDK, AOSP","Understanding of Android internals (Binder, ART, SurfaceFlinger)","C/C++ cho native development","Git, Gerrit","Tiếng Anh tốt (global team)"]',
 '["2+ năm Android development","Java hoặc Kotlin"]',
 '["Lương: 20-40 triệu VND","Thưởng 2-4 tháng lương/năm","Samsung devices miễn phí","Bảo hiểm Samsung Care","Canteen Hàn Quốc","Annual trip Korea","Training tại Samsung HQ Seoul"]',
 '20-40 triệu VND', 'MIDDLE', 'FULL_TIME', 'ENGINEER', 'BACHELORS_DEGREE',
 'Hà Nội', 'Số 2 Thái Hà, Đống Đa', '2026-02-01', '2026-07-31', 3, false,
 160, 25, 18, 10, 5,
 'c0000001-0000-0000-0000-000000000020', 'ACTIVE', NOW(), NOW(), 'd0000002-0000-0000-0000-000000000001'),

-- KMS Technology Jobs
('e0000007-0000-0000-0000-000000000001',
 'QA Automation Engineer - KMS',
 'Xây dựng automation testing framework cho healthcare software (US market).',
 '["Thiết kế và implement automation test framework","Viết automation scripts (Selenium, Cypress, Playwright)","API testing (Postman, RestAssured)","Performance testing (JMeter, K6)","CI/CD integration cho automated tests","Mentor junior QA engineers"]',
 '["3+ năm QA Automation","Java hoặc Python","Selenium WebDriver, Cypress, hoặc Playwright","API testing, REST/GraphQL","SQL queries","Git, Jenkins/GitHub Actions","ISTQB certification là plus"]',
 '["2+ năm QA Automation","Java hoặc Python","Selenium cơ bản"]',
 '["Lương: 15-30 triệu VND","13th month salary","Premium healthcare","English training","ISTQB certification support","Company trip Vietnam/abroad","Work from home 3 days/week"]',
 '15-30 triệu VND', 'MIDDLE', 'FULL_TIME', 'ENGINEER', 'BACHELORS_DEGREE',
 'Hồ Chí Minh', '76 Lê Lai, Quận 1', '2026-01-20', '2026-06-30', 2, false,
 85, 12, 8, 5, 2,
 'c0000001-0000-0000-0000-000000000008', 'ACTIVE', NOW(), NOW(), 'd0000002-0000-0000-0000-000000000003'),

-- VNPAY Jobs
('e0000008-0000-0000-0000-000000000001',
 'Fullstack Developer (.NET + React) - VNPAY',
 'Phát triển hệ thống thanh toán QR quốc gia.',
 '["Phát triển backend API bằng .NET Core 8","Frontend React with TypeScript","Tích hợp với hệ thống ngân hàng (Napas, banking gateway)","Xử lý transactions real-time","Ensure PCI-DSS compliance"]',
 '["2+ năm .NET Core + React","C#, TypeScript","SQL Server, PostgreSQL","Redis, RabbitMQ","Docker basics","Kinh nghiệm payment/banking là plus"]',
 '["1+ năm .NET Core hoặc React","C# hoặc TypeScript"]',
 '["Lương: 15-30 triệu VND","Thưởng lễ tết, project bonus","Bảo hiểm sức khỏe","Du lịch hàng năm","Đào tạo nội bộ fintech knowledge"]',
 '15-30 triệu VND', 'MIDDLE', 'FULL_TIME', 'ENGINEER', 'BACHELORS_DEGREE',
 'Hà Nội', '22 Láng Hạ, Ba Đình', '2026-01-25', '2026-06-15', 2, false,
 70, 10, 6, 3, 1,
 'c0000001-0000-0000-0000-000000000007', 'ACTIVE', NOW(), NOW(), 'd0000002-0000-0000-0000-000000000001'),

-- Viettel Jobs
('e0000009-0000-0000-0000-000000000001',
 'Cloud Architect - Viettel Solutions',
 'Thiết kế kiến trúc cloud cho hệ thống chính phủ điện tử và doanh nghiệp lớn.',
 '["Thiết kế cloud architecture (private/hybrid cloud)","Migration strategy từ on-premise lên cloud","Design high-availability, disaster recovery","Security architecture cho government systems","Technical leadership, solution consulting"]',
 '["5+ năm kinh nghiệm, trong đó 2+ năm Cloud Architecture","AWS/Azure/GCP certified (SA Professional)","Kubernetes, Docker, Terraform","Networking (VPN, Load Balancer, CDN)","Security best practices","Kinh nghiệm enterprise/government projects"]',
 '["3+ năm kinh nghiệm IT","Cloud computing experience","Kubernetes cơ bản"]',
 '["Lương: 30-60 triệu VND","Phụ cấp dự án","Bảo hiểm Viettel","Certification sponsorship","Project bonus hấp dẫn","Cơ hội làm dự án quốc gia"]',
 '30-60 triệu VND', 'SENIOR', 'FULL_TIME', 'ENGINEER', 'BACHELORS_DEGREE',
 'Hà Nội', 'Số 1 Trần Hữu Dực, Nam Từ Liêm', '2026-01-15', '2026-07-15', 1, false,
 110, 8, 10, 6, 3,
 'c0000001-0000-0000-0000-000000000003', 'ACTIVE', NOW(), NOW(), 'd0000002-0000-0000-0000-000000000001'),

-- Intern/Fresher positions
('e0000010-0000-0000-0000-000000000001',
 'Intern Backend Developer (Java/Spring Boot) - FPT',
 'Chương trình thực tập có lương tại FPT Software dành cho sinh viên năm cuối.',
 '["Học hỏi và thực hành Java, Spring Boot","Tham gia phát triển tính năng đơn giản","Viết unit test","Được mentor 1-1 hướng dẫn","Cơ hội chuyển full-time sau thực tập"]',
 '["Sinh viên năm cuối hoặc mới tốt nghiệp CNTT","Kiến thức cơ bản Java, OOP","Hiểu biết cơ bản SQL","Tinh thần học hỏi, chủ động","Có thể thực tập full-time 3-6 tháng"]',
 '["Sinh viên CNTT","Java cơ bản"]',
 '["Lương thực tập: 5-8 triệu VND","Mentor 1-1","Chứng chỉ hoàn thành","Cơ hội nhận offer full-time","Giữ xe, canteen miễn phí"]',
 '5-8 triệu VND', 'INTERN', 'INTERNSHIP', 'ENGINEER', 'BACHELORS_DEGREE',
 'Hà Nội', 'Tòa nhà FPT, Phố Duy Tân, Cầu Giấy', '2026-02-01', '2026-04-30', 10, false,
 200, 50, 15, 10, 8,
 'c0000001-0000-0000-0000-000000000001', 'ACTIVE', NOW(), NOW(), 'd0000002-0000-0000-0000-000000000001'),

('e0000010-0000-0000-0000-000000000002',
 'Fresher Frontend Developer - Tiki',
 'Tìm kiếm fresher tài năng gia nhập team frontend Tiki.',
 '["Phát triển UI components với React","Implement pixel-perfect designs từ Figma","Học hỏi best practices từ senior developers","Tham gia sprint planning, code review","Viết documentation"]',
 '["0-1 năm kinh nghiệm","HTML, CSS, JavaScript cơ bản vững","React basics (hooks, components)","Git workflow","Khả năng học hỏi nhanh","Portfolio hoặc personal projects là plus"]',
 '["HTML, CSS, JavaScript cơ bản","React basics"]',
 '["Lương: 8-15 triệu VND","Structured training program 3 tháng","Buddy system","MacBook","Bảo hiểm sức khỏe","Happy hour thứ 6"]',
 '8-15 triệu VND', 'FRESHER', 'FULL_TIME', 'ENGINEER', 'BACHELORS_DEGREE',
 'Hồ Chí Minh', '52 Út Tịch, Quận Tân Bình', '2026-01-20', '2026-05-15', 5, false,
 180, 40, 12, 8, 5,
 'c0000001-0000-0000-0000-000000000004', 'ACTIVE', NOW(), NOW(), 'd0000002-0000-0000-0000-000000000002'),

-- Remote/Part-time jobs
('e0000010-0000-0000-0000-000000000003',
 'Part-time Technical Writer - CMC Global',
 'Viết tài liệu kỹ thuật cho các sản phẩm phần mềm.',
 '["Viết API documentation, user guides","Tạo technical blog posts","Review và cập nhật documentation hiện tại","Collaborate với development team","Maintain knowledge base"]',
 '["Kỹ năng viết tiếng Anh tốt (IELTS 6.5+)","Hiểu biết cơ bản về software development","Kinh nghiệm Markdown, Confluence","Khả năng giải thích concepts phức tạp đơn giản","Part-time 20h/tuần"]',
 '["Tiếng Anh tốt","Hiểu biết CNTT cơ bản"]',
 '["150,000-250,000 VND/giờ","Làm việc remote 100%","Flexible schedule","Cơ hội chuyển full-time"]',
 '6-10 triệu VND', 'JUNIOR', 'PART_TIME', 'ENGINEER', 'BACHELORS_DEGREE',
 'Hà Nội', 'Remote', '2026-02-01', '2026-04-30', 2, true,
 60, 8, 5, 3, 1,
 'c0000001-0000-0000-0000-000000000012', 'ACTIVE', NOW(), NOW(), 'd0000002-0000-0000-0000-000000000001')
ON CONFLICT (id) DO NOTHING;

-- =============================================
-- 12. CANDIDATE_SKILL (skills belong to candidates, not jobs)
-- =============================================
INSERT INTO candidate_skill (id, candidate_id, skill_id, proficiency_level, years_of_experience, is_verified, endorsement_count, status, created_date, last_modified_date) VALUES
-- Candidate 1: Ngô Thanh Tùng - Backend Java
('h0000001-0000-0000-0000-000000000001', 'd0000003-0000-0000-0000-000000000001', 'a0000001-0000-0000-0000-000000000001', 'INTERMEDIATE', 2, false, 0, 'ACTIVE', NOW(), NOW()), -- Java
('h0000001-0000-0000-0000-000000000002', 'd0000003-0000-0000-0000-000000000001', 'a0000002-0000-0000-0000-000000000001', 'INTERMEDIATE', 2, false, 0, 'ACTIVE', NOW(), NOW()), -- Spring Boot
('h0000001-0000-0000-0000-000000000003', 'd0000003-0000-0000-0000-000000000001', 'a0000003-0000-0000-0000-000000000001', 'BEGINNER', 1, false, 0, 'ACTIVE', NOW(), NOW()), -- PostgreSQL
('h0000001-0000-0000-0000-000000000004', 'd0000003-0000-0000-0000-000000000001', 'a0000006-0000-0000-0000-000000000001', 'INTERMEDIATE', 2, false, 0, 'ACTIVE', NOW(), NOW()), -- Git

-- Candidate 2: Vũ Thị Hương - Frontend React
('h0000002-0000-0000-0000-000000000001', 'd0000003-0000-0000-0000-000000000002', 'a0000001-0000-0000-0000-000000000003', 'INTERMEDIATE', 1, false, 0, 'ACTIVE', NOW(), NOW()), -- JavaScript
('h0000002-0000-0000-0000-000000000002', 'd0000003-0000-0000-0000-000000000002', 'a0000002-0000-0000-0000-000000000002', 'BEGINNER', 1, false, 0, 'ACTIVE', NOW(), NOW()), -- React
('h0000002-0000-0000-0000-000000000003', 'd0000003-0000-0000-0000-000000000002', 'a0000002-0000-0000-0000-000000000014', 'BEGINNER', 1, false, 0, 'ACTIVE', NOW(), NOW()), -- TailwindCSS

-- Candidate 3: Đặng Quốc Việt - DevOps
('h0000003-0000-0000-0000-000000000001', 'd0000003-0000-0000-0000-000000000003', 'a0000004-0000-0000-0000-000000000001', 'ADVANCED', 3, false, 0, 'ACTIVE', NOW(), NOW()), -- Docker
('h0000003-0000-0000-0000-000000000002', 'd0000003-0000-0000-0000-000000000003', 'a0000004-0000-0000-0000-000000000002', 'INTERMEDIATE', 2, false, 0, 'ACTIVE', NOW(), NOW()), -- Kubernetes
('h0000003-0000-0000-0000-000000000003', 'd0000003-0000-0000-0000-000000000003', 'a0000004-0000-0000-0000-000000000003', 'INTERMEDIATE', 2, false, 0, 'ACTIVE', NOW(), NOW()), -- AWS
('h0000003-0000-0000-0000-000000000004', 'd0000003-0000-0000-0000-000000000003', 'a0000004-0000-0000-0000-000000000010', 'ADVANCED', 3, false, 0, 'ACTIVE', NOW(), NOW()), -- Linux

-- Candidate 5: Lý Hoàng Nam - Senior Backend
('h0000005-0000-0000-0000-000000000001', 'd0000003-0000-0000-0000-000000000005', 'a0000001-0000-0000-0000-000000000001', 'ADVANCED', 5, false, 0, 'ACTIVE', NOW(), NOW()), -- Java
('h0000005-0000-0000-0000-000000000002', 'd0000003-0000-0000-0000-000000000005', 'a0000002-0000-0000-0000-000000000001', 'ADVANCED', 4, false, 0, 'ACTIVE', NOW(), NOW()), -- Spring Boot
('h0000005-0000-0000-0000-000000000003', 'd0000003-0000-0000-0000-000000000005', 'a0000006-0000-0000-0000-000000000005', 'INTERMEDIATE', 3, false, 0, 'ACTIVE', NOW(), NOW()), -- Microservices
('h0000005-0000-0000-0000-000000000004', 'd0000003-0000-0000-0000-000000000005', 'a0000003-0000-0000-0000-000000000004', 'INTERMEDIATE', 3, false, 0, 'ACTIVE', NOW(), NOW()), -- Redis
('h0000005-0000-0000-0000-000000000005', 'd0000003-0000-0000-0000-000000000005', 'a0000006-0000-0000-0000-000000000008', 'INTERMEDIATE', 2, false, 0, 'ACTIVE', NOW(), NOW()), -- System Design

-- Candidate 7: Trương Công Thành - Fullstack Senior
('h0000007-0000-0000-0000-000000000001', 'd0000003-0000-0000-0000-000000000007', 'a0000001-0000-0000-0000-000000000002', 'ADVANCED', 5, false, 0, 'ACTIVE', NOW(), NOW()), -- Python
('h0000007-0000-0000-0000-000000000002', 'd0000003-0000-0000-0000-000000000007', 'a0000001-0000-0000-0000-000000000007', 'INTERMEDIATE', 3, false, 0, 'ACTIVE', NOW(), NOW()), -- Go
('h0000007-0000-0000-0000-000000000003', 'd0000003-0000-0000-0000-000000000007', 'a0000002-0000-0000-0000-000000000002', 'INTERMEDIATE', 4, false, 0, 'ACTIVE', NOW(), NOW()), -- React
('h0000007-0000-0000-0000-000000000004', 'd0000003-0000-0000-0000-000000000007', 'a0000003-0000-0000-0000-000000000001', 'ADVANCED', 5, false, 0, 'ACTIVE', NOW(), NOW()), -- PostgreSQL

-- Candidate 9: Phan Đình Khoa - ML/AI
('h0000009-0000-0000-0000-000000000001', 'd0000003-0000-0000-0000-000000000009', 'a0000001-0000-0000-0000-000000000002', 'ADVANCED', 4, false, 0, 'ACTIVE', NOW(), NOW()), -- Python
('h0000009-0000-0000-0000-000000000002', 'd0000003-0000-0000-0000-000000000009', 'a0000005-0000-0000-0000-000000000001', 'INTERMEDIATE', 3, false, 0, 'ACTIVE', NOW(), NOW()), -- Machine Learning
('h0000009-0000-0000-0000-000000000003', 'd0000003-0000-0000-0000-000000000009', 'a0000005-0000-0000-0000-000000000004', 'INTERMEDIATE', 2, false, 0, 'ACTIVE', NOW(), NOW()), -- PyTorch
('h0000009-0000-0000-0000-000000000004', 'd0000003-0000-0000-0000-000000000009', 'a0000005-0000-0000-0000-000000000005', 'BEGINNER', 1, false, 0, 'ACTIVE', NOW(), NOW()) -- NLP
ON CONFLICT (id) DO NOTHING;

-- =============================================
-- DONE! Summary:
-- - 80+ Skills (Programming, Frameworks, DB, DevOps, AI, Soft Skills)
-- - 20 Companies (real VN tech companies) via parties + companies tables
-- - 20 Company addresses (HEADQUARTERS)
-- - 15 Company contacts (EMAIL)
-- - 10 Candidates via parties + candidates tables
-- - 20 Candidate contacts (PHONE + EMAIL)
-- - 10 Candidate addresses
-- - 16 Accounts (1 admin + 5 HR + 10 candidates)
-- - 16 Jobs (diverse: senior, middle, junior, intern, part-time)
-- - 24 Candidate-Skill mappings
-- - Password for all accounts: Password@123
--
-- NOTE: This schema uses JOINED inheritance (Party -> Candidate/Company/Education)
-- Tables that DO NOT exist in this schema:
--   roles, permissions, roles_permissions, industries,
--   company_industry, users, users_roles, job_skill, company_user
-- Role is a simple enum column on accounts table (USER, ADMIN, HR, ASSISTANT)
-- =============================================
