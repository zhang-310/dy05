-- ProductIntegrationService 互调流水扩展字段
alter table gf_product_integration_invocation add column if not exists billing_mode varchar(32);
alter table gf_product_integration_invocation add column if not exists workflow_task_id varchar(96);
alter table gf_product_integration_invocation add column if not exists reservation_id varchar(96);
