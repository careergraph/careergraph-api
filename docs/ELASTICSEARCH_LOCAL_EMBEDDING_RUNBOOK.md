# Elasticsearch Sync and Local Embedding Runbook

## 1. Scope

This document describes the hardened Elasticsearch synchronization flow for jobs and candidates, the local embedding architecture in `careergraph-ai`, the retry controls added to avoid wasteful Gemini quota consumption, the validation that was run locally, and infrastructure sizing guidance for VPS deployment.

## 2. What was changed

### 2.1 Retry behavior for 429 / quota errors

Two retry layers are now controlled:

1. Application-level sync retry
   - `ElasticsearchDataInitializer`
   - `CandidateElasticsearchDataInitializer`
   - Both now detect rate-limit style failures (`429`, `quota`, `rate limit`) and stop retrying the same sync batch.

2. Spring AI client retry
   - `spring.ai.retry.max-attempts=1`
   - `spring.ai.retry.on-client-errors=false`
   - `spring.ai.retry.exclude-on-http-codes=429`
   - `spring.ai.retry.backoff.initial-interval=1ms`
   - `spring.ai.retry.backoff.max-interval=2ms`
   - `spring.ai.retry.backoff.multiplier=2`

Effectively, if Gemini fallback returns `429`, the backend will fail fast instead of generating extra requests from hidden client retry logic.

### 2.2 Local embedding is now the primary path

The backend now prefers the local FastAPI embedding service first.

- Java client: `HuggingFaceEmbeddingServiceImpl`
- Runtime orchestration: `EmbedServiceImpl`
- FastAPI endpoint: `POST /embed`

Gemini is only used as fallback when explicitly allowed by environment configuration.

The backend provider order is now configurable:

- `APP_EMBED_USE_LOCAL_FIRST=true`: local FastAPI `/embed` is primary
- `APP_EMBED_USE_LOCAL_FIRST=false`: Spring AI / Gemini is primary

For chat-side job matching inside `careergraph-ai`, provider selection is also configurable:

- `JOB_MATCHING_USE_LOCAL_EMBEDDING_STACK=true`: use the same local HF stack as `/embed`
- `JOB_MATCHING_USE_LOCAL_EMBEDDING_STACK=false`: keep using the legacy `EmbeddingsHelper` stack

### 2.3 Local embedding dimension compatibility

The local Hugging Face model is:

- `sentence-transformers/paraphrase-multilingual-MiniLM-L12-v2`

Native output dimension of this model is `384`, while the current Elasticsearch mapping expects `3072`.

To keep the current Java and Elasticsearch schema unchanged, `careergraph-ai` now:

1. generates native `384`-dimension embeddings,
2. normalizes them,
3. expands them deterministically to `3072` dimensions.

This keeps the local vector space internally consistent and compatible with the current `dense_vector` mapping.

Important note:

- This is schema-compatible with the existing Elasticsearch index design.
- It is not semantically identical to Gemini embeddings.
- It is the lightest practical local path currently integrated in this repository without introducing a significantly heavier model footprint.

### 2.4 Self-healing index recreation

If an old Elasticsearch index still has `embedding.dims=384`, the sync layer now detects the dense-vector dimension mismatch, recreates the index, and retries once.

This was added to both job and candidate synchronization flows.

## 3. Active endpoints and execution modes

### 3.1 Local AI service

- `GET /health`
- `POST /embed`

Request payload for `/embed`:

```json
{
  "inputs": [
    "java spring boot backend",
    "react frontend developer"
  ]
}
```

Response shape:

```json
{
  "embeddings": [[...]],
  "dimensions": 3072,
  "count": 2,
  "model": "sentence-transformers/paraphrase-multilingual-MiniLM-L12-v2",
  "model_name": "sentence-transformers/paraphrase-multilingual-MiniLM-L12-v2"
}
```

### 3.2 Manual Elasticsearch sync endpoint

Endpoint:

- `POST /careergraph/api/v1/internal/elasticsearch/sync`

Supported targets:

- `jobs`
- `candidates`
- `all`

The endpoint remains protected by `x-internal-api-key`. Do not place the real key inside docs, scripts committed to git, or screenshots.

### 3.3 Scheduled sync

The scheduler still supports the existing fixed-delay model:

- jobs: every 2 minutes
- candidates: every 4 minutes

Recommended environment flags:

```env
APP_ES_CRON_ENABLED=true
APP_ES_CRON_JOBS_ENABLED=true
APP_ES_CRON_CANDIDATES_ENABLED=true
APP_ES_CRON_JOBS_FIXED_DELAY_MS=120000
APP_ES_CRON_CANDIDATES_FIXED_DELAY_MS=240000
APP_ES_CRON_JOBS_BATCH_SIZE=25
APP_ES_CRON_CANDIDATES_BATCH_SIZE=10
APP_EMBED_USE_LOCAL_FIRST=true
APP_ES_ALLOW_GEMINI_FALLBACK=false
```

Recommended production principle:

- If local AI is available and stable, keep Gemini fallback off.
- Only enable Gemini fallback when there is a clear operational reason.
- If fallback is enabled, keep batch sizes conservative.

## 4. Environment variables that now matter

### 4.1 Backend retry and embedding variables

