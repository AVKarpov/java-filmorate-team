package ru.yandex.practicum.filmorate.model;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Data
@AllArgsConstructor
public class Review {
    private long reviewId;
    @NotNull
    private Integer filmId;
    @NotNull
    private Integer userId;
    @NotBlank
    private String content;
    private int useful;
    @JsonProperty("isPositive")
    @NotNull
    private Boolean isPositive;
}
