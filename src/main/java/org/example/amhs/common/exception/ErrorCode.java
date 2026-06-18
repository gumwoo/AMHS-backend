package org.example.amhs.common.exception;

import org.springframework.http.HttpStatus;

public enum ErrorCode {
    INVALID_REQUEST(HttpStatus.BAD_REQUEST, "요청 형식이 올바르지 않습니다."),
    VALIDATION_ERROR(HttpStatus.BAD_REQUEST, "요청 값 검증에 실패했습니다."),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "인증이 필요합니다."),
    FORBIDDEN(HttpStatus.FORBIDDEN, "접근 권한이 없습니다."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 내부 오류가 발생했습니다."),

    TRANSFER_REQUEST_NOT_FOUND(HttpStatus.NOT_FOUND, "반송 요청을 찾을 수 없습니다."),
    OHT_NOT_FOUND(HttpStatus.NOT_FOUND, "OHT를 찾을 수 없습니다."),
    FAB_NODE_NOT_FOUND(HttpStatus.NOT_FOUND, "FAB 노드를 찾을 수 없습니다."),
    FAB_EDGE_NOT_FOUND(HttpStatus.NOT_FOUND, "FAB 엣지를 찾을 수 없습니다."),
    INVALID_TRANSFER_STATUS(HttpStatus.CONFLICT, "반송 요청 상태가 올바르지 않습니다."),
    INVALID_OHT_STATUS(HttpStatus.CONFLICT, "OHT 상태가 올바르지 않습니다."),
    INVALID_STATE_TRANSITION(HttpStatus.CONFLICT, "허용되지 않는 상태 전이입니다."),
    AVAILABLE_OHT_NOT_FOUND(HttpStatus.CONFLICT, "배정 가능한 OHT가 없습니다."),
    OHT_ALREADY_ASSIGNED(HttpStatus.CONFLICT, "OHT가 이미 다른 요청에 배정되어 있습니다."),
    ROUTE_NOT_FOUND(HttpStatus.CONFLICT, "이동 가능한 경로를 찾을 수 없습니다."),
    EDGE_ALREADY_BLOCKED(HttpStatus.CONFLICT, "이미 차단된 엣지입니다."),
    EDGE_ALREADY_UNBLOCKED(HttpStatus.CONFLICT, "이미 차단 해제된 엣지입니다."),
    SIMULATION_ALREADY_RUNNING(HttpStatus.CONFLICT, "시뮬레이션이 이미 실행 중입니다."),
    SIMULATION_NOT_RUNNING(HttpStatus.CONFLICT, "시뮬레이션이 실행 중이 아닙니다.");

    private final HttpStatus httpStatus;
    private final String message;

    ErrorCode(HttpStatus httpStatus, String message) {
        this.httpStatus = httpStatus;
        this.message = message;
    }

    public HttpStatus httpStatus() {
        return httpStatus;
    }

    public String message() {
        return message;
    }
}
