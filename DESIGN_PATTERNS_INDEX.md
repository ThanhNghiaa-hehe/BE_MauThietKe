# Design Patterns Index (vị trí code để dán vào báo cáo)

File này dùng để bạn **copy/paste code** của từng mẫu thiết kế vào bài báo cáo môn **"Mẫu thiết kế phần mềm"**.

> Repo: Spring Boot (Java 17) – Course Selling / E-learning Backend
>
> Cập nhật: 2026-03-27

---

## 1) Dependency Injection (DI) – Spring

**Mô tả ngắn**
- Các class `@Service`, `@Component`, `@Controller` nhận phụ thuộc qua constructor injection.

**Vị trí tiêu biểu**
- `src/main/java/com/example/cake/quiz/service/QuizService.java`
- `src/main/java/com/example/cake/lesson/service/ProgressService.java`
- `src/main/java/com/example/cake/lesson/service/CurriculumFacade.java`

**Đoạn code tiêu biểu**
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
}
```

---

## 2) Repository Pattern – Spring Data MongoDB

**Mô tả ngắn**
- Dùng repository để truy cập DB thay vì viết DAO thủ công.

**Vị trí tiêu biểu**
- `src/main/java/com/example/cake/quiz/repository/QuizRepository.java`
- `src/main/java/com/example/cake/quiz/repository/QuizAttemptRepository.java`
- `src/main/java/com/example/cake/lesson/repository/LessonRepository.java`
- `src/main/java/com/example/cake/lesson/repository/UserProgressRepository.java`

**Đoạn code tiêu biểu** (ví dụ dạng repository)
```java
public interface LessonRepository extends MongoRepository<Lesson, String> {
    List<Lesson> findByCourseIdOrderByOrderAsc(String courseId);
}
```

---

## 3) Strategy Pattern – Quiz Scoring

**Mô tả ngắn**
- Tách thuật toán chấm điểm theo loại câu hỏi thành các strategy riêng.

**Vị trí**
- Interface: `src/main/java/com/example/cake/quiz/service/scoring/QuestionScoringStrategy.java`
- Concrete strategies:
  - `src/main/java/com/example/cake/quiz/service/scoring/SingleChoiceScoringStrategy.java`
  - `src/main/java/com/example/cake/quiz/service/scoring/MultipleChoiceScoringStrategy.java`
  - `src/main/java/com/example/cake/quiz/service/scoring/TrueFalseScoringStrategy.java`

**Đoạn code tiêu biểu**
```java
public interface QuestionScoringStrategy {
    Quiz.QuestionType supports();
    ScoredQuestion score(Quiz.Question question, List<String> selectedOptions);
}
```

---

## 4) Factory Method – Strategy Factory (Quiz)

**Mô tả ngắn**
- Factory chọn strategy phù hợp dựa trên `Quiz.QuestionType`.

**Vị trí**
- `src/main/java/com/example/cake/quiz/service/scoring/QuestionScoringStrategyFactory.java`

**Đoạn code tiêu biểu**
```java
public QuestionScoringStrategy get(Quiz.QuestionType type) {
    QuestionScoringStrategy s = map.get(type);
    if (s == null) throw new IllegalArgumentException("Unsupported question type: " + type);
    return s;
}
```

---

## 5) Observer – Quiz Submitted Event

**Mô tả ngắn**
- Khi user submit quiz, service phát event. Listener nhận event để xử lý side-effect (cập nhật progress).

**Vị trí**
- Event: `src/main/java/com/example/cake/quiz/service/event/QuizSubmittedEvent.java`
- Listener: `src/main/java/com/example/cake/quiz/service/listener/QuizProgressListener.java`
- Publish event: `src/main/java/com/example/cake/quiz/service/QuizService.java`

**Đoạn code tiêu biểu**
```java
@EventListener
public void onQuizSubmitted(QuizSubmittedEvent event) {
    if (Boolean.TRUE.equals(event.getPassed()) && event.getLessonId() != null) {
        progressService.updateQuizPassed(event.getUserId(), event.getLessonId(), event.getPercentage());
    }
}
```

---

## 6) State – Progress lifecycle

**Mô tả ngắn**
- Tiến độ học có workflow rõ ràng (NOT_ENROLLED → ENROLLED/IN_PROGRESS → COMPLETED).
- Rule transition được tách vào state actions (`completeLesson`, `updateVideoProgress`).

**Vị trí**
- `src/main/java/com/example/cake/lesson/service/progress/ProgressState.java`
- `src/main/java/com/example/cake/lesson/service/progress/ProgressStateResolver.java`
- Concrete states:
  - `src/main/java/com/example/cake/lesson/service/progress/NotEnrolledState.java`
  - `src/main/java/com/example/cake/lesson/service/progress/EnrolledState.java`
  - `src/main/java/com/example/cake/lesson/service/progress/InProgressState.java`
  - `src/main/java/com/example/cake/lesson/service/progress/CompletedState.java`
- Dùng trong: `src/main/java/com/example/cake/lesson/service/ProgressService.java`

**Đoạn code tiêu biểu**
```java
public interface ProgressState {
    UserProgressStatus getStatus();
    StateTransitionResult completeLesson(ProgressContext ctx);
    StateTransitionResult updateVideoProgress(ProgressContext ctx);
}
```

---

## 7) Observer – Progress events (LessonCompleted/CourseCompleted)

**Mô tả ngắn**
- Tách side-effects sau khi complete lesson/course: analytics, achievements, notifications...

**Vị trí**
- Events:
  - `src/main/java/com/example/cake/lesson/service/event/LessonCompletedEvent.java`
  - `src/main/java/com/example/cake/lesson/service/event/CourseCompletedEvent.java`
- Publish trong: `src/main/java/com/example/cake/lesson/service/ProgressService.java`
- Listener:
  - `src/main/java/com/example/cake/lesson/service/listener/ProgressAchievementListener.java`

---

## 8) Facade – Curriculum

**Mô tả ngắn**
- Gom logic lấy curriculum (chapter + lesson) vào 1 lớp Facade.
- Giữ nguyên endpoint PUBLIC hiện có để không phá FE.
- Thêm endpoint USER mới trả `unlocked/completed/videoProgress`.

**Vị trí**
- Facade: `src/main/java/com/example/cake/lesson/service/CurriculumFacade.java`
- PUBLIC controller:
  - `src/main/java/com/example/cake/lesson/controller/CurriculumController.java`
- USER controller:
  - `src/main/java/com/example/cake/lesson/controller/UserCurriculumController.java`
- DTO user-view:
  - `src/main/java/com/example/cake/lesson/dto/CurriculumUserViewDTO.java`

**Đoạn code tiêu biểu**
```java
public ResponseMessage<CurriculumUserViewDTO> getCurriculumForUser(String userId, String courseId) {
    UserProgress progress = userProgressRepository.findByUserIdAndCourseId(userId, courseId).orElse(null);
    List<Chapter> chapters = chapterRepository.findByCourseIdOrderByOrderAsc(courseId);
    List<Lesson> lessons = lessonRepository.findByCourseIdOrderByOrderAsc(courseId);
    // ... aggregate + unlocked/completed/videoProgress ...
    return new ResponseMessage<>(true, "Success", dto);
}
```

---

## 9) Template Method – Quiz option-based scoring

**Mô tả ngắn**
- Chuẩn hoá thuật toán chấm điểm câu hỏi dạng chọn đáp án theo 1 “khung” cố định.
- Các strategy chỉ override **hook** `isCorrectSelection(...)`.

**Vị trí**
- Template (abstract class):
  - `src/main/java/com/example/cake/quiz/service/scoring/AbstractOptionBasedScoringStrategy.java`
- Concrete hooks:
  - `src/main/java/com/example/cake/quiz/service/scoring/SingleChoiceScoringStrategy.java`
  - `src/main/java/com/example/cake/quiz/service/scoring/MultipleChoiceScoringStrategy.java`
  - `src/main/java/com/example/cake/quiz/service/scoring/TrueFalseScoringStrategy.java`

**Đoạn code tiêu biểu (Template Method)**
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

---

## 10) Adapter – PayOS Webhook Adapter

**Mô tả ngắn**
- PayOS webhook có schema JSON riêng. Adapter chuyển *PayOS payload* → *params map chuẩn hoá* để giữ nguyên logic cũ downstream.

**Vị trí**
- Adapter:
  - `src/main/java/com/example/cake/payment/service/payos/PayOSWebhookAdapter.java`
- Dùng trong controller:
  - `src/main/java/com/example/cake/payment/controller/PaymentController.java`

**Đoạn code tiêu biểu**
```java
PayOSWebhookAdapter.AdaptedWebhook adapted = payOSWebhookAdapter.adapt(rawBody, headers);
if (adapted.getParams() == null) {
    return ResponseEntity.ok(new ResponseMessage<>(false,
            adapted.getErrorMessage() != null ? adapted.getErrorMessage() : "Webhook payload không hợp lệ",
            null));
}
return ResponseEntity.ok(paymentService.processPayOSCallback(adapted.getParams()));
```

---

## 11) Decorator – PaymentGateway logging/timing

**Mô tả ngắn**
- Bọc `PaymentGateway` bằng decorator để thêm logging/timing mà không thay đổi hành vi.

**Vị trí**
- Decorator:
  - `src/main/java/com/example/cake/payment/service/gateway/LoggingPaymentGatewayDecorator.java`
- Nơi wrap:
  - `src/main/java/com/example/cake/payment/service/gateway/PaymentGatewayFactory.java`

---

## 12) Chain of Responsibility – Payment creation validation

**Mô tả ngắn**
- Chuỗi handler validate create payment (course tồn tại / published / chưa enroll / tổng tiền hợp lệ).
- Giữ nguyên message + rule cũ, chỉ refactor để đúng pattern.

**Vị trí**
- Context:
  - `src/main/java/com/example/cake/payment/service/validation/PaymentCreationContext.java`
- Handler interface/base:
  - `src/main/java/com/example/cake/payment/service/validation/PaymentCreationHandler.java`
  - `src/main/java/com/example/cake/payment/service/validation/AbstractPaymentCreationHandler.java`
- Concrete handlers:
  - `src/main/java/com/example/cake/payment/service/validation/CourseExistenceAndEligibilityHandler.java`
  - `src/main/java/com/example/cake/payment/service/validation/BuildPaymentItemsAndTotalHandler.java`
- Chain builder:
  - `src/main/java/com/example/cake/payment/service/validation/PaymentCreationValidationChain.java`
- Usage:
  - `src/main/java/com/example/cake/payment/service/facade/PaymentFacade.java`

---

## 13) Payment – Strategy/Factory/Facade/Observer (đã có trong module payment)

**Vị trí tham khảo**
- `src/main/java/com/example/cake/payment/service/facade/PaymentFacade.java`
- `src/main/java/com/example/cake/payment/service/gateway/*`
- `src/main/java/com/example/cake/payment/service/gateway/PaymentGatewayFactory.java`

---

## 14) Các pattern trong danh sách nhưng hiện CHƯA áp dụng
- Singleton (tự viết): chưa (Spring container singleton)
- Memento: chưa (phù hợp FE React)
- Prototype: chưa (phù hợp FE React)
- Bridge: chưa
- Visitor: chưa
