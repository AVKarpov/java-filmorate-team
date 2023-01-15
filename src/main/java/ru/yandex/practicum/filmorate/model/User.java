package ru.yandex.practicum.filmorate.model;

import lombok.AllArgsConstructor;
import lombok.Data;

import javax.validation.constraints.Email;
import javax.validation.constraints.PastOrPresent;
import javax.validation.constraints.Pattern;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

@Data
@AllArgsConstructor
//генерирует @Getter,@Setter,@ToString,@EqualsAndHashCode,@RequiredArgsConstructor
public class User {

        private long id; //целочисленный идентификатор

        @Email
        private String email; //электронная почта

        @Pattern(regexp="\\S+")
        private String login; //логин пользователя

        private String name; //имя для отображения

        @PastOrPresent
        private LocalDate birthday; //дата рождения

        private Set<Long> friends=new HashSet<>(); //друзья пользователя

}
