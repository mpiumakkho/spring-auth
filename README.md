# rbac-ums

> User Management System with Role-Based Access Control — Java 21 · Spring Boot 3.5 · PostgreSQL 16

![Java](https://img.shields.io/badge/Java-21-orange?style=flat-square&logo=java)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.5.0-brightgreen?style=flat-square&logo=spring-boot)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-blue?style=flat-square&logo=postgresql)

ระบบจัดการผู้ใช้พร้อม RBAC แยกเป็น 2 services:

- **core-api** — REST backend + JPA + RBAC
- **web-api** — Thymeleaf UI ที่เรียก core-api ผ่าน `X-API-Key`

---

## Quick Start

**ผ่าน Docker** (ง่ายสุด — รวม PostgreSQL + ทั้ง 2 services)

```bash
docker-compose up -d
```

**รัน local** (ต้องมี PostgreSQL 16 ทำงานอยู่ก่อน)

```bash
cd core-api && ./mvnw spring-boot:run    # :8091
cd web-api  && ./mvnw spring-boot:run    # :8081/ums
```

จากนั้นเปิด:

| Service | URL |
| --- | --- |
| Web UI | <http://localhost:8081/ums> |
| Swagger | <http://localhost:8091/swagger-ui.html> |
| Health | <http://localhost:8091/actuator/health> |

Login ด้วย `admin` / `password` (ดู [Default Accounts](#default-accounts))

---

## Architecture

```text
Web API (:8081/ums)             Core API (:8091)
┌──────────────────┐             ┌──────────────────────┐
│ Thymeleaf UI     │   X-API-Key │ REST Controllers     │
│ Controllers      │ ──────────► │ Service / Repository │
│ SessionFilter    │  REST/JSON  │ TokenFilter + RBAC   │
└──────────────────┘             │ PostgreSQL (JPA)     │
                                 └──────────────────────┘
```

## Tech Stack

- **Backend** — Java 21, Spring Boot 3.5, Spring Security
- **Database** — PostgreSQL 16, Flyway migrations
- **ORM** — Spring Data JPA (Hibernate)
- **Frontend** — Thymeleaf 3.1
- **Auth** — BCrypt $2A cost=12 + UUID token sessions
- **Docs** — Springdoc OpenAPI (Swagger UI)
- **Build** — Maven 3.9+ (wrapper included: `./mvnw`)

## Security

- **Inter-service auth** — `X-API-Key` header ระหว่าง web-api กับ core-api
- **User auth** — UUID token sessions (30 นาที idle timeout, scheduled cleanup)
- **Password hashing** — BCrypt $2A cost=12
- **RBAC** — `@PreAuthorize` + custom `PermissionEvaluator`; permission format: `USER:READ`, `ROLE:UPDATE`, ฯลฯ
- **CSRF** — เปิดสำหรับ web-api (form-based), ปิดสำหรับ core-api (stateless)
- **Rate limiting** — 10 req/min ต่อ IP บน login + session endpoints
- **Input validation** — Jakarta Bean Validation บนทุก controller; `GlobalExceptionHandler` คืน error format มาตรฐาน
- **Encryption** — AES/GCM/NoPadding (random IV) สำหรับ role payload

---

## Configuration

| Variable | Default | Note |
| --- | --- | --- |
| `DB_URL` | `jdbc:postgresql://localhost:5432/postgres` | |
| `DB_USERNAME` | `postgres` | |
| `DB_PASSWORD` | `P@ss4321` | |
| `AES_KEY` | `1234567890123456` | 16 chars · **เปลี่ยนใน production** |
| `API_KEY` | `changeme-dev-api-key-2024` | inter-service · **เปลี่ยนใน production** |

## API Reference

ดู endpoints ทั้งหมดผ่าน Swagger UI หรือ Postman collection ใน [`core-api/UMS Core API - Full Collection.postman_collection.json`](core-api/UMS%20Core%20API%20-%20Full%20Collection.postman_collection.json)

ตัวอย่างที่ใช้บ่อย:

**Auth** — `/api/users`, `/api/sessions`

- `POST /api/users/login` — login (รับ `username` หรือ `email` + `password`)
- `POST /api/sessions/validate` — ตรวจ token
- `POST /api/sessions/keep-alive` — refresh session
- `POST /api/sessions/logout` — logout

**Users** — `/api/users`

- `GET /api/users?page=0&size=20&sort=createdAt,desc` — paginated list
- `POST /api/users/create` · `PUT /api/users/update` · `DELETE /api/users/{id}`
- `POST /api/users/assign-role` · `POST /api/users/remove-role`

**Roles** — `/api/roles`

- `GET /api/roles?page=&size=&sort=` — paginated list
- `POST /api/roles/create` · `PUT /api/roles/update` · `POST /api/roles/delete`
- `POST /api/roles/assign-permission` · `POST /api/roles/remove-permission`

**Permissions** — `/api/permissions`

- `GET /api/permissions?page=&size=&sort=` — paginated list
- `POST /api/permissions/create` · `PUT /api/permissions/update` · `POST /api/permissions/delete`

---

## Default Accounts

Password เริ่มต้นทุก account คือ `password`

| Username | Role |
| --- | --- |
| `admin` | SUPER_ADMIN |
| `manager` | USER_MANAGER |
| `viewer` | VIEWER |
| `mod` | MODERATOR |
| `analyst` | ANALYST |
| `support` | SUPPORT |

## Development

```bash
# Run tests
cd core-api && ./mvnw test
cd web-api  && ./mvnw test
```

- **Roadmap** — [ROADMAP.md](ROADMAP.md)
- **Code-review notes** — [CONTEXT_ENGINEERING.md](CONTEXT_ENGINEERING.md)
