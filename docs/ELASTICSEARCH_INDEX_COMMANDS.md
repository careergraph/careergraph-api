# Elasticsearch Index Commands

Tài liệu này hướng dẫn các lệnh để:

- kiểm tra cấu hình index của `jobs_es` và `candidates_es`
- xóa index
- tạo lại index
- bơm lại dữ liệu sau khi tạo lại
```bash
curl -sS "http://localhost:9200/jobs_es/_mapping?pretty"
curl -sS "http://localhost:9200/candidates_es/_mapping?pretty"

curl -X DELETE -sS "http://localhost:9200/jobs_es?pretty"
curl -X DELETE -sS "http://localhost:9200/candidates_es?pretty"
```

```
curl -X POST "http://localhost:8010/careergraph/api/v1/internal/elasticsearch/sync?target=all&jobBatchSize=100&candidateBatchSize=50&force=true" \
  -H "x-internal-api-key: YOUR_INTERNAL_API_KEY" \
  -H 'Accept: application/json'
```
## 1. Tên index hiện tại

Theo code backend:

- Job index: `jobs_es`
- Candidate index: `candidates_es`

Tham chiếu:

- `JobES` dùng `@Document(indexName = "jobs_es")`
- `CandidateES` dùng `@Document(indexName = "candidates_es")`

## 2. Biến môi trường nên set trước khi chạy lệnh

```bash
export ES_URL="http://localhost:9200"
export API_URL="http://localhost:8010/careergraph/api/v1"
export INTERNAL_API_KEY="YOUR_INTERNAL_API_KEY"
```

If the API should prefer the local FastAPI embedding service during rebuild and resync, keep this enabled in backend env:

```env
APP_EMBED_USE_LOCAL_FIRST=true
APP_EMBED_ALLOW_GEMINI_FALLBACK=false
```

Ghi chú:

- Backend hiện đọc Elasticsearch URL từ `spring.elasticsearch.uris`
- Default trong backend là `http://localhost:9200`
- `INTERNAL_API_KEY` là giá trị của `SOCKET_INTERNAL_API_KEY` hoặc `socket.internal.api-key`

## 3. Kiểm tra index đang có hay chưa

### 3.1 Liệt kê 2 index

```bash
curl -sS "$ES_URL/_cat/indices/jobs_es,candidates_es?v"
```

### 3.2 Kiểm tra riêng từng index có tồn tại hay không

```bash
curl -i -sS "$ES_URL/jobs_es"
curl -i -sS "$ES_URL/candidates_es"
```

Nếu index tồn tại, Elasticsearch sẽ trả `200`.

Nếu chưa tồn tại, Elasticsearch thường trả `404`.

## 4. Kiểm tra cấu hình index

## 4.1 Xem mapping của job index

```bash
curl -sS "$ES_URL/jobs_es/_mapping?pretty"
```

### 4.2 Xem settings của job index

```bash
curl -sS "$ES_URL/jobs_es/_settings?pretty"
```

### 4.3 Xem mapping của candidate index

```bash
curl -sS "$ES_URL/candidates_es/_mapping?pretty"
```

### 4.4 Xem settings của candidate index

```bash
curl -sS "$ES_URL/candidates_es/_settings?pretty"
```

## 4.5 Kiểm tra nhanh dense vector dims

Job:

```bash
curl -sS "$ES_URL/jobs_es/_mapping?pretty"
```

Tìm field:

```json
"embedding": {
  "type": "dense_vector",
  "dims": 3072,
  "index": true,
  "similarity": "cosine"
}
```

Candidate:

```bash
curl -sS "$ES_URL/candidates_es/_mapping?pretty"
```

Tìm field:

```json
"embedding": {
  "type": "dense_vector",
  "dims": 3072,
  "index": true,
  "similarity": "cosine"
}
```

## 4.6 Kiểm tra số lượng document trong index

```bash
curl -sS "$ES_URL/jobs_es/_count?pretty"
curl -sS "$ES_URL/candidates_es/_count?pretty"
```

## 5. Xóa index

### 5.1 Xóa job index

