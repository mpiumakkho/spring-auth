# rbac-ums

> Multi-tenant User Management System with RBAC + ABAC — Java 21 · Spring Boot 3.5 · PostgreSQL 16 · React 18

![Java](https://img.shields.io/badge/Java-21-orange?style=flat-square&logo=java)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.5.0-brightgreen?style=flat-square&logo=spring-boot)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-blue?style=flat-square&logo=postgresql)
![React](https://img.shields.io/badge/React-18-61DAFB?style=flat-square&logo=react)
![TypeScript](https://img.shields.io/badge/TypeScript-5.6-3178C6?style=flat-square&logo=typescript)

3 services — SPA → BFF → API:

- **frontend** (`:5173`) — Vite + React + TypeScript SPA — owns 100% of the UI
- **web-api** (`:8081`) — pure BFF: cookie-JWT auth, response cache, aggregation, proxy to core-api
- **core-api** (`:8091`) — REST + JPA + RBAC/ABAC + multi-tenancy

> 🇹🇭 ฉบับภาษาไทยอยู่ด้านล่างสุด — เลื่อนลงไปดูที่ [README (ภาษาไทย)](#readme-ภาษาไทย)

---

## Quick Start

**Docker (recommended)** — bundles PostgreSQL + both backend services:

```bash
docker-compose up -d
```

Optional observability stack (Prometheus + Grafana):

```bash
docker-compose --profile observability up -d
```

**Local dev** — needs a PostgreSQL 16 running first:

```bash
cd core-api && ./mvnw spring-boot:run     # :8091
cd web-api  && ./mvnw spring-boot:run     # :8081
cd frontend && npm install && npm run dev # :5173
```

| Service | URL |
| --- | --- |
| SPA (login + everything else) | <http://localhost:5173> |
| BFF health | <http://localhost:8081/actuator/health> |
| Swagger | <http://localhost:8091/swagger-ui.html> |
| Prometheus | <http://localhost:9090> (observability profile) |
| Grafana | <http://localhost:3000> (admin / admin) |
| Health | <http://localhost:8091/actuator/health> |

Sign in with `admin` / `password` — see [Default Accounts](#default-accounts) for the rest.

---

## Architecture

```text
                  static SPA bundle               cookie-JWT  +  XHR
Browser ──HTTP──► frontend (:5173)    Browser ──fetch──► web-api (:8081)
                  Vite + React + TS              ─ BFF ─────────────────
                  owns ALL UI                     /bff/auth/{login,refresh,logout,oauth2-callback}
                                                  /bff/api/**            generic proxy
                                                  /bff/api/v1/me/full    parallel aggregation
                                                  Caffeine response cache · Apache HC5 pool
                                                          │
                                                          │ Authorization: Bearer <jwt>
                                                          │ X-API-Key: <inter-service>
                                                          ▼
                                                  core-api (:8091)
                                                  TokenFilter (JWT, no DB lookup)
                                                  TenantFilterAspect · AttributePolicy
                                                  @Async audit + notifications
                                                  PostgreSQL (Flyway V1..V10) · Redis · OAuth2 Login
```

The web-api earns its hop:

- **Cookie-JWT** — JWT and refresh tokens stay in `httpOnly` cookies, never exposed to JS
- **API key vault** — `X-API-Key` lives only between web-api ↔ core-api
- **Aggregation** — `GET /bff/api/v1/me/full` fans out 3 parallel core-api calls in 1 round-trip
- **Response cache** — Caffeine, ~20s TTL, scoped per token-hash for safe GETs
- **Pooled HTTP** — Apache HttpClient 5 keeps the TCP+TLS handshake amortized

---

## Tech Stack

**Backend (core-api)** — Java 21 · Spring Boot 3.5 · Spring Security · Spring Data JPA · Spring AOP · Spring Cache · Spring Data Redis · Spring Mail · Spring OAuth2 Client · jjwt 0.12.6 · Flyway · BCrypt · Caffeine

**BFF (web-api)** — Spring Boot 3.5 · Spring Security · Apache HttpClient 5 (pooled) · Caffeine · Micrometer + Prometheus

**Frontend** — Vite 5 · React 18 · TypeScript 5.6 · React Router 6

**Database** — PostgreSQL 16 (Flyway migrations V1–V10)

**Observability** — Micrometer + Prometheus + pre-provisioned Grafana dashboard

**CI** — GitHub Actions (`.github/workflows/ci.yml`)

---

## Security

- **JWT (HS256)** — 30 min access tokens, includes `tid` (tenant) + `roles` + `perms` claims; TokenFilter validates signature without hitting the DB
- **Refresh token** — UUID, 7 day TTL, persisted in `refresh_tokens` so it can be revoked
- **httpOnly cookies** — SPA never touches the JWT directly; BFF sets `rbac_token` + `rbac_refresh` with `SameSite=Lax`
- **Inter-service auth** — `X-API-Key` between BFF and core-api (constant-time comparison)
- **Password hashing** — BCrypt `$2A` cost=12; password policy enforced via `@PasswordPolicy` (8+ chars, upper/lower/digit/special)
- **Account lockout** — 5 failed logins → 15 min lockout; `POST /api/v1/users/admin/unlock` to override
- **RBAC** — `@PreAuthorize("hasPermission(null, 'USER:READ')")` + custom `PermissionEvaluator` reading authorities from JWT
- **ABAC** — `@attributePolicy.sameAttribute(#userId, 'department')` and friends, callable from `@PreAuthorize` SpEL
- **Multi-tenancy** — Hibernate `@Filter` activates per request; `SUPER_ADMIN` bypasses; default-tenant backfill for legacy rows
- **Rate limiting** — 10 req/min per IP on login + session endpoints
- **Input validation** — Jakarta Bean Validation on every `@RequestBody`; `GlobalExceptionHandler` returns RFC 9457-style errors
- **Encryption** — AES/GCM with random IV for encrypted role payloads
- **OAuth2 Login** — Spring OAuth2 Client chain conditionally activated when `spring.security.oauth2.client.registration.*` is set; success handler bridges provider userinfo into `OAuthLinkService.resolveOrCreate` and mints our own JWT

### Why a BFF? — what `web-api` actually buys you

Cutting `web-api` out (so the SPA hits `core-api` directly) loses the following defenses:

| Attack vector | Without BFF (SPA → core-api) | With BFF (SPA → web-api → core-api) |
| --- | --- | --- |
| **XSS reads the access token** | JWT lives in `localStorage` so any injected JS can read it and exfiltrate | JWT in `httpOnly` cookie — JS can't read it; cookie ships only on same-origin requests |
| **Inspecting the JS bundle** | `X-API-Key` baked into the bundle; every visitor can extract it | API key never leaves the BFF; each browser only carries an opaque session cookie |
| **Refresh token theft** | Refresh token must be in `localStorage` too (or in URL fragments after redirect) | Refresh token in second `httpOnly` cookie; rotated by `/bff/auth/refresh` server-side |
| **3rd-party JS (analytics, CDNs)** | Reads `localStorage`, can siphon tokens | `httpOnly` cookies are unreadable from JS regardless of who injected the script |
| **CSRF** | N/A (no cookies, but tokens in JS-managed headers) | Mitigated by `SameSite=Lax` on the cookies + CORS allowlist on `/bff/**` |
| **Replay attack on stolen token** | Whatever attacker copied is valid for the JWT TTL — no server side handle | BFF can revoke the refresh cookie + force re-login without changing the JWT secret |
| **Endpoint enumeration / abuse** | core-api exposed publicly, bots can probe every route | core-api can be put on a private subnet; only `/bff/**` is internet-facing |

The aggregation endpoint (`GET /bff/api/v1/me/full`) and short-TTL response cache also reduce the chattiness — the BFF earns its hop on perf as well as on security.

---

## Configuration

### Common (env vars)

| Variable | Default | Note |
| --- | --- | --- |
| `DB_URL` | `jdbc:postgresql://localhost:5432/postgres` | |
| `DB_USERNAME` | `postgres` | |
| `DB_PASSWORD` | `P@ss4321` | |
| `JWT_SECRET` | dev placeholder | **≥ 32 bytes**; rotate in prod |
| `JWT_ACCESS_TTL_MIN` | `30` | access-token lifetime |
| `JWT_ISSUER` | `rbac-ums` | |
| `REFRESH_TTL_DAYS` | `7` | |
| `API_KEY` | `changeme-dev-api-key-2024` | inter-service · **rotate in prod** |
| `AES_KEY` | `1234567890123456` | 16 chars · **rotate in prod** |

### Optional (off unless configured)

| Variable | Effect |
| --- | --- |
| `REDIS_HOST` / `REDIS_PORT` | Switches role/permission cache backend; set `CACHE_TYPE=redis` to activate |
| `GOOGLE_CLIENT_ID` / `GOOGLE_CLIENT_SECRET` | Enables Google OAuth2 Login (uncomment registration block in `application-dev.properties`) |
| `GITHUB_CLIENT_ID` / `GITHUB_CLIENT_SECRET` | Enables GitHub OAuth2 Login |
| `OAUTH2_SUCCESS_REDIRECT` | Where the OAuth2 success handler hands control back (default: BFF callback) |
| `AVATAR_DIR` / `AVATAR_BASE_URL` | Avatar upload location (defaults to `./uploads/avatars`) |

### BFF tuning (`web-api/application-dev.properties`)

| Property | Default | Effect |
| --- | --- | --- |
| `bff.http.max-total` / `bff.http.max-per-route` | `200` / `50` | Pooled HTTP client size |
| `bff.cache.ttl-seconds` | `20` | Response-cache TTL |
| `bff.cache.cacheable-paths` | `/v1/users/me, /v1/notifications/unread-count, /v1/tenants` | Whitelist for safe-GET caching |
| `bff.cookie.same-site` / `bff.cookie.secure` | `Lax` / `false` | Set `secure=true` in HTTPS prod |
| `bff.cors.allowed-origins` | `http://localhost:5173,http://localhost:3000` | SPA dev origins |

---

## API Reference

Every endpoint sits under `/api/v1/` (v1 versioning).
Browse them all via Swagger UI or the Postman collection at [`core-api/UMS Core API - Full Collection.postman_collection.json`](core-api/UMS%20Core%20API%20-%20Full%20Collection.postman_collection.json).

### Auth — `/api/v1/users`, `/api/v1/sessions`, `/api/v1/account`

- `POST /api/v1/users/login` — username|email + password → JWT + refresh token
- `POST /api/v1/sessions/refresh` — exchange refresh token for new JWT
- `POST /api/v1/sessions/logout` · `POST /api/v1/sessions/revoke-refresh`
- `POST /api/v1/account/verify-email` · `POST /api/v1/account/forgot-password` · `POST /api/v1/account/reset-password`
- `POST /api/v1/oauth2/callback/exchange` — BYO-identity OAuth2 alt path

### BFF — `/bff/auth`, `/bff/api/v1/**` (web-api)

- `POST /bff/auth/login` → sets `rbac_token` + `rbac_refresh` cookies
- `POST /bff/auth/logout` · `POST /bff/auth/refresh` · `GET /bff/auth/oauth2-callback`
- `GET /bff/api/v1/me/full` — aggregation (parallel fan-out)
- `* /bff/api/v1/**` — generic proxy (Authorization injected from cookie)

### Users — `/api/v1/users`

- `GET /api/v1/users?page=&size=&sort=` — paginated list
- `POST /api/v1/users/create` · `PUT /api/v1/users/update` · `DELETE /api/v1/users/{id}`
- `POST /api/v1/users/assign-role` · `POST /api/v1/users/remove-role`
- `GET /api/v1/users/me` · `PUT /api/v1/users/me` · `POST /api/v1/users/me/avatar` (multipart)
- `POST /api/v1/users/admin/unlock` · `POST /api/v1/users/admin/activate`
- `GET|PUT|DELETE /api/v1/users/{userId}/attributes` — ABAC user attributes

### Roles + Permissions — `/api/v1/roles`, `/api/v1/permissions`

- Standard CRUD, paginated; `/api/v1/roles/assign-permission` etc.

### Notifications — `/api/v1/notifications`

- `GET /api/v1/notifications?page=&size=` · `GET /unread-count`
- `PUT /{id}/read` · `PUT /read-all`

### Tenants (SUPER_ADMIN only) — `/api/v1/tenants`

- `GET /api/v1/tenants` · `POST /api/v1/tenants` · `GET /api/v1/tenants/{id}`

### Audit log (SUPER_ADMIN only) — `/api/v1/audit-logs`

- `GET /api/v1/audit-logs?actor=&action=&from=&to=&page=&size=`

---

## Default Accounts

Default password is `password` for all of them (in the default tenant, `default-tenant`).

| Username | Role |
| --- | --- |
| `admin` | SUPER_ADMIN |
| `manager` | USER_MANAGER |
| `viewer` | VIEWER |
| `mod` | MODERATOR |
| `analyst` | ANALYST |
| `support` | SUPPORT |

---

## Development

```bash
# Backend
cd core-api && ./mvnw test       # context-load + integration with PostgreSQL
cd web-api  && ./mvnw test       # 4 BFF + auth tests

# Frontend
cd frontend && npm run lint      # tsc --noEmit
cd frontend && npm run build     # Vite production build
```

**Migrations** — Flyway runs V1..V10 on startup. To add a new one, drop a `V11__*.sql` file into [core-api/src/main/resources/db/migration/](core-api/src/main/resources/db/migration/).

**Observability** — Prometheus scrapes `/actuator/prometheus` from both backend services. The pre-provisioned Grafana dashboard at [docs/observability/grafana-provisioning/dashboards/rbac-ums-overview.json](docs/observability/grafana-provisioning/dashboards/rbac-ums-overview.json) covers HTTP rate, p95 latency, error rate, JVM heap, Hikari, cache stats, and BFF proxy/aggregation timing.

**Docs:**

- [ROADMAP.md](ROADMAP.md) — phase status (Phase 1 + 2 done; Phase 3 mostly done)
- [CONTEXT_ENGINEERING.md](CONTEXT_ENGINEERING.md) — code-review notes
- [frontend/README.md](frontend/README.md) — SPA-specific docs

---
---

# README (ภาษาไทย)

> ระบบจัดการ user แบบ multi-tenant พร้อม RBAC + ABAC — Java 21 · Spring Boot 3.5 · PostgreSQL 16 · React 18

แยกเป็น 3 services ต่อกันเป็น SPA → BFF → API:

- **frontend** (`:5173`) — SPA เขียนด้วย Vite + React + TypeScript — ถือ UI ทั้งหมด รวม login
- **web-api** (`:8081`) — BFF (Backend-for-Frontend) ล้วน ๆ: auth ผ่าน cookie-JWT, response cache, aggregation, proxy ไป core-api
- **core-api** (`:8091`) — REST API ตัวจริง + JPA + RBAC/ABAC + multi-tenancy

---

## เริ่มใช้งาน

**ใช้ Docker (วิธีที่ง่ายสุด)** — มี PostgreSQL กับ backend ทั้งสองตัวมาให้ครบ:

```bash
docker-compose up -d
```

ถ้าอยากเปิด observability stack (Prometheus + Grafana) ด้วย:

```bash
docker-compose --profile observability up -d
```

**รันแบบ local** — ต้องเปิด PostgreSQL 16 ไว้ก่อน:

```bash
cd core-api && ./mvnw spring-boot:run     # :8091
cd web-api  && ./mvnw spring-boot:run     # :8081
cd frontend && npm install && npm run dev # :5173
```

| Service | URL |
| --- | --- |
| SPA (login + ทุกหน้า) | <http://localhost:5173> |
| BFF health | <http://localhost:8081/actuator/health> |
| Swagger | <http://localhost:8091/swagger-ui.html> |
| Prometheus | <http://localhost:9090> (เปิดเฉพาะ profile observability) |
| Grafana | <http://localhost:3000> (admin / admin) |
| Health | <http://localhost:8091/actuator/health> |

Login ด้วย `admin` / `password` — account อื่น ๆ ดูที่ [Default Accounts (TH)](#default-accounts-th)

---

## โครงสร้างระบบ

ดู ASCII diagram ในส่วน [Architecture](#architecture) ด้านบน — อันเดียวกัน

ทำไม web-api ถึงคุ้มที่จะมี (the BFF "earns its hop"):

- **Cookie-JWT** — JWT กับ refresh token เก็บไว้ใน `httpOnly` cookie, JS ใน browser อ่านไม่ได้
- **API key vault** — `X-API-Key` วิ่งระหว่าง web-api ↔ core-api เท่านั้น ไม่หลุดถึง browser
- **Aggregation** — `GET /bff/api/v1/me/full` ยิง 3 endpoints ไปที่ core-api แบบ parallel แล้วรวม response ให้ SPA เรียกแค่ครั้งเดียว
- **Response cache** — Caffeine, TTL ประมาณ 20 วิ, key แยกตาม token เพื่อไม่ให้ user เห็นข้อมูลกัน
- **Pooled HTTP** — ใช้ Apache HttpClient 5 pool connection ไว้ ทำให้ TCP+TLS handshake ไม่เกิดทุก request

---

## Tech Stack

**Backend (core-api)** — Java 21 · Spring Boot 3.5 · Spring Security · Spring Data JPA · Spring AOP · Spring Cache · Spring Data Redis · Spring Mail · Spring OAuth2 Client · jjwt 0.12.6 · Flyway · BCrypt · Caffeine

**BFF (web-api)** — Spring Boot 3.5 · Spring Security · Apache HttpClient 5 (pooled) · Caffeine · Micrometer + Prometheus

**Frontend** — Vite 5 · React 18 · TypeScript 5.6 · React Router 6

**Database** — PostgreSQL 16 (Flyway migrations V1–V10)

**Observability** — Micrometer + Prometheus + dashboard ของ Grafana ที่ provision ไว้แล้ว

**CI** — GitHub Actions (`.github/workflows/ci.yml`)

---

## Security

สรุปสั้น ๆ ของแต่ละจุด — รายละเอียด config อยู่ในไฟล์ `application-*.properties`

- **JWT (HS256)** — access token 30 นาที, ใน claims มี `tid` (tenant), `roles`, `perms`. TokenFilter เช็ค signature โดยไม่แตะ DB
- **Refresh token** — UUID อายุ 7 วัน เก็บในตาราง `refresh_tokens` เลย revoke ได้
- **httpOnly cookies** — SPA ไม่เคยแตะ JWT ตรง ๆ; BFF set ให้ทั้ง `rbac_token` กับ `rbac_refresh` พร้อม `SameSite=Lax`
- **Inter-service auth** — ใช้ `X-API-Key` ระหว่าง BFF กับ core-api เทียบแบบ constant-time
- **Password hashing** — BCrypt `$2A` cost=12, validate ผ่าน `@PasswordPolicy` (อย่างน้อย 8 ตัว ต้องมี upper/lower/digit/special)
- **Account lockout** — login ผิด 5 ครั้งโดน lock 15 นาที, แก้ผ่าน `POST /api/v1/users/admin/unlock`
- **RBAC** — `@PreAuthorize("hasPermission(null, 'USER:READ')")` กับ `PermissionEvaluator` ที่อ่าน authorities จาก JWT
- **ABAC** — เรียก `@attributePolicy.sameAttribute(#userId, 'department')` กับเพื่อน ๆ ของมันจาก SpEL ของ `@PreAuthorize`
- **Multi-tenancy** — Hibernate `@Filter` ทำงานต่อ request, `SUPER_ADMIN` ข้ามได้; row เก่า ๆ ที่มีอยู่ก่อน migration ถูก backfill เป็น default tenant
- **Rate limiting** — 10 req/นาที ต่อ IP เฉพาะ login กับ session endpoints
- **Input validation** — Jakarta Bean Validation บนทุก `@RequestBody`; `GlobalExceptionHandler` ส่ง error format ตามแนว RFC 9457
- **Encryption** — AES/GCM กับ random IV สำหรับ encrypt role payload
- **OAuth2 Login** — Spring OAuth2 Client chain เปิดให้เองเฉพาะตอนมี config `spring.security.oauth2.client.registration.*` ครบ; success handler เอา userinfo จาก provider ไปยิง `OAuthLinkService.resolveOrCreate` แล้วออก JWT ของเราเอง

### ทำไมต้องมี BFF? — `web-api` ช่วยกัน attack อะไรได้บ้าง

ถ้าตัด `web-api` ออก (ให้ SPA ยิงเข้า `core-api` ตรง ๆ) จะเสียกำแพงพวกนี้:

| ช่องโจมตี | ไม่มี BFF (SPA → core-api) | มี BFF (SPA → web-api → core-api) |
| --- | --- | --- |
| **XSS อ่าน access token** | JWT อยู่ใน `localStorage` JS ที่ inject เข้ามาอ่านได้ ส่งออกได้สบาย | JWT อยู่ใน `httpOnly` cookie — JS อ่านไม่ได้, cookie วิ่งเฉพาะ same-origin |
| **อ่านโค้ด JS bundle** | `X-API-Key` ฝังอยู่ใน bundle — เปิด DevTools ก็เห็น | API key อยู่แค่ที่ BFF, browser ถือแค่ session cookie ที่อ่านไม่ได้ |
| **ขโมย refresh token** | Refresh token ก็ต้องอยู่ใน `localStorage` หรือใน URL fragment | อยู่ในอีก `httpOnly` cookie, BFF rotate ให้ผ่าน `/bff/auth/refresh` |
| **3rd-party JS (analytics, CDN)** | อ่าน `localStorage` ได้ ดูด token ออกได้ | `httpOnly` cookie อ่านจาก JS ไม่ได้เลย ไม่ว่า JS นั้นจะมาจากไหน |
| **CSRF** | ไม่ต้องคิด (เพราะไม่ใช้ cookie) แต่ token ก็เปิดอยู่ใน header ที่ JS จัดการเอง | ป้องกันด้วย `SameSite=Lax` + CORS allowlist ที่ `/bff/**` |
| **Replay attack จาก token ที่หลุด** | ใครได้ JWT ไปก็ใช้ได้จนกว่ามันจะ expire — ไม่มีทางเรียกคืน | BFF revoke refresh cookie ได้, force ให้ login ใหม่โดยไม่ต้องเปลี่ยน JWT secret |
| **Probe/enumerate endpoints** | core-api เปิดสู่ public, bot ลอง path ได้ทุกอัน | core-api ซ่อนใน private subnet ก็ได้, เปิดเฉพาะ `/bff/**` ออก internet |

นอกจากเรื่อง security แล้ว aggregation endpoint (`GET /bff/api/v1/me/full`) กับ response cache ก็ลด chattiness ระหว่าง SPA กับ backend — BFF จึงคุ้มทั้งทางความปลอดภัยและ performance

---

## Configuration

### ตัวที่ใช้บ่อย (env vars)

| Variable | Default | หมายเหตุ |
| --- | --- | --- |
| `DB_URL` | `jdbc:postgresql://localhost:5432/postgres` | |
| `DB_USERNAME` | `postgres` | |
| `DB_PASSWORD` | `P@ss4321` | |
| `JWT_SECRET` | dev placeholder | ต้องอย่างน้อย **32 bytes** · เปลี่ยนใน prod |
| `JWT_ACCESS_TTL_MIN` | `30` | อายุ access token (นาที) |
| `JWT_ISSUER` | `rbac-ums` | |
| `REFRESH_TTL_DAYS` | `7` | |
| `API_KEY` | `changeme-dev-api-key-2024` | ใช้ระหว่าง service · **เปลี่ยนใน prod** |
| `AES_KEY` | `1234567890123456` | 16 ตัวอักษร · **เปลี่ยนใน prod** |

### ตัวเลือกเสริม (ปิดอยู่จนกว่าจะ config)

| Variable | ผลที่ได้ |
| --- | --- |
| `REDIS_HOST` / `REDIS_PORT` | สลับ cache ของ role/permission ไปใช้ Redis · ตั้ง `CACHE_TYPE=redis` ด้วย |
| `GOOGLE_CLIENT_ID` / `GOOGLE_CLIENT_SECRET` | เปิด Google OAuth2 Login (uncomment block ใน `application-dev.properties`) |
| `GITHUB_CLIENT_ID` / `GITHUB_CLIENT_SECRET` | เปิด GitHub OAuth2 Login |
| `OAUTH2_SUCCESS_REDIRECT` | URL ที่ OAuth2 success handler จะส่งกลับ (default: BFF callback) |
| `AVATAR_DIR` / `AVATAR_BASE_URL` | ที่เก็บไฟล์ avatar (default: `./uploads/avatars`) |

### BFF tuning (`web-api/application-dev.properties`)

| Property | Default | ผลที่ได้ |
| --- | --- | --- |
| `bff.http.max-total` / `bff.http.max-per-route` | `200` / `50` | ขนาด HTTP connection pool |
| `bff.cache.ttl-seconds` | `20` | TTL ของ response cache |
| `bff.cache.cacheable-paths` | `/v1/users/me, /v1/notifications/unread-count, /v1/tenants` | path ที่ cache เฉพาะ safe-GET |
| `bff.cookie.same-site` / `bff.cookie.secure` | `Lax` / `false` | บน HTTPS prod ตั้ง `secure=true` |
| `bff.cors.allowed-origins` | `http://localhost:5173,http://localhost:3000` | origin ของ SPA |

---

## API Reference

ทุก endpoint อยู่ใต้ `/api/v1/` (มี v1 versioning).
ดู endpoints ทั้งหมดได้ผ่าน Swagger UI หรือใน Postman collection ที่ [`core-api/UMS Core API - Full Collection.postman_collection.json`](core-api/UMS%20Core%20API%20-%20Full%20Collection.postman_collection.json)

### Auth — `/api/v1/users`, `/api/v1/sessions`, `/api/v1/account`

- `POST /api/v1/users/login` — username หรือ email + password → JWT + refresh token
- `POST /api/v1/sessions/refresh` — แลก refresh token เป็น JWT ใหม่
- `POST /api/v1/sessions/logout` · `POST /api/v1/sessions/revoke-refresh`
- `POST /api/v1/account/verify-email` · `POST /api/v1/account/forgot-password` · `POST /api/v1/account/reset-password`
- `POST /api/v1/oauth2/callback/exchange` — ทางเลือก BYO-identity OAuth2

### BFF — `/bff/auth`, `/bff/api/v1/**` (web-api)

- `POST /bff/auth/login` → set cookie `rbac_token` + `rbac_refresh`
- `POST /bff/auth/logout` · `POST /bff/auth/refresh` · `GET /bff/auth/oauth2-callback`
- `GET /bff/api/v1/me/full` — aggregation (parallel fan-out)
- `* /bff/api/v1/**` — proxy ทั่วไป (BFF จะใส่ Authorization จาก cookie ให้)

### Users — `/api/v1/users`

- `GET /api/v1/users?page=&size=&sort=` — list แบบ paginate
- `POST /api/v1/users/create` · `PUT /api/v1/users/update` · `DELETE /api/v1/users/{id}`
- `POST /api/v1/users/assign-role` · `POST /api/v1/users/remove-role`
- `GET /api/v1/users/me` · `PUT /api/v1/users/me` · `POST /api/v1/users/me/avatar` (multipart)
- `POST /api/v1/users/admin/unlock` · `POST /api/v1/users/admin/activate`
- `GET|PUT|DELETE /api/v1/users/{userId}/attributes` — user attributes สำหรับ ABAC

### Roles + Permissions — `/api/v1/roles`, `/api/v1/permissions`

- CRUD ปกติ + paginate · `/api/v1/roles/assign-permission` ฯลฯ

### Notifications — `/api/v1/notifications`

- `GET /api/v1/notifications?page=&size=` · `GET /unread-count`
- `PUT /{id}/read` · `PUT /read-all`

### Tenants (เฉพาะ SUPER_ADMIN) — `/api/v1/tenants`

- `GET /api/v1/tenants` · `POST /api/v1/tenants` · `GET /api/v1/tenants/{id}`

### Audit log (เฉพาะ SUPER_ADMIN) — `/api/v1/audit-logs`

- `GET /api/v1/audit-logs?actor=&action=&from=&to=&page=&size=`

---

## Default Accounts (TH)

Password เริ่มต้นทุก account = `password` (อยู่ใน default tenant ชื่อ `default-tenant`)

| Username | Role |
| --- | --- |
| `admin` | SUPER_ADMIN |
| `manager` | USER_MANAGER |
| `viewer` | VIEWER |
| `mod` | MODERATOR |
| `analyst` | ANALYST |
| `support` | SUPPORT |

---

## สำหรับ Developer

```bash
# Backend
cd core-api && ./mvnw test       # context-load + integration กับ PostgreSQL
cd web-api  && ./mvnw test       # 4 tests สำหรับ BFF + auth

# Frontend
cd frontend && npm run lint      # tsc --noEmit
cd frontend && npm run build     # Vite production build
```

**Migrations** — Flyway รัน V1..V10 ตอน startup เอง · จะเพิ่ม migration ก็แค่วาง `V11__*.sql` ไว้ใน [core-api/src/main/resources/db/migration/](core-api/src/main/resources/db/migration/)

**Observability** — Prometheus scrape `/actuator/prometheus` ของทั้งสอง backend services · Grafana dashboard ที่ provision ไว้แล้วอยู่ที่ [docs/observability/grafana-provisioning/dashboards/rbac-ums-overview.json](docs/observability/grafana-provisioning/dashboards/rbac-ums-overview.json) — มี HTTP rate, p95 latency, error rate, JVM heap, Hikari, cache stats, BFF proxy + aggregation timing

**เอกสารเพิ่มเติม:**

- [ROADMAP.md](ROADMAP.md) — สถานะของแต่ละ phase (Phase 1 + 2 done, Phase 3 ใกล้เสร็จ)
- [CONTEXT_ENGINEERING.md](CONTEXT_ENGINEERING.md) — code-review notes
- [frontend/README.md](frontend/README.md) — เอกสารเฉพาะของ SPA
