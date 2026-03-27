# ClickUp (Course/Payment Backend) – Project Summary & Design Patterns

> Updated on **2026-03-21**.
>
> Repo này là **Spring Boot backend** (Java 17) cho hệ thống **học online/bán khóa học** (direct purchase) gồm: Auth/User, Course/Curriculum, Lesson/Progress, Quiz, Favorite và Payment (**PayOS**).

---

## 1) Chủ đề / phạm vi hệ thống

### Chức năng chính (đang sử dụng)
- **Auth & User**: đăng ký/đăng nhập, phân quyền (ADMIN/USER), JWT, OTP email, Redis lưu OTP TTL, tích hợp Firebase.
- **Course/Curriculum**: quản lý khóa học, danh mục, chương/bài học.
- **Lesson/Progress**: theo dõi tiến độ học của người dùng (enroll sau khi thanh toán thành công).
- **Quiz**: tạo quiz, làm bài, lưu attempt/kết quả.
- **Favorite**: danh sách yêu thích.
- **Payment**: thanh toán qua **PayOS** (mua trực tiếp theo danh sách `courseIds`).

### Chức năng đã loại bỏ (không dùng trong chủ đề)
- **Cart** và **Order** đã được gỡ khỏi codebase để tập trung đúng chủ đề “bán khóa học” (direct checkout).

---

## 2) Các design pattern hiện có trong dự án (đã thấy trong code)

### 2.1 Dependency Injection (DI) – **Có**
- Spring inject dependency vào `@Service`, `@Component`, `@Configuration`.
- Ví dụ điển hình:
  - `com.example.cake.config.SecurityConfig`: inject `JwtAuthenticationFilter`.
  - `com.example.cake.payment.service.PaymentService`: inject `PaymentRepository`, `CourseRepository`, `VNPayService`, `UserProgressRepository`.

> Ghi chú: trong Spring, bean mặc định scope **singleton** (theo container).

### 2.2 Repository Pattern – **Có**
Dự án dùng Spring Data MongoDB repositories (`extends MongoRepository`). Repository tiêu biểu:
- `auth/repository/UserRepository`
- `course/repository/CourseRepository`, `CourseCategoryRepository`
- `favorite/repository/FavoriteRepository`
- `lesson/repository/ChapterRepository`, `LessonRepository`, `UserProgressRepository`
- `payment/repository/PaymentRepository`
- `quiz/repository/QuizRepository`, `QuizAttemptRepository`

---

## 3) Các design pattern cần áp dụng cho **các chức năng còn lại** (để đúng môn “Mẫu thiết kế phần mềm”)

Mục tiêu phần này:
- Áp dụng **nhiều pattern** nhưng không gượng ép.
- Mỗi pattern gắn với **1 use-case thật** trong hệ thống course/payment.

### 3.1 Backend (Spring Boot) – đề xuất pattern theo module

#### A) Payment (VNPay + mở rộng cổng thanh toán) – `payment/*`
**Pattern nên áp dụng:**
- **Strategy**: tách “cổng thanh toán” thành interface `PaymentGateway`.
  - Hiện tại: `VNPayGateway` (bọc logic từ `VNPayService`).
  - Tương lai: Momo/ZaloPay/PayPal (thêm class mới, không sửa luồng chính nhiều).
- **Factory Method**: chọn gateway theo `paymentMethod` hoặc config.
  - Ví dụ: `PaymentGatewayFactory.get(method)`.
- **Facade**: `PaymentFacade`/`CheckoutFacade` để gom orchestration:
  - validate courses → tạo Payment → tạo URL
  - verify callback → cập nhật status → phát event
- **Observer (Event-driven)**: tách side-effects khỏi `processVNPayReturn`:
  - Event `PaymentSucceeded` → listener enroll course (`UserProgress`), gửi mail, log/audit, thống kê.
- (**Tuỳ chọn**) **State**: nếu PaymentStatus sau này có thêm refund/expire/retry.

**Lý do phù hợp:** module payment là nơi “thay đổi thường xuyên theo nhà cung cấp”, rất hợp Strategy/Factory; còn sau thanh toán sẽ phát sinh nhiều side-effect → Observer.

---

#### B) Auth & User – `auth/*`, `user/*`
**Pattern nên áp dụng:**
- **Strategy**: tách luồng đăng nhập theo provider:
  - LOCAL (email/password)
  - GOOGLE (Firebase token)
  - (tuỳ) sau này thêm Facebook
