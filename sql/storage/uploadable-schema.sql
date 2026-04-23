-- ============================================================
-- 存储管理模块 (storage) - 分块上传功能表结构
-- 数据库：PostgreSQL
-- 说明：支持断点续传的分块上传管理
-- ============================================================

-- ============================================================
-- 1. sys_upload_task — 上传任务表
-- Entity: cn.gaifan.douyinOperations.module.storage.entity.SysUploadTask
-- 说明：存储一次完整的上传会话信息
-- ============================================================
CREATE TABLE IF NOT EXISTS sys_upload_task (
    id                  BIGSERIAL       PRIMARY KEY,
    owner_id            BIGINT          NOT NULL,                                      -- 上传者 ID（数据隔离）
    upload_id           VARCHAR(64)     NOT NULL UNIQUE,                              -- 上传会话 ID（UUID）
    bos_upload_id       VARCHAR(256),                                                 -- BOS 多部分上传 ID
    original_filename   VARCHAR(256)    NOT NULL,                                     -- 原始文件名
    storage_key         VARCHAR(512)    NOT NULL,                                     -- 存储路径 key
    file_size           BIGINT          NOT NULL,                                     -- 总文件大小（字节）
    file_md5            VARCHAR(64),                                                  -- 全文件 MD5（秒传判断）
    chunk_size          INTEGER         DEFAULT 5242880,                              -- 单块大小（默认 5 MB）
    total_chunks        INTEGER         NOT NULL,                                     -- 总块数
    uploaded_chunks     INTEGER         DEFAULT 0,                                    -- 已上传块数
    uploaded_bytes      BIGINT          DEFAULT 0,                                    -- 已上传字节数
    status              VARCHAR(32)     NOT NULL DEFAULT 'PENDING',                  -- 状态：PENDING/UPLOADING/COMPLETED/CANCELLED/FAILED
    failure_reason      VARCHAR(512),                                                 -- 失败原因
    module              VARCHAR(64),                                                  -- 所属模块
    deleted             SMALLINT        NOT NULL DEFAULT 0,                           -- 逻辑删除
    created_at          TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,           -- 创建时间
    updated_at          TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,           -- 更新时间
    completed_at        TIMESTAMP,                                                    -- 完成时间
    expire_at           TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP + INTERVAL '7 days',  -- 过期时间

    CONSTRAINT chk_upload_status CHECK (status IN ('PENDING', 'UPLOADING', 'COMPLETED', 'CANCELLED', 'FAILED')),
    CONSTRAINT chk_upload_deleted CHECK (deleted IN (0, 1))
);

-- 索引策略
CREATE INDEX IF NOT EXISTS idx_task_owner_status ON sys_upload_task (owner_id, status, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_task_upload_id ON sys_upload_task (upload_id);
CREATE INDEX IF NOT EXISTS idx_task_file_md5 ON sys_upload_task (file_md5, owner_id, file_size);
CREATE INDEX IF NOT EXISTS idx_task_expire ON sys_upload_task (expire_at) WHERE deleted = 0;

COMMENT ON TABLE  sys_upload_task                    IS '分块上传任务表';
COMMENT ON COLUMN sys_upload_task.owner_id           IS '上传者 ID（用户隔离）';
COMMENT ON COLUMN sys_upload_task.upload_id          IS '前端生成的唯一上传 ID（UUID）';
COMMENT ON COLUMN sys_upload_task.bos_upload_id      IS '百度 BOS InitiateMultipartUpload 返回的 ID';
COMMENT ON COLUMN sys_upload_task.file_md5           IS '全文件 MD5，用于秒传判断';
COMMENT ON COLUMN sys_upload_task.status             IS 'PENDING=初始 UPLOADING=上传中 COMPLETED=完成 CANCELLED=取消 FAILED=失败';
COMMENT ON COLUMN sys_upload_task.uploaded_chunks    IS '已上传的分块数量';
COMMENT ON COLUMN sys_upload_task.uploaded_bytes     IS '已上传的总字节数';
COMMENT ON COLUMN sys_upload_task.expire_at          IS '7 天未完成则自动过期（用于清理）';

-- ============================================================
-- 2. sys_upload_chunk — 分块记录表
-- Entity: cn.gaifan.douyinOperations.module.storage.entity.SysUploadChunk
-- 说明：记录每个分块的上传状态与校验信息
-- ============================================================
CREATE TABLE IF NOT EXISTS sys_upload_chunk (
    id                  BIGSERIAL       PRIMARY KEY,
    task_id             BIGINT          NOT NULL REFERENCES sys_upload_task(id) ON DELETE CASCADE,
    chunk_index         INTEGER         NOT NULL,                                     -- 分块序号（0-indexed）
    chunk_size          BIGINT          NOT NULL,                                     -- 当前分块大小（字节）
    chunk_md5           VARCHAR(64)     NOT NULL,                                     -- 分块 MD5 校验和
    bos_etag            VARCHAR(64),                                                  -- BOS UploadPart 返回的 ETag
    bos_part_number     INTEGER,                                                      -- BOS UploadPart 的 part number
    status              VARCHAR(32)     NOT NULL DEFAULT 'PENDING',                  -- 状态：PENDING/UPLOADING/COMPLETED/FAILED
    retry_count         INTEGER         DEFAULT 0,                                    -- 重试次数
    uploaded_at         TIMESTAMP,                                                    -- 上传完成时间
    created_at          TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,           -- 创建时间

    CONSTRAINT chk_chunk_status CHECK (status IN ('PENDING', 'UPLOADING', 'COMPLETED', 'FAILED')),
    CONSTRAINT uc_chunk_task_index UNIQUE (task_id, chunk_index)
);

-- 索引策略
CREATE INDEX IF NOT EXISTS idx_chunk_status ON sys_upload_chunk (status, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_chunk_md5 ON sys_upload_chunk (chunk_md5);

COMMENT ON TABLE  sys_upload_chunk              IS '分块上传详情表';
COMMENT ON COLUMN sys_upload_chunk.chunk_index  IS '分块序号（0-indexed），与 task 组成唯一标识';
COMMENT ON COLUMN sys_upload_chunk.chunk_md5    IS '分块 MD5，前端上传时校验';
COMMENT ON COLUMN sys_upload_chunk.bos_etag     IS 'BOS UploadPart 返回的 ETag，用于 CompleteMultipartUpload';
COMMENT ON COLUMN sys_upload_chunk.bos_part_number IS 'BOS 的 part number（1-10000 范围）';
COMMENT ON COLUMN sys_upload_chunk.status       IS 'PENDING=待上传 UPLOADING=上传中 COMPLETED=已完成 FAILED=失败';
