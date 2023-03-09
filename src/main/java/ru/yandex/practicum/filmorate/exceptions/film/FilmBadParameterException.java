package ru.yandex.practicum.filmorate.exceptions.film;

public class FilmBadParameterException extends RuntimeException {
    public FilmBadParameterException(String message) {
        super(message);
    }
}
