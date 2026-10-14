-- V3__user_profiles.sql
-- Справочники (регионы, города), профили пользователей и амплуа.

-- ============================================================
-- regions — регионы (субъекты) РФ
-- ============================================================
CREATE TABLE regions (
    id              BIGSERIAL       PRIMARY KEY,
    name            VARCHAR(100)    NOT NULL,
    code            VARCHAR(10)     NOT NULL,
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),

    CONSTRAINT uq_regions_name UNIQUE (name),
    CONSTRAINT uq_regions_code UNIQUE (code)
);

COMMENT ON TABLE regions IS 'Справочник регионов РФ';
COMMENT ON COLUMN regions.code IS 'ISO 3166-2 код (RU-MOW, RU-SPE, RU-MOS, ...)';


-- ============================================================
-- cities — города
-- ============================================================
CREATE TABLE cities (
    id              BIGSERIAL       PRIMARY KEY,
    region_id       BIGINT          NOT NULL,
    name            VARCHAR(150)    NOT NULL,
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_cities_region FOREIGN KEY (region_id) REFERENCES regions (id) ON DELETE RESTRICT,
    CONSTRAINT uq_cities_region_name UNIQUE (region_id, name)
);

CREATE INDEX idx_cities_region ON cities (region_id);

COMMENT ON TABLE cities IS 'Справочник городов, привязанных к регионам';


-- ============================================================
-- user_profiles — расширенный профиль пользователя
-- (без амплуа — оно вынесено в user_positions)
-- ============================================================
CREATE TABLE user_profiles (
    user_id         BIGINT          PRIMARY KEY,
    city_id         BIGINT          NOT NULL,
    last_name       VARCHAR(100)    NOT NULL,
    first_name      VARCHAR(100)    NOT NULL,
    middle_name     VARCHAR(100)    NOT NULL,
    birth_date      DATE            NOT NULL,
    height_cm       SMALLINT        NOT NULL,
    weight_kg       SMALLINT        NOT NULL,
    stick_hand      VARCHAR(10)     NOT NULL,
    jersey_size     VARCHAR(20),
    covers_size     VARCHAR(20),
    gaiters_size    VARCHAR(20),
    updated_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_user_profiles_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_user_profiles_city FOREIGN KEY (city_id) REFERENCES cities (id) ON DELETE RESTRICT,
    CONSTRAINT ck_user_profiles_stick_hand CHECK (stick_hand IN ('LEFT', 'RIGHT')),
    CONSTRAINT ck_user_profiles_height CHECK (height_cm BETWEEN 100 AND 250),
    CONSTRAINT ck_user_profiles_weight CHECK (weight_kg BETWEEN 30 AND 200)
);

CREATE INDEX idx_user_profiles_city ON user_profiles (city_id);

COMMENT ON TABLE user_profiles IS 'Расширенный профиль пользователя';
COMMENT ON COLUMN user_profiles.stick_hand IS 'Хват клюшки: LEFT или RIGHT';


-- ============================================================
-- user_positions — амплуа пользователя (может быть несколько)
-- ============================================================
CREATE TABLE user_positions (
    user_id         BIGINT          NOT NULL,
    position        VARCHAR(20)     NOT NULL,

    CONSTRAINT pk_user_positions PRIMARY KEY (user_id, position),
    CONSTRAINT fk_user_positions_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT ck_user_positions_position CHECK (position IN ('GOALKEEPER', 'DEFENDER', 'FORWARD'))
);

COMMENT ON TABLE user_positions IS 'Амплуа пользователя (может быть несколько)';
COMMENT ON COLUMN user_positions.position IS 'GOALKEEPER (вратарь), DEFENDER (защитник), FORWARD (нападающий)';