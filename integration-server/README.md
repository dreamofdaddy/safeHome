## 2026-10-21 사용자 확정 정책: 재시도 3회·호수 기반 층 추정

검증: Integration JUnit 24개·코어 105개, Backend 관련 6개, Frontend 41개·lint·배포 빌드 및 JAR 빌드 통과. 실제 공공 API·OpenAI는 이번 정책 변경 검증에서 호출하지 않았다.

이 절은 앞서 기록된 최대 1회 재시도 및 호수로 층을 추정하지 않는 정책을 대체한다. 과거 검증 기록은 당시 결과로 보존한다.

- 일시적 HTTP 502/503/504·통신 오류·빈 응답·일반 XML 구문 실패는 최초 호출 후 최대 3회 재시도한다(페이지당 최대 4회). 간격은 300/600/900ms이며 기존 전체 120초 예산·속도·페이지·응답 크기 제한을 유지하므로 남은 예산에 따라 일찍 중단할 수 있다.
- 인증/권한 오류, 금지된 DOCTYPE, 크기 초과, 입력·페이징 오류는 반복 호출로 해결하지 않는다. 실패·부분 수집을 확정 결과로 바꾸지 않는다.
- 공급기관의 유효한 층번호가 없고 지상 구분이 명확한 경우, 정규화된 3~5자리 숫자 호수에서 마지막 두 자리를 제외한 숫자를 층으로 추정한다. 예: 202호→2층, 1203호→12층. 끝 두 자리가 00인 호수, 문자·복합 호수와 지하/구분 불명은 이 규칙으로 추정하지 않는다.
- 명시된 공급기관 층번호를 우선하고 기존 지번·동·호·건물 관리번호 관계·유일성 검사는 유지한다. 추정 층과 요청 또는 호명칭의 명시적 층이 다르면 FLOOR_INFERENCE_CONFLICT / UNVERIFIABLE이다. 추정 불일치만으로 NOT_FOUND를 선언하지 않는다.
- 추정에 따른 일치 또는 중복 후보에는 FLOOR_INFERRED_FROM_UNIT_NUMBER를 reason에 저장·전달한다. 공급기관 원문과 원래 null인 층 필드는 덮어쓰지 않는다. 화면에서 추정 기반 결과임과 원본 확인 필요를 안내한다. 위험 점수·등급은 바꾸지 않는다.
- 본인 매물의 CALL_FAILED와 COMPLETED/UNVERIFIABLE/CANDIDATE_FLOOR_NUMBER_UNAVAILABLE에서 새 이력으로 재검증할 수 있다. 최신 PARSED 문서·스냅샷, 동시 중복 제한과 소유자 검사를 유지한다. 이전 이력은 보존하고 등기부 재판독·안전성 재분석은 실행하지 않는다.
- DB 열·응답 필드 추가는 없으며 기존 reason으로 근거를 보존한다. 변경 전 저장 결과는 자동으로 새 정책의 결과로 바꾸지 않는다.

# SafeHome Integration Server — 개발 6단계 1차 구현

Java 21 / Spring Boot 4.1.1 / Spring AI BOM 2.0.1 / Gradle Wrapper 9.7.1.
기존 Backend와 별도 프로세스이며 DB 스키마 변경이 없다.

## 실행

실제 키를 저장소·명령 실행 로그에 남기지 않는다.
`SAFEHOME_CONFIG_FILE`로 저장소 밖 `D:/safehome-data/config/safehome-local.yml`을 지정할 수 있다.
설정은 `safehome.building.service-key`와 `safehome.integration.token`을 사용한다.
서비스 키는 공공데이터포털의 **디코딩 키**, 내부 토큰은 32자 이상 무작위 값으로 구성한다.
Backend에도 같은 내부 토큰을 공급한다. 환경변수로 직접 주입할 때는 아래 이름을 사용한다.

