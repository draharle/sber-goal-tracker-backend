package ru.goaltracker.kkd.service;

/** Ожидаемая ошибка бизнес-логики. Сервис не знает про HTTP — статус берётся из ErrorCode в обработчике. */
public class KkdException extends RuntimeException {

    private final ErrorCode code;

    public KkdException(ErrorCode code, String message) {
        super(message);
        this.code = code;
    }

    public ErrorCode code() {
        return code;
    }

    public static KkdException invalidParam(String message) {
        return new KkdException(ErrorCode.INVALID_PARAM, message);
    }
}
