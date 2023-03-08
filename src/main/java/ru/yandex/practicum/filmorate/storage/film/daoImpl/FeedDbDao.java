package ru.yandex.practicum.filmorate.storage.film.daoImpl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.filmorate.model.feed.Feed;
import ru.yandex.practicum.filmorate.model.feed.EventType;
import ru.yandex.practicum.filmorate.model.feed.OperationType;
import ru.yandex.practicum.filmorate.storage.film.dao.FeedDao;
import ru.yandex.practicum.filmorate.storage.user.daoImpl.UserDbDao;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component("feedDbDao")
@Slf4j
public class FeedDbDao implements FeedDao {
    private final JdbcTemplate jdbcTemplate;
    private final UserDbDao userDbDao;

    public FeedDbDao(JdbcTemplate jdbcTemplate, UserDbDao userDbDao){
        this.jdbcTemplate = jdbcTemplate;
        this.userDbDao = userDbDao;
    }

    @Override
    public void addFeed(long userId, EventType eventType, OperationType operation, long entityId) {
        SimpleJdbcInsert simpleJdbcInsert = new SimpleJdbcInsert(jdbcTemplate)
                .withTableName("feeds")
                .usingGeneratedKeyColumns("event_id");
        Map<String, Object> values = new HashMap<>();
        values.put("timestamp", Instant.now().toEpochMilli());
        values.put("user_id", userId);
        values.put("event_type", getEventTypeId(eventType));
        values.put("operation", getOperationId(operation));
        values.put("entity_id", entityId);

        long id = simpleJdbcInsert.executeAndReturnKey(values).longValue();
        log.info("В Ленту добавлено событие c id = {}", id);
    }

    @Override
    public List<Feed> getFeed(long userId) {
        //проверяем, что пользователь с userId существует
        userDbDao.getUser(userId);
        //отправляем запрос
        String sqlQuery = "SELECT * FROM feeds WHERE user_id = ?";
        return jdbcTemplate.query(sqlQuery, this::mapRowToFeed, userId);
    }

    private Integer getEventTypeId(EventType eventType) {
        String sqlQuery="SELECT id FROM event_types WHERE event_type = ?";
        return jdbcTemplate.query(sqlQuery, (rs, rowNum) -> rs.getInt("id"),
                eventType.toString()).stream().findFirst().orElse(null);
    }

    private EventType getEventType(int eventTypeId) {
        String sqlQuery="SELECT event_type FROM event_types WHERE id = ?";
        return EventType.valueOf(jdbcTemplate.query(sqlQuery, (rs, rowNum) -> rs.getString("event_type"),
                eventTypeId).stream().findFirst().orElse(null));
    }

    private Integer getOperationId(OperationType operation) {
        String sqlQuery="SELECT id FROM operations WHERE operation = ?";
        return jdbcTemplate.query(sqlQuery, (rs, rowNum) -> rs.getInt("id"),
                operation.toString()).stream().findFirst().orElse(null);
    }

    private OperationType getOperationType(int operationTypeId) {
        String sqlQuery="SELECT operation FROM operations WHERE id = ?";
        return OperationType.valueOf(jdbcTemplate.query(sqlQuery, (rs, rowNum) -> rs.getString("operation"),
                operationTypeId).stream().findFirst().orElse(null));
    }

    private Feed mapRowToFeed(ResultSet resultSet, int rowNum) throws SQLException {
        return Feed.builder()
                .timestamp(resultSet.getLong("timestamp"))
                .userId(resultSet.getLong("user_id"))
                .eventType(getEventType(resultSet.getInt("event_type")))
                .operation(getOperationType(resultSet.getInt("operation")))
                .eventId(resultSet.getLong("event_id"))
                .entityId(resultSet.getLong("entity_id"))
                .build();
    }
}