-- V048: 知识图谱（Phase 3.1）
CREATE TABLE IF NOT EXISTS ai_graph_node (
    id BIGSERIAL PRIMARY KEY,
    owner_id BIGINT NOT NULL DEFAULT 0,
    entity_type VARCHAR(32) NOT NULL,
    entity_name VARCHAR(200) NOT NULL,
    properties JSONB DEFAULT '{}',
    source_doc_id BIGINT,
    confidence DOUBLE PRECISION DEFAULT 0.8,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted INTEGER DEFAULT 0
);
CREATE INDEX IF NOT EXISTS idx_graph_node_type ON ai_graph_node(entity_type, deleted);
CREATE INDEX IF NOT EXISTS idx_graph_node_name ON ai_graph_node(entity_name, deleted);
CREATE UNIQUE INDEX IF NOT EXISTS uk_graph_node ON ai_graph_node(owner_id, entity_type, entity_name) WHERE deleted = 0;

CREATE TABLE IF NOT EXISTS ai_graph_edge (
    id BIGSERIAL PRIMARY KEY,
    owner_id BIGINT NOT NULL DEFAULT 0,
    source_node_id BIGINT NOT NULL,
    target_node_id BIGINT NOT NULL,
    relation_type VARCHAR(64) NOT NULL,
    weight DOUBLE PRECISION DEFAULT 1.0,
    properties JSONB DEFAULT '{}',
    source_doc_id BIGINT,
    confidence DOUBLE PRECISION DEFAULT 0.8,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted INTEGER DEFAULT 0
);
CREATE INDEX IF NOT EXISTS idx_graph_edge_source ON ai_graph_edge(source_node_id, deleted);
CREATE INDEX IF NOT EXISTS idx_graph_edge_target ON ai_graph_edge(target_node_id, deleted);
CREATE INDEX IF NOT EXISTS idx_graph_edge_relation ON ai_graph_edge(relation_type, deleted);
