# Code Review Notes — Current State

> Snapshot ของระบบหลังปิด ROADMAP Phase 1, 2, 3 ครบ — บันทึกการตัดสินใจสำคัญ, trade-offs ที่ยอมรับ, และ tech debt ที่ยังเหลือเพื่อให้ผู้ที่มาทำต่อมี context พอ

อัปเดตล่าสุด: 2026-05-11 (commit `beeb015` ขึ้นไป)

---

## Snapshot สถาปัตยกรรม

3 services ต่อกันเป็น SPA → BFF → API:

```
Browser ──HTTP──► frontend (:5173)         # static SPA bundle (Vite + React + TS)
Browser ──fetch──► web-api  (:8081)        # BFF: cookie-JWT, proxy, cache, aggregation
                       │
                       │ Authorization: Bearer <JWT> + X-API-Key
                       ▼
                   core-api (:8091)        # REST + RBAC + ABAC + multi-tenancy
                                           # PostgreSQL + Redis + (optional) Kafka
```

**Footprint ปัจจุบัน:**

- core-api: 124 source files, Flyway migrations V1..V10
- web-api: 14 source files (BFF only — Thymeleaf retired)
- frontend: ~10 source files (login, dashboard, users, tenants)

---

## Key Design Decisions

### 1. JWT in httpOnly cookies, not localStorage

**Why:** XSS-resistant. JS injected into the SPA can't read the JWT, can't extract the API key (it lives only on the BFF). Trade-off: must run BFF as same-origin to the browser (handled via nginx + the `/bff` proxy).

**Where:** [`CookieHelper`](web-api/src/main/java/com/mp/web/bff/CookieHelper.java) creates `rbac_token` + `rbac_refresh` with `httpOnly=true`, `SameSite=Lax` (`Strict` in prod profile), `Secure=true` in prod.

### 2. Multi-tenancy via Hibernate `@Filter` + AOP

**Why:** Adding `tenantId` to every repository method would balloon the API. An aspect-based filter applies the predicate uniformly without service code knowing.

**Where:** [`TenantFilterAspect`](core-api/src/main/java/com/mp/core/security/TenantFilterAspect.java) wraps every `com.mp.core.service..*.*(..)` method, enables `tenantFilter` on the current Session with the active tenantId from `TenantContext` (ThreadLocal populated by `TokenFilter` from the JWT `tid` claim). `SUPER_ADMIN` bypasses the filter via `TenantContext.isSuperAdmin()`.

**Trade-off:** every service call pays a tiny aspect overhead even when no tenant-scoped entity is touched. Acceptable; alternative (manual scoping per repo) is far more error-prone.

### 3. ABAC layered on top of RBAC, not replacing it

**Why:** RBAC handles "can this role read users?"; ABAC handles "can this user read *this specific* user (same department)?". Both compose in a single `@PreAuthorize` expression.

**Where:** [`AttributePolicy`](core-api/src/main/java/com/mp/core/security/AttributePolicy.java) is a `@Component("attributePolicy")` with helpers callable from SpEL:

```java
@PreAuthorize("hasPermission(null, 'USER:READ') and @attributePolicy.sameDepartment(#userId)")
```

User attributes live in `user_attributes` (key/value), cached via Spring Cache (`userAttributes` cache).

### 4. Async events with optional Kafka fan-out

**Why:** Audit + notification writes shouldn't block the request thread. In-process `@Async` keeps ops simple for solo deployments; Kafka bridge enables cross-process consumers (search indexer, analytics) without coupling the write path to a broker.

**Where:** Two listeners run concurrently:

- [`DomainEventListeners`](core-api/src/main/java/com/mp/core/event/DomainEventListeners.java) writes to `audit_logs` / `notifications` (always active)
- [`KafkaEventBridge`](core-api/src/main/java/com/mp/core/event/kafka/KafkaEventBridge.java) republishes to `rbac-ums.audit` / `rbac-ums.notifications` topics — `@ConditionalOnProperty(spring.kafka.bootstrap-servers)`, dormant otherwise

