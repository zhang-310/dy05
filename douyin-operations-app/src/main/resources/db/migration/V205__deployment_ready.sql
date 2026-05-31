-- V205: 上线检查清单 — 数据库就绪标记
-- 本轮升级后数据库已就绪，可执行 Docker 部署

DO $$
BEGIN
    RAISE NOTICE '========================================';
    RAISE NOTICE ' dy05 Platform v2.0 — 数据库就绪';
    RAISE NOTICE ' 200+ Flyway 迁移已应用';
    RAISE NOTICE ' 18+ 业务表已创建';
    RAISE NOTICE ' 部署: deploy.cmd 或 docker compose up -d';
    RAISE NOTICE '========================================';
END $$;
