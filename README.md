# CareerGraph API

CareerGraph API là backend trung tâm của nền tảng. Service này điều phối toàn bộ miền nghiệp vụ chính: xác thực người dùng, hồ sơ ứng viên, doanh nghiệp, tuyển dụng, ứng tuyển, phỏng vấn, nhắn tin, thông báo, media, tìm kiếm ngữ nghĩa và tích hợp AI.

## Product role

`careergraph-api` là system of record cho các ứng dụng:

- `careergraph-client`
- `careergraph-hr`
- `careergraph-admin`

Service này giữ vai trò orchestration layer giữa frontend, dữ liệu vận hành và các service phụ trợ như AI, RTC, Redis, Elasticsearch và media storage.

## Core capabilities

- xác thực candidate và HR, bao gồm JWT, refresh token, OTP và Google login
- quản lý hồ sơ ứng viên, kỹ năng, học vấn, kinh nghiệm và file CV
- quản lý doanh nghiệp, xác thực doanh nghiệp và governance actions
- tạo, cập nhật, publish và tìm kiếm việc làm
- quản lý applications và pipeline tuyển dụng
- lịch phỏng vấn, reschedule flow, room lifecycle, feedback và recording metadata
- messaging threads, read status, archive và unsend
- notification center và unread counters
- semantic search và recommendation cho jobs và candidates
- tích hợp AI cho chat, CV review, keyword extraction và embeddings

## Architecture

```text
careergraph-client / careergraph-hr / careergraph-admin
                |
                v
        careergraph-api
          |       |       |
          |       |       +--> careergraph-rtc
          |       +----------> careergraph-ai
          |
          +--> PostgreSQL / Redis / Elasticsearch / Mail / Media providers
```

## Application modules

Các nhóm module lớn trong codebase:

- authentication và account management
- candidate profile domain
- company profile và verification domain
- jobs và applications domain
- interview scheduling và room coordination
- messaging và notifications
- analytics dashboard data
- Elasticsearch indexing và recommendation flows
- AI integration và embedding gateway

## Technology

- Java 17
- Spring Boot 3.4
- Spring Web
- Spring WebFlux
- Spring Security
- Spring Data JPA
- PostgreSQL
- Redis
- Elasticsearch
- Spring AI
- MapStruct
- Lombok
- Thymeleaf
- Cloudinary SDK
- PDFBox
- Apache POI

## Code structure

```text
src/main/java/com/hcmute/careergraph/
├── config/
├── controllers/
├── enums/
├── exception/
├── helper/
├── listeners/
├── mapper/
├── persistence/
│   ├── models/
│   ├── documents/
│   ├── dtos/
│   └── event/
├── repositories/
├── schedule/
└── services/
```

## Environment model

Cấu hình môi trường được phân tách theo nhóm hạ tầng và tích hợp, với `application.yml` làm canonical configuration layer và các biến môi trường làm nguồn override cho từng môi trường triển khai.

Các nhóm cấu hình quan trọng:

- server và servlet context
- PostgreSQL datasource
- Redis cache
- Elasticsearch
- JWT và security
- Google OAuth
- Gemini và AI integration
- RTC internal integration
- mail delivery
- media storage
- CORS và public endpoints

Môi trường production nên được cấp qua secret manager hoặc CI/CD variables; các file `.env*` chỉ nên phục vụ local development, staging bootstrap hoặc deployment packaging có kiểm soát.

## Local development

Hai cách làm việc phổ biến:

### Full stack bằng Docker Compose

```bash
docker compose -f careergraph-api/docker-compose.yaml up -d
```

### Chạy application local, hạ tầng chạy riêng

```bash
./mvnw spring-boot:run
```

Hoặc:

```bash
./mvnw package
java -jar target/*.jar
```

Trong local development hiện tại, service thường được ghép với:

- PostgreSQL
- Redis
- Elasticsearch
- `careergraph-ai`
- `careergraph-rtc`

## API operations

Khi service chạy đầy đủ, các bề mặt vận hành quan trọng gồm:

- Swagger UI
- OpenAPI docs
- actuator health endpoints

Đây là các điểm phù hợp cho smoke test sau deploy hoặc kiểm tra readiness trong môi trường staging.

## Deployment notes

- Service cần được triển khai cùng PostgreSQL, Redis và Elasticsearch
- `careergraph-ai` và `careergraph-rtc` phả- Nếu triển khai sau reverse proxy hoặc Traefik, cần đồng bộ context path, forwarded headers và public URLsi được cấu hình thành các dependency truy cập được qua network nội bộ hoặc service discovery
- Timezone, cookie policy, CORS, SMTP và media credentials cần được khóa chặt trước khi đưa lên production
- Nếu triển khai sau reverse proxy hoặc Traefik, cần đồng bộ context path, forwarded headers và public URLs

