# PlantText (PlantUML) – Design Pattern Diagrams

Tài liệu này chứa **mã PlantUML (PlantText)** để bạn dán trực tiếp vào **PlantText** rồi export hình cho báo cáo.

> Mặc định các sơ đồ bên dưới là **Class Diagram**.

---

## 1) Strategy Pattern – Class diagram (Quiz Scoring)

**Ánh xạ theo dự án**
- Strategy interface: `QuestionScoringStrategy`
- Concrete strategies: `SingleChoiceScoringStrategy`, `MultipleChoiceScoringStrategy`, `TrueFalseScoringStrategy`
- Context/caller: `QuizGrader`

```plantuml
@startuml
' CLASS DIAGRAM

title Strategy Pattern (Class Diagram) - Quiz question scoring
skinparam classAttributeIconSize 0

package "com.example.cake.quiz.service.scoring" {
  interface QuestionScoringStrategy {
    +supports(): Quiz.QuestionType
    +score(question: Quiz.Question, selectedOptions: List<String>): ScoredQuestion
  }

  abstract class AbstractOptionBasedScoringStrategy

  class SingleChoiceScoringStrategy
  class MultipleChoiceScoringStrategy
  class TrueFalseScoringStrategy

  class QuizGrader {
    -factory: QuestionScoringStrategyFactory
    +grade(quiz: Quiz, userAnswers: List<AnswerRequest>): GradingResult
  }
}

QuestionScoringStrategy <|.. AbstractOptionBasedScoringStrategy
AbstractOptionBasedScoringStrategy <|-- SingleChoiceScoringStrategy
AbstractOptionBasedScoringStrategy <|-- MultipleChoiceScoringStrategy
AbstractOptionBasedScoringStrategy <|-- TrueFalseScoringStrategy

QuizGrader --> QuestionScoringStrategyFactory : resolves
QuizGrader ..> QuestionScoringStrategy : uses

note right of QuestionScoringStrategy
Strategy: encapsulate scoring algorithm
per QuestionType.
end note

@enduml
```

---

## 2) Factory Method – Class diagram (Quiz Strategy Factory)

**Ánh xạ theo dự án**
- Factory: `QuestionScoringStrategyFactory`
- Products: `QuestionScoringStrategy` + concrete strategies

```plantuml
@startuml
' CLASS DIAGRAM

title Factory Method (Class Diagram) - Resolve scoring strategy by QuestionType
skinparam classAttributeIconSize 0

package "com.example.cake.quiz.service.scoring" {
  enum QuestionType {
    SINGLE_CHOICE
    MULTIPLE_CHOICE
    TRUE_FALSE
  }

  interface QuestionScoringStrategy

  class SingleChoiceScoringStrategy
  class MultipleChoiceScoringStrategy
  class TrueFalseScoringStrategy

  class QuestionScoringStrategyFactory {
    -map: Map<QuestionType, QuestionScoringStrategy>
    +get(type: QuestionType): QuestionScoringStrategy
  }
}

QuestionScoringStrategy <|.. SingleChoiceScoringStrategy
QuestionScoringStrategy <|.. MultipleChoiceScoringStrategy
QuestionScoringStrategy <|.. TrueFalseScoringStrategy

QuestionScoringStrategyFactory o-- QuestionScoringStrategy : registry

note bottom of QuestionScoringStrategyFactory
Factory Method: hide selection logic.
Extending types requires adding a new
strategy (Spring registers it).
end note

@enduml
```

---

## 3) Observer Pattern – Class diagram (Quiz submitted)

**Ánh xạ theo dự án**
- Publisher: `QuizService`
- Event: `QuizSubmittedEvent`
- Listener: `QuizProgressListener`
- Side-effect: `ProgressService.updateQuizPassed(...)`

