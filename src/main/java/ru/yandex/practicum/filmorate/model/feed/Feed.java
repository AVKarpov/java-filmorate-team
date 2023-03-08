package ru.yandex.practicum.filmorate.model.feed;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;


@Data
@Builder
@AllArgsConstructor
public class Feed {
    private final long timestamp;
    private final long userId;
    private final EventType eventType; // одно из значениий LIKE, REVIEW или FRIEND
    private final OperationType operation; // одно из значениий REMOVE, ADD, UPDATE
    private final long eventId; // primary key
    private final long entityId; // идентификатор сущности, с которой произошло событие
}