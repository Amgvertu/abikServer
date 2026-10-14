CREATE TABLE IF NOT EXISTS app_info (
                                        id          BIGSERIAL PRIMARY KEY,
                                        version     VARCHAR(50) NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
    );

INSERT INTO app_info (version) VALUES ('0.0.1');