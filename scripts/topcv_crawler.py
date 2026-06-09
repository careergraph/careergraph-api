#!/usr/bin/env python3
"""
TopCV crawler and CareerGraph seed generator.

Use cases:
1) Crawl TopCV job links and store normalized raw data to JSON.
2) Convert crawled JSON into a schema-compatible SQL seed for CareerGraph.

The generated SQL is designed for QA/demo data:
- around 1000 jobs
- around 100 companies
- around 200 candidate accounts
- applications, saved jobs, company followers and stage history backed by rows

Notes:
- Respect TopCV terms of use and robots policy.
- Candidate/application/follow metrics are synthesized deterministically because
  TopCV does not expose those private platform signals.
"""

from __future__ import annotations

import argparse
import html
import json
import random
import re
import time
import uuid
from collections import Counter, defaultdict
from dataclasses import asdict, dataclass
from datetime import date, datetime, timedelta
from pathlib import Path
from typing import Iterable
from urllib.parse import quote_plus

import requests

USER_AGENT = (
    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) "
    "AppleWebKit/537.36 (KHTML, like Gecko) "
    "Chrome/124.0.0.0 Safari/537.36"
)

DEFAULT_LISTING_URL = "https://www.topcv.vn/tim-viec-lam"
DEFAULT_OUTPUT_JSON = "scripts/output/topcv_jobs.json"
DEFAULT_OUTPUT_SQL = "init-scripts/topcv-import.sql"
DEFAULT_TARGET_JOBS = 1000
DEFAULT_TARGET_COMPANIES = 100
DEFAULT_TARGET_CANDIDATES = 200
SEED_PASSWORD = "12345678"
SEED_PASSWORD_HASH = "$2a$10$LCONFIH0lntguBp2Iob8HugwnmhLcT3G8ltz1lpURT/B6iXJoTVgW"
RNG_SEED = 20260525
NOW = datetime(2026, 5, 25, 9, 0, 0)

FIRST_NAMES = [
    "An",
    "Binh",
    "Chi",
    "Dung",
    "Giang",
    "Hanh",
    "Khanh",
    "Lam",
    "Minh",
    "Ngoc",
    "Phuc",
    "Quang",
    "Trang",
    "Tuan",
    "Vy",
    "Yen",
]

LAST_NAMES = [
    "Nguyen",
    "Tran",
    "Le",
    "Pham",
    "Hoang",
    "Phan",
    "Vu",
    "Dang",
    "Bui",
    "Do",
]

COMPANY_PREFIXES = [
    "Vina",
    "Nova",
    "Zen",
    "Atlas",
    "Lotus",
    "Kite",
    "Next",
    "Pixel",
    "Cloud",
    "Vertex",
]

COMPANY_SUFFIXES = [
    "Tech",
    "Solutions",
    "Labs",
    "Digital",
    "Systems",
    "Works",
    "Partners",
    "Dynamics",
    "Holdings",
    "Studio",
]

COMPANY_SERIES = ["Group Alpha", "Group Beta", "Group Delta", "Group Horizon", "Group Nexus"]

COMPANY_SIZES = ["11-50", "51-200", "201-500", "501-1000"]

TRUSTED_COMPANY_LOGO_DOMAINS = [
    "topcv.vn",
    "fpt.com",
    "fptsoftware.com",
    "viettel.com.vn",
    "vnpt.vn",
    "vng.com.vn",
    "cmc.com.vn",
    "momo.vn",
    "vnpay.vn",
    "tiki.vn",
    "shopee.vn",
    "lazada.vn",
    "masangroup.com",
    "vinamilk.com.vn",
    "vietcombank.com.vn",
    "techcombank.com",
    "vpbank.com.vn",
    "acb.com.vn",
    "vietjetair.com",
    "vietnamairlines.com",
]

LOCATIONS = [
    {
        "state": "Ho Chi Minh",
        "city": "District 1",
        "district": "Ben Nghe",
        "address": "12 Le Duan",
    },
    {
        "state": "Ha Noi",
        "city": "Cau Giay",
        "district": "Dich Vong",
        "address": "88 Duy Tan",
    },
    {
        "state": "Da Nang",
        "city": "Hai Chau",
        "district": "Thach Thang",
        "address": "15 Bach Dang",
    },
    {
        "state": "Can Tho",
        "city": "Ninh Kieu",
        "district": "Tan An",
        "address": "42 Hoa Binh",
    },
    {
        "state": "Hai Phong",
        "city": "Le Chan",
        "district": "Du Hang Kenh",
        "address": "101 To Hieu",
    },
    {
        "state": "Binh Duong",
        "city": "Thu Dau Mot",
        "district": "Phu Cuong",
        "address": "27 Hung Vuong",
    },
    {
        "state": "Dong Nai",
        "city": "Bien Hoa",
        "district": "Quang Vinh",
        "address": "64 Vo Thi Sau",
    },
    {
        "state": "Khanh Hoa",
        "city": "Nha Trang",
        "district": "Loc Tho",
        "address": "09 Tran Phu",
    },
]

UNIVERSITIES = [
    ("Ho Chi Minh City University of Technology", "HCMUT"),
    ("University of Information Technology", "UIT"),
    ("Foreign Trade University", "FTU"),
    ("Vietnam National University, Hanoi", "VNU"),
    ("Posts and Telecommunications Institute of Technology", "PTIT"),
    ("University of Economics Ho Chi Minh City", "UEH"),
    ("Da Nang University of Technology", "DUT"),
    ("FPT University", "FPTU"),
    ("Ton Duc Thang University", "TDTU"),
    ("RMIT Vietnam", "RMIT"),
    ("Can Tho University", "CTU"),
    ("Banking University of Ho Chi Minh City", "HUB"),
]