- **Facade**: `AuthFacade` gom Register/Login/Refresh/OTP/VerifyOtp để controller gọi 1–2 hàm.
- **Factory Method**: chọn `AuthProviderStrategy` theo `AutheProvider`.
- (**Tuỳ chọn**) **Observer**: phát event `UserRegistered`/`OtpVerified` để gửi welcome mail, audit log.

**Lý do phù hợp:** Auth có nhiều biến thể theo provider và dễ mở rộng.

---

#### C) Course/Curriculum – `course/*`, `lesson/*` (chapter/lesson)
**Pattern nên áp dụng:**
- **Facade**: `LearningFacade` (hoặc `CurriculumFacade`) gom:
  - lấy curriculum (chapters + lessons)
  - kiểm tra quyền học (đã enroll chưa)
  - trả về DTO tổng hợp cho FE
- (**Tuỳ chọn**) **Factory Method**: nếu bạn có nhiều kiểu response/view model (admin view vs user view) thì dùng factory tạo DTO builder phù hợp.

**Lý do phù hợp:** curriculum thường cần “điều phối nhiều repository/service” → Facade giúp controller mỏng.

---

#### D) Progress (enroll/complete lesson) – `lesson/service/ProgressService.java`
**Pattern nên áp dụng:**
- **State**: mô hình tiến độ học theo trạng thái (ví dụ: NOT_ENROLLED → ENROLLED → IN_PROGRESS → COMPLETED).
  - Hữu ích khi bạn có rule: chỉ cho complete lesson khi đã enroll, chỉ tính % khi lesson complete…
- **Observer**: event `LessonCompleted` → listener cập nhật thống kê, award badge, gửi notification (nếu muốn).

**Lý do phù hợp:** progress là workflow có “transition” rõ ràng → State/Observer rất hợp.

---

#### E) Quiz – `quiz/*`
**Pattern nên áp dụng:**
- **Strategy**: tách thuật toán chấm điểm / tính điểm:
  - Quiz single-choice, multiple-choice, true/false, (tuỳ) coding quiz
- (**Tuỳ chọn**) **Factory Method**: tạo scorer theo `quizType`.
- (**Tuỳ chọn**) **Observer**: event `QuizSubmitted` → listener lưu attempt, cập nhật progress, gửi mail.

**Lý do phù hợp:** quiz có nhiều loại câu hỏi và nhiều cách chấm điểm.

---

#### F) Favorite – `favorite/*`
**Pattern nên áp dụng (nhẹ nhàng, không gượng):**
- **Facade** (tuỳ): nếu Favorite cần kết hợp Course/User để trả response đầy đủ, tạo `FavoriteFacade`.
- **Observer** (tuỳ): event `CourseFavorited` để tracking analytics.

---

### 3.2 Frontend (React) – đề xuất pattern phù hợp
- **Memento**: Undo/Redo trong màn admin tạo/sửa quiz hoặc lesson (form nhiều bước).
- **Prototype**: Clone câu hỏi quiz / clone lesson block.
- **Decorator (composition/HOC/hooks)**: `withAuth`, `withRoleGuard`, `usePermissionGuard`.

---

## 4) Các pattern trong danh sách nhưng **không khuyến nghị / ít phù hợp** với hệ này

- **Visitor**: hợp với cây đối tượng phức tạp + nhiều phép toán (compiler/AST). Với hệ CRUD thường không cần.
- **Bridge**: chỉ cần khi có 2 trục thay đổi độc lập lớn; với payment hiện Strategy/Factory thường đủ.
- **Prototype (Java clone)**: ở BE thường dùng mapper/copy hơn là `clone()`.
- **Singleton tự viết**: không cần, vì Spring bean đã singleton theo container.

---

## 5) Gợi ý báo cáo nhanh (1 slide / 1 đoạn)

- **Hiện có trong code:** DI + Repository
- **Nên bổ sung (đúng chủ đề course/payment):** Strategy + Factory Method + Facade + Observer (+ State cho progress/payment nếu cần)
- **FE (React):** Memento/Prototype/Decorator (composition)

---

## 6) Cấu trúc thư mục (backend – hiện tại)

- `src/main/java/com/example/cake/auth` – auth, JWT, filter
- `src/main/java/com/example/cake/user` – user profile/admin user
- `src/main/java/com/example/cake/course` – course, category
- `src/main/java/com/example/cake/lesson` – chapter/lesson/progress
- `src/main/java/com/example/cake/quiz` – quiz, attempt
- `src/main/java/com/example/cake/favorite` – favorite
- `src/main/java/com/example/cake/payment` – payment + VNPay
- `src/main/java/com/example/cake/config` – security/cors/firebase config

