package org.example.amhs.common.exception;

import java.util.Map;

public class RouteNotFoundException extends BusinessException {

    public RouteNotFoundException(Map<String, Object> details) {
        super(ErrorCode.ROUTE_NOT_FOUND, details);
    }
}
