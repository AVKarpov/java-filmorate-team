package ru.yandex.practicum.filmorate.storage.user.daoImpl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.rowset.SqlRowSet;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.model.feed.EventType;
import ru.yandex.practicum.filmorate.model.feed.OperationType;
import ru.yandex.practicum.filmorate.storage.film.daoImpl.FeedDbDao;
import ru.yandex.practicum.filmorate.storage.user.dao.FriendsDao;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

@Component
@Qualifier("friendsDbDao")
@Primary
@Slf4j
public class FriendsDbDao implements FriendsDao {

    private final JdbcTemplate jdbcTemplate;
    private final FeedDbDao feedDbDao;

    public FriendsDbDao(JdbcTemplate jdbcTemplate, FeedDbDao feedDbDao) {
        this.jdbcTemplate = jdbcTemplate;
        this.feedDbDao = feedDbDao;
    }

    @Override
    public void addFriend(long userId, long friendId) {
        //считываем из таблицы friends
        String friendsSql = "SELECT user_id, friend_id, friend_status FROM friends WHERE (user_id = ? AND friend_id = ?) " +
                "OR (user_id = ? AND friend_id = ?)";
        SqlRowSet friendsRows = jdbcTemplate.queryForRowSet(friendsSql, userId, friendId, friendId, userId);
        if (friendsRows.first()) {
            int user1 = friendsRows.getInt("user_id");
            boolean status = friendsRows.getBoolean("friend_status");
            //если найдена запись, то смотрим на friend_status, если он true,то пользователи дружат между собой
            //тогда просто выходим
            //если friend_status=false, то user_id дружит с friend_id, но не наоборот
            //если user_id=userId, то просто выходим
            //если user_id=friendId и friend_status=true, то выходим
            if (status || user1 == userId) {
                return;
            } else if (user1 == friendId & !status) {
                //если user_id=friendId и friend_status=false, то отправляем запрос на friend_status=true (подтверждаем дружбу)
                String friendSqlTrue = "UPDATE friends SET user_id = ?, friend_id = ?, friend_status = TRUE";
                jdbcTemplate.update(friendSqlTrue, friendId, userId);
                feedDbDao.addFeed(userId, EventType.FRIEND, OperationType.ADD, friendId);
            }
        } else {
            //если запись не найдена, то добавляем её
            String addFriendSql = "INSERT INTO friends (user_id, friend_id) VALUES (?, ?)";
            jdbcTemplate.update(addFriendSql, userId, friendId);
            feedDbDao.addFeed(userId, EventType.FRIEND, OperationType.ADD, friendId);
        }
    }

    @Override
    public void deleteFriend(long userId, long friendId) {
        //userId удаляет из друзей friendId
        //возможно в след. случаях:
        //существует запись: userId, friendId, false - неподтверждённая дружба - просто удаляем
        //существует запись: friendId, userId, false - неподтверждённая дружба - ничего не делаем
        //существует запись: userId, friendId, true - подтверждённая дружба - удаляем и записываем friendId, userId, false
        //существует запись: friendId, userId, true - подтверждённая дружба - удаляем и записываем friendId, userId, false
        //если запись не найдена, то ничего не делаем.
        //считываем из таблицы friends
        String friendsSql = "SELECT user_id, friend_id, friend_status FROM friends WHERE (user_id = ? AND friend_id = ?) " +
                "OR (user_id = ? AND friend_id = ?)";
        SqlRowSet friendsRows = jdbcTemplate.queryForRowSet(friendsSql, userId, friendId, friendId, userId);
        if (friendsRows.first()) {
            log.debug("Получен непустой ответ на удаление от сервера...");
            boolean status = friendsRows.getBoolean("friend_status");
            log.debug("Статус дружбы: обоюдная...");
            String delFriendSql = "DELETE FROM friends WHERE (user_id = ? AND friend_id = ?) OR (user_id = ? AND friend_id = ?)";
            jdbcTemplate.update(delFriendSql, userId, friendId, friendId, userId);
            feedDbDao.addFeed(userId, EventType.FRIEND, OperationType.REMOVE, friendId);
            if (status) {
                String addFriendSql = "INSERT INTO friends (user_id, friend_id) VALUES (?, ?)";
                jdbcTemplate.update(addFriendSql, friendId, userId);
            }
        }
    }

    @Override
    public List<User> getFriends(long userId) {
        //возвращаем друзей пользователя userId
        //посмотреть, возможно переписать
        String getFriendsSql = "SELECT u2.* FROM users u2 LEFT JOIN (SELECT DISTINCT CASE WHEN "+
                "(f.friend_id = ? AND f.friend_status) THEN f.user_id ELSE f.friend_id END AS friend_id " +
                "FROM friends f WHERE f.user_id = ? OR (f.friend_id = ? AND f.friend_status)) fr " +
                "ON u2.user_id = fr.friend_id WHERE fr.friend_id IS NOT NULL";
        List<User> users = jdbcTemplate.query(getFriendsSql, (rs, rowNum) ->userMapper(rs), userId, userId, userId);
        log.debug("Количество друзей пользователя с id={}: {}", userId, users.size());
        return users;
    }

    @Override
    public List<User> getCommonFriends(long userId, long otherId) {
        String commonFriendSql="SELECT * FROM users WHERE user_id IN (SELECT f1.friend FROM " +
                "(SELECT DISTINCT CASE WHEN (u.user_id = f.friend_id AND " +
                "f.friend_status) THEN f.user_id ELSE f.friend_id END AS friend FROM " +
                "(SELECT * FROM users WHERE user_id = ? ) u LEFT JOIN " +
                "FRIENDS f ON u.user_id = f.user_id OR (u.user_id = f.friend_id AND f.friend_status) " +
                ") f1 INNER JOIN (SELECT DISTINCT CASE WHEN (u.user_id = f.friend_id AND " +
                "f.friend_status) THEN f.user_id ELSE f.friend_id END AS friend FROM " +
                "(SELECT * FROM users WHERE user_id = ? ) u LEFT JOIN friends f " +
                "ON u.user_id = f.user_id OR (u.user_id = f.friend_id AND f.friend_status)) f2 " +
                "ON f1.friend = f2.friend)";
        List<User> users = jdbcTemplate.query(commonFriendSql, (rs, rowNum) ->userMapper(rs), userId, otherId);
        log.debug("Количество общих друзей пользователей с id={},{}: {}", userId, otherId, users.size());
        return users;
    }

    private User userMapper(ResultSet rs) throws SQLException {
        return User.builder()
                .id(rs.getLong("user_id"))
                .email(rs.getString("email"))
                .login(rs.getString("login"))
                .name(rs.getString("name"))
                .birthday(rs.getDate("birthday").toLocalDate())
                .build();
    }
}