---

## 7) Hướng dẫn chuyển đổi Payment từ VNPay sang PayOS (Backend + React FE)

Mục tiêu:
- Bỏ hoàn toàn flow VNPay.
- Chuẩn hoá API Payment để FE React chỉ gọi PayOS.
- Webhook PayOS cập nhật trạng thái và tự động enroll khoá học.

### 7.1 Backend (Spring Boot)

#### A. Cấu hình PayOS
Trong `src/main/resources/application.yml`:
- `payos.client-id`
- `payos.api-key`
- `payos.checksum-key`
- `payos.api-base-url` (hiện: `https://api-merchant.payos.vn`)
- `payos.return-url` (FE route khi thanh toán xong)
- `payos.cancel-url` (FE route khi huỷ)
- `payos.webhook-url` (URL public để PayOS gọi webhook)

Lưu ý triển khai thật:
- `webhook-url` **phải public** (ngrok / domain thật), PayOS server mới gọi được.
- Không commit secret khi public repo (nên chuyển sang env).

#### B. API endpoints mới (PayOS)
Controller: `com.example.cake.payment.controller.PaymentController`

1) Tạo link thanh toán:
- `POST /api/payment/payos/create`
- Auth: **bắt buộc đăng nhập** (JWT)
- Body (FE gửi):
  - `courseIds: string[]`
  - `orderInfo?: string`
- Response:
  - `success=true`
  - `data.paymentUrl` (checkoutUrl)
  - `data.paymentId` (internal payment id)

2) Webhook (PayOS gọi server-to-server):
- `POST /api/payment/payos/webhook`
- Auth: **permitAll** (đã whitelist trong `SecurityConfig`)
- Payload PayOS mẫu:
```json
{
  "code": "00",
  "desc": "success",
  "success": true,
  "data": {
    "orderCode": 123,
    "amount": 3000,
    "reference": "...",
    "transactionDateTime": "...",
    "paymentLinkId": "...",
    "code": "00",
    "desc": "Thành công"
  },
  "signature": "..."
}
```
Backend hiện:
- Parse `data.orderCode`
- Verify signature (nếu có `rawBody` + `signature`)
- Lookup Payment theo `providerOrderCode` (PayOS orderCode)
- Nếu thành công → set `PaymentStatus.SUCCESS` + publish `PaymentSucceededEvent` để enroll khoá học.

3) Query trạng thái / lịch sử:
- `GET /api/payment/{paymentId}/status`
- `GET /api/payment/my-payments`
- `GET /api/payment/my-payments/success`

#### C. BE flow nội bộ (Design patterns)
- `PaymentGateway` (Strategy)
- `PaymentGatewayFactory` (Factory Method)
- `PaymentFacade` (Facade)
- `PaymentSucceededEvent` + listener (Observer)

#### D. Điểm cần chú ý khi test PayOS
- Khi tạo payment, backend set `payment.providerOrderCode = abs(paymentId.hashCode())`.
  - Webhook sẽ dựa `orderCode` để tìm lại payment.
- Nếu bạn muốn “chuẩn hơn”, có thể thay cách sinh orderCode sang sequence/redis/increment để tránh collision (rất hiếm nhưng có thể xảy ra).

---

### 7.2 Frontend (React)

#### A. Thay đổi endpoint
Trước đây (VNPay):
- `POST /api/payment/vnpay/create`
- `GET /api/payment/vnpay/return` hoặc `.../ipn`

Bây giờ (PayOS):
- `POST /api/payment/payos/create`
- FE **không tự gọi webhook**. Webhook do PayOS gọi vào backend.

#### B. FE flow đề xuất
1) User chọn khoá học → bấm “Thanh toán”
2) FE gọi `POST /api/payment/payos/create` → nhận `paymentUrl`
3) FE `window.location.href = paymentUrl` (redirect sang trang PayOS)
4) Sau khi thanh toán:
   - PayOS redirect về `returnUrl`/`cancelUrl` (route FE)
   - Đồng thời PayOS gọi webhook về backend để xác nhận
5) FE có thể poll `GET /api/payment/{paymentId}/status` vài giây (optional) để hiển thị trạng thái.

