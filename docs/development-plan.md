# 개발 계획 초안

## 작업 원칙

- 실제 OpenAI API 연동은 MVP 이후로 미룹니다.
- 먼저 Mock LLM Provider로 전체 서비스 흐름을 완성합니다.
- API Key와 민감 정보는 Git에 커밋하지 않습니다.
- `.env`는 제외하고 `.env.example`만 커밋합니다.
- 백엔드 API contract를 먼저 안정화한 뒤 프론트엔드를 연결합니다.
- 주요 설계는 `docs/` 아래에 문서화합니다.

## 단계별 계획

### 1단계: 초기 구조와 문서

- `backend/`, `frontend/`, `docs/`, `infra/` 생성
- README 초안 작성
- 아키텍처, API, DB, 프롬프트, 개발, 배포 문서 초안 작성
- `.gitignore`, `.env.example`, `docker-compose.yml` 작성

### 2단계: 백엔드 기본 프로젝트

- Kotlin Spring Boot 프로젝트 생성
- Gradle Kotlin DSL 구성
- Spring Web, JPA, Validation, Actuator, Springdoc, PostgreSQL 의존성 추가
- `application.yml` 작성
- 공통 패키지 구조 생성

### 3단계: Question/Rubric API

- Question, RubricItem Entity 구현
- Repository, Service, Controller, DTO 구현
- 문제 생성, 목록 조회, 상세 조회 API 구현

### 4단계: Mock 채점 API

- GradingRequest, GradingResult Entity 구현
- `GradingAiClient` 인터페이스 구현
- `MockGradingAiClient` 구현
- 채점 요청과 결과 조회 API 구현
- 채점 결과 검증 로직 구현

### 5단계: 프론트엔드 MVP

- React + TypeScript + Vite 프로젝트 생성
- API client 구성
- 문제 목록, 등록, 상세, 답안 채점, 결과 화면 구현

### 6단계: 문서 정리

- README 실행 방법 보강
- API 예시 업데이트
- DB 설계 문서 업데이트
- 프롬프트 설계와 OpenAI Provider 확장 계획 보강

## 테스트 전략

초기 구조 단계에서는 실행 코드가 없으므로 별도 테스트 코드는 작성하지 않습니다. 이후 단계부터 다음 검증을 추가합니다.

- Backend: Service 단위 테스트, Controller 통합 테스트
- Validation: 요청 DTO 검증 실패 테스트
- Grading: Mock 응답 검증 로직 테스트
- Frontend: 주요 화면 렌더링과 API 호출 흐름 테스트

## 커밋 전략

기능 의미 단위로 작게 커밋합니다.

- 초기 구조와 문서
- 백엔드 기본 프로젝트
- Question/Rubric API
- Mock 채점 API
- 프론트엔드 기본 화면
- 채점 결과 화면
- 문서 보강
