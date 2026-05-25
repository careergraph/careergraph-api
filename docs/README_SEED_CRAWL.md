# CareerGraph TopCV Seed Guide

This guide explains how to generate and load a clean QA dataset for CareerGraph from the TopCV crawler script.

The new generator does more than insert `companies` and `jobs`.
It builds a backend-compatible graph with real rows for:

- `parties`
- `companies`
- `candidates`
- `educations`
- `accounts`
- `contacts`
- `addresses`
- `company_recruitment_stages`
- `skills`
- `candidate_skill`
- `candidate_education`
- `candidate_experience`
- `connections` for company follows
- `jobs`
- `applications`
- `application_stage_history`
- `saved_jobs`

That means the counters shown in the UI are backed by actual database rows, not only by static numbers in `jobs`.

## 1. What the generator produces

Default output profile:

- about `1000` jobs
- about `100` companies
- about `200` candidates
- many categories, locations and experience levels
- candidates with mixed behavior:
  - no application
  - one application
  - multiple applications
  - multiple applications to the same company
- company followers stored through `connections`
- job applicants stored through `applications`
- saved jobs stored through `saved_jobs`
- BCrypt password for all seeded accounts

Default password for all seeded accounts:

```text
12345678
```

## 2. Important compatibility notes

### Passwords

The backend uses BCrypt.
Do not insert plain text into `accounts.password_hash`.
The updated generator already writes a valid BCrypt hash for `12345678`.

### Follower and applicant counters

- Company follow counts are reflected in `parties.no_of_followers` and backed by `connections` rows.
- Job applicant counts are reflected in `jobs.applicants` and backed by `applications` rows.
- Saved counts are reflected in `jobs.saved` and backed by `saved_jobs` rows.

### Reset versus additive import

You have 2 safe modes:

1. Clean rebuild: run SQL with `--reset`.
2. Additive import: run SQL without `--reset` and rely on deterministic IDs plus `ON CONFLICT DO NOTHING`.

For QA and UI verification, `--reset` is recommended.

## 3. Prerequisites

From `careergraph-api`:



```bash
python3 -m venv .venv
source .venv/bin/activate

pip install -U pip
pip install -r scripts/requirements-crawler.txt
```

If you run the full stack by Docker, make sure the database container is up first.

## 4. Generate SQL from TopCV data

### Option A. Crawl and directly generate SQL

```bash
python3 scripts/topcv_crawler.py crawl-to-sql \
  --pages 10 \
  --per-page 30 \
  --target-jobs 1000 \
  --target-companies 100 \
  --target-candidates 200 \
  --output-json scripts/output/topcv_jobs.json \
  --output-sql init-scripts/topcv-import.sql \
  --reset
```

Use this when outbound access to TopCV is available.

### Option B. Generate SQL from an existing crawled JSON file

```bash
python3 scripts/topcv_crawler.py to-sql \
  --input-json scripts/output/topcv_jobs.json \
  --output-sql init-scripts/topcv-import.sql \
  --target-jobs 1000 \
  --target-companies 100 \
  --target-candidates 200 \
  --reset
```

Use this when you already have a JSON snapshot or want reproducible runs without hitting TopCV again.

## 5. Load data into PostgreSQL

### Option A. Import into the running Docker database

```bash
docker exec -i careergraph-postgres psql -U postgres -d careergraph < init-scripts/topcv-import.sql
```

### Option B. Use the generated file as startup seed

Set:

```env
APP_SEED_ENABLED=true
APP_SEED_SCRIPT_PATH=init-scripts/topcv-import.sql
APP_SEED_CHECK_TABLE=jobs
APP_SEED_MIN_ROW_COUNT=1
```

Then restart the API.

This is useful for a fresh environment only.

## 6. Recreate the database from scratch

If you want a completely new database instead of truncating with `--reset`, use one of these approaches.

### Docker volume reset

```bash
docker compose down
rm -rf data/postgres
docker compose up -d careergraph-db
```

Then restart the API and import the generated SQL again.

### New database name

Change the DB name in `.env`:

```env
POSTGRES_DB=careergraph_seed_qa
```

Then restart the database and API containers.

## 7. Recommended QA flow

### Step 1. Start infra

```bash
docker compose up -d careergraph-db careergraph-redis careergraph-elasticsearch
```

### Step 2. Generate seed SQL

```bash
python3 scripts/topcv_crawler.py to-sql \
  --input-json scripts/output/topcv_jobs.json \
  --output-sql init-scripts/topcv-import.sql \
  --target-jobs 1000 \
  --target-companies 100 \
  --target-candidates 200 \
  --reset
```

### Step 3. Import

```bash
docker exec -i careergraph-postgres psql -U postgres -d careergraph < init-scripts/topcv-import.sql
```

### Step 4. Run API

```bash
./mvnw spring-boot:run
```

### Step 5. Run frontends

Client:

```bash
cd ../careergraph-client
npm install
npm run dev
```

HR:

```bash
cd ../careergraph-hr
npm install
npm run dev
```

## 8. Sync Elasticsearch manually by batch

Startup sync is now disabled by environment flags in local development.
Use the internal endpoint when you want to sync jobs or candidates on demand, for example from cron.