```env
APP_EMBED_USE_LOCAL_FIRST=true
APP_EMBED_ALLOW_GEMINI_FALLBACK=false
SPRING_AI_RETRY_MAX_ATTEMPTS=1
SPRING_AI_RETRY_ON_CLIENT_ERRORS=false
SPRING_AI_RETRY_EXCLUDE_ON_HTTP_CODES=429
SPRING_AI_RETRY_BACKOFF_INITIAL_INTERVAL=1ms
SPRING_AI_RETRY_BACKOFF_MAX_INTERVAL=2ms
SPRING_AI_RETRY_BACKOFF_MULTIPLIER=2
EMBEDDING_SERVICE_EXPECTED_DIMENSIONS=3072
```

### 4.2 AI service variables

The FastAPI service supports output dimension control through:

```env
HF_EMBEDDING_OUTPUT_DIMS=3072
JOB_MATCHING_USE_LOCAL_EMBEDDING_STACK=true
```

Backward compatibility with the older dimension variable is retained in code, but the output dimension used by the current backend should remain `3072` unless the Elasticsearch mappings are redesigned.

Recommended alignment:

- `APP_EMBED_USE_LOCAL_FIRST=true`
- `JOB_MATCHING_USE_LOCAL_EMBEDDING_STACK=true`

This reduces dependency on two parallel embedding stacks between API sync/search and AI chat job matching.

## 5. Validation that was run locally

### 5.1 AI service validation

The local AI service was started successfully and verified.

Observed results:

- `GET /health` returned:
  - `status=up`
  - `model=sentence-transformers/paraphrase-multilingual-MiniLM-L12-v2`
  - `dimensions=3072`
  - `native_dimensions=384`
- `POST /embed` returned vectors with:
  - `count=2`
  - `dimensions=3072`
  - first vector length `3072`

### 5.2 Backend validation

The backend configuration was validated after correcting Spring Retry builder constraints.

Validated results:

- backend health endpoint returned `{"status":"UP"}`
- manual job sync returned success with batch size `1`
- manual candidate sync returned success with batch size `1`

Observed sync responses:

- jobs:
  - `indexed=1`
  - `unchanged=351`
  - `pending=648`
- candidates:
  - `indexed=1`
  - `unchanged=0`
  - `pending=199`

### 5.3 Previous compatibility validation

Before the retry hardening was finalized, the following were also verified during implementation:

- local job sync works end-to-end with local embeddings
- local candidate sync works end-to-end with local embeddings
- candidate index dimension mismatch can be healed by index recreation logic

## 6. Deployment sizing guidance

## 6.1 Measured local AI footprint

Observed on the current machine:

- Python virtual environment: about `7.5 GB`
- Hugging Face cache: about `587 MB`
- running AI process RSS: about `1.24 GB`

This means the AI service alone should not be sized as a tiny sidecar.

### 6.2 Recommended AI-only sizing

Minimum acceptable for development or light internal use:

- `2 vCPU`
- `4 GB RAM`
- `15 GB SSD`

Recommended for stable use:

- `2 to 4 vCPU`
- `6 to 8 GB RAM`
- `20 to 30 GB SSD`

Reasoning:

- model files and Python environment already consume meaningful disk,
- runtime memory for the embedding service is above 1 GB before adding OS, logs, Docker overhead, and burst tolerance,
- leaving less than 2.5 to 3 GB effective headroom for AI usually causes unstable behavior during restarts, upgrades, or concurrent embedding bursts.

### 6.3 Recommended full-stack single-VPS sizing

If one VPS hosts all of these together:

- API
- PostgreSQL
- Elasticsearch
- Redis
- RTC
- AI service
- uploaded data and logs

Recommended sizing for a realistic non-demo deployment:

- `8 vCPU`
- `24 GB RAM`
- `200 GB NVMe SSD`

Minimum practical baseline for internal testing or UAT:

- `6 vCPU`
- `16 GB RAM`
- `120 GB SSD`

Reasoning:

- Elasticsearch is the most memory-sensitive service in the stack.
- PostgreSQL and API will compete with Elasticsearch and AI for RAM during indexing and search.
- Redis and RTC are lighter, but still add baseline memory and operational overhead.
- SSD use grows from container layers, model cache, PostgreSQL volume, Elasticsearch indices, uploads, and logs.

### 6.4 When to move beyond a single VPS

Move AI or Elasticsearch to separate nodes when one of these becomes true:

- Elasticsearch data size grows beyond a few GB and search latency matters.
- embedding throughput increases above occasional batch sync.
- you need higher reliability during reindexing or seed import.
- you want zero-downtime operations instead of a single-node compromise.

## 7. Operational recommendations

1. Prefer local AI as the default embedding provider.
2. Keep Gemini fallback disabled unless it is explicitly needed.
3. Keep `SPRING_AI_RETRY_MAX_ATTEMPTS=1` and exclude `429` from retry.
4. Keep batch sizes small enough to avoid burst load on embeddings.
5. If you change Elasticsearch vector dimensions in the future, redesign both the Java mapping and AI output together instead of relying on compatibility expansion.

## 8. Quick conclusion

Current status after this hardening pass:

- local embedding endpoint exists and is compatible with the backend,
- output dimension is now compatible with the current Elasticsearch mapping,
- job and candidate sync both work with local embeddings,
- hidden client retry on Gemini `429` is disabled through Spring AI retry configuration,
- scheduler and manual sync modes remain available.