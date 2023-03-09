package ru.yandex.practicum.filmorate.storage.film.daoImpl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.filmorate.exceptions.film.FilmNotFoundException;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.feed.EventType;
import ru.yandex.practicum.filmorate.model.feed.OperationType;
import ru.yandex.practicum.filmorate.storage.film.dao.FilmLikeDao;

import java.util.List;

@Component("filmLikeDbDao")
@Primary
@Slf4j
public class FilmLikeDbDao implements FilmLikeDao {

    private final JdbcTemplate jdbcTemplate;
    private final FilmDbDao filmDbDao;
    private final FeedDbDao feedDbDao;

    public FilmLikeDbDao(JdbcTemplate jdbcTemplate,
                        @Qualifier("filmDbStorage") FilmDbDao filmDbDao,
                        @Qualifier("feedDbDao") FeedDbDao feedDbDao) {
        this.jdbcTemplate = jdbcTemplate;
        this.filmDbDao = filmDbDao;
        this.feedDbDao = feedDbDao;
    }

    //добавить лайки фильмам в таблицу films_like
    @Override
    public void addLike(long filmId, long userId) {
        String updateSql = "UPDATE films_like SET film_id = ?, user_id = ? WHERE film_id = ? AND user_id = ?";
        int updateRow=jdbcTemplate.update(updateSql, filmId, userId, filmId, userId);
        if (updateRow == 0) {
            String addSql = "INSERT INTO films_like (film_id, user_id) VALUES(?,?)";
            int addRow = jdbcTemplate.update(addSql, filmId, userId);
            if (addRow <= 0) {
                log.debug("Ошибка добавления для фильма с id={} лайка от пользователя с id={}.", filmId, userId);
                throw new FilmNotFoundException("Фильм с id=" + filmId + " или пользователь с id=" + userId + " не найден.");
            }
        }
        feedDbDao.addFeed(userId, EventType.LIKE, OperationType.ADD, filmId);
        log.debug("Для фильма с id={} добавлен лайк пользователем с id={}.", filmId, userId);
    }

    //удалить лайки фильмам из таблицы films_like
    @Override
    public void deleteLike(long filmId, long userId) {
        try{
            String delSql = "DELETE FROM films_like WHERE film_id = ? AND user_id = ?";
            int delRow=jdbcTemplate.update(delSql, filmId, userId);
            if (delRow <= 0) {
                log.debug("Ошибка удаления для фильма с id={} лайка от пользователя с id={}.", filmId, userId);
                throw new FilmNotFoundException("Фильм с id=" + filmId + " или пользователь с id=" + userId + " не найден.");
            }
        } catch (RuntimeException e) {
            log.debug("Возникло исключение: фильм или пользователь не найдены.");
            throw new FilmNotFoundException("Фильм с id="+filmId+" или пользователь с id="+userId+" не найден.");
        }
        feedDbDao.addFeed(userId, EventType.LIKE, OperationType.REMOVE, filmId);
        log.debug("Для фильма с id={} удалён лайк пользователем с id={}.", filmId, userId);
    }

    @Override
    public List<Film> getRecomendationFilm(long userId) {
        String getSql = "SELECT a.film_id " +
                "FROM (SELECT DISTINCT fl.film_id " +
                "FROM (" +
                "SELECT gcf.user_id " +
                "FROM (SELECT ccf.user_id, " +
                "ccf.count_films, " +
                "RANK() OVER(ORDER BY count_films desc) AS group_num " +
                "FROM (" +
                "SELECT fl.USER_ID, " +
                "count(*) AS count_films " +
                "FROM (SELECT FILM_ID FROM films_like WHERE USER_ID = ? ) AS us " +
                "INNER JOIN " +
                "films_like AS FL ON us.film_id = fl.film_id " +
                "WHERE fl.user_id <> ? " +
                "GROUP BY fl.user_id) ccf " +
                ") gcf " +
                "WHERE gcf.group_num = 1 " +
                ") ou " +
                "LEFT JOIN films_like fl ON ou.user_id = fl.user_id " +
                "LEFT JOIN (SELECT film_id FROM films_like WHERE user_id = ? ) us " +
                "ON fl.film_id = us.film_id " +
                "WHERE us.film_id IS NULL) a " +
                "LEFT JOIN films_like fl ON a.film_id = fl.film_id " +
                "GROUP BY fl.film_id ORDER BY COUNT(fl.film_id) DESC";
        return filmDbDao.getFilms(jdbcTemplate.query(getSql, (rs, rowNum) -> rs.getLong("film_id"),
                userId, userId, userId));
    }
}
