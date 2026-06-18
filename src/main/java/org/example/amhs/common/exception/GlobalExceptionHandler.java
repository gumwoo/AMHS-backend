package org.example.amhs.common.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import java.util.LinkedHashMap;
import java.util.Map;
import org.example.amhs.common.response.ErrorBody;
import org.example.amhs.common.response.ErrorResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    ResponseEntity<ErrorResponse> handleBusinessException(
            BusinessException exception,
            HttpServletRequest request
    ) {
        return buildResponse(exception.errorCode(), exception.details(), request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ErrorResponse> handleMethodArgumentNotValid(
            MethodArgumentNotValidException exception,
            HttpServletRequest request
    ) {
        Map<String, Object> details = new LinkedHashMap<>();
        for (FieldError fieldError : exception.getBindingResult().getFieldErrors()) {
            details.put(fieldError.getField(), fieldError.getDefaultMessage());
        }
        return buildResponse(ErrorCode.VALIDATION_ERROR, details, request);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    ResponseEntity<ErrorResponse> handleConstraintViolation(
            ConstraintViolationException exception,
            HttpServletRequest request
    ) {
        Map<String, Object> details = Map.of("violations", exception.getConstraintViolations().size());
        return buildResponse(ErrorCode.VALIDATION_ERROR, details, request);
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ErrorResponse> handleException(Exception exception, HttpServletRequest request) {
        return buildResponse(ErrorCode.INTERNAL_SERVER_ERROR, Map.of(), request);
    }

    private ResponseEntity<ErrorResponse> buildResponse(
            ErrorCode errorCode,
            Map<String, Object> details,
            HttpServletRequest request
    ) {
        String traceId = (String) request.getAttribute("traceId");
        ErrorBody errorBody = new ErrorBody(errorCode, errorCode.message(), details, traceId);
        return ResponseEntity.status(errorCode.httpStatus()).body(ErrorResponse.of(errorBody));
    }
}
