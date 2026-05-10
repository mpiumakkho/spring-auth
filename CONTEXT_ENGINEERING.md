# Context Engineering — แผนพัฒนาต่อระบบ User Management System

เอกสารนี้สรุปบริบท ข้อสังเกตสำคัญ และแผนพัฒนาต่อของระบบ ตามการทบทวนโค้ดจริงใน `core-api` และ `web-api` พร้อมแนวทางปรับปรุงเป็นลำดับงานเล็กที่ส่งมอบได้ต่อเนื่อง

## ข้อสังเกตสำคัญ

- **Core API เปิดสิทธิ์ทุกคำขอ**: การตั้งค่า `SecurityFilterChain` อนุญาต `permitAll()` สำหรับทุกเส้นทาง ยังไม่มีการป้องกันที่ขอบบริการ
- **เส้นทาง (path) ใน Web API ไม่สอดคล้องกับ context-path `/demo`**: ใน `web-api` มีการตั้งค่า `server.servlet.context-path=/demo` แต่ `SecurityConfig` อนุญาตหน้า `/login` และกำหนด `loginPage`/`loginProcessingUrl` เป็น `/` ขณะที่ `SessionFilter` ใช้รายการยกเว้นเป็น `/demo/...` ทำให้ไม่ตรงกัน
- **คีย์ AES ฮาร์ดโค้ดใน `AESUtil`**: ใช้คีย์คงที่ในซอร์ส ไม่เหมาะสมกับการใช้งานจริง ควรย้ายไปจัดการผ่าน secrets/environment และทบทวนความจำเป็นของการเข้ารหัสบทบาท
- **บั๊กชุดบทบาทที่อนุญาตใน `SessionAuthSuccessHandler`**: ค่าหนึ่งถูกรวมเป็นสตริงยาว (`"ROLE_SUPPORT, ROLE_USER"`) แทนที่จะเป็น 2 ค่าแยกกัน
- **รูปแบบการตอบกลับจาก Login**: ส่งคืนเอนทิตีบทบาทโดยตรงและส่ง JSON เป็นสตริง ควรใช้ DTO สม่ำเสมอและส่งคืนอ็อบเจ็กต์ JSON โดยตรง
- **คอนฟิกฐานข้อมูล/สคีมา**: ฮาร์ดโค้ดในไฟล์ dev (`spring.datasource.*`, `hibernate.default_schema=sample_app`); Flyway มีเฉพาะสคริปต์แก้ไขตาราง `user_sessions` แต่ยังไม่มี baseline ของสคีมา/ตารางทั้งหมด

## งานที่ทำได้เร็ว (1–2 วัน)

- **ปรับ `web-api` ให้สอดคล้องกับ `/demo`**
  - ตั้งค่า `loginPage` และ `loginProcessingUrl` เป็น `/demo/login`
  - ปรับ `requestMatchers` ให้ใช้ `/demo/**` ให้ตรงกับ `SessionFilter`
- **แก้บั๊ก `allowedRoles`** ใน `SessionAuthSuccessHandler` ให้แยก `"ROLE_SUPPORT"` และ `"ROLE_USER"`
- **ปรับการตอบกลับของ Login**
  - ส่งคืนรายชื่อบทบาท (role names) แทนเอนทิตีบทบาท ใช้ `UserResponseDTO`
  - ส่งคืนอ็อบเจ็กต์ JSON โดยตรง ไม่ห่อเป็นสตริง
- **แยกค่าคอนฟิกอ่อนไหว** ออกเป็น environment variables และอัปเดตเอกสารวิธี override

## ระยะสั้น (ภายใน 1 สัปดาห์)

- **เพิ่มความปลอดภัยให้ Core API**
  - เปิดใช้ method-level security และกำหนด `@PreAuthorize` ให้ endpoint สิทธิ์สูง
  - เพิ่มการยืนยันตัวตนระหว่างบริการ (shared secret header) ปิดกั้นการเรียกตรงจากภายนอก
- **เสถียรภาพของเซสชัน**
  - กำหนดค่า `lastActivityAt` ตอนสร้างและอัปเดตในขั้น validate (อาจตัด `keep-alive` หาก validate อัปเดตแล้ว)
  - เพิ่มดัชนีในฐานข้อมูลที่ `user_sessions(token)` และ `user_sessions(last_activity_at)`