- BUILDING_HUB_SERVICE_KEY
- SAFEHOME_INTEGRATION_TOKEN
- 선택: INTEGRATION_PORT (기본 8090), INTEGRATION_BIND_ADDRESS (기본 127.0.0.1)

```powershell
cd D:\projects\safeHome\integration-server
.\gradlew.bat clean test coreTest bootJar
java -jar .\build\libs\safehome-integration-server-0.1.0-SNAPSHOT.jar
```

소스나 JAR를 갱신한 뒤에는 기존 서버 JVM을 종료하고 새 JAR로 다시 실행한다.
이미 실행 중인 `bootRun` 프로세스는 클래스 파일이 다시 빌드되어도 코드가 바뀌지 않는다.

GET /actuator/health는 인증 없이 로컬 상태만 제공한다.
REST POST /internal/v1/buildings/resolve 및 MCP /mcp 요청에는
X-SafeHome-Integration-Token 헤더가 필요하다. 서버 토큰 미설정·32자 미만이면 503,
잘못된 토큰이면 401이다. 공공 API 키 미설정은 UNVERIFIABLE이다.

REST 응답은 Backend와 같은 success/data/error 구조를 따른다.
식별 결과는 data.status이며 외부 API 장애도 정상 처리된 식별 응답으로
success=true, data.status=UNVERIFIABLE을 반환한다.
잘못된 요청은 HTTP 400 / INTEGRATION-001이다.
MCP 도구 resolve_building_unit은 같은 검증과 서비스로 Resolution을 직접 반환한다.

## 구현 범위

- 지번코드 검증, 선택적 동, 필수 지상/지하·층·호
- 전각 문자·공백·제/동/호 장식 정규화, 호명칭 안의 층 표시 교차 검사 및 원문 보존
- 전체 페이지 개수·페이지 번호·누락·중복 관리번호·총건수 변화 검사
- 전유부, 표제부, 기본개요 조회; 기본개요의 `mgmBldrgstPk`로 전유부를 찾고
  기본개요의 `mgmUpBldrgstPk`를 표제부 `mgmBldrgstPk`와 교차 확인
- 3초 연결·8초 HTTP 요청 제한, 전체 조회 120초 예산, 최대 2회 시도
- HTTP 502/503/504, I/O, 공공 API 01/05/23만 1회 재시도
- 인증 실패·일일 한도·형식 오류는 재시도하지 않음
- 동시 분석 2건, 외부 호출 최소 간격 200ms, 최대 200페이지/20,000건
- 페이지당 2,000,000바이트 수신 상한: 길이 지정·미지정 응답 모두 초과 시 취소·재시도 금지
- MCP 오류 응답의 예외·스택·원인·data 비노출, 정상 도구 근거 유지
- XML 외부 엔티티·DTD 차단; URI·응답 원문·키를 오류에 보관하지 않음
- Backend BuildingEvidenceClient는 safehome.integration.enabled=true일 때만 등록됨

Backend 연결 속성은 safehome.integration.base-url, safehome.integration.token이다.
자동 분석에서는 매물 주소 검색의 검증된 법정동 코드와 등기부·매물의 명시적 동·층·호를
대조해 입력을 만들고, 판독 커밋 이후에만 호출한다. 정보가 부족하거나 충돌하면 호출하지 않는다.
건축물 식별 결과를 위험점수로 자동 변환하지 않는다.

## 검증 상태와 제한

### 2026-10-21 후속 재점검 완료

- 기본 격리 실행에서는 Git·Docker 파이프·TCP 접근이 거부됐으나, 권한 허용 실행에서 원본·데이터 폴더의
  읽기/쓰기, Git, Gradle, 8090·8180·5432 접근을 확인했다. 종료돼 있던 기존 PostgreSQL·Redis 컨테이너를
  데이터 볼륨 변경 없이 다시 시작했으며 두 서버의 health가 UP이다.
