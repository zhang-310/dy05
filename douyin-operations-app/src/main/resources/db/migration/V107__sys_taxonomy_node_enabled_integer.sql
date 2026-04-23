-- V092 建表使用 SMALLINT；JPA SysTaxonomyNode.enabled 为 Integer → Hibernate 校验期望 INTEGER
ALTER TABLE sys_taxonomy_node
    ALTER COLUMN enabled TYPE INTEGER USING (enabled::integer);
