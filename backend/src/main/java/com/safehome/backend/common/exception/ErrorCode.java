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
