package com.safehome.backend.common.exception;

import com.safehome.backend.common.response.ApiResponse;
import com.safehome.backend.common.response.ErrorResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.http.converter.HttpMessageNotReadableException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log =
            LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<ErrorResponse>> handleBusinessException(
            BusinessException exception
    ) {
        ErrorCode errorCode = exception.getErrorCode();

        log.warn(
                "BusinessException: code={}, message={}",
                errorCode.getCode(),
                exception.getMessage()
        );

        return createErrorResponse(errorCode);
    }

    @ExceptionHandler({
            MethodArgumentNotValidException.class,
            HandlerMethodValidationException.class
    })
    public ResponseEntity<ApiResponse<ErrorResponse>> handleValidationException(
            Exception exception
    ) {
        log.warn(
                "ValidationException: type={}, message={}",
                exception.getClass().getSimpleName(),
                exception.getMessage()
        );

        return createErrorResponse(ErrorCode.INVALID_REQUEST);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<ErrorResponse>> handleMessageNotReadableException(
            HttpMessageNotReadableException exception
    ) {
        log.warn(
                "HttpMessageNotReadableException: message={}",
                exception.getMessage()
        );

        return createErrorResponse(ErrorCode.INVALID_REQUEST);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiResponse<ErrorResponse>> handleNotFound(
            NoResourceFoundException exception
    ) {
        log.warn(
                "NoResourceFoundException: message={}",
                exception.getMessage()
        );

        return createErrorResponse(ErrorCode.RESOURCE_NOT_FOUND);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<ErrorResponse>> handleException(
            Exception exception
    ) {
        log.error(
                "Unhandled exception: type={}, message={}",
                exception.getClass().getName(),
                exception.getMessage(),
                exception
        );

        return createErrorResponse(ErrorCode.INTERNAL_SERVER_ERROR);
    }

    private ResponseEntity<ApiResponse<ErrorResponse>> createErrorResponse(
            ErrorCode errorCode
    ) {
        ErrorResponse errorResponse = new ErrorResponse(
                errorCode.getCode(),
                errorCode.getMessage()
        );

        return ResponseEntity
                .status(errorCode.getStatus())
                .body(ApiResponse.failure(errorResponse));
    }
}