# DB 설계

## 개요

데이터베이스는 PostgreSQL을 사용합니다. MVP에서는 JPA `ddl-auto: update`로 테이블을 생성하고, 운영 배포 단계에서는 Flyway 또는 Liquibase 도입을 검토합니다.

현재 구현된 Entity 기준 테이블:

- `questions`
- `rubric_items`
- `grading_requests`
- `grading_results`

확장 설계 대상:

- `prompt_versions`
- `regrade_histories`

## questions

문제 본문, 모범 답안, 과목, 총점을 저장합니다.

| 필드 | 설명 |
| --- | --- |
| id | 문제 ID |
| title | 문제 제목 |
| subject | 과목 |
| content | 문제 본문 |
| model_answer | 모범 답안 |
| total_score | 총점 |
| created_at | 생성 시각 |
| updated_at | 수정 시각 |

관계:

- `questions` 1개는 여러 `rubric_items`를 가집니다.
- `questions` 1개는 여러 `grading_requests`를 가질 수 있습니다.

## rubric_items

문제별 평가 기준과 항목별 최대 점수를 저장합니다.

| 필드 | 설명 |
| --- | --- |
| id | rubric item ID |
| question_id | 문제 ID |
| name | 평가 항목명 |
| criteria | 평가 기준 설명 |
| max_score | 항목 최대 점수 |
| sort_order | 표시 순서 |

생성 시 `rubric_items.max_score` 합계는 `questions.total_score`와 일치해야 합니다.

## grading_requests

학생 답안에 대한 채점 요청과 처리 상태를 저장합니다.

| 필드 | 설명 |
| --- | --- |
| id | 채점 요청 ID |
| question_id | 문제 ID |
| student_answer | 학생 답안 |
| status | PENDING, PROCESSING, COMPLETED, FAILED |
| error_message | 실패 사유 |
| created_at | 생성 시각 |

상태 의미:

- `PENDING`: 요청 생성
- `PROCESSING`: 채점 처리 중
- `COMPLETED`: 채점 결과 저장 완료
- `FAILED`: 채점 또는 검증 실패

MVP에서는 동기 처리라 정상 요청은 대부분 즉시 `COMPLETED`가 됩니다.

## grading_results

LLM Provider가 반환한 구조화된 결과와 저장 메타데이터를 저장합니다.

| 필드 | 설명 |
| --- | --- |
| id | 채점 결과 ID |
| grading_request_id | 채점 요청 ID |
| model_name | 사용 모델명 |
| prompt_version_name | 프롬프트 버전명 |
| total_score | 총점 |
| confidence | HIGH, MEDIUM, LOW |
| review_required | 재검토 필요 여부 |
| result_json | 구조화된 결과 JSON |
| raw_response | 원본 응답 |
| created_at | 생성 시각 |

`result_json`에는 다음 항목이 포함됩니다.

- `totalScore`
- `maxScore`
- `rubricScores`
- `deductions`
- `studentFeedback`
- `learningPoints`
- `confidence`
- `reviewRequired`
- `reviewReasons`

현재는 `text` 컬럼에 JSON 문자열로 저장합니다. PostgreSQL 운영 최적화가 필요해지면 `jsonb` 전환을 검토할 수 있습니다.

## prompt_versions

MVP 이후 확장 대상입니다. 실제 OpenAI Provider를 구현할 때 system prompt, user prompt template, JSON Schema를 버전별로 관리합니다.

| 필드 | 설명 |
| --- | --- |
| id | 프롬프트 버전 ID |
| version_name | 버전명 |
| system_prompt | 시스템 프롬프트 |
| user_prompt_template | 사용자 프롬프트 템플릿 |
| response_schema | 응답 JSON Schema |
| is_active | 활성 버전 여부 |
| created_at | 생성 시각 |

현재 MVP에서는 `grading_results.prompt_version_name`에 `mock-v1` 값을 저장합니다.

## regrade_histories

MVP 이후 확장 대상입니다. 재채점이 발생했을 때 기존 결과와 새 결과의 관계를 저장합니다.

| 필드 | 설명 |
| --- | --- |
| id | 재채점 이력 ID |
| original_grading_result_id | 기존 채점 결과 ID |
| new_grading_result_id | 새 채점 결과 ID |
| reason | 재채점 사유 |
| created_at | 생성 시각 |
