# SafeHome

AI 기반 부동산 거래 안전 플랫폼

## 개발 환경

- Windows
- Python 3.12.10
- Java 21 LTS
- Node.js 24 LTS (`frontend/.nvmrc` 기준)
- Docker
- PostgreSQL 17
- Redis 8

## 로컬 실행

저장소 루트에서 다음 PowerShell 스크립트를 실행합니다.

```powershell
# PostgreSQL, Redis, Backend, Frontend 시작
.\scripts\start-dev.ps1

# 전체 상태 확인
.\scripts\status-dev.ps1

# Backend, Frontend, PostgreSQL, Redis 중지
.\scripts\stop-dev.ps1
```

PostgreSQL과 Redis를 계속 실행하려면 다음 옵션을 사용합니다.

```powershell
.\scripts\stop-dev.ps1 -KeepInfrastructure
.\scripts\start-dev.ps1 -SkipInfrastructure
```

로그는 저장소의 `.runlogs/`에 기록됩니다. 프런트엔드는 `http://localhost:5173`, Backend는 `http://localhost:8080`에서 실행됩니다.

스크립트 실행 정책 오류가 발생하면 현재 터미널에서만 다음과 같이 실행할 수 있습니다.

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\start-dev.ps1
```

수동으로 실행하는 경우 PostgreSQL과 Redis는 다음 명령으로 시작합니다.

```powershell
docker compose up -d
```

Frontend 전체 품질 점검은 다음 명령으로 실행합니다.

```powershell
cd frontend
npm ci
npm run check
```

## 프로젝트 구조

```text
safeHome/
├── ai/
├── backend/
├── frontend/
├── infrastructure/
│   └── docker/
├── docs/
├── scripts/
├── .env.example
├── .gitignore
├── docker-compose.yml
└── README.md
```

## 구성

```text
Frontend → Backend → Risk Engine → AI / LLM
                   → PostgreSQL
                   → Redis
```

## 핵심 모듈

1. User / Identity
2. Property
3. Registry / Rights
4. Risk Analysis
5. Contract
6. Payment / Protection
7. Monitoring
8. AI / LLM

개발 환경과 운영 전환 항목은 [환경 정리 및 운영 전환 계획](docs/environment-roadmap.md)을 참고합니다.
