package ru.yandex.practicum.filmorate.storage.film.daoImpl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.filmorate.storage.film.dao.ReviewLikeDao;

import java.sql.ResultSet;
import java.sql.SQLException;

@Component("reviewLikeDbStorage")
@Slf4j
public class ReviewLikeDbDao implements ReviewLikeDao {

    private final JdbcTemplate jdbcTemplate;

    public ReviewLikeDbDao(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void addLike(int userId, int reviewId) {
        String sql = "INSERT INTO useful_review (review_id, user_id, is_like) VALUES (?, ?, TRUE)";
        jdbcTemplate.update(sql, reviewId, userId);
    }

    @Override
    public void addDislike(int userId, int reviewId) {
        String sql = "INSERT INTO useful_review (review_id, user_id, is_like) VALUES (?, ?, FALSE)";
        jdbcTemplate.update(sql, reviewId, userId);
    }

    @Override
    public void deleteLike(int userId, int reviewId) {
        String sql = "DELETE FROM useful_review WHERE user_id = ? AND review_id = ? AND is_like = TRUE";
        jdbcTemplate.update(sql, reviewId, userId);
    }

    @Override
    public void deleteDislike(int userId, int reviewId) {
        String sql = "DELETE FROM useful_review WHERE user_id = ? AND review_id = ? AND is_like = FALSE";
        jdbcTemplate.update(sql, reviewId, userId);
    }

    @Override
    public int getUsefulByReviewId(long id) {
        String sql = "SELECT (COUNT(CASE is_like WHEN TRUE THEN 1 END) - " +
                "COUNT(CASE is_like WHEN FALSE THEN 1 END)) AS useful FROM useful_review WHERE review_id = ?";
        return jdbcTemplate.query(sql, (rs, rowNum) -> rs.getInt("useful"), id)
                .stream().findFirst().orElse(0);
    }
}