#### C. Những việc FE cần làm (checklist)
- [ ] Đổi `PaymentAPI.createVNPayPayment` → `createPayOSPayment` gọi `/payment/payos/create`
- [ ] Gỡ toàn bộ call `/payment/vnpay/*`
- [ ] Tạo page `payment/return` và `payment/cancel` đúng với config trong `application.yml`
- [ ] (Optional) Trong page return, gọi `getPaymentStatus(paymentId)` để confirm UI

---

### 7.3 Mapping nhanh: VNPay → PayOS
- VNPay “return/ipn” (FE/BE) → PayOS dùng **webhook** (BE-BE) + FE route return/cancel chỉ để hiển thị.
- VNPay ký theo querystring → PayOS ký theo body/canonical string (tuỳ spec).
- Codebase hiện đã bỏ VNPay endpoints.

---

### Notes bảo mật
- `application.yml` đang chứa secrets (Mongo uri, mail password, payos keys...). Khi nộp đồ án, OK; khi public repo thì nên chuyển sang env và tránh commit.

---

## 8) Design Patterns Index (vị trí code để dán vào báo cáo)

> Mục tiêu phần này: liệt kê **các mẫu thiết kế đã áp dụng thật trong code** + **đường dẫn file** + **đoạn code tiêu biểu** để bạn copy/paste vào bài báo cáo.

### 8.1 Dependency Injection (DI) – Spring
**Vị trí tiêu biểu**
- `src/main/java/com/example/cake/payment/service/PaymentService.java`
- `src/main/java/com/example/cake/lesson/service/ProgressService.java`
- `src/main/java/com/example/cake/quiz/service/QuizService.java`

**Code mẫu (constructor injection qua Lombok)**
```java
@Slf4j
@Service
@RequiredArgsConstructor
public class QuizService {
    private final QuizRepository quizRepository;
    private final QuizAttemptRepository attemptRepository;
    private final ProgressService progressService;
    private final QuizGrader quizGrader;
    private final ApplicationEventPublisher eventPublisher;
    // ...
}
```

---

### 8.2 Repository Pattern – Spring Data MongoDB
**Vị trí tiêu biểu**
- `src/main/java/com/example/cake/quiz/repository/QuizRepository.java`
- `src/main/java/com/example/cake/lesson/repository/LessonRepository.java`
- `src/main/java/com/example/cake/payment/repository/PaymentRepository.java`

**Code mẫu**
```java
@Repository
public interface LessonRepository extends MongoRepository<Lesson, String> {
    List<Lesson> findByCourseIdOrderByOrderAsc(String courseId);
}
```

---

### 8.3 Strategy + Factory Method (Quiz scoring)
#### Strategy
**Vị trí**
- Strategy interface: `src/main/java/com/example/cake/quiz/service/scoring/QuestionScoringStrategy.java`
- Concrete strategies:
  - `src/main/java/com/example/cake/quiz/service/scoring/SingleChoiceScoringStrategy.java`
  - `src/main/java/com/example/cake/quiz/service/scoring/MultipleChoiceScoringStrategy.java`
  - `src/main/java/com/example/cake/quiz/service/scoring/TrueFalseScoringStrategy.java`

**Code mẫu (Strategy interface)**
```java
public interface QuestionScoringStrategy {
    Quiz.QuestionType supports();
    ScoredQuestion score(Quiz.Question question, List<String> selectedOptions);
}
```

#### Factory Method
**Vị trí**
- `src/main/java/com/example/cake/quiz/service/scoring/QuestionScoringStrategyFactory.java`

**Code mẫu (Factory chọn strategy theo type)**
```java
@Component
public class QuestionScoringStrategyFactory {
    private final Map<Quiz.QuestionType, QuestionScoringStrategy> map;

    public QuestionScoringStrategy get(Quiz.QuestionType type) {
        QuestionScoringStrategy s = map.get(type);
        if (s == null) throw new IllegalArgumentException("Unsupported question type: " + type);
        return s;
    }
}
```

#### Orchestrator (Facade-like helper)
**Vị trí**
- `src/main/java/com/example/cake/quiz/service/scoring/QuizGrader.java`

**Vai trò**: gom luồng chấm quiz (dùng Strategy + Factory) để `QuizService` không chứa thuật toán.

---

### 8.4 Observer (Quiz submitted event)
**Vị trí**
- Event: `src/main/java/com/example/cake/quiz/service/event/QuizSubmittedEvent.java`
- Listener: `src/main/java/com/example/cake/quiz/service/listener/QuizProgressListener.java`
- Publish event: `src/main/java/com/example/cake/quiz/service/QuizService.java`

