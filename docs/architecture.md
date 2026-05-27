# 아키텍처

## 개요

AI Essay Grading Prototype은 문제 등록, rubric 관리, 학생 답안 제출, Mock AI 채점, 결과 저장, 프론트엔드 시각화를 하나의 풀스택 흐름으로 구성합니다.

MVP는 실제 OpenAI API를 호출하지 않고 `MockGradingAiClient`를 사용합니다. 대신 `GradingAiClient` 인터페이스를 기준으로 Provider를 분리해 이후 OpenAI Provider를 추가할 수 있게 합니다.

## 전체 구조

```text
Frontend
React + TypeScript + Vite
        |
        | REST API
        v
Backend
Kotlin + Spring Boot
        |
        | Spring Data JPA
        v
Database
PostgreSQL 16
```

## Frontend

프론트엔드는 다음 화면을 제공합니다.

- 문제 목록: `/`
- 문제 등록: `/questions/new`
- 문제 상세: `/questions/:questionId`
- 답안 채점: `/questions/:questionId/grade`
- 채점 결과 상세: `/grading-results/:gradingResultId`

API 호출은 `frontend/src/api` 아래에서 관리합니다.

- `questionApi.ts`
- `gradingApi.ts`
- `http.ts`

API base URL은 `VITE_API_BASE_URL` 환경변수를 사용합니다.

## Backend

백엔드는 REST API, 요청 검증, 채점 요청 생성, Mock LLM 호출, 결과 검증, PostgreSQL 저장을 담당합니다.

현재 주요 패키지 구조:

```text
com.codechaser.essaygrading
  api/question
  api/grading
  common/config
  common/error
  entity
  enums
  llm
  repository
```

역할:

- `api/question`: 문제 생성, 목록, 상세 API
- `api/grading`: 채점 요청, 결과 조회, 결과 검증
- `entity`: JPA Entity
- `repository`: Spring Data JPA Repository
- `llm`: LLM Provider 인터페이스와 Mock/OpenAI 구현
- `enums`: 채점 상태와 confidence enum
- `common`: CORS 설정과 공통 예외 응답

## LLM Provider 구조

```text
GradingService
      |
      v
GradingAiClient
      |
      +-- MockGradingAiClient
      |
      +-- OpenAiGradingAiClient
```

현재 설정:

```yaml
llm:
  provider: ${LLM_PROVIDER:mock}
```

`LLM_PROVIDER`가 없으면 `mock`이 기본값입니다. `OpenAiGradingAiClient`는 구조만 준비되어 있으며 MVP에서는 실제 API를 호출하지 않습니다.

## 채점 처리 흐름

```text
POST /api/grading-requests
        |
        v
Question 조회
        |
        v
GradingRequest 저장
        |
        v
MockGradingAiClient.grade()
        |
        v
GradingResultValidator 검증
        |
        v
GradingResult 저장
        |
        v
채점 결과 응답
```

검증 실패 시 `GradingRequest`는 `FAILED` 상태와 `errorMessage`를 저장합니다.

## Database

MVP에서 실제 사용하는 테이블:

- `questions`
- `rubric_items`
- `grading_requests`
- `grading_results`

문서상 확장 설계:

- `prompt_versions`
- `regrade_histories`

## 로컬 실행 구조

```text
PostgreSQL: docker compose up -d postgres
Backend:    cd backend && ./gradlew bootRun
Frontend:   cd frontend && npm run dev
```

프론트엔드 개발 서버는 `http://localhost:5173`, 백엔드는 `http://localhost:8080`을 사용합니다. 백엔드는 개발 환경에서 `http://localhost:5173` origin을 CORS로 허용합니다.
