# 개발 기록

## 작업 원칙

- 실제 LLM API 연동은 Gemini Provider로 검증합니다.
- Mock LLM Provider는 외부 API 없이 전체 서비스 흐름을 검증할 때 사용합니다.
- API Key와 민감 정보는 Git에 커밋하지 않습니다.
- `.env`는 제외하고 `.env.example`만 커밋합니다.
- 백엔드 API contract를 먼저 안정화한 뒤 프론트엔드를 연결합니다.
- 주요 설계는 `docs/` 아래에 문서화합니다.

## 단계별 구현 기록

### 1단계: 초기 구조와 문서 완료

- `backend/`, `frontend/`, `docs/`, `infra/` 생성
- README 초안 작성
- 아키텍처, API, DB, 프롬프트, 개발, 배포 문서 초안 작성
- `.gitignore`, `.env.example`, `docker-compose.yml` 작성

### 2단계: 백엔드 기본 프로젝트 완료

- Kotlin Spring Boot 프로젝트 생성
- Gradle Kotlin DSL 구성
- Spring Web, JPA, Validation, Actuator, Springdoc, PostgreSQL 의존성 추가
- `application.yml` 작성
- 공통 패키지 구조 생성

### 3단계: Question/Rubric API 완료

- Question, RubricItem Entity 구현
- Repository, Service, Controller, DTO 구현
- 문제 생성, 목록 조회, 상세 조회 API 구현

### 4단계: Mock 채점 API 완료

- GradingRequest, GradingResult Entity 구현
- `GradingAiClient` 인터페이스 구현
- `MockGradingAiClient` 구현
- 채점 요청과 결과 조회 API 구현
- 채점 결과 검증 로직 구현

### 5단계: 프론트엔드 MVP 완료

- React + TypeScript + Vite 프로젝트 생성
- API client 구성
- 문제 목록, 등록, 상세, 답안 채점, 결과 화면 구현

### 6단계: Gemini Provider 연동 완료

- `GeminiGradingAiClient` 구현
- Gemini `generateContent` API 호출
- JSON 응답 schema 적용
- Gemini 응답 파싱과 rubric 기준 보정
- API Key, 모델명 환경변수 추가
- Gemini Provider 단위 테스트 추가

### 7단계: 문서 정리 진행 중

- README 실행 방법 보강
- API 예시 업데이트
- DB 설계 문서 업데이트
- 프롬프트 설계와 LLM Provider 전략 보강

### 8단계: 마무리 개선

- 오개념이 포함된 답안의 감점 기준 보강
- `reviewRequired`와 rubric 점수의 정합성 개선
- Gemini 응답 품질을 높이기 위한 프롬프트 문구 조정

## 테스트 전략

현재 적용된 검증은 다음과 같습니다.

- Backend: Controller 통합 테스트
- Validation: 요청 DTO 검증 실패 테스트
- Grading: Mock 응답 검증 로직 테스트
- LLM: Gemini 응답 파싱과 요청 바디 테스트
- Frontend: TypeScript 빌드와 Vite production build

실행 명령:

```bash
cd backend
./gradlew ktlintCheck test
```

```bash
cd frontend
npm run build
```

## 커밋 전략

기능 의미 단위로 작게 커밋합니다.

- 초기 구조와 문서
- 백엔드 기본 프로젝트
- Question/Rubric API
- Mock 채점 API
- Gemini 채점 Provider
- 프론트엔드 기본 화면
- 채점 결과 화면
- 문서 보강
