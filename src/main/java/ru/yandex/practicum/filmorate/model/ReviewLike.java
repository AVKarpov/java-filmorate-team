package ru.yandex.practicum.filmorate.model;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ReviewLike {
    private Integer reviewId;
    private Integer userId;
    private boolean isLike;
}
