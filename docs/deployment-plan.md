# 배포 참고

## Local Docker Compose

현재 로컬 개발 단계에서는 PostgreSQL만 Docker Compose로 실행합니다. 백엔드와 프론트엔드는 개발 편의를 위해 로컬 프로세스로 실행합니다.

```bash
docker compose up -d postgres
```

## 로컬 실행 구성

- PostgreSQL: Docker Compose
- Backend: `LLM_PROVIDER=gemini GEMINI_API_KEY=... GEMINI_MODEL=gemini-2.5-flash ./gradlew bootRun`
- Frontend: `npm run dev`
- Swagger: `http://localhost:8080/swagger-ui.html`

## 운영 배포 참고 구성

운영 배포가 필요해질 경우 다음 구성을 기준으로 단순하게 배포할 수 있습니다.

- PostgreSQL 컨테이너
- Spring Boot 애플리케이션 컨테이너
- React 정적 빌드 서빙
- Caddy 또는 Nginx reverse proxy
- systemd 또는 Docker Compose 기반 프로세스 관리

## 도메인 연결 참고

도메인이 필요하면 DuckDNS 또는 Cloudflare를 사용할 수 있습니다.

- DuckDNS: 빠른 개인 프로젝트 배포에 적합
- Cloudflare: DNS 관리, TLS, 보안 설정 확장에 적합

## 배포 전 점검 항목

- 운영용 DB 비밀번호와 API Key는 환경변수 또는 Secret Manager로 관리
- `.env` 파일은 Git에 커밋하지 않음
- CORS 허용 origin을 운영 도메인으로 제한
- Swagger UI 공개 범위 검토
- Gemini API 사용량 제한과 timeout 설정