- 기존 JAR를 저장소 밖 작업 폴더에 백업한 뒤 `test coreTest bootJar --no-daemon --console=plain`을 통과했다.
  기존 진단 검사 100개를 보존하고 층번호 관련 5개를 추가해 코어 검사는 105개다.
  8090은 최종 빌드 JAR로 재시작했으며 SHA-256은
  `E862E549322FAF5FF1093016B4B7B4D6CBEAE52CB2E0272F0AE5FB79A5E0659F`이다.
- 검증 DB `safehome_stage6_real_pdf_test`의 매물 3에 저장된 요청으로 재현했다.
  현재 전유부 53건·표제부 3건·기본개요 57건은 XML 파싱과 전체 수집에 성공한다.
  동일한 현재 전유부 응답은 백업된 기존 JAR의 파서에서도 성공하므로, 과거 `MALFORMED_RESPONSE`의
  정확한 원인을 당시 응답 없이 단정하지 않는다. 원문 응답은 출력하거나 저장하지 않았다.
- 현재 대상 후보의 전유부 `flrNo`는 0이며 호명칭에도 명시적 층이 없다. 호수로 층을 추정하지 않으며
  단일 원인이 층번호 확인 불가일 때 `CANDIDATE_FLOOR_NUMBER_UNAVAILABLE`을 반환하도록 진단을 보완했다.
  수집은 완료됐으므로 `collectionComplete=true`, 결과는 `UNVERIFIABLE`이며 `MATCHED`로 처리하지 않는다.
  다른 필수 필드도 부족하면 기존 `UNINTERPRETABLE_CANDIDATE`를 유지한다.
- 기존 실패 이력 1번을 보존하고 요청·입력 스냅샷을 그대로 복사한 새 이력 2번을 실제 Backend의
  Worker·Client·Store 코드로 재실행했다. 결과는
  `COMPLETED / UNVERIFIABLE / CANDIDATE_FLOOR_NUMBER_UNAVAILABLE`이며 8180 상세 조회 API도 HTTP 200으로
  같은 결과를 반환한다. 검증용 임시 인증 토큰은 확인 직후 삭제했다.
  기존 `PARSED / OPENAI` 결과와 등기부 스냅샷은 변경하지 않았고 OpenAI 재판독은 호출하지 않았다.
- 기존 미커밋 변경과 데이터는 보존했으며 커밋·push하지 않았다. 아래 접근 차단 기록은 이전 실행 이력이다.

2026-10-21 응답 진단 패치를 적용하고 Java 단독 코어 검사 100개를 통과했다.
외부 호출 오류는 `EXPOS_PAGE_1_MALFORMED_RESPONSE_XML`처럼 API 종류·페이지·실패 단계를
`reason`에 포함한다. 파싱 단계는 `XML_SETUP`, `XML`, `HEADER`, `PAGINATION`, `ITEMS`이며,
응답 원문·요청 URI·키·예외 원인은 포함하지 않는다. HTTP 503의 1회 재시도와
형식 오류의 재시도 금지, 정상 0건 응답, 2페이지 실패 위치를 합성 자료로 검사했다.
Backend는 이 reason을 고정 문자열로 분기하지 않으므로 기존 실패 상태 판정은 유지된다.
매물 3의 기존 `MALFORMED_RESPONSE`만으로 실제 실패 단계를 확정할 수 없다.
실제 재현에는 검증 DB `safehome_stage6_real_pdf_test`의 저장된 `query_json`을 사용하고,
OpenAI 판독은 반복하지 않는다. DB 요청과 외부 응답 원문을 콘솔에 출력하지 않는다.
이번 실행 세션에서는 원본 쓰기·Git 조회는 가능했으나 Docker 엔진 파이프는 접근 거부,
8090·8180·5432 연결은 실패했다. 기본 Gradle 캐시 쓰기가 거부되어 별도 캐시로도 시도했지만
Gradle 데몬 연결에 실패했다. 따라서 이번 변경의 전체 `test coreTest bootJar`,
실행 중 서버 교체 및 저장 요청 실호출은 완료하지 못했으며 기존 JAR는 갱신하지 않았다.
실제 파싱 실패 단계는 미확정이고 기존 미커밋 변경과 검증 DB는 보존했다.

