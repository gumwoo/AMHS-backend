package org.example.amhs.common.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import org.example.amhs.common.response.ErrorBody;
import org.example.amhs.common.response.ErrorResponse;
import org.example.amhs.common.response.ValidationError;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

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
        List<ValidationError> fieldErrors = exception.getBindingResult().getFieldErrors().stream()
                .map(this::toValidationError)
                .toList();
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("fieldErrors", fieldErrors);
        return buildResponse(ErrorCode.VALIDATION_ERROR, details, request);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    ResponseEntity<ErrorResponse> handleConstraintViolation(
            ConstraintViolationException exception,
            HttpServletRequest request
    ) {
        List<ValidationError> fieldErrors = exception.getConstraintViolations().stream()
                .map(violation -> new ValidationError(
                        violation.getPropertyPath().toString(),
                        violation.getMessage(),
                        violation.getInvalidValue()
                ))
                .toList();
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("fieldErrors", fieldErrors);
        return buildResponse(ErrorCode.VALIDATION_ERROR, details, request);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<ErrorResponse> handleHttpMessageNotReadable(
            HttpMessageNotReadableException exception,
            HttpServletRequest request
    ) {
        Map<String, Object> details = Map.of("reason", "요청 본문을 읽을 수 없습니다.");
        return buildResponse(ErrorCode.INVALID_REQUEST, details, request);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    ResponseEntity<ErrorResponse> handleMethodArgumentTypeMismatch(
            MethodArgumentTypeMismatchException exception,
            HttpServletRequest request
    ) {
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("parameter", exception.getName());
        details.put("value", exception.getValue());
        if (exception.getRequiredType() != null) {
            details.put("requiredType", exception.getRequiredType().getSimpleName());
        }
        return buildResponse(ErrorCode.INVALID_REQUEST, details, request);
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

    private ValidationError toValidationError(FieldError fieldError) {
        return new ValidationError(
                fieldError.getField(),
                fieldError.getDefaultMessage(),
                fieldError.getRejectedValue()
        );
    }
}