BLUEPRINTS = [
    {
        "key": "backend",
        "keywords": ["backend", "java", "spring", "api", "microservice", "it phần mềm"],
        "titles": ["Backend Engineer", "Java Developer", "Software Engineer", "Platform Engineer"],
        "department": "Engineering",
        "job_category": "TECHNOLOGY", # Sửa từ ENGINEER -> TECHNOLOGY
        "employment_type": "FULL_TIME",
        "experience_level": "MIDDLE",
        "education": "BACHELORS_DEGREE",
        "salary": (900, 2500),
        "skills": ["Java", "Spring Boot", "PostgreSQL", "Redis", "Docker"],
        "industries": ["TECHNOLOGY"], # Sửa lại cho chuẩn logic
        "work_types": ["FULL_TIME"],
    },
    {
        "key": "engineer",
        "keywords": ["engineer", "engineering", "systems", "developer", "software", "platform"],
        "titles": ["Engineer", "Software Engineer", "Systems Engineer", "Platform Engineer", "Embedded Engineer"],
        "department": "Engineering",
        # Use ENGINEER category for true engineering jobs in VN
        "job_category": "ENGINEER",
        "employment_type": "FULL_TIME",
        "experience_level": "MIDDLE",
        "education": "BACHELORS_DEGREE",
        "salary": (800, 2600),
        "skills": ["Problem Solving", "System Design", "Debugging", "CI/CD", "Testing"],
        "industries": ["TECHNOLOGY"],
        "work_types": ["FULL_TIME", "CONTRACT"],
    },
    {
        "key": "frontend",
        "keywords": ["frontend", "react", "ui", "javascript", "typescript", "it phần mềm"],
        "titles": ["Frontend Engineer", "React Developer", "UI Engineer", "Web Developer"],
        "department": "Product",
        "job_category": "TECHNOLOGY", # Sửa từ ENGINEER -> TECHNOLOGY
        "employment_type": "FULL_TIME",
        "experience_level": "JUNIOR",
        "education": "BACHELORS_DEGREE",
        "salary": (700, 2200),
        "skills": ["React", "TypeScript", "CSS", "Vite", "Testing Library"],
        "industries": ["TECHNOLOGY"], # Sửa từ ART_MUSIC -> TECHNOLOGY
        "work_types": ["FULL_TIME", "CONTRACT"],
    },
    {
        "key": "data",
        "keywords": ["data", "analytics", "etl", "warehouse", "bi"],
        "titles": ["Data Engineer", "Business Intelligence Analyst", "Data Analyst", "Analytics Engineer"],
        "department": "Data",
        "job_category": "TECHNOLOGY", # Data thuộc mảng công nghệ
        "employment_type": "FULL_TIME",
        "experience_level": "MIDDLE",
        "education": "BACHELORS_DEGREE",
        "salary": (800, 2400),
        "skills": ["Python", "SQL", "Airflow", "dbt", "BigQuery"],
        "industries": ["TECHNOLOGY", "BUSINESS"],
        "work_types": ["FULL_TIME", "REMOTE"],
    },
    {
        "key": "devops",
        "keywords": ["devops", "sre", "cloud", "kubernetes", "infrastructure"],
        "titles": ["DevOps Engineer", "Site Reliability Engineer", "Cloud Engineer", "Infrastructure Engineer"],
        "department": "Infrastructure",
        "job_category": "TECHNOLOGY",
        "employment_type": "FULL_TIME",
        "experience_level": "SENIOR",
        "education": "BACHELORS_DEGREE",
        "salary": (1200, 3200),
        "skills": ["AWS", "Docker", "Kubernetes", "Terraform", "Linux"],
        "industries": ["TECHNOLOGY"],
        "work_types": ["FULL_TIME", "CONTRACT"],
    },
    {
        "key": "mobile",
        "keywords": ["android", "ios", "mobile", "flutter", "react native"],
        "titles": ["Mobile Engineer", "Android Developer", "iOS Developer", "Flutter Developer"],
        "department": "Mobile",
        "job_category": "TECHNOLOGY",
        "employment_type": "FULL_TIME",
        "experience_level": "MIDDLE",
        "education": "BACHELORS_DEGREE",
        "salary": (750, 2300),
        "skills": ["Kotlin", "Swift", "Flutter", "Firebase", "REST API"],
        "industries": ["TECHNOLOGY"],
        "work_types": ["FULL_TIME"],
    },
    {
        "key": "qa",
        "keywords": ["qa", "tester", "quality", "automation", "test"],
        "titles": ["QA Engineer", "Automation Tester", "Quality Analyst", "Test Engineer"],
        "department": "Quality",
        "job_category": "TECHNOLOGY",
        "employment_type": "FULL_TIME",
        "experience_level": "JUNIOR",
        "education": "BACHELORS_DEGREE",
        "salary": (600, 1800),
        "skills": ["Selenium", "Cypress", "Postman", "SQL", "JMeter"],
        "industries": ["TECHNOLOGY"],
        "work_types": ["FULL_TIME"],
    },
    {
        "key": "product",
        "keywords": ["product", "owner", "business", "growth", "strategy"],
        "titles": ["Product Manager", "Product Owner", "Growth Manager", "Business Analyst"],
        "department": "Product",
        "job_category": "TECHNOLOGY", # Hoặc BUSINESS tùy bạn, nhưng PM ngành IT thì xếp vào TECHNOLOGY hợp lý hơn ở VN
        "employment_type": "FULL_TIME",
        "experience_level": "SENIOR",
        "education": "BACHELORS_DEGREE",
        "salary": (1000, 3000),
        "skills": ["Roadmapping", "SQL", "A/B Testing", "Research", "Agile"],
        "industries": ["TECHNOLOGY", "BUSINESS"],
        "work_types": ["FULL_TIME"],
    },
    {
        "key": "marketing",
        "keywords": ["marketing", "seo", "content", "brand", "digital"],
        "titles": ["Digital Marketing Executive", "Performance Marketing Specialist", "Content Strategist", "Brand Executive"],
        "department": "Marketing",
        "job_category": "MARKETING", # Sửa từ BUSINESS -> MARKETING
        "employment_type": "FULL_TIME",
        "experience_level": "MIDDLE",
        "education": "BACHELORS_DEGREE",
        "salary": (650, 1900),
        "skills": ["SEO", "Google Ads", "Meta Ads", "Analytics", "Copywriting"],
        "industries": ["MARKETING", "BUSINESS"],
        "work_types": ["FULL_TIME", "PART_TIME"],
    },
    {
        "key": "sales",
        "keywords": ["sales", "account", "business development", "bd", "customer"],
        "titles": ["Sales Executive", "Account Manager", "Business Development Specialist", "Customer Success Executive"],
        "department": "Commercial",
        "job_category": "SALES",
        "employment_type": "FULL_TIME",
        "experience_level": "MIDDLE",
        "education": "BACHELORS_DEGREE",
        "salary": (600, 2200),
        "skills": ["CRM", "Negotiation", "Presentation", "Pipeline Management", "Customer Care"],
        "industries": ["SALES", "BUSINESS"],
        "work_types": ["FULL_TIME"],
    },
    {
        "key": "finance",
        "keywords": ["finance", "accounting", "audit", "tax", "investment"],
        "titles": ["Financial Analyst", "Accountant", "Audit Associate", "Finance Executive"],
        "department": "Finance",
        "job_category": "FINANCE", # Sửa từ BUSINESS -> FINANCE
        "employment_type": "FULL_TIME",
        "experience_level": "MIDDLE",
        "education": "BACHELORS_DEGREE",
        "salary": (700, 2100),
        "skills": ["Excel", "Financial Modeling", "Accounting", "ERP", "Reporting"],
        "industries": ["FINANCE", "BUSINESS"],
        "work_types": ["FULL_TIME"],
    },
    {
        "key": "hr",
        "keywords": ["hr", "recruitment", "talent", "people", "human"],
        "titles": ["Talent Acquisition Specialist", "HR Business Partner", "Recruiter", "People Operations Executive"],
        "department": "People",
        "job_category": "HUMAN_RESOURCES", # Sửa từ ADMINISTRATION -> HUMAN_RESOURCES
        "employment_type": "FULL_TIME",
        "experience_level": "MIDDLE",
        "education": "BACHELORS_DEGREE",
        "salary": (650, 2000),
        "skills": ["Recruitment", "Interviewing", "Employer Branding", "HRIS", "Communication"],
        "industries": ["HUMAN_RESOURCES", "ADMINISTRATION"],
        "work_types": ["FULL_TIME"],
    },
    {
        "key": "design",
        "keywords": ["design", "ux", "ui", "graphic", "research"],
        "titles": ["Product Designer", "UX/UI Designer", "Graphic Designer", "UX Researcher"],
        "department": "Design",
        "job_category": "DESIGN", # Sửa từ ART_MUSIC -> DESIGN
        "employment_type": "FULL_TIME",
        "experience_level": "MIDDLE",
        "education": "BACHELORS_DEGREE",
        "salary": (650, 2000),
        "skills": ["Figma", "Design Systems", "Research", "Wireframing", "Prototyping"],
        "industries": ["DESIGN", "TECHNOLOGY"],
        "work_types": ["FULL_TIME", "CONTRACT"],
    }
]
REMOTE_ALLOWED_WORK_TYPES = {"REMOTE", "CONTRACT"}


@dataclass
class JobRecord:
    source_url: str
    title: str
    company_name: str
    location: str
    salary_range: str
    description: str
    requirements: list[str]
    benefits: list[str]


@dataclass
class CompanySeed:
    id: str
    account_id: str
    tagname: str
    avatar: str
    cover: str
    name: str
    website: str
    ceo_name: str
    description: str
    size: str
    no_of_members: int
    year_founded: int
    location: dict[str, str]
    email: str
    phone: str
    no_of_followers: int = 0
    no_of_following: int = 0
    no_of_connections: int = 0
    offer_before_trial: bool = True
    enable_offboarded_stage: bool = False


@dataclass
class JobSeed:
    id: str
    source_url: str
    source_key: str
    company_id: str
    title: str
    description: str
    department: str
    responsibilities: list[str]
    qualifications: list[str]
    minimum_qualifications: list[str]
    benefits: list[str]
    salary_range: str
    min_experience: int
    max_experience: int
    experience_level: str
    employment_type: str
    job_category: str
    education: str
    posted_date: str
    expiry_date: str
    number_of_positions: int
    contact_email: str
    contact_phone: str
    promotion_type: str
    state: str
    city: str
    district: str
    address: str
    remote_job: bool
    views: int = 0
    applicants: int = 0
    saved: int = 0
    liked: int = 0
    shared: int = 0
    resume: bool = True
    cover_letter: bool = True
    blueprint_key: str = "backend"


@dataclass
class CandidateSeed:
    id: str
    account_id: str
    tagname: str
    avatar: str
    cover: str
    first_name: str
    last_name: str
    email: str
    phone: str
    gender: str
    date_of_birth: str
    current_job_title: str
    desired_position: str
    current_company: str
    industry: str
    years_of_experience: int
    work_location: str
    is_open_to_work: bool
    summary: str
    salary_expectation_min: int
    salary_expectation_max: int
    education_level: str
    current_position: str
    industries: list[str]
    locations: list[str]
    work_types: list[str]
    resumes: list[str]
    location: dict[str, str]
    no_of_followers: int = 0
    no_of_following: int = 0
    no_of_connections: int = 0


@dataclass
class EducationSeed:
    id: str
    tagname: str
    official_name: str
    short_name: str
    website: str
    overview: str
    established_year: int
    accreditation: str


@dataclass
class ApplicationSeed:
    id: str
    candidate_id: str
    candidate_account_id: str
    job_id: str
    cover_letter: str
    resume_url: str
    rating: int | None
    notes: str
    applied_date: str
    current_stage: str
    stage_changed_at: str
    current_stage_note: str


@dataclass
class StageHistorySeed:
    id: str
    application_id: str
    from_stage: str | None
    to_stage: str
    note: str
    changed_by: str
    changed_at: str


@dataclass
class SavedJobSeed:
    id: str
    candidate_id: str
    job_id: str


@dataclass
class ConnectionSeed:
    id: str
    candidate_id: str
    connected_company_id: str
    note: str


@dataclass
class CandidateSkillSeed:
    id: str
    candidate_id: str
    skill_id: str
    proficiency_level: str
    years_of_experience: int
    is_verified: bool
    endorsed_by: str
    endorsement_count: int


@dataclass
class CandidateEducationSeed:
    id: str
    candidate_id: str
    education_id: str
    start_date: str
    end_date: str
    degree_title: str
    major: str
    description: str


@dataclass
class CandidateExperienceSeed:
    id: str
    candidate_id: str
    company_id: str
    start_date: str
    end_date: str | None
    salary: int
    job_title: str
    is_current: bool
    description: str


