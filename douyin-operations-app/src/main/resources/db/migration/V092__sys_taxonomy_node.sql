-- Phase 0 / A-2：全站可控分类（taxonomy），无 DB 外键；跨模块共用 module_scope
CREATE TABLE IF NOT EXISTS sys_taxonomy_node (
    id              BIGSERIAL PRIMARY KEY,
    owner_id        BIGINT NOT NULL DEFAULT 0,
    module_scope    VARCHAR(64) NOT NULL,
    parent_id       BIGINT,
    code            VARCHAR(64) NOT NULL,
    name            VARCHAR(128) NOT NULL,
    sort_order      INT NOT NULL DEFAULT 0,
    enabled         SMALLINT NOT NULL DEFAULT 1,
    deleted         INT NOT NULL DEFAULT 0,
    create_time     TIMESTAMP,
    update_time     TIMESTAMP
);

COMMENT ON TABLE sys_taxonomy_node IS '全站分类节点；owner_id=0 为平台预置；应用层校验 parent，不设 FK';
COMMENT ON COLUMN sys_taxonomy_node.module_scope IS '域：shortvideo_material、live_session 等';
COMMENT ON COLUMN sys_taxonomy_node.parent_id IS '父节点，根为 NULL';

CREATE INDEX IF NOT EXISTS idx_sys_taxonomy_scope_parent ON sys_taxonomy_node (module_scope, parent_id)
    WHERE deleted = 0;
CREATE INDEX IF NOT EXISTS idx_sys_taxonomy_owner_scope ON sys_taxonomy_node (owner_id, module_scope)
    WHERE deleted = 0;

-- 种子：短视频素材（与 tag-presets 互补，可作为规范 code）
INSERT INTO sys_taxonomy_node (owner_id, module_scope, parent_id, code, name, sort_order, enabled, deleted, create_time, update_time)
SELECT 0, 'shortvideo_material', NULL, 'product_showcase', '产品展示', 10, 1, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
    WHERE NOT EXISTS (SELECT 1 FROM sys_taxonomy_node WHERE owner_id = 0 AND module_scope = 'shortvideo_material' AND code = 'product_showcase' AND deleted = 0);
INSERT INTO sys_taxonomy_node (owner_id, module_scope, parent_id, code, name, sort_order, enabled, deleted, create_time, update_time)
SELECT 0, 'shortvideo_material', NULL, 'talking_head', '口播', 20, 1, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
    WHERE NOT EXISTS (SELECT 1 FROM sys_taxonomy_node WHERE owner_id = 0 AND module_scope = 'shortvideo_material' AND code = 'talking_head' AND deleted = 0);
INSERT INTO sys_taxonomy_node (owner_id, module_scope, parent_id, code, name, sort_order, enabled, deleted, create_time, update_time)
SELECT 0, 'shortvideo_material', NULL, 'transition', '转场', 30, 1, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
    WHERE NOT EXISTS (SELECT 1 FROM sys_taxonomy_node WHERE owner_id = 0 AND module_scope = 'shortvideo_material' AND code = 'transition' AND deleted = 0);
