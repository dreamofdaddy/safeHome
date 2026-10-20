# 운영 배포 적합성 검토

## 결론

현재 저장소를 그대로 인터넷에 공개하는 것은 적절하지 않습니다. 프론트엔드는 정적 빌드와 Nginx 운영 이미지까지 준비했지만, 전체 시스템에는 로컬 개발용 기본값과 단일 서버 전제가 남아 있습니다.

## 프론트엔드

운영 배포 경로는 준비되어 있습니다.

- Vite 개발 서버는 로컬 개발에서만 사용합니다.
- `npm run build` 결과를 Nginx가 제공합니다.
- 브라우저의 `/api` 요청은 Nginx가 내부 백엔드로 전달합니다.
- SPA 경로 새로고침, 11MB 업로드 제한, 장기 캐시가 필요한 해시 자산, 기본 보안 헤더와 상태 확인 경로를 설정했습니다.
- Docker 빌드는 Node 24를 사용하고 런타임에는 Node와 개발 의존성을 포함하지 않습니다.
- `VITE_*` 값은 공개 번들에 포함되므로 비밀 값을 저장하지 않습니다.

프론트엔드에서 추가로 결정할 사항:

- 현재 액세스 토큰은 `sessionStorage`에 있습니다. XSS 발생 시 토큰이 노출될 수 있으므로, 공개 서비스에서는 백엔드와 함께 `Secure`, `HttpOnly`, `SameSite` 쿠키 방식 검토가 필요합니다.
- 실제 도메인에서 HTTPS를 적용하고 HSTS는 HTTPS 종단 프록시에서 설정해야 합니다.

## 공개 전에 반드시 수정할 항목

1. PostgreSQL과 Redis 포트를 인터넷에 공개하지 않고 내부 네트워크에만 둡니다.
2. `safehome_local`, `change_me` 같은 기본 비밀번호를 제거하고 Secret Manager 또는 배포 플랫폼의 비밀 변수로 주입합니다.
3. 백엔드의 데이터베이스·Redis 주소에서 `localhost` 기본값을 제거하고 운영 프로필을 분리합니다.
4. `management.endpoint.health.show-details=always`를 운영에서 제한합니다.
5. 실제 서비스 도메인만 CORS 허용 목록에 넣습니다. 프론트와 API를 같은 출처로 서비스하면 브라우저 CORS 의존성을 줄일 수 있습니다.
6. 등기부 PDF 로컬 파일 저장소를 영속 볼륨 또는 비공개 오브젝트 스토리지로 옮기고 백업·복구를 검증합니다.
7. HTTPS 인증서, 방화벽, 로그 수집, 오류 추적, 메트릭과 알림을 구성합니다.
8. 로그인·회원가입과 PDF 분석 API에 요청 제한을 적용해 무차별 대입과 비용 폭증을 방지합니다.
9. 데이터베이스 백업과 복구 훈련, Flyway 배포 전 백업 절차를 마련합니다.
10. 등기부 판독 작업을 여러 백엔드 인스턴스에서 운영하려면 현재 프로세스 내부 비동기 실행 대신 내구성 있는 작업 큐와 중복 실행 제어를 검토합니다.

## 권장 운영 경로

```text
Internet
  → HTTPS Load Balancer / Reverse Proxy
    → SafeHome Frontend (Nginx static files)
    → /api → SafeHome Backend
      → private PostgreSQL
      → private Redis
      → private document storage
      → OpenAI API
```

운영 환경은 개발용 `docker-compose.yml`을 그대로 재사용하지 않고 별도 Compose 또는 배포 매니페스트로 관리하는 편이 안전합니다.
