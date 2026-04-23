-- ============================================================
-- 五位主播人设配置表（行业大脑升级 - 五位主播项目）
-- 肖瑶/肖蝉/阳阳/智慧/田玲红
-- ============================================================

CREATE TABLE IF NOT EXISTS ai_host_persona (
    id              BIGSERIAL       PRIMARY KEY,
    host_code       VARCHAR(32)     NOT NULL,
    host_name       VARCHAR(64)     NOT NULL,
    age             INTEGER,
    orientation     VARCHAR(16)     NOT NULL DEFAULT 'C',  -- C=消费者 B=商业
    positioning     VARCHAR(256),
    content_matrix  TEXT,           -- JSON: 内容矩阵比例 教程40% 好物30%...
    ai_priorities   TEXT,           -- JSON: AI需求优先级
    style_vector    TEXT,           -- JSON: 多模态风格向量
    bayes_factors   TEXT,           -- JSON: 因果推理因子权重
    flow_phase      INTEGER         DEFAULT 0,  -- 流量路径阶段 0=入口 1=转化 2=sink
    sort_order      INTEGER         DEFAULT 0,
    status          INTEGER         NOT NULL DEFAULT 1,
    deleted         INTEGER         NOT NULL DEFAULT 0,
    create_time     TIMESTAMP       DEFAULT CURRENT_TIMESTAMP,
    update_time     TIMESTAMP       DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_ai_host_persona_code ON ai_host_persona(host_code) WHERE deleted = 0;

COMMENT ON TABLE ai_host_persona IS '五位主播人设配置';
COMMENT ON COLUMN ai_host_persona.orientation IS 'C=消费者导向 B=商业导向';
COMMENT ON COLUMN ai_host_persona.content_matrix IS 'JSON 内容矩阵';
COMMENT ON COLUMN ai_host_persona.flow_phase IS '0=流量入口 1=转化节点 2=B端沉淀';
