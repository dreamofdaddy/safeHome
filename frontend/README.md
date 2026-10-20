# SafeHome Frontend

SafeHome의 회원가입, 로그인, 내 매물, 등기부 PDF 등록·판독, 안전성 분석과 재분석 흐름을 제공하는 React 웹 앱입니다.

## 기술 구성

- React + TypeScript + Vite
- React Router
- Axios + TanStack Query
- Tailwind CSS + Lucide React + Recharts
- Vitest + React Testing Library

Node.js `24.15.0` 이상과 npm을 사용합니다. `.nvmrc`는 검증한 Node `24.19.0`을 지정합니다.

## 로컬 실행

백엔드를 `http://localhost:8080`에서 먼저 실행합니다. Vite가 `/api` 요청을 백엔드로 프록시하므로 브라우저에서는 동일 출처 요청으로 처리됩니다.

```powershell
cd D:\projects\safeHome\frontend
npm install
npm run dev
```

브라우저에서 `http://localhost:5173`을 엽니다.

```powershell
npm run build
npm test
npm run lint
```

## 환경변수

`VITE_API_BASE_URL`의 기본값은 `/api`입니다. Vite 변수는 빌드 결과에 포함되므로 비밀 값을 넣으면 안 됩니다.

## 운영 이미지

운영에서는 `npm run dev`나 `vite preview`를 서비스용으로 사용하지 않습니다. Docker 이미지는 Node 22로 정적 파일을 빌드한 뒤 Nginx가 파일을 제공하고 `/api`를 백엔드로 전달합니다.

```powershell
docker build -t safehome-frontend .
docker run --rm -p 80:80 -e BACKEND_UPSTREAM=http://host.docker.internal:8080 safehome-frontend
```

실제 운영에서는 `BACKEND_UPSTREAM`을 내부 백엔드 주소로 지정하고, 외부 로드 밸런서 또는 리버스 프록시에서 HTTPS를 종료해야 합니다.

## 인증과 분석 상태

- 액세스 토큰은 브라우저 탭의 `sessionStorage`에 보관합니다.
- 분석 동작은 로그인 사용자 ID와 `registeredByUserId`가 같은 매물에서만 노출됩니다. 백엔드도 소유권을 다시 검사합니다.
- 등기부 판독 중에는 5초마다 상태를 갱신합니다.
- 상세 화면을 다시 열거나 창에 포커스하면 결과를 조회해 `reanalysisAvailable`을 반영합니다.
