# 프롬프트 설계 초안

## 목표

프롬프트는 서술형 답안을 rubric 기준으로 일관되게 채점하고, 백엔드가 검증 가능한 JSON 구조로 결과를 반환하도록 설계합니다.

MVP에서는 실제 LLM 호출 없이 Mock Provider가 같은 구조의 응답을 반환합니다. 이후 OpenAI Provider를 구현할 때 이 문서를 기준으로 system prompt, user prompt template, Structured Outputs JSON Schema를 구체화합니다.

## System Prompt 초안

```text
당신은 교사를 보조하는 서술형 답안 채점 시스템입니다.
주어진 문제, 모범 답안, 평가 기준을 바탕으로 학생 답안을 공정하게 채점합니다.
반드시 지정된 JSON Schema에 맞는 응답만 반환해야 합니다.
점수는 각 rubric의 최대 점수를 초과할 수 없고, 총점은 rubric 점수 합계와 일치해야 합니다.
오개념 가능성이나 판단이 불확실한 경우 reviewRequired를 true로 설정합니다.
```

## User Prompt Template 초안

```text
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

## 응답 JSON 구조 초안

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
  "confidence": "medium",
  "reviewRequired": true,
  "reviewReasons": [
    "핵심 개념 설명에 오개념 가능성이 있습니다."
  ]
}
```

## 검증 전략

백엔드는 Mock 응답과 실제 LLM 응답 모두에 대해 다음 검증을 수행합니다.

1. `totalScore >= 0`
2. `totalScore <= question.totalScore`
3. `rubricScores` 합계가 `totalScore`와 일치
4. `rubricScores` 개수가 등록된 rubric item 개수와 일치
5. 각 rubric score가 0 이상 maxScore 이하
6. `confidence`가 `high`, `medium`, `low` 중 하나
7. `confidence`가 `low`면 `reviewRequired`는 `true`

## 프롬프트 버전 관리 이유

채점 결과는 모델, 프롬프트, JSON Schema 변화에 영향을 받습니다. 따라서 결과를 해석하거나 재현하려면 채점 당시 사용한 프롬프트 버전과 모델명을 함께 저장해야 합니다.

MVP에서는 기본 프롬프트 버전을 seed 또는 SQL로 제공하고, 이후 관리자 화면에서 프롬프트 버전을 관리할 수 있도록 확장합니다.
