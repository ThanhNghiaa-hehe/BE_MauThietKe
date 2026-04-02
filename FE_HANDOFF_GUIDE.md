# FE Handoff Guide — Cake Course Backend API (March 27, 2026)

Tài liệu này là **contract chính thức** để FE tích hợp API sau khi BE refactor (design patterns), chuyển Quiz sang API user-safe, và đổi Payment từ VNPay → **PayOS (Option B: redirect qua BE)**.

> Base BE: `http://localhost:8080`
>
> Base FE (mặc định): `http://localhost:5173`
>
> Wrapper chuẩn mọi API:
>
> ```json
> { "success": true, "message": "...", "data": null }
> ```

---

## 0) Auth Header rules
- API cần login: gửi header
  - `Authorization: Bearer <accessToken>`
- Lỗi:
  - `401`: chưa login/thiếu token
  - `403`: sai quyền hoặc không đủ điều kiện truy cập lesson

---

## 1) Auth flow
### 1.1 Register → Verify OTP → Login
- `POST /api/auth/register`
- `POST /api/auth/verify-otp`
- `POST /api/auth/login`

### 1.2 Refresh token
- `POST /api/auth/refresh-token`

### 1.3 Google login
- `POST /api/auth/google`

### 1.4 Forget/reset password
- `POST /api/auth/forget-password`
- `POST /api/auth/verify-otpPassword`
- `POST /api/auth/reset-password`

---

## 2) Courses & Curriculum
### 2.1 Public Courses (không cần token)
- `GET /api/courses`
- `GET /api/courses/{courseId}`

### 2.2 Public Curriculum (không cần token)
- `GET /api/curriculum/course/{courseId}/chapters`
- `GET /api/curriculum/chapters/{chapterId}`
- `GET /api/curriculum/chapters/{chapterId}/lessons`
- `GET /api/curriculum/course/{courseId}/full`

### 2.3 Curriculum for USER (cần token)
- `GET /api/me/curriculum/course/{courseId}`

> FE dùng endpoint này nếu cần dữ liệu unlocked/completed.

---

## 3) Progress
- `POST /api/progress/enroll/{courseId}` (cần token)
- `GET /api/progress/course/{courseId}` (cần token)
- `GET /api/progress/my-courses` (cần token)
- `GET /api/progress/course/{courseId}/chapters` (cần token)

---

## 4) Lesson APIs (ưu tiên cao)
### 4.1 Get lesson (có check access)
- `GET /api/lessons/{lessonId}` (cần token)
- Trả `403` nếu user chưa đủ quyền học.

### 4.2 Check access (ổn định true/false)
- `GET /api/lessons/{lessonId}/access` (cần token)
- `data` luôn boolean: `true/false`

### 4.3 Video progress
- `GET /api/lessons/{lessonId}/progress` (cần token)
- `POST /api/lessons/{lessonId}/progress?percent=0..100` (cần token)
  - Nếu percent ngoài 0..100 → `400` + wrapper `success=false`

### 4.4 Next lesson
- `GET /api/lessons/{lessonId}/next` (cần token)
- Contract hiện có trong `LessonCompleteResponse`:
  - `nextLesson` (object | null)
  - `courseCompleted` (boolean)
  - `totalProgress` (int)
  - `message` (string)
  - `suggestedAction` ("RETAKE_QUIZ" | "COMPLETE_REQUIRED" | "COURSE_DONE" | null)
  - `requiredLessonId` (string | null)

> Lưu ý: FE muốn field `requiresQuiz` riêng thì cần thống nhất thêm ở BE. Hiện BE dùng `suggestedAction=RETAKE_QUIZ`.

### 4.5 Legacy endpoint (compat)
- `POST /api/lessons/quiz/submit` **DEPRECATED nhưng vẫn chạy** để tránh vỡ FE cũ.
- BE sẽ map nội bộ sang hệ thống quiz mới.

---

## 5) Quiz APIs (API mới)
### 5.1 Get quiz by lessonId (user-safe)
- `GET /api/quizzes/lesson/{lessonId}` (cần token)
- BE **check access lesson** trước khi trả quiz.
- Nếu lesson không có quiz: trả `success=true`, `data=null`.

### 5.2 Get quiz by quizId
- `GET /api/quizzes/{quizId}` (cần token)

### 5.3 Submit quiz (schema duy nhất)
- `POST /api/quizzes/submit` (cần token)

