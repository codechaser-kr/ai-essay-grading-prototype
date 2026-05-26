# 아키텍처 초안

## 개요

AI Essay Grading Prototype은 문제 등록, rubric 관리, 학생 답안 제출, AI 채점, 결과 저장 및 시각화를 하나의 풀스택 흐름으로 구성하는 프로젝트입니다.

초기 MVP는 실제 OpenAI API를 호출하지 않고 Mock LLM Provider를 사용합니다. 다만 백엔드 내부에서는 `GradingAiClient` 인터페이스를 기준으로 채점 Provider를 분리해 이후 OpenAI Provider를 추가할 수 있게 합니다.

## 구성 요소

```text
React + TypeScript + Vite
        |
        | HTTP REST API
        v
Kotlin + Spring Boot
        |
        | Spring Data JPA
        v
PostgreSQL 16
```

## Frontend

프론트엔드는 사용자가 문제를 등록하고, 학생 답안을 제출하며, 채점 결과를 확인하는 화면을 제공합니다.

주요 화면은 다음과 같습니다.

- 문제 목록
- 문제 등록
- 문제 상세
- 답안 채점
- 채점 결과 상세

## Backend

백엔드는 REST API, 도메인 로직, 채점 요청 생성, Mock LLM 호출, 결과 검증, PostgreSQL 저장을 담당합니다.

도메인 패키지는 다음 구조를 기준으로 합니다.

- `question`: 문제와 rubric 관리
- `grading`: 채점 요청, 결과, 이력 관리
- `prompt`: 프롬프트 버전 관리
- `llm`: Mock/OpenAI Provider 인터페이스
- `common`: 공통 설정과 예외 처리

## Database

PostgreSQL은 다음 데이터를 저장합니다.

- 문제
- rubric 항목
- 프롬프트 버전
- 채점 요청
- 채점 결과
- 재채점 이력

## LLM Provider 교체 구조

MVP에서는 `MockGradingAiClient`가 구조화된 채점 결과를 반환합니다. 이후 `OpenAiGradingAiClient`를 추가하면 같은 `GradingAiClient` 인터페이스를 통해 실제 OpenAI API 호출로 교체할 수 있습니다.

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

## 로컬 실행 구조

로컬 개발 환경은 Docker Compose로 PostgreSQL을 실행하고, 백엔드와 프론트엔드는 각각 로컬 프로세스로 실행합니다.

- PostgreSQL: `docker compose up -d postgres`
- Backend: `./gradlew bootRun`
- Frontend: `npm run dev`
