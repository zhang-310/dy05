-- 六产品 M4：补全互调规则
insert into gf_product_integration_rule (rule_code, source_product_code, target_product_code, target_feature_code, invocation_name, scenario, billing_policy, audit_required, enabled)
select 'shortvideo-maker-to-photo-avatar', 'shortvideo-maker', 'photo-avatar-video', 'photo-avatar-video.generate', '成片调用照片口播', '口播片段', '按 photo-avatar-video 扣费', true, true
where not exists (select 1 from gf_product_integration_rule where rule_code = 'shortvideo-maker-to-photo-avatar');

insert into gf_product_integration_rule (rule_code, source_product_code, target_product_code, target_feature_code, invocation_name, scenario, billing_policy, audit_required, enabled)
select 'drama-ai-to-shortvideo-maker', 'drama-ai', 'shortvideo-maker', 'shortvideo-maker.export', '短剧导出成片', '剧本完成后导出', '按 shortvideo-maker 扣费', true, true
where not exists (select 1 from gf_product_integration_rule where rule_code = 'drama-ai-to-shortvideo-maker');