Payload chuẩn:
```json
{
  "quizId": "...",
  "answers": [
    { "questionId": "...", "selectedOptions": ["..."] }
  ],
  "timeSpent": 120,
  "startedAt": "2026-03-27T10:30:00"
}
```

> Không dùng schema cũ kiểu `questionIndex/selectedAnswer`.

### 5.4 Attempts + passed
- `GET /api/quizzes/{quizId}/attempts` (cần token)
- `GET /api/quizzes/{quizId}/passed` (cần token)

### 5.5 Admin endpoints (ADMIN only)
- `/api/admin/quizzes/all` chỉ dành ADMIN.

---

## 6) Favorites (USER)
Base path: `/api/favorites/`
- `POST /api/favorites/{userId}`
- `GET /api/favorites/{userId}`
- `DELETE /api/favorites/{userId}/{courseId}`
- `GET /api/favorites/{userId}/check/{courseId}`
- `GET /api/favorites/{userId}/count`
- `PUT /api/favorites/{userId}/{courseId}/select?selected=true|false`
- `DELETE /api/favorites/{userId}/clear`

---

## 7) Payment — PayOS (Option B: redirect qua BE)
### 7.1 Create payment
- `POST /api/payment/payos/create` (cần token)

Body:
```json
{ "courseIds": ["<courseId>"], "orderInfo": "Thanh toan khoa hoc" }
```

Response `data` tối thiểu:
- `checkoutUrl` (ưu tiên)
- `paymentUrl` (alias)
- `paymentId`
- `orderCode`

FE flow:
1) Call create → lấy `checkoutUrl`
2) Redirect browser tới `checkoutUrl`

### 7.2 PayOS redirect (browser)
PayOS sẽ redirect về BE:
- `GET /api/payment/payos/return`
- `GET /api/payment/payos/cancel`

BE sẽ **302 redirect** về FE tương ứng:
- `FE/payment/return?...`
- `FE/payment/cancel?...`

Query param chuẩn hoá (BE → FE):
- `status`: `SUCCESS|PENDING|CANCELLED`
- `code`: (nếu có) ví dụ `00`
- `cancel`: `true|false`
- `orderCode`: (nếu có)

> Trạng thái chính thức vẫn dựa webhook + DB. Return chỉ dùng để hiển thị và FE poll.

### 7.3 Webhook (server-to-server)
- `POST /api/payment/payos/webhook` (public)
- BE verify signature, xử lý idempotent.
- Nếu success → update `PaymentStatus.SUCCESS` và emit event enroll course.

### 7.4 Poll after return
- `GET /api/payment/my-payments/success` (cần token)
- `data` là list `Payment` gồm:
  - `id` (paymentId)
  - `providerOrderCode` (orderCode)
  - `courses[].courseId`
  - `amount`
  - `status` (SUCCESS)

---

## 8) E2E checklist bắt buộc trước bàn giao
### Case SUCCESS
1) FE login lấy token
2) Call `POST /api/payment/payos/create` lấy `checkoutUrl` + `orderCode`
3) Thanh toán thành công
4) Browser về đúng `http://localhost:5173/payment/return` (không còn 5174)
5) FE poll `GET /api/payment/my-payments/success` thấy giao dịch SUCCESS (match by orderCode/paymentId)
6) FE gọi `GET /api/progress/my-courses` thấy course vừa mua
7) Lesson access: `GET /api/lessons/{id}` không bị 403 (đã unlock)

### Case CANCEL
1) Thanh toán huỷ
2) Browser về đúng `http://localhost:5173/payment/cancel`
3) Payment không xuất hiện trong `my-payments/success`

---

## 9) Local config notes (cực quan trọng)
Sau khi đổi return/cancel (Option B), phải:
1) Restart BE
2) Tạo payment mới để link mới dùng return/cancel mới

ENV khuyên dùng:
- `FRONTEND_BASE_URL=http://localhost:5173`
- `PAYOS_RETURN_URL=http://localhost:8080/api/payment/payos/return`
- `PAYOS_CANCEL_URL=http://localhost:8080/api/payment/payos/cancel`
- `PAYOS_WEBHOOK_URL=http://localhost:8080/api/payment/payos/webhook`

---

## 10) Migration notes
- FE không dùng `/api/admin/quizzes/all` để lấy quiz nữa.
- Dùng `GET /api/quizzes/lesson/{lessonId}`.
- `POST /api/lessons/quiz/submit` còn chạy để compat, nhưng FE nên migrate sang `POST /api/quizzes/submit`.
