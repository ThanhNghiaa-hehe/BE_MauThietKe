# FE API Flow Guide (verified endpoints) — March 27, 2026

Tài liệu này giúp FE đi theo **đúng luồng API** và **xác thực (Auth/Role)** sau khi BE refactor (thêm nhiều mẫu thiết kế) và **đổi Payment từ VNPay → PayOS**.

> Base URL: `{{baseUrl}}` (default: `http://localhost:8080`)
>
> Header dùng cho các API cần đăng nhập:
>
> - `Authorization: Bearer <accessToken>`

---

## 0) Response format chung
Hầu hết API trả về wrapper:

```json
{
  "success": true,
  "message": "...",
  "data": { }
}
```

FE nên xử lý theo:
- `success === true`: dùng `data`
- `success === false`: hiển thị `message`

---

## 1) Auth flow (User)
### 1.1 Register → OTP → Login
1) **Register** (tạo tài khoản, nhận `otpToken`)
- `POST /api/auth/register`
- Body: `RegisterRequest`

```json
{
  "email": "student_...@example.com",
  "password": "123456",
  "fullname": "Student One",
  "phoneNumber": "0123456789"
}
```

2) **Verify OTP** (xác thực email)
- `POST /api/auth/verify-otp`
- Body: `VerifyOtpRequest`

```json
{
  "token": "<otpToken>",
  "otp": "123456"
}
```

3) **Login** (lấy accessToken)
- `POST /api/auth/login`
- Body: `LoginRequest`

```json
{
  "email": "...",
  "password": "123456"
}
```

Kết quả: `data.accessToken` hoặc `data.token` → FE lưu và gắn vào header `Authorization: Bearer ...`

### 1.2 Refresh token
- `POST /api/auth/refresh-token`
- Lưu ý: BE đọc refresh token từ request/cookie (tuỳ config). FE cứ gọi endpoint khi `401`.

### 1.3 Quên mật khẩu
1) `POST /api/auth/forget-password`
```json
{ "email": "..." }
```

2) `POST /api/auth/verify-otpPassword`
```json
{ "token": "<otpToken>", "otp": "123456" }
```

3) `POST /api/auth/reset-password`
```json
{ "email": "...", "newPassword": "12345678" }
```

### 1.4 Login Google
- `POST /api/auth/google`
```json
{ "idToken": "<GOOGLE_ID_TOKEN>" }
```

---

## 2) Lấy thông tin user hiện tại
### 2.1 Lấy userId (USER/ADMIN)
- `GET /api/users/find-userId`
- Auth: **Bắt buộc**

> Endpoint trả về `Optional<User>` trong `data`. Thường id nằm ở: `data.id`

### 2.2 Change password (USER/ADMIN)
- `PUT /api/users/change-password`
- Body: `ChangePasswordRequest`
```json
{ "password": "old", "newPassword": "new" }
```

### 2.3 Update profile (multipart)
- `PUT /api/users/update-user`
- Content-Type: `multipart/form-data`
- Parts:
  - `request`: **string JSON** theo `UpdateUserRequest`
  - `avatarFile`: file (optional)

`request` JSON mẫu:
```json
{
  "fullname": "...",
  "phoneNumber": "...",
  "gender": "MALE|FEMALE|OTHER",
  "dateOfBirth": "2026-03-27",
  "avatarUrl": "(optional)",
  "address": {
    "street": "...",
    "ward": "...",
    "district": "...",
    "city": "..."
  }
}
```

---

## 3) Courses & Curriculum (Public vs Logged-in)
### 3.1 Public courses
- `GET /api/courses` (published)
- `GET /api/courses/{courseId}`

### 3.2 Public curriculum (xem trước)
- `GET /api/curriculum/course/{courseId}/chapters`
- `GET /api/curriculum/chapters/{chapterId}`
- `GET /api/curriculum/chapters/{chapterId}/lessons`
- `GET /api/curriculum/course/{courseId}/full`

### 3.3 Curriculum cho user (có unlocked/completed)
- `GET /api/me/curriculum/course/{courseId}`
- Auth: **Bắt buộc**

---

