package com.safehome.backend.common.exception;

import org.springframework.http.HttpStatus;

public enum ErrorCode {

    INVALID_REQUEST(
            HttpStatus.BAD_REQUEST,
            "COMMON-001",
            "잘못된 요청입니다."
    ),

    RESOURCE_NOT_FOUND(
            HttpStatus.NOT_FOUND,
            "COMMON-002",
            "요청한 리소스를 찾을 수 없습니다."
    ),

    PAYLOAD_TOO_LARGE(HttpStatus.CONTENT_TOO_LARGE, "DOCUMENT-001", "PDF 파일 크기가 제한을 초과했습니다."),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "AUTH-001", "로그인이 필요합니다."),
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "AUTH-002", "이메일 또는 비밀번호가 올바르지 않습니다."),
    FORBIDDEN(HttpStatus.FORBIDDEN, "AUTH-003", "접근 권한이 없습니다."),
    INVALID_PASSWORD_POLICY(HttpStatus.BAD_REQUEST, "AUTH-004", "비밀번호는 12자 이상이며 영문, 숫자, 특수문자를 모두 포함해야 합니다."),
    PASSWORD_REUSE(HttpStatus.BAD_REQUEST, "AUTH-005", "현재 또는 직전 비밀번호는 다시 사용할 수 없습니다."),
    PASSWORD_CHANGE_REQUIRED(HttpStatus.FORBIDDEN, "AUTH-006", "비밀번호 변경이 필요합니다."),
    INVALID_PHONE(HttpStatus.BAD_REQUEST, "AUTH-007", "휴대전화번호는 010으로 시작하는 11자리 숫자여야 합니다."),
    CURRENT_PASSWORD_MISMATCH(HttpStatus.UNAUTHORIZED, "AUTH-008", "현재 비밀번호가 올바르지 않습니다."),
    INVALID_EMAIL(HttpStatus.BAD_REQUEST, "AUTH-009", "정확한 이메일 주소를 입력해 주세요. 예: name@example.com"),
    LISTING_IDENTITY_LOCKED(HttpStatus.CONFLICT, "LISTING-002", "매물 수정에서는 거래조건만 변경할 수 있습니다. 주소·상세주소·건물명·건물 유형은 변경할 수 없습니다."),
    LISTING_NOT_EDITABLE(HttpStatus.CONFLICT, "LISTING-001", "판독 또는 건축물 조회 처리 중입니다. 완료 후 매물을 수정·삭제해 주세요."),
    CONFLICT(HttpStatus.CONFLICT, "ANALYSIS-001", "이미 분석 중이거나 완료된 매물입니다."),
    ANALYSIS_NOT_READY(HttpStatus.CONFLICT, "ANALYSIS-002", "안전성 분석을 실행할 수 없는 매물 상태입니다."),
    ANALYSIS_RESULT_NOT_FOUND(HttpStatus.NOT_FOUND, "ANALYSIS-003", "안전성 분석 결과가 없습니다."),
    BUILDING_RETRY_NOT_ALLOWED(HttpStatus.CONFLICT, "BUILDING-001", "건축물대장 조회가 진행 중이거나 조회 정보가 변경되었습니다. 현재 상태를 새로고침해 주세요."),
    BUILDING_INTEGRATION_DISABLED(HttpStatus.SERVICE_UNAVAILABLE, "BUILDING-002", "건축물대장 조회 기능이 비활성화되어 있습니다."),

    BUILDING_DOCUMENT_INVALID(HttpStatus.BAD_REQUEST, "BUILDING-DOC-001", "암호가 없는 PDF를 선택해 주세요. 최대 5개, 합계 10MB·50페이지까지 등록할 수 있습니다."),
    BUILDING_DOCUMENT_STATE(HttpStatus.CONFLICT, "BUILDING-DOC-002", "최신 자료의 판독 대기·실패 상태에서만 시작할 수 있습니다. 자료가 변경됐다면 새 묶음을 등록해 주세요."),
    BUILDING_DOCUMENT_CONSENT(HttpStatus.BAD_REQUEST, "BUILDING-DOC-003", "건축물대장 PDF를 OpenAI로 전송하는 판독에 동의해 주세요."),

    INTERNAL_SERVER_ERROR(
            HttpStatus.INTERNAL_SERVER_ERROR,
            "COMMON-999",
            "서버 내부 오류가 발생했습니다."
    );

    private final HttpStatus status;
    private final String code;
    private final String message;

    ErrorCode(
            HttpStatus status,
            String code,
            String message
    ) {
        this.status = status;
        this.code = code;
        this.message = message;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}
