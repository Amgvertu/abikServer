-- Логотип команды необязателен; название, регион и город остаются обязательными.
ALTER TABLE teams ALTER COLUMN logo_url DROP NOT NULL;
