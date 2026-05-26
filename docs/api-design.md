# API 설계 초안

## 기본 원칙

- API prefix는 `/api`를 사용합니다.
- Entity를 직접 응답하지 않고 DTO를 사용합니다.
- 요청 검증은 Bean Validation으로 처리합니다.
- 에러 응답은 사용자용 메시지와 개발자용 메시지를 분리합니다.

## API 목록

| 기능 | Method | Path |
| --- | --- | --- |
| 문제 생성 | POST | `/api/questions` |
| 문제 목록 조회 | GET | `/api/questions` |
| 문제 상세 조회 | GET | `/api/questions/{questionId}` |
| 채점 요청 | POST | `/api/grading-requests` |
| 채점 결과 상세 조회 | GET | `/api/grading-results/{gradingResultId}` |
| 문제별 채점 이력 조회 | GET | `/api/grading-results?questionId={questionId}` |

## 문제 생성 Request 예시

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

## 채점 요청 Request 예시

```json
{
  "questionId": 1,
  "studentAnswer": "탄소 중립은 이산화탄소를 아예 배출하지 않는 것입니다. 실천 방법으로는 대중교통 이용과 전기 절약이 있습니다."
}
```

## 채점 요청 Response 예시

```json
{
  "gradingRequestId": 1,
  "gradingResultId": 1,
  "status": "COMPLETED",
  "totalScore": 72,
  "reviewRequired": true
}
```

## 에러 응답 형식 초안

```json
{
  "message": "요청 값을 확인해주세요.",
  "developerMessage": "studentAnswer must not be blank",
  "status": 400,
  "path": "/api/grading-requests",
  "timestamp": "2026-05-26T10:00:00"
}
```

## 상태 코드

- `200 OK`: 조회 성공
- `201 Created`: 생성 성공
- `400 Bad Request`: 요청 검증 실패
- `404 Not Found`: 리소스 없음
- `500 Internal Server Error`: 서버 내부 오류
