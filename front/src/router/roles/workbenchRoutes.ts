/**
 * 前端工作台路由 — Sprint 6 工作台拆分
 *
 * 3个差异化工作台的路由定义。
 * 当前 admin/talent/org 路由已存在，需扩展为:
 *   admin-console    → /admin/*     (管理员)
 *   live-workbench   → /live/*      (主播+运营)
 *   creator-studio   → /creator/*   (摄影+中控+选品)
 *
 * 拆分规则 (从 ADR 009):
 *   admin-console:  Dashboard, 用户管理, 系统配置, 审计日志
 *   live-workbench: 直播场次, 话术管理, 商品管理, GMV看板
 *   creator-studio: 短视频, 素材库, AI生成, 竞品分析
 *
 * 实现: 在 front/src/router/roles/ 下创建 workbenchRoutes.tsx
 * 可参考 adminRoutes.tsx / talentRoutes.tsx 的结构
 */
export const WORKBENCH_SPLIT_PLAN = {
  adminConsole: {
    path: '/admin',
    label: '管理后台',
    pages: ['DashboardOverview', 'Users', 'Roles', 'Config', 'AuditLog', 'OpsMonitor']
  },
  liveWorkbench: {
    path: '/live',
    label: '直播工作台',
    pages: ['LiveSession', 'ScriptManage', 'ProductManage', 'GmvDashboard', 'Briefing']
  },
  creatorStudio: {
    path: '/creator',
    label: '创作者工作室',
    pages: ['ShortVideo', 'MaterialLibrary', 'AiGenerate', 'CompetitorMonitor']
  }
};
