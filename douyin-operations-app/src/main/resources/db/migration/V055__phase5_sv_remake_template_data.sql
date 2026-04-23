-- V055: Phase 5 二创预设模板数据（P0-4）
-- 幂等：仅当无预设模板时插入
DO $$
BEGIN
  IF (SELECT COUNT(*) FROM sv_remake_template WHERE owner_id=0 AND deleted=0) = 0 THEN
    INSERT INTO sv_remake_template (owner_id, template_name, remake_type, structure_template, adaptation_guide, variable_slots, deleted) VALUES
    (0, '形式模仿-产品测评', 'form_copy', '{"segments":[{"type":"hook","duration":"0-3s","desc":"悬念开场"},{"type":"problem","duration":"3-8s","desc":"痛点展示"},{"type":"solution","duration":"8-20s","desc":"产品展示"},{"type":"proof","duration":"20-25s","desc":"效果证明"},{"type":"cta","duration":"25-30s","desc":"行动号召"}]}'::jsonb, '保留视频结构和节奏，替换产品信息和具体内容', '[{"slot":"productName","desc":"产品名称"},{"slot":"painPoint","desc":"用户痛点"},{"slot":"effect","desc":"产品效果"}]'::jsonb, 0),
    (0, '内容翻转-反向种草', 'content_flip', '{"segments":[{"type":"controversy","duration":"0-3s","desc":"反向观点开场"},{"type":"evidence","duration":"3-15s","desc":"反面论据"},{"type":"twist","duration":"15-22s","desc":"观点翻转"},{"type":"reveal","duration":"22-30s","desc":"真实推荐"}]}'::jsonb, '保留话题，反转观点角度，制造反差感', '[{"slot":"topic","desc":"话题"},{"slot":"reverseView","desc":"反向观点"},{"slot":"realRecommend","desc":"真实推荐"}]'::jsonb, 0),
    (0, '元素重组-多爆款融合', 'element_remix', '{"segments":[{"type":"bestHook","duration":"0-3s","desc":"取自爆款A的开场"},{"type":"bestContent","duration":"3-18s","desc":"取自爆款B的内容"},{"type":"bestCta","duration":"18-25s","desc":"取自爆款C的转化"},{"type":"original","duration":"25-30s","desc":"原创收尾"}]}'::jsonb, '提取多个爆款的优秀元素重新组合', '[{"slot":"hookSource","desc":"开场来源"},{"slot":"contentSource","desc":"内容来源"},{"slot":"ctaSource","desc":"转化来源"}]'::jsonb, 0),
    (0, '升维创新-专业测评', 'dimension_upgrade', '{"segments":[{"type":"original","duration":"0-5s","desc":"原视频精华"},{"type":"upgrade","duration":"5-20s","desc":"专业维度升级"},{"type":"comparison","duration":"20-25s","desc":"对比分析"},{"type":"conclusion","duration":"25-30s","desc":"专业结论"}]}'::jsonb, '在爆款基础上增加专业测评、成分分析等新维度', '[{"slot":"originalHighlight","desc":"原视频精华"},{"slot":"professionalAngle","desc":"专业角度"},{"slot":"conclusion","desc":"专业结论"}]'::jsonb, 0);
  END IF;
END $$;