2026-10-20 현재 `clean test coreTest bootJar` 및 Java 단독 합성 시나리오 70개 통과.
`101호`/`제101호`/`0101호`와 `202호`·`1203호`의 제 접두사·호 접미사·선행 0,
`1층`/`제1층`, 층·호 복합 표기,
`160동`/`제160동`의 동등성과 원문 보존을 검사한다.
지하 호명칭은 `지층 101호`, `지하2층 101호`, `B2층 101호` 형식을 지원한다.
공백과 `제` 접두사는 정규화한다. `지층`은 지하 여부만 나타내며 층번호는
별도 API 필드를 사용한다. `지하N층`과 대문자 `BN층`의 N은 별도 API 층번호와 교차 검사한다.
호명칭에 층 표시가 없으면 별도 API 층구분·층번호를 사용하며,
별도 층 정보가 없으면 호수 또는 호명칭의 층 표시만으로 층을 확정하지 않는다.
호명칭의 지상·지하 또는 층번호가 별도 정보와 충돌하면 UNVERIFIABLE이다.
합성 테스트의 5,678건/72개 동 자료와 실제 조회 결과를 혼동하지 않는다.

Spring 부트스트랩·REST 인증과 오류 응답·MCP 초기화와 도구 호출·Backend 회귀를 확인했다.
Docker Engine과 PostgreSQL·Redis 컨테이너의 healthy 상태도 확인했다.
업로드된 공식 가이드와 실제 API 응답을 대조했다. 가이드에서 상위 관리번호 필드는
전유부가 아닌 기본개요의 `mgmUpBldrgstPk`로 명시된다.
개봉동 355-18의 지상 1층 101호는 전유부 `1018152541` → 기본개요의 상위 PK
`1018112940` → 표제부 `1018112940`으로 확인되어 REST `MATCHED`를 반환했다.
잠실동 19의 160동 101호는 전유부 `10251100215481` → 기본개요의 상위 PK
`10251100215480` → 표제부 `10251100215480`으로 확인되어 REST `MATCHED`를 반환했다.
잠실동 조회는 표제부 95건, 전유부 5,678건, 기본개요 5,774건을 전체 수집했고
수집 완료 응답까지 약 24초가 걸렸다. 키·토큰·원문 응답은 저장하지 않았다.
현재 불명확한 항목은 UNVERIFIABLE로 처리하므로 확정 범위가 보수적이다.
당시 MCP의 잘못된 JSON 요청에 내부 스택이 포함되는 문제가 있었다.
2026-10-21 즉시 보완으로 오류 직렬화를 제한했으며 아래 보완 기록을 따른다. 로컬 바인딩은 유지한다.

실행 가능한 Java 단독 검사:

```powershell
.\scripts\test-core.ps1
```

Redis 캐시는 1차 구현에 넣지 않았다. 이후 전체 수집 성공한 지번별 원천 데이터만 TTL 캐시하고
원래 조회시각을 보존한다. 부분 수집·인증 오류·UNVERIFIABLE은 캐시하지 않는다.

로컬 호출 이력은 아래 2026-10-21 보완으로 구현했다. 중앙 수집·관측 지표, 분산 호출량 제한, Redis 캐시, 내부 인증키 순환,
Origin 검증 및 운영 TLS는 후속 작업이다. HTTP 응답의 수신 중 크기 제한은
2026-10-21 즉시 보완으로 적용했으며 실측 부하 검증은 운영 전환 전에 수행한다.
공개 네트워크 바인딩은 이 추가 보안·운영 작업 이후에 수행한다.

### 2026-10-21 화면 마감

