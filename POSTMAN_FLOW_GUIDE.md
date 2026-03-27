# Postman Flow Guide (E2E)

Tài liệu này hướng dẫn test **end-to-end** hệ thống bán khóa học (Spring Boot BE + React FE) bằng Postman theo đúng các endpoint hiện có trong dự án.

> Mục tiêu: từ **tạo dữ liệu (ADMIN)** → **tạo tài khoản + đăng nhập (USER)** → **mua khóa học + thanh toán PayOS** → **enroll + học + hoàn thành lesson/course**.

---

## 0) Chuẩn bị nhanh

### 0.1 Base URL

- `baseUrl`: `http://localhost:8080`

### 0.2 Token

- `token`: JWT của USER (lấy từ API login)
- `adminToken`: JWT của ADMIN

> Trong collection JSON, bạn có thể set variables: `baseUrl`, `token`, `adminToken`, `courseId`, `chapterId`, `lessonId`, `quizId`, `paymentId`, `userId`.

### 0.3 Thứ tự chạy khuyến nghị

1) ADMIN chuẩn bị dữ liệu: category → course → chapter → lesson → quiz (tuỳ)
2) USER register → verify otp → login
3) USER xem course/curriculum
4) USER tạo PayOS payment link → thanh toán (mở checkoutUrl)
5) PayOS webhook → BE xác nhận payment success → enroll/progress
6) USER học bài: access → get lesson → update progress → complete
7) USER xem progress/my-courses xác nhận hoàn thành

---

## 1) ADMIN: Chuẩn bị dữ liệu khóa học (1 lần)

> Các endpoint ADMIN thường yêu cầu `Authorization: Bearer {{adminToken}}`.

### 1.1 Tạo Course Category

**POST** `/api/admin/course-categories/create`

Body (đúng DTO):
```json
{
  "code": "WEB",
  "name": "Web Development"
}
```

### 1.2 Tạo Course

**POST** `/api/admin/courses/create`

Body (đúng DTO `CourseCreateRequest`):
```json
{
  "categoryCode": "WEB",
  "title": "React + Spring Boot Fullstack",
  "description": "Khoa hoc fullstack",
  "price": 360000,
  "thumbnailUrl": "http://localhost:8080/static/courses/demo.jpg",
  "duration": 20,
  "level": "Beginner",
  "isPublished": true
}
```

✅ Sau bước này bạn cần lấy `courseId` (từ response hoặc database) và gán vào variable `{{courseId}}`.

### 1.3 Tạo Chapter

**POST** `/api/admin/chapters/create`

Body (đúng DTO `ChapterRequest`):
```json
{
  "courseId": "{{courseId}}",
  "title": "Chuong 1: Bat dau",
  "description": "Intro",
  "order": 1,
  "isFree": true
}
```

✅ Lấy `chapterId` và gán vào `{{chapterId}}`.

### 1.4 Tạo Lesson

**POST** `/api/admin/lessons/create`

Body (tối giản theo DTO `LessonRequest`):
```json
{
  "chapterId": "{{chapterId}}",
  "courseId": "{{courseId}}",
  "title": "Lesson 1: Gioi thieu",
  "description": "Mo dau",
  "order": 1,
  "duration": 10,
  "isFree": true,
  "requiredPreviousLesson": null,

  "videoUrl": "https://example.com/video.mp4",
  "videoId": "vid_001",
  "videoType": "YOUTUBE",
  "videoThumbnail": "https://example.com/thumb.jpg",

  "contentType": "TEXT",
  "content": "Noi dung bai hoc",

  "codeSnippets": [],
  "attachments": [],

  "hasQuiz": false,
  "quiz": null
}
```

✅ Lấy `lessonId` và gán vào `{{lessonId}}`.

### 1.5 (Tuỳ chọn) Tạo Quiz

**POST** `/api/admin/quizzes/create`

Body (đúng DTO `QuizRequest`):
```json
{
  "lessonId": "{{lessonId}}",
  "courseId": "{{courseId}}",
  "chapterId": "{{chapterId}}",
  "title": "Quiz 1",
  "description": "Kiem tra nhanh",
  "passingScore": 70,
  "timeLimit": 600,
  "maxAttempts": 3,
  "questions": [
    {
      "question": "React la gi?",
      "type": "SINGLE_CHOICE",
      "points": 1,
      "explanation": "React la thu vien UI",
      "options": [
        { "text": "Thu vien UI", "isCorrect": true },
        { "text": "He dieu hanh", "isCorrect": false }
      ]
    }
  ]
}
```

