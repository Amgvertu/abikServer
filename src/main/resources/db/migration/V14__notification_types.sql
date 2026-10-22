-- V14__notification_types.sql
-- Комментарий к CHECK на notifications.type. Сам CHECK отсутствует — просто обновляем документацию.
COMMENT ON COLUMN notifications.type IS
    'Тип события: INVITATION, ROLE_CHANGED, NEW_CHARGE, PAYMENT, DEBT_REMINDER, '
        'OWNERSHIP_TRANSFER, EXCLUDED, ARCHIVED, EVENT_CREATED, EVENT_UPDATED, '
        'EVENT_CANCELLED, ROSTER_ADDED, ROSTER_REMOVED, TRAINER_ASSIGNED';