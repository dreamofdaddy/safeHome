# 사용자 안전성 분석 및 결과 조회 (7~9단계)

등기부 판독이 완료되어 Listing 상태가 `REGISTRY_READY`가 되면 로그인 사용자가 다음 API를 호출합니다. 두 API 모두 해당 매물을 등록한 사용자만 사용할 수 있습니다.

## 안전성 분석 실행

`POST /api/listings/{listingId}/safety-analysis`

서버는 해당 매물에 연결된 `PARSED` 등기부의 RegistrySnapshot을 기준으로 유효한 권리와 현재 소유자를 Risk Engine에 전달합니다. 활성 근저당권, 압류·가압류, 선순위 권리, 공동소유 관련 규칙을 평가하고 활성 정책 버전의 점수를 적용합니다.

분석 결과는 `risk_analyses`, 위험 요소는 `risk_factors`에 한 트랜잭션으로 저장합니다. Listing의 `risk_analysis_id`에 결과를 연결하고 상태를 `SAFETY_ANALYZED`로 변경합니다. 같은 RegistrySnapshot과 같은 정책 버전이면 저장된 결과를 반환해 중복 분석을 만들지 않습니다. 더 최신 RegistrySnapshot이 생기거나 정책 버전이 변경되면 다시 분석하고 Listing을 새 결과에 연결합니다. 과거 결과는 이력으로 보존합니다.

## 사용자 결과 조회

`GET /api/listings/{listingId}/safety-analysis`

응답의 주요 필드는 다음과 같습니다.

- `listingId`, `listingStatus`
- `analysis.id`, `riskLevel`, `riskScore`, `analysisVersion`, `policyVersion`, `analyzedAt`
- `analysis.factors[]`: 위험 유형, 심각도, 점수, 값, 설명
- `summary`, `highestSeverity`, `factorCount`
- `latestRegistrySnapshotId`, `reanalysisAvailable`, `reanalysisReason`

화면은 `/api/auth/me`의 사용자 ID와 Listing의 `registeredByUserId`가 같은 경우에만 분석 버튼을 표시합니다. 최초 분석은 Listing 상태가 `REGISTRY_READY`일 때, 재분석은 결과 응답의 `reanalysisAvailable`이 `true`일 때 활성화합니다. 서버도 매물 등록자 여부를 다시 검사하므로 다른 사용자의 직접 호출은 거부합니다.

등기부 판독이 완료되지 않은 매물의 분석 요청은 `409 ANALYSIS-002`, 분석 결과가 없는 매물의 결과 조회는 `404 ANALYSIS-003`, 다른 사용자의 매물 접근은 `403 AUTH-003`을 반환합니다.
