-- 账号详情「采集短视频」列表：按账号+未删除+时间/状态筛选排序
CREATE INDEX IF NOT EXISTS idx_sv_viral_account_del_ctime
    ON sv_viral_video (sv_account_id, deleted, create_time DESC);

CREATE INDEX IF NOT EXISTS idx_sv_viral_account_del_status
    ON sv_viral_video (sv_account_id, deleted, deep_analyze_status);
