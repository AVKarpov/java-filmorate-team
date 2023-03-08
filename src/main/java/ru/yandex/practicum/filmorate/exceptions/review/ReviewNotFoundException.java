package ru.yandex.practicum.filmorate.exceptions.review;

public class ReviewNotFoundException extends RuntimeException {
    public ReviewNotFoundException(String message) {
        super(message);
    }
}