**Code mẫu (publish event)**
```java
eventPublisher.publishEvent(QuizSubmittedEvent.builder()
    .userId(userId)
    .quizId(quiz.getId())
    .lessonId(quiz.getLessonId())
    .percentage(attempt.getPercentage())
    .passed(attempt.getPassed())
    .attempt(attempt)
    .build());
```

**Code mẫu (listener)**
```java
@EventListener
public void onQuizSubmitted(QuizSubmittedEvent event) {
    if (Boolean.TRUE.equals(event.getPassed()) && event.getLessonId() != null) {
        progressService.updateQuizPassed(event.getUserId(), event.getLessonId(), event.getPercentage());
    }
}
```

---

### 8.5 State pattern (Progress lifecycle)
**Vị trí**
- Interface state: `src/main/java/com/example/cake/lesson/service/progress/ProgressState.java`
- Resolver: `src/main/java/com/example/cake/lesson/service/progress/ProgressStateResolver.java`
- Concrete states:
  - `src/main/java/com/example/cake/lesson/service/progress/NotEnrolledState.java`
  - `src/main/java/com/example/cake/lesson/service/progress/EnrolledState.java`
  - `src/main/java/com/example/cake/lesson/service/progress/InProgressState.java`
  - `src/main/java/com/example/cake/lesson/service/progress/CompletedState.java`
- Service dùng state: `src/main/java/com/example/cake/lesson/service/ProgressService.java`

**Code mẫu (State interface có action methods)**
```java
public interface ProgressState {
    UserProgressStatus getStatus();
    StateTransitionResult completeLesson(ProgressContext ctx);
    StateTransitionResult updateVideoProgress(ProgressContext ctx);
}
```

---

### 8.6 Observer (Progress events)
**Vị trí**
- Events:
  - `src/main/java/com/example/cake/lesson/service/event/LessonCompletedEvent.java`
  - `src/main/java/com/example/cake/lesson/service/event/CourseCompletedEvent.java`
- Listener:
  - `src/main/java/com/example/cake/lesson/service/listener/ProgressAchievementListener.java`
- Publish events:
  - `src/main/java/com/example/cake/lesson/service/ProgressService.java`

---

### 8.7 Facade pattern (Curriculum)
**Vị trí**
- Facade: `src/main/java/com/example/cake/lesson/service/CurriculumFacade.java`
- Public controller (giữ API cũ nhưng controller mỏng):
  - `src/main/java/com/example/cake/lesson/controller/CurriculumController.java`
- User controller (API mới, có JWT):
  - `src/main/java/com/example/cake/lesson/controller/UserCurriculumController.java`
- DTO user-view:
  - `src/main/java/com/example/cake/lesson/dto/CurriculumUserViewDTO.java`

**Code mẫu (Facade method trả curriculum cho user)**
```java
public ResponseMessage<CurriculumUserViewDTO> getCurriculumForUser(String userId, String courseId) {
    UserProgress progress = userProgressRepository.findByUserIdAndCourseId(userId, courseId).orElse(null);
    List<Chapter> chapters = chapterRepository.findByCourseIdOrderByOrderAsc(courseId);
    List<Lesson> lessons = lessonRepository.findByCourseIdOrderByOrderAsc(courseId);
    // ...aggregate + add unlocked/completed/videoProgress...
    return new ResponseMessage<>(true, "Success", dto);
}
```

---

### 8.8 Payment (Strategy/Factory/Facade/Observer)
> Payment bạn đã làm trước đó (PayOS) và có gắn **Facade**. Vì phần payment codebase có thể khác theo nhánh bạn đang chạy, bạn nên trích code trực tiếp từ các file payment sau.

**Vị trí (tìm trong code)**
- `src/main/java/com/example/cake/payment/service/facade/PaymentFacade.java`
- `src/main/java/com/example/cake/payment/service/gateway/*` (Strategy implementations)
- `src/main/java/com/example/cake/payment/service/gateway/PaymentGatewayFactory.java` (Factory Method)
- `src/main/java/com/example/cake/payment/service/PayOSService.java` (integration)

---

## 9) Các pattern trong danh sách nhưng hiện tại CHƯA áp dụng (để ghi trong báo cáo)
- Singleton (tự viết): **chưa** (Spring beans đã singleton theo container)
- Memento: **chưa** (phù hợp FE React – undo/redo form)
- Prototype: **chưa** (phù hợp FE React – clone question/lesson block)
- Decorator: **chưa** (phù hợp FE React – HOC/hooks guard)
- Bridge: **chưa**
- Visitor: **chưa**
