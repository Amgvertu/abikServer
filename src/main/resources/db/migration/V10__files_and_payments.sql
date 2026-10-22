-- V10__files_and_payments.sql
-- Аватар пользователя и дата внесения платежа.

ALTER TABLE user_profiles ADD COLUMN avatar_url VARCHAR(500);
COMMENT ON COLUMN user_profiles.avatar_url IS 'URL аватара, полученный из POST /api/v1/files';

ALTER TABLE payments ADD COLUMN paid_at DATE NOT NULL DEFAULT CURRENT_DATE;
COMMENT ON COLUMN payments.paid_at IS 'Дата внесения платежа (редактируется вручную)';

CREATE INDEX idx_payments_paid_at ON payments (paid_at);