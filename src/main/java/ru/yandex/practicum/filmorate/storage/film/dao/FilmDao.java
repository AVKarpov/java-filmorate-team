package ru.yandex.practicum.filmorate.storage.film.dao;

import ru.yandex.practicum.filmorate.model.Film;

import java.util.List;
import java.util.Optional;

//методы добавления, удаления и модификации объектов.

public interface FilmDao {

    //добавление фильма
    Film addFilm(Film film);

    //обновление данных о фильме
    Film updateFilm(Film film);

    //удаление фильма
    void deleteFilm(long filmId);

    Film getFilm(long filmId);
    List<Film> getFilms();
    List<Film> getPopularFilms(long maxCount);

    Object getPopularFilmGenreIdYear(long year, long genreId, long count);

    List<Film> getDirectorsFilms(int directorId, String sortBy);
    List<Film> getCommonFilms(long userId,long friendId);
    List<Film> searchFilms(Optional<String> query, List<String> by);
}
