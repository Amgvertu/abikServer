ALTER TABLE events
    ADD COLUMN lineup_revision BIGINT NOT NULL DEFAULT 0,
    ADD COLUMN sweater_revision BIGINT NOT NULL DEFAULT 0;

CREATE TABLE offline_operations (
    id BIGSERIAL PRIMARY KEY,
    operation_id UUID NOT NULL,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    team_id BIGINT NOT NULL REFERENCES teams(id) ON DELETE CASCADE,
    operation_type VARCHAR(40) NOT NULL,
    request_hash VARCHAR(64) NOT NULL,
    response_json TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uk_offline_operation_user_uuid UNIQUE (user_id, operation_id)
);

CREATE INDEX idx_offline_operations_team ON offline_operations(team_id, created_at);
