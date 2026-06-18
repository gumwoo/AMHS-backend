package org.example.amhs.common.exception;

import org.springframework.http.HttpStatus;

public enum ErrorCode {
    INVALID_REQUEST(HttpStatus.BAD_REQUEST, "Request format is invalid."),
    VALIDATION_ERROR(HttpStatus.BAD_REQUEST, "Request validation failed."),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "Authentication is required."),
    FORBIDDEN(HttpStatus.FORBIDDEN, "Access is forbidden."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error."),

    TRANSFER_REQUEST_NOT_FOUND(HttpStatus.NOT_FOUND, "Transfer request was not found."),
    OHT_NOT_FOUND(HttpStatus.NOT_FOUND, "OHT was not found."),
    FAB_NODE_NOT_FOUND(HttpStatus.NOT_FOUND, "FAB node was not found."),
    FAB_EDGE_NOT_FOUND(HttpStatus.NOT_FOUND, "FAB edge was not found."),
    INVALID_TRANSFER_STATUS(HttpStatus.CONFLICT, "Transfer request status is invalid."),
    INVALID_OHT_STATUS(HttpStatus.CONFLICT, "OHT status is invalid."),
    INVALID_STATE_TRANSITION(HttpStatus.CONFLICT, "State transition is not allowed."),
    AVAILABLE_OHT_NOT_FOUND(HttpStatus.CONFLICT, "Available OHT was not found."),
    OHT_ALREADY_ASSIGNED(HttpStatus.CONFLICT, "OHT is already assigned."),
    ROUTE_NOT_FOUND(HttpStatus.CONFLICT, "Route was not found."),
    EDGE_ALREADY_BLOCKED(HttpStatus.CONFLICT, "Edge is already blocked."),
    EDGE_ALREADY_UNBLOCKED(HttpStatus.CONFLICT, "Edge is already unblocked."),
    SIMULATION_ALREADY_RUNNING(HttpStatus.CONFLICT, "Simulation is already running."),
    SIMULATION_NOT_RUNNING(HttpStatus.CONFLICT, "Simulation is not running.");

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
