package ru.yandex.practicum.filmorate.storage.film.dao;

import ru.yandex.practicum.filmorate.model.Review;

import java.util.List;

public interface ReviewDao {
    Review addReview(Review review);
    Review updateReview(Review review);
    void deleteReviewById(Integer id);
    Review getReviewById(long id);
    List<Review> getAllReviewsByFilmId(Integer id, Integer count);

    List<Review> getTopReviews(Integer count);
}