검증 Frontend 5174 → Backend 8180 → 검증 DB의 매물 3을 실제 브라우저로 확인했다.
현재 이력 2번의 조회 완료·식별 불가와 공급기관 층 정보 부족 안내가 표시되며,
과거 실패 문구나 내부 사유 코드는 노출되지 않는다. 기존 PARSED 상태를 유지했다.
저장된 위험 분석이 없는 실제 매물이므로 위험 결과 보존은 회귀 검사로 확인했다.
Frontend 36개 검사·배포 빌드·최종 lint 통과. 기존 서버 전체 검사·코어 105개·JAR 빌드 결과를 재사용했다.
이번에는 OpenAI 재판독과 외부 API 재조회를 실행하지 않았다.
임시 브라우저 인증 세션과 부트스트랩은 폐기했고 키·PDF·진단 도우미는 커밋 대상에서 제외했다.
완료 범위와 운영 전환 전 남은 과제는 `../docs/stage6-plan.md`를 따른다.
앞선 “커밋하지 않음” 기록은 당시 상태이며, 이번 마감 변경은 사용자 지시에 따라 선별 커밋한다. push는 하지 않는다.

## 2026-10-21 즉시 보완 완료: MCP 오류 응답·외부 응답 크기 제한

- MCP의 MVC 오류 응답과 JSON/SSE 응답에 같은 오류 직렬화 정책을 적용했다.
  JSON-RPC 오류는 코드와 확인 가능한 요청 ID를 유지하고 고정된 안내만 반환한다.
  해석되지 않은 요청의 ID는 null이다. 예외 메시지·스택·원인·data는 응답에 포함하지 않는다.
  도구 실행 실패의 content·structuredContent·메타정보도 고정 안내로 제한한다.
  정상 도구의 내용·구조화 근거·메타정보는 유지한다.
- 건축HUB 응답은 페이지당 기존 상한인 2,000,000바이트를 수신 중 적용한다.
  Content-Length가 상한을 넘으면 본문을 요청하지 않고 취소한다. 길이가 없거나 분할 수신되는 경우에도
  누적 크기를 먼저 검사하고 초과한 청크를 저장하지 않은 채 취소한다. 파싱 직전 검사도 유지한다.
  크기 초과는 `API_PAGE_n_RESPONSE_SIZE`로 보고하고 재시도하지 않는다. 부분 데이터를 확정하지 않는다.
- 전체 `test coreTest bootJar` 검증: JUnit 11개(기존 4개 + 추가 7개), 합성 코어 105개 통과.
  실제 로컬 HTTP에서 크기 경계·길이 지정/미지정 초과·503 분류를 확인했다.
  실제 로컬 MCP에서 인증·잘못된 JSON·알 수 없는 메서드·도구 인자 오류와
  정상 초기화·도구 목록·도구 호출을 확인했다. 외부 API와 OpenAI는 호출하지 않았다.
- 이번 변경은 DB·Backend·Frontend 계약을 바꾸지 않는다. 기존 판독·식별 이력은 재실행하지 않는다.
- 호출 이력·지표·로그 마스킹 점검, Redis 캐시, 다중 인스턴스 제어, 운영 TLS·인증·부하 검증은 후속 과제다.

실행 반영 확인: 8090을 새 JAR로 재시작하고 health UP, 잘못된 MCP JSON의 HTTP 400 및
예외·스택·입력 원문 비노출, 인증 헤더 누락 시 HTTP 401을 확인했다.
이 검사는 외부 API·OpenAI 호출이나 매물 결과 재실행을 하지 않았다.
적용 JAR SHA-256: `E1E12948EE84E778112AD0FC60408B618AD25F68355C63448CF4A733B6BD5669`.

## 2026-10-21 호출 이력 저장·비밀정보 비노출 검증

### 저장과 연결

