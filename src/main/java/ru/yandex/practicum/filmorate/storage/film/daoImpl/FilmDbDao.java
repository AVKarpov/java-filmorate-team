package ru.yandex.practicum.filmorate.storage.film.daoImpl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.filmorate.exceptions.film.FilmNotFoundException;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.FilmRating;
import ru.yandex.practicum.filmorate.model.Genre;
import ru.yandex.practicum.filmorate.model.MPA;
import ru.yandex.practicum.filmorate.storage.film.dao.FilmDao;
import ru.yandex.practicum.filmorate.storage.film.dao.GenreDao;
import ru.yandex.practicum.filmorate.storage.film.dao.MpaDao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Component("filmDbStorage")
@Primary
@Slf4j
public class FilmDbDao implements FilmDao {
    private final JdbcTemplate jdbcTemplate;
    private final MpaDao mpaDao;
    private final GenreDao genreDao;

    public FilmDbDao(JdbcTemplate jdbcTemplate, @Qualifier("mpaDbDao") MpaDao mpaDao,
                     @Qualifier("genreDbDao") GenreDao genreDao) {
        this.jdbcTemplate = jdbcTemplate;
        this.mpaDao = mpaDao;
        this.genreDao = genreDao;
    }

    @Override
    public Film addFilm(Film film) {
        log.info("Запрос на добавление фильма: {} получен хранилищем БД", film.getName());

        //добавить информацию о фильме в таблицу films
        String addFilmSql = "INSERT INTO films(name,description,release_date,duration,rate,rating_id) VALUES(?,?,?,?,?,?);";
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(
                new PreparedStatementCreator() {
                    public PreparedStatement createPreparedStatement(Connection connection) throws SQLException {
                        PreparedStatement ps =
                                connection.prepareStatement(addFilmSql, new String[]{"film_id"});
                        ps.setString(1, film.getName());
                        ps.setString(2, film.getDescription());
                        ps.setString(3, film.getReleaseDate().toString());
                        ps.setInt(4, film.getDuration());
                        ps.setInt(5, film.getRate());
                        ps.setInt(6, film.getMpa().getId());
                        return ps;
                    }
                },
                keyHolder);
        long filmId = keyHolder.getKey().intValue();
        film.setId(filmId);
        log.debug("Добавлен новый фильм с id={}", filmId);

        //если все жанры найдены в БД, то добавляем записи о жанрах в таблицу films_genre
        if (film.getGenres() != null && !film.getGenres().isEmpty()) {
            log.debug("Добавляем жанры вновь создаваемому фильму с id={}.", filmId);
            Set<Integer> genres = film.getGenres().stream()
                    .map(Genre::getId)
                    .collect(Collectors.toSet());
            for (int gr : genres) {
                genreDao.addFilmGenre(film.getId(), gr);
            }
            log.debug("Жанры для фильма с id={} добавлены.", filmId);
        }
        return getFilm(film.getId());
    }

    @Override
    //обновляем поля таблицы films: name, releaseDate, description, duration, rate, rating_id
    //поле rating_id сначала ищем в таблице ratings_mpa и если найден, то обновляем
    //обновляем поля таблицы films_genre:film_id, genre_id - удаляем и перезаписываем
    public Film updateFilm(Film film) {
        log.info("Получен запрос на обновление фильма с id={} в БД", film.getId());

        //обновляем данные в таблице films
        log.debug("Формируем sql запрос...");
        String updateFilmSql = "UPDATE films SET name=?,description=?,release_date=?,duration=?,rate=?," +
                "rating_id=? WHERE film_id=?;";
        Object[] args = new Object[]{film.getName(), film.getDescription(), film.getReleaseDate(),
                film.getDuration(), film.getRate(), film.getMpa().getId(), film.getId()};

        int updateRow = jdbcTemplate.update(updateFilmSql, args);
        if (updateRow <= 0) {
            log.debug("Фильм с id={} для обновления не найден.", film.getId());
            throw new FilmNotFoundException("Фильм с id=" + film.getId() + " для обновления не найден.");
        }
        log.debug("Фильм с id={} обновлён.", film.getId());
        //если все жанры найдены в БД, то сначала удаляем записи из films_genre
        // потом добавляем записи о жанрах в таблицу films_genre
        genreDao.delFilmGenre(film.getId());
        log.debug("Обновляем жанры фильма с film_id={}, жанры: {}", film.getId(), film.getGenres());
        if (film.getGenres() != null && !film.getGenres().isEmpty()) {
            Set<Integer> genres = film.getGenres().stream()
                    .map(Genre::getId)
                    .collect(Collectors.toSet());
            log.debug("id жанров для обновления: {}", genres.toString());
            for (int gr : genres) {
                genreDao.addFilmGenre(film.getId(), gr);
            }
            log.debug("Жанры фильма с film_id={} обновлены.", film.getId());
        }
        return getFilm(film.getId());
    }

