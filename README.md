# rbac-ums (User Management System)

![Java](https://img.shields.io/badge/Java-21-orange?style=flat-square&logo=java)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.5.0-brightgreen?style=flat-square&logo=spring-boot)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-blue?style=flat-square&logo=postgresql)

ระบบจัดการผู้ใช้งานพร้อม Role-Based Access Control (RBAC) แบ่งเป็น 2 services: **Core API** (backend) และ **Web API** (frontend)

## Architecture

```
Web API (:8081/demo)          Core API (:8091)
┌──────────────────┐          ┌──────────────────────┐
│ Thymeleaf UI     │  REST    │ REST Controllers     │
│ Controllers      │─────────>│ Service / Repository │
│ SessionFilter    │ X-API-Key│ TokenFilter + RBAC   │
└──────────────────┘          │ PostgreSQL (JPA)     │
                              └──────────────────────┘
```

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Language | Java 21 |
| Framework | Spring Boot 3.5.0, Spring Security |
| Database | PostgreSQL 16, Flyway migrations |
| ORM | Spring Data JPA (Hibernate) |
| Frontend | Thymeleaf 3.1 |
| Auth | BCrypt ($2A/12), UUID token sessions |
| API Docs | Springdoc OpenAPI (Swagger UI) |
| Build | Maven 3.9.5 |

## Security

- **Inter-service auth**: API key (`X-API-Key` header) ระหว่าง web-api กับ core-api
- **User auth**: UUID token-based sessions (30 min timeout, auto cleanup)
- **Password**: BCrypt $2A cost=12
- **RBAC**: `@PreAuthorize` + custom `PermissionEvaluator`
- **Permission model**: `ROLE_ADMIN`, `PERM_USER:READ`, `PERM_ROLE:UPDATE`
- **CSRF**: เปิดสำหรับ web-api (Thymeleaf), ปิดสำหรับ core-api (stateless)
- **Rate limiting**: 10 req/min per IP บน login/session endpoints

## Getting Started

### Prerequisites
- Java 21+, Maven 3.9+, PostgreSQL 16+

### Environment Variables

| Variable | Default | Description |
|----------|---------|-------------|
| `DB_URL` | `jdbc:postgresql://localhost:5432/postgres` | Database URL |
| `DB_USERNAME` | `postgres` | Database username |
| `DB_PASSWORD` | `P@ss4321` | Database password |
| `AES_KEY` | `1234567890123456` | AES encryption key (16 chars) |
| `API_KEY` | `changeme-dev-api-key-2024` | Inter-service API key |

### Run

```bash
# Core API (start first)
cd core-api && mvn spring-boot:run

# Web API
cd web-api && mvn spring-boot:run
```

### Docker

```bash
docker-compose up -d
```

### Access

| Service | URL |
|---------|-----|
| Web UI | http://localhost:8081/demo |
| Swagger UI | http://localhost:8091/swagger-ui.html |
| Health Check | http://localhost:8091/actuator/health |

## API Endpoints

### Authentication
| Method | Path | Description |
|--------|------|-------------|
| POST | `/api/users/login` | Login |
| POST | `/api/sessions/validate` | Validate token |
| POST | `/api/sessions/keep-alive` | Refresh session |
| POST | `/api/sessions/logout` | Logout |

### Users (`/api/users`)
| Method | Path | Description |
|--------|------|-------------|
| GET | `/api/users` | List all |
| POST | `/api/users/create` | Create |
| PUT | `/api/users/update` | Update |
| DELETE | `/api/users/{id}` | Delete |
| POST | `/api/users/assign-role` | Assign role |
| POST | `/api/users/remove-role` | Remove role |

### Roles (`/api/roles`)
| Method | Path | Description |
|--------|------|-------------|
| GET | `/api/roles` | List all |
| POST | `/api/roles/create` | Create |
| PUT | `/api/roles/update` | Update |
| POST | `/api/roles/assign-permission` | Assign permission |

### Permissions (`/api/permissions`)
| Method | Path | Description |
|--------|------|-------------|
| GET | `/api/permissions` | List all |
| POST | `/api/permissions/create` | Create |
| PUT | `/api/permissions/update` | Update |

## Default Accounts

| Username | Role | Password |
|----------|------|----------|
| `admin` | SUPER_ADMIN | `password` |
| `manager` | USER_MANAGER | `password` |
| `viewer` | VIEWER | `password` |
| `mod` | MODERATOR | `password` |
| `analyst` | ANALYST | `password` |
| `support` | SUPPORT | `password` |

## Testing

```bash
cd core-api && mvn test
cd web-api && mvn test
```
