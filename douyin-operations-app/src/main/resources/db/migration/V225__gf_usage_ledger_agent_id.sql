-- ProductIntegrationService / AiInvocationLedger 双写 gf_usage_ledger 需要 agent_id
alter table gf_usage_ledger add column if not exists agent_id varchar(64);