✅ Lấy `quizId` và gán vào `{{quizId}}`.

---

## 2) USER: Tạo tài khoản → xác thực OTP → đăng nhập

### 2.1 Register

**POST** `/api/auth/register`

```json
{
  "fullName": "Student One",
  "email": "student1@gmail.com",
  "password": "123456"
}
```

> Sau bước này hệ thống gửi OTP qua email. Response thường có `token` phục vụ verify OTP.

### 2.2 Verify OTP

**POST** `/api/auth/verify-otp`

```json
{
  "token": "TOKEN_FROM_REGISTER",
  "otp": "123456"
}
```

### 2.3 Login lấy JWT

**POST** `/api/auth/login`

```json
{
  "email": "student1@gmail.com",
  "password": "123456"
}
```

✅ Lấy access token trong response và set vào `{{token}}`.

---

## 3) USER: Xem khóa học & curriculum (trước khi mua)

### 3.1 Xem danh sách khóa học public

**GET** `/api/courses`

### 3.2 Xem curriculum public

- **GET** `/api/curriculum/course/{{courseId}}/chapters`
- **GET** `/api/curriculum/course/{{courseId}}/full`

### 3.3 (Nếu cần) Xem curriculum theo user (có unlocked/completed)

**GET** `/api/me/curriculum/course/{{courseId}}` (cần login)

---

## 4) USER: Mua khóa học bằng PayOS (Payment)

> Payment endpoint theo code:
> - **POST** `/api/payment/payos/create` (cần login)
> - **POST** `/api/payment/payos/webhook` (PayOS gọi vào)
> - **GET** `/api/payment/{paymentId}/status`
> - **GET** `/api/payment/my-payments`

### 4.1 Tạo PayOS payment link (cần login)

**POST** `/api/payment/payos/create`

```json
{
  "courseIds": ["{{courseId}}"],
  "orderInfo": "Thanh toan khoa hoc (PayOS)"
}
```

✅ Kết quả mong đợi: trả về URL/checkout link để user mở và thanh toán.

### 4.2 PayOS webhook (server-to-server)

PayOS sẽ gọi:

**POST** `/api/payment/payos/webhook`

Payload mẫu:
```json
{
  "code": "00",
  "desc": "success",
  "success": true,
  "data": {
    "orderCode": 123,
    "amount": 3000,
    "description": "VQRIO123",
    "accountNumber": "12345678",
    "reference": "TF230204212323",
    "transactionDateTime": "2023-02-04 18:25:00",
    "currency": "VND",
    "paymentLinkId": "124c33293c43417ab7879e14c8d9eb18",
    "code": "00",
    "desc": "Thành công",
    "counterAccountBankId": "",
    "counterAccountBankName": "",
    "counterAccountName": "",
    "counterAccountNumber": "",
    "virtualAccountName": "",
    "virtualAccountNumber": ""
  },
  "signature": "YOUR_SIGNATURE"
}
```

> Lưu ý: webhook thực tế sẽ do PayOS gửi. Trong test local, bạn có thể POST giả lập để kiểm tra flow xử lý callback.

---

## 5) USER: Enroll + học bài + hoàn thành

### 5.0 (Tuỳ chọn) Enroll thủ công để test nhanh

Nếu bạn muốn test progress mà bỏ qua payment, dùng:

**POST** `/api/progress/enroll/{{courseId}}`

### 5.1 Kiểm tra quyền truy cập lesson

**GET** `/api/lessons/{{lessonId}}/access`

✅ Mong đợi: `data = true`.

### 5.2 Lấy nội dung lesson

**GET** `/api/lessons/{{lessonId}}`

> Endpoint này sẽ trả **403** nếu `canAccessLesson` = false.

### 5.3 Update tiến độ video (ví dụ xem 50%)

**POST** `/api/lessons/{{lessonId}}/progress?percent=50`

### 5.4 Đánh dấu hoàn thành lesson

**POST** `/api/lessons/{{lessonId}}/complete`

### 5.5 Lấy thông tin lesson tiếp theo

**GET** `/api/lessons/{{lessonId}}/next`

### 5.6 (Nếu có quiz) Làm quiz

- Lấy quiz cho student (không có đáp án đúng):
  - **GET** `/api/quizzes/{{quizId}}`

- Submit quiz:
  - **POST** `/api/quizzes/submit`

Ví dụ body (tuỳ theo `QuizSubmitRequest` trong dự án):
```json
{
  "quizId": "{{quizId}}",
  "answers": [
    { "questionId": "q1", "selectedOptionIds": ["a"] }
  ]
}
```

