# Architecture Notes — สถานะปัจจุบัน

> สรุปสถาปัตยกรรมหลังปิด ROADMAP Phase 1, 2, 3 ครบ — บันทึกการตัดสินใจสำคัญ, trade-offs ที่ยอมรับ, และ tech debt ที่เหลือ เพื่อให้คนที่เข้ามาทำต่อมี context พอ

อัปเดตล่าสุด: 2026-05-11 (commit `beeb015` ขึ้นไป)

---

## Snapshot

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

**ขนาดโค้ดปัจจุบัน:**

- core-api — 124 source files, Flyway migrations V1..V10
- web-api — 14 source files (BFF อย่างเดียว, Thymeleaf ตัดทิ้งหมดแล้ว)
- frontend — ~10 source files (Login, Dashboard, Users, Tenants)

---

## การตัดสินใจสำคัญ (Key Design Decisions)

### 1. เก็บ JWT ใน httpOnly cookie ไม่ใช่ localStorage

**ทำไม:** กัน XSS ได้ — JS ที่ถูก inject เข้ามาใน SPA อ่าน JWT ไม่ได้, อ่าน X-API-Key ก็ไม่ได้ (key อยู่ที่ BFF เท่านั้น)
**ราคาที่จ่าย:** BFF ต้องอยู่ same-origin กับ browser (จัดการผ่าน nginx + reverse proxy `/bff`)

**ดูได้ที่:** [`CookieHelper`](web-api/src/main/java/com/mp/web/bff/CookieHelper.java) สร้าง cookie `rbac_token` + `rbac_refresh` ด้วย `httpOnly=true`, `SameSite=Lax` (`Strict` ใน prod), `Secure=true` ใน prod

### 2. Multi-tenancy ผ่าน Hibernate `@Filter` + AOP

**ทำไม:** ถ้าให้ทุก repository รับ `tenantId` เป็น argument code จะบวมทุกชั้น — aspect-based filter จะใส่ predicate ให้อัตโนมัติโดย service ไม่ต้องรู้

**ดูได้ที่:** [`TenantFilterAspect`](core-api/src/main/java/com/mp/core/security/TenantFilterAspect.java) wrap method `com.mp.core.service..*.*(..)` ทุกตัว แล้วเปิด `tenantFilter` บน Hibernate Session ด้วย tenantId จาก `TenantContext` (ThreadLocal ที่ `TokenFilter` ใส่ค่ามาจาก JWT claim `tid`) · `SUPER_ADMIN` ข้ามได้ผ่าน `TenantContext.isSuperAdmin()`

**Trade-off:** ทุก service call ต้องผ่าน aspect overhead เล็กน้อยแม้ไม่ได้แตะ entity ที่มี tenant — ยอมรับได้, ถ้าเปลี่ยนเป็น manual scoping ตามแต่ละ repo ก็จะพลาดง่ายกว่า

### 3. ABAC วางทับ RBAC ไม่ได้แทนที่

**ทำไม:** RBAC ตอบ "role นี้อ่าน user ได้ไหม"; ABAC ตอบ "user คนนี้อ่าน user **คนนั้น** ได้ไหม (department เดียวกัน)" — สอง check รวมใน `@PreAuthorize` เดียวกันได้

**ดูได้ที่:** [`AttributePolicy`](core-api/src/main/java/com/mp/core/security/AttributePolicy.java) เป็น `@Component("attributePolicy")` ที่ตัว method เรียกได้จาก SpEL:

```java
@PreAuthorize("hasPermission(null, 'USER:READ') and @attributePolicy.sameDepartment(#userId)")
```

User attributes อยู่ในตาราง `user_attributes` (key/value), cache ผ่าน Spring Cache (`userAttributes`)

### 4. Async events พร้อม optional Kafka fan-out

