#!/usr/bin/env python3
"""
TopCV crawler and SQL generator for CareerGraph.

Use cases:
1) Crawl TopCV job links and store raw normalized data to JSON.
2) Convert crawled JSON to idempotent SQL (companies + jobs).

Notes:
- Respect TopCV terms of use and robots policy.
- For production scale, replace this with official partner/API data source.
"""

from __future__ import annotations

import argparse
import json
import html
import re
import time
import uuid
from dataclasses import dataclass, asdict
from pathlib import Path
from typing import Iterable

import requests

USER_AGENT = (
    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) "
    "AppleWebKit/537.36 (KHTML, like Gecko) "
    "Chrome/124.0.0.0 Safari/537.36"
)

DEFAULT_LISTING_URL = "https://www.topcv.vn/tim-viec-lam"


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


def slugify(value: str) -> str:
    value = value.strip().lower()
    value = re.sub(r"\s+", "-", value)
    value = re.sub(r"[^a-z0-9\-]", "", value)
    return value[:80] or "unknown"


def sql_escape(value: str) -> str:
    return value.replace("'", "''")


def parse_json_ld_job(html: str) -> dict | None:
    pattern = re.compile(
        r"<script[^>]*type=['\"]application/ld\+json['\"][^>]*>(.*?)</script>",
        re.IGNORECASE | re.DOTALL,
    )
    for match in pattern.finditer(html):
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


