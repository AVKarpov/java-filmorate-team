package ru.yandex.practicum.filmorate.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Data
@AllArgsConstructor
public class ReviewLike {
    private Integer reviewId;
    private Integer userId;
    private boolean isLike;
}