```bash
curl -X DELETE -sS "$ES_URL/jobs_es?pretty"
```

### 5.2 Xóa candidate index

```bash
curl -X DELETE -sS "$ES_URL/candidates_es?pretty"
```

### 5.3 Xóa cả hai index

```bash
curl -X DELETE -sS "$ES_URL/jobs_es,candidates_es?pretty"
```

Lưu ý:

- Xóa index là xóa toàn bộ document trong index đó.
- Dữ liệu PostgreSQL không bị xóa.
- Sau khi xóa, cần tạo lại index và sync lại dữ liệu.

## 6. Tạo lại index thủ công bằng curl

Backend đang dùng các file mapping/settings này:

- `src/main/resources/elasticsearch/jobs-es-settings.json`
- `src/main/resources/elasticsearch/jobs-es-mappings.json`
- `src/main/resources/elasticsearch/candidates-es-settings.json`
- `src/main/resources/elasticsearch/candidates-es-mappings.json`

Đứng tại thư mục `careergraph-api` rồi chạy.

### 6.1 Tạo lại job index

```bash
curl -X PUT "$ES_URL/jobs_es" \
  -H 'Content-Type: application/json' \
  -d @<(jq -n \
    --slurpfile settings src/main/resources/elasticsearch/jobs-es-settings.json \
    --slurpfile mappings src/main/resources/elasticsearch/jobs-es-mappings.json \
    '{settings: $settings[0], mappings: $mappings[0]}')
```

### 6.2 Tạo lại candidate index

```bash
curl -X PUT "$ES_URL/candidates_es" \
  -H 'Content-Type: application/json' \
  -d @<(jq -n \
    --slurpfile settings src/main/resources/elasticsearch/candidates-es-settings.json \
    --slurpfile mappings src/main/resources/elasticsearch/candidates-es-mappings.json \
    '{settings: $settings[0], mappings: $mappings[0]}')
```

## 6.3 Nếu máy không có jq

Bạn có thể dùng payload inline.

### Job index

```bash
curl -X PUT "$ES_URL/jobs_es" \
  -H 'Content-Type: application/json' \
  -d '{
    "settings": {
      "analysis": {
        "analyzer": {
          "vi_analyzer": {
            "type": "custom",
            "tokenizer": "standard",
            "filter": ["lowercase", "asciifolding"]
          }
        }
      }
    },
    "mappings": {
      "properties": {
        "id": {"type": "keyword"},
        "title": {"type": "text", "analyzer": "vi_analyzer"},
        "description": {"type": "text", "analyzer": "vi_analyzer"},
        "status": {"type": "keyword"},
        "jobCategory": {
          "type": "text",
          "analyzer": "vi_analyzer",
          "fields": {"keyword": {"type": "keyword"}}
        },
        "employmentType": {"type": "keyword"},
        "experienceLevel": {"type": "keyword"},
        "education": {"type": "keyword"},
        "state": {"type": "text", "analyzer": "vi_analyzer"},
        "city": {"type": "keyword"},
        "companyId": {"type": "keyword"},
        "createdAt": {"type": "date", "format": "yyyy-MM-dd"},
        "embedding": {
          "type": "dense_vector",
          "dims": 3072,
          "index": true,
          "similarity": "cosine"
        }
      }
    }
  }'
```

### Candidate index

