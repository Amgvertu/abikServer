ALTER TABLE sms_codes ADD COLUMN IF NOT EXISTS request_id VARCHAR(36);
CREATE INDEX IF NOT EXISTS idx_sms_codes_request_id ON sms_codes(request_id);