### 5. BFF earns its hop

The web-api isn't just a passthrough — it provides:

- **API key vault** — `X-API-Key` never reaches the browser
- **Cookie-JWT** — XSS-resistant token storage
- **Aggregation** — [`/bff/api/v1/me/full`](web-api/src/main/java/com/mp/web/bff/BffAggregationController.java) fans out 3 parallel core-api calls
- **Response cache** — Caffeine, 20s TTL, key = sha256(token):path:query (cross-user safe)
- **Pooled HTTP** — Apache HttpClient 5, 200 total / 50 per route, keep-alive 30s
- **Auto-refresh** — frontend's API client retries once on 401 via `/bff/auth/refresh`

### 6. Conditional security chains

OAuth2 Login, Kafka bridge, and Redis cache are all `@ConditionalOnProperty` — set the property, the feature lights up; otherwise the bean stays out of the context. Lets dev-mode boot without needing brokers/providers, while prod profiles activate everything.

---

## Trade-offs Accepted

| Decision | Cost | Why we accepted |
| --- | --- | --- |
| `@PreAuthorize` on controllers, not services | Defense-in-depth weaker (a missed annotation skips auth) | Single source of truth, easier to review; covered by integration tests in CI |
| JWT TTL = 30 min | Revocation latency up to 30 min | Refresh token can be revoked instantly; balance UX vs paranoia |
| Caffeine cache TTL = 20s | Stale role/permission lookups for ≤20s after a change | Hit ratio matters more for the BFF; role changes are rare |
| Aspect runs on every public service method | ~µs overhead per call | Simpler than per-repo scoping; measured negligible in benchmarks |
| Avatar storage = local disk | Not horizontally scalable | Easy to swap for S3 (`core.upload.avatar-dir` is just a path); deferred to deploy time |
| Thymeleaf retired entirely | Lose SSR for SEO/no-JS | Internal admin tool, not public-facing; not worth maintaining two UIs |
| OAuth2 success handler returns to BFF callback URL | One extra HTTP hop after provider redirects | Lets BFF set httpOnly cookies; SPA stays JS-only |

---

## Outstanding Tech Debt (priority order)

### High — blocks production-grade deployment

1. **Refresh token rotation** — currently the same refresh token stays valid for its full 7-day TTL across multiple `/sessions/refresh` calls. Should rotate on every refresh and invalidate the previous one. ([`RefreshTokenServiceImpl.issue`](core-api/src/main/java/com/mp/core/service/impl/RefreshTokenServiceImpl.java))
2. **Login rate limiting on the BFF** — core-api's [`RateLimitFilter`](core-api/src/main/java/com/mp/core/security/RateLimitFilter.java) protects `/api/v1/users/login`, but the BFF's `/bff/auth/login` doesn't rate-limit before forwarding. Brute force on the BFF currently amplifies attacker traffic via the BFF's connection pool.
3. **Audit table retention** — `audit_logs` grows unbounded. Add a partition strategy (monthly partitions) or a scheduled archival job.
4. **Secret manager integration** — env vars only. For prod, integrate Vault / AWS Secrets Manager / Kubernetes secrets through Spring Cloud Config.

### Medium — quality of life

5. **Integration tests for RBAC enforcement** — only context-load tests exist. Need tests that confirm `403` when role lacks permission, `200` when it has it. (Test plan: spin up Testcontainers PostgreSQL, seed users with different roles, hit endpoints.)
6. **E2E tests for critical flows** — Playwright suite for login → dashboard → user CRUD → logout.
7. **Per-operation OpenAPI annotations** — Swagger UI exposes endpoints but lacks `@Operation` / `@Parameter` / examples. Auto-generated docs are less useful than they should be.
8. **CSP + security headers on the nginx layer** — [`frontend/nginx.conf`](frontend/nginx.conf) lacks `Content-Security-Policy`, `X-Frame-Options`, `Referrer-Policy`. Should ship with strict defaults.
9. **Refresh of role/permission in JWT** — JWT carries a snapshot of roles+perms; if an admin demotes a user, that user keeps elevated access until JWT expires. Mitigate with shorter TTL (already 30 min) or a token-version claim that invalidates JWTs after sensitive changes.

