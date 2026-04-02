# Báo cáo Design Patterns (Intent → Structure → Code → Where used)

> Dự án: **Hệ thống bán khóa học / E-learning** (Spring Boot + MongoDB)
>
> Ngày tạo: **2026-03-27**
>
> Mục tiêu: Tài liệu này tối ưu để **nộp báo cáo môn “Mẫu thiết kế phần mềm”**. Mỗi mẫu có 4 phần:
> - **Intent** (mục đích)
> - **Structure** (cấu trúc / thành phần)
> - **Code** (đoạn code tiêu biểu để dán vào báo cáo)
> - **Where used** (vị trí + use-case trong dự án)

---

## 1) Dependency Injection (DI) – Spring

### Intent
Giảm phụ thuộc cứng giữa các lớp bằng cách **inject** dependencies (service, repository, factory, …) thay vì tự `new`.

### Structure
- **Client**: class sử dụng dependency (ví dụ `QuizService`).
- **Dependency**: interface/class được inject (ví dụ `QuizRepository`, `QuizGrader`).
- **Injector/Container**: Spring IoC container tạo bean và inject qua constructor.

### Code
Ví dụ constructor injection thông qua Lombok `@RequiredArgsConstructor`:

```java
@Slf4j
@Service
@RequiredArgsConstructor
public class QuizService {
    private final QuizRepository quizRepository;
    private final QuizAttemptRepository attemptRepository;
    private final com.example.cake.lesson.service.ProgressService progressService;

    // Strategy + Factory orchestrator
    private final QuizGrader quizGrader;

    // Observer publisher
    private final ApplicationEventPublisher eventPublisher;
}
```

### Where used
- `src/main/java/com/example/cake/quiz/service/QuizService.java`
- `src/main/java/com/example/cake/lesson/service/ProgressService.java`
- `src/main/java/com/example/cake/lesson/service/CurriculumFacade.java`
- (và các `@Service/@Component` khác trong dự án)

---

## 2) Repository Pattern – Spring Data MongoDB

### Intent
Tách riêng tầng truy cập dữ liệu (CRUD/queries) khỏi business logic, giúp service chỉ tập trung vào nghiệp vụ.

### Structure
- **Repository interface**: định nghĩa hành vi truy cập dữ liệu (extends `MongoRepository`).
- **Domain Model**: entity/document MongoDB (ví dụ `Lesson`, `Quiz`, `QuizAttempt`).
- **Service**: gọi repository để đọc/ghi.

### Code
Ví dụ repository có query method theo naming convention:

```java
public interface LessonRepository extends MongoRepository<Lesson, String> {
    List<Lesson> findByCourseIdOrderByOrderAsc(String courseId);
}
```

### Where used
- `src/main/java/com/example/cake/lesson/repository/LessonRepository.java`
- `src/main/java/com/example/cake/lesson/repository/UserProgressRepository.java`
- `src/main/java/com/example/cake/quiz/repository/QuizRepository.java`
- `src/main/java/com/example/cake/quiz/repository/QuizAttemptRepository.java`

---

## 3) Strategy Pattern – Quiz Scoring

### Intent
Đóng gói các thuật toán chấm điểm khác nhau theo loại câu hỏi (SINGLE/MULTIPLE/TRUE_FALSE) để dễ mở rộng mà **không sửa** luồng chấm điểm chính.

### Structure
- **Strategy interface**: `QuestionScoringStrategy`
- **Concrete strategies**:
  - `SingleChoiceScoringStrategy`
  - `MultipleChoiceScoringStrategy`
  - `TrueFalseScoringStrategy`
- **Context/Caller**: `QuizGrader` dùng strategy để chấm từng question.

### Code
Strategy interface:

```java
public interface QuestionScoringStrategy {
    Quiz.QuestionType supports();
    ScoredQuestion score(Quiz.Question question, List<String> selectedOptions);
}
```

Ví dụ 1 concrete strategy (MULTIPLE_CHOICE):

