-- Seed a default ops commander agent for the local admin user when the legacy agent table exists.
DO $$
DECLARE
    admin_id BIGINT;
BEGIN
    IF to_regclass('public.agent') IS NULL OR to_regclass('public.auth_user') IS NULL THEN
        RETURN;
    END IF;

    SELECT id INTO admin_id
    FROM auth_user
    WHERE username = 'admin' AND deleted = 0
    ORDER BY id
    LIMIT 1;

    IF admin_id IS NULL THEN
        RETURN;
    END IF;

    IF NOT EXISTS (
        SELECT 1
        FROM agent
        WHERE user_id = admin_id
          AND agent_name = '抖音运营总控 Agent'
          AND deleted = 0
    ) THEN
        INSERT INTO agent (
            user_id,
            agent_name,
            description,
            agent_type,
            system_prompt,
            model_config,
            available_tools,
            rating_count,
            rating_sum,
            conversation_count,
            response_mode,
            status,
            version,
            deleted,
            create_time,
            update_time
        ) VALUES (
            admin_id,
            '抖音运营总控 Agent',
            '汇总官方知识库、违规规则、短视频分布式采集、直播话术和业务生成引用闭环的运营指挥智能体',
            5,
            '你是抖音运营总控 Agent。必须优先调用 douyin_ops_commander 获取系统状态，再按任务调用 kb_rag_search、compliance_check、product_search、script_generate、live_session_query。涉及直播话术、短视频脚本、千川素材审核时，必须要求引用 douyin 与 douyin_weigui 官方规则。',
            '{}',
            '["douyin_ops_commander","kb_rag_search","compliance_check","product_search","script_generate","live_session_query"]',
            0,
            0,
            0,
            1,
            1,
            1,
            0,
            CURRENT_TIMESTAMP,
            CURRENT_TIMESTAMP
        );
    END IF;
END $$;
