-- 积分冻结单（Gaifan V16 节选）

create table if not exists gf_credit_reservation (
    reservation_id varchar(96) primary key,
    tenant_id varchar(64) not null references gf_tenant (tenant_id),
    user_id varchar(64),
    agent_id varchar(96),
    product_code varchar(64) not null,
    feature_code varchar(128) not null,
    channel varchar(32) not null,
    requested_amount decimal(18, 4) not null,
    pricing_tier varchar(64),
    reserved_credits decimal(18, 2) not null,
    balance_after_reserve decimal(18, 2) not null,
    status varchar(32) not null,
    trace_id varchar(128) not null,
    business_key varchar(128),
    reason varchar(512),
    reserve_entry_id varchar(96),
    commit_entry_id varchar(96),
    release_entry_id varchar(96),
    created_at timestamp with time zone not null default current_timestamp,
    updated_at timestamp with time zone not null default current_timestamp
);

create unique index if not exists uk_credit_reservation_business_key
    on gf_credit_reservation (tenant_id, business_key)
    where business_key is not null;