**ทำไม:** การเขียน audit + notification ไม่ควร block request thread · ใช้ `@Async` ใน-process ทำให้ ops ง่ายตอน deploy เครื่องเดียว · Kafka bridge ปลดล็อก downstream consumer (search indexer, analytics) โดยไม่ผูก write path กับ broker

**ดูได้ที่:** มี 2 listener ทำงานคู่กัน:

- [`DomainEventListeners`](core-api/src/main/java/com/mp/core/event/DomainEventListeners.java) เขียนลง `audit_logs` / `notifications` (active เสมอ)
- [`KafkaEventBridge`](core-api/src/main/java/com/mp/core/event/kafka/KafkaEventBridge.java) ส่งต่อไป topic `rbac-ums.audit` / `rbac-ums.notifications` — `@ConditionalOnProperty(spring.kafka.bootstrap-servers)`, dormant ถ้าไม่ตั้ง property

### 5. BFF ต้อง earn its hop

web-api ไม่ได้เป็นแค่ proxy เปล่า ๆ — มันให้ value 5 จุด:

- **API key vault** — `X-API-Key` ไม่ออกถึง browser
- **Cookie-JWT** — token อยู่ใน httpOnly cookie อ่านจาก JS ไม่ได้
- **Aggregation** — [`/bff/api/v1/me/full`](web-api/src/main/java/com/mp/web/bff/BffAggregationController.java) ยิง 3 endpoint ของ core-api ขนาน
- **Response cache** — Caffeine, TTL 20 วิ, key = sha256(token):path:query (ป้องกัน data leak ข้าม user)
- **Pooled HTTP** — Apache HttpClient 5, 200 total / 50 per route, keep-alive 30 วิ
- **Auto-refresh** — frontend's API client retry ครั้งเดียวเมื่อเจอ 401 ผ่าน `/bff/auth/refresh`

### 6. Conditional security chains

OAuth2 Login, Kafka bridge, Redis cache ทั้ง 3 ใช้ `@ConditionalOnProperty` — ตั้ง property → feature เปิด, ไม่ตั้ง → bean ไม่เข้า context · ทำให้ dev mode boot ได้โดยไม่ต้องมี broker/provider ขณะที่ prod เปิดทุกอย่างได้

---

## Trade-offs ที่ยอมรับ

| การตัดสินใจ | สิ่งที่เสีย | ทำไมยอม |
| --- | --- | --- |
| `@PreAuthorize` ที่ controller ไม่ใช่ที่ service | Defense-in-depth อ่อนลง (ถ้าลืม annotation = หลุด) | Source of truth จุดเดียว, review ง่าย, มี integration test ใน CI ครอบ |
| JWT TTL 30 นาที | revocation latency สูงสุด 30 นาที | Refresh token revoke ได้ทันที — สมดุลระหว่าง UX กับความหวาดระแวง |
| Caffeine cache TTL 20 วิ | role/permission stale ได้สูงสุด 20 วิ | hit ratio สำคัญกว่าสำหรับ BFF · role change ไม่ได้เกิดบ่อย |
| Aspect ทำงานทุก public service method | overhead ระดับไมโครวินาทีต่อ call | ง่ายกว่า scope per repo · benchmark แล้ว negligible |
| Avatar เก็บบน local disk | ไม่ scale แนวนอน | สลับเป็น S3 ง่าย (`core.upload.avatar-dir` คือ path) — เลื่อนไป deploy time |
| ตัด Thymeleaf ทิ้งทั้งหมด | ไม่มี SSR สำหรับ SEO/no-JS | เป็น internal admin tool ไม่ใช่หน้า public · ไม่คุ้ม maintain 2 UI |
| OAuth2 success handler redirect ผ่าน BFF callback | เพิ่ม HTTP hop หลัง provider redirect | ให้ BFF set httpOnly cookie ได้ · SPA ก็ไม่ต้องแตะ token |

---

## Tech Debt ที่เหลือ (เรียงตามความสำคัญ)

### High — บล็อกการ deploy production จริง

