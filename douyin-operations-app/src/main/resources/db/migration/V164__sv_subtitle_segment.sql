-- Owner: shortvideo-center
-- Purpose: persist subtitle editor data instead of browser-only placeholder state.

CREATE TABLE IF NOT EXISTS sv_subtitle_segment (
    id BIGSERIAL PRIMARY KEY,
    owner_id BIGINT NOT NULL,
    video_id BIGINT NOT NULL,
    segment_key VARCHAR(64) NOT NULL,
    start_time DOUBLE PRECISION NOT NULL,
    end_time DOUBLE PRECISION NOT NULL,
    text TEXT NOT NULL,
    font_size INT,
    color VARCHAR(32),
    font_family VARCHAR(128),
    position VARCHAR(32),
    deleted INT NOT NULL DEFAULT 0,
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_sv_subtitle_owner_video
    ON sv_subtitle_segment(owner_id, video_id);

CREATE INDEX IF NOT EXISTS idx_sv_subtitle_video_start
    ON sv_subtitle_segment(video_id, start_time);

CREATE UNIQUE INDEX IF NOT EXISTS uk_sv_subtitle_owner_video_key_active
    ON sv_subtitle_segment(owner_id, video_id, segment_key)
    WHERE deleted = 0;

COMMENT ON TABLE sv_subtitle_segment IS 'Short video subtitle editor segments';
