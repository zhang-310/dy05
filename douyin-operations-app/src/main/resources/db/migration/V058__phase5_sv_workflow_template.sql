-- V058: 短视频工作流预设模板表及 3 个预设（P2-7）
CREATE TABLE IF NOT EXISTS sv_workflow_template (
    id BIGSERIAL PRIMARY KEY,
    owner_id BIGINT NOT NULL DEFAULT 0,
    template_name VARCHAR(128) NOT NULL,
    description VARCHAR(512),
    steps TEXT,
    is_system SMALLINT NOT NULL DEFAULT 0,
    deleted SMALLINT NOT NULL DEFAULT 0,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_sv_workflow_template_owner ON sv_workflow_template(owner_id) WHERE deleted = 0;
CREATE INDEX IF NOT EXISTS idx_sv_workflow_template_system ON sv_workflow_template(is_system) WHERE deleted = 0 AND is_system = 1;
COMMENT ON TABLE sv_workflow_template IS '短视频工作流预设模板';

INSERT INTO sv_workflow_template (owner_id, template_name, description, steps, is_system)
SELECT 0, '快速日更', '选题→生成→质检→排期，适合日常更新',
 '[{"id":"topic","type":"auto","action":"hot_topic_select","params":{"count":3}},
   {"id":"generate","type":"auto","action":"ai_generate","depends":["topic"]},
   {"id":"quality","type":"auto","action":"quality_score","depends":["generate"],"condition":"score>=60"},
   {"id":"schedule","type":"auto","action":"calendar_schedule","depends":["quality"]}]',
 1
WHERE NOT EXISTS (SELECT 1 FROM sv_workflow_template WHERE template_name = '快速日更' AND is_system = 1 AND deleted = 0);

INSERT INTO sv_workflow_template (owner_id, template_name, description, steps, is_system)
SELECT 0, '精细制作', '选题→生成→人工审核→分镜→素材→排期',
 '[{"id":"topic","type":"auto","action":"hot_topic_select","params":{"count":5}},
   {"id":"generate","type":"auto","action":"ai_generate","depends":["topic"]},
   {"id":"review","type":"manual","action":"review","depends":["generate"]},
   {"id":"storyboard","type":"auto","action":"generate_shots","depends":["review"]},
   {"id":"material","type":"manual","action":"prepare_material","depends":["storyboard"]},
   {"id":"schedule","type":"auto","action":"calendar_schedule","depends":["material"]}]',
 1
WHERE NOT EXISTS (SELECT 1 FROM sv_workflow_template WHERE template_name = '精细制作' AND is_system = 1 AND deleted = 0);

INSERT INTO sv_workflow_template (owner_id, template_name, description, steps, is_system)
SELECT 0, '爆款复刻', '选爆款→分析→二创→质检→排期',
 '[{"id":"select","type":"manual","action":"select_viral"},
   {"id":"analyze","type":"auto","action":"analyze_viral","depends":["select"]},
   {"id":"remake","type":"auto","action":"remake_from_template","depends":["analyze"]},
   {"id":"quality","type":"auto","action":"quality_score","depends":["remake"],"condition":"score>=70"},
   {"id":"schedule","type":"auto","action":"calendar_schedule","depends":["quality"]}]',
 1
WHERE NOT EXISTS (SELECT 1 FROM sv_workflow_template WHERE template_name = '爆款复刻' AND is_system = 1 AND deleted = 0);