@dataclass
class SeedBundle:
    companies: list[CompanySeed]
    jobs: list[JobSeed]
    candidates: list[CandidateSeed]
    educations: list[EducationSeed]
    skills: list[dict[str, str]]
    applications: list[ApplicationSeed]
    stage_history: list[StageHistorySeed]
    saved_jobs: list[SavedJobSeed]
    connections: list[ConnectionSeed]
    candidate_skills: list[CandidateSkillSeed]
    candidate_educations: list[CandidateEducationSeed]
    candidate_experiences: list[CandidateExperienceSeed]


def slugify(value: str) -> str:
    value = strip_html(value).strip().lower()
    value = re.sub(r"\s+", "-", value)
    value = re.sub(r"[^a-z0-9\-]", "", value)
    value = re.sub(r"-+", "-", value).strip("-")
    return value[:80] or "unknown"


def sql_escape(value: str) -> str:
    return value.replace("'", "''")


def sql_text(value: str | None) -> str:
    if value is None:
        return "NULL"
    return f"'{sql_escape(value)}'"


def sql_bool(value: bool) -> str:
    return "true" if value else "false"


def json_text(value: object) -> str:
    return sql_text(json.dumps(value, ensure_ascii=True))


def make_uuid(prefix: str, key: str) -> str:
    return str(uuid.uuid5(uuid.NAMESPACE_DNS, f"careergraph:{prefix}:{key}"))


def normalize_company_name(value: str) -> str:
    cleaned = strip_html(value)
    cleaned = re.sub(r"\s+", " ", cleaned).strip(" -|")
    cleaned = remove_company_numeric_suffix(cleaned)
    if not cleaned or cleaned.lower() == "unknown company":
        return "Unknown Company"
    return cleaned[:160]


def remove_company_numeric_suffix(value: str) -> str:
    cleaned = value.strip(" -|,.;")
    previous = None
    while previous != cleaned:
        previous = cleaned
        cleaned = re.sub(r"(?:[\s\-_/|#.,]*(?:0\d{2,}|\d{3,})\b[\s\-_/|#.,]*)+$", "", cleaned)
        cleaned = cleaned.strip(" -|,.;")
    return cleaned


def clean_company_codes_in_description(value: str) -> str:
    cleaned = strip_html(value)
    cleaned = re.sub(r"\b([A-Z][A-Za-z]*(?:\s+[A-Z][A-Za-z]*){0,5})\s+(?:0\d{2,}|\d{3,})\b", r"\1", cleaned)
    cleaned = re.sub(r"\s+", " ", cleaned)
    return cleaned.strip()


def normalize_title(value: str) -> str:
    cleaned = strip_html(value)
    cleaned = re.sub(r"\s*\|\s*TopCV.*$", "", cleaned, flags=re.IGNORECASE)
    cleaned = re.sub(r"\s*-\s*TopCV.*$", "", cleaned, flags=re.IGNORECASE)
    cleaned = re.sub(r"\s+", " ", cleaned).strip(" -|")
    return cleaned or "Unknown title"


def normalize_location(value: str) -> str:
    cleaned = strip_html(value)
    cleaned = re.sub(r"\s+", " ", cleaned).strip(" ,-")
    return cleaned or "Ho Chi Minh"


def parse_json_ld_job(content: str) -> dict | None:
    pattern = re.compile(
        r"<script[^>]*type=['\"]application/ld\+json['\"][^>]*>(.*?)</script>",
        re.IGNORECASE | re.DOTALL,
    )
    for match in pattern.finditer(content):
        raw = match.group(1).strip()
        if not raw:
            continue
        try:
            payload = json.loads(raw)
        except json.JSONDecodeError:
            continue
        candidates = payload if isinstance(payload, list) else [payload]
        for item in candidates:
            if isinstance(item, dict) and item.get("@type") == "JobPosting":
                return item
    return None


def strip_html(value: str) -> str:
    if not value:
        return ""
    value = re.sub(r"<[^>]+>", " ", value)
    value = html.unescape(value)
    value = re.sub(r"\s+", " ", value)
    return value.strip()


def parse_salary_string(value: str, default_range: tuple[int, int]) -> str:
    if not value:
        return f"{default_range[0]}-{default_range[1]}"
    numbers = [int(part) for part in re.findall(r"\d+", value)]
    if len(numbers) >= 2:
        low, high = numbers[0], numbers[1]
        if low > high:
            low, high = high, low
        return f"{low}-{high}"
    if len(numbers) == 1:
        base = numbers[0]
        return f"{max(base - 200, 300)}-{base + 300}"
    return f"{default_range[0]}-{default_range[1]}"


def extract_job_links(listing_html: str) -> list[str]:
    links: set[str] = set()
    for href in re.findall(r"href=['\"]([^'\"]+)['\"]", listing_html, flags=re.IGNORECASE):
        href = href.strip()
        if not href:
            continue
        if href.startswith("/viec-lam/"):
            links.add("https://www.topcv.vn" + href)
        elif href.startswith("https://www.topcv.vn/viec-lam/"):
            links.add(href)
    return sorted(links)


def normalize_job_detail(job_url: str, timeout: int) -> JobRecord | None:
    response = requests.get(job_url, timeout=timeout, headers={"User-Agent": USER_AGENT})
    if response.status_code != 200:
        return None
    content = response.text
    data = parse_json_ld_job(content)
    if data:
        title = normalize_title(data.get("title") or "Unknown title")
        company = "Unknown Company"
        hiring_org = data.get("hiringOrganization")
        if isinstance(hiring_org, dict):
            company = normalize_company_name(hiring_org.get("name") or company)

        location = "Unknown location"
        job_location = data.get("jobLocation")
        if isinstance(job_location, dict):
            address = job_location.get("address")
            if isinstance(address, dict):
                location = normalize_location(
                    address.get("addressLocality")
                    or address.get("addressRegion")
                    or location
                )

        salary = "Negotiable"
        base_salary = data.get("baseSalary")
        if isinstance(base_salary, dict):
            value = base_salary.get("value")
            if isinstance(value, dict):
                min_v = value.get("minValue")
                max_v = value.get("maxValue")
                if min_v is not None and max_v is not None:
                    salary = f"{int(min_v)}-{int(max_v)}"

        description = strip_html(data.get("description") or "")[:2500]
        return JobRecord(
            source_url=job_url,
            title=title,
            company_name=company,
            location=location,
            salary_range=salary,
            description=description,
            requirements=[],
            benefits=[],
        )

    title_match = re.search(r"<title>(.*?)</title>", content, re.IGNORECASE | re.DOTALL)
    desc_match = re.search(
        r"<meta[^>]*name=['\"]description['\"][^>]*content=['\"](.*?)['\"][^>]*>",
        content,
        re.IGNORECASE | re.DOTALL,
    )
    return JobRecord(
        source_url=job_url,
        title=normalize_title(title_match.group(1)) if title_match else "Unknown title",
        company_name="Unknown Company",
        location="Unknown location",
        salary_range="Negotiable",
        description=strip_html(desc_match.group(1))[:2500] if desc_match else "",
        requirements=[],
        benefits=[],
    )


def crawl_topcv(
    listing_url: str,
    pages: int,
    per_page: int,
    delay: float,
    timeout: int,
) -> list[JobRecord]:
    session = requests.Session()
    session.headers.update({"User-Agent": USER_AGENT})
    all_links: list[str] = []
    for page in range(1, pages + 1):
        url = f"{listing_url}?page={page}"
        response = session.get(url, timeout=timeout)
        if response.status_code != 200:
            continue
        page_links = extract_job_links(response.text)
        if per_page > 0:
            page_links = page_links[:per_page]
        all_links.extend(page_links)
        time.sleep(delay)

    dedup_links = list(dict.fromkeys(all_links))
    records: list[JobRecord] = []
    for link in dedup_links:
        try:
            job = normalize_job_detail(link, timeout=timeout)
            if job is not None:
                records.append(job)
        except Exception:
            continue
        time.sleep(delay)
    return records


def infer_blueprint(title: str, description: str) -> dict[str, object]:
    content = f"{title} {description}".lower()
    best = BLUEPRINTS[0]
    best_score = -1
    for blueprint in BLUEPRINTS:
        score = sum(2 for keyword in blueprint["keywords"] if keyword in content)
        if blueprint["key"] in content:
            score += 1
        if score > best_score:
            best = blueprint
            best_score = score
    return best