### Endpoint

```text
POST /careergraph/api/v1/internal/elasticsearch/sync
```

Required header:

```text
x-internal-api-key: <your internal api key>
```

Do not hardcode the key into this document, shell history, or committed scripts.
Load it from an environment variable or a secret manager instead.

### Supported query params

- `target=all|jobs|candidates`
- `jobBatchSize=<number>`
- `candidateBatchSize=<number>`
- `force=true|false`
- `forceJobs=true|false`
- `forceCandidates=true|false`

### Example: sync jobs only

```bash
export INTERNAL_API_KEY='replace-at-runtime'

curl -X POST \
  'http://localhost:8010/careergraph/api/v1/internal/elasticsearch/sync?target=jobs&jobBatchSize=25' \
  -H "x-internal-api-key: ${INTERNAL_API_KEY}"
```

### Example: sync candidates only

```bash
export INTERNAL_API_KEY='replace-at-runtime'

curl -X POST \
  'http://localhost:8010/careergraph/api/v1/internal/elasticsearch/sync?target=candidates&candidateBatchSize=15' \
  -H "x-internal-api-key: ${INTERNAL_API_KEY}"
```

### Example: sync both in one request

```bash
export INTERNAL_API_KEY='replace-at-runtime'

curl -X POST \
  'http://localhost:8010/careergraph/api/v1/internal/elasticsearch/sync?target=all&jobBatchSize=25&candidateBatchSize=15' \
  -H "x-internal-api-key: ${INTERNAL_API_KEY}"
```

### Example cron setup

Store the secret outside git first, for example in `/etc/careergraph/elasticsearch-sync.env`:

```bash
INTERNAL_API_KEY=replace-at-runtime
```

Then load it inside cron:

```cron
*/15 * * * * . /etc/careergraph/elasticsearch-sync.env && curl -s -X POST 'http://localhost:8010/careergraph/api/v1/internal/elasticsearch/sync?target=jobs&jobBatchSize=25' -H "x-internal-api-key: ${INTERNAL_API_KEY}" >/dev/null
*/20 * * * * . /etc/careergraph/elasticsearch-sync.env && curl -s -X POST 'http://localhost:8010/careergraph/api/v1/internal/elasticsearch/sync?target=candidates&candidateBatchSize=15' -H "x-internal-api-key: ${INTERNAL_API_KEY}" >/dev/null
```

### Notes

- Keep `APP_ES_SYNC_JOBS_ENABLED=false` and `APP_ES_SYNC_CANDIDATES_ENABLED=false` if cron or another scheduler will trigger sync.
- Use small batch sizes when the embedding backend is rate-limited.
- Use `force=true` only when you intentionally want to rebuild changed documents in the selected target.

## 9. Login accounts

All seeded accounts share the same password:

```text
12345678
```

The generator appends sample accounts at the bottom of the SQL file as comments:

```sql
-- sample HR account: ...
-- sample candidate account: ...
```

After generating `init-scripts/topcv-import.sql`, use those two accounts for quick login checks.

## 10. Quick verification SQL

After import, run these checks:

```sql
SELECT COUNT(*) AS companies FROM companies;
SELECT COUNT(*) AS jobs FROM jobs;
SELECT COUNT(*) AS candidates FROM candidates;
SELECT COUNT(*) AS applications FROM applications;
SELECT COUNT(*) AS saved_jobs FROM saved_jobs;
SELECT COUNT(*) AS follows FROM connections WHERE connection_type = 'FOLLOWED';
```

Consistency checks:

```sql
SELECT COUNT(*) AS mismatched_jobs
FROM jobs j
WHERE j.applicants <> (
  SELECT COUNT(*) FROM applications a WHERE a.job_id = j.id
);

SELECT COUNT(*) AS mismatched_saved
FROM jobs j
WHERE j.saved <> (
  SELECT COUNT(*) FROM saved_jobs s WHERE s.job_id = j.id
);

SELECT COUNT(*) AS mismatched_company_followers
FROM companies c
JOIN parties p ON p.id = c.id
WHERE p.no_of_followers <> (
  SELECT COUNT(*) FROM connections x
  WHERE x.connected_company_id = c.id AND x.connection_type = 'FOLLOWED'
);
```

Expected result for the 3 mismatch checks:

```text
0
```

## 10. UI verification checklist

### Candidate client

- login succeeds with seeded candidate account
- jobs page shows many jobs and multiple categories
- saved jobs page has data for candidates that saved jobs
- applied jobs page has data for candidates with applications
- job detail shows non-empty company info, salary, benefits, qualifications

### HR app

- login succeeds with seeded HR account
- dashboard loads without empty-state failure
- jobs grid shows many jobs for the company account
- kanban/application views show stage data
- candidate suggestion/profile views show skills, experience and education

## 11. Practical notes

- If TopCV crawling is blocked or slow, keep a local JSON snapshot and run `to-sql` from that file.
- The synthetic part of the dataset is deterministic, so repeated generation with the same targets produces stable IDs.
- If you need a smaller smoke dataset, lower the target flags instead of editing SQL manually.