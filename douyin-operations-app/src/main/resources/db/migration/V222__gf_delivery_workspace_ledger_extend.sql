-- 补全六产品交付台账（Phase 4 B1）

create table if not exists gf_douyin_ops_delivery_workspace_ledger (
    ledger_id varchar(96) primary key,
    tenant_id varchar(64) not null references gf_tenant (tenant_id),
    product_code varchar(64) not null default 'douyin-ops',
    trace_id varchar(128) not null,
    status varchar(32) not null,
    created_at timestamp with time zone not null default current_timestamp
);

create table if not exists gf_drama_ai_delivery_workspace_ledger (
    ledger_id varchar(96) primary key,
    tenant_id varchar(64) not null references gf_tenant (tenant_id),
    product_code varchar(64) not null default 'drama-ai',
    trace_id varchar(128) not null,
    status varchar(32) not null,
    created_at timestamp with time zone not null default current_timestamp
);

create table if not exists gf_photo_avatar_video_delivery_workspace_ledger (
    ledger_id varchar(96) primary key,
    tenant_id varchar(64) not null references gf_tenant (tenant_id),
    product_code varchar(64) not null default 'photo-avatar-video',
    trace_id varchar(128) not null,
    status varchar(32) not null,
    created_at timestamp with time zone not null default current_timestamp
);
