package ru.yandex.practicum.filmorate.storage.film.daoImpl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.filmorate.exceptions.genre.GenreNotFoundException;
import ru.yandex.practicum.filmorate.model.Genre;
import ru.yandex.practicum.filmorate.storage.film.dao.GenreDao;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;


@Component("genreDbDao")
@Primary
@Slf4j
public class GenreDbDao implements GenreDao {
    private final JdbcTemplate jdbcTemplate;

    public GenreDbDao(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Genre getGenre(int id) {
        log.debug("Получен запрос на поиск жанра с id={}", id);
        String getGenreSql = "SELECT genre_id, genre_name FROM genre WHERE genre_id = ?";
        Genre genre = jdbcTemplate.query(getGenreSql, (rs, rowNum) -> genreMapper(rs), id)
                .stream().findAny().orElse(null);
        if (genre == null) {
            log.debug("Жанр с id={} не найден.", id);
            throw new GenreNotFoundException("Рейтинг MPA не найдены");
        }
        log.debug("Жанр с id={} найден.", id);
        return genre;
    }

    @Override
    public List<Genre> getGenresFilm(long filmId) {
        log.info("Получен запрос на чтение жанров для фильма с id={}", filmId);
        String getGenreSql = "SELECT g.genre_id, g.genre_name FROM (SELECT * FROM films_genre WHERE film_id = ?) fg " +
                "LEFT JOIN genre g ON fg.genre_id = g.genre_id WHERE g.genre_id IS NOT NULL";
        return jdbcTemplate.query(getGenreSql, (rs, rowNum) -> genreMapper(rs), filmId);
    }

    @Override
    public List<Genre> getGenresFilms() {
        try {
            log.debug("Получен запрос на чтение всех жанров.");
            String getGenreSql = "SELECT genre_id, genre_name FROM genre ORDER BY genre_id";
            return jdbcTemplate.query(getGenreSql, (rs, rowNum) -> genreMapper(rs));
        } catch (Throwable e) {
            log.debug("Возникло исключение.");
            return null;
        }
    }

    @Override
    public void addFilmGenre(long filmId, int genreId) {
        try{
            jdbcTemplate.update("INSERT INTO films_genre (film_id, genre_id) VALUES (?, ?)", filmId, genreId);
        } catch (RuntimeException e) {
            throw new GenreNotFoundException("Ошибка добавления фильму с filmId=" + filmId + " жанра с genreId=" + genreId);
        }
    }

    @Override
    public void delFilmGenre(long filmId) {
        try{
            jdbcTemplate.update("DELETE FROM films_genre WHERE film_id = ?", filmId);
        } catch (RuntimeException e) {
            throw new GenreNotFoundException("Ошибка удаления жанров у фильма с filmId=" + filmId);
        }
    }

    private Genre genreMapper(ResultSet rs) throws SQLException {
        return Genre.builder()
                .id(rs.getInt("genre_id"))
                .name(rs.getString("genre_name"))
                .build();
    }
}
