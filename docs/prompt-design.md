# 프롬프트 설계

## 목표

프롬프트는 서술형 답안을 rubric 기준으로 일관되게 채점하고, 백엔드가 검증 가능한 JSON 구조로 결과를 반환하도록 설계합니다.

MVP에서는 실제 LLM 호출 없이 `MockGradingAiClient`가 같은 구조의 응답을 반환합니다. 이후 OpenAI Provider를 구현할 때 이 문서를 기준으로 system prompt, user prompt template, Structured Outputs JSON Schema를 구체화합니다.

## 현재 MVP 구조

현재 채점 결과는 다음 흐름으로 생성됩니다.

```text
GradingService
  -> GradingAiClient.grade()
  -> MockGradingAiClient
  -> GradingResultValidator
  -> grading_results.result_json 저장
```

저장되는 메타데이터:

- `modelName`: `mock-grading-model`
- `promptVersionName`: `mock-v1`
- `resultJson`: 구조화된 채점 결과 JSON
- `rawResponse`: Mock 원본 응답 문자열

## System Prompt 초안

```text
당신은 교사를 보조하는 서술형 답안 채점 시스템입니다.
주어진 문제, 모범 답안, 평가 기준을 바탕으로 학생 답안을 공정하게 채점합니다.
반드시 지정된 JSON Schema에 맞는 응답만 반환해야 합니다.
점수는 각 rubric의 최대 점수를 초과할 수 없고, 총점은 rubric 점수 합계와 일치해야 합니다.
오개념 가능성이나 판단이 불확실한 경우 reviewRequired를 true로 설정합니다.
학생에게 제공되는 피드백은 구체적이고 학습 가능한 문장으로 작성합니다.
```

## User Prompt Template 초안

```text
문제 제목:
{{question.title}}

문제:
{{question.content}}

모범 답안:
{{question.modelAnswer}}

총점:
{{question.totalScore}}

평가 기준:
{{rubricItems}}

학생 답안:
{{studentAnswer}}

위 정보를 바탕으로 학생 답안을 채점하고 지정된 JSON 구조로만 응답하세요.
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
    }
  ],
  "deductions": [
    {
      "rubricItemName": "개념 이해",
      "pointsLost": 8,
      "reason": "탄소 중립을 이산화탄소를 전혀 배출하지 않는 것으로 설명했습니다."
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

## Structured Outputs JSON Schema 초안

향후 OpenAI Provider에서는 Structured Outputs를 사용해 다음 JSON Schema에 맞는 응답만 받도록 합니다.

```json
{
  "type": "object",
  "additionalProperties": false,
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
      "type": "integer",
      "minimum": 0
    },
    "maxScore": {
      "type": "integer",
      "minimum": 1
    },
    "rubricScores": {
      "type": "array",
      "items": {
        "type": "object",
        "additionalProperties": false,
        "required": ["rubricItemName", "score", "maxScore", "reason"],
        "properties": {
          "rubricItemName": {
            "type": "string"
          },
          "score": {
            "type": "integer",
            "minimum": 0
          },
          "maxScore": {
            "type": "integer",
            "minimum": 1
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
        "additionalProperties": false,
        "required": ["rubricItemName", "pointsLost", "reason"],
        "properties": {
          "rubricItemName": {
            "type": "string"
          },
          "pointsLost": {
            "type": "integer",
            "minimum": 0
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

Structured Outputs를 사용하더라도 백엔드는 최종 저장 전에 자체 검증을 수행합니다.

1. `totalScore >= 0`
2. `totalScore <= question.totalScore`
3. `rubricScores` 합계가 `totalScore`와 일치
4. `rubricScores` 개수가 등록된 rubric item 개수와 일치
5. 각 rubric score가 0 이상 maxScore 이하
6. rubric score의 maxScore가 등록된 rubric item의 maxScore와 일치
7. `confidence`가 `LOW`이면 `reviewRequired`는 `true`

검증 실패 시:

- `grading_requests.status = FAILED`
- `grading_requests.error_message` 저장
- 사용자용 에러 메시지와 개발자용 메시지 분리

## 프롬프트 버전 관리 이유

채점 결과는 모델, 프롬프트, JSON Schema 변화에 영향을 받습니다. 따라서 결과를 해석하거나 재현하려면 채점 당시 사용한 프롬프트 버전과 모델명을 함께 저장해야 합니다.

MVP에서는 `promptVersionName = mock-v1` 문자열을 저장합니다. 이후 확장 단계에서는 `prompt_versions` 테이블을 추가해 활성 프롬프트 버전을 조회하고, 채점 결과가 해당 버전을 참조하도록 개선합니다.
