package ru.yandex.practicum.filmorate;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.jdbc.Sql;
import ru.yandex.practicum.filmorate.exceptions.film.FilmNotFoundException;
import ru.yandex.practicum.filmorate.model.feed.EventType;
import ru.yandex.practicum.filmorate.model.feed.Feed;
import ru.yandex.practicum.filmorate.model.feed.OperationType;
import ru.yandex.practicum.filmorate.service.FilmService;
import ru.yandex.practicum.filmorate.service.UserService;
import ru.yandex.practicum.filmorate.storage.film.dao.FeedDao;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@AutoConfigureTestDatabase
@RequiredArgsConstructor(onConstructor_ = @Autowired)
@Sql({"/data.sql"})
public class FeedTests {
    private final FeedDao feedDbDao;
    private final UserService userService;
    private final FilmService filmService;

    @Test
    public void shouldReturnFeedAddLikeToFilm1FromUser1() {
        filmService.addLike(1,1);
        Feed expectedFeed = Feed.builder()
                .userId(1)
                .entityId(1)
                .eventType(EventType.LIKE)
                .operation(OperationType.ADD)
                .build();
        List<Feed> actualFeed = feedDbDao.getFeed(1);
        assertEquals(expectedFeed.getUserId(), actualFeed.get(0).getUserId(), "userId не соответствует");
        assertEquals(expectedFeed.getEntityId(), actualFeed.get(0).getEntityId(), "Фильм не соответствует");
        assertEquals(expectedFeed.getEventType(), actualFeed.get(0).getEventType(), "Событие не соответствует");
        assertEquals(expectedFeed.getOperation(), actualFeed.get(0).getOperation(), "Операция не соответствует");
    }

    @Test
    public void shouldReturn404AddLikeToFilm999FromUser1() {
        assertThrows(FilmNotFoundException.class, () -> filmService.addLike(999,1));
        assertEquals(0, feedDbDao.getFeed(1).size(), "Количество записей в ленте не совпадает");
    }

    @Test
    public void shouldReturnFeedOnRemoveLikeFromFilm1ByUser1() {
        filmService.deleteLike(1,2);
        Feed expectedFeed = Feed.builder()
                .userId(2)
                .entityId(1)
                .eventType(EventType.LIKE)
                .operation(OperationType.REMOVE)
                .build();
        List<Feed> actualFeed = feedDbDao.getFeed(2);
        assertEquals(expectedFeed.getUserId(), actualFeed.get(0).getUserId(), "userId не соответствует");
        assertEquals(expectedFeed.getEntityId(), actualFeed.get(0).getEntityId(), "Фильм не соответствует");
        assertEquals(expectedFeed.getEventType(), actualFeed.get(0).getEventType(), "Событие не соответствует");
        assertEquals(expectedFeed.getOperation(), actualFeed.get(0).getOperation(), "Операция не соответствует");
    }

    @Test
    public void shouldReturnFeedOnUser2AddFriendUser3() {
        userService.addFriend(2,3);

        Feed expectedFeed = Feed.builder()
                .userId(2)
                .entityId(3)
                .eventType(EventType.FRIEND)
                .operation(OperationType.ADD)
                .build();
        List<Feed> actualFeed = feedDbDao.getFeed(2);
        assertEquals(expectedFeed.getUserId(), actualFeed.get(0).getUserId(), "userId не соответствует");
        assertEquals(expectedFeed.getEntityId(), actualFeed.get(0).getEntityId(), "id друга не соответствует");
        assertEquals(expectedFeed.getEventType(), actualFeed.get(0).getEventType(), "Событие не соответствует");
        assertEquals(expectedFeed.getOperation(), actualFeed.get(0).getOperation(), "Операция не соответствует");
    }

    @Test
    public void shouldReturnFeedOnUser1RemoveFriendUser2() {
        userService.deleteFriend(1,2);

        Feed expectedFeed = Feed.builder()
                .userId(1)
                .entityId(2)
                .eventType(EventType.FRIEND)
                .operation(OperationType.REMOVE)
                .build();
        List<Feed> actualFeed = feedDbDao.getFeed(1);
        assertEquals(expectedFeed.getUserId(), actualFeed.get(0).getUserId(), "userId не соответствует");
        assertEquals(expectedFeed.getEntityId(), actualFeed.get(0).getEntityId(), "id друга не соответствует");
        assertEquals(expectedFeed.getEventType(), actualFeed.get(0).getEventType(), "Событие не соответствует");
        assertEquals(expectedFeed.getOperation(), actualFeed.get(0).getOperation(), "Операция не соответствует");
    }

    @Test
    public void shouldReturn4Feeds() {
        filmService.addLike(1, 1);
        filmService.addLike(3, 1);
        userService.addFriend(1, 4);
        userService.addFriend(1, 5);

        int expectedFeedSize = 4;
        int actualFeedSize = feedDbDao.getFeed(1).size();
        assertEquals(expectedFeedSize, actualFeedSize, "Количество записей в ленте не соответствует");
    }
}
