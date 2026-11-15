-- V25__web_push_subscriptions.sql
-- Подписки стандартного Web Push отделены от нативных токенов RuStore/FCM/Huawei.
CREATE TABLE web_push_subscriptions (
    id           BIGSERIAL PRIMARY KEY,
    user_id      BIGINT       NOT NULL,
    endpoint     VARCHAR(2048) NOT NULL,
    p256dh       VARCHAR(512)  NOT NULL,
    auth         VARCHAR(255)  NOT NULL,
    user_agent   VARCHAR(512),
    created_at   TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    last_seen_at TIMESTAMPTZ   NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_web_push_subscriptions_user
        FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT uq_web_push_subscriptions_endpoint UNIQUE (endpoint)
);

CREATE INDEX idx_web_push_subscriptions_user ON web_push_subscriptions (user_id);

COMMENT ON TABLE web_push_subscriptions IS 'Браузерные Web Push-подписки пользователей';
COMMENT ON COLUMN web_push_subscriptions.endpoint IS 'Уникальный endpoint браузерной подписки, не нативный push-токен';
