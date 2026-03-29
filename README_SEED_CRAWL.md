# CareerGraph API - Seed Data and TopCV Crawl Guide

This guide provides a production-oriented setup for:
- Automatic base seed insertion when running Docker Compose.
- Flexible data loading with either static SQL or crawler-generated SQL.

## 1) What was added

1. `init-scripts/seed-basic.sql`
- Idempotent SQL base data for:
  - Company
  - Candidates
  - Education
  - Contacts
  - Addresses
  - Skills
  - Candidate skills
  - Candidate education/experience
  - Jobs
  - Applications + stage history
  - Basic connection

2. `DataSeeder` behavior update
- Reads configurable properties under `app.seed.*`.
- Runs seed only if `check-table` has less than `min-row-count`.
- Supports script path resolution for both local and Docker runtime.

3. `scripts/topcv_crawler.py`
- Crawl TopCV listings and job detail pages.
- Export normalized JSON.
- Generate SQL import file (`init-scripts/topcv-import.sql`) for CareerGraph.

## 2) Seed configuration

In `.env` / `.env.dev` / `.env.prod`:

```env
APP_SEED_ENABLED=true
APP_SEED_SCRIPT_PATH=init-scripts/seed-basic.sql
APP_SEED_CHECK_TABLE=jobs
APP_SEED_MIN_ROW_COUNT=1
```

Meaning:
- `APP_SEED_ENABLED`: enable/disable seeding on app startup.
- `APP_SEED_SCRIPT_PATH`: SQL file path to execute.
- `APP_SEED_CHECK_TABLE`: table used to decide whether DB is empty enough.
- `APP_SEED_MIN_ROW_COUNT`: if count is below this value, seed runs.

Production recommendation:
- Keep `APP_SEED_ENABLED=false` in stable production environments.
- Enable only in controlled provisioning or QA environments.

## 3) Auto seed when running Docker Compose

### Prerequisites
- Docker + Docker Compose installed.
- `.env` present in `careergraph-api`.

### Run

```bash
docker compose up -d --build
```

Runtime flow:
1. PostgreSQL/Redis/Elasticsearch start.
2. API starts.
3. Hibernate updates schema (`ddl-auto=update`).
4. `DataSeeder` checks `jobs` table count.
5. If empty, executes SQL script from `APP_SEED_SCRIPT_PATH`.

## 4) Use static SQL manually (optional)

You can still run SQL manually if needed:

```bash
docker exec -i careergraph-postgres psql -U postgres -d careergraph < init-scripts/seed-basic.sql
```

Or set custom script for startup:

```env
APP_SEED_SCRIPT_PATH=init-scripts/mock-data.sql
```

## 5) Crawl TopCV and generate SQL

## 5.1 Install crawler dependencies

From `careergraph-api`:

```bash
pip install -r scripts/requirements-crawler.txt
```

## 5.2 Crawl to JSON

```bash
python scripts/topcv_crawler.py crawl --pages 2 --per-page 20 --output-json scripts/output/topcv_jobs.json
```

## 5.3 Generate SQL from JSON

```bash
python scripts/topcv_crawler.py to-sql --input-json scripts/output/topcv_jobs.json --output-sql init-scripts/topcv-import.sql
```

## 5.4 One-step crawl + SQL

```bash
python scripts/topcv_crawler.py crawl-to-sql --pages 2 --per-page 20 --output-sql init-scripts/topcv-import.sql
```

## 5.5 Use crawler SQL at startup

```env
APP_SEED_SCRIPT_PATH=init-scripts/topcv-import.sql
APP_SEED_ENABLED=true
```

Then restart API container.

## 6) Operational recommendations (production mindset)

1. Keep base seed small and deterministic.
- `seed-basic.sql` should represent essential baseline only.

2. Keep crawler output separate from baseline.
- Use generated SQL (`topcv-import.sql`) as additive data.

3. Use idempotent insert statements.
- Keep `ON CONFLICT DO NOTHING` for safe re-runs.

4. Log and monitor seed execution.
- Verify startup logs include script resolution and row counts.

5. Respect source website policies.
- Crawler should be rate-limited and legally compliant.

## 7) Suggested workflow for your project

1. Local development baseline:
- Use `init-scripts/seed-basic.sql`.

2. Build richer dataset:
- Run crawler -> generate `topcv-import.sql`.
- Merge curated records into seed scripts if needed.

3. CI/QA environments:
- Enable seed with controlled script path.

4. Production:
- Disable auto seed by default.
- Execute curated migration/seed scripts explicitly.
