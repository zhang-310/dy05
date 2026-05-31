-- MCP / OpenAPI 开发者表（Phase 2）

create table if not exists gf_developer_app (
    app_id varchar(96) primary key,
    tenant_id varchar(64) not null references gf_tenant (tenant_id),
    app_name varchar(128) not null,
    status varchar(32) not null,
    created_at timestamp with time zone not null default current_timestamp
);

create table if not exists gf_api_key (
    key_id varchar(96) primary key,
    app_id varchar(96) not null references gf_developer_app (app_id),
    key_hash varchar(256) not null,
    masked_key varchar(64) not null,
    status varchar(32) not null,
    created_at timestamp with time zone not null default current_timestamp,
    last_used_at timestamp with time zone,
    expires_at timestamp with time zone
);

create table if not exists gf_mcp_invocation (
    invocation_id varchar(96) primary key,
    tenant_id varchar(64) not null references gf_tenant (tenant_id),
    user_id varchar(64) not null,
    agent_id varchar(96),
    tool_code varchar(128) not null,
    product_code varchar(64) not null,
    feature_code varchar(128) not null,
    channel varchar(32) not null,
    allowed boolean not null,
    trace_id varchar(128) not null,
    created_at timestamp with time zone not null default current_timestamp
);

create table if not exists gf_ai_invocation (
    request_id varchar(96) primary key,
    tenant_id varchar(64) not null references gf_tenant (tenant_id),
    user_id varchar(64) not null,
    product_code varchar(64) not null,
    feature_code varchar(128) not null,
    provider_code varchar(64) not null,
    model_code varchar(96) not null,
    prompt_code varchar(128) not null,
    total_tokens decimal(18, 4) not null,
    estimated_cost_cny decimal(18, 6) not null,
    trace_id varchar(128) not null,
    created_at timestamp with time zone not null default current_timestamp
);
