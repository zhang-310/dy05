# 智能体模块（agent）

## 模块概述

AI 智能体对话系统，支持创建自定义 Agent 并与用户进行多轮对话，记录用户偏好。

## 后端结构

```
module/agent/
├── controller/
│   ├── AgentController.java                  # Agent CRUD + 对话
│   └── UserPreferenceController.java         # 用户偏好
│
├── entity/
│   ├── Agent.java                            # 智能体（表名 agent）
│   ├── AgentConversation.java                # 对话（agent_conversation）
│   ├── AgentMessage.java                     # 消息（agent_message）
│   └── AgentUserPreference.java              # 用户偏好
│
├── service/
│   ├── AgentService.java / impl/             # 智能体管理 + 对话
│   └── UserPreferenceService.java / impl/    # 偏好管理
│
└── vo/
    ├── AgentSaveVO / SearchVO / VO
    └── ...
```

## 前端页面

| 页面 | 文件 | 路由 |
|------|------|------|
| 智能体列表 | `pages/agent/AgentListPage.tsx` | `/admin/agent/list` |
| 智能体对话 | `pages/agent/AgentChatPage.tsx` | `/admin/agent/chat/:id` |

## 前端 API

文件：`api/agent.ts`

支持 SSE 流式对话：`chatWithAgentStream()`

SQL 文件：`sql/agent/`
