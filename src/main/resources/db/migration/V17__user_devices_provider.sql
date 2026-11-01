-- V17__user_devices_provider.sql
-- Провайдер push-уведомлений устройства: FCM (по умолчанию), RUSTORE, HUAWEI
ALTER TABLE user_devices ADD COLUMN provider VARCHAR(20) NOT NULL DEFAULT 'FCM';

COMMENT ON COLUMN user_devices.provider IS 'Провайдер push: FCM, RUSTORE, HUAWEI';