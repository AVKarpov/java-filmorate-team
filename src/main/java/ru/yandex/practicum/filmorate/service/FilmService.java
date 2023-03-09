package ru.yandex.practicum.filmorate.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;
import ru.yandex.practicum.filmorate.exceptions.SortingIsNotSupportedException;
import ru.yandex.practicum.filmorate.exceptions.ValidationException;
import ru.yandex.practicum.filmorate.exceptions.director.DirectorNotFoundException;
import ru.yandex.practicum.filmorate.exceptions.film.FilmBadParameterException;
import ru.yandex.practicum.filmorate.exceptions.film.FilmNotFoundException;
import ru.yandex.practicum.filmorate.exceptions.user.UserNotFoundException;
import ru.yandex.practicum.filmorate.model.*;
import ru.yandex.practicum.filmorate.storage.film.dao.*;
import ru.yandex.practicum.filmorate.storage.film.daoImpl.DirectorDbDao;
import ru.yandex.practicum.filmorate.storage.user.dao.UserDao;

import javax.validation.constraints.Min;
import javax.validation.constraints.Positive;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

//отвечает за операции с фильмами, — добавление и удаление лайка, вывод 10 наиболее популярных фильмов
// по количеству лайков. Пусть пока каждый пользователь может поставить лайк фильму только один раз.

@Service
@Slf4j
@Validated
public class FilmService {
    private final FilmDao filmStorage;
    private final UserDao userStorage;
    private final MpaDao mpaDao;
    private final FilmLikeDao filmLikeDao;
    private final GenreDao genreDao;
    private final DirectorDbDao directorDao;

    public FilmService(@Qualifier("filmDbStorage") FilmDao filmStorage,
                       @Qualifier("userDbDao") UserDao userStorage,
                       @Qualifier("mpaDbDao") MpaDao mpaDao,
                       @Qualifier("filmLikeDbDao") FilmLikeDao filmLikeDao,
                       @Qualifier("genreDbDao") GenreDao genreDao,
                       @Qualifier("directorDbDao") DirectorDbDao directorDao) {
        this.filmStorage = filmStorage;
        this.userStorage = userStorage;
        this.mpaDao = mpaDao;
        this.filmLikeDao = filmLikeDao;
        this.genreDao = genreDao;
        this.directorDao = directorDao;
    }

    //добавляем фильм
    public Film addFilm(Film film) {
        log.info("Запрос на добавление фильма: {} направлен в хранилище...",film.getName());

        //проверка наличия рейтинга в таблице ratings (MPA)
        validateRatingsMpa(film.getMpa().getId());

        //проверка наличия жанра в таблице genres
        if (film.getGenres() != null && !film.getGenres().isEmpty())
            validateGenres(film.getGenres());

        //проверка наличия режиссера в таблице directors
        if (film.getDirectors() != null && !film.getDirectors().isEmpty())
            validateDirectors(film.getDirectors());
        return filmStorage.addFilm(film);
    }

    //обновляем фильм
    public Film updateFilm(Film film) {
        validateFilmId(film.getId());
        //проверка наличия рейтинга в таблице ratings (MPA)
        //если он задан
        validateRatingsMpa(film.getMpa().getId());

        //поиск жанра в таблице genres
        //если получено пустое поле с жанрами, то игнорируем проверку
        if (film.getGenres() != null && !film.getGenres().isEmpty())
            validateGenres(film.getGenres());
        if ((film.getDirectors() != null && !film.getDirectors().isEmpty()))
            validateDirectors(film.getDirectors());
        return filmStorage.updateFilm(film);
    }

    //удаление фильма по id
    public void deleteFilm(@Positive long filmId) {
        filmStorage.deleteFilm(filmId);
    }

    //получение фильма по id
    public Film getFilm(@Positive long filmId) {
        log.info("GET Запрос на поиск фильма с id={}", filmId);
        return filmStorage.getFilm(filmId);
    }

    //возвращает информацию обо всех фильмах
    public List<Film> getFilms() {
        return filmStorage.getFilms();
    }

    //пользователь ставит лайк фильму.
    public void addLike(@Positive long filmId, @Positive long userId) {
        log.debug("Запрос на добавление фильму с id={} лайка от пользователя с userId={}", filmId, userId);
        Film film = Optional.ofNullable(filmStorage.getFilm(filmId))
                .orElseThrow(() -> new FilmNotFoundException("Фильм с id=" + filmId + " не найден."));
        //проверка существования пользователя с id
        User user = Optional.ofNullable(userStorage.getUser(userId))
                .orElseThrow(() -> new UserNotFoundException("Пользователь с id=" + userId + " не найден."));
        filmLikeDao.addLike(filmId, userId);
    }

