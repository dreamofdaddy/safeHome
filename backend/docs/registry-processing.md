# 등기부 PDF 판독 (6단계)

매물과 PDF 등록(1~5단계) 후, 로그인 사용자가 `POST /api/listings/{listingId}/registry-processing`을 호출하면 `202 Accepted`와 문서의 `PROCESSING` 상태를 즉시 받습니다. 등록자 본인만 호출할 수 있습니다. 중복 시작이나 완료 문서 재시작은 `409`입니다.

서버는 PDF 파일을 읽고 SHA-256 무결성을 확인한 뒤 `RegistryDocumentParser`를 통해 OpenAI Responses API에 판독을 요청합니다. 파서 구현은 교체할 수 있도록 인터페이스로 분리했습니다. 판독 결과의 고유번호, 매물 주소, 현재 소유자와 권리를 검증한 다음 한 트랜잭션에서 Property를 고유번호로 식별 또는 생성하고 PropertyOwner, RegistrySnapshot, RegistryRight를 저장합니다. 사용자가 입력한 매물 주소는 판독된 지번 주소 또는 도로명 주소 중 하나와 일치하면 통과합니다. 완료하면 Listing은 `REGISTRY_READY`, 문서는 `PARSED`가 됩니다. 이번 단계는 안전성 분석과 분석 결과 저장을 실행하지 않습니다.

`GET /api/listings/{listingId}/registry-processing`으로 문서 상태, 오류 코드, Property ID, RegistrySnapshot ID를 조회할 수 있습니다. 판독 실패 시 문서는 `FAILED`, Listing은 `REGISTRY_FAILED`가 되며 같은 POST로 재시도할 수 있습니다. 서버가 처리 중 재시작되면 `PROCESSING` 문서를 다시 큐에 넣습니다.

## 로컬 설정

실제 비밀 값은 저장소 밖 `D:/safehome-data/config/safehome-local.yml`에서 관리합니다. 사용자 환경변수 `SAFEHOME_CONFIG_FILE`은 그 파일의 경로를 가리킵니다. 설정 파일에는 `safehome.document.storage-root`, `safehome.document.encryption-key`, `safehome.owner-identity.encryption-key`, `safehome.owner-identity.hash-key`, `safehome.openai.api-key`, `safehome.openai.model`을 둡니다. 설정 파일을 수정한 뒤 서버를 다시 시작해야 반영됩니다. 소스의 `application.yml`은 환경변수 또는 기본값을 제공하며 실제 키는 포함하지 않습니다.

OpenAI API 결제 잔액이 없으면 `PARSER_CREDITS_EXHAUSTED`, 인증 오류는 `PARSER_AUTH_FAILED`, 요청 제한은 `PARSER_RATE_LIMITED`로 문서에 기록합니다. 실패한 문서에는 Property와 Snapshot을 저장하지 않습니다.

주소 검증은 `listings.address`와 `detail_address`를 공백으로 연결한 전체 주소를 판독된 지번 또는 도로명 전체 주소와 비교합니다. `제101동`·`101동`, `제202호`·`202호`와 공백·괄호·쉼표 표기는 통일합니다. 같은 전체 동·호수에서 한쪽의 지상 층 표기만 생략된 경우는 허용하되, 다른 지번·동·호수 또는 명시된 층/지하 차이는 거절합니다. 기존 Property 주소 비교도 같은 규칙을 적용합니다.
