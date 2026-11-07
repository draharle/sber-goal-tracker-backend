package ru.goaltracker.kkd.web;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import ru.goaltracker.kkd.dto.ErrorResponse;
import ru.goaltracker.kkd.service.ErrorCode;
import ru.goaltracker.kkd.service.KkdException;

/** Все ошибки API — в одном формате {"error": {"code": "...", "message": "..."}}. */
@RestControllerAdvice
public class ApiExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(KkdException.class)
    public ResponseEntity<ErrorResponse> handle(KkdException e) {
        return error(e.code(), e.getMessage());
    }

    /** Нарушены @Min / @Max на параметрах (limit, offset). */
    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ErrorResponse> handle(HandlerMethodValidationException e) {
        String message = e.getAllErrors().stream()
                .map(MessageSourceResolvable::getDefaultMessage)
                .findFirst().orElse("Некорректные параметры запроса");
        return error(ErrorCode.INVALID_PARAM, message);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handle(ConstraintViolationException e) {
        String message = e.getConstraintViolations().stream()
                .map(ConstraintViolation::getMessage)
                .findFirst().orElse("Некорректные параметры запроса");
        return error(ErrorCode.INVALID_PARAM, message);
    }

    /** Например, limit=abc. */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handle(MethodArgumentTypeMismatchException e) {
        return error(ErrorCode.INVALID_PARAM, "Параметр " + e.getName() + " имеет неверный формат: " + e.getValue());
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handle(MissingServletRequestParameterException e) {
        return error(ErrorCode.INVALID_PARAM, "Не передан параметр " + e.getParameterName());
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> handle(NoResourceFoundException e) {
        return error(ErrorCode.NOT_FOUND, "Адрес /" + e.getResourcePath() + " не найден");
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handle(HttpRequestMethodNotSupportedException e) {
        return error(ErrorCode.METHOD_NOT_ALLOWED, "Метод " + e.getMethod() + " не поддерживается");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handle(Exception e) {
        log.error("Необработанная ошибка", e);
        return error(ErrorCode.INTERNAL_ERROR, "Внутренняя ошибка сервиса");
    }

    private static ResponseEntity<ErrorResponse> error(ErrorCode code, String message) {
        return ResponseEntity.status(code.httpStatus()).body(ErrorResponse.of(code.name(), message));
    }
}
