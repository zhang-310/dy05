-- JWT E2E：admin(userId=1) 映射 tenantId=user-1，需与 demo-tenant 同等权益
INSERT INTO gf_tenant (tenant_id, tenant_name, status) VALUES
    ('user-1', 'JWT 用户租户', 'ACTIVE')
ON CONFLICT (tenant_id) DO NOTHING;

INSERT INTO gf_entitlement (entitlement_id, tenant_id, product_code, grant_type, valid_from, valid_to, status) VALUES
    ('ent_user1_douyin_ops', 'user-1', 'douyin-ops', 'PRODUCT', current_date - 1, current_date + 365, 'ACTIVE'),
    ('ent_user1_video_insight', 'user-1', 'video-insight', 'PRODUCT', current_date - 1, current_date + 365, 'ACTIVE'),
    ('ent_user1_shortvideo_maker', 'user-1', 'shortvideo-maker', 'PRODUCT', current_date - 1, current_date + 365, 'ACTIVE'),
    ('ent_user1_digital_human', 'user-1', 'digital-human', 'PRODUCT', current_date - 1, current_date + 365, 'ACTIVE'),
    ('ent_user1_photo_avatar', 'user-1', 'photo-avatar-video', 'PRODUCT', current_date - 1, current_date + 365, 'ACTIVE'),
    ('ent_user1_drama_ai', 'user-1', 'drama-ai', 'PRODUCT', current_date - 1, current_date + 365, 'ACTIVE'),
    ('ent_user1_knowledge_base', 'user-1', 'knowledge-base', 'PRODUCT', current_date - 1, current_date + 365, 'ACTIVE')
ON CONFLICT (entitlement_id) DO NOTHING;

INSERT INTO gf_credit_account (tenant_id, total_granted, available_credits, frozen_credits, used_credits, expired_credits)
SELECT 'user-1', 20000, 17120, 0, 0, 0
WHERE NOT EXISTS (SELECT 1 FROM gf_credit_account WHERE tenant_id = 'user-1');

UPDATE gf_credit_account SET available_credits = GREATEST(available_credits, 17120)
WHERE tenant_id = 'user-1' AND available_credits < 1000;

-- JWT admin(organization_id=1) → tenantId=org-1
INSERT INTO gf_tenant (tenant_id, tenant_name, status) VALUES
    ('org-1', '组织租户 org-1', 'ACTIVE')
ON CONFLICT (tenant_id) DO NOTHING;

INSERT INTO gf_entitlement (entitlement_id, tenant_id, product_code, grant_type, valid_from, valid_to, status) VALUES
    ('ent_org1_douyin_ops', 'org-1', 'douyin-ops', 'PRODUCT', current_date - 1, current_date + 365, 'ACTIVE'),
    ('ent_org1_video_insight', 'org-1', 'video-insight', 'PRODUCT', current_date - 1, current_date + 365, 'ACTIVE'),
    ('ent_org1_shortvideo_maker', 'org-1', 'shortvideo-maker', 'PRODUCT', current_date - 1, current_date + 365, 'ACTIVE'),
    ('ent_org1_digital_human', 'org-1', 'digital-human', 'PRODUCT', current_date - 1, current_date + 365, 'ACTIVE'),
    ('ent_org1_photo_avatar', 'org-1', 'photo-avatar-video', 'PRODUCT', current_date - 1, current_date + 365, 'ACTIVE'),
    ('ent_org1_drama_ai', 'org-1', 'drama-ai', 'PRODUCT', current_date - 1, current_date + 365, 'ACTIVE'),
    ('ent_org1_knowledge_base', 'org-1', 'knowledge-base', 'PRODUCT', current_date - 1, current_date + 365, 'ACTIVE')
ON CONFLICT (entitlement_id) DO NOTHING;

INSERT INTO gf_credit_account (tenant_id, total_granted, available_credits, frozen_credits, used_credits, expired_credits)
SELECT 'org-1', 20000, 17120, 0, 0, 0
WHERE NOT EXISTS (SELECT 1 FROM gf_credit_account WHERE tenant_id = 'org-1');

UPDATE gf_credit_account SET available_credits = GREATEST(available_credits, 17120)
WHERE tenant_id = 'org-1' AND available_credits < 1000;
