-- 工作流任务表（Gaifan V17 节选）

create table if not exists gf_workflow_task (
    task_id varchar(96) primary key,
    tenant_id varchar(64) not null references gf_tenant (tenant_id),
    user_id varchar(64),
    agent_id varchar(96),
    product_code varchar(64) not null,
    feature_code varchar(128) not null,
    workflow_code varchar(128) not null,
    title varchar(256) not null,
    status varchar(32) not null,
    progress_percent integer not null,
    source_asset_id varchar(96),
    result_asset_id varchar(96),
    reservation_id varchar(96),
    required_feature_codes varchar(1000),
    authorization_required boolean not null,
    cost_tracked boolean not null,
    review_required boolean not null,
    trace_id varchar(128) not null,
    max_attempts integer not null default 3,
    attempt_count integer not null default 0,
    next_run_at timestamp with time zone not null default current_timestamp,
    timeout_at timestamp with time zone,
    last_error varchar(1000),
    created_at timestamp with time zone not null default current_timestamp,
    updated_at timestamp with time zone not null default current_timestamp
);

create table if not exists gf_workflow_event (
    event_id varchar(96) primary key,
    task_id varchar(96) not null references gf_workflow_task (task_id),
    tenant_id varchar(64) not null references gf_tenant (tenant_id),
    from_status varchar(32),
    to_status varchar(32) not null,
    operator_type varchar(64) not null,
    message varchar(1000),
    trace_id varchar(128) not null,
    occurred_at timestamp with time zone not null default current_timestamp
);

create table if not exists gf_workflow_task_attempt (
    attempt_id varchar(96) primary key,
    task_id varchar(96) not null references gf_workflow_task (task_id),
    tenant_id varchar(64) not null references gf_tenant (tenant_id),
    attempt_no integer not null,
    status varchar(32) not null,
    message varchar(1000),
    trace_id varchar(128) not null,
    started_at timestamp with time zone not null default current_timestamp,
    finished_at timestamp with time zone
);
