# rbac-ums Development Roadmap

> Last updated: 2026-05-10

แผนพัฒนาต่อยอดระบบ User Management System (RBAC) แบ่งเป็น 3 phases

**Status Legend:** `[ ]` Not Started | `[~]` In Progress | `[x]` Done

---

## Phase 1: Foundation (Q2 2026)

เสริมความแข็งแรงพื้นฐานก่อน deploy production

### 1.1 Audit Log

| | |
|---|---|
| **Status** | `[x]` Done |
| **Complexity** | Medium |
| **Depends on** | - |

**Problem:** ไม่มีบันทึกว่าใครทำอะไร เมื่อไหร่ ตรวจสอบย้อนหลังไม่ได้

**Scope:**
- สร้าง `audit_logs` table (actor, action, target, timestamp, ip_address, detail)
- สร้าง `AuditService` บันทึก event ทุก create/update/delete
- สร้าง `GET /api/audit-logs` endpoint (SUPER_ADMIN only)
- สร้างหน้า UI แสดง audit log

**Files:**
- `core-api/.../entity/AuditLog.java` (new)
- `core-api/.../service/AuditService.java` (new)
- `core-api/.../service/impl/UserServiceImpl.java` (modify)
- `core-api/.../service/impl/RoleServiceImpl.java` (modify)
- `core-api/.../resources/db/migration/V2__add_audit_logs.sql` (new)

**Done when:**
- ทุก create/update/delete operation ถูกบันทึกใน audit_logs
- Admin ดู audit log ผ่าน UI ได้ พร้อม filter by actor/action/date

---

### 1.2 Pagination

| | |
|---|---|
| **Status** | `[x]` Done |
| **Complexity** | Low |
| **Depends on** | - |

**Problem:** `getAllUsers/Roles/Permissions` return ข้อมูลทั้ง table ข้อมูลเยอะจะช้าและกิน memory

**Scope:**
- เปลี่ยน repository ใช้ `Pageable` parameter
- แก้ service/controller รับ `page`, `size`, `sort`
- แก้ web-api แสดง pagination controls

**Files:**
- `core-api/.../repository/UserRepository.java` (modify)
- `core-api/.../service/UserService.java` (modify)
- `core-api/.../controller/UserController.java` (modify)
- `web-api/.../templates/users/list.html` (modify)

**Done when:**
- API return `Page<T>` พร้อม totalElements, totalPages, pageNumber
- UI แสดง pagination controls ทำงานถูกต้อง
- Default page size = 20

---

### 1.3 Password Policy & Account Lockout

| | |
|---|---|
| **Status** | `[x]` Done |
| **Complexity** | Medium |
| **Depends on** | - |

**Problem:** ไม่มี validation ความแข็งแรงของ password, ไม่ล็อค account หลัง login ผิดหลายครั้ง

**Scope:**
- สร้าง `PasswordPolicyValidator` (min 8 chars, uppercase, lowercase, digit, special)
- เพิ่ม `failedLoginAttempts`, `lockedUntil` fields ใน User entity
- ล็อค account หลัง login ผิด 5 ครั้ง (lockout 15 min)
- สร้าง API สำหรับ admin unlock account

**Files:**
- `core-api/.../entity/User.java` (modify - add fields)
- `core-api/.../validation/PasswordPolicyValidator.java` (new)
- `core-api/.../controller/UserController.java` (modify - login logic)
- `core-api/.../resources/db/migration/V3__add_account_lockout.sql` (new)

**Done when:**
- Password ที่ไม่ผ่าน policy ถูก reject พร้อมบอกเหตุผล
- Account ถูกล็อคหลัง login ผิด 5 ครั้ง, auto unlock หลัง 15 min
- Admin unlock ผ่าน API ได้

---

### 1.4 Refresh Token

| | |
|---|---|
| **Status** | `[x]` Done |
| **Complexity** | Medium |
| **Depends on** | - |

**Problem:** token หมดอายุแล้ว user ต้อง login ใหม่ทุกครั้ง

