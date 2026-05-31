-- ProductIntegrationService 互调审计写入 agent_id
alter table gf_audit_event add column if not exists agent_id varchar(64);
