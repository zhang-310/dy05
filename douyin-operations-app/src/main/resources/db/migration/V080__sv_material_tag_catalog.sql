-- A-2：素材标签字典（运营维护 + 与素材 tags 字段配合）
CREATE TABLE IF NOT EXISTS sv_material_tag_catalog (
    id              BIGSERIAL PRIMARY KEY,
    owner_id        BIGINT NOT NULL,
    tag             VARCHAR(64) NOT NULL,
    deleted         INTEGER NOT NULL DEFAULT 0,
    create_time     TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time     TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_sv_material_tag_catalog_owner_tag
    ON sv_material_tag_catalog (owner_id, lower(tag))
    WHERE deleted = 0;

CREATE INDEX IF NOT EXISTS idx_sv_material_tag_catalog_owner
    ON sv_material_tag_catalog (owner_id) WHERE deleted = 0;

COMMENT ON TABLE sv_material_tag_catalog IS '短视频素材标签字典（按用户隔离）';