- Lịch sử attempt:
  - **GET** `/api/quizzes/{{quizId}}/attempts`

- Check passed:
  - **GET** `/api/quizzes/{{quizId}}/passed`

### 5.7 Xem progress toàn course

**GET** `/api/progress/course/{{courseId}}`

### 5.8 Xem danh sách khóa học đã đăng ký (My Courses)

**GET** `/api/progress/my-courses`

---

## 6) Debug nhanh khi lỗi

### 6.1 403 Forbidden

- Sai/missing `Authorization: Bearer {{token}}`
- Token hết hạn
- Endpoint yêu cầu role ADMIN/USER
- Lesson bị chặn vì `canAccessLesson=false` (chưa enroll / chưa complete lesson trước / chưa pass quiz)

### 6.2 Payment create/link lỗi

- Thiếu `courseIds` hoặc courseId không tồn tại
- PayOS credential sai (clientId/apiKey/checksumKey)
- Signature sai (nếu bạn tự ký)

### 6.3 CORS lỗi (FE)

- Lỗi này không liên quan Postman. Postman không bị CORS.

---

## 7) Liên kết collection Postman

- File collection full đã tạo trong workspace:
  - `postman_full_collection.json`

> Bạn có thể import file này trong Postman, sau đó dùng guide này để chạy từng bước theo đúng luồng.

---

## 8) Chế độ “Auto-capture” (chạy 1 mạch không cần copy ID)

File collection `postman_full_collection.json` đã được nâng cấp để tự lưu biến sau mỗi bước quan trọng:

### 8.1 Auto-capture đang có

- **Register** (`POST /api/auth/register`)
  - Lưu `{{otpToken}}` nếu response có `data.token`/`data.otpToken`
  - Lưu `{{loginEmail}}` bằng email đã gửi lên
  - Body register mặc định dùng `{{randomEmail}}` (tự sinh)

- **Login** (`POST /api/auth/login`)
  - (Đã có sẵn trước đó) cố gắng bắt JWT từ các shape phổ biến và set `{{token}}`

- **Get userId** (`GET /api/users/find-userId`)
  - Lưu `{{userId}}`

- **Create course** (`POST /api/admin/courses/create`)
  - Lưu `{{courseId}}` và đồng thời set `{{courseId1}}` (để payment dùng luôn)

- **Create chapter** (`POST /api/admin/chapters/create`)
  - Lưu `{{chapterId}}`

- **Create lesson** (`POST /api/admin/lessons/create`)
  - Lưu `{{lessonId}}`

- **Create quiz** (`POST /api/admin/quizzes/create`)
  - Lưu `{{quizId}}`

- **Create PayOS payment link** (`POST /api/payment/payos/create`)
  - Lưu `{{checkoutUrl}}` nếu response có `data.checkoutUrl`/`data.paymentUrl`/`data.url`
  - Lưu `{{paymentId}}` nếu response trả về

> Lưu ý: Nếu một endpoint create **không trả id** trong `data`, script sẽ không bắt được. Khi đó bạn cần copy id từ DB/logs rồi set vào variable.

### 8.2 Thứ tự chạy “1 mạch” khuyến nghị

1) (ADMIN) `POST /api/admin/course-categories/create`
2) (ADMIN) `POST /api/admin/courses/create` → tự set `courseId`
3) (ADMIN) `POST /api/admin/chapters/create` → tự set `chapterId`
4) (ADMIN) `POST /api/admin/lessons/create` → tự set `lessonId`
5) (ADMIN) `POST /api/admin/quizzes/create` (tuỳ) → tự set `quizId`

6) (USER) `POST /api/auth/register` → tự set `otpToken`, `loginEmail`
7) (USER) `POST /api/auth/verify-otp`
8) (USER) `POST /api/auth/login` → tự set `token`
9) (USER) `GET /api/users/find-userId` → tự set `userId`

10) (USER) `POST /api/payment/payos/create` → tự set `checkoutUrl`
11) Mở `checkoutUrl` để thanh toán (trong browser) và chờ PayOS gọi webhook

12) (USER) `POST /api/progress/enroll/{{courseId}}` (chỉ dùng khi bạn muốn test nhanh, bỏ qua payment)
13) (USER) Lesson flow: `/access` → `/` → `/progress?percent=` → `/complete` → `/next`
14) (USER) `GET /api/progress/course/{{courseId}}` và `GET /api/progress/my-courses`
