package ru.yandex.practicum.filmorate.service;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.storage.film.dao.ReviewLikeDao;

@Service
public class ReviewLikeService {
    private final ReviewLikeDao reviewLikeDao;

    public ReviewLikeService(@Qualifier("reviewLikeDbStorage") ReviewLikeDao reviewLikeDao) {
        this.reviewLikeDao = reviewLikeDao;
    }

    public void addLike(int userId, int reviewId) {
        reviewLikeDao.addLike(userId, reviewId);
    }

    public void addDislike(int userId, int reviewId) {
        reviewLikeDao.addDislike(userId, reviewId);
    }

    public void deleteLike(int userId, int reviewId) {
        reviewLikeDao.deleteLike(userId, reviewId);
    }

    public void deleteDislike(int userId, int reviewId) {
        reviewLikeDao.deleteDislike(userId, reviewId);
    }
}