```java
@Component
public class MultipleChoiceScoringStrategy extends AbstractOptionBasedScoringStrategy {
    @Override
    public Quiz.QuestionType supports() {
        return Quiz.QuestionType.MULTIPLE_CHOICE;
    }

    @Override
    public ScoredQuestion score(Quiz.Question question, List<String> selectedOptions) {
        List<String> correct = correctAnswers(question);
        List<String> selected = selectedOptions != null ? selectedOptions : List.of();

        boolean isCorrect = selected.size() == correct.size()
                && selected.containsAll(correct)
                && correct.containsAll(selected);

        int points = isCorrect ? (question.getPoints() != null ? question.getPoints() : 0) : 0;

        return new ScoredQuestion(
                points,
                buildSavedAnswer(question, selected, isCorrect, points),
                buildQuestionResult(question, selected, correct, isCorrect, points)
        );
    }
}
```

### Where used
- Interface: `src/main/java/com/example/cake/quiz/service/scoring/QuestionScoringStrategy.java`
- Concrete:
  - `src/main/java/com/example/cake/quiz/service/scoring/SingleChoiceScoringStrategy.java`
  - `src/main/java/com/example/cake/quiz/service/scoring/MultipleChoiceScoringStrategy.java`
  - `src/main/java/com/example/cake/quiz/service/scoring/TrueFalseScoringStrategy.java`
- Caller: `src/main/java/com/example/cake/quiz/service/scoring/QuizGrader.java`

---

## 4) Factory Method – Strategy Factory (Quiz)

### Intent
Tạo cơ chế lựa chọn strategy phù hợp theo `QuestionType` mà không cần `if/else` rải khắp code.

### Structure
- **Factory**: `QuestionScoringStrategyFactory`
- **Products**: các `QuestionScoringStrategy` concrete.
- **Client**: `QuizGrader` gọi `factory.get(question.getType())`.

### Code

```java
@Component
public class QuestionScoringStrategyFactory {

    private final Map<Quiz.QuestionType, QuestionScoringStrategy> map;

    public QuestionScoringStrategyFactory(List<QuestionScoringStrategy> strategies) {
        Map<Quiz.QuestionType, QuestionScoringStrategy> tmp = new EnumMap<>(Quiz.QuestionType.class);
        for (QuestionScoringStrategy s : strategies) {
            tmp.put(s.supports(), s);
        }
        this.map = Map.copyOf(tmp);
    }

    public QuestionScoringStrategy get(Quiz.QuestionType type) {
        QuestionScoringStrategy s = map.get(type);
        if (s == null) {
            throw new IllegalArgumentException("Unsupported question type: " + type);
        }
        return s;
    }
}
```

### Where used
- Factory: `src/main/java/com/example/cake/quiz/service/scoring/QuestionScoringStrategyFactory.java`
- Usage: `src/main/java/com/example/cake/quiz/service/scoring/QuizGrader.java`

---

## 5) Observer Pattern – Quiz Submitted Event

### Intent
Tách các side-effect sau khi submit quiz (ví dụ cập nhật progress) ra khỏi `QuizService` bằng cơ chế event/listener.

### Structure
- **Subject / Publisher**: `QuizService` (publish event)
- **Event**: `QuizSubmittedEvent`
- **Observers / Listeners**: `QuizProgressListener`

### Code
Publish event trong `QuizService`:

```java
// (trích) QuizService publish event
// eventPublisher.publishEvent(QuizSubmittedEvent.builder()...build());
```

Listener nhận event:

```java
// (trích) QuizProgressListener
@EventListener
public void onQuizSubmitted(QuizSubmittedEvent event) {
    if (Boolean.TRUE.equals(event.getPassed()) && event.getLessonId() != null) {
        progressService.updateQuizPassed(event.getUserId(), event.getLessonId(), event.getPercentage());
    }
}
```