- **ความสม่ำเสมอของ DTO/Response**
  - ใช้ `UserRequestDTO`/`UserResponseDTO` ใน `UserController` แทนการคืนเอนทิตี/สตริง
  - จัดรูปแบบ Response ให้สม่ำเสมอ (เช่น success, data, error)

## ระยะกลาง (1–2 สัปดาห์)

- **โครงสร้างสคีมาและไมเกรชัน**
  - เปลี่ยน `spring.jpa.hibernate.ddl-auto` เป็น `validate`
  - เพิ่ม Flyway baseline (`V1__init.sql`) ครอบคลุมสคีมา `sample_app` และตารางทั้งหมด แล้วย้ายสคริปต์แก้ไขปัจจุบันเป็นเวอร์ชันถัดไป
- **ขอบเขตการยืนยันตัวตน**
  - ทางเลือก A (เปลี่ยนแปลงน้อย): คงโทเค็นเซสชัน แต่บังคับ shared secret และทำ rate-limit สำหรับ login/validate
  - ทางเลือก B (แนะนำระยะยาว): ออก JWT ที่ `core-api` และตรวจสอบใน `web-api` ลดการเรียกตรวจสอบข้ามบริการ
- **การเข้ารหัส/ความลับ**
  - เลิกเข้ารหัสบทบาทด้วย AES หรือย้ายคีย์ไปที่ secrets ที่ตั้งค่าได้ หากยังจำเป็นให้พิจารณาการ “ลงนาม (sign)” มากกว่าการ “เข้ารหัส (encrypt)”
- **RBAC เชิงละเอียด**
  - เพิ่มการตรวจสอบสิทธิ์ระดับ permission ใน `core-api` (เกินจาก role อย่างเดียว) ที่ชั้นบริการหรือผ่าน `@PreAuthorize` แบบตรวจ permission

## การทดสอบและคุณภาพ (Testing & Quality)

- เพิ่ม integration tests สำหรับ: login, ตรวจสอบ/หมดอายุเซสชัน, ข้อจำกัด RBAC (ทั้งสองโมดูล)
- เพิ่ม unit tests ให้ `UserServiceImpl` และ `UserSessionServiceImpl`
- เพิ่มการทดสอบความปลอดภัย: ยืนยันว่า endpoint ที่ต้องป้องกันถูกบล็อกจริงเมื่อเปิด security

## ด้านปฏิบัติการ (DevOps)

- เพิ่ม Docker Compose สำหรับ Postgres และไฟล์ `.env` ตัวอย่าง
- อัปเดต README เรื่องคอนฟิกตามสภาพแวดล้อมและการจัดการ secrets
- ป้องกัน Actuator endpoints ในโปรไฟล์ที่ไม่ใช่ dev

## แผนส่งมอบเป็นชุดเล็ก (ลำดับ)

1. ปรับ Security ใน `web-api` ให้ตรงกับ `/demo` และแก้บั๊ก `allowedRoles`
2. ปรับ login response ให้ใช้ DTO (ไม่รั่วไหลเอนทิตี) และส่ง JSON อ็อบเจ็กต์
3. เพิ่ม method-level security ใน `core-api` และ inter-service auth แบบ shared secret
4. ปรับปรุงบริการเซสชัน/ดัชนีฐานข้อมูล
5. เพิ่ม Flyway baseline และเปลี่ยน JPA เป็น `validate`
6. แยกคอนฟิกอ่อนไหวออกจากซอร์ส + อัปเดตเอกสาร
7. เพิ่มชุดทดสอบหลักสำหรับ auth/session/RBAC

## หมายเหตุด้านความปลอดภัยและคอนฟิก

- หลีกเลี่ยงการฮาร์ดโค้ดข้อมูลอ่อนไหว (DB credentials, keys) ในซอร์สโค้ด
- ปิดกั้น Core API จากการเข้าถึงภายนอกโดยตรงในสภาพแวดล้อมที่ไม่ใช่ dev (เช่น เฉพาะ subnet ภายใน + shared secret)
- ตรวจทานนโยบาย timeout และการจัดการเซสชันให้เหมาะกับโหลดและความเสี่ยงของระบบ
