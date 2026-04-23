# 系统管理模块（system）

## 模块概述

系统级管理功能，包含外部 API 管理、告警引擎、监控指标、仪表盘数据、系统配置等。

## 后端结构

```
module/system/
├── config/
│   ├── ExternalApiHealthCheckScheduler.java  # 外部 API 健康检查
│   ├── ApiCallLogCleanupScheduler.java       # 调用日志清理
│   ├── ApiCallLogInterceptor.java            # 调用日志拦截器
│   └── SystemRestTemplateConfig.java         # RestTemplate 配置
│
├── controller/
│   ├── ExternalApiConfigController.java      # 外部 API 配置
│   ├── AlertController.java                  # 告警管理
│   ├── MonitoringController.java             # 监控
│   ├── MetricsController.java                # 指标
│   ├── SystemController.java                 # 系统信息
│   └── SystemPerformanceController.java      # 性能监控
│
├── entity/
│   ├── ExternalApiConfig.java                # 外部 API 配置
│   ├── ExternalApiCallLog.java               # 外部 API 调用日志
│   ├── SysApiCallLog.java                    # 系统 API 调用日志
│   └── SysSyncLog.java                       # 同步日志
│
├── service/
│   ├── ExternalApiConfigService / Impl       # 外部 API 管理
│   ├── ExternalApiGateway / Impl             # API 网关代理
│   ├── AlertEngineService / Impl             # 告警引擎
│   ├── MetricsCollectorService / Impl        # 指标采集
│   ├── DashboardDataService / Impl           # 仪表盘数据
│   └── SystemService / Impl                  # 系统信息
│
└── vo/
    ├── ExternalApiConfigSaveVO / SearchVO
    ├── AlertRuleVO / AlertRecordVO
    ├── MetricsVO
    └── DashboardDataVO

module/config/                                # 系统配置（独立子模块）
├── controller/ConfigController.java          # 配置 CRUD
├── entity/
│   ├── SysConfig.java                        # 系统配置
│   ├── SysConfigGroup.java                   # 配置分组
│   ├── SysIndustry.java                      # 行业分类
│   └── ConfigVersionHistory.java             # 配置版本历史
├── service/ConfigService / Impl
└── vo/ConfigSaveVO / SearchVO / VO

module/log/                                   # 日志（独立子模块）
├── config/
│   ├── OperationLogFilter.java               # 操作日志过滤器
│   ├── LoginSuccessLogListener.java          # 登录成功日志
│   └── SystemLogStartupListener.java         # 系统启动日志
├── controller/LogController.java             # 日志查询
├── entity/
│   ├── OperationLog.java                     # 操作日志
│   └── SystemLog.java                        # 系统日志
├── service/OperationLogService / Impl
└── vo/OperationLogVO
```

## 前端页面

| 页面 | 文件 | 路由 |
|------|------|------|
| 系统状态 | `pages/crud/SystemStatusPage.tsx` | `/admin/system` |
| API 日志 | `pages/crud/ApiLogPage.tsx` | `/admin/system/api-log` |
| 同步日志 | `pages/crud/SyncLogPage.tsx` | `/admin/system/sync-log` |
| 外部 API 配置 | `pages/system/ExternalApiConfigPage.tsx` | `/admin/system/external-api` |
| 外部 API 健康 | `pages/system/ExternalApiHealthPage.tsx` | `/admin/system/external-api-health` |
| 合规检查 | `pages/system/ComplianceCheckPage.tsx` | `/admin/system/compliance` |
| 性能监控 | `pages/system/PerformanceMonitoringPage.tsx` | `/admin/system/performance`（未注册路由） |
| 系统配置 | `pages/crud/ConfigPage.tsx` | `/admin/config` |
| 操作日志 | `pages/crud/OperationLogPage.tsx` | `/admin/log/operation` |
| 系统日志 | `pages/crud/SystemLogPage.tsx` | `/admin/log/system` |

## 前端 API

文件：`api/system.ts`、`api/external-api.ts`、`api/config.ts`、`api/log.ts`、`api/performance.ts`、`api/monitoring.ts`

SQL 文件：`sql/config/`、`sql/log/`
