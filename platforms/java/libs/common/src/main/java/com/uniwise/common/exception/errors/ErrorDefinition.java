package com.uniwise.common.exception.errors;

import org.springframework.http.HttpStatus;

public interface ErrorDefinition {
    String getCode();
    String getMessage();
    HttpStatus getHttpStatus();
}
