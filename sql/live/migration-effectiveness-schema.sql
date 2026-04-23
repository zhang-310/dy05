-- W-04: 直播话术效果评分系统表
-- 创建时间: 2026-03-05
-- 描述: 存储话术的效果评分和排行数据

CREATE TABLE IF NOT EXISTS live_script_effectiveness (
    id BIGSERIAL PRIMARY KEY,
    script_id BIGINT NOT NULL,
    session_id BIGINT NOT NULL,
    version VARCHAR(16),

    -- 关键指标
    conversion_rate NUMERIC(5, 2) DEFAULT 0,      -- 转化率 (%)
    likes BIGINT DEFAULT 0,                         -- 点赞数
    comments INTEGER DEFAULT 0,                     -- 评论数
    completion_rate NUMERIC(5, 2) DEFAULT 0,       -- 完播率 (%)

    -- 评分结果
    total_score NUMERIC(4, 2) DEFAULT 0,            -- 综合评分 (0-10)
    score_formula VARCHAR(256),                     -- 评分公式
    ranking INTEGER DEFAULT 1,                      -- 排名
    ranking_trend INTEGER DEFAULT 0,                -- 排名趋势: 1=上升, 0=持平, -1=下降
    tag VARCHAR(32),                                -- 标签: hot(热门)/recommend(推荐)/emerging(新兴)

    -- 样本量
    sample_size INTEGER DEFAULT 0,                  -- 使用次数

    -- 时间字段
    calculated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted INTEGER NOT NULL DEFAULT 0,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 创建索引
CREATE INDEX idx_live_script_effectiveness_script_id ON live_script_effectiveness(script_id);
CREATE INDEX idx_live_script_effectiveness_session_id ON live_script_effectiveness(session_id);
CREATE INDEX idx_live_script_effectiveness_total_score ON live_script_effectiveness(total_score DESC);
CREATE INDEX idx_live_script_effectiveness_ranking ON live_script_effectiveness(ranking ASC);
CREATE INDEX idx_live_script_effectiveness_tag ON live_script_effectiveness(tag);
CREATE INDEX idx_live_script_effectiveness_deleted ON live_script_effectiveness(deleted);
