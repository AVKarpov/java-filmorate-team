package ru.yandex.practicum.filmorate.exceptions.director;

public class DirectorIdIsNullException extends RuntimeException{
    public DirectorIdIsNullException(String message) {
        super(message);
    }
}
