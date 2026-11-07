package ru.goaltracker.kkd.service;

/** Коды ошибок API (как в эталонном mock API) и HTTP-статус для каждого. */
public enum ErrorCode {
    RUN_NOT_FOUND(404),
    UNIT_NOT_FOUND(404),
    NOT_FOUND(404),
    METHOD_NOT_ALLOWED(405),
    RUN_FAILED(409),
    RUN_IN_PROGRESS(409),
    INVALID_PARAM(422),
    INTERNAL_ERROR(500);

    private final int httpStatus;

    ErrorCode(int httpStatus) {
        this.httpStatus = httpStatus;
    }

    public int httpStatus() {
        return httpStatus;
    }
}
