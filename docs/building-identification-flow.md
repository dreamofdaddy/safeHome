# 건축물 식별 자동 연계

## 실행 경계

등기부 판독 결과·스냅샷을 저장하는 트랜잭션에서 `building_identifications` 기록을 만들고
`listings.current_building_identification_id`를 새 기록으로 바꾼다. 주소 검색의 확정 지번과
명시적 동·층·호가 충분하면 `PENDING`, 부족하거나 충돌하면 `INPUT_INCOMPLETE` 또는
`INPUT_CONFLICT`, 기능이 꺼져 있으면 `DISABLED`로 남긴다. 실제 Integration Server 호출은
판독 트랜잭션의 `afterCommit` 이후 별도 실행기에서만 시작한다.
각 기록에는 당시 매물 주소·상세주소·검색 근거, 판독한 동·층·호의 원문·정규화 값과
구성된 요청을 함께 보관한다.

주소 또는 상세주소·검색 근거가 변경되면 현재 포인터를 비운다. 새 등기부 스냅샷이
확정되면 새 기록을 만들고 포인터를 바꾼다. 이전 기록은 삭제하지 않는다. 작업자의
최종 저장은 포인터가 여전히 자기 기록을 가리키는 경우에만 `COMPLETED` 또는
`CALL_FAILED`가 된다. 그 사이 포인터가 바뀌면 이전 기록은 `SUPERSEDED`로 남고
새 기록의 최신 상태를 덮어쓰지 않는다.

## 상태와 표시

| 실행 상태 | 식별 결과 | 의미 |
|---|---|---|
| `NOT_REQUESTED` | 없음 | 현재 기록 없음 |
| `PENDING`, `RUNNING` | 없음 | 대기·조회 중 |
| `INPUT_INCOMPLETE` | 없음 | 확정 지번 또는 명시적 층·호 등 부족 |
| `INPUT_CONFLICT` | 없음 | 원문 출처 간 동·층·호 또는 주소 근거 충돌 |
| `DISABLED` | 없음 | 기능 비활성화 |
| `CALL_FAILED` | 없음 | 내부 호출·공급기관 호출 실패 또는 응답 계약 오류 |
| `COMPLETED` | `MATCHED`, `NOT_FOUND`, `AMBIGUOUS`, `UNVERIFIABLE` | 완료된 식별 결과 |
| `SUPERSEDED` | 있을 수 있음 | 최신성 무효화된 이전 기록 |

`PARENT_NOT_CORROBORATED`는 완료된 조회의 `UNVERIFIABLE`이다. 공급기관 장애,
시간 초과, 인증 실패, 내부 호출 장애 등 불완전한 호출은 `CALL_FAILED`로 기록한다.
`NOT_FOUND`를 안전 판정으로 해석하지 않는다. 건축물 식별 상태는 별도 매물 상세 API와
화면에 표시하며 위험 점수·등급·위험 요소 계산에 입력하지 않는다.

조회가 모두 완료됐지만 대상 전유부의 층번호만 확인할 수 없는 경우는
`COMPLETED / UNVERIFIABLE / CANDIDATE_FLOOR_NUMBER_UNAVAILABLE`이다.
공급기관의 `flrNo=0`을 유효한 층으로 해석하거나 호수로 층을 추정하지 않는다.
2026-10-21 검증 DB의 매물 3을 기존 저장 요청으로 재실행해 이 상태를 확인했으며,
기존 실패 이력은 남기고 새 이력만 현재 포인터로 연결했다. PDF 판독은 다시 실행하지 않았다.

## 재시작과 재시도

작업 기록은 외부 호출 전에 DB에 커밋되므로 재시작으로 실행기가 중단되어도 남는다.
기동 시 현재 포인터에서 벗어난 대기·진행 기록은 `SUPERSEDED`로 정리한다. 기능이
활성화되어 있으면 `PENDING`과 중단된 `RUNNING`을 다시 실행하되, 한 기록당 외부
호출 시도는 최대 2회다. 2회에 도달한 중단 작업은 `CALL_FAILED`로 종료한다.
이미 `CALL_FAILED`인 공급기관 장애는 재시작만으로 자동 반복하지 않는다. 기능이
꺼진 상태로 기동하면 남은 대기·진행 기록을 `DISABLED`로 두고 호출하지 않는다.
다시 활성화해 기동하면 현재 기록 중 남은 시도가 있는 `DISABLED` 작업을 재개한다.

현재 복구 정책은 로컬 단일 Backend 인스턴스를 기준으로 한다. 다중 인스턴스 운영에는
작업 소유권 임대와 분산 호출량 제한이 필요하다. 실행기 큐가 가득 차서 커밋 이후
즉시 제출하지 못한 `PENDING` 작업은 다음 기동에서 복구한다.

## 로컬 실호출 검증

Backend 자동 연계를 사용하려면 저장소 밖의 두 서버 공용 설정에
`safehome.integration.token`(32자 이상)을 두고, Backend 설정에서
`safehome.integration.enabled=true`를 명시한다. Integration Server에는
`safehome.building.service-key`가 필요하다. 키와 토큰은 저장소나 테스트 출력에
기록하지 않는다. 기능 활성화 설정이 없으면 Backend의 기본값은 `false`다.

`BuildingIdentificationLiveIntegrationTest`는 명시적으로 실행할 때만 동작한다.
실행 전 `safehome_stage6_live_test`라는 별도 PostgreSQL DB를 준비하고
`SPRING_DATASOURCE_URL`을 해당 DB로, `SPRING_FLYWAY_LOCATIONS`를
`classpath:db/migration`으로 지정한다. `SAFEHOME_LIVE_INTEGRATION_TEST=true`와
저장소 밖 공용 설정 파일 경로를 제공한 뒤 Integration Server를 켜고 이 테스트만 실행한다.
테스트는 실제 주소를 사용한 별도 매물을 만들고, 판독 커밋 이후 작업자 호출, 실서버
응답 저장, 상세 API 노출 및 개봉동 355-18의 `MATCHED`와 관리번호 연결을 확인한다.
Integration Server를 다시 빌드했다면 기존 JVM을 종료하고 새 JAR로 실행한 뒤 테스트한다.
PDF와 파서 출력은 합성 자료이므로 실제 등기부
원본 판독의 정확성을 입증하지 않는다. 화면의 완료·실패 문구는 Frontend 상세 페이지
회귀 테스트에서 검증한다.

## 2026-10-21 실제 화면 검증

전용 Frontend 5174를 Backend 8180에 연결해 검증 DB의 매물 3을 브라우저로 확인했다.
현재 이력 2번의 완료·식별 불가 상태를 표시하며 “건축물대장 조회 완료”와
“공급기관 층 정보 부족으로 확인 불가”를 함께 안내한다. 내부 reason 코드는 직접 표시하지 않는다.
기존 실패 이력 1번은 최신 결과로 표시되지 않고 등기부 판독 완료 상태도 유지된다.
실제 매물의 저장된 위험 분석은 없으므로, 위험 결과 유지 여부는 Backend 기존 장애 검사와
Frontend의 CALL_FAILED·COMPLETED/UNVERIFIABLE 회귀 검사로 확인했다.
Frontend 전체 36개 검사와 빌드가 통과했으며 재판독이나 위험 분석 시작 요청은 보내지 않았다.
