# DB 설계 초안

## 개요

데이터베이스는 PostgreSQL을 사용합니다. MVP에서는 JPA `ddl-auto: update`로 테이블을 생성하고, 이후 운영 배포 단계에서는 Flyway 또는 Liquibase 도입을 검토합니다.

## questions

문제 본문과 모범 답안을 저장합니다.

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

## rubric_items

문제별 평가 기준을 저장합니다. 하나의 문제는 여러 rubric item을 가질 수 있습니다.

| 필드 | 설명 |
| --- | --- |
| id | rubric item ID |
| question_id | 문제 ID |
| name | 평가 항목명 |
| criteria | 평가 기준 설명 |
| max_score | 항목 최대 점수 |
| sort_order | 표시 순서 |

## prompt_versions

채점에 사용한 system prompt, user prompt template, JSON schema 버전을 저장합니다.

| 필드 | 설명 |
| --- | --- |
| id | 프롬프트 버전 ID |
| version_name | 버전명 |
| system_prompt | 시스템 프롬프트 |
| user_prompt_template | 사용자 프롬프트 템플릿 |
| response_schema | 응답 JSON Schema |
| is_active | 활성 버전 여부 |
| created_at | 생성 시각 |

## grading_requests

학생 답안에 대한 채점 요청 상태를 저장합니다.

| 필드 | 설명 |
| --- | --- |
| id | 채점 요청 ID |
| question_id | 문제 ID |
| student_answer | 학생 답안 |
| status | PENDING, PROCESSING, COMPLETED, FAILED |
| error_message | 실패 사유 |
| created_at | 생성 시각 |

## grading_results

LLM Provider가 반환한 구조화된 채점 결과와 원본 응답을 저장합니다.

| 필드 | 설명 |
| --- | --- |
| id | 채점 결과 ID |
| grading_request_id | 채점 요청 ID |
| prompt_version_id | 프롬프트 버전 ID |
| model_name | 사용 모델명 |
| total_score | 총점 |
| confidence | HIGH, MEDIUM, LOW |
| review_required | 재검토 필요 여부 |
| result_json | 구조화된 결과 JSON |
| raw_response | 원본 응답 |
| created_at | 생성 시각 |

## regrade_histories

재채점이 발생했을 때 기존 결과와 새 결과의 관계를 저장합니다.

| 필드 | 설명 |
| --- | --- |
| id | 재채점 이력 ID |
| original_grading_result_id | 기존 채점 결과 ID |
| new_grading_result_id | 새 채점 결과 ID |
| reason | 재채점 사유 |
| created_at | 생성 시각 |
