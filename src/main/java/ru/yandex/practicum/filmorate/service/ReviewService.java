package ru.yandex.practicum.filmorate.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.model.Review;
import ru.yandex.practicum.filmorate.storage.film.dao.FilmDao;
import ru.yandex.practicum.filmorate.storage.film.dao.ReviewDao;
import ru.yandex.practicum.filmorate.storage.user.dao.UserDao;

import java.util.List;

@Service
@Slf4j
public class ReviewService {

    private final ReviewDao reviewDao;
    private final FilmDao filmDao;
    private final UserDao userDao;

    public ReviewService(@Qualifier("reviewDbStorage")ReviewDao reviewDao,
                         @Qualifier("filmDbStorage") FilmDao filmDao,
                         @Qualifier("userDbDao") UserDao userDao) {
        this.reviewDao = reviewDao;
        this.filmDao = filmDao;
        this.userDao = userDao;
    }

    public Review addReview(Review review) {
        filmDao.getFilm(review.getFilmId());
        userDao.getUser(review.getUserId());
        return reviewDao.addReview(review);
    }

    public Review updateReview(Review review) {
        filmDao.getFilm(review.getFilmId());
        userDao.getUser(review.getUserId());
        return reviewDao.updateReview(review);
    }

    public void deleteReviewById(Integer id) {
        reviewDao.deleteReviewById(id);
    }

    public Review getReviewById(long id) {
        return reviewDao.getReviewById(id);
    }

    public List<Review> getAllReviewsByFilmId(Integer id, Integer count) {
        return id != 0 ? reviewDao.getAllReviewsByFilmId(id, count) :
                reviewDao.getTopReviews(count);
    }
}
