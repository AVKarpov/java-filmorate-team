package ru.yandex.practicum.filmorate.storage.film.dao;

public interface ReviewLikeDao {
    void addLike(int userId, int reviewId);
    void addDislike(int userId, int reviewId);
    void deleteLike(int userId, int reviewId);
    void deleteDislike(int userId, int reviewId);

    int getUsefulByReviewId(long id);
}