1. **Refresh token rotation** — ตอนนี้ refresh token เดิมใช้ได้ตลอด 7 วัน TTL ไม่หมุน · ควรหมุนทุกครั้งที่เรียก `/sessions/refresh` แล้ว invalidate ตัวเก่า · ([`RefreshTokenServiceImpl.issue`](core-api/src/main/java/com/mp/core/service/impl/RefreshTokenServiceImpl.java))
2. **Rate limit ฝั่ง BFF login** — core-api มี [`RateLimitFilter`](core-api/src/main/java/com/mp/core/security/RateLimitFilter.java) ป้องกัน `/api/v1/users/login` แล้ว แต่ BFF's `/bff/auth/login` ไม่ rate limit ก่อน forward · brute force ที่ BFF จะถูกขยายผ่าน connection pool
3. **Audit log retention** — `audit_logs` โตไม่มีขีด · ควรเพิ่ม partitioning (รายเดือน) หรือ scheduled archival job
4. **Secret manager integration** — ใช้แค่ env var · ใน prod ควรต่อ Vault / AWS Secrets Manager / Kubernetes secrets ผ่าน Spring Cloud Config

### Medium — เพิ่มคุณภาพชีวิต

5. **Integration tests สำหรับ RBAC enforcement** — มีแค่ context-load test · ต้องเขียน test ที่ confirm `403` ตอน role ขาด permission, `200` ตอนมี · (test plan: Testcontainers PostgreSQL, seed users คนละ role, hit endpoints)
6. **E2E tests สำหรับ critical flow** — Playwright suite ไล่ login → dashboard → user CRUD → logout
7. **OpenAPI annotations รายต่อ operation** — Swagger UI list endpoints ได้แต่ขาด `@Operation` / `@Parameter` / examples · auto-generated docs ใช้งานได้ไม่เต็มศักยภาพ
8. **Security headers ที่ nginx** — [`frontend/nginx.conf`](frontend/nginx.conf) ขาด `Content-Security-Policy`, `X-Frame-Options`, `Referrer-Policy` · ควร ship พร้อม strict default
9. **Refresh role/permission ใน JWT** — JWT ถือ snapshot ของ roles+perms · ถ้า admin ลด role ให้ user, user คนนั้นยังใช้สิทธิ์เดิมได้จนกว่า JWT จะหมดอายุ · บรรเทาด้วย TTL สั้น (มีแล้ว 30 นาที) หรือ token-version claim ที่ invalidate JWT หลัง sensitive change

### Low — มีก็ดี

10. **ER diagram** — generate จาก JPA entities สำหรับคนใหม่ที่เข้ามา
11. **เปลี่ยน `org.json` เป็น Jackson** ใน `UserController.login` (legacy ก่อนที่ Jackson จะมาผ่าน starter-web)
12. **ลบ `RoleEncryptor` / `AESUtil` ทิ้ง** — encrypted role payload เป็น feature ที่ทำเองตอนยังไม่มี JWT · ตอนนี้ JWT ครอบหมดแล้ว · deprecate และเอาออก
13. **เพิ่มหน้า frontend** — ตอนนี้มีแค่ Login/Dashboard/Users/Tenants · ต้องเพิ่ม Roles, Permissions, Notifications, Profile editor ให้ feature parity
14. **Actuator authentication** — `/actuator/health` แสดง component status ใครเข้าก็เห็นใน dev · prod profile ตั้ง `show-details=when-authorized` แล้วแต่ยังไม่ได้ wire auth provider สำหรับ actuator

---

## Maintenance Notes

### Lombok IDE noise

NetBeans LSP / VS Code Java extension บางที init Lombok annotation processor ไม่สำเร็จ แล้ว report false-positive `cannot find symbol` บน `log`, getter/setter ต่าง ๆ · Maven build (ใช้ `annotationProcessorPaths` ใน `pom.xml`) คือ source of truth — มันจะ compile ได้เสมอ · เจอ error แบบนี้ให้ reload language server workspace

