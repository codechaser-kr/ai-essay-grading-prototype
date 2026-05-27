# 배포 계획

## Local Docker Compose

MVP 현재 단계에서는 PostgreSQL만 Docker Compose로 실행합니다. 백엔드와 프론트엔드는 개발 편의를 위해 로컬 프로세스로 실행합니다.

```bash
docker compose up -d postgres
```

## 로컬 실행 구성

- PostgreSQL: Docker Compose
- Backend: `./gradlew bootRun`
- Frontend: `npm run dev`
- Swagger: `http://localhost:8080/swagger-ui.html`

## Oracle Cloud Ubuntu VM 배포 계획

MVP 이후 Oracle Cloud Ubuntu VM에 다음 구성으로 배포합니다.

- PostgreSQL 컨테이너
- Spring Boot 애플리케이션 컨테이너
- React 정적 빌드 서빙
- Caddy 또는 Nginx reverse proxy
- systemd 또는 Docker Compose 기반 프로세스 관리

## 도메인 연결 계획

초기 배포에서는 DuckDNS 또는 Cloudflare를 사용해 도메인을 연결합니다.

- DuckDNS: 빠른 개인 프로젝트 배포에 적합
- Cloudflare: DNS 관리, TLS, 보안 설정 확장에 적합

## 향후 AWS 확장 계획

서비스 확장이나 운영 안정성이 필요해지면 AWS 기반 구성을 검토합니다.

- RDS for PostgreSQL
- ECS 또는 Elastic Beanstalk
- S3 + CloudFront
- Route 53
- CloudWatch Logs

## 배포 전 점검 항목

- 운영용 DB 비밀번호와 API Key는 환경변수 또는 Secret Manager로 관리
- `.env` 파일은 Git에 커밋하지 않음
- CORS 허용 origin을 운영 도메인으로 제한
- Swagger UI 공개 범위 검토
- OpenAI API 사용량 제한과 timeout 설정
