-- Add owner_id column to benchmark_video table
ALTER TABLE benchmark_video ADD COLUMN owner_id BIGINT NOT NULL DEFAULT 0;
CREATE INDEX idx_benchmark_video_owner_id ON benchmark_video(owner_id);

-- Add owner_id column to benchmark_analysis table
ALTER TABLE benchmark_analysis ADD COLUMN owner_id BIGINT NOT NULL DEFAULT 0;
CREATE INDEX idx_benchmark_analysis_owner_id ON benchmark_analysis(owner_id);