- Integration Server는 작업 시작·종료 및 외부 API의 각 페이지·시도별 메타정보를 기록한다.
  저장 위치는 `D:/safehome-data/integration/call-history`, 파일명은 `building-calls-yyyy-MM-dd.jsonl`이다.
  날짜는 UTC이며 한 줄에 JSON 한 건을 추가하고 파일 동기화를 요청한다. 기존 기록은 다시 열어도 유지한다.
  저장소 안에 이력 파일을 두지 않는다. 설정은 `safehome.building.call-history-directory` 또는
  `SAFEHOME_INTEGRATION_CALL_HISTORY_DIR`로 변경할 수 있다. 별도 DB·의존성·스키마는 추가하지 않았다.
- 작업마다 UUID `job.id`를 생성한다. Backend 작업자는 `building_identifications.id`를
  선택적 내부 헤더 `X-SafeHome-Identification-Id`로 전달하며 이력의 `job.identificationId`에 저장한다.
  REST 응답의 `X-SafeHome-Integration-Job-Id`로 작업 ID를 확인한다. 기존 JSON 본문 계약은 유지한다.
  REST 직접 호출·MCP에는 Backend 식별 번호가 없을 수 있다. 유효하지 않은 헤더는 원문 없이 HTTP 400이다.
- `JOB_STARTED`, `API_ATTEMPT`, `JOB_FINISHED`를 기록한다. 필드는 시각, 작업 ID, 식별 이력 번호,
  진입 경로(REST/MCP/DIRECT), API 종류(TITLE/EXPOS/BASIS), 페이지, 페이지 내 시도 번호(1/2),
  처리시간(ms), 성공·실패 구분, 허용된 오류 코드, 최종 식별 상태·수집 완료 여부다.
  재시도는 각각 별도 줄이다. 중단·재기동 후 같은 Backend 이력의 재시도에는 새 작업 UUID가 부여된다.
- 페이지의 SUCCESS는 수신·파싱 성공이다. 전체 수집·관계 검증·최종 식별 성공을 의미하지 않으며
  `JOB_FINISHED`의 상태와 함께 해석한다. 부분 조회 실패는 여전히 UNVERIFIABLE이며 확정하지 않는다.

### 비노출·저장 장애 정책

- 스키마에 주소·동·호·공공 API 키·내부 토큰·요청/응답 본문·URL·헤더·예외 객체를 넣지 않는다.
  오류 코드는 고정 허용 목록으로 제한하며 알 수 없는 예외 내용은 `UNEXPECTED_FAILURE`로 대체한다.
  마스킹에 의존해 원문을 저장하지 않는다.
- 기동 시 디렉터리 쓰기를 확인하고 실패하면 경로나 예외 내용을 포함하지 않는 메시지로 기동을 중단한다.
  실행 중 저장 실패는 식별 결과를 바꾸지 않고 `BUILDING_CALL_HISTORY_WRITE_FAILED` 경고와
  프로세스 내 실패 횟수를 남긴다. 실패 시 일부 기록은 누락될 수 있으므로 이 파일은 회계 수준 감사 원장이 아니다.
- 한 Integration Server 프로세스의 동시 요청 기록을 직렬화한다. 운영에서는 인스턴스별 저장 위치와
  중앙 수집, 디스크 용량·보존 기간·백업·권한·알림 정책을 별도로 확정한다. 자동 삭제는 구현하지 않았다.
- 이번 비노출 검증 범위는 건축HUB 호출 이력과 관련 오류 로그다. 전체 시스템의 운영 로그 검토는 별도다.

### 확인 결과

- Integration Server `test coreTest bootJar`: JUnit 18개, 합성 코어 105개 통과.
  페이지 2 실패·재시도·인증 오류·파일 재개방·동시 기록·UTC 날짜 분리·저장 실패를 검사했다.
  합성 키·토큰·주소/응답·URI가 이력과 관련 애플리케이션 로그에 노출되지 않는지 확인했다.
