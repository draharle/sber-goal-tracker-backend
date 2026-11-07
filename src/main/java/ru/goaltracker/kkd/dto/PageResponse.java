package ru.goaltracker.kkd.dto;

import java.util.List;

/** Страница списка: элементы + общее число + параметры пагинации. */
public record PageResponse<T>(List<T> items, int total, int limit, int offset) {
}
