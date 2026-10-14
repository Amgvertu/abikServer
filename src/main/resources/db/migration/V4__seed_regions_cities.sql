-- V4__seed_regions_cities.sql
-- Наполнение справочников регионов и городов (базовый набор).

-- ============================================================
-- Регионы
-- ============================================================
INSERT INTO regions (name, code) VALUES
                                     ('Москва',                'RU-MOW'),
                                     ('Санкт-Петербург',       'RU-SPE'),
                                     ('Московская область',    'RU-MOS'),
                                     ('Ленинградская область', 'RU-LEN'),
                                     ('Свердловская область',  'RU-SVE'),
                                     ('Новосибирская область', 'RU-NVS'),
                                     ('Краснодарский край',    'RU-KDA'),
                                     ('Республика Татарстан',  'RU-TA'),
                                     ('Ростовская область',    'RU-ROS'),
                                     ('Челябинская область',   'RU-CHE');

-- ============================================================
-- Города — фиксируем алиасы: regions.id и t.name
-- ============================================================

INSERT INTO cities (region_id, name)
SELECT regions.id, 'Москва' FROM regions WHERE regions.code = 'RU-MOW';

INSERT INTO cities (region_id, name)
SELECT regions.id, 'Санкт-Петербург' FROM regions WHERE regions.code = 'RU-SPE';

INSERT INTO cities (region_id, name)
SELECT regions.id, t.name FROM regions
                                   CROSS JOIN (VALUES
                                                   ('Балашиха'), ('Химки'), ('Подольск'), ('Красногорск'),
                                                   ('Мытищи'), ('Люберцы'), ('Королёв')
) AS t(name)
WHERE regions.code = 'RU-MOS';

INSERT INTO cities (region_id, name)
SELECT regions.id, t.name FROM regions
                                   CROSS JOIN (VALUES
                                                   ('Всеволожск'), ('Гатчина'), ('Выборг'), ('Тихвин')
) AS t(name)
WHERE regions.code = 'RU-LEN';

INSERT INTO cities (region_id, name)
SELECT regions.id, t.name FROM regions
                                   CROSS JOIN (VALUES
                                                   ('Екатеринбург'), ('Нижний Тагил'), ('Каменск-Уральский'), ('Первоуральск')
) AS t(name)
WHERE regions.code = 'RU-SVE';

INSERT INTO cities (region_id, name)
SELECT regions.id, t.name FROM regions
                                   CROSS JOIN (VALUES
                                                   ('Новосибирск'), ('Бердск'), ('Искитим')
) AS t(name)
WHERE regions.code = 'RU-NVS';

INSERT INTO cities (region_id, name)
SELECT regions.id, t.name FROM regions
                                   CROSS JOIN (VALUES
                                                   ('Краснодар'), ('Сочи'), ('Новороссийск'), ('Армавир'), ('Анапа')
) AS t(name)
WHERE regions.code = 'RU-KDA';

INSERT INTO cities (region_id, name)
SELECT regions.id, t.name FROM regions
                                   CROSS JOIN (VALUES
                                                   ('Казань'), ('Набережные Челны'), ('Нижнекамск'), ('Альметьевск')
) AS t(name)
WHERE regions.code = 'RU-TA';

INSERT INTO cities (region_id, name)
SELECT regions.id, t.name FROM regions
                                   CROSS JOIN (VALUES
                                                   ('Ростов-на-Дону'), ('Таганрог'), ('Шахты'), ('Новочеркасск')
) AS t(name)
WHERE regions.code = 'RU-ROS';

INSERT INTO cities (region_id, name)
SELECT regions.id, t.name FROM regions
                                   CROSS JOIN (VALUES
                                                   ('Челябинск'), ('Магнитогорск'), ('Златоуст'), ('Миасс')
) AS t(name)
WHERE regions.code = 'RU-CHE';