```plantuml
@startuml
' CLASS DIAGRAM

title Observer Pattern (Class Diagram) - QuizSubmittedEvent
skinparam classAttributeIconSize 0

package "com.example.cake.quiz.service" {
  class QuizService {
    -eventPublisher: ApplicationEventPublisher
    +submitQuiz(userId: String, request: QuizSubmitRequest): QuizResultResponse
  }
}

package "com.example.cake.quiz.service.event" {
  class QuizSubmittedEvent {
    +userId: String
    +quizId: String
    +lessonId: String
    +percentage: Double
    +passed: Boolean
  }
}

package "com.example.cake.quiz.service.listener" {
  class QuizProgressListener {
    -progressService: ProgressService
    +onQuizSubmitted(event: QuizSubmittedEvent)
  }
}

package "com.example.cake.lesson.service" {
  class ProgressService {
    +updateQuizPassed(userId: String, lessonId: String, score: Double)
  }
}

QuizService ..> QuizSubmittedEvent : publishes
QuizProgressListener ..> QuizSubmittedEvent : observes
QuizProgressListener --> ProgressService : calls

note right of QuizProgressListener
Observer: side-effects are moved
out of QuizService.
end note

@enduml
```

---

## 4) State Pattern – Class diagram (Progress lifecycle)

**Ánh xạ theo dự án**
- State interface: `ProgressState`
- Concrete states: `NotEnrolledState`, `EnrolledState`, `InProgressState`, `CompletedState`
- Resolver: `ProgressStateResolver`
- Context: `ProgressContext`
- Client: `ProgressService`

```plantuml
@startuml
' CLASS DIAGRAM

title State Pattern (Class Diagram) - UserProgress lifecycle
skinparam classAttributeIconSize 0

package "com.example.cake.lesson.service.progress" {
  interface ProgressState {
    +getStatus(): UserProgressStatus
    +completeLesson(ctx: ProgressContext): StateTransitionResult
    +updateVideoProgress(ctx: ProgressContext): StateTransitionResult
  }

  class NotEnrolledState
  class EnrolledState
  class InProgressState
  class CompletedState

  class ProgressStateResolver {
    +resolve(progress: UserProgress): ProgressState
  }

  class ProgressContext {
    +progress: UserProgress
    +lesson: Lesson
    +percent: Integer
    +canAccessLesson: boolean
    +alreadyCompleted: boolean
  }

  class StateTransitionResult {
    +allowed: boolean
    +message: String
  }

  enum UserProgressStatus {
    NOT_ENROLLED
    ENROLLED
    IN_PROGRESS
    COMPLETED
  }
}

package "com.example.cake.lesson.service" {
  class ProgressService {
    -stateResolver: ProgressStateResolver
    +markLessonComplete(userId: String, lessonId: String)
    +updateVideoProgress(userId: String, lessonId: String, percent: Integer)
  }
}

ProgressState <|.. NotEnrolledState
ProgressState <|.. EnrolledState
ProgressState <|.. InProgressState
ProgressState <|.. CompletedState

ProgressService --> ProgressStateResolver : resolve()
ProgressStateResolver --> ProgressState : returns
ProgressService ..> ProgressContext : builds
ProgressService ..> ProgressState : delegates

note right of ProgressState
State: encapsulate transition rules
inside state methods.
end note

@enduml
```

---

## 5) (Tuỳ chọn) Sequence diagram – submit quiz

Nếu thầy/cô yêu cầu thêm **sequence diagram**, bạn có thể dùng block dưới (không phải class diagram):

```plantuml
@startuml
' SEQUENCE DIAGRAM (OPTIONAL)

title Sequence - User submits quiz

actor User
participant "QuizUserController" as C
participant "QuizService" as S
participant "QuizGrader" as G
participant "QuestionScoringStrategyFactory" as F
participant "QuestionScoringStrategy" as ST
participant "QuizAttemptRepository" as R
participant "ApplicationEventPublisher" as P
participant "QuizProgressListener" as L
participant "ProgressService" as PS

User -> C : POST /api/quizzes/submit
C -> S : submitQuiz(userId, request)
S -> G : grade(quiz, answers)
loop each Question
  G -> F : get(question.type)
  F --> G : strategy
  G -> ST : score(question, selected)
  ST --> G : ScoredQuestion
end
G --> S : GradingResult
S -> R : save(QuizAttempt)
R --> S : attempt
S -> P : publish(QuizSubmittedEvent)
P -> L : onQuizSubmitted(event)
L -> PS : updateQuizPassed(...)
PS --> L : ResponseMessage
S --> C : QuizResultResponse
C --> User : 200 OK
@enduml
```