### Where used
- Event: `src/main/java/com/example/cake/quiz/service/event/QuizSubmittedEvent.java`
- Publisher: `src/main/java/com/example/cake/quiz/service/QuizService.java`
- Listener: `src/main/java/com/example/cake/quiz/service/listener/QuizProgressListener.java`

---

## 6) State Pattern – Progress Lifecycle

### Intent
Mô hình hóa workflow tiến độ học theo các **trạng thái** và **quy tắc chuyển trạng thái** (transition rules), tránh kiểm tra điều kiện rải rác trong service.

### Structure
- **Context**: `ProgressContext` (gói dữ liệu runtime cho action)
- **State interface**: `ProgressState`
- **Concrete states**: `NotEnrolledState`, `EnrolledState`, `InProgressState`, `CompletedState`
- **Resolver**: `ProgressStateResolver` (chọn state dựa vào dữ liệu progress)
- **Client**: `ProgressService` delegate sang state để kiểm tra quyền update/complete.

### Code
State interface (hành động nằm trong state):

```java
public interface ProgressState {
    UserProgressStatus getStatus();

    StateTransitionResult completeLesson(ProgressContext ctx);

    StateTransitionResult updateVideoProgress(ProgressContext ctx);
}
```

Ví dụ service dùng state để quyết định:

```java
// (trích) ProgressService delegate sang state
// StateTransitionResult transition = currentState.updateVideoProgress(ctx);
// if (!transition.isAllowed()) return new ResponseMessage<>(false, transition.getMessage(), progress);
```

### Where used
- `src/main/java/com/example/cake/lesson/service/progress/ProgressState.java`
- `src/main/java/com/example/cake/lesson/service/progress/ProgressContext.java`
- `src/main/java/com/example/cake/lesson/service/progress/ProgressStateResolver.java`
- Concrete states trong `src/main/java/com/example/cake/lesson/service/progress/*State.java`
- Caller: `src/main/java/com/example/cake/lesson/service/ProgressService.java`

---

## 7) Observer Pattern – Progress Events (Lesson/Course completed)

### Intent
Tách các tác vụ phụ sau khi hoàn thành lesson/course (achievement/analytics/notification) ra khỏi core progress logic.

### Structure
- **Publisher**: `ProgressService` publish events.
- **Events**: `LessonCompletedEvent`, `CourseCompletedEvent`
- **Listener**: `ProgressAchievementListener`

### Code

```java
// (trích) ProgressService publish event
// eventPublisher.publishEvent(LessonCompletedEvent.builder()...build());
```

### Where used
- Events:
  - `src/main/java/com/example/cake/lesson/service/event/LessonCompletedEvent.java`
  - `src/main/java/com/example/cake/lesson/service/event/CourseCompletedEvent.java`
- Listener:
  - `src/main/java/com/example/cake/lesson/service/listener/ProgressAchievementListener.java`
- Publish trong:
  - `src/main/java/com/example/cake/lesson/service/ProgressService.java`

---

## 8) Facade Pattern – Curriculum (Public + User view)

### Intent
Cung cấp 1 lớp “điều phối” duy nhất để trả curriculum:
- **PUBLIC**: giữ API cũ (permitAll), không phá FE.
- **USER**: endpoint mới có JWT, trả thêm `unlocked/completed/videoProgress/quizPassed`.

### Structure
- **Facade**: `CurriculumFacade` (aggregate data từ chapter/lesson/progress)
- **Controllers**:
  - `CurriculumController` (PUBLIC) gọi Facade
  - `UserCurriculumController` (USER) gọi Facade
- **DTO**: `CurriculumUserViewDTO` (user-view)

### Code
Facade method (user curriculum):

```java
public ResponseMessage<CurriculumUserViewDTO> getCurriculumForUser(String userId, String courseId) {
    UserProgress progress = userProgressRepository.findByUserIdAndCourseId(userId, courseId).orElse(null);
    List<Chapter> chapters = chapterRepository.findByCourseIdOrderByOrderAsc(courseId);
    List<Lesson> lessons = lessonRepository.findByCourseIdOrderByOrderAsc(courseId);

    // ... group by chapter + compute unlocked/completed/videoProgress ...

    return new ResponseMessage<>(true, "Success", dto);
}
```

