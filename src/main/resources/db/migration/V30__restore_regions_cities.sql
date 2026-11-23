-- Восстановление базового справочника после очистки данных.
-- Набор регионов и городов повторяет V4, часовые пояса соответствуют V5.
-- Миграция идемпотентна: на новой БД после V4 и V5 дубликаты не создаются.

INSERT INTO regions (name, code, timezone) VALUES
    ('Москва',                'RU-MOW', 'Europe/Moscow'),
    ('Санкт-Петербург',       'RU-SPE', 'Europe/Moscow'),
    ('Московская область',    'RU-MOS', 'Europe/Moscow'),
    ('Ленинградская область', 'RU-LEN', 'Europe/Moscow'),
    ('Свердловская область',  'RU-SVE', 'Asia/Yekaterinburg'),
    ('Новосибирская область', 'RU-NVS', 'Asia/Novosibirsk'),
    ('Краснодарский край',    'RU-KDA', 'Europe/Moscow'),
    ('Республика Татарстан',  'RU-TA',  'Europe/Moscow'),
    ('Ростовская область',    'RU-ROS', 'Europe/Moscow'),
    ('Челябинская область',   'RU-CHE', 'Asia/Yekaterinburg')
ON CONFLICT (code) DO UPDATE SET
    name = EXCLUDED.name,
    timezone = EXCLUDED.timezone;

INSERT INTO cities (region_id, name)
SELECT regions.id, seed.name
FROM (VALUES
    ('RU-MOW', 'Москва'),
    ('RU-SPE', 'Санкт-Петербург'),
    ('RU-MOS', 'Балашиха'),
    ('RU-MOS', 'Химки'),
    ('RU-MOS', 'Подольск'),
    ('RU-MOS', 'Красногорск'),
    ('RU-MOS', 'Мытищи'),
    ('RU-MOS', 'Люберцы'),
    ('RU-MOS', 'Королёв'),
    ('RU-LEN', 'Всеволожск'),
    ('RU-LEN', 'Гатчина'),
    ('RU-LEN', 'Выборг'),
    ('RU-LEN', 'Тихвин'),
    ('RU-SVE', 'Екатеринбург'),
    ('RU-SVE', 'Нижний Тагил'),
    ('RU-SVE', 'Каменск-Уральский'),
    ('RU-SVE', 'Первоуральск'),
    ('RU-NVS', 'Новосибирск'),
    ('RU-NVS', 'Бердск'),
    ('RU-NVS', 'Искитим'),
    ('RU-KDA', 'Краснодар'),
    ('RU-KDA', 'Сочи'),
    ('RU-KDA', 'Новороссийск'),
    ('RU-KDA', 'Армавир'),
    ('RU-KDA', 'Анапа'),
    ('RU-TA',  'Казань'),
    ('RU-TA',  'Набережные Челны'),
    ('RU-TA',  'Нижнекамск'),
    ('RU-TA',  'Альметьевск'),
    ('RU-ROS', 'Ростов-на-Дону'),
    ('RU-ROS', 'Таганрог'),
    ('RU-ROS', 'Шахты'),
    ('RU-ROS', 'Новочеркасск'),
    ('RU-CHE', 'Челябинск'),
    ('RU-CHE', 'Магнитогорск'),
    ('RU-CHE', 'Златоуст'),
    ('RU-CHE', 'Миасс')
) AS seed(region_code, name)
JOIN regions ON regions.code = seed.region_code
ON CONFLICT (region_id, name) DO NOTHING;
