CREATE TABLE guide_progress (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    screen_key VARCHAR(80) NOT NULL,
    role_key VARCHAR(40) NOT NULL,
    completed_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_guide_progress UNIQUE (user_id, screen_key, role_key)
);

CREATE TABLE app_releases (
    id BIGSERIAL PRIMARY KEY,
    platform VARCHAR(20) NOT NULL,
    version_code INTEGER NOT NULL,
    version_name VARCHAR(40) NOT NULL,
    release_notes TEXT NOT NULL,
    download_url TEXT NOT NULL,
    mandatory BOOLEAN NOT NULL DEFAULT false,
    published_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_app_release UNIQUE (platform, version_code)
);
CREATE INDEX idx_app_release_latest ON app_releases(platform, version_code DESC);

CREATE TABLE team_expenses (
    id BIGSERIAL PRIMARY KEY,
    team_id BIGINT NOT NULL REFERENCES teams(id) ON DELETE CASCADE,
    category VARCHAR(80) NOT NULL,
    amount_kopecks BIGINT NOT NULL CHECK (amount_kopecks > 0),
    expense_date DATE NOT NULL,
    comment TEXT,
    event_id BIGINT REFERENCES events(id) ON DELETE SET NULL,
    created_by BIGINT NOT NULL REFERENCES users(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_team_expenses_list ON team_expenses(team_id, expense_date DESC, id DESC);
