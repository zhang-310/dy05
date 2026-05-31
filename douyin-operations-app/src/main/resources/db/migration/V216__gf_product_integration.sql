-- 产品互调规则与调用流水（Gaifan V4 + V11 节选）

create table if not exists gf_product_integration_rule (
    rule_code varchar(128) primary key,
    source_product_code varchar(64) not null,
    target_product_code varchar(64) not null,
    target_feature_code varchar(128) not null,
    invocation_name varchar(128) not null,
    scenario varchar(512) not null,
    billing_policy varchar(256) not null,
    audit_required boolean not null,
    enabled boolean not null,
    created_at timestamp with time zone not null default current_timestamp
);

create table if not exists gf_product_integration_invocation (
    invocation_id varchar(96) primary key,
    tenant_id varchar(64) not null references gf_tenant (tenant_id),
    user_id varchar(64),
    agent_id varchar(96),
    source_product_code varchar(64) not null,
    source_feature_code varchar(128),
    target_product_code varchar(64) not null,
    target_feature_code varchar(128) not null,
    billing_product_code varchar(64) not null,
    billing_feature_code varchar(128) not null,
    channel varchar(32) not null,
    status varchar(32) not null,
    requested_amount decimal(18, 4) not null,
    retail_revenue_cny decimal(18, 2) not null,
    provider_cost_cny decimal(18, 2) not null,
    trace_id varchar(128) not null,
    decision_code varchar(64) not null,
    decision_reason varchar(1000),
    created_at timestamp with time zone not null default current_timestamp
);

insert into gf_product_integration_rule (rule_code, source_product_code, target_product_code, target_feature_code, invocation_name, scenario, billing_policy, audit_required, enabled)
select 'douyin-to-video-insight', 'douyin-ops', 'video-insight', 'video-insight.breakdown', '抖音运营调用短视频洞察', '合规视频拆解', '按 video-insight 扣费', true, true
where not exists (select 1 from gf_product_integration_rule where rule_code = 'douyin-to-video-insight');

insert into gf_product_integration_rule (rule_code, source_product_code, target_product_code, target_feature_code, invocation_name, scenario, billing_policy, audit_required, enabled)
select 'shortvideo-maker-to-digital-human', 'shortvideo-maker', 'digital-human', 'digital-human.generate', '短视频成片调用数字人', '口播片段', '按 digital-human 扣费', true, true
where not exists (select 1 from gf_product_integration_rule where rule_code = 'shortvideo-maker-to-digital-human');