    //пользователь удаляет лайк.
    public void deleteLike(@Positive long filmId, @Positive long userId) {
        log.debug("Запрос на удаление лайка фильму с id={} лайка от пользователя с userId={}", filmId, userId);
        filmLikeDao.deleteLike(filmId, userId);
    }

    //вывод популярных фильмов,если параметр не задан, то выводим 10 фильмов
    public List<Film> getPopularFilmGenreIdYear(@Min(1) long count, @Min(0)  long genreId, @Min(0) long year) {
        log.info("Запрос на получение популярных фильмов, параметры фильтра count={}, genreId={}, year={}"
                , count, genreId, year);
        return filmStorage.getPopularFilmGenreIdYear(count, genreId, year);
    }

    public List<Film> getDirectorFilms(int directorId, String sortBy) {
        if (directorDao.getDirectorById(directorId).isEmpty()) {
            log.debug("Director with id = {} is not exist.", directorId);
            throw new DirectorNotFoundException("Director with id = {" + directorId  + "} is not exist.");
        }

        if (!sortBy.equals("year") && !sortBy.equals("likes")) {
            log.debug("Sorting {} is not supported.", sortBy);
            throw new SortingIsNotSupportedException("Sorting " + sortBy + " is not supported.");
        }
        return filmStorage.getDirectorsFilms(directorId, sortBy);
    }

    //вернуть общие фильмы для пользователей
    public List<Film> getCommonFilms(@Positive long userId, @Positive long friendId) {
        log.info("FilmService: Запрошены общие фильмы пользователей.");
        //проверка значений userId и friendId как на значение >0, так и на соответствие Long
        log.info("FilmService: Запрос на получение общих фильмов пользователей с userId={} и friendId={}..."
                , userId, friendId);
        validationNotEqualIdUser(userId, friendId);
        return filmStorage.getCommonFilms(userId, friendId);
    }

    //проверка корректности значений filmId
    private boolean validateFilmId(long filmId) {
        if (filmId <= 0) {
            throw new FilmNotFoundException("Некорректный id фильма.");
        }
        return true;
    }

    //проверка корректности значений filmId
    private boolean isValidUserId(long userId) {
        if (userId <= 0) {
            throw new UserNotFoundException("Некорректный id пользователя.");
        }
        return true;
    }

    //проверка корректности параметров вывода фильмов
    private boolean isValidAboveZero(long param) {
        if (param < 0) {
            throw new FilmBadParameterException("Некорректное значение параметра.");
        }
        return true;
    }
    //проверка наличие видов рейтингов добавляемого/обновляемого фильма в БД
    private void validateRatingsMpa(int mpaId) {
        MPA ratingMpa = mpaDao.getRating(mpaId);
        if (ratingMpa == null)
            throw new ValidationException("Не найден рейтинг фильма с id=" + mpaId);
    }

    //проверка наличие видов жанров добавляемого/обновляемого фильма в БД
    private void validateGenres(Set<Genre> genres) {
        Set<Integer> genresId = genreDao.getGenresFilms().stream().map(g -> g.getId()).collect(Collectors.toSet());
        for (Genre gr : genres) {
            if (!genresId.contains(gr.getId())) {
                throw new ValidationException("Для обновляемого фильма не найдены все жанры.");
            }
        }
    }

    private void validateDirectors(Set<Director> directors) {
        List<Integer> directorsId = directorDao.getAllDirectors().stream()
                .map(Director::getId)
                .collect(Collectors.toList());
        for (Director director : directors) {
            if (!directorsId.contains(director.getId())) {
                throw new DirectorNotFoundException("Для фильма не найден директор с id=" + director.getId());
            }
        }
    }

    //проверяет не равныли id пользователя и друга
    public boolean validationNotEqualIdUser(long userId, long friendId) {
        if (userId == friendId) {
            throw new UserNotFoundException("Пользователь с id=" + userId + " не может добавить сам себя в друзья.");
        }
        return true;
    }

    public List<Film> searchFilms(Optional<String> query, List<String> by) {
        return filmStorage.searchFilms(query, by);
    }

}
