# SafeHome 개발 지침

이 파일은 SafeHome 저장소 전체에 적용됩니다.

## 작업 시작 전 필수 확인

- 모든 개발 작업을 시작하기 전에 `docs/environment-roadmap.md`를 읽고 현재 개발 기준과 운영 전환 항목을 확인합니다.
- Frontend 작업은 `frontend/docs/production-readiness.md`도 함께 확인합니다.
- 환경, 의존성, 포트, 인증, 배포 또는 저장 방식이 바뀌면 구현과 같은 변경에서 관련 문서를 갱신합니다.
- 현재 개발 단계 항목과 운영 전환 항목을 섞지 않습니다. 운영 인프라가 필요한 변경은 `docs/environment-roadmap.md`의 운영 전환 목록에 기록합니다.

## 현재 개발 기준

- Frontend는 Node.js 24 LTS와 npm을 사용합니다.
- Frontend 설치는 `npm ci`, 전체 점검은 `npm run check`를 사용합니다.
- 브라우저의 Backend 요청은 `/api` 상대 경로를 사용합니다.
- PostgreSQL과 Redis의 로컬 포트는 `127.0.0.1`에만 연결합니다.
- 실제 API 키, 암호, 토큰, 운영 도메인과 암호화 키를 Git에 저장하지 않습니다.
- Backend API 계약 변경 시 Frontend 타입, 오류 처리와 관련 문서를 함께 갱신합니다.
- PDF 업로드 제한은 Frontend, Nginx와 Backend에서 일관되게 관리합니다.

## 완료 보고

- 적용한 변경과 검증 결과를 보고합니다.
- 운영 전환 시 처리할 사항이 새로 발견되면 `docs/environment-roadmap.md`에 추가하고 보고합니다.

## 등기부 테스트 자료

- 사용자 흐름 테스트에는 `test-data/registry-pdfs/`의 합성 PDF를 사용합니다.
- 각 PDF에 대응하는 주소와 거래 조건은 같은 폴더의 `README.md`를 따릅니다.
- 실제 개인정보나 실제 등기부 PDF는 Git에 추가하지 않습니다.
- 합성 PDF의 `테스트 전용 · 법적 효력 없음` 표시는 제거하지 않습니다.