    @Override
    public void deleteFilm(long filmId) {
        log.debug("Получен запрос на удаление фильма с id={}", filmId);
        String deleteFilmSql = "delete from films where film_id= ?";
        Object[] args = new Object[]{filmId};
        int delRow = jdbcTemplate.update(deleteFilmSql, args);
        if (delRow <= 0) {
            log.debug("Фильм с id={} для удаления не найден.", filmId);
            throw new FilmNotFoundException("Фильм с id=" + filmId + " для удаления не найден.");
        }
        log.debug("Фильм с id={} удалён.", filmId);
    }

    @Override
    //возвращаемые поля:
    //из таблицы films: film_id, name, description, release_date, duration, rate,
    //genre - Set: genre.id...
    //из таблицы ratings_mpa: mpa.id,mpa.name
    public Film getFilm(long filmId) {
        log.debug("Получен запрос на фильм с id={};", filmId);
        String getFilmSql = "select f.FILM_ID,f.NAME,f.DESCRIPTION,f.RELEASE_DATE,f.RELEASE_DATE,f.DURATION,f.RATE," +
                "rm.RATING_ID,rm.RATING_NAME,g.GENRE_ID,g.GENRE_NAME from films f " +
                "LEFT JOIN RATINGS_MPA rm " +
                "ON f.RATING_ID =rm.RATING_ID LEFT JOIN FILMS_GENRE fg ON f.FILM_ID =fg.FILM_ID LEFT JOIN GENRE g " +
                "ON fg.GENRE_ID =g.GENRE_ID WHERE f.film_id=?; ";
        List<Film> films = jdbcTemplate.query(getFilmSql, (rs, rowNum) -> filmMapper(rs), filmId);
        //перебираем films, убираем дубли и группируем жанры
        Optional<Film> film = getUniqueFilm(films).values().stream().findFirst();
        if (!film.isPresent()) {
            log.debug("С id={} фильм не найден.", filmId);
            throw new FilmNotFoundException("С id=" + filmId + " фильм не найден.");
        }
        log.debug("С id={} возвращён фильм: {}", filmId, films.stream().findFirst().get().getName());
        return film.get();
    }

    @Override
    public List<Film> getFilms() {
        log.debug("Получен запрос на чтение всех фильмов");
        String getFilmSql = "select f.FILM_ID ,f.NAME ,f.DESCRIPTION ,f.RELEASE_DATE ,f.RELEASE_DATE ,f.DURATION ,f.RATE ," +
                "rm.RATING_ID ,rm.RATING_NAME ,g.GENRE_ID ,g.GENRE_NAME from films f LEFT JOIN RATINGS_MPA rm " +
                "ON f.RATING_ID =rm.RATING_ID LEFT JOIN FILMS_GENRE fg ON f.FILM_ID =fg.FILM_ID LEFT JOIN GENRE g " +
                "ON fg.GENRE_ID =g.GENRE_ID ORDER BY f.FILM_ID;";
        //запрашиваем все фильмы с жанрами и рейтингом MPA
        List<Film> films = jdbcTemplate.query(getFilmSql, (rs, rowNum) -> filmMapper(rs));
        if (films == null) {
            log.debug("Фильмы не найдены.");
            throw new FilmNotFoundException("Фильмы не найдены.");
        }
        log.debug("Получен списко из {} фильмов.", films.size());
        //если у фильма несколько жанров, то будут дубли
        //перебираем получившийся набор, если дубль, то добавляем в Set<Genre> ещё один жанр
        //long - film_id
        //Film - информация о фильме
        //перебираем films, убираем дубли и группируем жанры
        LinkedHashMap<Long, Film> filmsMap = getUniqueFilm(films);
        log.debug("После удаления дублей осталось {} фильмов.", filmsMap.size());
        return filmsMap.values().stream().collect(Collectors.toList());
    }


    @Override
    public List<Film> getPopularFilms(long maxCount) {
        String popFilmSql = "SELECT f2.FILM_ID ,f2.NAME ,f2.DESCRIPTION ,f2.RELEASE_DATE ,f2.RELEASE_DATE ,f2.DURATION ,f2.RATE," +
                "rm.RATING_ID ,rm.RATING_NAME ,g.GENRE_ID ,g.GENRE_NAME FROM (SELECT f.* FROM FILMS f LEFT JOIN " +
                "(SELECT FILM_ID,COUNT(*) cLike FROM FILMS_LIKE GROUP BY FILM_ID ) fl ON fl.FILM_ID=f.FILM_ID " +
                "ORDER BY clike DESC limit(?)) f2 " +
                "LEFT JOIN RATINGS_MPA rm ON f2.RATING_ID =rm.RATING_ID " +
                "LEFT JOIN FILMS_GENRE fg ON f2.FILM_ID =fg.FILM_ID " +
                "LEFT JOIN GENRE g ON fg.GENRE_ID =g.GENRE_ID;";
        List<Film> popFilms = jdbcTemplate.query(popFilmSql, (rs, rowNum) -> filmMapper(rs), maxCount);
        log.debug("Популярные фильмы:");
        for (Film film : popFilms) {
            log.debug("Фильм с film_id={}: {}", film.getId(), film);
        }
        //перебираем films, убираем дубли и группируем жанры
        LinkedHashMap<Long, Film> filmsMap = getUniqueFilm(popFilms);
        return filmsMap.values().stream().collect(Collectors.toList());
    }