## 4) Progress flow (sau khi mua khoá học)
Các API này dùng để hiển thị “My Courses”, tiến độ học, chapter progress.

### 4.1 Enroll course (khởi tạo progress)
- `POST /api/progress/enroll/{courseId}`
- Auth: **Bắt buộc**

> Thường được gọi sau khi payment thành công (server cũng có listener tự enroll, nhưng FE có thể gọi để chắc chắn nếu cần).

### 4.2 Lấy progress của course
- `GET /api/progress/course/{courseId}`
- Auth: **Bắt buộc**

### 4.3 My courses
- `GET /api/progress/my-courses`
- Auth: **Bắt buộc**

### 4.4 Chapter progress
- `GET /api/progress/course/{courseId}/chapters`
- Auth: **Bắt buộc**

---

## 5) Lesson flow (xem bài học + cập nhật tiến độ)
> Lưu ý: `GET /api/lessons/{id}` có **check quyền truy cập**. Nếu chưa mua/chưa unlock sẽ trả `403`.

### 5.1 Get lesson (có kiểm tra access)
- `GET /api/lessons/{lessonId}`
- Auth: **Bắt buộc**
- Error: `403` nếu không đủ quyền

### 5.2 Check access trước khi navigate
- `GET /api/lessons/{lessonId}/access`
- Auth: **Bắt buộc**
- `data: true/false`

### 5.3 Like lesson
- `POST /api/lessons/{lessonId}/like`
- (Hiện controller không yêu cầu auth nhưng thực tế nên yêu cầu. Nếu BE đã bật security toàn cục thì vẫn cần token.)

### 5.4 Mark complete
- `POST /api/lessons/{lessonId}/complete`
- Auth: **Bắt buộc**

### 5.5 Video progress
- `GET /api/lessons/{lessonId}/progress`
- `POST /api/lessons/{lessonId}/progress?percent=0..100`
- Auth: **Bắt buộc**

### 5.6 Next lesson
- `GET /api/lessons/{lessonId}/next`
- Auth: **Bắt buộc**

### 5.7 Deprecated endpoint
- `POST /api/lessons/quiz/submit` **DEPRECATED**
- FE phải dùng quiz API mới bên dưới.

---

## 6) Quiz flow (API mới)
### 6.1 Lấy quiz cho học viên
- `GET /api/quizzes/{quizId}`
- Auth: **Bắt buộc**

### 6.2 Nộp bài
- `POST /api/quizzes/submit`
- Auth: **Bắt buộc**
- Body: `QuizSubmitRequest`

```json
{
  "quizId": "...",
  "answers": [
    {
      "questionId": "...",
      "selectedOptions": ["optionId1", "optionId2"]
    }
  ],
  "timeSpent": 120,
  "startedAt": "2026-03-27T10:30:00"
}
```

### 6.3 Lịch sử attempts
- `GET /api/quizzes/{quizId}/attempts`
- Auth: **Bắt buộc**

### 6.4 Đã pass chưa?
- `GET /api/quizzes/{quizId}/passed`
- Auth: **Bắt buộc**

---

## 7) Favorites (USER)
Controller đang `@PreAuthorize("hasRole('USER')")`.

> Base path có dấu `/` ở cuối: `/api/favorites/`.
> Postman/FE nên gọi theo đúng URL bên dưới (không ảnh hưởng nhiều nếu server normalize, nhưng nên chuẩn).

- Add favorite: `POST /api/favorites/{userId}` (Body: `FavoriteRequest`)
- List favorites: `GET /api/favorites/{userId}`
- Remove: `DELETE /api/favorites/{userId}/{courseId}`
- Check: `GET /api/favorites/{userId}/check/{courseId}`
- Count: `GET /api/favorites/{userId}/count`
- Select status: `PUT /api/favorites/{userId}/{courseId}/select?selected=true|false`
- Clear all: `DELETE /api/favorites/{userId}/clear`

`FavoriteRequest` mẫu (tối thiểu cần đúng field service dùng, nhưng DTO đang cho phép đầy đủ):
```json
{
  "courseId": "...",
  "title": "...",
  "thumbnailUrl": "...",
  "price": 360000,
  "discountedPrice": 300000,
  "discountPercent": 20,
  "level": "Beginner",
  "duration": 20,
  "instructorName": "...",
  "rating": 4.8,
  "totalStudents": 1234
}
```

