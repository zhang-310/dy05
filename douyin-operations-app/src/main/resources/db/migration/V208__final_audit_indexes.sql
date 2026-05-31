-- V208: 最终索引优化 — 审计 + 通知 + 导出
CREATE INDEX IF NOT EXISTS idx_audit_user ON sys_audit_event(user_id, create_time DESC);
CREATE INDEX IF NOT EXISTS idx_notif_unread ON sys_notification(user_id, read_status) WHERE read_status = false;
CREATE INDEX IF NOT EXISTS idx_export_user ON sys_export_task(user_id, create_time DESC);
CREATE INDEX IF NOT EXISTS idx_file_user ON sys_file_record(user_id, create_time DESC);
CREATE INDEX IF NOT EXISTS idx_refund_tenant ON sys_refund_record(tenant_id, create_time DESC);
CREATE INDEX IF NOT EXISTS idx_kb_doc_title ON sys_knowledge_document USING gin(to_tsvector('simple', title));
