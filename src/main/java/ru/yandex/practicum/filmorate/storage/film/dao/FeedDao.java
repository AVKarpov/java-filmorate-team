package ru.yandex.practicum.filmorate.storage.film.dao;

import ru.yandex.practicum.filmorate.model.feed.Feed;
import ru.yandex.practicum.filmorate.model.feed.EventType;
import ru.yandex.practicum.filmorate.model.feed.OperationType;

import java.util.List;

public interface FeedDao {
    void addFeed(long userId, EventType eventType, OperationType operation, long entityId);
    List<Feed> getFeed(long userId);
}