-- Gaifan 商业化核心表（节选 V1）+ sys_product 同步

create table if not exists gf_tenant (
    tenant_id varchar(64) primary key,
    tenant_name varchar(128) not null,
    status varchar(32) not null,
    created_at timestamp with time zone not null default current_timestamp,
    updated_at timestamp with time zone not null default current_timestamp
);

create table if not exists gf_user (
    user_id varchar(64) primary key,
    mobile varchar(32),
    email varchar(128),
    display_name varchar(128) not null,
    password_hash varchar(256),
    status varchar(32) not null,
    created_at timestamp with time zone not null default current_timestamp,
    updated_at timestamp with time zone not null default current_timestamp
);

create table if not exists gf_product (
    product_code varchar(64) primary key,
    product_name varchar(128) not null,
    independently_sellable boolean not null default true,
    enabled boolean not null default true,
    created_at timestamp with time zone not null default current_timestamp
);

create table if not exists gf_feature (
    feature_code varchar(128) primary key,
    product_code varchar(64) not null references gf_product (product_code),
    feature_name varchar(128) not null,
    quota_unit varchar(32),
    monthly_limit decimal(18, 4),
    enabled boolean not null default true,
    created_at timestamp with time zone not null default current_timestamp
);

create table if not exists gf_entitlement (
    entitlement_id varchar(96) primary key,
    tenant_id varchar(64) not null references gf_tenant (tenant_id),
    user_id varchar(64),
    product_code varchar(64) not null references gf_product (product_code),
    feature_code varchar(128),
    grant_type varchar(32) not null,
    valid_from date not null,
    valid_to date not null,
    status varchar(32) not null,
    created_at timestamp with time zone not null default current_timestamp
);

create table if not exists gf_credit_account (
    tenant_id varchar(64) primary key references gf_tenant (tenant_id),
    total_granted decimal(18, 2) not null,
    available_credits decimal(18, 2) not null,
    frozen_credits decimal(18, 2) not null,
    used_credits decimal(18, 2) not null,
    expired_credits decimal(18, 2) not null,
    updated_at timestamp with time zone not null default current_timestamp
);

create table if not exists gf_credit_ledger (
    entry_id varchar(96) primary key,
    tenant_id varchar(64) not null references gf_tenant (tenant_id),
    user_id varchar(64),
    agent_id varchar(96),
    product_code varchar(64) not null,
    feature_code varchar(128) not null,
    channel varchar(32) not null,
    transaction_type varchar(32) not null,
    credits decimal(18, 2) not null,
    balance_after decimal(18, 2) not null,
    trace_id varchar(128) not null,
    reason varchar(512),
    created_at timestamp with time zone not null default current_timestamp
);

create table if not exists gf_usage_ledger (
    usage_id varchar(96) primary key,
    tenant_id varchar(64) not null references gf_tenant (tenant_id),
    user_id varchar(64),
    product_code varchar(64) not null,
    feature_code varchar(128) not null,
    channel varchar(32) not null,
    unit varchar(32) not null,
    amount decimal(18, 4) not null,
    retail_revenue_cny decimal(18, 2) not null,
    provider_cost_cny decimal(18, 2) not null,
    trace_id varchar(128) not null,
    created_at timestamp with time zone not null default current_timestamp
);

create table if not exists gf_audit_event (
    audit_id varchar(96) primary key,
    tenant_id varchar(64) not null references gf_tenant (tenant_id),
    user_id varchar(64),
    product_code varchar(64) not null,
    feature_code varchar(128) not null,
    channel varchar(32) not null,
    risk_level varchar(32) not null,
    status varchar(32) not null,
    trace_id varchar(128) not null,
    decision_note varchar(1000),
    created_at timestamp with time zone not null default current_timestamp
);

insert into gf_tenant (tenant_id, tenant_name, status)
select 'demo-tenant', '演示租户', 'ACTIVE'
where not exists (select 1 from gf_tenant where tenant_id = 'demo-tenant');

insert into gf_product (product_code, product_name, independently_sellable, enabled)
select code, name, true, enabled from sys_product
on conflict (product_code) do nothing;

insert into gf_feature (feature_code, product_code, feature_name, quota_unit, monthly_limit, enabled)
select code, product_code, name, quota_unit, monthly_limit::decimal, enabled from sys_feature
on conflict (feature_code) do nothing;

insert into gf_entitlement (entitlement_id, tenant_id, product_code, grant_type, valid_from, valid_to, status)
select 'ent_demo_' || code, 'demo-tenant', code, 'PRODUCT', current_date - 1, current_date + 365, 'ACTIVE'
from sys_product
on conflict (entitlement_id) do nothing;

insert into gf_credit_account (tenant_id, total_granted, available_credits, frozen_credits, used_credits, expired_credits)
select 'demo-tenant', 20000, 17120, 120, 2860, 0
where not exists (select 1 from gf_credit_account where tenant_id = 'demo-tenant');
