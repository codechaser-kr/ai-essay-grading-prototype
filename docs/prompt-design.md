# 프롬프트 설계

## 목표

프롬프트는 서술형 답안을 rubric 기준으로 일관되게 채점하고, 백엔드가 검증 가능한 JSON 구조로 결과를 반환하도록 설계합니다.

현재 실제 LLM 호출은 `GeminiGradingAiClient`가 담당합니다. `MockGradingAiClient`는 외부 API 없이 같은 응답 구조를 검증하는 로컬 Provider입니다.

## 현재 구조

현재 채점 결과는 다음 흐름으로 생성됩니다.

```text
GradingService
  -> GradingAiClient.grade()
  -> GeminiGradingAiClient 또는 MockGradingAiClient
  -> Gemini 응답 파싱 및 rubric 기준 보정
  -> GradingResultValidator
  -> grading_results.result_json 저장
```

저장되는 메타데이터:

- `modelName`: `gemini-2.5-flash` 또는 `mock-grading-model`
- `promptVersionName`: `gemini-grading-v1` 또는 `mock-v1`
- `resultJson`: 구조화된 채점 결과 JSON
- `rawResponse`: LLM 원본 응답 또는 Mock 원본 응답 문자열

## Gemini Prompt 규칙

```text
당신은 한국어 서술형 답안을 채점하는 보조 교사입니다.
문제, 모범 답안, 평가 기준을 기준으로 학생 답안을 엄격하게 채점하세요.
반드시 제공된 JSON schema에 맞는 JSON만 반환하세요.

채점 규칙:
- totalScore는 rubricScores.score 합계와 정확히 일치해야 합니다.
- maxScore는 문제 총점과 같아야 합니다.
- 모든 평가 항목은 rubricScores에 정확히 한 번씩 포함되어야 합니다.
- 각 rubricScores.maxScore는 제공된 평가 항목의 maxScore와 일치해야 합니다.
- deductions에는 제공된 평가 항목명만 사용해야 합니다.
- confidence는 HIGH, MEDIUM, LOW 중 하나여야 합니다.
- confidence가 LOW이면 reviewRequired는 반드시 true여야 합니다.
- 학생 답안에 오개념, 핵심 개념 누락, 모범 답안과의 의미 차이가 있으면 관련 평가 항목을 만점으로 줄 수 없습니다.
- reviewReasons에 점수 조정 필요성, 오개념, 누락, 불확실성을 적었다면 관련 rubricScores에서 반드시 감점해야 합니다.
- 학생 답안이 부분적으로 맞더라도 정의가 틀렸거나 과도하게 단정적이면 개념 이해 항목에서 감점하세요.
- 피드백, 채점 사유, 학습 포인트, 재검토 사유는 모두 한국어로 작성하세요.
```

## User Prompt 구성

```text
문제 제목:
{{questionTitle}}

문제:
{{questionContent}}

모범 답안:
{{question.modelAnswer}}

학생 답안:
{{studentAnswer}}

총점:
{{totalScore}}

평가 기준:
{{rubricItems}}
```

## 응답 JSON 예시

```json
{
  "totalScore": 78,
  "maxScore": 100,
  "rubricScores": [
    {
      "rubricItemName": "개념 이해",
      "score": 32,
      "maxScore": 40,
      "reason": "핵심 개념은 대체로 이해했지만 일부 표현이 부정확합니다."
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
      "reason": "탄소 중립을 이산화탄소를 전혀 배출하지 않는 것으로 설명했습니다."
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
  "studentFeedback": "답변의 방향은 적절하지만 핵심 개념을 더 정확히 설명할 필요가 있습니다.",
  "learningPoints": [
    "탄소 중립의 정확한 정의 복습",
    "구체적인 실천 사례 추가 연습"
  ],
  "confidence": "MEDIUM",
  "reviewRequired": true,
  "reviewReasons": [
    "핵심 개념 설명에 오개념 가능성이 있습니다."
  ]
}
```

## JSON 응답 Schema

Gemini Provider는 `generationConfig.responseMimeType=application/json`과 `responseSchema`를 사용해 다음 구조의 JSON 응답을 요청합니다. 백엔드 저장 전에는 이 schema와 별도로 자체 검증을 다시 수행합니다.

```json
{
  "type": "object",
  "required": [
    "totalScore",
    "maxScore",
    "rubricScores",
    "deductions",
    "studentFeedback",
    "learningPoints",
    "confidence",
    "reviewRequired",
    "reviewReasons"
  ],
  "properties": {
    "totalScore": {
      "type": "integer"
    },
    "maxScore": {
      "type": "integer"
    },
    "rubricScores": {
      "type": "array",
      "items": {
        "type": "object",
        "required": ["rubricItemName", "score", "maxScore", "reason"],
        "properties": {
          "rubricItemName": {
            "type": "string"
          },
          "score": {
            "type": "integer"
          },
          "maxScore": {
            "type": "integer"
          },
          "reason": {
            "type": "string"
          }
        }
      }
    },
    "deductions": {
      "type": "array",
      "items": {
        "type": "object",
        "required": ["rubricItemName", "pointsLost", "reason"],
        "properties": {
          "rubricItemName": {
            "type": "string"
          },
          "pointsLost": {
            "type": "integer"
          },
          "reason": {
            "type": "string"
          }
        }
      }
    },
    "studentFeedback": {
      "type": "string"
    },
    "learningPoints": {
      "type": "array",
      "items": {
        "type": "string"
      }
    },
    "confidence": {
      "type": "string",
      "enum": ["HIGH", "MEDIUM", "LOW"]
    },
    "reviewRequired": {
      "type": "boolean"
    },
    "reviewReasons": {
      "type": "array",
      "items": {
        "type": "string"
      }
    }
  }
}
```

## 백엔드 검증 전략

LLM 응답 schema를 사용하더라도 백엔드는 최종 저장 전에 자체 검증을 수행합니다. Gemini 응답은 저장 전 등록된 rubric 기준으로 점수와 감점 항목을 보정합니다.

1. `totalScore >= 0`
2. `totalScore <= question.totalScore`
3. `maxScore == question.totalScore`
4. `rubricScores` 합계가 `totalScore`와 일치
5. `rubricScores` 개수가 등록된 rubric item 개수와 일치
6. 각 rubric score가 0 이상 maxScore 이하
7. rubric score의 maxScore가 등록된 rubric item의 maxScore와 일치
8. deduction의 rubric 항목명이 등록된 rubric item에 포함됨
9. `confidence`가 `LOW`이면 `reviewRequired`는 `true`

검증 실패 시:

- `grading_requests.status = FAILED`
- `grading_requests.error_message` 저장
- 사용자용 에러 메시지와 개발자용 메시지 분리

## 프롬프트 버전 기록

채점 결과는 모델, 프롬프트, JSON Schema 변화에 영향을 받습니다. 따라서 결과를 해석하려면 채점 당시 사용한 프롬프트 버전과 모델명을 함께 저장해야 합니다.

현재는 별도 테이블 없이 Provider별 문자열인 `gemini-grading-v1`, `mock-v1`을 `promptVersionName`에 저장합니다.