PUBLIC controller chỉ gọi Facade (controller mỏng):

```java
@GetMapping("/course/{courseId}/chapters")
public ResponseEntity<ResponseMessage<List<Chapter>>> getCourseChapters(@PathVariable String courseId) {
    return ResponseEntity.ok(curriculumFacade.getCourseChaptersPublic(courseId));
}
```

### Where used
- Facade: `src/main/java/com/example/cake/lesson/service/CurriculumFacade.java`
- PUBLIC controller: `src/main/java/com/example/cake/lesson/controller/CurriculumController.java`
- USER controller: `src/main/java/com/example/cake/lesson/controller/UserCurriculumController.java`
- DTO: `src/main/java/com/example/cake/lesson/dto/CurriculumUserViewDTO.java`

---

## 9) Template Method – Quiz option-based scoring

### Intent
Chuẩn hoá quy trình chấm điểm câu hỏi dạng “chọn đáp án” theo một khung thuật toán cố định, tránh lặp code giữa nhiều strategy.

### Structure
- **Abstract class (Template)**: `AbstractOptionBasedScoringStrategy`
  - `scoreTemplate(...)` là template method
  - `isCorrectSelection(...)` là hook/primitive operation
- **Concrete classes**:
  - `SingleChoiceScoringStrategy`
  - `MultipleChoiceScoringStrategy`
  - `TrueFalseScoringStrategy`

### Code
Template method + hook:

```java
protected final ScoredQuestion scoreTemplate(Quiz.Question question, List<String> selectedOptions) {
    List<String> correct = correctAnswers(question);
    List<String> selected = safeSelected(selectedOptions);

    boolean isCorrect = isCorrectSelection(question, selected, correct);
    int points = isCorrect ? safePoints(question) : 0;

    return new ScoredQuestion(
            points,
            buildSavedAnswer(question, selected, isCorrect, points),
            buildQuestionResult(question, selected, correct, isCorrect, points)
    );
}

protected abstract boolean isCorrectSelection(Quiz.Question question, List<String> selected, List<String> correct);
```

Ví dụ 1 concrete override hook (MULTIPLE_CHOICE):

```java
@Override
protected boolean isCorrectSelection(Quiz.Question question, List<String> selected, List<String> correct) {
    return selected.size() == correct.size()
            && selected.containsAll(correct)
            && correct.containsAll(selected);
}
```

### Where used
- Template: `src/main/java/com/example/cake/quiz/service/scoring/AbstractOptionBasedScoringStrategy.java`
- Concrete:
  - `src/main/java/com/example/cake/quiz/service/scoring/SingleChoiceScoringStrategy.java`
  - `src/main/java/com/example/cake/quiz/service/scoring/MultipleChoiceScoringStrategy.java`
  - `src/main/java/com/example/cake/quiz/service/scoring/TrueFalseScoringStrategy.java`

---

## 10) Adapter – PayOS Webhook Adapter

### Intent
PayOS webhook có schema JSON riêng (adaptee). Adapter chuyển payload PayOS → params map chuẩn hoá (target) để tái sử dụng `PaymentFacade / PaymentGateway` mà không phải sửa logic nghiệp vụ.

### Structure
- **Adaptee**: JSON schema của PayOS (rawBody)
- **Adapter**: `PayOSWebhookAdapter`
- **Target**: `Map<String,String>` params (keys: orderCode, signature, rawBody, code, success, ...)
- **Client**: `PaymentController.payOSWebhook(...)`

### Code

```java
// (trích) PaymentController dùng adapter
// PayOSWebhookAdapter.AdaptedWebhook adapted = payOSWebhookAdapter.adapt(rawBody, headers);
// return paymentService.processPayOSCallback(adapted.getParams());
```

