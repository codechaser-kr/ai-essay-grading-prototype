# API 설계

## 기본 원칙

- API prefix는 `/api`입니다.
- Entity는 직접 노출하지 않고 DTO로 응답합니다.
- 요청 검증은 Bean Validation으로 처리합니다.
- 에러 응답은 사용자용 메시지와 개발자용 메시지를 분리합니다.
- Swagger UI는 `http://localhost:8080/swagger-ui.html`에서 확인합니다.

## API 목록

| 기능 | Method | Path |
| --- | --- | --- |
| 문제 생성 | POST | `/api/questions` |
| 문제 목록 조회 | GET | `/api/questions` |
| 문제 상세 조회 | GET | `/api/questions/{questionId}` |
| 채점 요청 | POST | `/api/grading-requests` |
| 채점 결과 상세 조회 | GET | `/api/grading-results/{gradingResultId}` |
| 문제별 채점 이력 조회 | GET | `/api/grading-results?questionId={questionId}` |

## 문제 생성

```http
POST /api/questions
```

Request:

```json
{
  "title": "탄소 중립의 의미 설명",
  "subject": "science",
  "content": "탄소 중립이 무엇인지 설명하고, 실천 방법을 두 가지 이상 서술하시오.",
  "modelAnswer": "탄소 중립은 배출한 이산화탄소의 양만큼 흡수하거나 감축하여 실질 배출량을 0으로 만드는 것이다.",
  "totalScore": 100,
  "rubricItems": [
    {
      "name": "개념 이해",
      "criteria": "탄소 중립의 의미를 정확히 설명한다.",
      "maxScore": 40,
      "sortOrder": 1
    },
    {
      "name": "실천 방안",
      "criteria": "실천 방법을 두 가지 이상 구체적으로 제시한다.",
      "maxScore": 40,
      "sortOrder": 2
    },
    {
      "name": "표현 명확성",
      "criteria": "문장이 명확하고 논리적으로 구성되어 있다.",
      "maxScore": 20,
      "sortOrder": 3
    }
  ]
}
```

Response:

```json
{
  "id": 1,
  "title": "탄소 중립의 의미 설명",
  "subject": "science",
  "content": "탄소 중립이 무엇인지 설명하고, 실천 방법을 두 가지 이상 서술하시오.",
  "modelAnswer": "탄소 중립은 배출한 이산화탄소의 양만큼 흡수하거나 감축하여 실질 배출량을 0으로 만드는 것이다.",
  "totalScore": 100,
  "rubricItems": [
    {
      "id": 1,
      "name": "개념 이해",
      "criteria": "탄소 중립의 의미를 정확히 설명한다.",
      "maxScore": 40,
      "sortOrder": 1
    }
  ],
  "createdAt": "2026-05-27T13:00:00",
  "updatedAt": "2026-05-27T13:00:00"
}
```

검증:

- `title`, `subject`, `content`, `modelAnswer`는 필수입니다.
- `totalScore`는 1 이상입니다.
- `rubricItems`는 1개 이상입니다.
- rubric `maxScore` 합계는 `totalScore`와 일치해야 합니다.

## 문제 목록 조회

```http
GET /api/questions
```

Response:

```json
[
  {
    "id": 1,
    "title": "탄소 중립의 의미 설명",
    "subject": "science",
    "totalScore": 100,
    "rubricItemCount": 3,
    "createdAt": "2026-05-27T13:00:00",
    "updatedAt": "2026-05-27T13:00:00"
  }
]
```

## 문제 상세 조회

```http
GET /api/questions/{questionId}
```

Response는 문제 생성 응답과 같은 `QuestionData` 구조를 사용합니다.

## 채점 요청

```http
POST /api/grading-requests
```

Request:

```json
{
  "questionId": 1,
  "studentAnswer": "탄소 중립은 이산화탄소를 아예 배출하지 않는 것입니다. 실천 방법으로는 대중교통 이용과 전기 절약이 있습니다."
}
```

Response:

```json
{
  "gradingRequestId": 1,
  "gradingResultId": 1,
  "status": "COMPLETED",
  "totalScore": 78,
  "reviewRequired": true
}
```

채점 처리는 동기 방식입니다. 선택된 LLM Provider가 정상 응답을 반환하고 백엔드 검증을 통과하면 즉시 `COMPLETED` 상태를 반환합니다.

## 채점 결과 상세 조회

```http
GET /api/grading-results/{gradingResultId}
```

Response:

