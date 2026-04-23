# 企业微信模块（wecom）

## 模块概述

企业微信机器人管理，支持配置 Webhook 机器人、设置推送规则（触发条件 + 消息模板），记录消息发送日志。

## 后端结构

```
module/wecom/
├── controller/
│   └── WecomController.java                  # /api/v1/wecom — 机器人、规则、日志、手动推送
├── entity/
│   ├── WcRobotConfig.java                    # 机器人配置（wc_robot_config）
│   ├── WcPushRule.java                       # 推送规则（wc_push_rule）
│   └── WcMessageLog.java                     # 消息日志（wc_message_log，无逻辑删除）
├── service/
│   ├── WecomService / impl/                  # 机器人、规则、日志 CRUD
│   └── NotificationTriggerService / impl/    # 推送触发（事件驱动）
└── vo/
    ├── WcRobotConfigSaveVO / SearchVO / VO
    ├── WcPushRuleSaveVO / VO
    ├── WcMessageLogSearchVO / VO
    └── WcSendMessageVO                       # 手动推送
```

## 数据库表

| 表名 | 说明 |
|------|------|
| wc_robot_config | 机器人配置（webhook_url, robot_type） |
| wc_push_rule | 推送规则（trigger_type, trigger_config, message_template） |
| wc_message_log | 消息日志（无 deleted 字段） |

> 注意：表前缀为 `wc_`，不是 `wecom_`。

SQL 文件：`sql/wecom/`

## 前端页面

| 页面 | 路由 | 说明 |
|------|------|------|
| 机器人管理 | `/admin/wecom/robots` | DataTablePage（通用表格） |
| 推送规则 | `/admin/wecom/rules` | DataTablePage |
| 消息日志 | `/admin/wecom/log` | DataTablePage |

## 前端 API

文件：`api/wecom.ts`

| 函数 | 说明 |
|------|------|
| listRobots | 机器人列表 |
| listRules | 推送规则列表 |
| listMessageLogs | 消息日志列表 |
