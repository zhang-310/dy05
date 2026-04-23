-- ============================================================
-- 存储管理模块 (storage) - 表结构
-- 数据库：PostgreSQL
-- 说明：文件上传记录，使用逻辑删除
-- ============================================================

-- ============================================================
-- 1. sys_file — 文件记录表
-- Entity: cn.gaifan.douyinOperations.module.storage.entity.SysFile
-- 使用 @SQLRestriction("deleted = 0") 逻辑删除
-- ============================================================
CREATE TABLE IF NOT EXISTS sys_file (
    id              BIGSERIAL       PRIMARY KEY,
    owner_id        BIGINT,                                                -- 上传用户 ID
    original_name   VARCHAR(256)    NOT NULL,                              -- 原始文件名（用户上传时的文件名）
    storage_name    VARCHAR(256)    NOT NULL,                              -- 存储文件名（UUID + 扩展名）
    storage_path    VARCHAR(512)    NOT NULL,                              -- 存储路径（年/月/日/uuid.ext）
    file_url        VARCHAR(512)    NOT NULL,                              -- 访问 URL（完整 HTTP 地址）
    file_type       VARCHAR(32),                                           -- 文件类型：image / video / audio
    file_ext        VARCHAR(16),                                           -- 扩展名：jpg / png / mp4 / mp3 / wav
    file_size       BIGINT                   DEFAULT 0,                    -- 文件大小（字节）
    module          VARCHAR(64),                                           -- 所属模块：auth / douyin / live / shortvideo / ai
    provider        VARCHAR(32)              DEFAULT 'local',              -- 存储提供商：local / bos / oss
    deleted         INTEGER         NOT NULL DEFAULT 0,                    -- 逻辑删除：0=正常 1=已删除
    create_time     TIMESTAMP                DEFAULT CURRENT_TIMESTAMP     -- 上传时间
);

-- 按用户+模块+时间查询
CREATE INDEX IF NOT EXISTS idx_file_owner_module ON sys_file (owner_id, module, create_time DESC);

-- 按文件类型查询
CREATE INDEX IF NOT EXISTS idx_file_type ON sys_file (file_type, create_time DESC);

COMMENT ON TABLE  sys_file                IS '文件上传记录表';
COMMENT ON COLUMN sys_file.owner_id       IS '上传用户 ID';
COMMENT ON COLUMN sys_file.original_name  IS '原始文件名（用户上传时的文件名）';
COMMENT ON COLUMN sys_file.storage_name   IS '存储文件名（UUID + 扩展名）';
COMMENT ON COLUMN sys_file.storage_path   IS '存储路径（年/月/日/uuid.ext）';
COMMENT ON COLUMN sys_file.file_url       IS '访问 URL（完整 HTTP 地址）';
COMMENT ON COLUMN sys_file.file_type      IS '文件类型：image / video / audio';
COMMENT ON COLUMN sys_file.file_ext       IS '扩展名：jpg / png / mp4 / mp3 / wav';
COMMENT ON COLUMN sys_file.file_size      IS '文件大小（字节）';
COMMENT ON COLUMN sys_file.module         IS '所属模块：auth / douyin / live / shortvideo / ai';
COMMENT ON COLUMN sys_file.provider       IS '存储提供商：local / bos / oss';
COMMENT ON COLUMN sys_file.deleted        IS '逻辑删除：0=正常 1=已删除';
