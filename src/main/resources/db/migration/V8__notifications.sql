-- V8__notifications.sql
-- Уведомления пользователей (push через FCM).

-- ============================================================
-- notifications — уведомления
-- ============================================================
CREATE TABLE notifications (
                               id              BIGSERIAL       PRIMARY KEY,
                               user_id         BIGINT          NOT NULL,
                               type            VARCHAR(50)     NOT NULL,
                               title           VARCHAR(200)    NOT NULL,
                               body            VARCHAR(1000)   NOT NULL,
                               team_id         BIGINT,
                               payload_json    JSONB,
                               is_read         BOOLEAN         NOT NULL DEFAULT FALSE,
                               sent_at         TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
                               read_at         TIMESTAMPTZ,
                               delivery_status VARCHAR(20)     NOT NULL DEFAULT 'SENT',

                               CONSTRAINT fk_notifications_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
                               CONSTRAINT fk_notifications_team FOREIGN KEY (team_id) REFERENCES teams (id) ON DELETE SET NULL,
                               CONSTRAINT ck_notifications_status CHECK (delivery_status IN ('PENDING', 'SENT', 'FAILED'))
);

CREATE INDEX idx_notifications_user       ON notifications (user_id);
CREATE INDEX idx_notifications_user_read  ON notifications (user_id, is_read);
CREATE INDEX idx_notifications_sent_at    ON notifications (sent_at DESC);
CREATE INDEX idx_notifications_type       ON notifications (type);

COMMENT ON TABLE notifications IS 'Уведомления пользователей';
COMMENT ON COLUMN notifications.type IS 'Тип события: INVITATION, ROLE_CHANGED, NEW_CHARGE, PAYMENT, DEBT_REMINDER, OWNERSHIP_TRANSFER, EXCLUDED, ARCHIVED';
COMMENT ON COLUMN notifications.payload_json IS 'Дополнительные данные (JSON), например {teamId, chargeId, amount}';
COMMENT ON COLUMN notifications.is_read IS 'Прочитано ли уведомление';
COMMENT ON COLUMN notifications.delivery_status IS 'Статус отправки: SENT (mock), FAILED (реальные ошибки FCM)';