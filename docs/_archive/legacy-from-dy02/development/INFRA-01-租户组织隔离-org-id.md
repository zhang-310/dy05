# INFRA-01：租户组织（org_id）与应用层隔离

## 本阶段范围

- **数据库**：`live_session`、`payment_order`、`sv_project`、`live_product` 增加可空列 `org_id`（Flyway **V097**），并按 `auth_user.organization_id` 回填历史行。
- **应用层**：新建/下单/写入选品时若 `org_id` 为空，由 `TenantOrgResolutionHelper` 从 `auth_user` 解析并写入。列表与权限仍以 **user_id / owner_id** 为主（与既有契约一致）。
- **PostgreSQL RLS**：**本阶段不做**。防御纵深依赖应用层查询条件 +  JWT 中的 `organizationId`（见 `AuthTokenFilter`）。若将来启用 RLS，需单独迁移与会话变量注入设计。

## 开发约定

1. 新增涉及多租户汇总类查询时，优先使用 `org_id` 过滤（可空时需 `OR org_id IS NULL` 策略与产品确认）。
2. 个人账号未绑定组织时 `org_id` 可为 `NULL`，不得拒绝写业务主表。
3. 后台任务（如 `DouyinDataCollector`）写入 `live_product` 时，优先沿用场次的 `org_id`，否则回退为用户解析。

## 相关代码

- `TenantOrgResolutionHelper`
- `LiveSessionServiceImpl`、`OrderServiceImpl`、`SvProjectServiceImpl`、`LiveProductServiceImpl`、`DouyinDataCollector`
