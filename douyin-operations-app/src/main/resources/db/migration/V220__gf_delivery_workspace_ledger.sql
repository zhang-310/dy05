-- 六产品交付台账（Phase 4 模板）

create table if not exists gf_video_insight_delivery_workspace_ledger (
    ledger_id varchar(96) primary key,
    tenant_id varchar(64) not null references gf_tenant (tenant_id),
    product_code varchar(64) not null default 'video-insight',
    trace_id varchar(128) not null,
    status varchar(32) not null,
    created_at timestamp with time zone not null default current_timestamp
);

create table if not exists gf_shortvideo_maker_delivery_workspace_ledger (
    ledger_id varchar(96) primary key,
    tenant_id varchar(64) not null references gf_tenant (tenant_id),
    product_code varchar(64) not null default 'shortvideo-maker',
    trace_id varchar(128) not null,
    status varchar(32) not null,
    created_at timestamp with time zone not null default current_timestamp
);

create table if not exists gf_digital_human_delivery_workspace_ledger (
    ledger_id varchar(96) primary key,
    tenant_id varchar(64) not null references gf_tenant (tenant_id),
    product_code varchar(64) not null default 'digital-human',
    trace_id varchar(128) not null,
    status varchar(32) not null,
    created_at timestamp with time zone not null default current_timestamp
);

create table if not exists gf_knowledge_base_delivery_workspace_ledger (
    ledger_id varchar(96) primary key,
    tenant_id varchar(64) not null references gf_tenant (tenant_id),
    product_code varchar(64) not null default 'knowledge-base',
    trace_id varchar(128) not null,
    status varchar(32) not null,
    created_at timestamp with time zone not null default current_timestamp
);
