# 회원가입·로그인·등기부 업로드 매물 등록 API

현재 범위는 사용자 흐름의 1~5단계입니다. PDF 저장 후 자동 분석은 시작하지 않습니다. 매물은 `READY_FOR_ANALYSIS`, 문서는 `UPLOADED` 상태로 남습니다. `propertyId`는 분석 전까지 `null`입니다.

## 실행 설정

- PostgreSQL과 Redis를 실행합니다.
- 로컬 프론트엔드 개발 서버는 기본적으로 `http://localhost:5173`, `http://localhost:3000`에서 API를 호출할 수 있습니다. 다른 주소는 `SAFEHOME_CORS_ALLOWED_ORIGINS`에 쉼표로 구분해 지정합니다.
- PDF 원본은 서버 비공개 폴더에 파일로 저장합니다. `SAFEHOME_DOCUMENT_STORAGE_ROOT`로 경로를 지정합니다. 로컬 설정 예: `D:/safehome-data/registry-documents`. 미설정 시 앱 작업 폴더의 `./var/registry-documents`를 사용합니다.
- 새 PDF는 저장 폴더 아래 한국 시간 기준 일별 하위 폴더(`YYYY-MM-DD/UUID.pdf`)에 저장합니다. DB의 `registry_documents.storage_key`에는 이 상대 경로를 저장하며, PDF 파일을 정적 웹 경로로 공개하지 않습니다. 기존 최상위 폴더의 UUID 파일명도 읽을 수 있습니다.
- V10에서 저장된 기존 암호화 PDF를 읽을 때만 `SAFEHOME_DOCUMENT_ENCRYPTION_KEY`가 필요합니다.
- 기존 소유자 등록 API를 사용할 경우 `SAFEHOME_OWNER_ENCRYPTION_KEY`, `SAFEHOME_OWNER_HASH_KEY`도 각각 32자 이상 설정합니다.
- Flyway가 V11까지 적용되도록 앱을 재시작합니다.
- PDF는 최대 10MB, 1~50페이지이며 암호가 걸리지 않은 유효한 PDF여야 합니다.

## 1. 회원가입

`POST /api/auth/signup`, `Content-Type: application/json`

```json
{"email":"user@example.com","password":"at-least-12-characters","name":"홍길동","phone":"010-1234-5678"}
```

응답 `201`의 `data`에 사용자 ID가 들어갑니다. 이메일은 소문자로 정규화하고 비밀번호는 BCrypt 해시로만 저장합니다. 기존 `POST /api/users`는 사용하지 않습니다.

## 2. 로그인

`POST /api/auth/login`, `Content-Type: application/json`

```json
{"email":"user@example.com","password":"at-least-12-characters"}
```

응답 `200`의 `data.accessToken`을 이후 요청의 `Authorization: Bearer <accessToken>` 헤더로 전달합니다. 토큰은 12시간 유효합니다. `GET /api/auth/me`로 로그인 사용자 확인, `POST /api/auth/logout`으로 로그아웃할 수 있습니다.

## 3~5. 매물 정보와 등기부 PDF를 함께 등록

`POST /api/registry-documents/listings`, `Content-Type: multipart/form-data`

- `metadata`: `application/json` 파트. 예시:

```json
{
  "address":"서울특별시 강남구 테스트로 1",
  "buildingName":"테스트아파트",
  "buildingType":"APARTMENT",
  "detailAddress":"테스트아파트 101동 제101호",
  "transactionType":"JEONSE",
  "depositAmount":200000000
}
```

- `registryPdf`: `application/pdf` 파일 파트.

거래 유형은 `JEONSE`, `MONTHLY_RENT`, `SALE`입니다. 월세는 `depositAmount`와 `monthlyRent`, 매매는 `salePrice`가 필요합니다.
`address`에는 사용자가 선택한 지번 또는 도로명 기본 주소를 전달하고 `detailAddress`에는 나머지 상세주소를 전달합니다. 검증 시 두 값을 합칩니다. 우편번호와 별도 도로명 주소 필드는 사용하지 않습니다.

성공 시 `201`로 `data.listing`, `data.document`, `data.message`를 반환합니다. `message`는 `등기부 업로드 완료`입니다. 등록자 ID는 요청 본문이 아닌 로그인 토큰에서 결정됩니다. Listing과 PDF 메타데이터는 하나의 DB 트랜잭션으로 저장됩니다. 트랜잭션이 실패하면 새로 쓴 PDF 파일을 삭제합니다.

## 등록 결과 조회

- `GET /api/listings/{id}`: 등록된 매물과 `READY_FOR_ANALYSIS` 상태 확인
- `GET /api/listings/mine`: 로그인 사용자가 등록한 매물 목록
- `GET /api/registry-documents/mine`: 본인이 올린 문서 목록
- `GET /api/registry-documents/{id}`: 문서 메타데이터
- `GET /api/registry-documents/{id}/file`: 본인이 올린 PDF 다운로드

모든 조회에는 Bearer 토큰이 필요합니다. 다른 사용자의 PDF는 `403`입니다. 이전 개발용 범용 CRUD API는 기본 설정에서 `403`으로 차단됩니다. 필요한 내부 테스트에서만 `safehome.legacy-admin-api.enabled=true`로 열 수 있습니다. PDF 판독과 Property·소유자·권리 저장은 `registry-processing.md`의 6단계 API에서 시작합니다. 안전성 분석은 이후 단계입니다.

## 판독 전 수정·삭제

`PUT /api/listings/{id}`는 등록 metadata와 같은 JSON으로 입력 내용을 수정합니다(PDF는 유지). `DELETE /api/listings/{id}`는 매물과 첨부 PDF를 삭제합니다. 본인 소유의 `READY_FOR_ANALYSIS` 또는 `REGISTRY_FAILED` 매물만 허용합니다. 판독 시작과 같은 행 잠금을 사용하며 판독 중·성공 이후는 409로 거절합니다.
