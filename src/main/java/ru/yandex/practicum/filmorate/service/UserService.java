package ru.yandex.practicum.filmorate.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;
import ru.yandex.practicum.filmorate.exceptions.ValidationException;
import ru.yandex.practicum.filmorate.exceptions.user.UserNotFoundException;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.feed.Feed;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.storage.film.dao.FilmLikeDao;
import ru.yandex.practicum.filmorate.storage.film.daoImpl.FeedDbDao;
import ru.yandex.practicum.filmorate.storage.user.dao.FriendsDao;
import ru.yandex.practicum.filmorate.storage.user.dao.UserDao;

import javax.validation.constraints.Positive;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
@Validated
public class UserService {

   private final UserDao userStorage;
   private final FriendsDao friendsDao;
   private final FilmLikeDao filmLikeDao;
   private final FeedDbDao feedDbDao;

    public UserService(UserDao userStorage,
                       FriendsDao friendsDao,
                       FilmLikeDao filmLikeDao,
                       FeedDbDao feedDbDao) {
        this.userStorage = userStorage;
        this.friendsDao = friendsDao;
        this.filmLikeDao = filmLikeDao;
        this.feedDbDao = feedDbDao;
    }

    //добавление пользователя
    public User addUser(User user) {
        log.info("Получен запрос на добавление пользователя...");
        log.debug("Получен для долбавления пользователь с id="+user.getId());
        return userStorage.addUser(user);
    }

    //обновление пользователя
    public User updateUser(User user) {
        log.info("Получен запрос на обновление пользователя...");
        validationIdUser(user.getId());
        return userStorage.updateUser(user);
    }

    //возвращает информацию обо всех пользователях
    public List<User> getUsers() {
        log.info("Получен запрос на чтение пользователей...");
        return userStorage.getUsers().stream()
                .sorted(Comparator.comparingLong(User::getId))
                .collect(Collectors.toList());
    }

    //получение данных о пользователе
    public User getUser(@Positive long userId) {
        log.info("Получен запрос на получение данных пользователя с id={}", userId);
        log.info("Пользователь с id={} получен.", userId);
        return userStorage.getUser(userId);
    }

    //добавление в друзья
    public void addFriend(@Positive long userId, @Positive long friendId) {
        log.debug("Получен запрос на добавление для пользователя с id={} друга с id={}", userId, friendId);
        validationNotEqualIdUser(userId, friendId);
        //проверка наличия пользователей в БД
        userStorage.getUser(userId);
        userStorage.getUser(friendId);
        friendsDao.addFriend(userId,friendId);
        log.info("Для пользователя с id = {} добавлен друг с id={}", userId, friendId);
    }

    //удаление из друзей
    public void deleteFriend(@Positive long userId, @Positive long friendId) {
        log.debug("Получен запрос на удаление для пользователя с id={} друга с id={}", userId, friendId);
        validationNotEqualIdUser(userId, friendId);
        log.debug("Запрос на удаление для пользователя с id={} друга с id={} одобрен.", userId, friendId);
        friendsDao.deleteFriend(userId,friendId);
    }

    //возвращение списка друзей пользователя
    public List<User> getFriends(@Positive long userId) {
        log.debug("Получен запрос на получение для пользователя с id={} списка друзей", userId);
        userStorage.getUser(userId);
        return friendsDao.getFriends(userId);
    }

    //список друзей, общих с другим пользователем.
    public List<User> getCommonFriends(@Positive long userId, @Positive long otherId) {
        log.debug("Получен запрос на поиск общих друзей для пользователей с userId={} и otherId={}.", userId, otherId);
        validationNotEqualIdUser(userId, otherId);
        return friendsDao.getCommonFriends(userId,otherId);
    }

    //выводим рекомендуемых фильмов для пользователя
    public List<Film> getRecommendations(@Positive long userId) {
        log.info("Service: получен запрос на вывод рекомендаций фильмов для userId={}.", userId);
        return filmLikeDao.getRecomendationFilm(userId);
    }

    public List<Feed> getUserFeed(@Positive long userId) {
        log.info("Получен запрос ленты событий пользователя с userId={}", userId);
        return feedDbDao.getFeed(userId);
    }

    private void validationIdUser(long userId) {
        if (userId <= 0) {
            throw new ValidationException("Некорректный id=" + userId + " пользователя.");
        }
        log.debug("Валидация пользователя с id={} прошла успешно.", userId);
    }

    //проверяет не равныли id пользователя и друга
    public void validationNotEqualIdUser(long userId, long friendId) {
        if (userId == friendId) {
            throw new UserNotFoundException("Пользователь с id=" + userId + " не может добавить сам себя в друзья.");
        }
    }

     public void deleteUserById (@Positive long id) {
         userStorage.deleteUser(id);
     }
}