**Scope:**
- สร้าง `refresh_tokens` table
- Login response ส่ง `accessToken` (30 min) + `refreshToken` (7 days)
- สร้าง `POST /api/sessions/refresh` endpoint
- web-api SessionFilter auto-refresh เมื่อ access token หมด

**Files:**
- `core-api/.../entity/RefreshToken.java` (new)
- `core-api/.../service/RefreshTokenService.java` (new)
- `core-api/.../controller/SessionController.java` (modify)
- `core-api/.../resources/db/migration/V4__add_refresh_tokens.sql` (new)

**Done when:**
- User ไม่ถูก force logout เมื่อ access token หมด (ถ้า refresh token ยังอยู่)
- Refresh token สามารถ revoke ได้

---

### 1.5 Input Validation

| | |
|---|---|
| **Status** | `[x]` Done |
| **Complexity** | Low-Medium |
| **Depends on** | - |

**Problem:** หลาย endpoint รับ `@RequestBody String` แล้ว parse JSON เอง ไม่มี validation, เสี่ยง injection

**Scope:**
- สร้าง Request DTO ทุก endpoint ที่ยังใช้ raw String/Map
- เพิ่ม `@Valid` + Bean Validation (`@NotBlank`, `@Email`, `@Size`)
- สร้าง `GlobalExceptionHandler` สำหรับ validation errors

**Files:**
- `core-api/.../dto/` (new DTOs)
- `core-api/.../controller/UserController.java` (modify)
- `core-api/.../controller/RoleController.java` (modify)
- `core-api/.../exception/GlobalExceptionHandler.java` (new)

**Done when:**
- ไม่มี controller ที่รับ raw `String` หรือ `JSONObject` เป็น request body
- Invalid input return 400 พร้อม field-level error messages

---

## Phase 2: Features (Q3 2026)

เพิ่ม feature ที่ user ต้องการ

### 2.1 JWT Token

| | |
|---|---|
| **Status** | `[x]` Done |
| **Complexity** | Medium |
| **Depends on** | 1.4 Refresh Token |

**Problem:** UUID token ต้อง query DB ทุก request เพิ่ม latency

**Scope:**
- เพิ่ม `jjwt` dependency
- สร้าง `JwtService` (generate/validate)
- JWT payload: userId, username, roles, permissions, exp
- แก้ TokenFilter validate JWT แทน DB lookup
- Refresh token ยังเก็บใน DB เพื่อ revoke ได้

**Done when:**
- Token validation ไม่ query DB (ยกเว้น refresh)
- เปลี่ยน token format ได้โดยไม่กระทบ web-api

---

### 2.2 Email Verification & Password Reset

| | |
|---|---|
| **Status** | `[x]` Done |
| **Complexity** | Medium |
| **Depends on** | 1.3 Password Policy |

**Problem:** สมัครแล้วใช้งานได้เลย ไม่มียืนยัน email, ไม่มี forgot password

**Scope:**
- เพิ่ม `spring-boot-starter-mail`
- สร้าง `EmailService`, `email_verifications` table, `password_resets` table
- Endpoints: verify-email, forgot-password, reset-password
- UI สำหรับ forgot/reset password

**Done when:**
- User ต้องยืนยัน email ก่อน account เป็น active
- Forgot password flow ทำงานครบ (request -> email -> reset -> login)

---

### 2.3 OAuth2 / SSO

| | |
|---|---|
| **Status** | `[~]` In Progress (BYO-identity exchange shipped; full Spring OAuth2 client config requires provider creds) |
| **Complexity** | High |
| **Depends on** | 2.1 JWT Token |

**Problem:** รองรับแค่ username/password login

**Scope:**
- เพิ่ม `spring-boot-starter-oauth2-client`
- Config OAuth2 providers (Google, GitHub, Azure AD)
- Map external user -> internal user พร้อม auto-create
- สร้าง `user_oauth_links` table