- Backend 연결 단위·흐름 검사 6개 통과. 흐름 검사는 전용 DB `safehome_stage6_history_test`에서
  합성 PDF·가짜 파서를 사용했다. 기존 실제 PDF 검증 DB는 테스트에 사용하지 않았다.
- 8090 Integration Server와 8180 검증 Backend를 새 JAR로 재시작하고 health UP을 확인했다.
  8180은 `safehome_stage6_real_pdf_test`와 기존 검증 PDF 저장 위치를 유지한다.
  저장된 판독·스냅샷·매물·식별 이력·위험 분석/요소의 해시가 재시작 전후 동일하다.
- 실제 저장 폴더에 외부 호출이 없는 합성 DIRECT 작업의 시작·종료 2줄을 저장해 영속성을 확인했다.
  실제 실행 로그에서도 공유 설정의 키·토큰이 발견되지 않았다. 공공 API와 OpenAI는 호출하지 않았다.
  기존 매물의 판독·식별 재실행은 하지 않았으며 이전 이력을 소급 생성하지 않는다.

### 로컬 조회 예시

```powershell
# UTC 날짜 파일에서 특정 Backend 식별 이력의 이벤트를 조회한다. 아래 번호는 예시다.
Get-Content 'D:/safehome-data/integration/call-history/building-calls-2026-10-21.jsonl' |
    ForEach-Object { $_ | ConvertFrom-Json } |
    Where-Object { $_.job.identificationId -eq 42 }
```

호출량·응답시간 지표 집계와 Redis 캐시·조회 예산 조정은 이 기록을 측정한 뒤 진행한다.
# 2026-10-21 XML 실패 진단 보완

XML 파싱 실패의 API_ATTEMPT 이벤트에 선택적인 `xmlFailure`를 추가한다.
필드는 `kind`, `line`, `column`, `responseBytes`, `responseSha256`이다.
유형은 ENTITY_REFERENCE, INVALID_CHARACTER, ENCODING, PROLOG, TRUNCATED, DOCTYPE_FORBIDDEN, OTHER_SYNTAX로 제한한다.
원문·예외 메시지·원인·URL·키는 보존하지 않는다. 기존 MALFORMED_RESPONSE_XML 코드와 자동 재시도 제한은 유지한다.
합성 잘못된 XML로 진단 정보와 비노출을 검증했다. JUnit 19개·코어 105개·JAR 빌드 통과.

일반 개발 매물의 BASIS 첫 페이지 XML 실패는 승인된 재조회에서 재현되지 않았다.
현재 응답 57건은 정상 파싱되었으며 최초 실패 원인은 미확정이다. 실제 원문은 Git·로그에 저장하지 않는다.
사용자가 요청한 열람 자료는 제한 필드만 별도 데이터 폴더로 내보냈다.
Backend의 사용자 재시도는 기존 실패 이력을 보존하고 새 이력을 생성한다. PDF 재판독은 실행하지 않는다.
# 2026-10-21 빈 응답 분리

일반 개발 매물 49의 사용자 재시도 이력 2번에서 EXPOS 첫 페이지의 응답 바이트 수가 0으로 확인됐다.
그 이력은 기존 MALFORMED_RESPONSE_XML로 보존한다. 빈 응답의 외부 원인은 미확정이며 이전 BASIS 오류와 동일 원인으로 단정하지 않는다.
새 요청에서 0바이트 본문은 EMPTY_RESPONSE로 구분하고 최대 한 번 자동 재시도한다.
기존 페이지당 최대 2회 시도와 전체 조회 예산을 유지한다. 반복 빈 응답은 수집 실패이며 NOT_FOUND나 확정 결과로 바꾸지 않는다.
인증 오류와 비어 있지 않은 잘못된 XML의 재시도 정책은 유지한다. 키·URL·응답 원문은 기록하지 않는다.
로컬 HTTP 서버로 빈 HTTP 200 후 정상 응답 복구 및 반복 빈 응답 중단을 검사한다. 실제 공공 API·OpenAI 호출은 하지 않는다.
