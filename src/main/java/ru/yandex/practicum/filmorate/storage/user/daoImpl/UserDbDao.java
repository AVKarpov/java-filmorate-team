package ru.yandex.practicum.filmorate.storage.user.daoImpl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.filmorate.exceptions.user.UserNotFoundException;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.storage.user.dao.UserDao;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component
@Qualifier("userDbDao")
@Primary
@Slf4j
public class UserDbDao implements UserDao {
    private final JdbcTemplate jdbcTemplate;

    public UserDbDao(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public User addUser(User user) {
        //добавить информацию о пользователе в таблицу users
        if(user.getName() == null || user.getName().isBlank()) {
            user.setName(user.getLogin());
        }
        String addUserSql = "INSERT INTO users(email, login, name, birthday) VALUES (?, ?, ?, ?)";
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(
                connection -> {
                    PreparedStatement ps =
                            connection.prepareStatement(addUserSql, new String[]{"user_id"});
                    ps.setString(1,user.getEmail());
                    ps.setString(2,user.getLogin());
                    ps.setString(3,user.getName());
                    ps.setString(4,user.getBirthday().toString());
                    return ps;
                },
                keyHolder);
        long userId = keyHolder.getKey().intValue();
        user.setId(userId);
        log.debug("Добавлен новый пользователь с id={}", userId);
        return user;
    }

    @Override
    public User updateUser(User user) {
        if(user.getName() == null || user.getName().isBlank()) {
            user.setName(user.getLogin());
        }
        log.debug("Формируем sql запрос...");
        String updateUserSql = "UPDATE users SET email = ?, login = ?, name = ?, birthday = ? WHERE user_id = ?";
        int updateRow=jdbcTemplate.update(updateUserSql, user.getEmail(), user.getLogin(),
                user.getName(), user.getBirthday(), user.getId());
        if (updateRow <= 0) {
            log.debug("Пользователь с id={} для обновления не найден.", user.getId());
            throw new UserNotFoundException("Пользователь с id=" + user.getId() + " не найден.");
        }
        log.debug("Пользователь с id={} обновлён.", user.getId());
        return user;
    }

    @Override
    public Set<User> getUsers() {
        log.debug("Получен запрос на чтение всех пользователей");
        String getUserSql = "SELECT * FROM users";
        List<User> users = jdbcTemplate.query(getUserSql, (rs, rowNum) -> userMapper(rs));
        if (users == null) {
            log.debug("Пользователи не найдены.");
            throw new UserNotFoundException("Пользователи не найдены.");
        }
        log.debug("Найдено пользователей: {} шт.", users.size());
        return new HashSet<>(users);
    }

    @Override
    public User getUser(long userId) {
        log.debug("Получен запрос на пользователя с id={};", userId);
        String getFilmSql = "SELECT * FROM users WHERE user_id = ?";
        User user = jdbcTemplate.query(getFilmSql, (rs, rowNum) -> userMapper(rs), userId)
                .stream().findAny().orElseThrow(() -> new UserNotFoundException("Пользователи не найдены."));
        log.debug("С id={} возвращён пользователь: {}", userId, user.getName());
        return user;
    }

    @Override
    public void deleteUser(long userId) {
        log.debug("Получен запрос на удаление пользователя с id={}", userId);
        String deleteUserSql = "DELETE FROM users WHERE user_id = ?";
        int delRow = jdbcTemplate.update(deleteUserSql, userId);
        if (delRow <= 0) {
            log.debug("Пользователь с id={} для удаления не найден.", userId);
            throw new UserNotFoundException("Пользователь с id=" + userId + " для удаления не найден.");
        }
        log.debug("Пользователь с id={} удалён.", userId);
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
