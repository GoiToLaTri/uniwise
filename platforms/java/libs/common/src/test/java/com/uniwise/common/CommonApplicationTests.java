package com.uniwise.common;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.server.ResponseStatusException;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.uniwise.common.dto.response.ErrorResponse;
import com.uniwise.common.exception.GlobalExceptionHandler;
import com.uniwise.common.exception.HttpException;
import com.uniwise.common.exception.errors.AuthError;
import com.uniwise.common.exception.errors.GatewayError;
import com.uniwise.common.exception.errors.ServerError;
import com.uniwise.common.exception.errors.ValidationError;
import com.uniwise.common.exception.flux.GatewayExceptionHandler;

class CommonApplicationTests {

    private final GlobalExceptionHandler globalExceptionHandler = new GlobalExceptionHandler();

    @SuppressWarnings("unused")
    private void dummyValidationTarget(Object payload) {
    }

    private MethodParameter dummyMethodParameter() throws NoSuchMethodException {
        Method method = CommonApplicationTests.class.getDeclaredMethod("dummyValidationTarget", Object.class);
        return new MethodParameter(method, 0);
    }

    private ObjectMapper createObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.findAndRegisterModules();
        if (mapper.getRegisteredModuleIds().isEmpty()) {
            SimpleModule instantModule = new SimpleModule();
            instantModule.addSerializer(Instant.class, ToStringSerializer.instance);
            mapper.registerModule(instantModule);
        }
        return mapper;
    }

    @Test
    void httpExceptionPopulatesMessageFromErrorDefinition() {
        HttpException exception = new HttpException(AuthError.ACCESS_DENIED);

        assertEquals(AuthError.ACCESS_DENIED, exception.getError());
        assertEquals(AuthError.ACCESS_DENIED.getMessage(), exception.getMessage());

        HttpException nullErrorException = new HttpException(null);
        assertNull(nullErrorException.getError());
        assertNull(nullErrorException.getMessage());
    }

    @Test
    void globalExceptionHandlerMapsHttpException() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/sessions/1");

        ResponseEntity<ErrorResponse> response = globalExceptionHandler.runtimeExceptionHandling(
                new HttpException(AuthError.SESSION_NOT_FOUND), request);

        assertEquals(HttpStatus.NOT_FOUND.value(), response.getStatusCode().value());
        ErrorResponse body = response.getBody();
        assertNotNull(body);
        assertEquals(HttpStatus.NOT_FOUND.value(), body.getStatus());
        assertEquals(AuthError.SESSION_NOT_FOUND.getCode(), body.getCode());
        assertEquals(AuthError.SESSION_NOT_FOUND.getMessage(), body.getDetail());
        assertEquals("/api/v1/sessions/1", body.getPath());
        assertNotNull(body.getTimestamp());
    }

    @Test
    void globalExceptionHandlerHandlesClassLevelValidationWithoutFieldError() throws NoSuchMethodException {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/register");
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "registerRequest");
        bindingResult.addError(new ObjectError("registerRequest", ValidationError.EMAIL_INVALID.name()));
        MethodArgumentNotValidException exception = new MethodArgumentNotValidException(
                dummyMethodParameter(), bindingResult);

        ResponseEntity<ErrorResponse> response = globalExceptionHandler.validationExceptionHandling(exception, request);

        assertEquals(HttpStatus.BAD_REQUEST.value(), response.getStatusCode().value());
        ErrorResponse body = response.getBody();
        assertNotNull(body);
        assertEquals(ValidationError.EMAIL_INVALID.getCode(), body.getCode());
        assertEquals(ValidationError.EMAIL_INVALID.getMessage(), body.getDetail());
        assertEquals("/api/v1/register", body.getPath());
    }

    @Test
    void globalExceptionHandlerValidationFallsBackToKeyInvalidWhenEmptyOrUnknown() throws NoSuchMethodException {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/items");

        BeanPropertyBindingResult emptyBindingResult = new BeanPropertyBindingResult(new Object(), "itemRequest");
        MethodArgumentNotValidException emptyException = new MethodArgumentNotValidException(
                dummyMethodParameter(), emptyBindingResult);
        ResponseEntity<ErrorResponse> emptyResponse = globalExceptionHandler.validationExceptionHandling(
                emptyException, request);

        assertEquals(HttpStatus.BAD_REQUEST.value(), emptyResponse.getStatusCode().value());
        assertNotNull(emptyResponse.getBody());
        assertEquals(ValidationError.KEY_INVALID.getCode(), emptyResponse.getBody().getCode());

        BeanPropertyBindingResult unknownKeyResult = new BeanPropertyBindingResult(new Object(), "itemRequest");
        unknownKeyResult.addError(new FieldError("itemRequest", "name", "UNKNOWN_VALIDATION_KEY"));
        MethodArgumentNotValidException unknownKeyException = new MethodArgumentNotValidException(
                dummyMethodParameter(), unknownKeyResult);
        ResponseEntity<ErrorResponse> unknownKeyResponse = globalExceptionHandler.validationExceptionHandling(
                unknownKeyException, request);

        assertNotNull(unknownKeyResponse.getBody());
        assertEquals(ValidationError.KEY_INVALID.getCode(), unknownKeyResponse.getBody().getCode());

        BeanPropertyBindingResult knownFieldResult = new BeanPropertyBindingResult(new Object(), "itemRequest");
        knownFieldResult.addError(new FieldError("itemRequest", "name", ValidationError.NAME_REQUIRED.name()));
        MethodArgumentNotValidException knownFieldException = new MethodArgumentNotValidException(
                dummyMethodParameter(), knownFieldResult);
        ResponseEntity<ErrorResponse> knownFieldResponse = globalExceptionHandler.validationExceptionHandling(
                knownFieldException, request);

        assertNotNull(knownFieldResponse.getBody());
        assertEquals(ValidationError.NAME_REQUIRED.getCode(), knownFieldResponse.getBody().getCode());

        BeanPropertyBindingResult nullFieldMsgResult = new BeanPropertyBindingResult(new Object(), "itemRequest");
        nullFieldMsgResult.addError(new FieldError("itemRequest", "name", null, false, null, null, null));
        nullFieldMsgResult.addError(new ObjectError("itemRequest", ValidationError.EMAIL_INVALID.name()));
        MethodArgumentNotValidException nullFieldMsgException = new MethodArgumentNotValidException(
                dummyMethodParameter(), nullFieldMsgResult);
        ResponseEntity<ErrorResponse> nullFieldMsgResponse = globalExceptionHandler.validationExceptionHandling(
                nullFieldMsgException, request);

        assertNotNull(nullFieldMsgResponse.getBody());
        assertEquals(ValidationError.EMAIL_INVALID.getCode(), nullFieldMsgResponse.getBody().getCode());
    }

    @Test
    void globalExceptionHandlerHandlesUnhandledException() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/crash");

        ResponseEntity<ErrorResponse> response = globalExceptionHandler.exceptionHandling(
                new IllegalStateException("boom"), request);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.value(), response.getStatusCode().value());
        ErrorResponse body = response.getBody();
        assertNotNull(body);
        assertEquals(ServerError.SERVER_ERROR.getCode(), body.getCode());
        assertEquals(ServerError.SERVER_ERROR.getMessage(), body.getDetail());
        assertEquals("/api/v1/crash", body.getPath());
    }

    @Test
    void gatewayExceptionHandlerUsesOwnTypeInConditionalOnMissingBean() {
        ConditionalOnMissingBean annotation = GatewayExceptionHandler.class
                .getAnnotation(ConditionalOnMissingBean.class);

        assertNotNull(annotation);
        assertArrayEquals(new Class<?>[] { GatewayExceptionHandler.class }, annotation.value());
    }

    @Test
    void gatewayExceptionHandlerWritesActualStatusInJsonForResponseStatusException() throws Exception {
        ObjectMapper objectMapper = createObjectMapper();
        GatewayExceptionHandler handler = new GatewayExceptionHandler(objectMapper);
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/v1/does-not-exist").build());

        handler.handle(exchange, new ResponseStatusException(HttpStatus.NOT_FOUND, "No matching route")).block();

        assertEquals(HttpStatus.NOT_FOUND, exchange.getResponse().getStatusCode());
        assertEquals(MediaType.APPLICATION_JSON, exchange.getResponse().getHeaders().getContentType());

        String responseBody = exchange.getResponse().getBodyAsString().block();
        assertNotNull(responseBody);
        assertTrue(!responseBody.isBlank());

        JsonNode json = objectMapper.readTree(responseBody);
        assertEquals(HttpStatus.NOT_FOUND.value(), json.get("status").asInt());
        assertEquals(GatewayError.GATEWAY_PROCESSING_ERROR.getCode(), json.get("code").asText());
        assertEquals("No matching route", json.get("detail").asText());
        assertEquals("/api/v1/does-not-exist", json.get("path").asText());
    }

    @Test
    void gatewayExceptionHandlerMapsConnectionRefusedToServiceUnavailable() throws Exception {
        ObjectMapper objectMapper = createObjectMapper();
        GatewayExceptionHandler handler = new GatewayExceptionHandler(objectMapper);
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/v1/courses").build());

        handler.handle(exchange, new RuntimeException("Connection refused: localhost/127.0.0.1:8083")).block();

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, exchange.getResponse().getStatusCode());
        String responseBody = exchange.getResponse().getBodyAsString().block();
        assertNotNull(responseBody);

        JsonNode json = objectMapper.readTree(responseBody);
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE.value(), json.get("status").asInt());
        assertEquals(GatewayError.SERVICE_UNAVAILABLE.getCode(), json.get("code").asText());
        assertEquals(GatewayError.SERVICE_UNAVAILABLE.getMessage(), json.get("detail").asText());
        assertEquals("/api/v1/courses", json.get("path").asText());
    }
}
