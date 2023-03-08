package ru.yandex.practicum.filmorate.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.filmorate.model.Review;
import ru.yandex.practicum.filmorate.service.ReviewLikeService;
import ru.yandex.practicum.filmorate.service.ReviewService;

import javax.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/reviews")
@Slf4j
public class ReviewController {
    private final ReviewService reviewService;
    private final ReviewLikeService reviewLikeService;

    public ReviewController(ReviewService reviewService, ReviewLikeService reviewLikeService) {
        this.reviewService = reviewService;
        this.reviewLikeService = reviewLikeService;
    }

    @PostMapping
    public Review addReview(@Valid @RequestBody Review review) {
        return reviewService.addReview(review);
    }

    @PutMapping
    public Review updateReview(@Valid @RequestBody Review review) {
        return reviewService.updateReview(review);
    }

    @DeleteMapping("/{id}")
    public void deleteReviewById(@PathVariable("id") Integer id) {
        reviewService.deleteReviewById(id);
    }

    @GetMapping("/{id}")
    public Review getReviewById(@PathVariable("id") long id) {
        return reviewService.getReviewById(id);
    }

    @GetMapping
    public List<Review> getAllReviewsByFilmId(@RequestParam(defaultValue = "0", required = false) Integer filmId,
                                          @RequestParam(defaultValue = "10", required = false) Integer count) {
        return reviewService.getAllReviewsByFilmId(filmId, count);
    }

    @PutMapping("/{id}/like/{userId}")
    public void addLike(@PathVariable("userId") int userId, @PathVariable("id") int reviewId) {
        reviewLikeService.addLike(userId, reviewId);
    }

    @PutMapping("/{id}/dislike/{userId}")
    public void addDislike(@PathVariable("userId") int userId, @PathVariable("id") int reviewId) {
        reviewLikeService.addDislike(userId, reviewId);
    }

    @DeleteMapping("/{id}/like/{userId}")
    public void deleteLike(@PathVariable("userId") int userId, @PathVariable("id") int reviewId) {
        reviewLikeService.deleteLike(userId, reviewId);
    }

    @DeleteMapping("/{id}/dislike/{userId}")
    public void deleteDislike(@PathVariable("userId") int userId, @PathVariable("id") int reviewId) {
        reviewLikeService.deleteDislike(userId, reviewId);
    }
}