```json
{
  "id": 1,
  "gradingRequestId": 1,
  "questionId": 1,
  "studentAnswer": "탄소 중립은 이산화탄소를 아예 배출하지 않는 것입니다.",
  "modelName": "gemini-2.5-flash",
  "promptVersionName": "gemini-grading-v2",
  "totalScore": 78,
  "maxScore": 100,
  "confidence": "MEDIUM",
  "reviewRequired": true,
  "rubricScores": [
    {
      "rubricItemName": "개념 이해",
      "score": 32,
      "maxScore": 40,
      "reason": "개념 이해 기준을 일부 충족했지만 보완할 부분이 있습니다."
    },
    {
      "rubricItemName": "실천 방안",
      "score": 30,
      "maxScore": 40,
      "reason": "실천 방법은 제시했지만 구체적인 설명이 부족합니다."
    },
    {
      "rubricItemName": "표현 명확성",
      "score": 16,
      "maxScore": 20,
      "reason": "문장은 이해 가능하지만 일부 표현이 단정적입니다."
    }
  ],
  "deductions": [
    {
      "rubricItemName": "개념 이해",
      "pointsLost": 8,
      "reason": "개념 이해 항목에서 설명의 정확성 또는 구체성이 부족합니다."
    },
    {
      "rubricItemName": "실천 방안",
      "pointsLost": 10,
      "reason": "실천 방법의 효과와 연결 설명이 부족합니다."
    },
    {
      "rubricItemName": "표현 명확성",
      "pointsLost": 4,
      "reason": "일부 문장이 모범 답안의 개념과 다르게 해석될 수 있습니다."
    }
  ],
  "studentFeedback": "답변의 방향은 적절하지만 탄소 중립의 핵심 개념을 더 정확하게 설명할 필요가 있습니다.",
  "learningPoints": [
    "문제의 핵심 개념을 모범 답안과 비교해 복습하세요.",
    "평가 기준별로 빠진 내용을 한 문장씩 보완하는 연습을 하세요."
  ],
  "reviewReasons": [
    "핵심 개념 설명에 오개념 또는 누락 가능성이 있습니다."
  ],
  "createdAt": "2026-05-27T13:00:00"
}
```

## 문제별 채점 이력 조회

```http
GET /api/grading-results?questionId=1
```

Response:

```json
[
  {
    "id": 1,
    "gradingRequestId": 1,
    "questionId": 1,
    "studentAnswer": "탄소 중립은 이산화탄소를 아예 배출하지 않는 것입니다.",
    "modelName": "gemini-2.5-flash",
    "promptVersionName": "gemini-grading-v2",
    "totalScore": 78,
    "maxScore": 100,
    "confidence": "MEDIUM",
    "reviewRequired": true,
    "rubricScores": [
      {
        "rubricItemName": "개념 이해",
        "score": 32,
        "maxScore": 40,
        "reason": "개념 이해 기준을 일부 충족했지만 보완할 부분이 있습니다."
      },
      {
        "rubricItemName": "실천 방안",
        "score": 30,
        "maxScore": 40,
        "reason": "실천 방법은 제시했지만 구체적인 설명이 부족합니다."
      },
      {
        "rubricItemName": "표현 명확성",
        "score": 16,
        "maxScore": 20,
        "reason": "문장은 이해 가능하지만 일부 표현이 단정적입니다."
      }
    ],
    "deductions": [
      {
        "rubricItemName": "개념 이해",
        "pointsLost": 8,
        "reason": "개념 이해 항목에서 설명의 정확성 또는 구체성이 부족합니다."
      },
      {
        "rubricItemName": "실천 방안",
        "pointsLost": 10,
        "reason": "실천 방법의 효과와 연결 설명이 부족합니다."
      },
      {
        "rubricItemName": "표현 명확성",
        "pointsLost": 4,
        "reason": "일부 문장이 모범 답안의 개념과 다르게 해석될 수 있습니다."
      }
    ],
    "studentFeedback": "답변의 방향은 적절하지만 탄소 중립의 핵심 개념을 더 정확하게 설명할 필요가 있습니다.",
    "learningPoints": [
      "문제의 핵심 개념을 모범 답안과 비교해 복습하세요.",
      "평가 기준별로 빠진 내용을 한 문장씩 보완하는 연습을 하세요."
    ],
    "reviewReasons": [
      "핵심 개념 설명에 오개념 또는 누락 가능성이 있습니다."
    ],
    "createdAt": "2026-05-27T13:00:00"
  }
]
```

## 에러 응답

```json
{
  "message": "요청 값을 확인해주세요.",
  "developerMessage": "studentAnswer must not be blank",
  "status": 400,
  "path": "/api/grading-requests",
  "timestamp": "2026-05-27T13:00:00"
}
```

상태 코드:

- `200 OK`: 조회 성공
- `201 Created`: 생성 성공
- `400 Bad Request`: 요청 검증 실패 또는 비즈니스 검증 실패
- `404 Not Found`: 리소스 없음
- `500 Internal Server Error`: 서버 내부 오류
