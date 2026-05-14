package com.uniwise.common.exception;

import com.uniwise.common.exception.errors.ErrorDefinition;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class HttpException extends RuntimeException{
    ErrorDefinition error;
    public HttpException(ErrorDefinition error, Object ...arObjects){
        this.error = error;
    }
}
