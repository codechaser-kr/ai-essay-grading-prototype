# AI Essay Grading Prototype

AI Essay Grading Prototype은 LLM API를 활용하여 서술형 답안을 채점하고, 결과를 구조화된 JSON으로 저장·검증·시각화하는 AI 에듀테크 포트폴리오 프로젝트입니다.

이 프로젝트는 단순한 OpenAI API 호출 데모가 아니라, 문제 등록, 평가 기준 rubric 관리, 학생 답안 제출, AI 채점, 항목별 점수, 감점 사유, 피드백, 재검토 플래그, 채점 이력, 프롬프트 버전 관리를 포함한 실제 서비스형 구조를 목표로 합니다.

## 프로젝트 목적

- 문제와 평가 기준을 등록하고 학생 답안을 채점하는 전체 흐름을 구현합니다.
- Mock LLM Provider로 외부 API 없이 MVP를 검증합니다.
- OpenAI Provider를 추가할 수 있도록 LLM 호출 인터페이스를 분리합니다.
- 채점 결과 JSON, 모델명, 프롬프트 버전명을 함께 저장해 추적 가능성을 확보합니다.
- 백엔드, 프론트엔드, 데이터베이스, 로컬 인프라가 연결된 풀스택 포트폴리오를 구성합니다.

## 주요 기능

- 문제와 rubric 생성
- 문제 목록 및 상세 조회
- 학생 답안 채점 요청
- Mock LLM 기반 구조화된 채점 결과 생성
- 채점 결과 검증 후 DB 저장
- 총점, 항목별 점수, 감점 사유, 피드백, 보완 학습 포인트 표시
- 재검토 필요 여부와 재검토 사유 표시
- 문제별 채점 이력 조회

## 기술 스택

| 영역 | 기술 |
| --- | --- |
| Frontend | React, TypeScript, Vite, Axios, React Router |
| Backend | Kotlin, Spring Boot, Spring Web, Spring Data JPA, Validation |
| API Docs | Springdoc OpenAPI, Swagger UI |
| Database | PostgreSQL 16 |
| Local Infra | Docker Compose |
| LLM | Mock Provider, OpenAI Provider 확장 구조 |

## 아키텍처

```text
React + TypeScript + Vite
        |
        | REST API
        v
Kotlin + Spring Boot
        |
        | Spring Data JPA
        v
PostgreSQL 16

GradingService
        |
        v
GradingAiClient
        |
        +-- MockGradingAiClient
        +-- OpenAiGradingAiClient
```

백엔드는 `GradingAiClient` 인터페이스 뒤에 채점 Provider를 둡니다. 현재 MVP는 `LLM_PROVIDER=mock`으로 Mock Provider를 사용하며, 이후 `LLM_PROVIDER=openai`로 실제 OpenAI Provider를 연결할 수 있게 설계했습니다.

## 로컬 실행 방법

### PostgreSQL 실행

```bash
docker compose up -d postgres
```

### Backend 실행

```bash
cd backend
./gradlew bootRun
```

기본 DB 연결 정보:

- URL: `jdbc:postgresql://localhost:5432/essay_grading`
- Username: `essay_grading_user`
- Password: `essay_grading_password`

### Frontend 실행

```bash
cd frontend
npm install
npm run dev
```

### 접속 URL

- Frontend: http://localhost:5173
- Backend: http://localhost:8080
- Swagger: http://localhost:8080/swagger-ui.html
- OpenAPI JSON: http://localhost:8080/v3/api-docs
- PostgreSQL: localhost:5432

## API 문서

백엔드 실행 후 Swagger UI에서 API를 확인할 수 있습니다.

- http://localhost:8080/swagger-ui.html
- http://localhost:8080/swagger-ui/index.html

주요 API:

| 기능 | Method | Path |
| --- | --- | --- |
| 문제 생성 | POST | `/api/questions` |
| 문제 목록 조회 | GET | `/api/questions` |
| 문제 상세 조회 | GET | `/api/questions/{questionId}` |
| 채점 요청 | POST | `/api/grading-requests` |
| 채점 결과 상세 조회 | GET | `/api/grading-results/{gradingResultId}` |
| 문제별 채점 이력 조회 | GET | `/api/grading-results?questionId={questionId}` |

## 샘플 시나리오

1. Swagger 또는 프론트엔드에서 문제와 rubric을 등록합니다.
2. 문제 상세 화면에서 채점하기를 선택합니다.
3. 학생 답안을 입력하고 채점 요청을 보냅니다.
4. 채점 결과 화면에서 총점, 항목별 점수, 감점 사유, 피드백, 재검토 여부를 확인합니다.
5. 문제 상세 화면에서 채점 이력을 다시 확인합니다.

시연용 JSON은 [docs/sample-data.md](docs/sample-data.md)에 정리되어 있습니다.

## LLM Provider 전략

MVP는 실제 OpenAI API를 호출하지 않습니다. `MockGradingAiClient`가 rubric 기준의 구조화된 결과를 반환하고, 백엔드는 `GradingResultValidator`로 점수 합계와 검증 규칙을 확인한 뒤 저장합니다.

향후 `OpenAiGradingAiClient`는 다음 역할을 맡습니다.

- 활성 프롬프트 버전 조회
- system prompt와 user prompt 생성
- OpenAI Responses API 호출
- Structured Outputs JSON Schema 적용
- 응답 JSON 파싱
- 백엔드 검증 후 저장

## DB 설계 요약

- `questions`: 문제 본문, 모범 답안, 총점
- `rubric_items`: 문제별 평가 항목과 배점
- `grading_requests`: 학생 답안과 채점 처리 상태
- `grading_results`: 채점 결과 JSON, 모델명, 프롬프트 버전명, 총점, 재검토 여부

프롬프트 버전과 재채점 이력 테이블은 MVP 이후 확장 대상으로 문서에 설계해 두었습니다.

## 문서

- [아키텍처](docs/architecture.md)
- [API 설계](docs/api-design.md)
- [DB 설계](docs/db-design.md)
- [프롬프트 설계](docs/prompt-design.md)
- [개발 계획](docs/development-plan.md)
- [배포 계획](docs/deployment-plan.md)
- [샘플 데이터](docs/sample-data.md)

## 향후 확장 계획

- OpenAI Provider 실제 구현
- Structured Outputs JSON Schema 적용
- 프롬프트 버전 관리 API와 화면 추가
- 재채점 기능 추가
- 채점 이력 비교 화면 추가
- 관리자 검토 상태 추가
- Docker Compose 전체 배포 구성
- Oracle Cloud Ubuntu VM 배포
- Caddy 또는 Nginx reverse proxy 추가
- Cloudflare 또는 DuckDNS 도메인 연결

## 강조 역량

- Kotlin/Spring Boot 기반 REST API 설계와 검증
- JPA Entity와 API DTO 분리
- LLM Provider 추상화와 Mock 기반 MVP 설계
- 구조화 JSON 저장과 검증 로직 구현
- React/TypeScript 기반 API 연동 화면 구현
- Docker Compose 기반 로컬 개발 환경 구성
