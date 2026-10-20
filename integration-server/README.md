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
.\gradlew.bat bootRun
```

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
자동 분석 흐름에는 아직 연결하지 않았다. 현재 등기 판독 결과에는 법정동 코드와
지상/지하 정보가 없어 이를 추정해 자동 전송하면 오매칭될 수 있다.
건축물 식별 결과를 위험점수로 자동 변환하지 않는다.

## 검증 상태와 제한

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