### Where used
- Adapter: `src/main/java/com/example/cake/payment/service/payos/PayOSWebhookAdapter.java`
- Client: `src/main/java/com/example/cake/payment/controller/PaymentController.java`

---

## 11) Decorator – PaymentGateway logging/timing

### Intent
Thêm tính năng logging/timing cho gateway mà không thay đổi logic của gateway thật (Open/Closed Principle).

### Structure
- **Component interface**: `PaymentGateway`
- **Concrete component**: `PayOSGateway`
- **Decorator**: `LoggingPaymentGatewayDecorator` (wrap 1 `PaymentGateway delegate`)
- **Client/Builder**: `PaymentGatewayFactory` wrap gateways bằng decorator

### Code
Decorator skeleton:

```java
public class LoggingPaymentGatewayDecorator implements PaymentGateway {
    private final PaymentGateway delegate;

    @Override
    public String createPaymentUrl(Payment payment, String orderInfo, String ipAddress) {
        long start = System.nanoTime();
        try {
            String url = delegate.createPaymentUrl(payment, orderInfo, ipAddress);
            log.info("createPaymentUrl elapsedMs={}", (System.nanoTime() - start) / 1_000_000);
            return url;
        } catch (Exception e) {
            log.error("createPaymentUrl FAILED", e);
            throw e;
        }
    }
}
```

### Where used
- Decorator: `src/main/java/com/example/cake/payment/service/gateway/LoggingPaymentGatewayDecorator.java`
- Wrapped in: `src/main/java/com/example/cake/payment/service/gateway/PaymentGatewayFactory.java`

---

## 12) Chain of Responsibility – Payment creation validation

### Intent
Tách validation create payment thành các bước nhỏ (handlers) và nối chuỗi xử lý. Dễ mở rộng thêm rule mới mà không làm `PaymentFacade.createPayment()` phình to.

### Structure
- **Context**: `PaymentCreationContext` (dữ liệu đầu vào + kết quả)
- **Handler interface**: `PaymentCreationHandler`
- **Base handler**: `AbstractPaymentCreationHandler` (giữ next pointer)
- **Concrete handlers**:
  - `CourseExistenceAndEligibilityHandler`
  - `BuildPaymentItemsAndTotalHandler`
- **Chain builder**: `PaymentCreationValidationChain`
- **Client**: `PaymentFacade.createPayment()` gọi chain trước khi tạo payment

### Code

```java
// (trích) PaymentFacade gọi chain
// paymentCreationValidationChain.validate(ctx);
// if (!ctx.isValid()) return new ResponseMessage<>(false, ctx.getErrorMessage(), null);
```

---

## 13) Payment (Strategy/Factory/Facade/Observer) – (tham khảo module payment)

### Intent
Mở rộng dễ dàng nhiều cổng thanh toán (PayOS/VNPay/MoMo/…) và tách orchestration khỏi controller.

### Structure
- Strategy: `PaymentGateway` + các implementation trong `payment/service/gateway/*`
- Factory: `PaymentGatewayFactory`
- Facade: `PaymentFacade`
- Observer: payment events + listeners (tuỳ theo code payment hiện tại của bạn)

### Where used (trỏ file)
- `src/main/java/com/example/cake/payment/service/facade/PaymentFacade.java`
- `src/main/java/com/example/cake/payment/service/gateway/*`
- `src/main/java/com/example/cake/payment/service/gateway/PaymentGatewayFactory.java`

---

## 14) Các pattern trong danh sách nhưng hiện CHƯA áp dụng (để ghi rõ trong báo cáo)
- Singleton (tự viết): chưa (Spring beans thường đã singleton theo container)
- Memento: chưa (khuyến nghị làm ở FE React – undo/redo form)
- Prototype: chưa (khuyến nghị làm ở FE React – clone question/lesson block)
- Bridge: chưa
- Visitor: chưa