def normalize_job_detail(job_url: str, timeout: int) -> JobRecord | None:
    resp = requests.get(job_url, timeout=timeout, headers={"User-Agent": USER_AGENT})
    if resp.status_code != 200:
        return None

    html = resp.text
    data = parse_json_ld_job(html)

    if data:
        title = data.get("title") or "Unknown title"
        company = "Unknown company"
        hiring_org = data.get("hiringOrganization")
        if isinstance(hiring_org, dict):
            company = hiring_org.get("name") or company

        location = "Unknown location"
        job_location = data.get("jobLocation")
        if isinstance(job_location, dict):
            address = job_location.get("address")
            if isinstance(address, dict):
                location = (
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
                    salary = f"{min_v}-{max_v}"

        description = data.get("description") or ""
        description = strip_html(description)

        return JobRecord(
            source_url=job_url,
            title=title.strip(),
            company_name=company.strip(),
            location=location.strip(),
            salary_range=salary.strip(),
            description=description.strip()[:2500],
            requirements=[],
            benefits=[],
        )

    # Fallback parse by meta tags/title
    title_match = re.search(r"<title>(.*?)</title>", html, re.IGNORECASE | re.DOTALL)
    title = strip_html(title_match.group(1)) if title_match else "Unknown title"

    desc_match = re.search(
        r"<meta[^>]*name=['\"]description['\"][^>]*content=['\"](.*?)['\"][^>]*>",
        html,
        re.IGNORECASE | re.DOTALL,
    )
    description = strip_html(desc_match.group(1)) if desc_match else ""

    return JobRecord(
        source_url=job_url,
        title=title,
        company_name="Unknown company",
        location="Unknown location",
        salary_range="Negotiable",
        description=description[:2500],
        requirements=[],
        benefits=[],
    )


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


def strip_html(value: str) -> str:
    if not value:
        return ""
    value = re.sub(r"<[^>]+>", " ", value)
    value = html.unescape(value)
    value = re.sub(r"\s+", " ", value)
    return value.strip()


def crawl_topcv(listing_url: str, pages: int, per_page: int, delay: float, timeout: int) -> list[JobRecord]:
    session = requests.Session()
    session.headers.update({"User-Agent": USER_AGENT})

    all_links: list[str] = []
    for page in range(1, pages + 1):
        url = f"{listing_url}?page={page}"
        resp = session.get(url, timeout=timeout)
        if resp.status_code != 200:
            continue

        page_links = extract_job_links(resp.text)
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


def to_sql(records: Iterable[JobRecord]) -> str:
    records = list(records)
    now_expr = "CURRENT_TIMESTAMP"

    company_ids: dict[str, str] = {}
    sql_lines: list[str] = []

    sql_lines.append("-- Auto-generated from TopCV crawler")
    sql_lines.append("-- Idempotent SQL for CareerGraph")
    sql_lines.append("")

    # parties + companies
    for r in records:
        normalized_company_name = (r.company_name or "Unknown company").strip() or "Unknown company"
        company_key = slugify(normalized_company_name)
        if company_key in company_ids:
            continue

        company_uuid = str(uuid.uuid5(uuid.NAMESPACE_DNS, f"topcv-company:{company_key}"))
        company_ids[company_key] = company_uuid
        party_tag = slugify(normalized_company_name)

        sql_lines.append(
            "INSERT INTO parties (id, party_type, tagname, avatar, cover, no_of_followers, no_of_following, no_of_connections, created_date, last_modified_date, status) "
            f"VALUES ('{company_uuid}', 'Company', '{sql_escape(party_tag)}', '', '', 0, 0, 0, {now_expr}, {now_expr}, 'ACTIVE') "
            "ON CONFLICT (id) DO NOTHING;"
        )

        sql_lines.append(
            "INSERT INTO companies (id, size, name, website, ceo_name, no_of_members, year_founded) "
            f"VALUES ('{company_uuid}', '11-50', '{sql_escape(normalized_company_name)}', NULL, NULL, 0, NULL) "
            "ON CONFLICT (id) DO NOTHING;"
        )

    sql_lines.append("")

    # jobs
    for idx, r in enumerate(records, start=1):
        job_uuid = str(uuid.uuid5(uuid.NAMESPACE_DNS, f"topcv-job:{r.source_url}"))
        company_uuid = company_ids[slugify((r.company_name or "Unknown company").strip() or "Unknown company")]
        normalized_location = (r.location or "Unknown location").strip() or "Unknown location"

        responsibilities = json.dumps([
            "Analyze requirement and deliver assigned tasks.",
            "Collaborate with team members and stakeholders.",
            "Maintain code quality and documentation.",
        ], ensure_ascii=True)

        qualifications = json.dumps([
            "Relevant professional experience.",
            "Good communication and teamwork skills.",
            "Ability to adapt in fast-paced environment.",
        ], ensure_ascii=True)

        minimum_qualifications = json.dumps([
            "Bachelor degree or equivalent practical experience."
        ], ensure_ascii=True)

        benefits = json.dumps(r.benefits or ["Competitive compensation", "Insurance and allowance"], ensure_ascii=True)

        sql_lines.append(
            "INSERT INTO jobs ("
            "id, title, description, department, responsibilities, qualifications, minimum_qualifications, benefits, "
            "salary_range, min_experience, max_experience, experience_level, employment_type, job_category, education, "
            "posted_date, expiry_date, number_of_positions, contact_email, contact_phone, promotion_type, state, city, district, address, "
            "remote_job, views, applicants, saved, liked, shared, resume, cover_letter, created_date, last_modified_date, status, company_id"
            ") VALUES ("
            f"'{job_uuid}', '{sql_escape(r.title)}', '{sql_escape(r.description or 'Sourced from TopCV')}', 'Engineering', "
            f"'{sql_escape(responsibilities)}', '{sql_escape(qualifications)}', '{sql_escape(minimum_qualifications)}', '{sql_escape(benefits)}', "
            f"'{sql_escape(r.salary_range or 'Negotiable')}', 1, 5, 'MIDDLE', 'FULL_TIME', 'ENGINEER', 'BACHELORS_DEGREE', "
            "CURRENT_DATE::text, (CURRENT_DATE + INTERVAL '60 day')::text, 1, 'hr@careergraph.vn', NULL, 'free', "
            f"'{sql_escape(normalized_location)}', '{sql_escape(normalized_location)}', NULL, '{sql_escape(r.source_url)}', "
            "true, 0, 0, 0, 0, 0, true, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'ACTIVE', "
            f"'{company_uuid}'"
            ") ON CONFLICT (id) DO NOTHING;"
        )

        if idx % 50 == 0:
            sql_lines.append("")

    return "\n".join(sql_lines) + "\n"


def save_json(records: list[JobRecord], output_json: Path) -> None:
    output_json.parent.mkdir(parents=True, exist_ok=True)
    output_json.write_text(
        json.dumps([asdict(r) for r in records], indent=2, ensure_ascii=True),
        encoding="utf-8",
    )


def load_json(input_json: Path) -> list[JobRecord]:
    payload = json.loads(input_json.read_text(encoding="utf-8"))
    return [JobRecord(**item) for item in payload]


def build_arg_parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser(description="TopCV crawler + SQL generator")
    sub = parser.add_subparsers(dest="cmd", required=True)

    crawl_cmd = sub.add_parser("crawl", help="Crawl TopCV jobs and save JSON")
    crawl_cmd.add_argument("--listing-url", default=DEFAULT_LISTING_URL)
    crawl_cmd.add_argument("--pages", type=int, default=1)
    crawl_cmd.add_argument("--per-page", type=int, default=20)
    crawl_cmd.add_argument("--delay", type=float, default=0.8)
    crawl_cmd.add_argument("--timeout", type=int, default=20)
    crawl_cmd.add_argument("--output-json", default="scripts/output/topcv_jobs.json")

    sql_cmd = sub.add_parser("to-sql", help="Generate SQL from crawled JSON")
    sql_cmd.add_argument("--input-json", default="scripts/output/topcv_jobs.json")
    sql_cmd.add_argument("--output-sql", default="init-scripts/topcv-import.sql")

    crawl_sql_cmd = sub.add_parser("crawl-to-sql", help="Crawl and directly output SQL")
    crawl_sql_cmd.add_argument("--listing-url", default=DEFAULT_LISTING_URL)
    crawl_sql_cmd.add_argument("--pages", type=int, default=1)
    crawl_sql_cmd.add_argument("--per-page", type=int, default=20)
    crawl_sql_cmd.add_argument("--delay", type=float, default=0.8)
    crawl_sql_cmd.add_argument("--timeout", type=int, default=20)
    crawl_sql_cmd.add_argument("--output-json", default="scripts/output/topcv_jobs.json")
    crawl_sql_cmd.add_argument("--output-sql", default="init-scripts/topcv-import.sql")

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
        input_json = Path(args.input_json)
        records = load_json(input_json)
        sql_content = to_sql(records)
        output_sql = Path(args.output_sql)
        output_sql.parent.mkdir(parents=True, exist_ok=True)
        output_sql.write_text(sql_content, encoding="utf-8")
        print(f"Generated SQL with {len(records)} jobs: {output_sql}")
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

        sql_content = to_sql(records)
        output_sql = Path(args.output_sql)
        output_sql.parent.mkdir(parents=True, exist_ok=True)
        output_sql.write_text(sql_content, encoding="utf-8")

        print(f"Saved {len(records)} records to JSON: {output_json}")
        print(f"Generated SQL with {len(records)} jobs: {output_sql}")
        return


if __name__ == "__main__":
    main()
