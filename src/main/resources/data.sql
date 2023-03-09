--удаляем все данные и обнуляем счётчики первичных ключей
DELETE FROM feeds;
DELETE FROM event_types;
DELETE FROM operations;
DELETE FROM films_genre;
DELETE FROM genre;
DELETE FROM films_like;
DELETE FROM films;
DELETE FROM ratings_mpa;
DELETE FROM friends;
DELETE FROM users;
DELETE FROM films_review;
DELETE FROM useful_review;
DELETE FROM directors;

ALTER TABLE films ALTER COLUMN film_id RESTART WITH 1;
ALTER TABLE ratings_mpa ALTER COLUMN rating_id RESTART WITH 1;
ALTER TABLE films_genre ALTER COLUMN films_genre_id RESTART WITH 1;
ALTER TABLE films_like ALTER COLUMN films_like_id RESTART WITH 1;
ALTER TABLE friends ALTER COLUMN user_friend_id RESTART WITH 1;
ALTER TABLE genre ALTER COLUMN genre_id RESTART WITH 1;
ALTER TABLE users ALTER COLUMN user_id RESTART WITH 1;
ALTER TABLE feeds ALTER COLUMN event_id RESTART WITH 1;
ALTER TABLE event_types ALTER COLUMN id RESTART WITH 1;
ALTER TABLE operations ALTER COLUMN id RESTART WITH 1;
ALTER TABLE directors ALTER COLUMN director_id RESTART WITH 1;
ALTER TABLE films_review ALTER COLUMN review_id RESTART WITH 1;

--заполняем таблицу ratings
INSERT INTO ratings_mpa(rating_name) VALUES('G'),
                                        ('PG'),
                                        ('PG-13'),
                                        ('R'),
                                        ('NC-17');

--заполянем таблицу genre
INSERT INTO genre(genre_name) VALUES('Комедия'),
                                    ('Драма'),
                                    ('Мультфильм'),
                                    ('Триллер'),
                                    ('Документальный'),
                                    ('Боевик');

--заполянем таблицу event_types
INSERT INTO event_types(event_type) VALUES('LIKE'),
                                    ('REVIEW'),
                                    ('FRIEND');

--заполянем таблицу operations
INSERT INTO operations(operation) VALUES('REMOVE'),
                                    ('ADD'),
                                    ('UPDATE');