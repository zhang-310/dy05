-- Add ai_score column to live_product for Smart Product Scheduling
ALTER TABLE live_product ADD COLUMN IF NOT EXISTS ai_score DOUBLE PRECISION;