---

## 8) Payment — PayOS (thay cho VNPay)
### 8.1 Create payment link
- `POST /api/payment/payos/create`
- Auth: **Bắt buộc** (controller kiểm tra `authentication`)

Body mẫu:
```json
{
  "courseIds": ["<courseId>", "<courseId2>"] ,
  "orderInfo": "Thanh toan khoa hoc (PayOS)"
}
```

Response thường có `data.checkoutUrl` (FE redirect user ra cổng PayOS).

### 8.2 Webhook (server-to-server)
- `POST /api/payment/payos/webhook`
- FE **không gọi** (dùng để PayOS gọi về BE)

### 8.3 Payment status
- `GET /api/payment/{paymentId}/status`
- Auth: (controller không check role, nhưng thường vẫn cần token tuỳ security config)

### 8.4 Lịch sử payment của tôi
- `GET /api/payment/my-payments`
- `GET /api/payment/my-payments/success`
- Auth: **Bắt buộc**

---

## 9) Admin APIs (ADMIN)
### 9.1 Course categories
- `POST /api/admin/course-categories/create`
- `GET /api/admin/course-categories/getAll`
- `PUT /api/admin/course-categories/update`
- `DELETE /api/admin/course-categories/delete/{code}`

### 9.2 Courses
- `POST /api/admin/courses/create`
- `GET /api/admin/courses/getAll`
- `GET /api/admin/courses/{id}`
- `PUT /api/admin/courses/update`
- `DELETE /api/admin/courses/delete/{id}`
- Upload thumbnail: `POST /api/admin/courses/upload-thumbnail` (multipart, field `file`)

### 9.3 Chapters
- `POST /api/admin/chapters/create`
- `GET /api/admin/chapters/course/{courseId}`
- `GET /api/admin/chapters/{id}`
- `PUT /api/admin/chapters/{id}`
- `DELETE /api/admin/chapters/{id}`

### 9.4 Lessons
- `POST /api/admin/lessons/create`
- `GET /api/admin/lessons/chapter/{chapterId}`
- `GET /api/admin/lessons/course/{courseId}`
- `GET /api/admin/lessons/{id}`
- `PUT /api/admin/lessons/{id}`
- `DELETE /api/admin/lessons/{id}`

### 9.5 Quizzes
- `GET /api/admin/quizzes/all`
- `POST /api/admin/quizzes/create`
- `GET /api/admin/quizzes/{quizId}`
- `PUT /api/admin/quizzes/{quizId}`
- `DELETE /api/admin/quizzes/{quizId}`

### 9.6 Users
- `GET /api/admin/users/read-users`
- `PUT /api/admin/users/active/{id}`
- `PUT /api/admin/users/{id}/role`

---

## 10) FE verification checklist (sau refactor + payment đổi PayOS)
1) **Auth**
- Register → Verify OTP → Login trả về token ok
- Token gắn vào Authorization header gọi được `/api/users/find-userId`

2) **Public browsing**
- `/api/courses` + curriculum public chạy không cần token

3) **Payment PayOS**
- Có token: gọi `/api/payment/payos/create` nhận `checkoutUrl`
- Sau khi thanh toán, `GET /api/payment/my-payments/success` thấy payment success

4) **Enroll & learn**
- `/api/progress/my-courses` có khoá học vừa mua
- `/api/lessons/{id}` không còn 403 (nếu lesson đã unlock)

5) **Quiz**
- Dùng `POST /api/quizzes/submit` (không dùng endpoint deprecated trong lessons)

6) **Favorites**
- Các endpoint `/api/favorites/...` yêu cầu role USER

---

## Ghi chú nhanh cho FE
- Nếu FE thấy ô URL bị “trống” trong Postman: hãy import lại collection mới nhất.
- Nếu bị `403` lesson: gọi `/api/lessons/{lessonId}/access` để debug.
- Nếu payment success nhưng chưa thấy course trong My Courses: gọi `POST /api/progress/enroll/{courseId}` (tạm thời), đồng thời BE check listener enroll theo payment.