**Done when:**
- User login ผ่าน Google ได้ พร้อม auto-create account
- Account ที่ผูก OAuth แล้วยังใช้ username/password login ได้

---

### 2.4 User Profile & Avatar

| | |
|---|---|
| **Status** | `[x]` Done |
| **Complexity** | Low |
| **Depends on** | - |

**Problem:** user แก้ไขข้อมูลตัวเองไม่ได้ ไม่มีรูป profile

**Scope:**
- เพิ่ม `avatarUrl`, `phone`, `bio` fields ใน User entity
- Upload avatar API + storage (local/S3)
- หน้า profile ใน web-api
- user แก้ได้แค่ profile ตัวเอง

**Done when:**
- User edit profile + upload avatar ได้
- User ดู profile คนอื่นได้แต่แก้ไม่ได้

---

### 2.5 Notification System

| | |
|---|---|
| **Status** | `[x]` Done |
| **Complexity** | Medium |
| **Depends on** | 1.1 Audit Log |

**Problem:** ไม่มีแจ้งเตือนเมื่อเกิดเหตุการณ์สำคัญ

**Scope:**
- สร้าง `notifications` table
- Trigger: role changed, account locked/unlocked, password reset
- API: list notifications, mark as read
- UI: notification bell ใน header

**Done when:**
- User เห็น notification เมื่อ role ถูกเปลี่ยน
- Admin เห็น notification เมื่อมี account ถูก lock

---

### 2.6 API Versioning

| | |
|---|---|
| **Status** | `[x]` Done |
| **Complexity** | Low |
| **Depends on** | - |

**Problem:** เปลี่ยน API แล้ว client เก่าพัง

**Scope:**
- เปลี่ยน base path เป็น `/api/v1/`
- สร้าง version strategy (URL-based)
- แก้ web-api เรียก `/api/v1/`

**Done when:**
- ทุก endpoint อยู่ใต้ `/api/v1/`
- เพิ่ม v2 endpoint ได้โดยไม่กระทบ v1

---

## Phase 3: Scale & Production (Q4 2026+)

เตรียมระบบสำหรับ production scale

### 3.1 Caching (Redis)

| | |
|---|---|
| **Status** | `[ ]` Not Started |
| **Complexity** | Medium |
| **Depends on** | 2.1 JWT Token |

**Problem:** ทุก request query DB สำหรับ roles/permissions

**Scope:**
- เพิ่ม `spring-boot-starter-data-redis`
- Cache user roles/permissions, invalidate on change
- `@Cacheable`, `@CacheEvict` annotations

**Done when:**
- Role/permission lookups served from cache
- Cache invalidation ทำงานถูกต้องเมื่อ assign/remove role

---

### 3.2 Event-Driven Architecture

| | |
|---|---|
| **Status** | `[ ]` Not Started |
| **Complexity** | High |
| **Depends on** | 1.1 Audit Log, 2.5 Notification |

**Problem:** audit log, notification ทำ synchronous เพิ่ม latency ให้ main flow

**Scope:**
- เพิ่ม message broker (Kafka/RabbitMQ)
- Publish events: UserCreated, RoleAssigned, LoginFailed
- Consumers: AuditLogConsumer, NotificationConsumer

**Done when:**
- Audit log + notification ไม่เพิ่ม latency ให้ main API response
- Events ไม่หาย แม้ consumer down ชั่วคราว

---

### 3.3 CI/CD Pipeline

| | |
|---|---|
| **Status** | `[ ]` Not Started |
| **Complexity** | Medium |
| **Depends on** | - |

**Problem:** build, test, deploy ทำ manual

**Scope:**
- GitHub Actions: test on PR, build Docker on merge, deploy to staging
- Code quality (SonarQube/Checkstyle)

**Done when:**
- PR ถูก auto-test ก่อน merge
- Merge to main auto-deploy to staging

---

### 3.4 Monitoring (Prometheus + Grafana)