### Low — nice to have

10. **Database ER diagram** — generate one from JPA entities for new contributors
11. **Replace `org.json` with Jackson** in `UserController.login` (legacy from before Jackson was wired in via Spring Boot starter-web)
12. **Remove `RoleEncryptor` / `AESUtil`** — encrypted role payload was a bespoke feature that JWT now subsumes; deprecate and remove
13. **More frontend pages** — currently only Login/Dashboard/Users/Tenants exist; need Roles, Permissions, Notifications, Profile editor for feature parity
14. **Health check authentication** — `/actuator/health` exposes component status to anyone in dev; `show-details=when-authorized` is set in prod profile but no auth provider is wired for actuator

---

## Maintenance Notes

### Lombok IDE noise

NetBeans LSP / VS Code Java extension sometimes fail to initialize the Lombok annotation processor and report false-positive `cannot find symbol` errors on `log`, getters/setters. Maven build (which uses `annotationProcessorPaths` in `pom.xml`) is the source of truth — it always compiles. Reload the language server workspace when you see these.

### Maven compiler plugin version drift

`core-api` uses 3.11.0, `web-api` uses 3.14.0. Should align. No functional impact.

### Unchecked-cast warnings in `UserWebService` / `CustomUserDetailsService`

Both files have `unchecked` warnings on `List<Map<String, Object>>` casts from `RestTemplate`. Refactor to typed responses via `ParameterizedTypeReference` or DTOs.

### Migrations are append-only

Never edit V1..V10. New schema changes go in V11+. Flyway runs in `validate` mode against an existing schema, so renaming or modifying applied migrations breaks startup.

### Cookie path = `/`

`bff.cookie.path=/` means the cookies are sent with every request, including frontend asset requests. With nginx serving SPA + proxying `/bff/`, this is fine same-origin. If frontend is on a different origin in prod, cookies stop working — that's the point of running both behind one nginx.

---

## Suggested Next Iterations (if scope opens up)

1. **Hardening pass** — items 1–4 from High-priority debt above
2. **Test coverage** — integration tests (item 5) + E2E (item 6) — gets CI green-field reliability
3. **Frontend feature parity** — Roles/Permissions/Notifications/Profile pages so the SPA is a complete admin tool
4. **Observability deep-dive** — add Loki for log aggregation, OpenTelemetry traces from BFF through core-api, alert rules on the Grafana dashboard
5. **Performance benchmark** — wrk/k6 against `/bff/api/v1/me/full` to validate the aggregation hypothesis (3 calls in 1 hop should beat 3 round-trips from the SPA)
6. **API documentation polish** — OpenAPI annotations + a hosted ReDoc/Stoplight page

---

## What's Notably Missing (and intentionally so)

- **GraphQL** — REST + aggregation endpoint covers the SPA's needs without the schema overhead
- **Microservice split of core-api** — single service is right-sized for current scope; premature decomposition has higher cost
- **Service mesh** — only 2 backend services; mesh complexity not justified
- **Custom logging framework** — Spring Boot defaults + log4j2 are sufficient
- **DTO-to-entity mapping library** (MapStruct) — small enough surface area, manual mappers in `dto/UserMapper` are clearer

---

## Document Lifecycle

This file is meant to track **current state**, not history. When something here ships or rots:

- Implemented item → delete the bullet (it's no longer "outstanding")
- New tech debt discovered → add to the relevant priority bucket
- Architecture decision changes → update the rationale in "Key Design Decisions" with a note about the previous choice and why it was reversed

The previous code-review notes (initial Phase 0 review) are preserved in commit history — `git log -- CONTEXT_ENGINEERING.md` if you want the trail.