### Maven compiler plugin version drift

`core-api` ใช้ 3.11.0, `web-api` ใช้ 3.14.0 · ควร align · ไม่กระทบ functional

### Unchecked-cast warnings

`UserWebService` / `CustomUserDetailsService` มี warning `unchecked` จาก `List<Map<String, Object>>` cast จาก `RestTemplate` · refactor เป็น typed response ผ่าน `ParameterizedTypeReference` หรือ DTO

### Migration เป็น append-only

อย่าแก้ V1..V10 · schema change ใหม่ไปอยู่ที่ V11+ · Flyway รันแบบ `validate` กับ schema เดิม · rename หรือแก้ไฟล์ที่ apply ไปแล้ว = startup พัง

### Cookie path = `/`

`bff.cookie.path=/` แปลว่า cookie ถูกส่งทุก request รวม frontend asset · เมื่อ nginx serve SPA + proxy `/bff/` แบบ same-origin จะ work ปกติ · ถ้า prod แยก frontend ไปอยู่อีก origin cookie จะใช้ไม่ได้ — นั่นแหละคือเหตุผลที่ run ทั้งสองหลัง nginx เดียวกัน

---

## Iteration ที่แนะนำต่อ (ถ้า scope เปิด)

1. **Hardening pass** — ทำข้อ 1–4 ของ High-priority debt ข้างบน
2. **Test coverage** — integration tests (ข้อ 5) + E2E (ข้อ 6) — ทำ CI ให้น่าเชื่อถือขึ้น
3. **Frontend feature parity** — เพิ่มหน้า Roles/Permissions/Notifications/Profile ให้ SPA เป็น admin tool ครบเครื่อง
4. **Observability deep-dive** — เพิ่ม Loki ทำ log aggregation, OpenTelemetry trace จาก BFF ลง core-api, alert rules บน Grafana dashboard
5. **Performance benchmark** — wrk/k6 ยิง `/bff/api/v1/me/full` เพื่อ validate สมมติฐาน aggregation (3 calls ใน 1 hop ต้องเร็วกว่า 3 round-trip จาก SPA)
6. **API documentation polish** — OpenAPI annotations + ReDoc/Stoplight page

---

## ที่ตั้งใจไม่มี (intentionally NOT in scope)

- **GraphQL** — REST + aggregation endpoint ครอบ use case ของ SPA ได้แล้วโดยไม่ต้องมี schema overhead
- **microservice split ของ core-api** — service เดียวยังพอเหมาะกับ scope ปัจจุบัน · split เร็วเกินไปคือ technical debt อีกแบบ
- **Service mesh** — มี backend แค่ 2 ตัว · ความซับซ้อน mesh ไม่คุ้ม
- **Custom logging framework** — Spring Boot default + log4j2 พอใช้
- **DTO-to-entity mapping library (MapStruct)** — surface area เล็ก · manual mapper ใน `dto/UserMapper` อ่านง่ายกว่า

---

## Lifecycle ของเอกสาร

ไฟล์นี้เก็บ **สถานะปัจจุบัน** ไม่ใช่ประวัติ · ของในนี้เปลี่ยนไปได้:

- ทำเสร็จ → ลบ bullet ออก (ไม่ใช่ outstanding แล้ว)
- เจอ tech debt ใหม่ → เพิ่มเข้า bucket ที่เหมาะสม
- เปลี่ยน design decision → อัปเดต rationale ใน "การตัดสินใจสำคัญ" พร้อมหมายเหตุว่าก่อนหน้านี้ทำยังไง ทำไมถึงเปลี่ยน

Code review notes ตอนเริ่มโปรเจ็ก (Phase 0) เก็บอยู่ใน git history — `git log -- ARCHITECTURE.md CONTEXT_ENGINEERING.md` ถ้าอยากดู
