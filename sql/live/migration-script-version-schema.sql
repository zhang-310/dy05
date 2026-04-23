-- ================================================================
-- Live Script Version Table
-- 直播话术版本管理表
-- ================================================================
-- 说明：
--   记录同一话术（live_script）的所有历史版本。
--   场景：直播话术被多次修改，需要追踪版本变化、支持版本对比、支持版本回滚。
--
-- 示例：
--   某个话术 script_id=123 在直播前修改了 3 次：
--   - v1: "亲爱的观众们，欢迎来到我的直播间！"
--   - v2: "各位亲爱的观众们，欢迎来到我的直播间！我是 XXX"（增加了主播名字）
--   - v3: "各位亲爱的观众们，欢迎来到我的直播间！我是 XXX，今天给大家带来..."（增加了预告）
--
-- 创建时间：2026-03-05
-- 作者：DBA
-- ================================================================

CREATE TABLE IF NOT EXISTS live_script_version (
    -- 主键
    id BIGSERIAL PRIMARY KEY,

    -- 关联字段
    script_id BIGINT NOT NULL,                -- 关联 live_script.id
    owner_id BIGINT,                          -- 数据隔离：创建者/所有者（auth_user.id）

    -- 版本信息
    version_number INTEGER NOT NULL,          -- 版本号（1, 2, 3, ...）
                                              -- 从 1 开始递增，每修改一次话术 +1
    content TEXT NOT NULL,                    -- 当前版本的话术内容
    content_hash VARCHAR(64),                 -- 内容 SHA256 hash（用于秒级版本对比）

    -- 变更跟踪
    created_by BIGINT,                        -- 创建者 ID（auth_user.id）
    change_reason VARCHAR(256),               -- 变更原因（为什么修改这个版本？）
                                              -- 例："优化开场白，提高吸引力"
                                              -- 或 "AI 生成建议"

    -- 快照（JSONB，用于存储完整的元数据，便于后续扩展）
    snapshot JSONB,                           -- 完整快照（包含 style/script_type/effectiveness_score/etc）
                                              -- 示例：
                                              -- {
                                              --   "style": "promotional",
                                              --   "scriptType": "custom",
                                              --   "effectivenessScore": 8.5,
                                              --   "productId": 456,
                                              --   "metadata": {...}
                                              -- }

    -- 逻辑删除
    deleted INTEGER NOT NULL DEFAULT 0,       -- 0=未删除, 1=已删除

    -- 时间戳（自动维护，见 Entity 中的 @PrePersist/@PreUpdate）
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- 数据库约束
    CONSTRAINT fk_lsv_script FOREIGN KEY (script_id) REFERENCES live_script(id) ON DELETE CASCADE,
    CONSTRAINT fk_lsv_owner FOREIGN KEY (owner_id) REFERENCES auth_user(id) ON DELETE SET NULL,
    CONSTRAINT fk_lsv_created_by FOREIGN KEY (created_by) REFERENCES auth_user(id) ON DELETE SET NULL,
    CONSTRAINT uk_lsv_script_version UNIQUE (script_id, version_number, deleted)  -- 同一话术中，版本号唯一
);

-- 索引（用于加速查询）
CREATE INDEX idx_lsv_script_id ON live_script_version(script_id) WHERE deleted = 0;
CREATE INDEX idx_lsv_script_version ON live_script_version(script_id, version_number) WHERE deleted = 0;
CREATE INDEX idx_lsv_created_by ON live_script_version(created_by) WHERE deleted = 0;
CREATE INDEX idx_lsv_owner_id ON live_script_version(owner_id) WHERE deleted = 0;
CREATE INDEX idx_lsv_create_time ON live_script_version(create_time) WHERE deleted = 0;

-- GIN 索引（用于加速 JSONB 查询，M2+ 可能用到）
-- CREATE INDEX idx_lsv_snapshot_gin ON live_script_version USING GIN(snapshot) WHERE deleted = 0;

-- ================================================================
-- 数据初始化示例（可选，测试用）
-- ================================================================

-- 如果需要测试，可插入样本数据（假设 live_script.id=1 存在）：
-- INSERT INTO live_script_version (script_id, owner_id, version_number, content, content_hash, created_by, change_reason, snapshot, deleted)
-- VALUES (
--     1,
--     1,
--     1,
--     '亲爱的各位观众，欢迎来到我的直播间！',
--     'sha256_hash_here',
--     1,
--     'Initial version',
--     '{"style": "promotional", "scriptType": "custom", "effectivenessScore": 0}',
--     0
-- );

-- ================================================================
-- 回滚脚本（如需删除，执行以下命令）
-- ================================================================
-- DROP INDEX IF EXISTS idx_lsv_snapshot_gin;
-- DROP INDEX IF EXISTS idx_lsv_create_time;
-- DROP INDEX IF EXISTS idx_lsv_owner_id;
-- DROP INDEX IF EXISTS idx_lsv_created_by;
-- DROP INDEX IF EXISTS idx_lsv_script_version;
-- DROP INDEX IF EXISTS idx_lsv_script_id;
-- DROP TABLE IF EXISTS live_script_version;

-- ================================================================
-- 表设计说明（给开发人员的注释）
-- ================================================================
/*
为什么这样设计？

1. version_number 为什么从 1 开始？
   - 用户友好：版本显示为 V1/V2/V3，而非 V0/V1/V2
   - 易理解：版本号与修改次数 +1 的对应关系清晰

2. content_hash 的用途？
   - 秒级对比：两个版本是否修改过，无需逐字符比较整个 content
   - 数据完整性：检测数据库中的话术是否被篡改（虽然不太可能）

3. snapshot 为什么用 JSONB？
   - 灵活：后续 M2/M3 可能需要存储 AI 生成参数、评分配置、标签等
   - 无需 migration：新增字段无需修改表结构
   - 可查询：PostgreSQL JSONB 支持高效的 JSON 查询和索引

4. created_by 和 owner_id 为什么分开？
   - created_by：记录谁创建了这个版本（用于审计日志）
   - owner_id：数据隔离，确保用户只能看到自己的版本（多租户隔离）
   - 场景：A 用户邀请 B 用户共同编辑话术，两人都是 owner_id，但创建版本时分别记录在 created_by

5. 为什么有 UNIQUE(script_id, version_number, deleted)？
   - 防止同一话术中重复的版本号
   - 加入 deleted 条件：允许逻辑删除后重新创建相同版本号（虽然通常不这样做）

6. 为什么需要外键约束？
   - 确保 script_id 存在
   - 级联删除：如果 script 被删除，其版本也自动删除
   - 但应用层在 repository 中会强制过滤 deleted=0，所以物理删除不会发生

7. M2+ 需要什么扩展？
   - snapshot 中新增 ai_generated 标记（标记是否 AI 生成）
   - snapshot 中新增 evaluation_score（M2 的评分）
   - snapshot 中新增 suggested_by_agent（M3 的自进化引擎）
   - 可能需要新增字段 is_current（标记当前版本），用于快速查询
*/
