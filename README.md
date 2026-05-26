# AI Essay Grading Prototype

AI Essay Grading Prototype은 LLM API를 활용하여 서술형 답안을 채점하고, 결과를 구조화된 JSON으로 저장·검증·시각화하는 AI 에듀테크 포트폴리오 프로젝트입니다.

이 프로젝트는 단순한 OpenAI API 호출 데모가 아니라, 문제 등록, 평가 기준 rubric 관리, 학생 답안 제출, AI 채점, 항목별 점수, 감점 사유, 피드백, 재검토 플래그, 채점 이력, 프롬프트 버전 관리를 포함한 실제 서비스형 구조를 목표로 합니다.

## 프로젝트 목적

- 교사 또는 관리자가 문제와 평가 기준을 등록하는 흐름을 구현합니다.
- 학생의 서술형 답안을 Mock LLM Provider로 채점하고 구조화된 결과를 저장합니다.
- OpenAI Provider를 나중에 추가할 수 있도록 LLM 호출 인터페이스를 분리합니다.
- 프롬프트 버전, 모델명, 채점 결과 JSON을 함께 저장해 결과 추적 가능성을 확보합니다.
- 백엔드 API, 데이터베이스, 프론트엔드 화면, 로컬 인프라까지 연결된 풀스택 MVP를 완성합니다.

## 주요 기능

- 문제와 rubric 등록
- 문제 목록 및 상세 조회
- 학생 답안 채점 요청
- Mock LLM Provider 기반 구조화된 채점 결과 생성
- 총점, 항목별 점수, 감점 사유, 피드백, 보완 학습 포인트 표시
- 재검토 필요 여부와 사유 관리
- 채점 이력 조회
- 향후 OpenAI Provider 교체를 고려한 인터페이스 설계

## 기술 스택

### Frontend

- React
- TypeScript
- Vite
- Axios
- React Router

### Backend

- Kotlin
- Spring Boot
- Spring Web
- Spring Data JPA
- Bean Validation
- PostgreSQL Driver
- Spring Boot Actuator
- Springdoc OpenAPI
- Jackson Kotlin Module

### Database / Infra

- PostgreSQL 16
- Docker Compose

### LLM

- MVP: Mock Provider
- 확장: OpenAI Provider

## 아키텍처 요약

React 프론트엔드는 Spring Boot 백엔드의 REST API를 호출합니다. 백엔드는 문제, rubric, 채점 요청, 채점 결과를 PostgreSQL에 저장합니다. 채점 로직은 `GradingAiClient` 인터페이스 뒤에 두어 MVP에서는 Mock Provider를 사용하고, 이후 OpenAI Provider로 교체할 수 있게 설계합니다.

```text
Frontend(React/Vite)
        |
        | REST API
        v
Backend(Kotlin/Spring Boot)
        |
        | JPA
        v
PostgreSQL
        ^
        |
Mock LLM Provider -> OpenAI Provider 확장 예정
```

## MVP 범위

- Docker Compose 기반 PostgreSQL 실행
- Kotlin Spring Boot 백엔드 기본 프로젝트 구성
- Question/Rubric API
- Mock LLM 기반 Grading API
- 채점 결과 검증 및 저장
- React 기반 문제 등록, 답안 제출, 결과 확인 화면
- Swagger API 문서
- 아키텍처, API, DB, 프롬프트 설계 문서

## 로컬 실행 계획

현재 단계에서는 초기 문서와 PostgreSQL Docker Compose만 준비되어 있습니다. 이후 단계에서 백엔드와 프론트엔드 프로젝트가 생성되면 다음 명령으로 실행합니다.

### PostgreSQL 실행

```bash
docker compose up -d postgres
```

### Backend 실행

```bash
cd backend
./gradlew bootRun
```

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
- PostgreSQL: localhost:5432

## 향후 확장 계획

- OpenAI Provider 실제 구현
- Structured Outputs JSON Schema 적용
- 프롬프트 버전 관리 화면 추가
- 재채점 기능과 채점 이력 비교 화면 추가
- 관리자 검토 상태 추가
- Docker Compose 전체 배포 구성
- Oracle Cloud Ubuntu VM 배포
- Caddy 또는 Nginx reverse proxy 추가
- Cloudflare 또는 DuckDNS 도메인 연결
