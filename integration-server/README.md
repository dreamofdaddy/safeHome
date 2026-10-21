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
MCP의 잘못된 JSON 요청에서 프레임워크 내부 스택 정보가 응답에 포함될 수 있어
현재 로컬 바인딩을 유지하며 운영 노출 전에 오류 직렬화를 제한해야 한다.

실행 가능한 Java 단독 검사:

```powershell
.\scripts\test-core.ps1
```

Redis 캐시는 1차 구현에 넣지 않았다. 이후 전체 수집 성공한 지번별 원천 데이터만 TTL 캐시하고
원래 조회시각을 보존한다. 부분 수집·인증 오류·UNVERIFIABLE은 캐시하지 않는다.

지속 호출 이력, 분산 호출량 제한, Redis 캐시, 내부 인증키 순환,
Origin 검증 및 운영 TLS는 후속 작업이다. HTTP 응답은 파싱 전에 메모리에 수신하므로
운영 배포 전 스트리밍 수신 크기 제한이 필요하다.
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
