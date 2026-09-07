CREATE TABLE system_banners (
    id UUID PRIMARY KEY,
    message TEXT NOT NULL,
    type VARCHAR(32) NOT NULL,
    icon VARCHAR(64),
    priority VARCHAR(32),
    color VARCHAR(32),
    text_color VARCHAR(32),
    start_date DATE,
    end_date DATE,
    target_audience VARCHAR(64),
    active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);
