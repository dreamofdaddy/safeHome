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
    LISTING_NOT_EDITABLE(HttpStatus.CONFLICT, "LISTING-001", "등기부 판독 대기 또는 실패 상태에서만 수정·삭제할 수 있습니다. 판독 중에는 완료 후 다시 확인해 주세요."),
    CONFLICT(HttpStatus.CONFLICT, "ANALYSIS-001", "이미 분석 중이거나 완료된 매물입니다."),
    ANALYSIS_NOT_READY(HttpStatus.CONFLICT, "ANALYSIS-002", "안전성 분석을 실행할 수 없는 매물 상태입니다."),
    ANALYSIS_RESULT_NOT_FOUND(HttpStatus.NOT_FOUND, "ANALYSIS-003", "안전성 분석 결과가 없습니다."),

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
