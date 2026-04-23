-- 智能体模块增强：添加工具配置字段
-- 日期: 2026-04-22

-- 添加可用工具字段
ALTER TABLE agent ADD COLUMN IF NOT EXISTS available_tools TEXT NULL;
COMMENT ON COLUMN agent.available_tools IS '可用工具列表（JSON 数组）';

-- 更新类型注释
COMMENT ON COLUMN agent.agent_type IS '智能体类型：0=自定义 1=话术生成 2=违规检测 3=商品分析 4=场次规划 5=数据分析 6=客户服务';

-- 验证
SELECT
    COUNT(*) as total_agents,
    COUNT(available_tools) as has_tools
FROM agent
WHERE deleted = 0;