```bash
curl -X PUT "$ES_URL/candidates_es" \
  -H 'Content-Type: application/json' \
  -d '{
    "settings": {
      "analysis": {
        "analyzer": {
          "vi_analyzer": {
            "type": "custom",
            "tokenizer": "standard",
            "filter": ["lowercase", "asciifolding"]
          }
        }
      }
    },
    "mappings": {
      "properties": {
        "id": {"type": "keyword"},
        "firstName": {"type": "text", "analyzer": "vi_analyzer"},
        "lastName": {"type": "text", "analyzer": "vi_analyzer"},
        "email": {"type": "keyword"},
        "phone": {"type": "keyword"},
        "avatar": {"type": "keyword"},
        "gender": {"type": "keyword"},
        "yearsOfExperience": {"type": "integer"},
        "desiredPosition": {
          "type": "text",
          "analyzer": "vi_analyzer",
          "fields": {"keyword": {"type": "keyword"}}
        },
        "currentJobTitle": {
          "type": "text",
          "analyzer": "vi_analyzer",
          "fields": {"keyword": {"type": "keyword"}}
        },
        "summary": {"type": "text", "analyzer": "vi_analyzer"},
        "resumeText": {"type": "text", "analyzer": "vi_analyzer"},
        "isOpenToWork": {"type": "boolean"},
        "educationLevel": {"type": "keyword"},
        "experienceLevel": {"type": "keyword"},
        "industries": {"type": "keyword"},
        "locations": {"type": "keyword"},
        "workTypes": {"type": "keyword"},
        "salaryExpectationMin": {"type": "integer"},
        "salaryExpectationMax": {"type": "integer"},
        "skills": {"type": "text", "analyzer": "vi_analyzer"},
        "createdAt": {"type": "date", "format": "yyyy-MM-dd"},
        "lastActive": {"type": "date", "format": "yyyy-MM-dd'T''HH:mm:ss||yyyy-MM-dd"},
        "embedding": {
          "type": "dense_vector",
          "dims": 3072,
          "index": true,
          "similarity": "cosine"
        }
      }
    }
  }'
```

## 7. Tạo lại index bằng backend sync

Trong code hiện tại, backend có khả năng tự recreate index nếu phát hiện lỗi mismatch dense vector dimensions.

Nhưng nếu bạn đã xóa index thủ công, cách an toàn nhất là:

1. tạo lại index bằng `PUT` như mục 6
2. gọi sync endpoint để nạp lại dữ liệu

### 7.1 Sync lại job index

```bash
curl -X POST "$API_URL/internal/elasticsearch/sync?target=jobs&jobBatchSize=100&force=true" \
  -H "x-internal-api-key: $INTERNAL_API_KEY" \
  -H 'Accept: application/json'
```

### 7.2 Sync lại candidate index

```bash
curl -X POST "$API_URL/internal/elasticsearch/sync?target=candidates&candidateBatchSize=50&force=true" \
  -H "x-internal-api-key: $INTERNAL_API_KEY" \
  -H 'Accept: application/json'
```

### 7.3 Sync lại cả hai

```bash
curl -X POST "$API_URL/internal/elasticsearch/sync?target=all&jobBatchSize=100&candidateBatchSize=50&force=true" \
  -H "x-internal-api-key: $INTERNAL_API_KEY" \
  -H 'Accept: application/json'
```

Nếu local AI chưa chạy và fallback Gemini đang bị limit, nên giảm batch size xuống nhỏ hơn.

## 8. Quy trình khuyến nghị

## 8.1 Kiểm tra mapping hiện tại

```bash
curl -sS "$ES_URL/jobs_es/_mapping?pretty"
curl -sS "$ES_URL/candidates_es/_mapping?pretty"
```

## 8.2 Nếu thấy dims sai hoặc index lỗi

```bash
curl -X DELETE -sS "$ES_URL/jobs_es?pretty"
curl -X DELETE -sS "$ES_URL/candidates_es?pretty"
```

## 8.3 Tạo lại index

Chạy lệnh `PUT` ở mục 6.

## 8.4 Sync lại dữ liệu

Chạy lệnh `POST /internal/elasticsearch/sync` ở mục 7.

## 8.5 Kiểm tra lại sau sync

```bash
curl -sS "$ES_URL/_cat/indices/jobs_es,candidates_es?v"
curl -sS "$ES_URL/jobs_es/_count?pretty"
curl -sS "$ES_URL/candidates_es/_count?pretty"
```

## 9. Kết quả mong muốn

Sau khi hoàn tất:

- `jobs_es` tồn tại
- `candidates_es` tồn tại
- cả hai index có `embedding.dims = 3072`
- `_count` của `jobs_es` và `candidates_es` lớn hơn `0`
- lệnh sync backend trả về `status=OK`