def make_company_name(index: int) -> str:
    prefix = COMPANY_PREFIXES[index % len(COMPANY_PREFIXES)]
    suffix = COMPANY_SUFFIXES[(index // len(COMPANY_PREFIXES)) % len(COMPANY_SUFFIXES)]
    cycle = index // (len(COMPANY_PREFIXES) * len(COMPANY_SUFFIXES))
    if cycle == 0:
        return f"{prefix} {suffix}"
    series = COMPANY_SERIES[(cycle - 1) % len(COMPANY_SERIES)]
    return f"{prefix} {suffix} {series}"


def make_company_avatar(name: str) -> str:
    domain_index = uuid.uuid5(uuid.NAMESPACE_DNS, f"careergraph-logo:{name}").int % len(TRUSTED_COMPANY_LOGO_DOMAINS)
    domain = TRUSTED_COMPANY_LOGO_DOMAINS[domain_index]
    return f"https://www.google.com/s2/favicons?domain={quote_plus(domain)}&sz=256"


def make_seed_cover(seed: str, width: int = 1200, height: int = 320) -> str:
    return f"https://picsum.photos/seed/{quote_plus(seed)}/{width}/{height}"


def make_candidate_avatar(index: int) -> str:
    return f"https://i.pravatar.cc/300?img={(index % 70) + 1}"


def make_company_seed(name: str, index: int, rng: random.Random) -> CompanySeed:
    normalized_name = normalize_company_name(name)
    company_key = slugify(normalized_name) or f"company-{index:03d}"
    location = LOCATIONS[index % len(LOCATIONS)]
    return CompanySeed(
        id=make_uuid("company", company_key),
        account_id=make_uuid("account-company", company_key),
        tagname=company_key,
        avatar=make_company_avatar(normalized_name),
        cover=make_seed_cover(f"company-cover-{company_key}"),
        name=normalized_name,
        website=f"https://{company_key}.example.vn",
        ceo_name=f"{LAST_NAMES[index % len(LAST_NAMES)]} {FIRST_NAMES[(index * 3) % len(FIRST_NAMES)]}",
        description=(
            f"{normalized_name} is a curated TopCV-derived demo employer in the CareerGraph QA dataset, "
            "with hiring activity across engineering, business and operations roles."
        ),
        size=COMPANY_SIZES[index % len(COMPANY_SIZES)],
        no_of_members=40 + (index % 10) * 18,
        year_founded=2008 + (index % 15),
        location=location,
        email=f"hr+{company_key}@careergraph.demo",
        phone=f"+84-28-{1000 + index:04d}-{2000 + index:04d}",
        no_of_followers=0,
        no_of_following=0,
        no_of_connections=0,
        offer_before_trial=True,
        enable_offboarded_stage=False,
    )


def build_company_pool(records: list[JobRecord], target_companies: int, rng: random.Random) -> list[CompanySeed]:
    company_names: list[str] = []
    seen: set[str] = set()
    for record in records:
        normalized = normalize_company_name(record.company_name)
        if normalized not in seen and normalized != "Unknown Company":
            seen.add(normalized)
            company_names.append(normalized)
        if len(company_names) >= target_companies:
            break

    index = 0
    while len(company_names) < target_companies:
        synthetic = make_company_name(index)
        index += 1
        if synthetic not in seen:
            seen.add(synthetic)
            company_names.append(synthetic)

    rng.shuffle(company_names)
    return [make_company_seed(name, idx, rng) for idx, name in enumerate(company_names[:target_companies])]


def base_job_description(record: JobRecord, company: CompanySeed, blueprint: dict[str, object]) -> str:
    source_description = clean_company_codes_in_description(record.description)
    if source_description:
        return source_description[:2200]
    return (
        f"{company.name} is hiring for {record.title}. This position was curated from a TopCV crawl and "
        "normalized into a CareerGraph-ready profile with clean responsibilities, qualifications and realistic hiring metrics."
    )


def make_responsibilities(title: str, company_name: str, blueprint_key: str) -> list[str]:
    return [
        f"Own the delivery of {title.lower()} outcomes at {company_name}, coordinating with stakeholders to translate business needs into well-scoped execution plans and measurable results.",
        f"Collaborate across product, engineering and operations teams to improve delivery quality, documentation, observability and handoff discipline for the {blueprint_key} function.",
        "Continuously optimize workflows, remove recurring bottlenecks and contribute reusable standards that make the team faster, more predictable and easier to scale.",
    ]


def make_qualifications(blueprint: dict[str, object], years: int) -> list[str]:
    skills = blueprint["skills"]
    return [
        f"{years}+ years of relevant hands-on experience with {skills[0]}, {skills[1]} and adjacent tooling used in modern product teams.",
        "Strong communication, structured problem-solving and the ability to work effectively with cross-functional stakeholders under changing priorities.",
        "Comfort interpreting requirements, documenting trade-offs and maintaining high execution quality across day-to-day delivery.",
    ]


def make_minimum_qualifications(blueprint: dict[str, object]) -> list[str]:
    return [
        "Bachelor degree or equivalent practical experience.",
        f"Working familiarity with {', '.join(blueprint['skills'][:3])}.",
        "Able to work in a fast-moving, collaborative environment with clear ownership.",
    ]


def make_benefits(company_name: str) -> list[str]:
    return [
        f"Hybrid working model, private health coverage and a transparent growth path at {company_name}.",
        "Annual learning budget, modern equipment allowance and structured performance feedback every quarter.",
        "Team-based bonus, paid leave and support for professional certifications relevant to the role.",
    ]


def iter_source_records(records: list[JobRecord]) -> list[JobRecord]:
    deduped: dict[str, JobRecord] = {}
    for record in records:
        title = normalize_title(record.title)
        company = normalize_company_name(record.company_name)
        key = f"{title}|{company}|{slugify(record.source_url)}"
        if key not in deduped:
            deduped[key] = JobRecord(
                source_url=record.source_url,
                title=title,
                company_name=company,
                location=normalize_location(record.location),
                salary_range=record.salary_range,
                description=strip_html(record.description),
                requirements=record.requirements,
                benefits=record.benefits,
            )
    return list(deduped.values())


def build_jobs(
    records: list[JobRecord],
    companies: list[CompanySeed],
    target_jobs: int,
    rng: random.Random,
) -> list[JobSeed]:
    source_records = iter_source_records(records)
    if not source_records:
        source_records = [
            JobRecord(
                source_url="https://www.topcv.vn/viec-lam/synthetic-bootstrap",
                title=blueprint["titles"][0],
                company_name=companies[idx % len(companies)].name,
                location=companies[idx % len(companies)].location["state"],
                salary_range="Negotiable",
                description="Synthetic seed bootstrap record.",
                requirements=[],
                benefits=[],
            )
            for idx, blueprint in enumerate(BLUEPRINTS)
        ]

    companies_by_name = {company.name: company for company in companies}
    jobs: list[JobSeed] = []
    company_cursor = 0
    used_titles: set[str] = set()
    variant_words = ["Platform", "Growth", "Core", "Regional", "Digital", "Enterprise", "Automation", "Customer", "Insight", "Operations"]

    for index in range(target_jobs):
        record = source_records[index % len(source_records)]
        blueprint = infer_blueprint(record.title, record.description)

        if record.company_name in companies_by_name and index < len(source_records):
            company = companies_by_name[record.company_name]
        else:
            company = companies[company_cursor % len(companies)]
            company_cursor += 1

        location = company.location if index % 4 else LOCATIONS[index % len(LOCATIONS)]
        title = normalize_title(record.title)
        if index >= len(source_records):
            title = blueprint["titles"][(index // len(source_records)) % len(blueprint["titles"])]
            title = f"{title} ({variant_words[index % len(variant_words)]})"

        # Ensure generated titles are unique across the seed bundle to avoid
        # accidental duplication between different blueprints/categories.
        # If a title is already used, append a numeric suffix until unique.
        original_title = title
        if title in used_titles:
            suffix = 1
            candidate_title = f"{original_title} ({suffix})"
            while candidate_title in used_titles:
                suffix += 1
                candidate_title = f"{original_title} ({suffix})"
            title = candidate_title
        used_titles.add(title)

        min_experience = max(0, index % 5)
        max_experience = min_experience + 2 + (index % 4)
        salary_range = parse_salary_string(record.salary_range, blueprint["salary"])
        posted = (NOW.date() - timedelta(days=index % 75)).isoformat()
        expiry = (date.fromisoformat(posted) + timedelta(days=45 + (index % 20))).isoformat()
        source_key = f"{slugify(title)}-{slugify(company.name)}-{index:04d}"
        remote_job = (index + len(title)) % 5 == 0
        contact_email = company.email
        contact_phone = company.phone
        description = base_job_description(record, company, blueprint)
        jobs.append(
            JobSeed(
                id=make_uuid("job", source_key),
                source_url=record.source_url if index < len(source_records) else f"{record.source_url}?variant={index:04d}",
                source_key=source_key,
                company_id=company.id,
                title=title[:180],
                description=description,
                department=str(blueprint["department"]),
                responsibilities=make_responsibilities(title, company.name, str(blueprint["key"])),
                qualifications=make_qualifications(blueprint, min_experience + 1),
                minimum_qualifications=make_minimum_qualifications(blueprint),
                benefits=make_benefits(company.name),
                salary_range=salary_range,
                min_experience=min_experience,
                max_experience=max_experience,
                experience_level=str(blueprint["experience_level"]),
                employment_type=str(blueprint["employment_type"] if not remote_job else "FULL_TIME"),
                job_category=str(blueprint["job_category"]),
                education=str(blueprint["education"]),
                posted_date=posted,
                expiry_date=expiry,
                number_of_positions=1 + (index % 4),
                contact_email=contact_email,
                contact_phone=contact_phone,
                promotion_type="paid" if index % 9 == 0 else "free",
                state=location["state"],
                city=location["city"],
                district=location["district"],
                address=location["address"],
                remote_job=remote_job,
                blueprint_key=str(blueprint["key"]),
            )
        )

    return jobs


def build_candidates(
    target_candidates: int,
    companies: list[CompanySeed],
    rng: random.Random,
) -> list[CandidateSeed]:
    candidates: list[CandidateSeed] = []
    for index in range(target_candidates):
        first_name = FIRST_NAMES[index % len(FIRST_NAMES)]
        last_name = LAST_NAMES[(index * 2) % len(LAST_NAMES)]
        blueprint = BLUEPRINTS[index % len(BLUEPRINTS)]
        location = LOCATIONS[(index * 3) % len(LOCATIONS)]
        company = companies[(index * 5) % len(companies)]
        years = index % 8
        desired_position = blueprint["titles"][index % len(blueprint["titles"])]
        email = f"candidate.{index + 1:03d}@careergraph.demo"
        tagname = slugify(f"{first_name}-{last_name}-{index + 1:03d}")
        candidates.append(
            CandidateSeed(
                id=make_uuid("candidate", email),
                account_id=make_uuid("account-candidate", email),
                tagname=tagname,
                avatar=make_candidate_avatar(index),
                cover=make_seed_cover(f"candidate-cover-{index + 1:03d}"),
                first_name=first_name,
                last_name=last_name,
                email=email,
                phone=f"+84-90-{3000 + index:04d}-{1000 + index:04d}",
                gender="Male" if index % 2 == 0 else "Female",
                date_of_birth=(date(1990, 1, 1) + timedelta(days=index * 97)).isoformat(),
                current_job_title=desired_position,
                desired_position=desired_position,
                current_company=company.name,
                industry=str(blueprint["job_category"]),
                years_of_experience=years,
                work_location=location["state"],
                is_open_to_work=index % 7 != 0,
                summary=(
                    f"{first_name} {last_name} is part of the CareerGraph QA candidate pool, with focus on "
                    f"{desired_position.lower()} opportunities and consistent profile data for end-to-end testing."
                ),
                salary_expectation_min=500 + years * 120,
                salary_expectation_max=1100 + years * 220,
                education_level=str(blueprint["education"]),
                current_position=desired_position,
                industries=list(dict.fromkeys(str(item) for item in blueprint["industries"])),
                locations=[location["state"], LOCATIONS[(index + 2) % len(LOCATIONS)]["state"]],
                work_types=["FULL_TIME", "CONTRACT"] if index % 5 == 0 else ["FULL_TIME"],
                resumes=[f"/seed/resumes/candidate-{index + 1:03d}.pdf"],
                location=location,
            )
        )
    return candidates


def build_educations() -> list[EducationSeed]:
    educations: list[EducationSeed] = []
    for index, (official_name, short_name) in enumerate(UNIVERSITIES):
        educations.append(
            EducationSeed(
                id=make_uuid("education", official_name),
                tagname=slugify(short_name),
                official_name=official_name,
                short_name=short_name,
                website=f"https://{slugify(short_name)}.edu.vn",
                overview=f"{official_name} is included as a reusable education entity for the CareerGraph QA dataset.",
                established_year=1957 + index * 3,
                accreditation="MOET",
            )
        )
    return educations


def build_skill_catalog() -> list[dict[str, str]]:
    catalog: dict[str, dict[str, str]] = {}
    for blueprint in BLUEPRINTS:
        for skill in blueprint["skills"]:
            skill_id = make_uuid("skill", skill)
            catalog[skill] = {
                "id": skill_id,
                "name": skill,
                "category": str(blueprint["job_category"]),
                "description": f"Seed skill used by {blueprint['key']} candidates and jobs.",
            }
    return list(catalog.values())


def choose_candidate_jobs(
    candidate: CandidateSeed,
    jobs: list[JobSeed],
    candidate_index: int,
) -> list[JobSeed]:
    preferred = [job for job in jobs if job.job_category in candidate.industries or job.state in candidate.locations]
    fallback = preferred or jobs

    if candidate_index < 40:
        return []
    if candidate_index < 120:
        count = 1 + (candidate_index % 3)
    if candidate_index < 180:
        count = 4 + (candidate_index % 5)
    else:
        count = 8 + (candidate_index % 6)

    anchor_company = fallback[candidate_index % len(fallback)].company_id
    same_company_jobs = [job for job in fallback if job.company_id == anchor_company]
    chosen: list[JobSeed] = []

    if candidate_index >= 180 and same_company_jobs:
        for job in same_company_jobs[: min(4, len(same_company_jobs))]:
            chosen.append(job)

    start = (candidate_index * 7) % len(fallback)
    cursor = start
    while len(chosen) < count:
        job = fallback[cursor % len(fallback)]
        if job.id not in {item.id for item in chosen}:
            chosen.append(job)
        cursor += 1

    return chosen


def make_stage_path(target_stage: str) -> list[str]:
    canonical = [
        "APPLIED",
        "SCREENING",
        "HR_CONTACTED",
        "INTERVIEW",
        "INTERVIEW_COMPLETED",
        "OFFER_EXTENDED",
        "TRIAL",
        "HIRED",
    ]
    if target_stage in {"REJECTED", "WITHDRAWN"}:
        return ["APPLIED", "SCREENING", target_stage]
    if target_stage not in canonical:
        return ["APPLIED"]
    return canonical[: canonical.index(target_stage) + 1]


def build_relationships(
    candidates: list[CandidateSeed],
    companies: list[CompanySeed],
    jobs: list[JobSeed],
    educations: list[EducationSeed],
    skills: list[dict[str, str]],
    rng: random.Random,
) -> tuple[
    list[ApplicationSeed],
    list[StageHistorySeed],
    list[SavedJobSeed],
    list[ConnectionSeed],
    list[CandidateSkillSeed],
    list[CandidateEducationSeed],
    list[CandidateExperienceSeed],
]:
    applications: list[ApplicationSeed] = []
    stage_history: list[StageHistorySeed] = []
    saved_jobs: list[SavedJobSeed] = []
    connections: list[ConnectionSeed] = []
    candidate_skills: list[CandidateSkillSeed] = []
    candidate_educations: list[CandidateEducationSeed] = []
    candidate_experiences: list[CandidateExperienceSeed] = []

    skill_lookup = {item["name"]: item["id"] for item in skills}
    blueprint_by_key = {str(item["key"]): item for item in BLUEPRINTS}

    for index, candidate in enumerate(candidates):
        jobs_for_candidate = choose_candidate_jobs(candidate, jobs, index)
        stage_pool = [
            "APPLIED",
            "SCREENING",
            "HR_CONTACTED",
            "INTERVIEW",
            "INTERVIEW_COMPLETED",
            "OFFER_EXTENDED",
            "TRIAL",
            "REJECTED",
        ]

        for job_offset, job in enumerate(jobs_for_candidate):
            applied_at = NOW - timedelta(days=(index * 3 + job_offset) % 120, hours=(index + job_offset) % 9)
            target_stage = stage_pool[(index + job_offset) % len(stage_pool)]
            application_id = make_uuid("application", f"{candidate.id}:{job.id}")
            note = f"Candidate aligned with {job.title} requirements and entered {target_stage.lower().replace('_', ' ')} stage."
            applications.append(
                ApplicationSeed(
                    id=application_id,
                    candidate_id=candidate.id,
                    candidate_account_id=candidate.account_id,
                    job_id=job.id,
                    cover_letter=(
                        f"I am applying for {job.title} at {next(company.name for company in companies if company.id == job.company_id)} "
                        "because the role aligns with my current experience and long-term career direction."
                    ),
                    resume_url=candidate.resumes[0],
                    rating=None if target_stage in {"APPLIED", "SCREENING"} else 3 + ((index + job_offset) % 3),
                    notes=note,
                    applied_date=applied_at.date().isoformat(),
                    current_stage=target_stage,
                    stage_changed_at=applied_at.strftime("%Y-%m-%d %H:%M:%S"),
                    current_stage_note=note,
                )
            )

            path = make_stage_path(target_stage)
            for stage_index, stage in enumerate(path):
                changed_at = applied_at + timedelta(days=stage_index * 3)
                from_stage = path[stage_index - 1] if stage_index > 0 else None
                stage_history.append(
                    StageHistorySeed(
                        id=make_uuid("application-stage", f"{application_id}:{stage_index}"),
                        application_id=application_id,
                        from_stage=from_stage,
                        to_stage=stage,
                        note=f"Application moved to {stage.lower().replace('_', ' ')}.",
                        changed_by=candidate.account_id if stage_index == 0 else next(
                            company.account_id for company in companies if company.id == job.company_id
                        ),
                        changed_at=changed_at.strftime("%Y-%m-%d %H:%M:%S"),
                    )
                )

        saved_count = 2 + (index % 7)
        saved_source = [job for job in jobs if job.job_category in candidate.industries] or jobs
        cursor = (index * 11) % len(saved_source)
        chosen_saved: set[str] = set()
        while len(chosen_saved) < saved_count:
            job = saved_source[cursor % len(saved_source)]
            chosen_saved.add(job.id)
            cursor += 1
        for job_id in chosen_saved:
            saved_jobs.append(
                SavedJobSeed(
                    id=make_uuid("saved-job", f"{candidate.id}:{job_id}"),
                    candidate_id=candidate.id,
                    job_id=job_id,
                )
            )

        company_follow_count = index % 5
        for follow_offset in range(company_follow_count):
            company = companies[(index * 3 + follow_offset) % len(companies)]
            connections.append(
                ConnectionSeed(
                    id=make_uuid("connection", f"{candidate.id}:{company.id}"),
                    candidate_id=candidate.id,
                    connected_company_id=company.id,
                    note="Seeded company follow relationship for QA analytics.",
                )
            )

        preferred_blueprint = blueprint_by_key[BLUEPRINTS[index % len(BLUEPRINTS)]["key"]]
        for skill_offset, skill_name in enumerate(preferred_blueprint["skills"][:4]):
            candidate_skills.append(
                CandidateSkillSeed(
                    id=make_uuid("candidate-skill", f"{candidate.id}:{skill_name}"),
                    candidate_id=candidate.id,
                    skill_id=skill_lookup[skill_name],
                    proficiency_level=["Beginner", "Intermediate", "Advanced", "Expert"][skill_offset % 4],
                    years_of_experience=max(0, candidate.years_of_experience - 1 + skill_offset),
                    is_verified=skill_offset % 2 == 0,
                    endorsed_by="seed-system",
                    endorsement_count=1 + ((index + skill_offset) % 8),
                )
            )

        education = educations[index % len(educations)]
        grad_year = 2012 + (index % 10)
        candidate_educations.append(
            CandidateEducationSeed(
                id=make_uuid("candidate-education", candidate.id),
                candidate_id=candidate.id,
                education_id=education.id,
                start_date=f"{grad_year - 4}-09-01",
                end_date=f"{grad_year}-06-30",
                degree_title="Bachelor of Applied Science",
                major=candidate.desired_position,
                description="Seeded education record for candidate profile completeness.",
            )
        )

        current_company = companies[index % len(companies)]
        candidate_experiences.append(
            CandidateExperienceSeed(
                id=make_uuid("candidate-experience", candidate.id),
                candidate_id=candidate.id,
                company_id=current_company.id,
                start_date=f"{2018 + (index % 5)}-01-01",
                end_date=None if index % 4 else f"{2024 + (index % 2)}-12-31",
                salary=900 + candidate.years_of_experience * 150,
                job_title=candidate.current_job_title,
                is_current=index % 4 != 0,
                description="Seeded experience entry aligned with the candidate's current job title.",
            )
        )

    return (
        applications,
        stage_history,
        saved_jobs,
        connections,
        candidate_skills,
        candidate_educations,
        candidate_experiences,
    )


def sync_counts(
    companies: list[CompanySeed],
    jobs: list[JobSeed],
    candidates: list[CandidateSeed],
    applications: list[ApplicationSeed],
    saved_jobs: list[SavedJobSeed],
    connections: list[ConnectionSeed],
    candidate_experiences: list[CandidateExperienceSeed],
) -> None:
    applications_by_job = Counter(item.job_id for item in applications)
    saved_by_job = Counter(item.job_id for item in saved_jobs)
    follows_by_company = Counter(item.connected_company_id for item in connections)
    following_by_candidate = Counter(item.candidate_id for item in connections)
    experiences_by_company = Counter(item.company_id for item in candidate_experiences)

    for job in jobs:
        job.applicants = applications_by_job[job.id]
        job.saved = saved_by_job[job.id]
        job.views = 40 + job.applicants * 17 + job.saved * 11 + (abs(hash(job.id)) % 160)
        job.liked = max(0, min(job.saved, job.saved // 2 + job.applicants // 3))
        job.shared = max(0, job.views // 25)

    for company in companies:
        company.no_of_followers = follows_by_company[company.id]
        company.no_of_connections = company.no_of_followers
        company.no_of_members += experiences_by_company[company.id]

    for candidate in candidates:
        candidate.no_of_following = following_by_candidate[candidate.id]
        candidate.no_of_connections = candidate.no_of_following


def build_seed_bundle(
    records: list[JobRecord],
    target_jobs: int,
    target_companies: int,
    target_candidates: int,
) -> SeedBundle:
    rng = random.Random(RNG_SEED)
    companies = build_company_pool(records, target_companies, rng)
    jobs = build_jobs(records, companies, target_jobs, rng)
    candidates = build_candidates(target_candidates, companies, rng)
    educations = build_educations()
    skills = build_skill_catalog()
    (
        applications,
        stage_history,
        saved_jobs,
        connections,
        candidate_skills,
        candidate_educations,
        candidate_experiences,
    ) = build_relationships(candidates, companies, jobs, educations, skills, rng)
    sync_counts(companies, jobs, candidates, applications, saved_jobs, connections, candidate_experiences)
    return SeedBundle(
        companies=companies,
        jobs=jobs,
        candidates=candidates,
        educations=educations,
        skills=skills,
        applications=applications,
        stage_history=stage_history,
        saved_jobs=saved_jobs,
        connections=connections,
        candidate_skills=candidate_skills,
        candidate_educations=candidate_educations,
        candidate_experiences=candidate_experiences,
    )


def append_reset_block(sql_lines: list[str]) -> None:
    sql_lines.extend(
        [
            "-- Optional destructive reset for a clean QA dataset",
            "TRUNCATE TABLE application_stage_history, applications, saved_jobs, candidate_skill, candidate_experience, candidate_education, connections, company_recruitment_stages, jobs, accounts, contacts, addresses, candidates, companies, educations, skills, parties RESTART IDENTITY CASCADE;",
            "",
        ]
    )


def append_parties(sql_lines: list[str], bundle: SeedBundle) -> None:
    for company in bundle.companies:
        sql_lines.append(
            "INSERT INTO parties (id, party_type, tagname, avatar, cover, no_of_followers, no_of_following, no_of_connections, created_date, last_modified_date, status) "
            f"VALUES ({sql_text(company.id)}, 'COMPANY', {sql_text(company.tagname)}, {sql_text(company.avatar)}, {sql_text(company.cover)}, {company.no_of_followers}, {company.no_of_following}, {company.no_of_connections}, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'ACTIVE') "
            "ON CONFLICT (id) DO NOTHING;"
        )
    for candidate in bundle.candidates:
        sql_lines.append(
            "INSERT INTO parties (id, party_type, tagname, avatar, cover, no_of_followers, no_of_following, no_of_connections, created_date, last_modified_date, status) "
            f"VALUES ({sql_text(candidate.id)}, 'CANDIDATE', {sql_text(candidate.tagname)}, {sql_text(candidate.avatar)}, {sql_text(candidate.cover)}, {candidate.no_of_followers}, {candidate.no_of_following}, {candidate.no_of_connections}, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'ACTIVE') "
            "ON CONFLICT (id) DO NOTHING;"
        )
    for education in bundle.educations:
        sql_lines.append(
            "INSERT INTO parties (id, party_type, tagname, avatar, cover, no_of_followers, no_of_following, no_of_connections, created_date, last_modified_date, status) "
            f"VALUES ({sql_text(education.id)}, 'EDUCATION', {sql_text(education.tagname)}, '', '', 0, 0, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'ACTIVE') "
            "ON CONFLICT (id) DO NOTHING;"
        )
    sql_lines.append("")


def append_companies(sql_lines: list[str], bundle: SeedBundle) -> None:
    for company in bundle.companies:
        sql_lines.append(
            "INSERT INTO companies (id, size, name, website, ceo_name, description, no_of_members, year_founded, offer_before_trial, enable_offboarded_stage) "
            f"VALUES ({sql_text(company.id)}, {sql_text(company.size)}, {sql_text(company.name)}, {sql_text(company.website)}, {sql_text(company.ceo_name)}, {sql_text(company.description)}, {company.no_of_members}, {company.year_founded}, {sql_bool(company.offer_before_trial)}, {sql_bool(company.enable_offboarded_stage)}) "
            "ON CONFLICT (id) DO NOTHING;"
        )
    sql_lines.append("")


def append_candidates(sql_lines: list[str], bundle: SeedBundle) -> None:
    for candidate in bundle.candidates:
        sql_lines.append(
            "INSERT INTO candidates (id, first_name, last_name, date_of_birth, gender, current_job_title, desired_position, current_company, industry, years_of_experience, work_location, is_open_to_work, is_open_to_notify_new_job, summary, is_married, salary_expectation_min, salary_expectation_max, resumes, education_level, current_position, industries, locations, work_types) "
            f"VALUES ({sql_text(candidate.id)}, {sql_text(candidate.first_name)}, {sql_text(candidate.last_name)}, {sql_text(candidate.date_of_birth)}, {sql_text(candidate.gender)}, {sql_text(candidate.current_job_title)}, {sql_text(candidate.desired_position)}, {sql_text(candidate.current_company)}, {sql_text(candidate.industry)}, {candidate.years_of_experience}, {sql_text(candidate.work_location)}, {sql_bool(candidate.is_open_to_work)}, true, {sql_text(candidate.summary)}, false, {candidate.salary_expectation_min}, {candidate.salary_expectation_max}, {json_text(candidate.resumes)}, {sql_text(candidate.education_level)}, {sql_text(candidate.current_position)}, {json_text(candidate.industries)}, {json_text(candidate.locations)}, {json_text(candidate.work_types)}) "
            "ON CONFLICT (id) DO NOTHING;"
        )
    sql_lines.append("")


def append_educations(sql_lines: list[str], bundle: SeedBundle) -> None:
    for education in bundle.educations:
        sql_lines.append(
            "INSERT INTO educations (id, official_name, short_name, established_year, university_type, level, website, overview, accreditation) "
            f"VALUES ({sql_text(education.id)}, {sql_text(education.official_name)}, {sql_text(education.short_name)}, {education.established_year}, 'PUBLIC', 'UNIVERSITY', {sql_text(education.website)}, {sql_text(education.overview)}, {sql_text(education.accreditation)}) "
            "ON CONFLICT (id) DO NOTHING;"
        )
    sql_lines.append("")


def append_accounts(sql_lines: list[str], bundle: SeedBundle) -> None:
    for company in bundle.companies:
        sql_lines.append(
            "INSERT INTO accounts (id, email, password_hash, role, email_verified, company_id, created_date, last_modified_date, status) "
            f"VALUES ({sql_text(company.account_id)}, {sql_text(company.email)}, {sql_text(SEED_PASSWORD_HASH)}, 'HR', true, {sql_text(company.id)}, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'ACTIVE') "
            "ON CONFLICT (id) DO NOTHING;"
        )
    for candidate in bundle.candidates:
        sql_lines.append(
            "INSERT INTO accounts (id, email, password_hash, role, email_verified, candidate_id, created_date, last_modified_date, status) "
            f"VALUES ({sql_text(candidate.account_id)}, {sql_text(candidate.email)}, {sql_text(SEED_PASSWORD_HASH)}, 'USER', true, {sql_text(candidate.id)}, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'ACTIVE') "
            "ON CONFLICT (id) DO NOTHING;"
        )
    sql_lines.append("")


def append_contacts_and_addresses(sql_lines: list[str], bundle: SeedBundle) -> None:
    for company in bundle.companies:
        email_contact_id = make_uuid("contact", f"{company.id}:email")
        phone_contact_id = make_uuid("contact", f"{company.id}:phone")
        sql_lines.append(
            "INSERT INTO contacts (id, value, verified, is_primary, contact_type, party_id, created_date, last_modified_date, status) "
            f"VALUES ({sql_text(email_contact_id)}, {sql_text(company.email)}, true, true, 'EMAIL', {sql_text(company.id)}, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'ACTIVE') ON CONFLICT (id) DO NOTHING;"
        )
        sql_lines.append(
            "INSERT INTO contacts (id, value, verified, is_primary, contact_type, party_id, created_date, last_modified_date, status) "
            f"VALUES ({sql_text(phone_contact_id)}, {sql_text(company.phone)}, true, true, 'PHONE', {sql_text(company.id)}, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'ACTIVE') ON CONFLICT (id) DO NOTHING;"
        )
        address_id = make_uuid("address", company.id)
        sql_lines.append(
            "INSERT INTO addresses (id, name, country, province, district, ward, is_primary, address_type, party_id, created_date, last_modified_date, status) "
            f"VALUES ({sql_text(address_id)}, {sql_text(company.name + ' Headquarters')}, 'Vietnam', {sql_text(company.location['state'])}, {sql_text(company.location['city'])}, {sql_text(company.location['district'])}, true, 'HEADQUARTERS', {sql_text(company.id)}, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'ACTIVE') ON CONFLICT (id) DO NOTHING;"
        )
    for candidate in bundle.candidates:
        email_contact_id = make_uuid("contact", f"{candidate.id}:email")
        phone_contact_id = make_uuid("contact", f"{candidate.id}:phone")
        sql_lines.append(
            "INSERT INTO contacts (id, value, verified, is_primary, contact_type, party_id, created_date, last_modified_date, status) "
            f"VALUES ({sql_text(email_contact_id)}, {sql_text(candidate.email)}, true, true, 'EMAIL', {sql_text(candidate.id)}, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'ACTIVE') ON CONFLICT (id) DO NOTHING;"
        )
        sql_lines.append(
            "INSERT INTO contacts (id, value, verified, is_primary, contact_type, party_id, created_date, last_modified_date, status) "
            f"VALUES ({sql_text(phone_contact_id)}, {sql_text(candidate.phone)}, true, true, 'PHONE', {sql_text(candidate.id)}, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'ACTIVE') ON CONFLICT (id) DO NOTHING;"
        )
        address_id = make_uuid("address", candidate.id)
        sql_lines.append(
            "INSERT INTO addresses (id, name, country, province, district, ward, is_primary, address_type, party_id, created_date, last_modified_date, status) "
            f"VALUES ({sql_text(address_id)}, {sql_text(candidate.first_name + ' ' + candidate.last_name)}, 'Vietnam', {sql_text(candidate.location['state'])}, {sql_text(candidate.location['city'])}, {sql_text(candidate.location['district'])}, true, 'HOME_ADDRESS', {sql_text(candidate.id)}, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'ACTIVE') ON CONFLICT (id) DO NOTHING;"
        )
    sql_lines.append("")


def append_company_recruitment_stages(sql_lines: list[str], bundle: SeedBundle) -> None:
    default_stages = [
        "APPLIED",
        "SCREENING",
        "HR_CONTACTED",
        "INTERVIEW",
        "INTERVIEW_COMPLETED",
        "OFFER_EXTENDED",
        "TRIAL",
        "HIRED",
        "OFFBOARDED",
        "REJECTED",
    ]
    for company in bundle.companies:
        for order, stage in enumerate(default_stages, start=1):
            status = "INACTIVE" if stage == "OFFBOARDED" and not company.enable_offboarded_stage else "ACTIVE"
            stage_id = make_uuid("company-stage", f"{company.id}:{stage}")
            sql_lines.append(
                "INSERT INTO company_recruitment_stages (id, company_id, stage, display_order, created_date, last_modified_date, status) "
                f"VALUES ({sql_text(stage_id)}, {sql_text(company.id)}, '{stage}', {order}, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '{status}') "
                "ON CONFLICT (id) DO NOTHING;"
            )
    sql_lines.append("")


def append_skills(sql_lines: list[str], bundle: SeedBundle) -> None:
    for skill in bundle.skills:
        sql_lines.append(
            "INSERT INTO skills (id, name, category, description, created_date, last_modified_date, status) "
            f"VALUES ({sql_text(skill['id'])}, {sql_text(skill['name'])}, {sql_text(skill['category'])}, {sql_text(skill['description'])}, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'ACTIVE') "
            "ON CONFLICT (id) DO NOTHING;"
        )
    sql_lines.append("")


def append_candidate_profile_details(sql_lines: list[str], bundle: SeedBundle) -> None:
    for item in bundle.candidate_skills:
        sql_lines.append(
            "INSERT INTO candidate_skill (id, proficiency_level, years_of_experience, is_verified, endorsed_by, endorsement_date, endorsement_count, candidate_id, skill_id, created_date, last_modified_date, status) "
            f"VALUES ({sql_text(item.id)}, {sql_text(item.proficiency_level)}, {item.years_of_experience}, {sql_bool(item.is_verified)}, {sql_text(item.endorsed_by)}, NULL, {item.endorsement_count}, {sql_text(item.candidate_id)}, {sql_text(item.skill_id)}, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'ACTIVE') "
            "ON CONFLICT (id) DO NOTHING;"
        )
    for item in bundle.candidate_educations:
        sql_lines.append(
            "INSERT INTO candidate_education (id, start_date, end_date, degree_title, major, is_current, description, candidate_id, education_id, created_date, last_modified_date, status) "
            f"VALUES ({sql_text(item.id)}, {sql_text(item.start_date)}, {sql_text(item.end_date)}, {sql_text(item.degree_title)}, {sql_text(item.major)}, false, {sql_text(item.description)}, {sql_text(item.candidate_id)}, {sql_text(item.education_id)}, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'ACTIVE') "
            "ON CONFLICT (id) DO NOTHING;"
        )
    for item in bundle.candidate_experiences:
        sql_lines.append(
            "INSERT INTO candidate_experience (id, start_date, end_date, salary, job_title, is_current, description, candidate_id, company_id, created_date, last_modified_date, status) "
            f"VALUES ({sql_text(item.id)}, {sql_text(item.start_date)}, {sql_text(item.end_date)}, {item.salary}, {sql_text(item.job_title)}, {sql_bool(item.is_current)}, {sql_text(item.description)}, {sql_text(item.candidate_id)}, {sql_text(item.company_id)}, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'ACTIVE') "
            "ON CONFLICT (id) DO NOTHING;"
        )
    sql_lines.append("")


def append_connections(sql_lines: list[str], bundle: SeedBundle) -> None:
    for connection in bundle.connections:
        sql_lines.append(
            "INSERT INTO connections (id, note, connection_type, has_seen, disable_notification, candidate_id, connected_company_id, created_date, last_modified_date, status) "
            f"VALUES ({sql_text(connection.id)}, {sql_text(connection.note)}, 'FOLLOWED', true, false, {sql_text(connection.candidate_id)}, {sql_text(connection.connected_company_id)}, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'ACTIVE') "
            "ON CONFLICT (id) DO NOTHING;"
        )
    sql_lines.append("")


def append_jobs(sql_lines: list[str], bundle: SeedBundle) -> None:
    for job in bundle.jobs:
        sql_lines.append(
            "INSERT INTO jobs (id, title, description, department, responsibilities, qualifications, minimum_qualifications, benefits, salary_range, min_experience, max_experience, experience_level, employment_type, job_category, education, posted_date, expiry_date, number_of_positions, contact_email, contact_phone, promotion_type, state, city, district, address, remote_job, views, applicants, saved, liked, shared, resume, cover_letter, created_date, last_modified_date, status, company_id) "
            f"VALUES ({sql_text(job.id)}, {sql_text(job.title)}, {sql_text(job.description)}, {sql_text(job.department)}, {json_text(job.responsibilities)}, {json_text(job.qualifications)}, {json_text(job.minimum_qualifications)}, {json_text(job.benefits)}, {sql_text(job.salary_range)}, {job.min_experience}, {job.max_experience}, '{job.experience_level}', '{job.employment_type}', '{job.job_category}', '{job.education}', {sql_text(job.posted_date)}, {sql_text(job.expiry_date)}, {job.number_of_positions}, {sql_text(job.contact_email)}, {sql_text(job.contact_phone)}, {sql_text(job.promotion_type)}, {sql_text(job.state)}, {sql_text(job.city)}, {sql_text(job.district)}, {sql_text(job.address)}, {sql_bool(job.remote_job)}, {job.views}, {job.applicants}, {job.saved}, {job.liked}, {job.shared}, {sql_bool(job.resume)}, {sql_bool(job.cover_letter)}, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'ACTIVE', {sql_text(job.company_id)}) "
            "ON CONFLICT (id) DO NOTHING;"
        )
    sql_lines.append("")


def append_applications(sql_lines: list[str], bundle: SeedBundle) -> None:
    for application in bundle.applications:
        sql_lines.append(
            "INSERT INTO applications (id, cover_letter, resume_url, rating, notes, applied_date, current_stage, stage_changed_at, current_stage_note, candidate_id, job_id, created_date, last_modified_date, status) "
            f"VALUES ({sql_text(application.id)}, {sql_text(application.cover_letter)}, {sql_text(application.resume_url)}, {str(application.rating) if application.rating is not None else 'NULL'}, {sql_text(application.notes)}, {sql_text(application.applied_date)}, '{application.current_stage}', {sql_text(application.stage_changed_at)}, {sql_text(application.current_stage_note)}, {sql_text(application.candidate_id)}, {sql_text(application.job_id)}, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'ACTIVE') "
            "ON CONFLICT (id) DO NOTHING;"
        )
    sql_lines.append("")


def append_stage_history(sql_lines: list[str], bundle: SeedBundle) -> None:
    for history in bundle.stage_history:
        sql_lines.append(
            "INSERT INTO application_stage_history (id, application_id, from_stage, to_stage, note, changed_by, changed_at, created_date, last_modified_date, status) "
            f"VALUES ({sql_text(history.id)}, {sql_text(history.application_id)}, {sql_text(history.from_stage)}, '{history.to_stage}', {sql_text(history.note)}, {sql_text(history.changed_by)}, {sql_text(history.changed_at)}, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'ACTIVE') "
            "ON CONFLICT (id) DO NOTHING;"
        )
    sql_lines.append("")


def append_saved_jobs(sql_lines: list[str], bundle: SeedBundle) -> None:
    for saved_job in bundle.saved_jobs:
        sql_lines.append(
            "INSERT INTO saved_jobs (id, candidate_id, job_id, created_date, last_modified_date, status) "
            f"VALUES ({sql_text(saved_job.id)}, {sql_text(saved_job.candidate_id)}, {sql_text(saved_job.job_id)}, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'ACTIVE') "
            "ON CONFLICT (id) DO NOTHING;"
        )
    sql_lines.append("")


def to_sql(
    records: Iterable[JobRecord],
    target_jobs: int = DEFAULT_TARGET_JOBS,
    target_companies: int = DEFAULT_TARGET_COMPANIES,
    target_candidates: int = DEFAULT_TARGET_CANDIDATES,
    reset: bool = False,
) -> str:
    bundle = build_seed_bundle(list(records), target_jobs, target_companies, target_candidates)
    sql_lines: list[str] = [
        "-- Auto-generated from TopCV crawler",
        "-- CareerGraph schema-compatible QA seed",
        f"-- jobs={len(bundle.jobs)}, companies={len(bundle.companies)}, candidates={len(bundle.candidates)}, applications={len(bundle.applications)}",
        "BEGIN;",
        "",
    ]

    if reset:
        append_reset_block(sql_lines)

    append_parties(sql_lines, bundle)
    append_companies(sql_lines, bundle)
    append_candidates(sql_lines, bundle)
    append_educations(sql_lines, bundle)
    append_accounts(sql_lines, bundle)
    append_contacts_and_addresses(sql_lines, bundle)
    append_company_recruitment_stages(sql_lines, bundle)
    append_skills(sql_lines, bundle)
    append_candidate_profile_details(sql_lines, bundle)
    append_connections(sql_lines, bundle)
    append_jobs(sql_lines, bundle)
    append_applications(sql_lines, bundle)
    append_stage_history(sql_lines, bundle)
    append_saved_jobs(sql_lines, bundle)

    sql_lines.append("COMMIT;")
    sql_lines.append("")
    sql_lines.append("-- Seed credentials")
    sql_lines.append(f"-- password: {SEED_PASSWORD}")
    sql_lines.append(f"-- sample HR account: {bundle.companies[0].email}")
    sql_lines.append(f"-- sample candidate account: {bundle.candidates[0].email}")
    sql_lines.append("")
    return "\n".join(sql_lines)


def save_json(records: list[JobRecord], output_json: Path) -> None:
    output_json.parent.mkdir(parents=True, exist_ok=True)
    output_json.write_text(
        json.dumps([asdict(record) for record in records], indent=2, ensure_ascii=True),
        encoding="utf-8",
    )


def load_json(input_json: Path) -> list[JobRecord]:
    payload = json.loads(input_json.read_text(encoding="utf-8"))
    return [JobRecord(**item) for item in payload]


def build_arg_parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser(description="TopCV crawler + CareerGraph seed generator")
    sub = parser.add_subparsers(dest="cmd", required=True)

    crawl_cmd = sub.add_parser("crawl", help="Crawl TopCV jobs and save JSON")
    crawl_cmd.add_argument("--listing-url", default=DEFAULT_LISTING_URL)
    crawl_cmd.add_argument("--pages", type=int, default=1)
    crawl_cmd.add_argument("--per-page", type=int, default=20)
    crawl_cmd.add_argument("--delay", type=float, default=0.8)
    crawl_cmd.add_argument("--timeout", type=int, default=20)
    crawl_cmd.add_argument("--output-json", default=DEFAULT_OUTPUT_JSON)

    sql_cmd = sub.add_parser("to-sql", help="Generate full CareerGraph SQL seed from crawled JSON")
    sql_cmd.add_argument("--input-json", default=DEFAULT_OUTPUT_JSON)
    sql_cmd.add_argument("--output-sql", default=DEFAULT_OUTPUT_SQL)
    sql_cmd.add_argument("--target-jobs", type=int, default=DEFAULT_TARGET_JOBS)
    sql_cmd.add_argument("--target-companies", type=int, default=DEFAULT_TARGET_COMPANIES)
    sql_cmd.add_argument("--target-candidates", type=int, default=DEFAULT_TARGET_CANDIDATES)
    sql_cmd.add_argument("--reset", action="store_true")

    crawl_sql_cmd = sub.add_parser("crawl-to-sql", help="Crawl TopCV and directly build the full SQL seed")
    crawl_sql_cmd.add_argument("--listing-url", default=DEFAULT_LISTING_URL)
    crawl_sql_cmd.add_argument("--pages", type=int, default=3)
    crawl_sql_cmd.add_argument("--per-page", type=int, default=30)
    crawl_sql_cmd.add_argument("--delay", type=float, default=0.8)
    crawl_sql_cmd.add_argument("--timeout", type=int, default=20)
    crawl_sql_cmd.add_argument("--output-json", default=DEFAULT_OUTPUT_JSON)
    crawl_sql_cmd.add_argument("--output-sql", default=DEFAULT_OUTPUT_SQL)
    crawl_sql_cmd.add_argument("--target-jobs", type=int, default=DEFAULT_TARGET_JOBS)
    crawl_sql_cmd.add_argument("--target-companies", type=int, default=DEFAULT_TARGET_COMPANIES)
    crawl_sql_cmd.add_argument("--target-candidates", type=int, default=DEFAULT_TARGET_CANDIDATES)
    crawl_sql_cmd.add_argument("--reset", action="store_true")

    return parser


def main() -> None:
    parser = build_arg_parser()
    args = parser.parse_args()

    if args.cmd == "crawl":
        records = crawl_topcv(
            listing_url=args.listing_url,
            pages=args.pages,
            per_page=args.per_page,
            delay=args.delay,
            timeout=args.timeout,
        )
        output_json = Path(args.output_json)
        save_json(records, output_json)
        print(f"Saved {len(records)} records to JSON: {output_json}")
        return

    if args.cmd == "to-sql":
        records = load_json(Path(args.input_json))
        sql_content = to_sql(
            records,
            target_jobs=args.target_jobs,
            target_companies=args.target_companies,
            target_candidates=args.target_candidates,
            reset=args.reset,
        )
        output_sql = Path(args.output_sql)
        output_sql.parent.mkdir(parents=True, exist_ok=True)
        output_sql.write_text(sql_content, encoding="utf-8")
        print(
            "Generated schema-compatible SQL seed: "
            f"jobs~{args.target_jobs}, companies~{args.target_companies}, candidates~{args.target_candidates} -> {output_sql}"
        )
        return

    if args.cmd == "crawl-to-sql":
        records = crawl_topcv(
            listing_url=args.listing_url,
            pages=args.pages,
            per_page=args.per_page,
            delay=args.delay,
            timeout=args.timeout,
        )
        output_json = Path(args.output_json)
        save_json(records, output_json)
        sql_content = to_sql(
            records,
            target_jobs=args.target_jobs,
            target_companies=args.target_companies,
            target_candidates=args.target_candidates,
            reset=args.reset,
        )
        output_sql = Path(args.output_sql)
        output_sql.parent.mkdir(parents=True, exist_ok=True)
        output_sql.write_text(sql_content, encoding="utf-8")
        print(f"Saved {len(records)} crawled records to JSON: {output_json}")
        print(
            "Generated schema-compatible SQL seed: "
            f"jobs~{args.target_jobs}, companies~{args.target_companies}, candidates~{args.target_candidates} -> {output_sql}"
        )
        return


if __name__ == "__main__":
    main()