    @Override
    public List<Film> getCommonFilms(long userId, long friendId) {
        log.info("FilmDb: Запрос на получение общих фильмов пользователей с userId={} и friendId={}...", userId, friendId);
        String getSql = "SELECT u.film_id,count(fl.FILM_ID) rating\n" +
                "FROM (SELECT * FROM FILMS_LIKE fl WHERE USER_ID =?) u\n" +
                "INNER JOIN \n" +
                "(SELECT * FROM FILMS_LIKE fl WHERE USER_ID =?) f\n" +
                "ON u.film_id=f.film_id\n" +
                "LEFT JOIN FILMS_LIKE fl ON u.film_id=fl.FILM_ID \n" +
                "GROUP BY fl.FILM_ID";
        Object[] args = new Object[]{userId, friendId};
        return getFilmsRatingSort(getSql,args);
    }

    @Override
    public List<Film> getFilms(List<Long> filmsId) {
        log.debug("FilmDbDao: Получен запрос на чтение нескольких фильмов с определённым filmId");
        log.debug("FilmDbDao: получены filmsId рекомендуемых фильмов: {}", filmsId.toString());
        String inSql = String.join(",", Collections.nCopies(filmsId.size(), "?"));
        String getFilmSql = String.format("select f.FILM_ID ,f.NAME ,f.DESCRIPTION ,f.RELEASE_DATE ,f.RELEASE_DATE ,f.DURATION ,f.RATE ," +
                "rm.RATING_ID ,rm.RATING_NAME ,g.GENRE_ID ,g.GENRE_NAME from films f LEFT JOIN RATINGS_MPA rm " +
                "ON f.RATING_ID =rm.RATING_ID LEFT JOIN FILMS_GENRE fg ON f.FILM_ID =fg.FILM_ID LEFT JOIN GENRE g " +
                "ON fg.GENRE_ID =g.GENRE_ID WHERE f.FILM_ID IN (%s) ORDER BY f.FILM_ID;", inSql);
        //запрашиваем все фильмы с жанрами и рейтингом MPA
        List<Film> films = jdbcTemplate.query(getFilmSql, (rs, rowNum) -> filmMapper(rs), filmsId.toArray());
        if (films == null) {
            log.debug("Фильмы не найдены.");
            throw new FilmNotFoundException("Фильмы не найдены.");
        }
        log.debug("Получен списко из {} фильмов.", films.size());
        LinkedHashMap<Long, Film> filmsMap = getUniqueFilm(films);
        log.debug("После удаления дублей осталось {} фильмов.", filmsMap.size());
        return filmsMap.values().stream().collect(Collectors.toList());
    }

    public List<Film> getFilmsRatingSort(String sql, Object[] args) {
        Map<Long, Integer> filmsId = jdbcTemplate.query(sql, (rs, rowNum) -> filmRatingMapper(rs), args)
                .stream()
                .collect(Collectors.toMap(v -> v.getFilmId(), v -> v.getRating()));
        //получаем список общих фильмов и сортируем фильмы по убыванию рейтинга
        return getFilms(filmsId.keySet().stream()
                .collect(Collectors.toList()))
                .stream()
                .sorted((f1, f2) -> filmsId.get(f2.getId()) - filmsId.get(f1.getId()))
                .collect(Collectors.toList());
    }

    //формирует объект с id фильма и его рейтинга (количество лайков)
    public FilmRating filmRatingMapper(ResultSet rs) throws SQLException {
        return FilmRating.builder()
                .filmId(rs.getLong("film_id"))
                .rating(rs.getInt("rating"))
                .build();
    }

    private Film filmMapper(ResultSet rs) throws SQLException {
        //перебираем записи результирующего набора
        long id = rs.getLong("film_id");
        String name = rs.getString("name");
        String description = rs.getString("description");
        LocalDate releaseDate = rs.getDate("release_date").toLocalDate();
        int duration = rs.getInt("duration");
        int rate = rs.getInt("rate");
        MPA mpa = new MPA();
        mpa.setId(rs.getInt("rating_id"));
        mpa.setName(rs.getString("rating_name"));
        int genreId = rs.getInt("genre_id");
        log.debug("Получен жанр фильма с film_id={} - genre_id={}", id, genreId);
        Set<Genre> genres = new HashSet<>();
        if (genreId > 0) {
            genres.add(new Genre(genreId, rs.getString("genre_name")));
        }
        return new Film(id, name, description, releaseDate, duration, rate, mpa, genres);
    }

    private LinkedHashMap<Long, Film> getUniqueFilm(List<Film> films) {
        LinkedHashMap<Long, Film> filmsMap = new LinkedHashMap<>();
        for (Film film : films) {
            if (filmsMap.containsKey(film.getId())) {
                filmsMap.get(film.getId()).addGenres(film.getGenres().stream().findFirst().get());
            } else {
                filmsMap.put(film.getId(), film);
            }
        }
        return filmsMap;
    }
}