| | |
|---|---|
| **Status** | `[ ]` Not Started |
| **Complexity** | Medium |
| **Depends on** | 3.3 CI/CD |

**Problem:** ไม่มี visibility เรื่อง performance, error rate

**Scope:**
- เพิ่ม `micrometer-registry-prometheus`
- Grafana dashboard: latency, error rate, active sessions
- Alerting rules

**Done when:**
- Dashboard แสดง real-time metrics
- Alert เมื่อ error rate > threshold

---

### 3.5 Multi-tenancy

| | |
|---|---|
| **Status** | `[ ]` Not Started |
| **Complexity** | High |
| **Depends on** | 2.6 API Versioning |

**Problem:** รองรับแค่ organization เดียว

**Scope:**
- เพิ่ม `tenantId` ใน entities
- `TenantFilter` ใส่ context ทุก request
- Hibernate `@Filter` สำหรับ tenant isolation

**Done when:**
- 2 tenants ใช้ระบบเดียวกัน เห็นแค่ข้อมูลตัวเอง
- SUPER_ADMIN ข้าม tenant ได้

---

### 3.6 ABAC (Attribute-Based Access Control)

| | |
|---|---|
| **Status** | `[ ]` Not Started |
| **Complexity** | High |
| **Depends on** | 3.5 Multi-tenancy |

**Problem:** RBAC ไม่ละเอียดพอ เช่น "ดูได้เฉพาะ department ตัวเอง"

**Scope:**
- Policy engine รองรับ attribute-based rules
- User attributes (department, location, level)
- ขยาย `PermissionEvaluator`

**Done when:**
- สร้าง policy "user อ่านข้อมูลได้เฉพาะ department ตัวเอง" ผ่าน config ได้

---

### 3.7 Frontend Migration (React/Vue)

| | |
|---|---|
| **Status** | `[ ]` Not Started |
| **Complexity** | High |
| **Depends on** | 2.6 API Versioning, 3.1 Caching |

**Problem:** Thymeleaf จำกัด UX ทุกอย่างเป็น full page reload

**Scope:**
- สร้าง SPA frontend (React/Vue)
- เรียก core-api โดยตรง
- เพิ่ม CORS configuration
- web-api เป็น BFF หรือเลิกใช้

**Done when:**
- SPA ทำงานได้ครบทุก feature เท่า Thymeleaf version
- Response time (perceived) ดีขึ้น

---

## Dependency Graph

```
Phase 1 (Foundation)          Phase 2 (Features)           Phase 3 (Scale)
                                                          
1.1 Audit Log ─────────────> 2.5 Notification ──────────> 3.2 Event-Driven
1.2 Pagination                                            
1.3 Password Policy ───────> 2.2 Email/Reset              
1.4 Refresh Token ─────────> 2.1 JWT ───> 2.3 OAuth2 ──> 3.1 Redis Cache
1.5 Input Validation                                      
                              2.4 User Profile             
                              2.6 API Versioning ────────> 3.5 Multi-tenancy
                                                            └──> 3.6 ABAC
                                                          3.3 CI/CD
                                                            └──> 3.4 Monitoring
                                                          3.7 Frontend Migration
```

## Priority Matrix

```
          Urgent                    Not Urgent
        ┌───────────────────────┬───────────────────────┐
        │ 1.1 Audit Log         │ 2.1 JWT Token         │
  High  │ 1.2 Pagination        │ 2.2 Email/Reset       │
        │ 1.3 Password/Lockout  │ 2.3 OAuth2/SSO        │
        │ 1.5 Input Validation  │ 2.5 Notifications     │
        ├───────────────────────┼───────────────────────┤
        │ 1.4 Refresh Token     │ 3.1 Redis Cache       │
  Low   │ 2.6 API Versioning    │ 3.2 Event-Driven      │
        │                       │ 3.3 CI/CD             │
        │                       │ 3.4 Monitoring        │
        │                       │ 3.5-3.7 Advanced      │
        └───────────────────────┴───────────────────────┘
```
